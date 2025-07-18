#ifndef MOTOR_H
#define MOTOR_H

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
#include "motor_processes.h"

typedef struct {
    Pedra* pedra;
    pthread_t bmov_thread;
} Bmov;

// Global variables
extern Player players[MAX_PLAYERS];
extern int* numPlayers;

// Function prototypes
void launchTestBot();
void displayMap(Map* map);
void loadMap(char mapa[MAP_HEIGHT][MAP_WIDTH], const char *fileName);
void getEnvs(MotorData* motorData);
void termina(int s, siginfo_t *i, void *v);
void verify_command(Pipemotor *pipemotor, const char* command);
void readAdminCommands(Pipemotor *pipemotor);
void launchTestBot();
void displayMap(Map* map);

#endif // MOTOR_H

