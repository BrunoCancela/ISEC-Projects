#ifndef MOTOR_COMUNICATION_H
#define MOTOR_COMUNICATION_H

#include "game_structures.h" // Inclui definições de estruturas usadas pelo jogo

#define MAX 265
#define MOTORPIPE "motorp"

// Estrutura para comandos enviados e recebidos pelo motor do jogo
typedef struct {
    int cmdType; // 1 - Join; 2 - Key; 3 - Show maze; 4 - Chat
    char player[MAX];
    char cmd[MAX];
    Map map;
} Commands;

// Estrutura para gerenciar o pipe do motor do jogo
typedef struct {
    int mayContinue;
    int pipe;
    MotorData* motorData;
} Pipemotor;

// Funções expostas
void sendCmd(Commands commands);
void sendToAll(Player players[], int numPlayers, Commands commands);
void* receiveCmd(void* arg);

#endif // MOTOR_COMUNICATION_H
