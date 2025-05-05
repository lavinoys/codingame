#include <stdlib.h>
#include <stdio.h>
#include <string.h>
#include <stdbool.h>

#define MAX_SIZE 100
#define MAX_MOVES 1000000
#define DEBUG_MODE 1  // 1: 디버깅 활성화, 0: 비활성화

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

void debugPrintGrid(Game* game) {
    if (!DEBUG_MODE) return;
    fprintf(stderr, "\n현재 그리드 상태:\n");
    for (int i = 0; i < game->L; i++) {
        for (int j = 0; j < game->C; j++) {
            if (i == game->pos.y && j == game->pos.x)
                fprintf(stderr, "@");  // 현재 블런더 위치
            else
                fprintf(stderr, "%c", game->grid[i][j]);
        }
        fprintf(stderr, "\n");
    }
    fprintf(stderr, "\n");
}

void initGame(Game* game) {
    game->teleCount = 0;
    game->dir = 0;  // Start facing SOUTH
    game->breakerMode = false;
    game->invertedPriorities = false;
    game->moveCount = 0;
    memset(game->visited, 0, sizeof(game->visited));
    
    if (DEBUG_MODE) {
        fprintf(stderr, "게임 초기화 완료\n");
    }
}

bool canMove(Game* game, int dir) {
    int nx = game->pos.x + dx[dir];
    int ny = game->pos.y + dy[dir];
    char cell = game->grid[ny][nx];

    if (DEBUG_MODE) {
        fprintf(stderr, "이동 시도: %s (%d,%d) → (%d,%d), 셀: '%c' ", 
                directions[dir], game->pos.x, game->pos.y, nx, ny, cell);
    }

    if (cell == '#') {
        if (DEBUG_MODE) fprintf(stderr, "→ 벽 충돌\n");
        return false;
    }
    if (cell == 'X' && !game->breakerMode) {
        if (DEBUG_MODE) fprintf(stderr, "→ 장애물 충돌 (파괴 불가능)\n");
        return false;
    }
    if (DEBUG_MODE) fprintf(stderr, "→ 이동 가능\n");
    return true;
}

int getNextDir(Game* game) {
    if (DEBUG_MODE) {
        fprintf(stderr, "방향 재설정 (현재: %s, 파괴모드: %s, 우선순위반전: %s)\n", 
                directions[game->dir],
                game->breakerMode ? "활성" : "비활성", 
                game->invertedPriorities ? "활성" : "비활성");
    }

    if (game->invertedPriorities) {
        // WEST, NORTH, EAST, SOUTH
        int priorities[] = {3, 2, 1, 0};
        for (int i = 0; i < 4; i++) {
            if (DEBUG_MODE) {
                fprintf(stderr, "  우선순위 검사 %d: %s\n", i+1, directions[priorities[i]]);
            }
            if (canMove(game, priorities[i])) {
                if (DEBUG_MODE) {
                    fprintf(stderr, "  새 방향 선택: %s\n", directions[priorities[i]]);
                }
                return priorities[i];
            }
        }
    } else {
        // SOUTH, EAST, NORTH, WEST
        int priorities[] = {0, 1, 2, 3};
        for (int i = 0; i < 4; i++) {
            if (DEBUG_MODE) {
                fprintf(stderr, "  우선순위 검사 %d: %s\n", i+1, directions[priorities[i]]);
            }
            if (canMove(game, priorities[i])) {
                if (DEBUG_MODE) {
                    fprintf(stderr, "  새 방향 선택: %s\n", directions[priorities[i]]);
                }
                return priorities[i];
            }
        }
    }
    return game->dir;  // Shouldn't happen with valid maps
}

void handleCell(Game* game) {
    char cell = game->grid[game->pos.y][game->pos.x];

    if (DEBUG_MODE) {
        fprintf(stderr, "현재 위치 (%d,%d)의 셀: '%c' 처리\n", 
                game->pos.x, game->pos.y, cell);
    }

    switch(cell) {
        case 'S': 
            game->dir = 0; 
            if (DEBUG_MODE) fprintf(stderr, "  방향 변경: SOUTH\n");
            break;
        case 'E': 
            game->dir = 1; 
            if (DEBUG_MODE) fprintf(stderr, "  방향 변경: EAST\n");
            break;
        case 'N': 
            game->dir = 2; 
            if (DEBUG_MODE) fprintf(stderr, "  방향 변경: NORTH\n");
            break;
        case 'W': 
            game->dir = 3; 
            if (DEBUG_MODE) fprintf(stderr, "  방향 변경: WEST\n");
            break;
        case 'I': 
            game->invertedPriorities = !game->invertedPriorities; 
            if (DEBUG_MODE) fprintf(stderr, "  우선순위 반전: %s\n", 
                                  game->invertedPriorities ? "활성화" : "비활성화");
            break;
        case 'B': 
            game->breakerMode = !game->breakerMode; 
            if (DEBUG_MODE) fprintf(stderr, "  파괴 모드: %s\n", 
                                  game->breakerMode ? "활성화" : "비활성화");
            break;
        case 'X':
            if (game->breakerMode) {
                game->grid[game->pos.y][game->pos.x] = ' ';  // Destroy X
                if (DEBUG_MODE) fprintf(stderr, "  장애물 X 파괴!\n");
            }
            break;
        case 'T':
            // Teleport to the other T
            for (int i = 0; i < game->teleCount; i++) {
                if (game->teleporters[i].x != game->pos.x || game->teleporters[i].y != game->pos.y) {
                    int oldX = game->pos.x, oldY = game->pos.y;
                    game->pos = game->teleporters[i];
                    if (DEBUG_MODE) fprintf(stderr, "  텔레포트: (%d,%d) → (%d,%d)\n", 
                                          oldX, oldY, game->pos.x, game->pos.y);
                    break;
                }
            }
            break;
    }
}

bool isLoop(Game* game) {
    bool result = game->visited[game->pos.y][game->pos.x][game->dir][game->breakerMode][game->invertedPriorities];
    if (DEBUG_MODE && result) {
        fprintf(stderr, "루프 감지! 위치 (%d,%d), 방향: %s, 파괴모드: %s, 우선순위반전: %s\n",
                game->pos.x, game->pos.y, directions[game->dir],
                game->breakerMode ? "활성" : "비활성",
                game->invertedPriorities ? "활성" : "비활성");
    }
    return result;
}

void markVisited(Game* game) {
    game->visited[game->pos.y][game->pos.x][game->dir][game->breakerMode][game->invertedPriorities] = true;
    if (DEBUG_MODE) {
        fprintf(stderr, "방문 기록: 위치 (%d,%d), 방향: %s, 파괴모드: %s, 우선순위반전: %s\n",
                game->pos.x, game->pos.y, directions[game->dir],
                game->breakerMode ? "활성" : "비활성",
                game->invertedPriorities ? "활성" : "비활성");
    }
}

bool runSimulation(Game* game) {
    if (DEBUG_MODE) {
        fprintf(stderr, "시뮬레이션 시작\n");
        debugPrintGrid(game);
    }

    while (true) {
        // 현재 상태 기록 (이동 전)
        markVisited(game);

        // 현재 방향으로 이동 시도
        if (canMove(game, game->dir)) {
            // 이동 기록
            strcpy(game->moves[game->moveCount++], directions[game->dir]);
            if (DEBUG_MODE) {
                fprintf(stderr, "이동 #%d: %s\n", game->moveCount, directions[game->dir]);
            }

            // 이동
            game->pos.x += dx[game->dir];
            game->pos.y += dy[game->dir];

            if (DEBUG_MODE) {
                fprintf(stderr, "새 위치: (%d,%d)\n", game->pos.x, game->pos.y);
            }

            // 종료 지점 확인
            if (game->grid[game->pos.y][game->pos.x] == '$') {
                if (DEBUG_MODE) {
                    fprintf(stderr, "종료 지점($) 도달! 이동 횟수: %d\n", game->moveCount);
                }
                return true;
            }

            // 특수 셀 효과 처리
            handleCell(game);

            // 루프 확인 - 이동 후에 체크
            if (isLoop(game)) {
                return false;
            }

            if (DEBUG_MODE) debugPrintGrid(game);
        } else {
            // 우선순위에 따른 새 방향 찾기
            int newDir = getNextDir(game);

            // 방향이 변경되면 루프 체크 필요
            if (newDir != game->dir) {
                game->dir = newDir;
                if (DEBUG_MODE) {
                    fprintf(stderr, "방향 변경: %s\n", directions[game->dir]);
                }

                // 방향 변경 후 방문 기록 및 루프 확인
                if (isLoop(game)) {
                    return false;
                }
                markVisited(game);
            } else {
                // 유효한 방향을 찾지 못했을 때 (맵 설계 오류)
                if (DEBUG_MODE) {
                    fprintf(stderr, "오류: 이동 가능한 방향이 없음!\n");
                }
                return false;
            }
        }

        if (game->moveCount >= MAX_MOVES) {
            // 무한 루프 방지 안전장치
            if (DEBUG_MODE) {
                fprintf(stderr, "최대 이동 횟수(%d)에 도달! 루프로 간주\n", MAX_MOVES);
            }
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

    if (DEBUG_MODE) {
        fprintf(stderr, "맵 크기: %d x %d\n", game.L, game.C);
    }

    for (int i = 0; i < game.L; i++) {
        scanf("%[^\n]", game.grid[i]);
        fgetc(stdin);

        if (DEBUG_MODE) {
            fprintf(stderr, "행 %d: %s\n", i, game.grid[i]);
        }

        // Find start position and teleporters
        for (int j = 0; j < game.C; j++) {
            if (game.grid[i][j] == '@') {
                game.pos.x = j;
                game.pos.y = i;
                game.grid[i][j] = ' ';  // Replace start with empty space
                if (DEBUG_MODE) {
                    fprintf(stderr, "시작 위치: (%d,%d)\n", j, i);
                }
            } else if (game.grid[i][j] == 'T') {
                game.teleporters[game.teleCount].x = j;
                game.teleporters[game.teleCount].y = i;
                if (DEBUG_MODE) {
                    fprintf(stderr, "텔레포터 #%d: (%d,%d)\n", game.teleCount+1, j, i);
                }
                game.teleCount++;
            }
        }
    }

    if (DEBUG_MODE) {
        fprintf(stderr, "맵 로딩 완료, 텔레포터 수: %d\n", game.teleCount);
    }

    // Run the simulation
    bool success = runSimulation(&game);

    // Output results
    if (success) {
        if (DEBUG_MODE) {
            fprintf(stderr, "시뮬레이션 성공! 결과 출력:\n");
        }
        for (int i = 0; i < game.moveCount; i++) {
            printf("%s\n", game.moves[i]);
            if (DEBUG_MODE) {
                fprintf(stderr, "출력 #%d: %s\n", i+1, game.moves[i]);
            }
        }
    } else {
        if (DEBUG_MODE) {
            fprintf(stderr, "시뮬레이션 실패 - 루프 감지!\n");
        }
        printf("LOOP\n");
    }

    return 0;
}
