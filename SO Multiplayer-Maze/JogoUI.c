#include <ncurses.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <unistd.h>
#include <fcntl.h>
#include <sys/types.h>
#include <signal.h>
#include <sys/wait.h>
#include <pthread.h>
#include <sys/stat.h>
#include <errno.h>
#include <time.h>
#include <string.h>
#include "game_structures.h"
#include "jogador_comunication.h"

#define MOTORPIPE "motorp"

char name[MAX];

void iniciar() {
    initscr();
    noecho();
    curs_set(FALSE);
    keypad(stdscr, TRUE);
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

void menssagemInicial(int mode, const char *command) {
    if (mode == 0) {
        mvprintw(0, 0, "Pressione uma tecla:");
        mvprintw(1, 0, "Tecla: %s", command);
    } else {
        mvprintw(0, 0, "Modo de espaço. Digite um comando:");
        mvprintw(1, 0, "Comando: %s", command);
    }
    refresh();
}

void processComandos(int * tipo, char *command, WINDOW *janela,Commands pipe_comands) {
    if (strcmp(command, "exit") == 0) {
        union sigval val;
        sigqueue(getpid(), SIGINT, val);
    } else if (strcmp(command, "players") == 0) {
        *tipo = 6;
    } else if (strncmp(command, "msg", 3) == 0) {
        int num_args = 0,si=0;

        // Copying the command string to preserve the original
        char *command_copy = strdup(command); // or use your own string copy method

        // Tokenizing the copy
        char *token = strtok(command_copy, " ");
        while (token != NULL) {
            num_args++;
            if(strcmp(token,pipe_comands.player)==0)
                si=1;
            token = strtok(NULL, " ");
        }

        // Use the original command string for display
        if (num_args >= 3 && si!=1) {
            mvwprintw(janela, 2, 1, " Mensagem enviada [%s] ", command);
            *tipo = 5;
        } else if(si == 1){
            mvwprintw(janela, 2, 1, " Não é possivel mandar mensagem para si mesmo ");
            *tipo = -1;
        }else {
            mvwprintw(janela, 2, 1, " Numero invalido de argumentos: [%s] ", command);
            *tipo = -1;
        }

        // Free the copied string
        free(command_copy);
    }else {
        mvwprintw(janela,2,1, " Comando invalido : [%s] ", command); // Linha 4
        *tipo = -1;
    }
}

void processarTeclaNormal(int *tipo, char *command, int ch,WINDOW *janela, Commands pipe_comands) {
    *tipo = 2;
    if (ch == KEY_UP) {
        strncpy(command, "up", sizeof(command));
    } else if (ch == KEY_RIGHT) {
        strncpy(command, "right", sizeof(command));
    } else if (ch == KEY_LEFT) {
        strncpy(command, "left", sizeof(command));
    } else if (ch == KEY_DOWN) {
        strncpy(command, "down", sizeof(command));
    } else if (ch == ' ') {
        wclear(janela);
        desenhaMapa(janela,2);
        //werase(janela);
        echo();
        memset(command, 0, sizeof(command));
        mvwprintw(janela, 1, 1,"Escreva um comando: ");
        wgetstr(janela, command);
        noecho(); //voltar a desabilitar o que o utilizador escreve
        processComandos(tipo,command,janela,pipe_comands);
        wrefresh(janela);

    }
    //clear();
    refresh();
}

void limparComando(char *command, int *command_length) {
    memset(command, 0, sizeof(command));
    *command_length = 0;
}

/*void executarComando(int *mode, char *command, int *command_length, int ch) {
    int num_args = 0;
    if (ch == 10) { // Enter
         if (strcmp(command, "exit") == 0) {
	        endwin();
	        exit(0);
	   } else if (strcmp(command, "players") == 0) {
                    clear();
                    mvprintw(3, 0, "Comando executado: %s", command); // Linha 3
                    refresh();
            } else if (strncmp(command, "msg", 3) == 0) {
                    char *token = strtok(command, " ");
                    while (token != NULL) {
                   	 token = strtok(NULL, " ");
                   	 num_args++;
		    }
		    if (num_args == 3) {
                        clear();
                        mvprintw(3, 0, "Comando executado: %s", command); // Linha 3
                        refresh();
                    } else {
                        clear();
                        mvprintw(4, 0, "Comando inválido: %s", command); // Linha 4
                        refresh();
                    }
             }else {
                    clear();
                    mvprintw(4, 0, "Comando inválido: %s", command); // Linha 4
                    refresh();
              }
        limparComando(command, command_length);
        *mode = 1;
        clear();
    } else if (ch == 27) { // Esc
        *mode = 0;
        limparComando(command, command_length);
        clear();
    } else if (*command_length < sizeof(command) - 1) {
        command[*command_length] = ch; // Adiciona o caractere atual ao comando
        (*command_length)++; // Incrementa o comprimento do comando
        command[*command_length] = '\0';
    }
}*/

void termina(int s, siginfo_t *i, void *v)
{
    endwin();
    unlink(name);
    if(s != SIGUSR1){
        Commands commands;
        commands.cmdType = 4;
        strcpy(commands.player,name);
        int pipe_fd = open(MOTORPIPE, O_WRONLY);
        if (pipe_fd == -1) {
            fprintf(stderr, "Erro ao abrir o pipe %s!\n", MOTORPIPE);
            exit(-1);
        }
        write(pipe_fd, &commands, sizeof(commands));
        close(pipe_fd);
    }
    printf("\nDesligar Jogo\n");
    exit(1);
}



int main(int argc, char *argv[]) {
    if (argc != 2) {
        printf("Numero invalido de argumenros, Necessita 2 argumentos\n");
        return 1; // Encerre o programa se os argumentos estiverem incorretos
    }
    int pipe_fd;
    pipe_fd = open(MOTORPIPE, O_WRONLY);
    if (pipe_fd == -1) {
        fprintf(stderr, "Erro ao abrir o pipe %s!\n", MOTORPIPE);
        exit(-1);
    }
    close(pipe_fd);

    iniciar();

    struct sigaction sa;
    sa.sa_sigaction = termina;
    sigaction(SIGUSR1, &sa, NULL);
    sigaction(SIGINT, &sa, NULL);

    strcpy(name,argv[1]);

    WINDOW *janelaComando = newwin(10, 100, MAP_HEIGHT+5, 1);
    desenhaMapa(janelaComando, 2);
    wrefresh(janelaComando);

    pthread_t player_thread;
    pthread_create(&player_thread, NULL, receiveMsg, (void *)janelaComando);

    int tipo = 0; // 0 para o modo normal, 1 para o modo de espaço
    char command_player[256] = "";
    int command_length = 0;
    Commands command_pipe;



    while (1) {
        fflush(stdin);
        int ch = getch();
        //if (mode == 0) {
            processarTeclaNormal(&tipo, command_player, ch,janelaComando,command_pipe);
            if(tipo != -1) {
                pipe_fd = open(MOTORPIPE, O_WRONLY);
                if (pipe_fd == -1) {
                    fprintf(stderr, "Erro ao abrir o pipe %s!\n", MOTORPIPE);
                    exit(-1);
                }
                //mvprintw(1, 10, "Tecla: %s", command_player);

                command_pipe.cmdType = tipo; // Exemplo: Tipo de comando 1
                strcpy(command_pipe.player, name);
                strcpy(command_pipe.cmd, command_player);

                //printf("\n%s",command.player);
                //printf("\n%s",command.cmd);

                write(pipe_fd, &command_pipe, sizeof(command_pipe));
                close(pipe_fd);
            }
        /*}else{
            if (ch == 10) { // Tecla Enter pressionada para executar o comando
                if (strcmp(command_player, "exit") == 0) {
                    break;
                } else if (strcmp(command_player, "players") == 0) {
                    clear();
                    mvprintw(3, 0, "Comando executado: %s", command_player); // Linha 3
                    refresh();
                } else if (strncmp(command_player, "msg", 3) == 0) {
                    char *token = strtok(command_player, " ");
                    while (token != NULL) {
                        token = strtok(NULL, " ");
                        num_args++;
                    }

                    if (num_args == 3) {
                        clear();
                        mvprintw(3, 0, "Comando executado: %s", command_player); // Linha 3
                        refresh();
                    } else {
                        clear();
                        mvprintw(4, 0, "Comando inválido: %s", command_player); // Linha 4
                        refresh();
                    }
                } else {
                    clear();
                    mvprintw(4, 0, "Comando inválido: %s", command_player); // Linha 4
                    refresh();
                }

                mode = 1;
                memset(command_player, 0, sizeof(command_player)); // Limpa o comando atual
                command_length = 0;
                num_args = 0;
            } else if (ch == 27) { // Tecla Esc pressionada para sair do modo de espaço
                mode = 0;
                memset(command_player,0,sizeof(command_player));
                command_length = 0;
                num_args = 0;
                clear();
            } else if (command_length < sizeof(command_player) - 1) {
                command_player[command_length++] = ch;
            }
        }*/
        //menssagemInicial(mode, command);
     }
    return 0;
}


