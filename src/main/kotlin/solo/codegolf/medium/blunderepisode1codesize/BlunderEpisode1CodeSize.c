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
        // WEST, NORTH, EAST, SOUTH 우선순위
        int priorities[] = {3, 2, 1, 0};
        for (int i = 0; i < 4; i++) {
            if (canMove(game, priorities[i])) {
                return priorities[i];
            }
        }
    } else {
        // SOUTH, EAST, NORTH, WEST 우선순위
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
    return game->visited[game->pos.y][game->pos.x][game->dir][game->breakerMode][game->invertedPriorities];
}

void markVisited(Game* game) {
    game->visited[game->pos.y][game->pos.x][game->dir][game->breakerMode][game->invertedPriorities] = true;
}

void handleCell(Game* game) {
    char cell = game->grid[game->pos.y][game->pos.x];
    
    // 방향 지시자 처리
    if (cell == 'S') game->dir = 0;
    else if (cell == 'E') game->dir = 1;
    else if (cell == 'N') game->dir = 2;
    else if (cell == 'W') game->dir = 3;

    // 다른 특수 기능 처리
    if (cell == 'I') {
        game->invertedPriorities = !game->invertedPriorities;
    } 
    else if (cell == 'B') {
        game->breakerMode = !game->breakerMode;
    } 
    else if (cell == 'X' && game->breakerMode) {
        game->grid[game->pos.y][game->pos.x] = ' ';
    } 
    else if (cell == 'T' && game->teleCount == 2) {
        // 텔레포터는 항상 2개만 있다고 가정
        if (game->pos.x == game->teleporters[0].x && game->pos.y == game->teleporters[0].y) {
            game->pos = game->teleporters[1];
        } else {
            game->pos = game->teleporters[0];
        }
    }
}

bool runSimulation(Game* game) {
    while (game->moveCount < MAX_MOVES) {
        // 현재 방향으로 이동할 수 없으면 다음 우선순위 방향 선택
        if (!canMove(game, game->dir)) {
            game->dir = getNextDir(game);
        }

        // 루프 검출
        if (isLoop(game)) {
            return false;
        }

        markVisited(game);
        strcpy(game->moves[game->moveCount++], directions[game->dir]);
        
        // 이동
        game->pos.x += dx[game->dir];
        game->pos.y += dy[game->dir];

        // 종료 지점 도달 체크
        if (game->grid[game->pos.y][game->pos.x] == '$') {
            return true;
        }

        // 현재 위치의 셀 처리
        handleCell(game);
    }

    return false; // MAX_MOVES에 도달하면 무한루프로 간주
}

int main()
{
    Game game;
    initGame(&game);
    
    scanf("%d%d", &game.L, &game.C);
    fgetc(stdin);
    
    // 맵 파싱
    for (int i = 0; i < game.L; i++) {
        scanf("%[^\n]", game.grid[i]);
        fgetc(stdin);
        
        // 시작점, 텔레포터 찾기
        for (int j = 0; j < game.C; j++) {
            if (game.grid[i][j] == '@') {
                game.pos.x = j;
                game.pos.y = i;
                game.grid[i][j] = ' '; // 시작 위치를 빈 공간으로 변경
            }
            else if (game.grid[i][j] == 'T') {
                if (game.teleCount < 2) {
                    game.teleporters[game.teleCount].x = j;
                    game.teleporters[game.teleCount].y = i;
                    game.teleCount++;
                }
            }
        }
    }
    
    // 시뮬레이션 실행
    bool success = runSimulation(&game);
    
    // 결과 출력
    if (success) {
        for (int i = 0; i < game.moveCount; i++) {
            printf("%s\n", game.moves[i]);
        }
    } else {
        printf("LOOP\n");
    }

    return 0;
}
