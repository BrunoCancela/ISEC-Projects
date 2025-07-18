#ifndef MOTOR_PROCESSES_H
#define MOTOR_PROCESSES_H
#include "game_structures.h"
#include "motor_comunication.h"

typedef struct {
    int interval;
    int duration;
    Pipemotor* pipemotor;
}BotLauch;

void processCommands(Pipemotor* pipemotor, Commands commands);
void* stones(void* arg);
void* mobileBlocks(void* arg);
void checkMap(Pipemotor* pipemotor);
void* endGameTimer(void *args);
void* joinTimer(void *args);
void* setupBots(void* args);
void* launchBot(void *args);
void findRandomEmptyPosition(Pedra* pedra);

#endif //MOTOR_PROCESSES_H