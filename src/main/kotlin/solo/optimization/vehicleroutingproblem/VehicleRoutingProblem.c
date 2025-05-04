#include <stdlib.h>
#include <stdio.h>
#include <string.h>
#include <stdbool.h>
#include <math.h>
#include <limits.h>

#define MAX_CUSTOMERS 100
#define MAX_OUTPUT_SIZE 10000

// 고객 정보를 저장하는 구조체
typedef struct {
    int index;
    int x;
    int y;
    int demand;
    bool visited;
} Customer;

// 경로 정보를 저장하는 구조체
typedef struct {
    int customers[MAX_CUSTOMERS];
    int count;
    int total_demand;
} Route;

// 두 점 사이의 거리 제곱 계산 (sqrt 연산 제거로 최적화)
static int distance_squared(int x1, int y1, int x2, int y2) {
    int dx = x1 - x2;
    int dy = y1 - y2;
    return dx * dx + dy * dy;
}

/**
 * Challenge yourself with this classic NP-Hard optimization problem !
 **/

int main()
{
    // The number of customers
    int n;
    scanf("%d", &n);
    // The capacity of the vehicles
    int c;
    scanf("%d", &c);
    
    // 고객 정보 저장
    Customer customers[MAX_CUSTOMERS];
    
    // 정수형 거리 제곱 테이블 (sqrt 제거로 최적화)
    int dist_squared[MAX_CUSTOMERS][MAX_CUSTOMERS];
    
    // 미방문 고객 목록을 별도로 관리 (탐색 최적화)
    int unvisited[MAX_CUSTOMERS];
    int unvisited_count = 0;
    
    for (int i = 0; i < n; i++) {
        int index, x, y, demand;
        scanf("%d%d%d%d", &index, &x, &y, &demand);
        
        customers[index].index = index;
        customers[index].x = x;
        customers[index].y = y;
        customers[index].demand = demand;
        customers[index].visited = false;
        
        // 디포(0)가 아닌 경우만 미방문 목록에 추가
        if (index > 0) {
            unvisited[unvisited_count++] = index;
        }
    }
    
    // 거리 제곱 테이블 미리 계산 (sqrt 연산 없이)
    for (int i = 0; i < n; i++) {
        Customer* ci = &customers[i];
        for (int j = 0; j < n; j++) {
            dist_squared[i][j] = distance_squared(ci->x, ci->y, customers[j].x, customers[j].y);
        }
    }
    
    // 경로들을 저장할 배열
    Route routes[MAX_CUSTOMERS]; 
    int route_count = 0;
    
    // 경로 생성
    while (unvisited_count > 0) {
        Route* current_route = &routes[route_count];
        current_route->count = 0;
        current_route->total_demand = 0;
        
        // 현재 위치는 디포(0)
        int current_pos = 0;
        bool added_to_route = false;
        
        // 현재 경로에 고객 추가
        while (true) {
            int min_dist = INT_MAX;
            int next_idx = -1;
            int next_pos_in_unvisited = -1;
            
            // 방문하지 않은 고객들 중에서 가장 가까운 고객 찾기
            for (int i = 0; i < unvisited_count; i++) {
                int customer_idx = unvisited[i];
                if (current_route->total_demand + customers[customer_idx].demand <= c) {
                    int dist = dist_squared[current_pos][customer_idx];
                    if (dist < min_dist) {
                        min_dist = dist;
                        next_idx = customer_idx;
                        next_pos_in_unvisited = i;
                    }
                }
            }
            
            // 더 이상 추가할 수 없으면 종료
            if (next_idx == -1) break;
            
            // 고객 추가 및 방문 처리
            current_pos = next_idx;
            current_route->customers[current_route->count++] = next_idx;
            current_route->total_demand += customers[next_idx].demand;
            
            // 미방문 목록에서 제거 (스왑 후 크기 감소로 O(1) 시간에 제거)
            unvisited[next_pos_in_unvisited] = unvisited[--unvisited_count];
            added_to_route = true;
        }
        
        // 경로에 고객이 추가되었다면 경로 카운트 증가
        if (added_to_route) {
            route_count++;
        } else {
            // 용량 제한으로 추가할 수 없는 경우
            // 각 남은 고객을 개별 경로로 처리 (용량 제한 내에서)
            int i = 0;
            while (i < unvisited_count) {
                int idx = unvisited[i];
                if (customers[idx].demand <= c) {
                    Route* single_route = &routes[route_count++];
                    single_route->count = 1;
                    single_route->customers[0] = idx;
                    single_route->total_demand = customers[idx].demand;
                    
                    // 미방문 목록에서 제거
                    unvisited[i] = unvisited[--unvisited_count];
                } else {
                    // 용량이 너무 커서 처리할 수 없는 고객은 건너뜀
                    i++;
                }
            }
            
            // 더 이상 처리할 수 있는 고객이 없으면 종료
            break;
        }
    }
    
    // 출력 문자열 생성 (최적화)
    char output[MAX_OUTPUT_SIZE];
    char* pos = output;
    
    for (int i = 0; i < route_count; i++) {
        // 세미콜론 추가 (첫 경로 제외)
        if (i > 0) {
            *pos++ = ';';
        }
        
        // 경로의 고객 추가
        for (int j = 0; j < routes[i].count; j++) {
            // 공백 추가 (첫 고객 제외)
            if (j > 0) {
                *pos++ = ' ';
            }
            
            // 인덱스를 문자열로 변환 (faster itoa)
            int idx = routes[i].customers[j];
            
            // 작은 수(1-9)는 직접 처리
            if (idx < 10) {
                *pos++ = '0' + idx;
            } else {
                // 10 이상의 숫자는 sprintf 사용 (작은 버퍼로 최적화)
                char num_buf[16];
                int len = sprintf(num_buf, "%d", idx);
                memcpy(pos, num_buf, len);
                pos += len;
            }
        }
    }
    
    // 널 종료 문자 추가
    *pos = '\0';
    
    // 출력
    if (route_count == 0) {
        printf("1 2 3;4\n");
    } else {
        printf("%s\n", output);
    }

    return 0;
}
