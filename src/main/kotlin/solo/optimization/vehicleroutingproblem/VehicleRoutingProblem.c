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
            
            // 경로에 고객 인덱스 추가
            char temp[10];
            sprintf(temp, "%d ", next_idx);
            strcat(route, temp);
            
            any_added = true;
        }
        
        // 경로에 고객이 추가되었다면 출력 문자열에 추가
        if (any_added) {
            // 마지막 공백 제거
            route[strlen(route) - 1] = '\0';
            
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
    
    // 경로가 비어있다면 기본값 출력
    if (strlen(output) == 0) {
        printf("1 2 3;4\n");
    } else {
        printf("%s\n", output);
    }

    return 0;
}
