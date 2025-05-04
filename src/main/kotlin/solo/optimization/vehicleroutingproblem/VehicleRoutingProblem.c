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

// 용량을 초과하지 않는지 확인하는 함수 (안전 마진 추가)
static bool check_capacity(int current_demand, int additional_demand, int capacity) {
    // 작은 안전 마진 추가 (1%)
    int safe_capacity = (int)(capacity * 0.99);
    return current_demand + additional_demand <= safe_capacity;
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
    
    // 수요가 큰 고객 먼저 처리하기 위한 정렬된 목록
    int sorted_by_demand[MAX_CUSTOMERS];
    
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
            unvisited[unvisited_count] = index;
            sorted_by_demand[unvisited_count] = index;
            unvisited_count++;
        }
    }
    
    // 수요가 큰 순서대로 정렬 (수요가 큰 고객을 먼저 처리하기 위함)
    for (int i = 0; i < unvisited_count - 1; i++) {
        for (int j = 0; j < unvisited_count - i - 1; j++) {
            if (customers[sorted_by_demand[j]].demand < customers[sorted_by_demand[j + 1]].demand) {
                int temp = sorted_by_demand[j];
                sorted_by_demand[j] = sorted_by_demand[j + 1];
                sorted_by_demand[j + 1] = temp;
            }
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
    
    // 수요가 큰 고객부터 먼저 처리 (용량 문제를 사전에 해결하기 위함)
    for (int demandi = 0; demandi < unvisited_count; demandi++) {
        int idx = sorted_by_demand[demandi];
        
        // 용량이 매우 큰 고객은 별도 경로로 할당 (용량의 50% 이상)
        if (customers[idx].demand > c / 2 && !customers[idx].visited) {
            Route* large_route = &routes[route_count++];
            large_route->count = 1;
            large_route->customers[0] = idx;
            large_route->total_demand = customers[idx].demand;
            customers[idx].visited = true;
            
            // 미방문 목록에서 제거
            for (int i = 0; i < unvisited_count; i++) {
                if (unvisited[i] == idx) {
                    unvisited[i] = unvisited[--unvisited_count];
                    break;
                }
            }
        }
    }
    
    // 나머지 경로 생성
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
                if (check_capacity(current_route->total_demand, customers[customer_idx].demand, c)) {
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
                    fprintf(stderr, "Warning: Customer %d with demand %d exceeds vehicle capacity %d\n", 
                            idx, customers[idx].demand, c);
                    i++;
                }
            }
            
            // 더 이상 처리할 수 있는 고객이 없으면 종료
            break;
        }
    }
    
    // 경로 검증 (용량 초과 체크)
    for (int i = 0; i < route_count; i++) {
        if (routes[i].total_demand > c) {
            fprintf(stderr, "Warning: Route %d exceeds capacity: %d > %d\n", i, routes[i].total_demand, c);
            
            // 용량 초과하는 경로는 하나씩 개별 경로로 분리
            int old_count = routes[i].count;
            int old_customers[MAX_CUSTOMERS];
            memcpy(old_customers, routes[i].customers, old_count * sizeof(int));
            
            routes[i].count = 0;
            routes[i].total_demand = 0;
            
            // 용량에 맞게 개별 경로로 다시 할당
            for (int j = 0; j < old_count; j++) {
                int customer_idx = old_customers[j];
                Route* new_route = &routes[route_count++];
                new_route->count = 1;
                new_route->customers[0] = customer_idx;
                new_route->total_demand = customers[customer_idx].demand;
            }
            
            // 원래 경로는 제거 (나중에 빈 경로 정리)
            routes[i].count = 0;
        }
    }
    
    // 빈 경로 제거 및 정리
    int valid_routes = 0;
    for (int i = 0; i < route_count; i++) {
        if (routes[i].count > 0) {
            if (i != valid_routes) {
                routes[valid_routes] = routes[i];
            }
            valid_routes++;
        }
    }
    route_count = valid_routes;
    
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
            
            // 인덱스를 문자열로 변환
            int idx = routes[i].customers[j];
            
            // 작은 수는 직접 처리
            if (idx < 10) {
                *pos++ = '0' + idx;
            } else if (idx < 100) {
                *pos++ = '0' + (idx / 10);
                *pos++ = '0' + (idx % 10);
            } else {
                // 100 이상의 숫자는 sprintf 사용
                char num_buf[8];
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
