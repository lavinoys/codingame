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
    char cell = game->grid[ny][nx];

    if (cell == '#') return false;
    if (cell == 'X' && !game->breakerMode) return false;
    return true;
}

int getNextDir(Game* game) {
    if (game->invertedPriorities) {
        // WEST, NORTH, EAST, SOUTH
        int priorities[] = {3, 2, 1, 0};
        for (int i = 0; i < 4; i++) {
            if (canMove(game, priorities[i])) {
                return priorities[i];
            }
        }
    } else {
        // SOUTH, EAST, NORTH, WEST
        int priorities[] = {0, 1, 2, 3};
        for (int i = 0; i < 4; i++) {
            if (canMove(game, priorities[i])) {
                return priorities[i];
            }
        }
    }
    return game->dir;  // Shouldn't happen with valid maps
}

void handleCell(Game* game) {
    char cell = game->grid[game->pos.y][game->pos.x];

    switch(cell) {
        case 'S': game->dir = 0; break;  // SOUTH
        case 'E': game->dir = 1; break;  // EAST
        case 'N': game->dir = 2; break;  // NORTH
        case 'W': game->dir = 3; break;  // WEST
        case 'I': game->invertedPriorities = !game->invertedPriorities; break;
        case 'B': game->breakerMode = !game->breakerMode; break;
        case 'X':
            if (game->breakerMode) {
                game->grid[game->pos.y][game->pos.x] = ' ';  // Destroy X
            }
            break;
        case 'T':
            // Teleport to the other T
            for (int i = 0; i < game->teleCount; i++) {
                if (game->teleporters[i].x != game->pos.x || game->teleporters[i].y != game->pos.y) {
                    game->pos = game->teleporters[i];
                    break;
                }
            }
            break;
    }
}

bool isLoop(Game* game) {
    return game->visited[game->pos.y][game->pos.x][game->dir][game->breakerMode][game->invertedPriorities];
}

void markVisited(Game* game) {
    game->visited[game->pos.y][game->pos.x][game->dir][game->breakerMode][game->invertedPriorities] = true;
}

bool runSimulation(Game* game) {
    while (true) {
        // Check for loop
        if (isLoop(game)) {
            return false;
        }
        markVisited(game);

        // Try to move in current direction
        if (canMove(game, game->dir)) {
            // Record move
            strcpy(game->moves[game->moveCount++], directions[game->dir]);

            // Move
            game->pos.x += dx[game->dir];
            game->pos.y += dy[game->dir];

            // Check for finish
            if (game->grid[game->pos.y][game->pos.x] == '$') {
                return true;
            }

            // Handle special cell effects
            handleCell(game);
        } else {
            // Find new direction using priorities
            game->dir = getNextDir(game);
        }

        if (game->moveCount >= MAX_MOVES) {
            // Safety check to avoid infinite loops
            return false;
        }
    }
}

int main() {
    Game game;
    initGame(&game);

    // Read the map
    scanf("%d %d", &game.L, &game.C);
    fgetc(stdin);  // consume newline

    for (int i = 0; i < game.L; i++) {
        scanf("%[^\n]", game.grid[i]);
        fgetc(stdin);

        // Find start position and teleporters
        for (int j = 0; j < game.C; j++) {
            if (game.grid[i][j] == '@') {
                game.pos.x = j;
                game.pos.y = i;
                game.grid[i][j] = ' ';  // Replace start with empty space
            } else if (game.grid[i][j] == 'T') {
                game.teleporters[game.teleCount].x = j;
                game.teleporters[game.teleCount].y = i;
                game.teleCount++;
            }
        }
    }

    // Run the simulation
    bool success = runSimulation(&game);

    // Output results
    if (success) {
        for (int i = 0; i < game.moveCount; i++) {
            printf("%s\n", game.moves[i]);
        }
    } else {
        printf("LOOP\n");
    }

    return 0;
}