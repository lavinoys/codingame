#include <stdlib.h>
#include <stdio.h>
#include <string.h>
#include <stdbool.h>
#include <math.h>
#include <limits.h>
#include <float.h>

#define MAX_CUSTOMERS 100
#define MAX_ROUTES 20

typedef struct {
    int index;
    int x;
    int y;
    int demand;
    bool visited;
} Customer;

typedef struct {
    int customers[MAX_CUSTOMERS];
    int count;
    int total_demand;
} Route;

// 두 지점 사이의 거리 제곱 계산 (sqrt 제거로 성능 향상)
static inline int distance_squared(const Customer* a, const Customer* b) {
    int dx = a->x - b->x;
    int dy = a->y - b->y;
    return dx * dx + dy * dy;
}

// 경로 내의 두 고객 위치를 교환하여 개선 시도 (2-opt)
static bool improve_route(Route* route, const Customer* customers, const int** dist_squared) {
    if (route->count < 2) return false;
    
    bool improved = false;
    
    for (int i = 0; i < route->count - 1; i++) {
        int cust_i = route->customers[i];
        int cust_i_next = route->customers[i+1];
        
        for (int j = i + 1; j < route->count; j++) {
            int cust_j = route->customers[j];
            int cust_j_next = (j < route->count - 1) ? route->customers[j+1] : 0;
            int cust_i_prev = (i > 0) ? route->customers[i-1] : 0;
            
            // 현재 비용 계산
            int current_cost = dist_squared[cust_i_prev][cust_i] + 
                              dist_squared[cust_i][cust_i_next] + 
                              dist_squared[cust_j][cust_j_next];
            
            // 새 연결 비용 계산
            int new_cost = dist_squared[cust_i_prev][cust_j] + 
                          dist_squared[cust_j][cust_i_next] + 
                          dist_squared[cust_i][cust_j_next];
            
            if (new_cost < current_cost) {
                // 경로 개선을 위해 i+1부터 j까지의 고객들 순서를 뒤집음
                int start = i + 1;
                int end = j;
                while (start < end) {
                    int temp = route->customers[start];
                    route->customers[start] = route->customers[end];
                    route->customers[end] = temp;
                    start++;
                    end--;
                }
                improved = true;
                // 개선된 경우 즉시 다음 i로 이동
                break;
            }
        }
    }
    
    return improved;
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
    
    // 방문 여부 추적을 위한 배열 (초기화)
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
        
        if (index > 0) { // 디포(0)는 방문할 필요 없음
            unvisited[unvisited_count++] = index;
        }
    }
    
    // 거리 제곱 행렬 계산 및 저장 (정수형으로 저장하여 부동 소수점 연산 회피)
    int* dist_squared[MAX_CUSTOMERS];
    for (int i = 0; i < n; i++) {
        dist_squared[i] = (int*)malloc(n * sizeof(int));
        for (int j = 0; j < n; j++) {
            dist_squared[i][j] = distance_squared(&customers[i], &customers[j]);
        }
    }
    
    // 경로 생성
    Route routes[MAX_ROUTES];
    int route_count = 0;
    
    while (unvisited_count > 0) {
        Route* current_route = &routes[route_count];
        current_route->count = 0;
        current_route->total_demand = 0;
        
        int current_customer = 0; // 디포에서 시작
        
        while (true) {
            int min_dist = INT_MAX;
            int next_customer = -1;
            int next_idx_in_unvisited = -1;
            
            // 가장 가까운 미방문 고객 찾기
            for (int i = 0; i < unvisited_count; i++) {
                int cust_idx = unvisited[i];
                if (current_route->total_demand + customers[cust_idx].demand <= c) {
                    int dist = dist_squared[current_customer][cust_idx];
                    if (dist < min_dist) {
                        min_dist = dist;
                        next_customer = cust_idx;
                        next_idx_in_unvisited = i;
                    }
                }
            }
            
            // 더 이상 방문할 고객이 없으면 현재 경로 종료
            if (next_customer == -1) break;
            
            // 고객 추가
            current_route->customers[current_route->count++] = next_customer;
            current_route->total_demand += customers[next_customer].demand;
            
            // 미방문 배열에서 제거 (O(1) 시간에 처리하기 위해 스왑 후 크기 감소)
            unvisited[next_idx_in_unvisited] = unvisited[--unvisited_count];
            
            current_customer = next_customer;
        }
        
        // 경로에 고객이 추가된 경우에만 경로 개수 증가
        if (current_route->count > 0) {
            route_count++;
        } else {
            // 용량 제약으로 인해 경로를 구성할 수 없는 경우
            // 남은 고객들 중에서 개별 경로 할당
            int i = 0;
            while (i < unvisited_count) {
                int cust_idx = unvisited[i];
                if (customers[cust_idx].demand <= c) {
                    Route* single_route = &routes[route_count++];
                    single_route->count = 1;
                    single_route->customers[0] = cust_idx;
                    single_route->total_demand = customers[cust_idx].demand;
                    
                    // 미방문 목록에서 제거
                    unvisited[i] = unvisited[--unvisited_count];
                } else {
                    // 용량을 초과하는 고객은 처리할 수 없음 (문제 조건에서는 이런 경우가 없다고 가정)
                    i++;
                }
            }
        }
    }
    
    // 2-opt를 사용하여 각 경로 개선
    for (int r = 0; r < route_count; r++) {
        Route* route = &routes[r];
        if (route->count < 2) continue;
        
        // 최대 3번만 반복 (계속 반복하는 것은 비효율적일 수 있음)
        int max_iterations = 3;
        for (int iter = 0; iter < max_iterations; iter++) {
            if (!improve_route(route, customers, (const int**)dist_squared)) {
                break;
            }
        }
    }
    
    // 출력 문자열 생성 (최적화)
    char output[10000];
    char* pos = output;
    
    for (int r = 0; r < route_count; r++) {
        if (r > 0) {
            *pos++ = ';';
        }
        
        for (int i = 0; i < routes[r].count; i++) {
            if (i > 0) {
                *pos++ = ' ';
            }
            
            // 더 효율적인 정수-문자열 변환
            int idx = routes[r].customers[i];
            if (idx < 10) {
                *pos++ = '0' + idx;
            } else if (idx < 100) {
                *pos++ = '0' + (idx / 10);
                *pos++ = '0' + (idx % 10);
            } else {
                // 3자리 이상 숫자는 sprintf 사용
                pos += sprintf(pos, "%d", idx);
            }
        }
    }
    
    *pos = '\0';
    
    printf("%s\n", output);
    
    // 메모리 해제
    for (int i = 0; i < n; i++) {
        free(dist_squared[i]);
    }

    return 0;
}
