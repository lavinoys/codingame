#include <stdlib.h>
#include <stdio.h>
#include <string.h>
#include <stdbool.h>

// 미로의 최대 크기 및 기타 상수 정의
#define MAX_SIZE 25
#define MAX_SWITCHES 15
#define MAX_QUEUE_SIZE 1000000  // 큐 크기 증가
#define DIRECTIONS 4
#define MAX_PATH_LENGTH 10000

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

// BFS 큐에 사용할 상태 정의 - 최적화
typedef struct {
    int x, y;           // 현재 위치
    int switchBitmask;  // 스위치 상태를 비트마스크로 저장
    int parent;         // 이전 상태 인덱스
    char move;          // 이전 상태에서 현재 상태로 이동한 방향
} QueueState;

// 미로 및 전역 변수
char maze[MAX_SIZE][MAX_SIZE];
Switch switches[MAX_SWITCHES];
int width, height, switchCount;
int startX, startY, targetX, targetY;
QueueState queue[MAX_QUEUE_SIZE];
bool visited[MAX_SIZE][MAX_SIZE][1 << MAX_SWITCHES]; // 방문 상태

// 맨해튼 거리 계산 (A* 알고리즘용)
int manhattanDistance(int x1, int y1, int x2, int y2) {
    return abs(x1 - x2) + abs(y1 - y2);
}

// 스위치 상태 업데이트 - 비트마스크로 바로 작업
int updateSwitchState(int bitmask, int switchIndex) {
    return bitmask ^ (1 << switchIndex);
}

// 자기장 필드 확인
bool isMagneticField(int x, int y, int switchBitmask) {
    for (int i = 0; i < switchCount; i++) {
        if ((switchBitmask & (1 << i)) && switches[i].blockX == x && switches[i].blockY == y) {
            return true;
        }
    }
    return false;
}

// 유효한 이동인지 확인
bool isValidMove(int x, int y, int switchBitmask) {
    // 미로 경계 확인
    if (x < 0 || x >= width || y < 0 || y >= height) {
        return false;
    }
    
    // 벽 확인
    if (maze[y][x] == WALL) {
        return false;
    }
    
    // 쓰레기 볼 확인 (문제 명시: 쓰레기 볼을 움직이지 않고도 해결 가능)
    if (maze[y][x] == GARBAGE) {
        return false;
    }
    
    // 자기장 필드 확인
    if (isMagneticField(x, y, switchBitmask)) {
        return false;
    }
    
    return true;
}

// 경로 역추적
char* reconstructPath(int targetIndex) {
    static char path[MAX_PATH_LENGTH];
    int pathLen = 0;
    int currentIndex = targetIndex;
    
    // 경로를 역으로 추적
    while (currentIndex > 0) {  // 0은 시작 상태
        path[pathLen++] = queue[currentIndex].move;
        currentIndex = queue[currentIndex].parent;
    }
    
    // 경로 뒤집기
    for (int i = 0; i < pathLen / 2; i++) {
        char temp = path[i];
        path[i] = path[pathLen - i - 1];
        path[pathLen - i - 1] = temp;
    }
    
    path[pathLen] = '\0';
    return path;
}

// A* 알고리즘으로 최단 경로 찾기
char* findPath() {
    int front = 0, rear = 0;
    
    // 초기 상태 설정
    QueueState initialState;
    initialState.x = startX;
    initialState.y = startY;
    initialState.parent = -1;  // 시작점은 부모가 없음
    initialState.move = '\0';  // 시작점 이동 명령 없음
    
    // 초기 스위치 상태 설정
    initialState.switchBitmask = 0;
    for (int i = 0; i < switchCount; i++) {
        if (switches[i].isOn) {
            initialState.switchBitmask |= (1 << i);
        }
    }
    
    queue[rear++] = initialState;
    memset(visited, false, sizeof(visited));
    visited[startY][startX][initialState.switchBitmask] = true;
    
    while (front < rear) {
        // 현재 상태는 큐의 맨 앞
        QueueState current = queue[front++];
        
        // 목표 도달 확인
        if (current.x == targetX && current.y == targetY) {
            return reconstructPath(front - 1);  // 현재 상태의 인덱스
        }
        
        // 4방향 탐색
        for (int dir = 0; dir < DIRECTIONS; dir++) {
            int nx = current.x + dx[dir];
            int ny = current.y + dy[dir];
            
            // 유효한 이동인지 확인
            if (!isValidMove(nx, ny, current.switchBitmask)) {
                continue;
            }
            
            // 새 상태 생성
            QueueState newState = current;
            newState.x = nx;
            newState.y = ny;
            newState.parent = front - 1;  // 현재 상태가 새 상태의 부모
            newState.move = dir_chars[dir];  // 이동 방향 저장
            
            // 스위치 토글 확인
            int updatedBitmask = current.switchBitmask;
            for (int i = 0; i < switchCount; i++) {
                if (nx == switches[i].x && ny == switches[i].y) {
                    updatedBitmask = updateSwitchState(updatedBitmask, i);
                }
            }
            newState.switchBitmask = updatedBitmask;
            
            // 방문 체크
            if (!visited[ny][nx][updatedBitmask]) {
                visited[ny][nx][updatedBitmask] = true;
                queue[rear++] = newState;
                
                if (rear >= MAX_QUEUE_SIZE) {
                    fprintf(stderr, "큐 오버플로우 발생! 큐 크기 증가 필요\n");
                    exit(1);
                }
            }
        }
    }
    
    return NULL; // 경로를 찾지 못함
}

// 경로 압축 - 반복 패턴을 찾아 함수로 대체
char* compressPath(char* path) {
    // 이 부분은 사실 매우 복잡한 알고리즘이 필요
    // 현재는 경로 압축 없이 원본 경로 반환
    return path;
}

int main() {
    // 미로 크기 입력
    scanf("%d%d", &width, &height); fgetc(stdin);
    
    // 미로 입력
    for (int i = 0; i < height; i++) {
        scanf("%[^\n]", maze[i]); fgetc(stdin);
        fprintf(stderr, "Line %d: %s\n", i, maze[i]);
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
    
    // 경로 압축 - 이 프로젝트에서는 아직 구현하지 않음
    // char* compressedPath = compressPath(path);
    // printf("%s\n", compressedPath);
    
    printf("%s\n", path);
    
    return 0;
}
