#include <stdlib.h>
#include <stdio.h>
#include <string.h>
#include <stdbool.h>
#include <math.h>
#include <limits.h>
#include <float.h>

#define MAX_CUSTOMERS 100
#define MAX_ROUTES 20
#define MAX_SAVINGS (MAX_CUSTOMERS * MAX_CUSTOMERS)

// k-d 트리 구조체 정의
typedef struct KDNode {
    int index;             // 고객 인덱스
    int point[2];          // 2D 좌표 (x, y)
    struct KDNode* left;   // 왼쪽 자식
    struct KDNode* right;  // 오른쪽 자식
} KDNode;

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

// 절약값(saving)을 위한 구조체
typedef struct {
    int i;
    int j;
    int saving_value;
} Saving;

// k-d 트리를 위한 거리 제곱 계산
static inline int point_distance_squared(int* a, int* b) {
    int dx = a[0] - b[0];
    int dy = a[1] - b[1];
    return dx * dx + dy * dy;
}

// 두 지점 사이의 거리 제곱 계산 (sqrt 제거로 성능 향상)
static inline int distance_squared(const Customer* a, const Customer* b) {
    int dx = a->x - b->x;
    int dy = a->y - b->y;
    return dx * dx + dy * dy;
}

// k-d 트리 노드 생성
static KDNode* create_kdnode(int index, int x, int y) {
    KDNode* node = (KDNode*)malloc(sizeof(KDNode));
    node->index = index;
    node->point[0] = x;
    node->point[1] = y;
    node->left = NULL;
    node->right = NULL;
    return node;
}

// k-d 트리에서 x축 또는 y축 기준으로 정렬하기 위한 비교 함수
static int compare_x(const void* a, const void* b) {
    Customer* ca = (Customer*)a;
    Customer* cb = (Customer*)b;
    return ca->x - cb->x;
}

static int compare_y(const void* a, const void* b) {
    Customer* ca = (Customer*)a;
    Customer* cb = (Customer*)b;
    return ca->y - cb->y;
}

// Customer 배열에서 k-d 트리 구축 (재귀적 구현)
static KDNode* build_kdtree(Customer* customers, int start, int end, int depth) {
    if (start > end) return NULL;
    
    // 현재 차원에 따라 정렬 (depth % 2 == 0이면 x축, 아니면 y축)
    int axis = depth % 2;
    if (axis == 0) {
        qsort(customers + start, end - start + 1, sizeof(Customer), compare_x);
    } else {
        qsort(customers + start, end - start + 1, sizeof(Customer), compare_y);
    }
    
    // 중앙값을 기준으로 분할
    int median = start + (end - start) / 2;
    KDNode* node = create_kdnode(customers[median].index, customers[median].x, customers[median].y);
    
    // 좌우 서브트리 재귀적으로 구축
    node->left = build_kdtree(customers, start, median - 1, depth + 1);
    node->right = build_kdtree(customers, median + 1, end, depth + 1);
    
    return node;
}

// 최근접 이웃 검색을 위한 전역 변수
static int best_index;
static int best_dist;

// k-d 트리에서 최근접 이웃 검색
static void nearest_neighbor_search(KDNode* root, int target_point[2], int depth) {
    if (root == NULL) return;
    
    // 현재 노드와의 거리 계산
    int dist = point_distance_squared(root->point, target_point);
    
    // 더 가까운 점을 찾았으면 업데이트
    if (dist < best_dist && root->index != 0) { // 0번(depot)이 아닌 경우에만
        best_dist = dist;
        best_index = root->index;
    }
    
    // 현재 축
    int axis = depth % 2;
    int diff = target_point[axis] - root->point[axis];
    
    // 먼저 가능성이 높은 서브트리 탐색
    KDNode* near_subtree = (diff <= 0) ? root->left : root->right;
    KDNode* far_subtree = (diff <= 0) ? root->right : root->left;
    
    nearest_neighbor_search(near_subtree, target_point, depth + 1);
    
    // 다른 서브트리에 더 가까운 점이 있을 수 있는 경우 탐색
    if (diff * diff < best_dist) {
        nearest_neighbor_search(far_subtree, target_point, depth + 1);
    }
}

// k-d 트리 메모리 해제
static void free_kdtree(KDNode* root) {
    if (root == NULL) return;
    free_kdtree(root->left);
    free_kdtree(root->right);
    free(root);
}

// 주어진 고객 위치에서 가장 가까운 미방문 고객 찾기
static int find_nearest_unvisited(KDNode* root, Customer* customers, int current_idx, bool* visited) {
    int target_point[2] = {customers[current_idx].x, customers[current_idx].y};
    best_index = -1;
    best_dist = INT_MAX;
    
    nearest_neighbor_search(root, target_point, 0);
    
    // 방문 여부 확인하고, 가장 가까운 미방문 고객 찾기
    if (best_index >= 0 && !visited[best_index]) {
        return best_index;
    }
    
    // k-d 트리로 찾은 가장 가까운 고객이 이미 방문했다면, 
    // 모든 미방문 고객 중에서 선형 탐색으로 가장 가까운 고객 찾기
    int nearest_idx = -1;
    int min_dist = INT_MAX;
    
    for (int i = 1; i < MAX_CUSTOMERS; i++) {
        if (!visited[i] && customers[i].index == i) { // 유효한 고객인지 확인
            int dist = distance_squared(&customers[current_idx], &customers[i]);
            if (dist < min_dist) {
                min_dist = dist;
                nearest_idx = i;
            }
        }
    }
    
    return nearest_idx;
}

// 경로 내의 두 고객 위치를 교환하여 개선 시도 (개선된 2-opt)
static bool improve_route(Route* route, const Customer* customers, const int** dist_squared) {
    if (route->count < 2) return false;
    
    bool improved = false;
    int best_gain = 0;
    int best_i = -1, best_j = -1;
    
    // 최대 이득을 주는 교환 찾기
    for (int i = 0; i < route->count - 1; i++) {
        int cust_i = route->customers[i];
        int cust_i_next = route->customers[i+1];
        int cust_i_prev = (i > 0) ? route->customers[i-1] : 0;
        
        for (int j = i + 1; j < route->count; j++) {
            int cust_j = route->customers[j];
            int cust_j_next = (j < route->count - 1) ? route->customers[j+1] : 0;
            
            // 현재 비용
            int current_cost = dist_squared[cust_i_prev][cust_i] + 
                              dist_squared[cust_i][cust_i_next] + 
                              dist_squared[cust_j][cust_j_next];
            
            // 새 연결 비용
            int new_cost = dist_squared[cust_i_prev][cust_j] + 
                          dist_squared[cust_j][cust_i_next] + 
                          dist_squared[cust_i][cust_j_next];
            
            int gain = current_cost - new_cost;
            if (gain > best_gain) {
                best_gain = gain;
                best_i = i;
                best_j = j;
            }
        }
    }
    
    // 개선점이 발견되면 경로 업데이트
    if (best_gain > 0) {
        // 경로 부분 뒤집기
        int start = best_i + 1;
        int end = best_j;
        while (start < end) {
            int temp = route->customers[start];
            route->customers[start] = route->customers[end];
            route->customers[end] = temp;
            start++;
            end--;
        }
        improved = true;
    }
    
    return improved;
}

// 저축값 비교 함수 (내림차순 정렬)
static int compare_savings(const void* a, const void* b) {
    const Saving* s1 = (const Saving*)a;
    const Saving* s2 = (const Saving*)b;
    return s2->saving_value - s1->saving_value;
}

// Clarke-Wright 저축 알고리즘 구현
static void clarke_wright_algorithm(Customer* customers, int n, int capacity, 
                                  int** dist_squared, Route* routes, int* route_count) {
    // 저축값(savings) 계산
    Saving savings[MAX_SAVINGS];
    int savings_count = 0;
    
    // 1. 초기 상태: 각 고객을 위한 개별 경로 (depot -> customer -> depot)
    *route_count = n - 1;  // 디포(0) 제외한 고객 수
    for (int i = 1; i < n; i++) {
        routes[i-1].count = 1;
        routes[i-1].customers[0] = i;
        routes[i-1].total_demand = customers[i].demand;
    }
    
    // 2. 모든 고객 쌍에 대한 저축값 계산
    for (int i = 1; i < n; i++) {
        for (int j = i + 1; j < n; j++) {
            // savings = d(0,i) + d(0,j) - d(i,j)
            int saving = dist_squared[0][i] + dist_squared[0][j] - dist_squared[i][j];
            
            savings[savings_count].i = i;
            savings[savings_count].j = j;
            savings[savings_count].saving_value = saving;
            savings_count++;
        }
    }
    
    // 3. 저축값 기준 내림차순 정렬
    qsort(savings, savings_count, sizeof(Saving), compare_savings);
    
    // 각 경로의 끝 고객(first/last)을 추적하기 위한 배열
    int route_first[MAX_CUSTOMERS], route_last[MAX_CUSTOMERS];
    int route_of[MAX_CUSTOMERS];
    
    for (int i = 1; i < n; i++) {
        route_first[i-1] = i;
        route_last[i-1] = i;
        route_of[i] = i-1;
    }
    
    // 4. 저축값이 큰 순서대로 경로 병합 시도
    for (int s = 0; s < savings_count; s++) {
        int i = savings[s].i;
        int j = savings[s].j;
        
        int route_i = route_of[i];
        int route_j = route_of[j];
        
        // 두 고객이 이미 같은 경로에 있으면 건너뜀
        if (route_i == route_j) continue;
        
        // i가 자신의 경로에서 끝 고객이어야 함
        if (route_last[route_i] != i) continue;
        
        // j가 자신의 경로에서 첫 고객이어야 함
        if (route_first[route_j] != j) continue;
        
        // 용량 제약 조건 확인
        if (routes[route_i].total_demand + routes[route_j].total_demand > capacity) continue;
        
        // 두 경로 병합
        int old_route_j = route_j;
        
        // route_i + route_j
        for (int k = 0; k < routes[route_j].count; k++) {
            int cust = routes[route_j].customers[k];
            routes[route_i].customers[routes[route_i].count++] = cust;
            route_of[cust] = route_i;
        }
        
        // 수요 업데이트
        routes[route_i].total_demand += routes[route_j].total_demand;
        
        // 끝 고객 업데이트
        route_last[route_i] = route_last[route_j];
        
        // 병합된 경로(j)를 마지막 경로와 교체하고 총 경로 수 감소
        if (old_route_j < *route_count - 1) {
            routes[old_route_j] = routes[*route_count - 1];
            
            // 경로 매핑 업데이트
            for (int k = 0; k < routes[old_route_j].count; k++) {
                int cust = routes[old_route_j].customers[k];
                route_of[cust] = old_route_j;
            }
            
            route_first[old_route_j] = route_first[*route_count - 1];
            route_last[old_route_j] = route_last[*route_count - 1];
        }
        
        (*route_count)--;
    }
    
    // 5. 병합된 경로만 유효한 경로로 취급
    int valid_count = 0;
    for (int i = 0; i < *route_count; i++) {
        if (routes[i].count > 0) {
            if (i != valid_count) {
                routes[valid_count] = routes[i];
            }
            valid_count++;
        }
    }
    *route_count = valid_count;
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
    Customer customers_copy[MAX_CUSTOMERS]; // k-d 트리 구축용 복사본
    
    // 특별히 큰 수요를 가진 고객 목록 (개별 경로 처리용)
    int large_demands[MAX_CUSTOMERS];
    int large_count = 0;
    
    for (int i = 0; i < n; i++) {
        int index, x, y, demand;
        scanf("%d%d%d%d", &index, &x, &y, &demand);
        
        customers[index].index = index;
        customers[index].x = x;
        customers[index].y = y;
        customers[index].demand = demand;
        customers[index].visited = false;
        
        // 복사본도 동일하게 초기화
        customers_copy[index] = customers[index];
        
        // 용량의 절반 이상인 큰 수요를 가진 고객은 별도 추적
        if (index > 0 && demand > c/2) {
            large_demands[large_count++] = index;
        }
    }
    
    // k-d 트리 구축
    KDNode* kdtree = build_kdtree(customers_copy, 0, n-1, 0);
    
    // 거리 제곱 행렬 계산 및 저장 (일부 핵심 연산에 대해서만 필요할 수 있음)
    int* dist_squared[MAX_CUSTOMERS];
    for (int i = 0; i < n; i++) {
        dist_squared[i] = (int*)malloc(n * sizeof(int));
        for (int j = 0; j < n; j++) {
            dist_squared[i][j] = distance_squared(&customers[i], &customers[j]);
        }
    }
    
    // 경로 생성을 위한 배열
    Route routes[MAX_ROUTES];
    int route_count = 0;
    
    // 1단계: 수요가 큰 고객들을 개별 경로로 처리
    for (int i = 0; i < large_count; i++) {
        int idx = large_demands[i];
        Route* route = &routes[route_count++];
        route->count = 1;
        route->customers[0] = idx;
        route->total_demand = customers[idx].demand;
        customers[idx].visited = true;
    }
    
    // 2단계: 남은 고객들을 위한 별도의 배열 구성
    Customer remaining_customers[MAX_CUSTOMERS];
    int remaining_count = 1;  // 디포(0)부터 시작
    remaining_customers[0] = customers[0];
    
    for (int i = 1; i < n; i++) {
        if (!customers[i].visited) {
            remaining_customers[remaining_count++] = customers[i];
        }
    }
    
    // 남은 고객들 간의 거리 행렬 계산
    int* remaining_dist[MAX_CUSTOMERS];
    for (int i = 0; i < remaining_count; i++) {
        remaining_dist[i] = (int*)malloc(remaining_count * sizeof(int));
        for (int j = 0; j < remaining_count; j++) {
            int orig_i = remaining_customers[i].index;
            int orig_j = remaining_customers[j].index;
            remaining_dist[i][j] = dist_squared[orig_i][orig_j];
        }
    }
    
    // 남은 고객이 있다면 Clarke-Wright 알고리즘으로 경로 생성
    if (remaining_count > 1) {
        Route cw_routes[MAX_ROUTES];
        int cw_route_count = 0;
        
        clarke_wright_algorithm(remaining_customers, remaining_count, c, 
                              remaining_dist, cw_routes, &cw_route_count);
        
        // Clarke-Wright로 생성된 경로를 실제 인덱스로 변환하여 추가
        for (int i = 0; i < cw_route_count; i++) {
            Route* new_route = &routes[route_count++];
            new_route->count = cw_routes[i].count;
            new_route->total_demand = cw_routes[i].total_demand;
            
            for (int j = 0; j < cw_routes[i].count; j++) {
                int local_idx = cw_routes[i].customers[j];
                new_route->customers[j] = remaining_customers[local_idx].index;
            }
        }
        
        // 임시 거리 행렬 메모리 해제
        for (int i = 0; i < remaining_count; i++) {
            free(remaining_dist[i]);
        }
    }
    
    // 3단계: 모든 경로에 대해 개선된 2-opt 적용
    for (int r = 0; r < route_count; r++) {
        Route* route = &routes[r];
        if (route->count < 3) continue;  // 2개 이하의 고객은 최적화 필요 없음
        
        bool improved;
        int iteration = 0;
        int max_iterations = 5;  // 최대 반복 횟수
        
        // 개선이 없을 때까지 또는 최대 반복 횟수에 도달할 때까지 반복
        do {
            improved = improve_route(route, customers, (const int**)dist_squared);
            iteration++;
        } while (improved && iteration < max_iterations);
    }
    
    // 경로 검증 (용량 초과 체크)
    for (int i = 0; i < route_count; i++) {
        if (routes[i].total_demand > c) {
            fprintf(stderr, "Warning: Route %d exceeds capacity: %d > %d\n", 
                    i, routes[i].total_demand, c);
            
            // 용량 초과하는 경로는 개별 고객 경로로 분리
            for (int j = 0; j < routes[i].count; j++) {
                int cust_idx = routes[i].customers[j];
                
                Route* new_route = &routes[route_count++];
                new_route->count = 1;
                new_route->customers[0] = cust_idx;
                new_route->total_demand = customers[cust_idx].demand;
            }
            
            // 원래 경로 비우기
            routes[i].count = 0;
            routes[i].total_demand = 0;
        }
    }
    
    // 빈 경로 제거
    int valid_count = 0;
    for (int i = 0; i < route_count; i++) {
        if (routes[i].count > 0) {
            if (i != valid_count) {
                routes[valid_count] = routes[i];
            }
            valid_count++;
        }
    }
    route_count = valid_count;
    
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
    
    // k-d 트리 메모리 해제
    free_kdtree(kdtree);

    return 0;
}
