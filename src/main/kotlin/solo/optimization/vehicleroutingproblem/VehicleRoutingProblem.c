#include <stdlib.h>
#include <stdio.h>
#include <string.h>
#include <stdbool.h>
#include <math.h>
#include <limits.h>

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

// 두 지점 사이의 유클리드 거리 계산
double calculate_distance(Customer a, Customer b) {
    return sqrt((a.x - b.x) * (a.x - b.x) + (a.y - b.y) * (a.y - b.y));
}

// 경로 내의 두 고객 위치를 교환하여 개선 시도 (2-opt)
bool improve_route(Route* route, Customer* customers, double** distances) {
    if (route->count < 2) return false;
    
    bool improved = false;
    
    for (int i = 0; i < route->count - 1; i++) {
        for (int j = i + 1; j < route->count; j++) {
            // 현재 경로: ... - i - (i+1) - ... - j - (j+1) - ...
            // 새 경로:   ... - i - j - ... - (i+1) - (j+1) - ...
            
            double current_cost = 0;
            if (i > 0) {
                current_cost += distances[route->customers[i-1]][route->customers[i]];
            } else {
                current_cost += distances[0][route->customers[i]]; // 디포에서 시작
            }
            current_cost += distances[route->customers[i]][route->customers[i+1]];
            
            if (j < route->count - 1) {
                current_cost += distances[route->customers[j]][route->customers[j+1]];
            } else {
                current_cost += distances[route->customers[j]][0]; // 디포로 돌아감
            }
            
            double new_cost = 0;
            if (i > 0) {
                new_cost += distances[route->customers[i-1]][route->customers[j]];
            } else {
                new_cost += distances[0][route->customers[j]]; // 디포에서 시작
            }
            new_cost += distances[route->customers[j]][route->customers[i+1]];
            
            if (j < route->count - 1) {
                new_cost += distances[route->customers[i]][route->customers[j+1]];
            } else {
                new_cost += distances[route->customers[i]][0]; // 디포로 돌아감
            }
            
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
    bool unvisited[MAX_CUSTOMERS];
    
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
        
        customers[index].index = index;
        customers[index].x = x;
        customers[index].y = y;
        customers[index].demand = demand;
        customers[index].visited = false;
        
        if (index > 0) { // 디포(0)는 방문할 필요 없음
            unvisited[index] = true;
        }
    }
    
    // 거리 행렬 계산 및 저장
    double** distances = (double**)malloc(n * sizeof(double*));
    for (int i = 0; i < n; i++) {
        distances[i] = (double*)malloc(n * sizeof(double));
        for (int j = 0; j < n; j++) {
            distances[i][j] = calculate_distance(customers[i], customers[j]);
        }
    }
    
    // 경로 생성
    Route routes[MAX_ROUTES];
    int route_count = 0;
    int remaining_customers = n - 1; // 디포(0) 제외
    
    while (remaining_customers > 0) {
        Route* current_route = &routes[route_count];
        current_route->count = 0;
        current_route->total_demand = 0;
        
        int current_customer = 0; // 디포에서 시작
        
        while (true) {
            double min_distance = DBL_MAX;
            int next_customer = -1;
            
            // 가장 가까운 미방문 고객 찾기
            for (int i = 1; i < n; i++) {
                if (unvisited[i] && current_route->total_demand + customers[i].demand <= c) {
                    double dist = distances[current_customer][i];
                    if (dist < min_distance) {
                        min_distance = dist;
                        next_customer = i;
                    }
                }
            }
            
            // 더 이상 방문할 고객이 없으면 현재 경로 종료
            if (next_customer == -1) {
                break;
            }
            
            // 고객 추가
            current_route->customers[current_route->count++] = next_customer;
            current_route->total_demand += customers[next_customer].demand;
            unvisited[next_customer] = false;
            current_customer = next_customer;
            remaining_customers--;
        }
        
        // 경로에 고객이 추가된 경우에만 경로 개수 증가
        if (current_route->count > 0) {
            route_count++;
        } else {
            // 용량 제약으로 인해 경로를 구성할 수 없는 경우
            // 남은 고객들 중 용량이 가장 작은 고객부터 개별 경로 할당
            for (int i = 1; i < n; i++) {
                if (unvisited[i] && customers[i].demand <= c) {
                    Route* single_route = &routes[route_count++];
                    single_route->count = 1;
                    single_route->customers[0] = i;
                    single_route->total_demand = customers[i].demand;
                    unvisited[i] = false;
                    remaining_customers--;
                }
            }
        }
    }
    
    // 2-opt를 사용하여 각 경로 개선
    for (int r = 0; r < route_count; r++) {
        bool improved = true;
        while (improved) {
            improved = improve_route(&routes[r], customers, distances);
        }
    }
    
    // 출력 형식 구성: 세미콜론으로 경로 구분, 공백으로 고객 구분
    char output[10000] = "";
    
    for (int r = 0; r < route_count; r++) {
        if (r > 0) {
            strcat(output, ";");
        }
        
        for (int i = 0; i < routes[r].count; i++) {
            char customer[10];
            sprintf(customer, "%d", routes[r].customers[i]);
            
            if (i > 0) {
                strcat(output, " ");
            }
            strcat(output, customer);
        }
    }
    
    // 결과 출력
    printf("%s\n", output);
    
    // 메모리 해제
    for (int i = 0; i < n; i++) {
        free(distances[i]);
    }
    free(distances);

    return 0;
}
