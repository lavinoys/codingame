#include <stdlib.h>
#include <stdio.h>
#include <string.h>
#include <stdbool.h>
#include <math.h>
#include <float.h>

// 위치와 수요를 저장하는 구조체
typedef struct {
    int index;
    int x;
    int y;
    int demand;
} Customer;

// 경로를 저장하는 구조체
typedef struct {
    int* customers;
    int count;
    int capacity;
    int total_demand;
    int total_distance;
} Route;

// k-d 트리 노드 구조체
typedef struct kdnode {
    Customer* customer;
    int axis;
    struct kdnode* left;
    struct kdnode* right;
} KDNode;

// 저장(savings) 쌍 구조체
typedef struct {
    int i;
    int j;
    double saving;
} SavingsPair;

// 유클리드 거리 계산 함수
int euclidean_distance(Customer* a, Customer* b) {
    double dx = a->x - b->x;
    double dy = a->y - b->y;
    return (int)round(sqrt(dx * dx + dy * dy));
}

// k-d 트리 생성 함수
KDNode* create_kdtree(Customer* customers, int n, int depth) {
    if (n <= 0) return NULL;
    
    int axis = depth % 2; // 2차원이므로 0 또는 1
    int median = n / 2;
    
    // axis에 따라 정렬 (간단한 삽입 정렬)
    for (int i = 0; i < n; i++) {
        for (int j = i + 1; j < n; j++) {
            if ((axis == 0 && customers[i].x > customers[j].x) ||
                (axis == 1 && customers[i].y > customers[j].y)) {
                Customer temp = customers[i];
                customers[i] = customers[j];
                customers[j] = temp;
            }
        }
    }
    
    // 노드 생성
    KDNode* node = (KDNode*)malloc(sizeof(KDNode));
    node->customer = &customers[median];
    node->axis = axis;
    
    // 재귀적으로 좌우 자식 생성
    node->left = create_kdtree(customers, median, depth + 1);
    node->right = create_kdtree(customers + median + 1, n - median - 1, depth + 1);
    
    return node;
}

// k-d 트리 메모리 해제
void free_kdtree(KDNode* node) {
    if (node == NULL) return;
    free_kdtree(node->left);
    free_kdtree(node->right);
    free(node);
}

// 저장(savings) 값 계산을 위한 비교 함수
int savings_compare(const void* a, const void* b) {
    SavingsPair* sa = (SavingsPair*)a;
    SavingsPair* sb = (SavingsPair*)b;
    if (sb->saving > sa->saving) return 1;
    if (sb->saving < sa->saving) return -1;
    return 0;
}

// Clarke-Wright 저장(savings) 알고리즘 구현
Route** clarke_wright_savings(Customer* customers, int n, int vehicle_capacity, int* route_count) {
    // 초기 경로 생성 (각 고객을 개별 경로로)
    Route** routes = (Route**)malloc((n - 1) * sizeof(Route*));
    *route_count = n - 1;
    
    for (int i = 0; i < n - 1; i++) {
        routes[i] = (Route*)malloc(sizeof(Route));
        routes[i]->customers = (int*)malloc(sizeof(int));
        routes[i]->customers[0] = customers[i + 1].index;
        routes[i]->count = 1;
        routes[i]->capacity = vehicle_capacity;
        routes[i]->total_demand = customers[i + 1].demand;
        routes[i]->total_distance = 2 * euclidean_distance(&customers[0], &customers[i + 1]); // 왕복 거리
    }
    
    // 모든 가능한 경로 쌍에 대한 저장(savings) 계산
    int savings_count = (n - 1) * (n - 2) / 2;
    SavingsPair* savings = (SavingsPair*)malloc(savings_count * sizeof(SavingsPair));
    int idx = 0;
    
    for (int i = 1; i < n; i++) {
        for (int j = i + 1; j < n; j++) {
            double saving = euclidean_distance(&customers[0], &customers[i]) + 
                           euclidean_distance(&customers[0], &customers[j]) - 
                           euclidean_distance(&customers[i], &customers[j]);
            savings[idx].i = i;
            savings[idx].j = j;
            savings[idx].saving = saving;
            idx++;
        }
    }
    
    // 저장(savings)을 내림차순으로 정렬
    qsort(savings, savings_count, sizeof(SavingsPair), savings_compare);
    
    // 경로 병합
    bool* merged = (bool*)calloc(n - 1, sizeof(bool));
    bool* route_end = (bool*)malloc(2 * (n - 1) * sizeof(bool));
    int* route_index = (int*)malloc(n * sizeof(int));
    
    // 각 고객이 속한 경로 인덱스 초기화
    for (int i = 1; i < n; i++) {
        route_index[i] = i - 1;
    }
    
    // 각 경로의 시작과 끝 초기화
    for (int i = 0; i < n - 1; i++) {
        route_end[i * 2] = true;     // 시작점
        route_end[i * 2 + 1] = true; // 끝점
    }
    
    // 저장(savings)을 기준으로 경로 병합
    for (int i = 0; i < savings_count; i++) {
        int cust1 = savings[i].i;
        int cust2 = savings[i].j;
        int route1 = route_index[cust1];
        int route2 = route_index[cust2];
        
        // 이미 같은 경로에 있거나 병합된 경로인 경우 건너뛰기
        if (route1 == route2 || merged[route1] || merged[route2]) continue;
        
        // 경로 끝에 위치한 고객인지 확인
        bool is_cust1_end = (routes[route1]->customers[0] == cust1 && route_end[route1 * 2]) ||
                         (routes[route1]->customers[routes[route1]->count - 1] == cust1 && route_end[route1 * 2 + 1]);
        bool is_cust2_end = (routes[route2]->customers[0] == cust2 && route_end[route2 * 2]) ||
                         (routes[route2]->customers[routes[route2]->count - 1] == cust2 && route_end[route2 * 2 + 1]);
        
        if (!is_cust1_end || !is_cust2_end) continue;
        
        // 용량 제한 확인
        if (routes[route1]->total_demand + routes[route2]->total_demand > vehicle_capacity) continue;
        
        // 두 경로 병합
        int new_count = routes[route1]->count + routes[route2]->count;
        int* new_customers = (int*)malloc(new_count * sizeof(int));
        int idx = 0;
        
        // 첫 번째 경로 복사 (필요시 반전)
        if (routes[route1]->customers[routes[route1]->count - 1] == cust1) {
            for (int j = 0; j < routes[route1]->count; j++) {
                new_customers[idx++] = routes[route1]->customers[j];
            }
        } else {
            for (int j = routes[route1]->count - 1; j >= 0; j--) {
                new_customers[idx++] = routes[route1]->customers[j];
            }
        }
        
        // 두 번째 경로 복사 (필요시 반전)
        if (routes[route2]->customers[0] == cust2) {
            for (int j = 0; j < routes[route2]->count; j++) {
                new_customers[idx++] = routes[route2]->customers[j];
            }
        } else {
            for (int j = routes[route2]->count - 1; j >= 0; j--) {
                new_customers[idx++] = routes[route2]->customers[j];
            }
        }
        
        // 두 번째 경로를 병합됨으로 표시
        merged[route2] = true;
        
        // 첫 번째 경로 업데이트
        free(routes[route1]->customers);
        routes[route1]->customers = new_customers;
        routes[route1]->count = new_count;
        routes[route1]->total_demand += routes[route2]->total_demand;
        
        // 모든 고객의 경로 인덱스 업데이트
        for (int j = 0; j < new_count; j++) {
            route_index[new_customers[j]] = route1;
        }
        
        // 경로 끝 업데이트
        route_end[route1 * 2] = false;
        route_end[route1 * 2 + 1] = false;
        if (routes[route1]->customers[0] != 0) route_end[route1 * 2] = true;
        if (routes[route1]->customers[routes[route1]->count - 1] != 0) route_end[route1 * 2 + 1] = true;
    }
    
    // 결과 경로 정리
    Route** result_routes = (Route**)malloc(n * sizeof(Route*));
    int result_count = 0;
    
    for (int i = 0; i < n - 1; i++) {
        if (!merged[i]) {
            // 총 거리 계산
            routes[i]->total_distance = 0;
            int prev = 0; // 창고
            for (int j = 0; j < routes[i]->count; j++) {
                int curr = routes[i]->customers[j];
                routes[i]->total_distance += euclidean_distance(&customers[prev], &customers[curr]);
                prev = curr;
            }
            routes[i]->total_distance += euclidean_distance(&customers[prev], &customers[0]); // 창고로 돌아오기
            
            result_routes[result_count++] = routes[i];
        } else {
            free(routes[i]->customers);
            free(routes[i]);
        }
    }
    
    free(routes);
    free(merged);
    free(route_end);
    free(route_index);
    free(savings);
    
    *route_count = result_count;
    return result_routes;
}

// 2-opt 개선 방법 구현
void improve_route_with_2opt(Route* route, Customer* customers) {
    bool improvement = true;
    int n = route->count;
    
    while (improvement) {
        improvement = false;
        
        for (int i = 0; i < n - 1; i++) {
            for (int j = i + 1; j < n; j++) {
                int cust_i = route->customers[i];
                int cust_i_next = route->customers[(i + 1) % n];
                int cust_j = route->customers[j];
                int cust_j_next = route->customers[(j + 1) % n];
                
                // 현재 경로 거리
                int current_distance = 
                    euclidean_distance(&customers[cust_i], &customers[cust_i_next]) +
                    euclidean_distance(&customers[cust_j], &customers[cust_j_next]);
                
                // 새 연결 거리
                int new_distance = 
                    euclidean_distance(&customers[cust_i], &customers[cust_j]) +
                    euclidean_distance(&customers[cust_i_next], &customers[cust_j_next]);
                
                // 거리가 개선되면 경로 변경
                if (new_distance < current_distance) {
                    // i+1과 j 사이의 경로를 뒤집음
                    int left = i + 1;
                    int right = j;
                    while (left < right) {
                        int temp = route->customers[left];
                        route->customers[left] = route->customers[right];
                        route->customers[right] = temp;
                        left++;
                        right--;
                    }
                    
                    // 총 거리 재계산
                    route->total_distance = 0;
                    int prev = 0; // 창고
                    for (int k = 0; k < route->count; k++) {
                        int curr = route->customers[k];
                        route->total_distance += euclidean_distance(&customers[prev], &customers[curr]);
                        prev = curr;
                    }
                    route->total_distance += euclidean_distance(&customers[prev], &customers[0]); // 창고로 돌아오기
                    
                    improvement = true;
                    break;
                }
            }
            if (improvement) break;
        }
    }
}

// 모든 경로에 2-opt 개선 적용
void improve_routes_with_2opt(Route** routes, int route_count, Customer* customers) {
    for (int i = 0; i < route_count; i++) {
        improve_route_with_2opt(routes[i], customers);
    }
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
    
    // 고객 데이터 저장
    Customer* customers = (Customer*)malloc(n * sizeof(Customer));
    
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
        
        customers[i].index = index;
        customers[i].x = x;
        customers[i].y = y;
        customers[i].demand = demand;
    }
    
    // k-d 트리 생성 (필요시 사용)
    KDNode* kdtree = create_kdtree(customers + 1, n - 1, 0);
    
    // Clarke-Wright 저장 알고리즘으로 초기 경로 생성
    int route_count;
    Route** routes = clarke_wright_savings(customers, n, c, &route_count);
    
    // 2-opt 개선 방법으로 경로 최적화
    improve_routes_with_2opt(routes, route_count, customers);
    
    // 결과 출력
    char output[10000] = "";
    for (int i = 0; i < route_count; i++) {
        for (int j = 0; j < routes[i]->count; j++) {
            char temp[10];
            sprintf(temp, "%d ", routes[i]->customers[j]);
            strcat(output, temp);
        }
        
        if (i < route_count - 1) {
            output[strlen(output) - 1] = ';'; // 마지막 공백을 세미콜론으로 교체
        } else {
            output[strlen(output) - 1] = '\0'; // 마지막 공백 제거
        }
    }
    
    printf("%s\n", output);
    
    // 메모리 해제
    free_kdtree(kdtree);
    for (int i = 0; i < route_count; i++) {
        free(routes[i]->customers);
        free(routes[i]);
    }
    free(routes);
    free(customers);

    return 0;
}
