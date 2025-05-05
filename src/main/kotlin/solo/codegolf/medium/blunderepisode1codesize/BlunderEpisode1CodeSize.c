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
    fprintf(stderr, "\n현재 그리드 상태 (L=%d, C=%d):\n", game->L, game->C);
    for (int i = 0; i < game->L; i++) {
        fprintf(stderr, "%2d: ", i);  // 행 번호 표시 (2자리)
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
    
    // 경계 체크 추가
    if (nx < 0 || nx >= game->C || ny < 0 || ny >= game->L) {
        if (DEBUG_MODE) fprintf(stderr, "이동 시도: %s (%d,%d) → (%d,%d) → 맵 경계 초과!\n", 
                directions[dir], game->pos.x, game->pos.y, nx, ny);
        return false;
    }
    
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
        fprintf(stderr, "처리 전 상태 - 방향: %s, 파괴모드: %s, 우선순위반전: %s\n",
                directions[game->dir],
                game->breakerMode ? "활성" : "비활성",
                game->invertedPriorities ? "활성" : "비활성");
    }

    // 방향 변경 우선 처리
    if (cell == 'S' || cell == 'E' || cell == 'N' || cell == 'W') {
        int prevDir = game->dir;
        switch(cell) {
            case 'S': game->dir = 0; break;
            case 'E': game->dir = 1; break;
            case 'N': game->dir = 2; break;
            case 'W': game->dir = 3; break;
        }
        if (DEBUG_MODE && prevDir != game->dir) {
            fprintf(stderr, "  강제 방향 변경: %s → %s\n", 
                    directions[prevDir], directions[game->dir]);
        }
        return; // 방향 변경 후 다른 효과 무시
    }

    // 다른 특수 효과 처리
    switch(cell) {
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
                game->grid[game->pos.y][game->pos.x] = ' ';
                if (DEBUG_MODE) fprintf(stderr, "  장애물 X 파괴!\n");
            }
            break;
        case 'T':
            for (int i = 0; i < game->teleCount; i++) {
                if (game->teleporters[i].x != game->pos.x || 
                    game->teleporters[i].y != game->pos.y) {
                    Point oldPos = game->pos;
                    game->pos = game->teleporters[i];
                    if (DEBUG_MODE) fprintf(stderr, "  텔레포트: (%d,%d) → (%d,%d)\n", 
                                          oldPos.x, oldPos.y, game->pos.x, game->pos.y);
                    break;
                }
            }
            break;
    }

    if (DEBUG_MODE) {
        fprintf(stderr, "처리 후 상태 - 방향: %s, 파괴모드: %s, 우선순위반전: %s\n",
                directions[game->dir],
                game->breakerMode ? "활성" : "비활성",
                game->invertedPriorities ? "활성" : "비활성");
    }
}

bool isLoop(Game* game) {
    bool result = game->visited[game->pos.y][game->pos.x][game->dir][game->breakerMode][game->invertedPriorities];
    if (DEBUG_MODE && result) {
        fprintf(stderr, "루프 감지! 위치 (%d,%d), 방향: %s, 파괴모드: %s, 우선순위반전: %s\n",
                game->pos.x, game->pos.y, directions[game->dir],
                game->breakerMode ? "활성" : "비활성",
                game->invertedPriorities ? "활성" : "비활성");
        
        // 루프 감지 시 추가 정보 출력
        fprintf(stderr, "이전에 같은 상태로 방문한 적 있음 (루프 발생)\n");
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

    int loopPrevention = 0;
    while (loopPrevention++ < MAX_MOVES) {
        // 현재 상태에서 이동 가능 체크
        if (!canMove(game, game->dir)) {
            int newDir = getNextDir(game);
            if (newDir == game->dir) {
                if (DEBUG_MODE) fprintf(stderr, "이동 불가능한 상태!\n");
                return false;
            }
            game->dir = newDir;
        }

        // 현재 상태가 이미 방문했던 상태인지 체크
        if (isLoop(game)) {
            if (DEBUG_MODE) {
                fprintf(stderr, "루프 상태 감지! (%d,%d) 방향:%s\n", 
                        game->pos.x, game->pos.y, directions[game->dir]);
                fprintf(stderr, "파괴모드:%s 우선순위반전:%s\n",
                        game->breakerMode ? "활성" : "비활성",
                        game->invertedPriorities ? "활성" : "비활성");
            }
            return false;
        }

        // 현재 상태 방문 기록
        markVisited(game);

        // 이동 실행
        strcpy(game->moves[game->moveCount++], directions[game->dir]);
        game->pos.x += dx[game->dir];
        game->pos.y += dy[game->dir];

        if (DEBUG_MODE) {
            fprintf(stderr, "\n이동 #%d: %s → (%d,%d)\n", 
                    game->moveCount, directions[game->dir], game->pos.x, game->pos.y);
        }

        // 목표 도달 체크
        if (game->grid[game->pos.y][game->pos.x] == '$') {
            if (DEBUG_MODE) fprintf(stderr, "목표 도달! 이동 횟수: %d\n", game->moveCount);
            return true;
        }

        // 특수 셀 효과 처리
        handleCell(game);

        if (DEBUG_MODE) debugPrintGrid(game);
    }

    if (DEBUG_MODE) fprintf(stderr, "최대 이동 횟수 초과!\n");
    return false;
}

int main() {
    Game game;
    initGame(&game);

    // Read the map
    if (scanf("%d %d", &game.L, &game.C) != 2) {
        if (DEBUG_MODE) fprintf(stderr, "맵 크기 읽기 오류!\n");
        return 1;
    }
    
    if (getchar() != '\n') {
        // 줄바꿈 문자가 아니면 하나 더 읽기
        getchar();
    }

    if (DEBUG_MODE) {
        fprintf(stderr, "맵 크기: %d행 x %d열\n", game.L, game.C);
    }

    for (int i = 0; i < game.L; i++) {
        if (fgets(game.grid[i], MAX_SIZE+1, stdin) == NULL) {
            if (DEBUG_MODE) fprintf(stderr, "행 %d 읽기 오류!\n", i);
            continue;
        }
        
        // 개행문자 제거
        int len = strlen(game.grid[i]);
        if (len > 0 && game.grid[i][len-1] == '\n') {
            game.grid[i][len-1] = '\0';
        }

        if (DEBUG_MODE) {
            fprintf(stderr, "행 %2d: %s (길이: %d)\n", i, game.grid[i], (int)strlen(game.grid[i]));
        }

        // Find start position and teleporters
        for (int j = 0; j < game.C; j++) {
            if (j >= strlen(game.grid[i])) {
                if (DEBUG_MODE) fprintf(stderr, "경고: 행 %d, 열 %d 맵 데이터 부족\n", i, j);
                break;
            }
            
            if (game.grid[i][j] == '@') {
                game.pos.x = j;
                game.pos.y = i;
                game.grid[i][j] = ' ';  // Replace start with empty space
                if (DEBUG_MODE) {
                    fprintf(stderr, "시작 위치: (%d,%d)\n", j, i);
                }
            } else if (game.grid[i][j] == 'T') {
                if (game.teleCount < 2) {
                    game.teleporters[game.teleCount].x = j;
                    game.teleporters[game.teleCount].y = i;
                    if (DEBUG_MODE) {
                        fprintf(stderr, "텔레포터 #%d: (%d,%d)\n", game.teleCount+1, j, i);
                    }
                    game.teleCount++;
                } else {
                    if (DEBUG_MODE) {
                        fprintf(stderr, "경고: 텔레포터 초과 발견 (%d,%d)\n", j, i);
                    }
                }
            }
        }
    }

    if (DEBUG_MODE) {
        fprintf(stderr, "맵 로딩 완료, 텔레포터 수: %d\n", game.teleCount);
        debugPrintGrid(&game);
    }

    // Run the simulation
    bool success = runSimulation(&game);

    // Output results
    if (success) {
        if (DEBUG_MODE) {
            fprintf(stderr, "시뮬레이션 성공! 결과 출력 (이동 %d회):\n", game.moveCount);
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
