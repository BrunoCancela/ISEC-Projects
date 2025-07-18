//
// Created by vboxuser on 12/23/23.
//
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <unistd.h>
#include <signal.h>
#include <fcntl.h>
#include <sys/stat.h>
#include <sys/types.h>
#include <pthread.h>

#include "game_structures.h"
#include "motor_comunication.h"
#include "motor_processes.h"
#include "motor.h"


void processCommands(Pipemotor* pipemotor, Commands commands){
    Player* player = NULL;
    int i;
    MotorData* motorData = pipemotor->motorData;
    // Encontrar o jogador que enviou o comando
    commands.map = motorData->map;
    for (i = 0; i < motorData->numPlayers; ++i) {
        if (strcmp(motorData->players[i].player_name, commands.player) == 0) {
            player = &motorData->players[i];
            break;
        }
    }
    // Se o jogador não foi encontrado, retorne ou trate o erro
    if (player == NULL) {
        if(commands.cmdType==1){
            if(motorData->numPlayers >= MAX_PLAYERS){
                commands.cmdType = 0;
                commands.map = motorData->map;
                sendCmd(commands);
            }else{
                Player player1;
                memcpy(player1.player_name, commands.player,sizeof(commands.player));
                commands.map = motorData->map;
                sendCmd(commands);
                if(motorData->canJoin == 1){
                    int spawns[5],spawns_aval=0;
                    for(int i = 0; i < MAP_WIDTH; i++){
                        if(motorData->map.map[MAP_HEIGHT-1][i] == ' ') {
                            spawns[spawns_aval++] = i;
                        }
                    }
                    int randomSpawn = rand() % spawns_aval;
                    player1.current_y = MAP_HEIGHT-1;
                    player1.current_x = spawns[randomSpawn];
                    motorData->map.map[player1.current_y][player1.current_x] = commands.player[0];
                    player1.ativo = 1;
                }else{
                    player1.current_y = 0;
                    player1.current_x = 0;
                    player1.ativo = 0;
                }
                motorData->players[motorData->numPlayers] = player1;
                motorData->numPlayers++;
                commands.map = motorData->map;
                commands.cmdType = 3;
                sendToAll(motorData->players,motorData->numPlayers,commands);
            }
        }else{
            fprintf(stderr, "Jogador não encontrado: %s\n", commands.player);
        }
        return;
    }

    if(commands.cmdType == 2 && player->ativo == 1 && motorData->canJoin == 0){
        pthread_mutex_lock(&motorData->mapMutex);
        if(strcmp(commands.cmd , "up") == 0){
            if(motorData->map.map[player->current_y-1][player->current_x] == ' '){
                motorData->map.map[player->current_y-1][player->current_x] = motorData->map.map[player->current_y][player->current_x];
                motorData->map.map[player->current_y][player->current_x] = ' ';
                player->current_y--;
            }
        }else if(strcmp(commands.cmd , "down") == 0){
            if(motorData->map.map[player->current_y+1][player->current_x] == ' '){
                motorData->map.map[player->current_y+1][player->current_x] = motorData->map.map[player->current_y][player->current_x];
                motorData->map.map[player->current_y][player->current_x] = ' ';
                player->current_y++;
            }
        }else if(strcmp(commands.cmd , "left") == 0){
            if(motorData->map.map[player->current_y][player->current_x-1] == ' '){
                motorData->map.map[player->current_y][player->current_x-1] = motorData->map.map[player->current_y][player->current_x];
                motorData->map.map[player->current_y][player->current_x] = ' ';
                player->current_x--;
            }
        }else{
            if(motorData->map.map[player->current_y][player->current_x+1] == ' '){
                motorData->map.map[player->current_y][player->current_x+1] = motorData->map.map[player->current_y][player->current_x];
                motorData->map.map[player->current_y][player->current_x] = ' ';
                player->current_x++;
            }
        }
        pthread_mutex_unlock(&motorData->mapMutex);
        checkMap(pipemotor);

    }else if(commands.cmdType == 4){
        fprintf(stderr, "Jogador %s saiu\n", player->player_name);
        sprintf(commands.cmd, "Jogador %s saiu", player->player_name);
        if(motorData->players[i].ativo == 1) {
            int playerX = motorData->players[i].current_x;
            int playerY = motorData->players[i].current_y;
            pthread_mutex_lock(&motorData->mapMutex);
            motorData->map.map[playerY][playerX] = ' ';
            pthread_mutex_unlock(&motorData->mapMutex);
        }
        for (int j = i; j < motorData->numPlayers - 1; ++j) {
            motorData->players[j] = motorData->players[j + 1];
        }
        motorData->numPlayers--;
        memset(&motorData->players[motorData->numPlayers], 0, sizeof(Player));
        commands.cmdType = 3;
        commands.map = motorData->map;
        sendToAll(motorData->players,motorData->numPlayers,commands);
    }else if(commands.cmdType == 5){
        char *player_rec,player_env[MAX],*comand,frase[MAX];
        strcpy(player_env, commands.player);
        strcpy(frase, commands.cmd);

        char* token = strtok(frase, " ");//retira msg
        player_rec = strtok(NULL, " ");
        comand = strtok(NULL, "");

        /*printf("Player receiver: %s\n", player_rec);
        printf("Player snder: %s\n", player_env);
        printf("Command: %s\n", comand);*/
        strcpy(commands.cmd, player_env);
        strcat(commands.cmd," ");
        strcat(commands.cmd,comand);
        strcpy(commands.player, player_rec);
        /*printf("Player receiver: %s\n", player_rec);
        printf("Player snder: %s\n", player_env);
        printf("Command: %s\n", comand);*/
        sendCmd(commands);
    }else if(commands.cmdType == 6){
        int players_act=0;
        for(i = 0; i < motorData->numPlayers; i++){
            if(motorData->players[i].ativo) {
                printf("\n%d - %s", i + 1, motorData->players[i].player_name);
                if (players_act == 0)
                    strcpy(commands.cmd, "\n");
                else
                    strcat(commands.cmd, "\n");
                strcat(commands.cmd, motorData->players[i].player_name);
                players_act++;
            }
        }
        if(i == 0){
            strcpy(commands.cmd,"Vazio");
        }
        sendCmd(commands);
    }
}

void checkMap(Pipemotor* pipemotor){
    int chegou = 1;

    for(int i = 0; i < MAP_WIDTH; i++){
        if(pipemotor->motorData->map.map[0][i] == ' ') {
            chegou = 0;
            break;
        }
    }

    if(chegou == 1){
        pipemotor->motorData->map.lvl++;
        pipemotor->motorData->timer = pipemotor->motorData->config.duracao -
                ((pipemotor->motorData->map.lvl-1)*pipemotor->motorData->config.decremento);
        pipemotor->motorData->config.duracao -= pipemotor->motorData->config.decremento;

        pthread_join(pipemotor->motorData->setup_thread, NULL);


        if(pipemotor->motorData->map.lvl > 3){
            pipemotor->motorData->timer = 0;
            union sigval val;
            sigqueue(getpid(),SIGINT,val);
        }else{
            pthread_mutex_lock(&pipemotor->motorData->mapMutex);
            char filename[256];
            int level = pipemotor->motorData->map.lvl;

            sprintf(filename, "maze%d.txt", level);
            loadMap(pipemotor->motorData->map.map, filename);

            int spawns[5], spawns_aval = 0;
            for (int i = 0; i < MAP_WIDTH; i++) {
                if (pipemotor->motorData->map.map[MAP_HEIGHT - 1][i] == ' ') {
                    spawns[spawns_aval++] = i;
                }
            }

            for (int i = 0; i < pipemotor->motorData->numPlayers; i++) {
                if (pipemotor->motorData->players[i].ativo == 1) {
                    int randomSpawn = rand() % spawns_aval;
                    pipemotor->motorData->players[i].current_y = MAP_HEIGHT - 1;
                    pipemotor->motorData->players[i].current_x = spawns[randomSpawn];
                    pipemotor->motorData->map.map[pipemotor->motorData->players[i].current_y][pipemotor->motorData->players[i].current_x] = pipemotor->motorData->players[i].player_name[0];
                }
            }

            pthread_mutex_unlock(&pipemotor->motorData->mapMutex);
        }
        pthread_create(&pipemotor->motorData->setup_thread, NULL, setupBots, pipemotor);

    }
    Commands commands;
    commands.map = pipemotor->motorData->map;
    commands.cmdType = 3;
    sendToAll(pipemotor->motorData->players,pipemotor->motorData->numPlayers,commands);
}

void* endGameTimer(void *args){
    Pipemotor * motor = (Pipemotor *)args;
    motor->motorData->timer = motor->motorData->config.duracao;
    while(motor->motorData->canJoin){}
    while(motor->motorData->timer){
        sleep(1);
        motor->motorData->timer--;
    }
    union sigval val;
    sigqueue(getpid(),SIGINT,val);
}

void* joinTimer(void *args){
    Pipemotor * motor = (Pipemotor *)args;
    while(motor->motorData->canJoin){
        sleep(1);
        if(motor->motorData->config.inscricao <= 0 && motor->motorData->numPlayers >= motor->motorData->config.nPlayers){
            motor->motorData->canJoin = 0;
        }else if(motor->motorData->numPlayers > 5){
            motor->motorData->canJoin = 0;
        }
        motor->motorData->config.inscricao--;
    }
}

void* stones(void *args){
    Pedra * pedra = (Pedra *)args;
    int currentLvl = pedra->motorData->map.lvl;
    if(pedra->motorData->canJoin == 1 && pedra->motorData->map.stones >= 50){
        pthread_exit(NULL);
    }

    pedra->motorData->map.map[pedra->botConfig.lin][pedra->botConfig.col] = 'P';
    pedra->motorData->map.stones++;
    Commands commands;
    commands.map = pedra->motorData->map;
    commands.cmdType = 3;
    sendToAll(pedra->motorData->players,pedra->motorData->numPlayers,commands);

    while(pedra->botConfig.duration > 0 && currentLvl == pedra->motorData->map.lvl){
        sleep(1);
        pedra->botConfig.duration--;
    }
    if(currentLvl == pedra->motorData->map.lvl) {
        pedra->motorData->map.map[pedra->botConfig.lin][pedra->botConfig.col] = ' ';
        Commands commands;
        commands.map = pedra->motorData->map;
        commands.cmdType = 3;
        sendToAll(pedra->motorData->players,pedra->motorData->numPlayers,commands);
    }

    pedra->motorData->map.stones--;

}

void* setupBots(void * arg){
    Pipemotor* pipemotor = (Pipemotor*) arg;

    if (!pipemotor || !pipemotor->motorData) {
        printf("Erro: pipemotor ou pipemotor->motorData é NULL.\n");
        pthread_exit(NULL);
    }

    BotLauch *botLauch1 = NULL, *botLauch2 = NULL, *botLauch3 = NULL, *botLauch4 = NULL;

    if(pipemotor->motorData->map.lvl == 1) {
        botLauch1 = malloc(sizeof(BotLauch));
        botLauch2 = malloc(sizeof(BotLauch));

        if (!botLauch1 || !botLauch2) {
            printf("Erro ao alocar memória para botLauch.\n");
            free(botLauch1);
            free(botLauch2);
            pthread_exit(NULL);
        }

        botLauch1->pipemotor = pipemotor; botLauch1->interval = 30; botLauch1->duration = 10;
        botLauch2->pipemotor = pipemotor; botLauch2->interval = 25; botLauch2->duration = 5;

        // Criação das threads
        if (pipemotor->motorData->numBots < MAX_BOTS) {
            pthread_create(&pipemotor->motorData->bots[pipemotor->motorData->numBots++], NULL, launchBot, botLauch1);
        }
        if (pipemotor->motorData->numBots < MAX_BOTS) {
            pthread_create(&pipemotor->motorData->bots[pipemotor->motorData->numBots++], NULL, launchBot, botLauch2);
        }
    }else if(pipemotor->motorData->map.lvl == 2) {
        botLauch1 = malloc(sizeof(BotLauch));
        botLauch2 = malloc(sizeof(BotLauch));
        botLauch3 = malloc(sizeof(BotLauch));

        if (!botLauch1 || !botLauch2 || !botLauch3) {
            printf("Erro ao alocar memória para botLauch.\n");
            free(botLauch1);
            free(botLauch2);
            free(botLauch3);
            pthread_exit(NULL);
        }

        botLauch1->pipemotor = pipemotor; botLauch1->interval = 30; botLauch1->duration = 15;
        botLauch2->pipemotor = pipemotor; botLauch2->interval = 25; botLauch2->duration = 10;
        botLauch3->pipemotor = pipemotor; botLauch3->interval = 20; botLauch3->duration = 5;
        if (pipemotor->motorData->numBots < MAX_BOTS) {
            pthread_create(&pipemotor->motorData->bots[pipemotor->motorData->numBots++], NULL, launchBot, botLauch1);
        }
        if (pipemotor->motorData->numBots < MAX_BOTS) {
            pthread_create(&pipemotor->motorData->bots[pipemotor->motorData->numBots++], NULL, launchBot, botLauch2);
        }
        if (pipemotor->motorData->numBots < MAX_BOTS) {
            pthread_create(&pipemotor->motorData->bots[pipemotor->motorData->numBots++], NULL, launchBot, botLauch3);
        }

        // Criação das threads para o nível 2
    } else if(pipemotor->motorData->map.lvl == 3) {
        botLauch1 = malloc(sizeof(BotLauch));
        botLauch2 = malloc(sizeof(BotLauch));
        botLauch3 = malloc(sizeof(BotLauch));
        botLauch4 = malloc(sizeof(BotLauch));

        if (!botLauch1 || !botLauch2 || !botLauch3 || !botLauch4) {
            printf("Erro ao alocar memória para botLauch.\n");
            free(botLauch1);
            free(botLauch2);
            free(botLauch3);
            free(botLauch4);
            pthread_exit(NULL);
        }

        botLauch1->pipemotor = pipemotor; botLauch1->interval = 30; botLauch1->duration = 20;
        botLauch2->pipemotor = pipemotor; botLauch2->interval = 25; botLauch2->duration = 15;
        botLauch3->pipemotor = pipemotor; botLauch3->interval = 20; botLauch3->duration = 10;
        botLauch4->pipemotor = pipemotor; botLauch4->interval = 15; botLauch4->duration = 5;
        if (pipemotor->motorData->numBots < MAX_BOTS) {
            pthread_create(&pipemotor->motorData->bots[pipemotor->motorData->numBots++], NULL, launchBot, botLauch1);
        }
        if (pipemotor->motorData->numBots < MAX_BOTS) {
            pthread_create(&pipemotor->motorData->bots[pipemotor->motorData->numBots++], NULL, launchBot, botLauch2);
        }
        if (pipemotor->motorData->numBots < MAX_BOTS) {
            pthread_create(&pipemotor->motorData->bots[pipemotor->motorData->numBots++], NULL, launchBot, botLauch3);
        }
        if (pipemotor->motorData->numBots < MAX_BOTS) {
            pthread_create(&pipemotor->motorData->bots[pipemotor->motorData->numBots++], NULL, launchBot, botLauch4);
        }

        // Criação das threads para o nível 3
    }

    // Aguardar o término das threads
    for (int i = 0; i < pipemotor->motorData->numBots; i++) {
        pthread_join(pipemotor->motorData->bots[i], NULL);
    }
    pipemotor->motorData->numBots = 0;

    // Liberação de memória
    free(botLauch1);
    free(botLauch2);
    free(botLauch3);
    free(botLauch4);
}

void* launchBot(void* arg) {
    BotLauch* botLauch = (BotLauch* ) arg;
    if (!botLauch) {
        printf("Erro: botLauch é NULL.\n");
        pthread_exit(NULL);
    }

    Pipemotor* pipemotor = botLauch->pipemotor;
    if (!pipemotor) {
        printf("Erro: pipemotor é NULL.\n");
        pthread_exit(NULL);
    }

    int interval = botLauch->interval;
    int duration = botLauch->duration;

    while (pipemotor->motorData->canJoin){}

    int FDFP[2];
    if(pipe(FDFP) == -1){
        printf("Erro ao criar o pipe\n");
        pthread_exit(NULL);
    }

    int currentLvl = pipemotor->motorData->map.lvl;

    int p = fork();
    if(p < 0){
        printf("\nErro ao iniciar o fork\n");
        close(FDFP[0]);
        close(FDFP[1]);
        pthread_exit(NULL);
    }

    if(p == 0){
        // Processo filho
        close(STDOUT_FILENO);
        dup(FDFP[1]);
        close(FDFP[1]);
        close(FDFP[0]);
        char intervalStr[10];
        char durationStr[10];
        sprintf(intervalStr, "%d", interval);
        sprintf(durationStr, "%d", duration);

        if(execl("./bot","bot",intervalStr,durationStr,NULL) == -1){
            printf("Erro ao executar execl\n");
            exit(EXIT_FAILURE);
        }
    } else {
        // Processo pai
        fd_set readfds;
        int retval;
        char str[365];
        close(FDFP[1]);

        while(currentLvl == pipemotor->motorData->map.lvl) {
            FD_ZERO(&readfds);
            FD_SET(FDFP[0], &readfds);

            retval = select(FDFP[0] + 1, &readfds, NULL, NULL, NULL);

            if (retval == -1) {
                perror("select()");
                break;
            } else if (retval) {
                ssize_t size = read(FDFP[0], str, sizeof(str)-1);
                if (size > 0) {
                    str[size-1] = '\0'; // Garante terminação correta da string
                    printf("RECEBI: (%s)\n", str);
                    int x, y, d;
                    if (sscanf(str, "%d %d %d", &x, &y, &d) == 3) {
                        // Os três inteiros foram lidos com sucesso
                        if (pipemotor->motorData->map.stones < MAX_OBSTACLES) {
                            if(pipemotor->motorData->map.map[y][x] == ' ' && y > 0 && y < MAP_HEIGHT-2) {
                                Pedra pedra;
                                pedra.botConfig.col = x;
                                pedra.botConfig.lin = y;
                                pedra.botConfig.duration = d;
                                pedra.motorData = pipemotor->motorData;
                                pthread_t stone_thread;
                                pthread_create(&stone_thread, NULL, stones, &pedra);
                            }
                        }
                    } else {
                        printf("Erro ao ler os três inteiros.\n");
                    }
                } else if (size == 0) {
                    break;
                }
            }
            usleep(100000); // 100ms
        }

        pipemotor->motorData->numBots--;

        union sigval val;
        sigqueue(p, SIGINT, val);
        waitpid(p, NULL, 0);
        close(FDFP[0]);
        pthread_exit(NULL);
    }
}

void* mobileBlocks(void *args) {
    Pedra *pedra = (Pedra *)args;
    int dx[] = {0, 0, -1, 1}; // Deslocamentos no eixo X para esquerda e direita
    int dy[] = {-1, 1, 0, 0}; // Deslocamentos no eixo Y para cima e baixo
    int currentLevel = pedra->motorData->map.lvl; // Armazena o nível atual

    // Encontrar uma posição inicial aleatória para a pedra
    findRandomEmptyPosition(pedra);

    while (pedra->botConfig.duration != 0) {
        // Verificar se o nível mudou
        if (currentLevel != pedra->motorData->map.lvl) {
            currentLevel = pedra->motorData->map.lvl;
            findRandomEmptyPosition(pedra); // Regenerar a posição da pedra
        }

        sleep(1); // Esperar um segundo antes de mover novamente

        // Gerar uma nova posição aleatória
        int direction = rand() % 4; // Escolher uma direção aleatória
        int new_x = pedra->botConfig.col + dx[direction];
        int new_y = pedra->botConfig.lin + dy[direction];

        pthread_mutex_lock(&pedra->motorData->mapMutex);
        // Checar se a nova posição está dentro dos limites e vazia
        if (new_x >= 0 && new_x < MAP_WIDTH && new_y >= 1 && new_y < MAP_HEIGHT-2 &&
            pedra->motorData->map.map[new_y][new_x] == ' ') {
            // Mover a pedra para a nova posição
            pedra->motorData->map.map[pedra->botConfig.lin][pedra->botConfig.col] = ' ';
            pedra->botConfig.col = new_x;
            pedra->botConfig.lin = new_y;
            pedra->motorData->map.map[new_y][new_x] = 'B';
        }
        pthread_mutex_unlock(&pedra->motorData->mapMutex);
        Commands commands;
        commands.map = pedra->motorData->map;
        commands.cmdType = 3;
        sendToAll(pedra->motorData->players,pedra->motorData->numPlayers,commands);
    }

    return NULL;
}

void findRandomEmptyPosition(Pedra *pedra) {
    pthread_mutex_lock(&pedra->motorData->mapMutex);

    int emptyPositions[MAP_HEIGHT * MAP_WIDTH][2];
    int count = 0;
    for (int y = 0; y < MAP_HEIGHT; ++y) {
        for (int x = 0; x < MAP_WIDTH; ++x) {
            if (pedra->motorData->map.map[y][x] == ' ') {
                emptyPositions[count][0] = x;
                emptyPositions[count][1] = y;
                ++count;
            }
        }
    }

    if (count > 0) {
        int randomIndex = rand() % count;
        int new_x = emptyPositions[randomIndex][0];
        int new_y = emptyPositions[randomIndex][1];

        // Move a pedra para a nova posição
        if(pedra->botConfig.lin != 0 && pedra->botConfig.col != 0){
            pedra->motorData->map.map[pedra->botConfig.lin][pedra->botConfig.col] = ' ';
        }

        pedra->botConfig.col = new_x;
        pedra->botConfig.lin = new_y;
        pedra->motorData->map.map[new_y][new_x] = 'B';
    }

    pthread_mutex_unlock(&pedra->motorData->mapMutex);
}
