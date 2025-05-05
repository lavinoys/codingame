#include <stdlib.h>
#include <stdio.h>
#include <string.h>
#include <stdbool.h>

// 미로의 최대 크기 및 기타 상수 정의
#define MAX_SIZE 25
#define MAX_SWITCHES 15
#define MAX_QUEUE_SIZE 100000
#define DIRECTIONS 4

// 미로 요소 정의
#define EMPTY '.'
#define WALL '#'
#define GARBAGE '+'

// 방향 정의
const int dx[4] = {0, 0, -1, 1};  // 상, 하, 좌, 우
const int dy[4] = {-1, 1, 0, 0};
const char dir_chars[4] = {'U', 'D', 'L', 'R'};

// 스위치 정보
typedef struct {
    int x, y;           // 스위치 위치
    int blockX, blockY; // 자기장 위치
    bool isOn;          // 자기장 활성화 상태
} Switch;

// BFS 큐에 사용할 상태 정의
typedef struct {
    int x, y;           // 현재 위치
    int steps;          // 이동 단계 수
    char path[5000];    // 현재까지의 경로
    bool switchStates[MAX_SWITCHES]; // 각 스위치의 상태
} State;

// 미로 및 전역 변수
char maze[MAX_SIZE][MAX_SIZE];
Switch switches[MAX_SWITCHES];
int width, height, switchCount;
int startX, startY, targetX, targetY;
State queue[MAX_QUEUE_SIZE];
bool visited[MAX_SIZE][MAX_SIZE][1 << MAX_SWITCHES]; // 방문 상태 (위치 + 스위치 상태 조합)

// 스위치 상태를 비트마스크로 변환
int switchStatesToBitmask(bool switchStates[]) {
    int bitmask = 0;
    for (int i = 0; i < switchCount; i++) {
        if (switchStates[i]) {
            bitmask |= (1 << i);
        }
    }
    return bitmask;
}

// 유효한 이동인지 확인
bool isValidMove(int x, int y, bool switchStates[]) {
    // 미로 경계 확인
    if (x < 0 || x >= width || y < 0 || y >= height) {
        return false;
    }
    
    // 벽 확인
    if (maze[y][x] == WALL) {
        return false;
    }
    
    // 쓰레기 볼 확인 (현재는 움직일 수 없는 장애물로 처리)
    if (maze[y][x] == GARBAGE) {
        return false;
    }
    
    // 자기장 확인
    for (int i = 0; i < switchCount; i++) {
        if (switchStates[i] && switches[i].blockX == x && switches[i].blockY == y) {
            return false;
        }
    }
    
    return true;
}

// BFS로 최단 경로 찾기
char* findPath() {
    int front = 0, rear = 0;
    static char finalPath[5000];
    
    // 초기 상태 설정
    State initialState;
    initialState.x = startX;
    initialState.y = startY;
    initialState.steps = 0;
    initialState.path[0] = '\0';
    
    for (int i = 0; i < switchCount; i++) {
        initialState.switchStates[i] = switches[i].isOn;
    }
    
    queue[rear++] = initialState;
    memset(visited, false, sizeof(visited));
    
    int bitmask = switchStatesToBitmask(initialState.switchStates);
    visited[startY][startX][bitmask] = true;
    
    while (front < rear) {
        State current = queue[front++];
        
        // 목표 도달 확인
        if (current.x == targetX && current.y == targetY) {
            strcpy(finalPath, current.path);
            return finalPath;
        }
        
        // 4방향 탐색
        for (int dir = 0; dir < DIRECTIONS; dir++) {
            int nx = current.x + dx[dir];
            int ny = current.y + dy[dir];
            
            // 유효한 이동인지 확인
            if (!isValidMove(nx, ny, current.switchStates)) {
                continue;
            }
            
            // 새 상태 생성
            State newState = current;
            newState.x = nx;
            newState.y = ny;
            newState.steps = current.steps + 1;
            
            // 경로에 이동 추가
            newState.path[current.steps] = dir_chars[dir];
            newState.path[current.steps + 1] = '\0';
            
            // 스위치 토글 확인
            for (int i = 0; i < switchCount; i++) {
                if (nx == switches[i].x && ny == switches[i].y) {
                    newState.switchStates[i] = !newState.switchStates[i];
                }
            }
            
            // 방문 체크
            int newBitmask = switchStatesToBitmask(newState.switchStates);
            if (!visited[ny][nx][newBitmask]) {
                visited[ny][nx][newBitmask] = true;
                queue[rear++] = newState;
                
                if (rear >= MAX_QUEUE_SIZE) {
                    fprintf(stderr, "큐 오버플로우\n");
                    exit(1);
                }
            }
        }
    }
    
    return NULL; // 경로를 찾지 못함
}

int main() {
    // 미로 크기 입력
    scanf("%d%d", &width, &height); fgetc(stdin);
    
    // 미로 입력
    for (int i = 0; i < height; i++) {
        scanf("%[^\n]", maze[i]); fgetc(stdin);
        fprintf(stderr, "Line %d: %s\n", i, maze[i]); // 디버깅용
    }
    
    // 시작 위치와 목표 위치 입력
    scanf("%d%d", &startX, &startY);
    scanf("%d%d", &targetX, &targetY);
    fprintf(stderr, "Start: (%d, %d), Target: (%d, %d)\n", startX, startY, targetX, targetY);
    
    // 스위치 정보 입력
    scanf("%d", &switchCount);
    for (int i = 0; i < switchCount; i++) {
        int switchX, switchY, blockX, blockY, initialState;
        scanf("%d%d%d%d%d", &switchX, &switchY, &blockX, &blockY, &initialState);
        
        switches[i].x = switchX;
        switches[i].y = switchY;
        switches[i].blockX = blockX;
        switches[i].blockY = blockY;
        switches[i].isOn = (initialState == 1);
        
        fprintf(stderr, "Switch %d: (%d, %d) controls (%d, %d), initial state: %d\n", 
                i, switchX, switchY, blockX, blockY, initialState);
    }
    
    // 경로 찾기
    char* path = findPath();
    
    if (path == NULL) {
        fprintf(stderr, "경로를 찾을 수 없습니다.\n");
        printf("경로를 찾을 수 없습니다.\n");
        return 1;
    }
    
    // 결과 출력
    fprintf(stderr, "경로 길이: %ld\n", strlen(path));
    fprintf(stderr, "경로: %s\n", path);
    printf("%s\n", path);
    
    return 0;
}
