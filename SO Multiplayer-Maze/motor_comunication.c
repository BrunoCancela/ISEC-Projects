//motor_comunication.c
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
#include "motor_processes.h"


//MOTOR:
void sendCmd(Commands commands){
    int pipe_fd;
    pipe_fd = open(commands.player, O_WRONLY);
    if(pipe_fd == -1){
        fprintf(stderr, "Erro a abrir o pipe\n");
    }
    write(pipe_fd, &commands, sizeof(commands));
    close(pipe_fd);
}

void sendToAll(Player players[],int numPlayers, Commands commands){
    for (int i = 0; i < numPlayers; ++i) {
        int pipe_fd;
        pipe_fd = open(players[i].player_name, O_WRONLY);
        if(pipe_fd == -1){
            fprintf(stderr, "Erro a abrir o pipe\n");
        }else {
            write(pipe_fd, &commands, sizeof(commands));
            close(pipe_fd);
        }
    }
}

void* receiveCmd(void* arg) {
    Pipemotor *a = (Pipemotor *)arg;
    int nbytes;
    Commands commands;
    a->pipe = open(MOTORPIPE, O_RDONLY);
    if (a->pipe == -1) {
        fprintf(stderr, "Erro a abrir o pipe %s!\n", MOTORPIPE);
        exit(-1);
    }
    while (1) {
        nbytes = read(a->pipe, &commands, sizeof(commands));
        if(nbytes > 0) {
            printf("Received: %s\n", commands.player);
            processCommands(a, commands);
        }
    }
}