#include <stdlib.h>
#include <stdio.h>
#include <string.h>
#include <stdbool.h>

#define ROWS 15
#define COLS 15
#define EMPTY -1

// 방향 벡터 (상, 우, 하, 좌)
const int dr[] = {1, 0, -1, 0};
const int dc[] = {0, 1, 0, -1};

int board[ROWS][COLS];
int tempBoard[ROWS][COLS];

// 게임판 디버깅용 출력 함수
void printBoard() {
    fprintf(stderr, "Board state:\n");
    for (int r = ROWS - 1; r >= 0; r--) {
        for (int c = 0; c < COLS; c++) {
            fprintf(stderr, "%2d ", board[r][c]);
        }
        fprintf(stderr, "\n");
    }
}

// Flood Fill로 연결된 셀 수 계산
int countConnectedCells(int row, int col, int color, bool visited[ROWS][COLS]) {
    if (row < 0 || row >= ROWS || col < 0 || col >= COLS || 
        visited[row][col] || board[row][col] != color || 
        board[row][col] == EMPTY) {
        return 0;
    }
    
    visited[row][col] = true;
    int count = 1;
    
    // 인접한 셀 세기
    for (int i = 0; i < 4; i++) {
        count += countConnectedCells(row + dr[i], col + dc[i], color, visited);
    }
    
    return count;
}

// 연결된 셀 제거
void removeConnectedCells(int row, int col, int color, bool visited[ROWS][COLS]) {
    if (row < 0 || row >= ROWS || col < 0 || col >= COLS || 
        visited[row][col] || board[row][col] != color || 
        board[row][col] == EMPTY) {
        return;
    }
    
    visited[row][col] = true;
    board[row][col] = EMPTY;
    
    // 인접한 셀 제거
    for (int i = 0; i < 4; i++) {
        removeConnectedCells(row + dr[i], col + dc[i], color, visited);
    }
}

// 중력 적용 - 셀이 아래로 떨어지도록 함
void applyGravity() {
    for (int col = 0; col < COLS; col++) {
        int writeRow = 0;
        for (int row = 0; row < ROWS; row++) {
            if (board[row][col] != EMPTY) {
                board[writeRow][col] = board[row][col];
                if (writeRow != row) {
                    board[row][col] = EMPTY;
                }
                writeRow++;
            }
        }
    }
}

// 빈 열 접기 - 오른쪽 열을 왼쪽으로 이동
void collapseEmptyColumns() {
    int writeCol = 0;
    for (int readCol = 0; readCol < COLS; readCol++) {
        // 열이 비어있는지 확인
        bool isEmpty = true;
        for (int row = 0; row < ROWS; row++) {
            if (board[row][readCol] != EMPTY) {
                isEmpty = false;
                break;
            }
        }
        
        if (!isEmpty) {
            // 빈 열이 아니면, 현재 쓰기 위치로 복사
            if (writeCol != readCol) {
                for (int row = 0; row < ROWS; row++) {
                    board[row][writeCol] = board[row][readCol];
                    board[row][readCol] = EMPTY;
                }
            }
            writeCol++;
        }
    }
}

// 이동 실행 및 점수 계산
int makeMove(int row, int col) {
    int color = board[row][col];
    if (color == EMPTY) {
        return -1;  // 잘못된 이동
    }
    
    bool visited[ROWS][COLS] = {false};
    int regionSize = countConnectedCells(row, col, color, visited);
    
    if (regionSize < 2) {
        return -1;  // 잘못된 이동
    }
    
    memset(visited, false, sizeof(visited));
    removeConnectedCells(row, col, color, visited);
    applyGravity();
    collapseEmptyColumns();
    
    // 점수 계산: (n-2)²
    int score = (regionSize - 2) * (regionSize - 2);
    
    // 보드 클리어 체크
    bool cleared = true;
    for (int r = 0; r < ROWS; r++) {
        for (int c = 0; c < COLS; c++) {
            if (board[r][c] != EMPTY) {
                cleared = false;
                break;
            }
        }
        if (!cleared) break;
    }
    
    if (cleared) {
        score += 1000;  // 보드 클리어 보너스
    }
    
    return score;
}

// 이동 구조체
typedef struct {
    int col;
    int row;
    int size;
    int score;
} Move;

// 가장 좋은 이동 찾기
Move findBestMove() {
    Move bestMove = {-1, -1, 0, -1};
    
    for (int row = 0; row < ROWS; row++) {
        for (int col = 0; col < COLS; col++) {
            if (board[row][col] != EMPTY) {
                bool visited[ROWS][COLS] = {false};
                int size = countConnectedCells(row, col, board[row][col], visited);
                
                if (size >= 2) {
                    // 보드 복사
                    memcpy(tempBoard, board, sizeof(board));
                    
                    // 이동 시도
                    int score = makeMove(row, col);
                    
                    // 보드 복원
                    memcpy(board, tempBoard, sizeof(board));
                    
                    if (score > bestMove.score) {
                        bestMove.row = row;
                        bestMove.col = col;
                        bestMove.size = size;
                        bestMove.score = score;
                    }
                }
            }
        }
    }
    
    return bestMove;
}

int main() {
    // 게임 루프
    while (1) {
        // 보드 읽기 (위에서 아래로 입력됨)
        for (int i = ROWS - 1; i >= 0; i--) {
            for (int j = 0; j < COLS; j++) {
                scanf("%d", &board[i][j]);
            }
        }
        
        // 최적의 이동 찾기
        Move bestMove = findBestMove();
        
        if (bestMove.col != -1 && bestMove.row != -1) {
            printf("%d %d Size: %d, Score: %d\n", 
                   bestMove.col, bestMove.row, bestMove.size, bestMove.score);
        } else {
            printf("0 0 No valid moves\n");
        }
    }
    
    return 0;
}
