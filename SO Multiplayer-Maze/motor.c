// file: motor.c
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <unistd.h>
#include <signal.h>
#include <time.h>
#include <sys/wait.h>
#include <pthread.h>
#include <sys/stat.h>
#include <errno.h>
#include <fcntl.h>
#include "game_structures.h"
#include "motor_comunication.h"
#include "motor.h"
#include "motor_processes.h"

Player players[MAX_PLAYERS];
int* numPlayers;
Bmov bmoveis[MAX_MOVABLE_BLOCKS];

void readAdminCommands();
void displayMap(Map* map);
void loadMap(char mapa[16][40], const char *fileName);

void getEnvs(MotorData* motorData) {
    char *envInscricao = getenv("INSCRICAO");
    char *envNPlayers = getenv("NPLAYERS");
    char *envDuracao = getenv("DURACAO");
    char *envDecremento = getenv("DECREMENTO");

    Configuracoes config;
    motorData->config.inscricao = (envInscricao != NULL) ? atoi(envInscricao) : 30;  // Valor padrão: 30 segundos
    motorData->config.nPlayers = (envNPlayers != NULL) ? atoi(envNPlayers) : 2;     // Valor padrão: 2 jogadores
    motorData->config.duracao = (envDuracao != NULL) ? atoi(envDuracao) : 60;       // Valor padrão: 60 segundos
    motorData->config.decremento = (envDecremento != NULL) ? atoi(envDecremento) : 5; // Valor padrão: 5 segundos
}

void termina(int s, siginfo_t *i, void *v)
{
    Commands commands;
    commands.cmdType = 4;
    strcpy(commands.cmd, "Jogo terminou");
    sendToAll(players,*numPlayers,commands);
    unlink(MOTORPIPE);
    printf("\nDesligar Motor\n");
    exit(1);
}


int main() {
    srand((unsigned) time (NULL));
    struct sigaction sa;
    sa.sa_sigaction = termina;
    sa.sa_flags = SA_SIGINFO;
    sigaction(SIGINT, &sa, NULL);


    Map map;
    map.lvl=1; map.stones = 0; map.mobileBlockers = 0;
    loadMap(map.map,"maze1.txt");
    MotorData motorData;
    motorData.players = players;
    motorData.map = map; motorData.numPlayers = 0; motorData.canJoin = 1;
    motorData.numBots = 0;
    numPlayers = &motorData.numPlayers;
    motorData.timer = 0;

    if (pthread_mutex_init(&motorData.mapMutex, NULL) != 0) {
        return 1;
    }

    getEnvs(&motorData);

    Pipemotor motor;
    motor.mayContinue = 1;
    motor.motorData = &motorData;

    if (mkfifo(MOTORPIPE, 0666) == -1) {
        if(errno == EEXIST){
            printf("Ja existe um motor a correr\n");
        }else{
            fprintf(stderr, "Erro ao criar o pipe %s!\n", MOTORPIPE);
        }
        exit(-1);
    }

    pthread_t motor_thread, end_game_thread, join_timer_thread;
    pthread_create(&motor_thread, NULL, receiveCmd, &motor);
    pthread_create(&end_game_thread, NULL, endGameTimer, &motor);
    pthread_create(&join_timer_thread, NULL, joinTimer, &motor);
    displayMap(&map);

    //setupBots(&motor);
    pthread_create(&motor.motorData->setup_thread, NULL, setupBots, &motor);



    // Leitura e processamento de comandos do administrador
    readAdminCommands(&motor);

    return 0;
}

void loadMap(char map[MAP_HEIGHT][MAP_WIDTH], const char *fileName) {
    FILE *file = fopen(fileName, "r");
    if (file == NULL) {
        printf("Erro opening file %s.\n", fileName);
        exit(1);
    }

    for (int i = 0; i < MAP_HEIGHT; i++) {
        for (int j = 0; j < MAP_WIDTH; j++) {
            char ch = (char) fgetc(file); // Read a single character
            if (ch == '\n') {
                // If it's a newline, adjust the column index
                j--;
                continue;
            }
            if (ch == EOF) { // Check for the end of file
                break;
            }
            map[i][j] = ch; // Store the character in the array
        }
    }
    fclose(file);
}

void verify_command(Pipemotor *pipemotor, const char* command) {
    // Assume input is a single word command or command followed by a single argument
    char buffer[256];
    strncpy(buffer, command, 255);
    buffer[255] = '\0'; // Ensure null termination

    char *cmd = strtok(buffer, " ");
    if (cmd == NULL) {
        return; // No command found
    }

    int num_args = 1;
    char *arg;
    char nome[MAX];

    while ((arg = strtok(NULL, " ")) != NULL) {
        strcpy(nome,arg);
        num_args++;
        if (num_args > 3) {
            return; // More than 3 arguments, return 0
        }
    }
    // Verify 'users' command
    if (strcmp(cmd, "users") == 0 && num_args == 1) {
        printf("'%s' is a valid command.\n", command);
        printf("\nLista de users:");
        int i;
        for(i = 0; i < pipemotor->motorData->numPlayers; i++){
            printf("\n%d - %s",i+1,pipemotor->motorData->players[i].player_name);

        }
        if(i == 0){
            printf("\nVazio");
        }
        printf("\n");
        return;
    }

    // Verify 'kick' command
    if (strcmp(cmd, "kick") == 0 && num_args == 2) {
        printf("'%s' is a valid command.\n", command);
        int encontrou = -1;

        Commands commands;
        for(int i = 0; i < pipemotor->motorData->numPlayers; i++){
            if (strcmp(pipemotor->motorData->players[i].player_name, nome) == 0) {
                encontrou = i;
                strcpy(commands.player,pipemotor->motorData->players[i].player_name);
                break;
            }
        }
        if(encontrou != -1){
            commands.cmdType = 4;
            strcpy(commands.cmd, "Kick");
            sendCmd(commands);
            int playerX = 0, playerY = 0;
            if(pipemotor->motorData->players[encontrou].ativo == 1) {
                playerX = pipemotor->motorData->players[encontrou].current_x;
                playerY = pipemotor->motorData->players[encontrou].current_y;
                pthread_mutex_lock(&pipemotor->motorData->mapMutex);
                pipemotor->motorData->map.map[playerY][playerX] = ' ';
                pthread_mutex_unlock(&pipemotor->motorData->mapMutex);
            }
            sprintf(commands.cmd, "Jogador %s saiu", pipemotor->motorData->players[encontrou].player_name);
            for (int j = encontrou; j < pipemotor->motorData->numPlayers - 1; ++j) {
                pipemotor->motorData->players[j] = pipemotor->motorData->players[j + 1];
            }
            pipemotor->motorData->numPlayers--;

            memset(&pipemotor->motorData->players[pipemotor->motorData->numPlayers], 0, sizeof(Player));

            commands.cmdType = 3;
            commands.map = pipemotor->motorData->map;
            sendToAll(pipemotor->motorData->players,pipemotor->motorData->numPlayers,commands);
        }
        return;
    }

    // Verify 'bots' command
    if (strcmp(cmd, "bots") == 0 && num_args == 1) {
        printf("Existem %d ativos\n", pipemotor->motorData->numBots);
        return;
    }

    // Verify 'bmov' command
    if (strcmp(cmd, "bmov") == 0 && num_args == 1) {
        if (pipemotor->motorData->map.mobileBlockers < MAX_MOVABLE_BLOCKS) {
            Pedra* pedra = malloc(sizeof(Pedra));
            if (pedra == NULL) {
                perror("Falha ao alocar memória para mobileBlock");
                exit(EXIT_FAILURE);
            }
            pedra->motorData = pipemotor->motorData;
            pedra->botConfig.duration = 1;

            bmoveis[pipemotor->motorData->map.mobileBlockers].pedra = pedra;

            if (pthread_create(&bmoveis[pipemotor->motorData->map.mobileBlockers].bmov_thread, NULL, mobileBlocks, pedra) != 0) {
                perror("Falha ao criar thread para mobileBlock");
                free(bmoveis[0].pedra);
                exit(EXIT_FAILURE);
            }
            pipemotor->motorData->map.mobileBlockers++;
            printf("MobileBlock adicionado.\n");
        } else {
            printf("Limite de MobileBlocks atingido.\n");
        }
        return;
    }

    // Verify 'rbm' command
    if (strcmp(cmd, "rbm") == 0 && num_args == 1) {
        if (pipemotor->motorData->map.mobileBlockers > 0) {
            // Sinalizar para a thread parar
            bmoveis[0].pedra->botConfig.duration = 0;
            // Aguardar a thread terminar
            pthread_join(bmoveis[0].bmov_thread, NULL);
            // Limpar a posição no mapa
            pthread_mutex_lock(&pipemotor->motorData->mapMutex);
            pipemotor->motorData->map.map[bmoveis[0].pedra->botConfig.lin][bmoveis[0].pedra->botConfig.col] = ' ';
            pthread_mutex_unlock(&pipemotor->motorData->mapMutex);
            // Liberar a memória
            free(bmoveis[0].pedra);
            // Reordenar o array de mobileBlocks
            for (int i = 0; i < pipemotor->motorData->map.mobileBlockers - 1; i++) {
                bmoveis[i] = bmoveis[i + 1];
            }
            pipemotor->motorData->map.mobileBlockers--;
            printf("MobileBlock removido.\n");
        } else {
            printf("Nao foi possivel remover um MobileBlock\n");
        }
        return;
    }

    // Verify 'begin' command
    if (strcmp(cmd, "begin") == 0 && num_args == 1) {
        printf("'%s' is a valid command.\n", command);
        pipemotor->motorData->canJoin = 0;
        return;
    }

    // Verify 'end' command
    if (strcmp(cmd, "end") == 0 && num_args == 1) {
        union sigval val;
        sigqueue(getpid(),SIGINT,val);
    }

    // If none of the above checks pass, return 0
    printf("'%s' is an invalid command. Please try again.\n", command);
}

void readAdminCommands(Pipemotor* pipemotor) {
    char command[256]; // Alocar espaço para o comando
    printf("Enter admin command: ");
    while(fgets(command, sizeof(command), stdin)) {
        command[strcspn(command, "\n")] = 0;

        verify_command(pipemotor, command);
        printf("Enter admin command: ");
    }
}

void displayMap(Map* map) {
    system("clear");
    for (int i = 0; i < MAP_HEIGHT; ++i) {
        for (int j = 0; j < MAP_WIDTH; ++j) {
            printf(" %c ",map->map[i][j]);
        }
        printf("\n");
    }
}