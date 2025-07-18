//
// Created by roger on 30-12-2023.
//

#ifndef SO_JOGADOR_COMUNICATION_H
#define SO_JOGADOR_COMUNICATION_H
#include "game_structures.h"

extern char name[MAX];

typedef struct {
    int cmdType; // 1 - Join; 2 - Key; 3 - Show maze; 4 - Chat
    char player[MAX];
    char cmd[MAX];
    Map map;
} Commands;

void desenhaMapa(WINDOW *janela, int tipo);

void* receiveMsg(void* arg);
#endif //SO_JOGADOR_COMUNICATION_H
