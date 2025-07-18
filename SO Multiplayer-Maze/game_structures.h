// file: game_structures.h

#ifndef GAME_STRUCTURES_H
#define GAME_STRUCTURES_H

#define MAX_PLAYERS 5
#define MAX_BOTS 10
#define MAX_OBSTACLES 50
#define MAX_LEVELS 3
#define MAX_MOVABLE_BLOCKS 5
#define MAP_WIDTH 40
#define MAP_HEIGHT 16
#define MAX 265

typedef struct {
    char map[MAP_HEIGHT][MAP_WIDTH];
    int stones;
    int mobileBlockers;
    int lvl;
} Map;

typedef struct {
    char player_name[MAX];
    int current_x;
    int current_y;
    int ativo;
} Player;

typedef struct {
    int lin;
    int col;
    int duration;
} BotConfig;

typedef struct {
    int inscricao;
    int nPlayers;
    int duracao;
    int decremento;
} Configuracoes;

typedef struct{
    Map map;
    Player* players;
    int numPlayers;
    int canJoin;
    int timer;
    pthread_mutex_t mapMutex;
    Configuracoes config;
    pthread_t bots[MAX_BOTS];
    pthread_t setup_thread;
    int numBots;
} MotorData;

typedef struct {
    BotConfig botConfig;
    MotorData* motorData;
} Pedra;

#endif // GAME_STRUCTURES_H