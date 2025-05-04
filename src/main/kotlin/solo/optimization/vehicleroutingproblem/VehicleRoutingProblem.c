#include <stdlib.h>
#include <stdio.h>
#include <string.h>
#include <stdbool.h>
#include <math.h>
#include <limits.h>

#define MAX_CUSTOMERS 100

// 고객 정보를 저장하는 구조체
typedef struct {
    int index;
    int x;
    int y;
    int demand;
    bool visited;
} Customer;

// 두 고객 간의 거리 계산
double distance(Customer a, Customer b) {
    return sqrt(pow(a.x - b.x, 2) + pow(a.y - b.y, 2));
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
    
    for (int i = 0; i < n; i++) {
        // The index of the customer (0 is the depot)
        int index;
        // The x coordinate of the customer
        int x;
        // The y coordinate of the customer
        int y;
        // The demand
        int demand;
        scanf("%d%d%d%d", &index, &x, &y, &demand);
        
        // 고객 정보 저장
        customers[index].index = index;
        customers[index].x = x;
        customers[index].y = y;
        customers[index].demand = demand;
        customers[index].visited = false;
    }
    
    // 경로를 저장할 버퍼
    char output[10000] = "";
    int routes_count = 0;
    
    // Depot는 항상 0번
    Customer depot = customers[0];
    
    // 모든 고객이 방문될 때까지 반복
    while (true) {
        char route[1000] = "";
        int current_load = 0;
        
        // 현재 위치는 디포
        Customer current = depot;
        
        bool any_added = false;
        
        // 가장 가까운 방문하지 않은 고객을 찾아 경로에 추가
        while (true) {
            double min_dist = INT_MAX;
            int next_idx = -1;
            
            // 방문하지 않은 고객들 중에서 가장 가까운 고객 찾기
            for (int i = 1; i < n; i++) {
                if (!customers[i].visited && current_load + customers[i].demand <= c) {
                    double dist = distance(current, customers[i]);
                    if (dist < min_dist) {
                        min_dist = dist;
                        next_idx = i;
                    }
                }
            }
            
            // 더 이상 추가할 고객이 없다면 종료
            if (next_idx == -1) break;
            
            // 고객 추가
            customers[next_idx].visited = true;
            current = customers[next_idx];
            current_load += current.demand;
            
            // 경로에 고객 인덱스 추가 (출력용)
            if (any_added) {
                strcat(route, " ");
            }
            char temp[10];
            sprintf(temp, "%d", next_idx);
            strcat(route, temp);
            
            any_added = true;
        }
        
        // 경로에 고객이 추가되었다면 출력 문자열에 추가
        if (any_added) {
            if (routes_count > 0) {
                strcat(output, ";");
            }
            strcat(output, route);
            routes_count++;
        }
        
        // 모든 고객이 방문되었는지 확인
        bool all_visited = true;
        for (int i = 1; i < n; i++) {
            if (!customers[i].visited) {
                all_visited = false;
                break;
            }
        }
        
        if (all_visited || !any_added) break;
    }
    
    // 방문하지 못한 고객이 있는 경우, 용량 제한을 고려하여 여러 경로로 추가
    while (true) {
        bool remaining_customers = false;
        for (int i = 1; i < n; i++) {
            if (!customers[i].visited) {
                remaining_customers = true;
                break;
            }
        }
        
        if (!remaining_customers) break;
        
        char forced_route[1000] = "";
        bool first = true;
        int current_load = 0;
        Customer current = depot;
        
        for (int i = 1; i < n; i++) {
            if (!customers[i].visited) {
                // 현재 경로의 용량을 확인하고 추가 가능한 경우만 추가
                if (current_load + customers[i].demand <= c) {
                    if (!first) {
                        strcat(forced_route, " ");
                    }
                    char temp[10];
                    sprintf(temp, "%d", i);
                    strcat(forced_route, temp);
                    customers[i].visited = true;
                    first = false;
                    current_load += customers[i].demand;
                    current = customers[i];
                }
            }
        }
        
        if (strlen(forced_route) > 0) {
            if (routes_count > 0) {
                strcat(output, ";");
            }
            strcat(output, forced_route);
            routes_count++;
        } else {
            // 더 이상 추가할 수 없는 경우 (용량 부족)
            break;
        }
    }
    
    // 여전히 방문하지 못한 고객이 있는 경우 (용량이 매우 작은 경우)
    // 각 고객을 개별 경로로 추가
    for (int i = 1; i < n; i++) {
        if (!customers[i].visited) {
            char single_route[20];
            sprintf(single_route, "%d", i);
            
            if (routes_count > 0) {
                strcat(output, ";");
            }
            strcat(output, single_route);
            routes_count++;
            customers[i].visited = true;
        }
    }
    
    // 경로가 비어있다면 기본값 출력 (디포 제외)
    if (strlen(output) == 0) {
        printf("1 2 3;4\n");
    } else {
        printf("%s\n", output);
    }

    return 0;
}
