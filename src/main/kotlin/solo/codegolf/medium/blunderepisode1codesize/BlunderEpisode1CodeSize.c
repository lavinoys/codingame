#include <stdlib.h>
#include <stdio.h>
#include <string.h>
#include <stdbool.h>

#define MAX_SIZE 100
#define MAX_MOVES 1000000

typedef struct {
    int x, y;
} Point;

typedef struct {
    char grid[MAX_SIZE][MAX_SIZE+1];
    int L, C;
    Point pos;
    Point teleporters[2];
    int teleCount;
    int dir;           // 0: SOUTH, 1: EAST, 2: NORTH, 3: WEST
    bool breakerMode;
    bool invertedPriorities;
    char moves[MAX_MOVES][6];  // "SOUTH", "EAST", "NORTH", "WEST"
    int moveCount;
    bool visited[MAX_SIZE][MAX_SIZE][4][2][2];  // [y][x][direction][breakerMode][invertedPriorities]
} Game;

const int dx[] = {0, 1, 0, -1};  // SOUTH, EAST, NORTH, WEST
const int dy[] = {1, 0, -1, 0};
const char* directions[] = {"SOUTH", "EAST", "NORTH", "WEST"};

void initGame(Game* game) {
    game->teleCount = 0;
    game->dir = 0;  // Start facing SOUTH
    game->breakerMode = false;
    game->invertedPriorities = false;
    game->moveCount = 0;
    memset(game->visited, 0, sizeof(game->visited));
}

bool canMove(Game* game, int dir) {
    int nx = game->pos.x + dx[dir];
    int ny = game->pos.y + dy[dir];
    
    if (nx < 0 || nx >= game->C || ny < 0 || ny >= game->L) {
        return false;
    }
    
    char cell = game->grid[ny][nx];

    if (cell == '#') {
        return false;
    }
    if (cell == 'X' && !game->breakerMode) {
        return false;
    }
    return true;
}

int getNextDir(Game* game) {
    if (game->invertedPriorities) {
        int priorities[] = {3, 2, 1, 0};
        for (int i = 0; i < 4; i++) {
            if (canMove(game, priorities[i])) {
                return priorities[i];
            }
        }
    } else {
        int priorities[] = {0, 1, 2, 3};
        for (int i = 0; i < 4; i++) {
            if (canMove(game, priorities[i])) {
                return priorities[i];
            }
        }
    }
    return game->dir;  // Shouldn't happen with valid maps
}

bool isLoop(Game* game) {
    // 현재 상태가 이전에 방문한 상태와 정확히 일치하는지 확인
    if (game->visited[game->pos.y][game->pos.x][game->dir][game->breakerMode][game->invertedPriorities]) {
        // 추가적으로 주변 상태도 확인
        for (int d = 0; d < 4; d++) {
            int nx = game->pos.x + dx[d];
            int ny = game->pos.y + dy[d];
            if (nx >= 0 && nx < game->C && ny >= 0 && ny < game->L) {
                if (game->grid[ny][nx] == 'X' && game->breakerMode) {
                    return false;  // 부술 수 있는 벽이 있다면 아직 루프가 아님
                }
            }
        }
        return true;
    }
    return false;
}

void markVisited(Game* game) {
    game->visited[game->pos.y][game->pos.x][game->dir][game->breakerMode][game->invertedPriorities] = true;
}

void handleCell(Game* game) {
    char cell = game->grid[game->pos.y][game->pos.x];
    
    // 방향 변경자 처리
    if (cell == 'S' || cell == 'E' || cell == 'N' || cell == 'W') {
        game->dir = (cell == 'S') ? 0 : (cell == 'E') ? 1 : (cell == 'N') ? 2 : 3;
        return;
    }

    switch(cell) {
        case 'I': 
            game->invertedPriorities = !game->invertedPriorities; 
            break;
        case 'B': 
            game->breakerMode = !game->breakerMode; 
            break;
        case 'X':
            if (game->breakerMode) {
                game->grid[game->pos.y][game->pos.x] = ' ';
            }
            break;
        case 'T':
            if (game->teleCount == 2) {
                Point temp = game->pos;
                if (temp.x == game->teleporters[0].x && temp.y == game->teleporters[0].y) {
                    game->pos = game->teleporters[1];
                } else {
                    game->pos = game->teleporters[0];
                }
            }
            break;
    }
}

bool runSimulation(Game* game) {
    int loopPrevention = 0;
    while (loopPrevention++ < MAX_MOVES) {
        if (!canMove(game, game->dir)) {
            int newDir = getNextDir(game);
            if (newDir == game->dir) {
                return false;
            }
            game->dir = newDir;
        }

        if (isLoop(game)) {
            return false;
        }

        markVisited(game);

        strcpy(game->moves[game->moveCount++], directions[game->dir]);
        game->pos.x += dx[game->dir];
        game->pos.y += dy[game->dir];

        if (game->grid[game->pos.y][game->pos.x] == '$') {
            return true;
        }

        handleCell(game);
    }

    return false;
}

int main() {
    Game game;
    initGame(&game);

    if (scanf("%d %d", &game.L, &game.C) != 2) {
        return 1;
    }
    
    if (getchar() != '\n') {
        getchar();
    }

    for (int i = 0; i < game.L; i++) {
        if (fgets(game.grid[i], MAX_SIZE+1, stdin) == NULL) {
            continue;
        }
        
        int len = strlen(game.grid[i]);
        if (len > 0 && game.grid[i][len-1] == '\n') {
            game.grid[i][len-1] = '\0';
        }

        for (int j = 0; j < game.C; j++) {
            if (j >= strlen(game.grid[i])) {
                break;
            }
            
            if (game.grid[i][j] == '@') {
                game.pos.x = j;
                game.pos.y = i;
                game.grid[i][j] = ' ';
            } else if (game.grid[i][j] == 'T') {
                if (game.teleCount < 2) {
                    game.teleporters[game.teleCount].x = j;
                    game.teleporters[game.teleCount].y = i;
                    game.teleCount++;
                }
            }
        }
    }

    bool success = runSimulation(&game);

    if (success) {
        for (int i = 0; i < game.moveCount; i++) {
            printf("%s\n", game.moves[i]);
        }
    } else {
        printf("LOOP\n");
    }

    return 0;
}
