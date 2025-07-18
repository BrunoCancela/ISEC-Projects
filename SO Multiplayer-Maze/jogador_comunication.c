//
// Created by roger on 30-12-2023.
//
#include <ncurses.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <unistd.h>
#include <time.h>
#include <signal.h>
#include <fcntl.h>
#include <sys/stat.h>
#include <sys/types.h>
#include <pthread.h>
#include "jogador_comunication.h"

#define MOTORPIPE "motorp"
void desenhaMapa(WINDOW *janela, int tipo)
{

    // quando temos o scroll ativo, não deveremos ter a borda desenhada na janela para não termos o problema escrever em cima das bordas
    if (tipo == 1)
    {
        scrollok(janela, TRUE); // liga o scroll na "janela".
        wprintw(janela, "\n #> ");
    }
    else
    {
        keypad(janela, TRUE); // para ligar as teclas de direção (aplicar à janela)
        wclear(janela);// limpa a janela
        wborder(janela, '|', '|', '-', '-', '+', '+', '+', '+'); // Desenha uma borda. Nota importante: tudo o que escreverem, devem ter em conta a posição da borda
    }
    refresh(); // necessário para atualizar a janela
    wrefresh(janela); // necessário para atualizar a janela
}

void *receiveMsg(void *arg) {
    int pipe_fd;
    pipe_fd = open(MOTORPIPE, O_WRONLY);
    if (pipe_fd == -1) {
        fprintf(stderr, "Erro ao abrir o pipe %s!\n", MOTORPIPE);
        exit(-1);
    }

    if (mkfifo(name, 0666) == -1) {
        endwin();
        fprintf(stderr, "Erro ao criar o pipe %s!\n", name);
        exit(-1);
    }

    int pipe_player = open(name, O_RDONLY | O_NONBLOCK);
    if (pipe_player == -1) {
        fprintf(stderr, "Erro ao abrir o pipe %s!\n", name);
        exit(-1);
    }

    WINDOW *janelaComando = (WINDOW *)arg;

    Commands command;
    command.cmdType = 1; // Exemplo: Tipo de comando 1
    strcpy(command.player, name);
    strcpy(command.cmd, "Join Game");

    //printf("\n%s",command.player);
    //printf("\n%s",command.cmd);

    write(pipe_fd, &command, sizeof(command));
    close(pipe_fd);

    //mvprintw(1, 10, "Bem-vindo : %s",command.player);


    WINDOW *janelaJogo = newwin(MAP_HEIGHT+2, MAP_WIDTH+2, 2, 1);
    //WINDOW *janelaComando = newwin(5, 25, MAP_HEIGHT+5, 1);

    //desenhaMapa(janelaJogo, 2);  // função exemplo que desenha o janela no ecrã
    //desenhaMapa(janelaComando, 2);

    wrefresh(janelaJogo);
    //wrefresh(janelaComando);

    while(1){
        if(read(pipe_player, &command, sizeof(command)) > 0) {
            //mvprintw(1, 10,"%s\n", command.cmd);

            if(command.cmdType == 3) {
                wclear(janelaJogo);
                desenhaMapa(janelaJogo,2);
                for (int i = 0; i < MAP_HEIGHT; ++i) {
                    for (int j = 0; j < MAP_WIDTH; ++j) {
                        mvwprintw(janelaJogo, i+1, j+1, "%c",command.map.map[i][j]);
                        wrefresh(janelaJogo);
                        //printf(" %c ", command.map.map[i][j]);
                    }
                    //printf("\n");
                }
            }
            else if(command.cmdType == 4){
                printf("%s",command.cmd);
                union sigval val;
                sigqueue(getpid(),SIGUSR1,val);
            }
            else if(command.cmdType == 5) {
                wclear(janelaComando);
                desenhaMapa(janelaComando,2);
                char *player, *msg;

                // Usando strtok para dividir a string no primeiro espaço
                player = strtok(command.cmd, " ");
                msg = strtok(NULL, "");

                mvwprintw(janelaComando, 1, 1, "Mensagem de %s :",player);
                mvwprintw(janelaComando, 2, 1, "%s",msg);
                wrefresh(janelaComando);
            }
            else if(command.cmdType == 6) {
                wclear(janelaComando);
                desenhaMapa(janelaComando,2);
                char *player;
                int num=0;
                char *token = strtok(command.cmd, "\n");
                while (token != NULL) {
                    num++;
                    mvwprintw(janelaComando, num, 1, "%d - %s",num ,token);
                    token = strtok(NULL, "\n");
                }
                wrefresh(janelaComando);
            }
        }
    }
    close(pipe_fd);
}