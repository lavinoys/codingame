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

// 두 고객 간의 거리 계산 (미리 계산하여 저장)
double calculate_distance(Customer a, Customer b) {
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
    // 방문하지 않은 고객 수 추적
    int remaining_count = n - 1; // 디포(0)는 제외
    
    // 거리 테이블 미리 계산
    double distances[MAX_CUSTOMERS][MAX_CUSTOMERS];
    
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
    
    // 거리 테이블 미리 계산
    for (int i = 0; i < n; i++) {
        for (int j = 0; j < n; j++) {
            distances[i][j] = calculate_distance(customers[i], customers[j]);
        }
    }
    
    // 경로들을 저장할 배열
    Route routes[MAX_CUSTOMERS]; // 최악의 경우 각 고객당 하나의 경로
    int route_count = 0;
    
    // Depot는 항상 0번
    Customer depot = customers[0];
    
    // 모든 고객이 방문될 때까지 반복
    while (remaining_count > 0) {
        // 새 경로 시작
        Route current_route;
        current_route.count = 0;
        current_route.total_demand = 0;
        
        // 현재 위치는 디포
        int current_pos = 0;
        
        // 가장 가까운 방문하지 않은 고객을 찾아 경로에 추가
        while (true) {
            double min_dist = INT_MAX;
            int next_idx = -1;
            
            // 방문하지 않은 고객들 중에서 가장 가까운 고객 찾기
            for (int i = 1; i < n; i++) {
                if (!customers[i].visited && current_route.total_demand + customers[i].demand <= c) {
                    double dist = distances[current_pos][i];
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
            current_pos = next_idx;
            current_route.total_demand += customers[next_idx].demand;
            
            // 경로에 고객 인덱스 추가
            current_route.customers[current_route.count++] = next_idx;
            remaining_count--;
        }
        
        // 경로에 고객이 추가되었다면 경로 배열에 저장
        if (current_route.count > 0) {
            routes[route_count++] = current_route;
        } else {
            // 더 이상 추가할 수 없는데 남은 고객이 있는 경우
            // 용량 제한으로 추가하지 못하는 경우가 발생함
            // 각 고객을 개별 경로로 처리
            for (int i = 1; i < n; i++) {
                if (!customers[i].visited && customers[i].demand <= c) {
                    Route single_route;
                    single_route.count = 1;
                    single_route.customers[0] = i;
                    single_route.total_demand = customers[i].demand;
                    routes[route_count++] = single_route;
                    customers[i].visited = true;
                    remaining_count--;
                }
            }
            
            // 용량이 c보다 큰 고객이 있다면 이는 처리할 수 없음
            // 이 경우는 문제 조건에서 발생하지 않는다고 가정
            break;
        }
    }
    
    // 모든 경로를 출력 문자열로 변환
    char output[MAX_OUTPUT_SIZE] = "";
    int output_pos = 0;
    
    for (int i = 0; i < route_count; i++) {
        Route route = routes[i];
        
        // 세미콜론 추가 (첫 경로 제외)
        if (i > 0) {
            output[output_pos++] = ';';
        }
        
        // 경로의 모든 고객 추가
        for (int j = 0; j < route.count; j++) {
            // 공백 추가 (첫 고객 제외)
            if (j > 0) {
                output[output_pos++] = ' ';
            }
            
            // 고객 인덱스를 문자열로 변환
            int idx = route.customers[j];
            int temp_idx = idx;
            int digit_count = 0;
            
            // 자릿수 계산
            do {
                temp_idx /= 10;
                digit_count++;
            } while (temp_idx > 0);
            
            // 인덱스 추가
            temp_idx = idx;
            for (int k = digit_count - 1; k >= 0; k--) {
                output[output_pos + k] = '0' + (temp_idx % 10);
                temp_idx /= 10;
            }
            output_pos += digit_count;
        }
    }
    
    // 널 종료 문자 추가
    output[output_pos] = '\0';
    
    // 경로가 비어있다면 기본값 출력 (디포 제외)
    if (output_pos == 0) {
        printf("1 2 3;4\n");
    } else {
        printf("%s\n", output);
    }

    return 0;
}
