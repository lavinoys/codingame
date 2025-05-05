#include <stdlib.h>
#include <stdio.h>
#include <string.h>
#include <stdbool.h>
#include <math.h>

#define MAX_BUILDINGS 150
#define MAX_TUBES 500
#define MAX_PODS 500
#define MAX_ASTRONAUTS 1000
#define MAX_PATH 50
#define TELEPORT_COST 5000
#define POD_COST 1000
#define POD_DESTROY_REFUND 750

// 건물 유형 상수
#define LANDING_PAD 0
#define MODULE_TYPE_COUNT 20

// 건물 구조체
typedef struct {
    int id;
    int type;         // 0: 착륙장, 1-20: 달 모듈(타입에 따라)
    int x, y;         // 좌표
    int astronaut_count[MODULE_TYPE_COUNT + 1];  // 각 타입별 우주비행사 수
    int connected_routes[5];  // 최대 5개 연결 가능
    int route_count;  // 현재 연결된 경로 수
    bool has_teleporter;  // 텔레포터 유무
} Building;

// 경로 구조체
typedef struct {
    int building1;
    int building2;
    int capacity;     // 0: 텔레포터, 1+: 튜브 용량
    double length;    // 길이 (비용 계산용)
} Route;

// 포드 구조체
typedef struct {
    int id;
    int path[MAX_PATH];
    int path_length;
    int current_pos;
    bool is_moving;
} Pod;

// 우주비행사 구조체
typedef struct {
    int id;
    int type;                // 1-20: 우주비행사 타입
    int current_building;    // 현재 위치한 건물
    int target_module;       // 목적지 모듈
    int pod_id;              // 할당된 포드 ID (-1: 미할당)
    int arrival_day;         // 착륙한 날짜
    bool arrived;            // 목적지 도착 여부
} Astronaut;

// 전역 변수
Building buildings[MAX_BUILDINGS];
Route routes[MAX_TUBES];
Pod pods[MAX_PODS];
Astronaut astronauts[MAX_ASTRONAUTS];
int building_count = 0;
int route_count = 0;
int pod_count = 0;
int astronaut_count = 0;
int month = 0;
int current_day = 0;
int total_score = 0;
int buildings_with_arrivals[MAX_BUILDINGS];  // 이번 달에 도착한 우주비행사 수 기록

// 함수 선언
double calculate_distance(int x1, int y1, int x2, int y2);
bool point_on_segment(int ax, int ay, int bx, int by, int cx, int cy);
bool segments_intersect(int ax, int ay, int bx, int by, int cx, int cy, int dx, int dy);
bool can_build_tube(int building_id1, int building_id2);
int calculate_tube_cost(int building_id1, int building_id2);
void parse_building_properties(char* properties);
void parse_pod_properties(char* properties);
void update_building_info(int id, int type, int x, int y);
void execute_strategy(int resources);
int find_building_index(int id);
void simulate_astronaut_movement();
int calculate_distance_to_target(int building_id, int target_type);
void calculate_scores();
void apply_interest(int* resources);
void initialize_monthly_data();

// 두 점 사이의 거리 계산
double calculate_distance(int x1, int y1, int x2, int y2) {
    return sqrt(pow(x2 - x1, 2) + pow(y2 - y1, 2));
}

// 점이 선분 위에 있는지 확인
bool point_on_segment(int ax, int ay, int bx, int by, int cx, int cy) {
    double epsilon = 0.0000001;
    double dist_bc = calculate_distance(bx, by, cx, cy);
    double dist_ba = calculate_distance(bx, by, ax, ay);
    double dist_ac = calculate_distance(ax, ay, cx, cy);
    
    return (fabs((dist_ba + dist_ac) - dist_bc) < epsilon);
}

// 두 선분이 교차하는지 확인
int orientation(int p1x, int p1y, int p2x, int p2y, int p3x, int p3y) {
    double prod = (p3y - p1y) * (p2x - p1x) - (p2y - p1y) * (p3x - p1x);
    if (fabs(prod) < 0.0000001) return 0;
    return (prod > 0) ? 1 : -1;
}

bool segments_intersect(int ax, int ay, int bx, int by, int cx, int cy, int dx, int dy) {
    int o1 = orientation(ax, ay, bx, by, cx, cy);
    int o2 = orientation(ax, ay, bx, by, dx, dy);
    int o3 = orientation(cx, cy, dx, dy, ax, ay);
    int o4 = orientation(cx, cy, dx, dy, bx, by);
    
    return (o1 * o2 < 0 && o3 * o4 < 0);
}

// 튜브 건설 가능 여부 확인
bool can_build_tube(int building_id1, int building_id2) {
    Building *b1 = &buildings[building_id1];
    Building *b2 = &buildings[building_id2];
    
    // 각 건물이 이미 5개의 연결을 가지고 있는지 확인
    if (b1->route_count >= 5 || b2->route_count >= 5) {
        return false;
    }
    
    // 두 건물 사이에 이미 튜브가 있는지 확인
    for (int i = 0; i < route_count; i++) {
        if ((routes[i].building1 == building_id1 && routes[i].building2 == building_id2) ||
            (routes[i].building1 == building_id2 && routes[i].building2 == building_id1)) {
            return false;
        }
    }
    
    // 다른 건물이 경로 상에 있는지 확인
    for (int i = 0; i < building_count; i++) {
        if (i != building_id1 && i != building_id2) {
            if (point_on_segment(buildings[i].x, buildings[i].y, b1->x, b1->y, b2->x, b2->y)) {
                return false;
            }
        }
    }
    
    // 다른 튜브와 교차하는지 확인
    for (int i = 0; i < route_count; i++) {
        if (routes[i].capacity > 0) { // 튜브만 확인
            Building *rb1 = &buildings[routes[i].building1];
            Building *rb2 = &buildings[routes[i].building2];
            
            if (segments_intersect(b1->x, b1->y, b2->x, b2->y, rb1->x, rb1->y, rb2->x, rb2->y)) {
                return false;
            }
        }
    }
    
    return true;
}

// 튜브 비용 계산 수정
int calculate_tube_cost(int building_id1, int building_id2) {
    int b1_idx = find_building_index(building_id1);
    int b2_idx = find_building_index(building_id2);
    
    if (b1_idx == -1 || b2_idx == -1) {
        return 1000000; // 매우 큰 값으로 설정하여 건설 불가능하게 함
    }
    
    double distance = calculate_distance(buildings[b1_idx].x, buildings[b1_idx].y, 
                                        buildings[b2_idx].x, buildings[b2_idx].y);
    
    // 0.1km당 1 자원, 내림
    return (int)(distance / 100);
}

// 건물 ID로 인덱스를 찾는 함수 추가
int find_building_index(int id) {
    for (int i = 0; i < building_count; i++) {
        if (buildings[i].id == id) {
            return i;
        }
    }
    return -1; // 건물을 찾지 못함
}

// 건물 정보 파싱
void parse_building_properties(char* properties) {
    int values[504]; // 최대 500 astronauts + 4 parameters
    char* token = strtok(properties, " ");
    int count = 0;
    
    while (token != NULL && count < 504) {
        values[count++] = atoi(token);
        token = strtok(NULL, " ");
    }
    
    int type = values[0];
    int id = values[1];
    int x = values[2];
    int y = values[3];
    
    // 새 건물 정보 업데이트
    update_building_info(id, type, x, y);
    
    // 착륙장이라면 우주비행사 정보도 저장
    if (type == LANDING_PAD) {
        int astronaut_count = values[4];
        int building_idx = find_building_index(id);
        if (building_idx != -1) {
            for (int i = 0; i < astronaut_count && i + 5 < count; i++) {
                int astronaut_type = values[i + 5];
                buildings[building_idx].astronaut_count[astronaut_type]++;
            }
        }
    }
}

// 포드 정보 파싱
void parse_pod_properties(char* properties) {
    int values[MAX_PATH + 2]; // id + path_length + path
    char* token = strtok(properties, " ");
    int count = 0;
    
    while (token != NULL && count < MAX_PATH + 2) {
        values[count++] = atoi(token);
        token = strtok(NULL, " ");
    }
    
    int pod_id = values[0];
    int path_length = values[1];
    
    // 포드 정보 업데이트
    Pod* pod = NULL;
    
    // 기존 포드인지 확인
    for (int i = 0; i < pod_count; i++) {
        if (pods[i].id == pod_id) {
            pod = &pods[i];
            break;
        }
    }
    
    // 새 포드라면 추가
    if (pod == NULL) {
        pod = &pods[pod_count++];
        pod->id = pod_id;
    }
    
    pod->path_length = path_length;
    for (int i = 0; i < path_length && i + 2 < count; i++) {
        pod->path[i] = values[i + 2];
    }
    pod->current_pos = 0;
    pod->is_moving = true;
}

// 건물 정보 업데이트
void update_building_info(int id, int type, int x, int y) {
    // ID가 이미 존재하는지 확인
    for (int i = 0; i < building_count; i++) {
        if (buildings[i].id == id) {
            // 기존 건물 정보 업데이트
            buildings[i].type = type;
            buildings[i].x = x;
            buildings[i].y = y;
            return;
        }
    }
    
    // 새 건물 추가
    Building* building = &buildings[building_count++];
    building->id = id;
    building->type = type;
    building->x = x;
    building->y = y;
    building->route_count = 0;
    building->has_teleporter = false;
    
    for (int i = 0; i <= MODULE_TYPE_COUNT; i++) {
        building->astronaut_count[i] = 0;
    }
}

// 건물 간의 최단 거리 계산 (BFS 알고리즘)
int calculate_distance_to_target(int building_id, int target_type) {
    int queue[MAX_BUILDINGS];
    int distance[MAX_BUILDINGS];
    bool visited[MAX_BUILDINGS];
    int front = 0, rear = 0;
    
    // 초기화
    for (int i = 0; i < building_count; i++) {
        distance[i] = -1;
        visited[i] = false;
    }
    
    // 시작 건물 큐에 추가
    int start_idx = find_building_index(building_id);
    if (start_idx == -1) return -1;
    
    queue[rear++] = start_idx;
    distance[start_idx] = 0;
    visited[start_idx] = true;
    
    // BFS 탐색
    while (front < rear) {
        int current = queue[front++];
        
        // 타겟 타입 도달 확인
        if (buildings[current].type == target_type) {
            return distance[current];
        }
        
        // 연결된 모든 경로 탐색
        for (int i = 0; i < buildings[current].route_count; i++) {
            int route_idx = buildings[current].connected_routes[i];
            int next_building;
            
            if (routes[route_idx].building1 == buildings[current].id) {
                next_building = find_building_index(routes[route_idx].building2);
            } else {
                next_building = find_building_index(routes[route_idx].building1);
            }
            
            // 텔레포터는 무시하지 않고 길이 0으로 처리
            if (!visited[next_building]) {
                visited[next_building] = true;
                
                // 텔레포터인 경우 거리 증가 없음
                if (routes[route_idx].capacity == 0) {
                    distance[next_building] = distance[current];
                } else {
                    distance[next_building] = distance[current] + 1;
                }
                
                queue[rear++] = next_building;
            }
        }
    }
    
    return -1;  // 경로 없음
}

// 이동 시뮬레이션 - 4단계 이동 로직
void simulate_astronaut_movement() {
    // 매일 20일 동안 이동 시뮬레이션
    for (current_day = 0; current_day < 20; current_day++) {
        fprintf(stderr, "Simulating day %d\n", current_day);
        
        // 1단계: 텔레포터 이동
        for (int i = 0; i < astronaut_count; i++) {
            if (astronauts[i].arrived) continue;
            
            int current_building_idx = find_building_index(astronauts[i].current_building);
            if (current_building_idx == -1) continue;
            
            // 각 건물에서 가능한 텔레포터 확인
            for (int r = 0; r < buildings[current_building_idx].route_count; r++) {
                int route_idx = buildings[current_building_idx].connected_routes[r];
                
                // 텔레포터인지 확인
                if (routes[route_idx].capacity == 0) {
                    int exit_building_id;
                    
                    // 텔레포터 출구 찾기
                    if (routes[route_idx].building1 == astronauts[i].current_building) {
                        exit_building_id = routes[route_idx].building2;
                    } else {
                        exit_building_id = routes[route_idx].building1;
                    }
                    
                    // 텔레포터 출구 건물의 목적지까지 거리 확인
                    int current_dist = calculate_distance_to_target(astronauts[i].current_building, astronauts[i].type);
                    int exit_dist = calculate_distance_to_target(exit_building_id, astronauts[i].type);
                    
                    // 출구가 목적지에 가깝거나 같은 거리이면 텔레포트
                    if (exit_dist != -1 && (current_dist == -1 || exit_dist <= current_dist)) {
                        astronauts[i].current_building = exit_building_id;
                        fprintf(stderr, "Astronaut %d teleported to building %d\n", i, exit_building_id);
                        
                        // 텔레포트 후 목적지 도착 확인
                        int exit_building_idx = find_building_index(exit_building_id);
                        if (exit_building_idx != -1 && buildings[exit_building_idx].type == astronauts[i].type) {
                            astronauts[i].arrived = true;
                            buildings_with_arrivals[exit_building_idx]++;
                            fprintf(stderr, "Astronaut %d arrived at destination!\n", i);
                        }
                        break;
                    }
                }
            }
        }
        
        // 2단계: 포드 할당 - 튜브 용량 고려
        for (int i = 0; i < pod_count; i++) {
            pods[i].is_moving = false;
        }
        
        int pods_on_tubes[MAX_TUBES] = {0};  // 각 튜브에 할당된 포드 수
        
        // 포드 ID 순으로 정렬해서 우선권 부여
        for (int i = 0; i < pod_count; i++) {
            // 현재 포드 위치 확인
            if (pods[i].current_pos >= pods[i].path_length - 1) {
                // 루프가 아니면 멈춤
                if (pods[i].path[0] != pods[i].path[pods[i].path_length - 1]) {
                    continue;
                }
                pods[i].current_pos = 0;  // 루프 시작으로 리셋
            }
            
            int current_building = pods[i].path[pods[i].current_pos];
            int next_building = pods[i].path[pods[i].current_pos + 1];
            
            // 해당 건물 사이의 튜브 찾기
            for (int r = 0; r < route_count; r++) {
                if ((routes[r].building1 == current_building && routes[r].building2 == next_building) ||
                    (routes[r].building2 == current_building && routes[r].building1 == next_building)) {
                    
                    // 텔레포터는 용량 제한 없음
                    if (routes[r].capacity == 0) {
                        pods[i].is_moving = true;
                        break;
                    }
                    
                    // 튜브 용량 확인
                    if (pods_on_tubes[r] < routes[r].capacity) {
                        pods[i].is_moving = true;
                        pods_on_tubes[r]++;
                        break;
                    }
                }
            }
        }
        
        // 3단계: 우주비행사 할당
        for (int i = 0; i < astronaut_count; i++) {
            if (astronauts[i].arrived || astronauts[i].pod_id != -1) continue;
            
            int current_building_id = astronauts[i].current_building;
            int best_pod = -1;
            int best_distance_improvement = 0;
            
            // 현재 건물에 있는 모든 움직이는 포드 확인
            for (int p = 0; p < pod_count; p++) {
                if (!pods[p].is_moving) continue;
                
                // 포드가 현재 건물에서 출발하는지 확인
                if (pods[p].path[pods[p].current_pos] == current_building_id) {
                    int next_building_id = pods[p].path[pods[p].current_pos + 1];
                    
                    // 다음 건물이 목적지에 더 가깝게 하는지 확인
                    int current_dist = calculate_distance_to_target(current_building_id, astronauts[i].type);
                    int next_dist = calculate_distance_to_target(next_building_id, astronauts[i].type);
                    
                    if (next_dist != -1 && (current_dist == -1 || next_dist < current_dist)) {
                        int improvement = current_dist - next_dist;
                        if (improvement > best_distance_improvement) {
                            best_distance_improvement = improvement;
                            best_pod = p;
                        }
                    }
                }
            }
            
            // 가장 좋은 포드에 우주비행사 할당
            if (best_pod != -1) {
                astronauts[i].pod_id = pods[best_pod].id;
                fprintf(stderr, "Assigned astronaut %d to pod %d\n", i, pods[best_pod].id);
            }
        }
        
        // 4단계: 포드 이동
        for (int i = 0; i < pod_count; i++) {
            if (pods[i].is_moving) {
                int current_building = pods[i].path[pods[i].current_pos];
                int next_building = pods[i].path[pods[i].current_pos + 1];
                
                // 모든 우주비행사 이동
                for (int a = 0; a < astronaut_count; a++) {
                    if (astronauts[a].pod_id == pods[i].id) {
                        astronauts[a].current_building = next_building;
                        astronauts[a].pod_id = -1;  // 포드에서 하차
                        
                        // 목적지 도착 확인
                        int building_idx = find_building_index(next_building);
                        if (building_idx != -1 && buildings[building_idx].type == astronauts[a].type) {
                            astronauts[a].arrived = true;
                            buildings_with_arrivals[building_idx]++;
                            fprintf(stderr, "Astronaut %d arrived at destination!\n", a);
                        }
                    }
                }
                
                // 포드 위치 업데이트
                pods[i].current_pos++;
                if (pods[i].current_pos >= pods[i].path_length - 1 && 
                    pods[i].path[0] == pods[i].path[pods[i].path_length - 1]) {
                    pods[i].current_pos = 0;  // 루프 시작으로 리셋
                }
            }
        }
    }
    
    // 점수 계산
    calculate_scores();
}

// 점수 계산 시스템
void calculate_scores() {
    int month_score = 0;
    
    for (int i = 0; i < astronaut_count; i++) {
        if (astronauts[i].arrived) {
            // 속도 점수: 50점에서 소요된 날짜 차감
            int speed_score = 50 - (astronauts[i].arrival_day - astronauts[i].arrival_day);
            if (speed_score < 0) speed_score = 0;
            
            // 인구 균형 점수: 50점에서 이미 도착한 우주비행사 수 차감
            int building_idx = find_building_index(astronauts[i].current_building);
            int balance_score = 50 - buildings_with_arrivals[building_idx] + 1;  // 자신은 제외
            if (balance_score < 0) balance_score = 0;
            
            int total = speed_score + balance_score;
            month_score += total;
            
            fprintf(stderr, "Astronaut %d scored %d points (speed: %d, balance: %d)\n",
                    i, total, speed_score, balance_score);
        }
    }
    
    total_score += month_score;
    fprintf(stderr, "Month %d score: %d, Total score: %d\n", month, month_score, total_score);
}

// 매월 말 이자 계산
void apply_interest(int* resources) {
    int interest = (*resources) * 0.1;  // 10% 이자
    *resources += interest;
    fprintf(stderr, "Applied 10%% interest: +%d resources, new total: %d\n", interest, *resources);
}

// 매월 초기화
void initialize_monthly_data() {
    // 착륙장에서 새로 도착한 우주비행사 설정
    astronaut_count = 0;
    
    // 건물별 도착 우주비행사 수 초기화
    for (int i = 0; i < building_count; i++) {
        buildings_with_arrivals[i] = 0;
    }
    
    // 이번 달에 이전 포드 위치 리셋
    for (int i = 0; i < pod_count; i++) {
        pods[i].current_pos = 0;
    }
}

// 게임 전략 실행 수정
void execute_strategy(int resources) {
    char actions[1024] = "";
    int original_resources = resources; // 원래 자원 저장
    
    // 첫 번째 달: 착륙장과 해당하는 모듈 간에 튜브 연결
    if (month == 0) {
        // 착륙장 찾기
        int landing_pads[MAX_BUILDINGS];
        int landing_pad_count = 0;
        
        for (int i = 0; i < building_count; i++) {
            if (buildings[i].type == LANDING_PAD) {
                landing_pads[landing_pad_count++] = i;
            }
        }
        
        // 각 착륙장과 가까운 모듈 연결
        for (int i = 0; i < landing_pad_count && resources > 0; i++) {
            Building* landing = &buildings[landing_pads[i]];
            
            // 각 우주비행사 타입에 대해 가장 가까운 모듈 찾기
            for (int type = 1; type <= MODULE_TYPE_COUNT; type++) {
                if (landing->astronaut_count[type] > 0) {
                    // 이 타입의 모든 모듈 찾기
                    int closest_module = -1;
                    double min_distance = 1e9;
                    
                    for (int j = 0; j < building_count; j++) {
                        if (buildings[j].type == type) {
                            double dist = calculate_distance(landing->x, landing->y, buildings[j].x, buildings[j].y);
                            if (dist < min_distance) {
                                min_distance = dist;
                                closest_module = j;
                            }
                        }
                    }
                    
                    // 가장 가까운 모듈에 연결
                    if (closest_module != -1) {
                        int landing_id = landing->id;
                        int module_id = buildings[closest_module].id;
                        
                        if (can_build_tube(landing_id, module_id)) {
                            int cost = calculate_tube_cost(landing_id, module_id);
                            if (cost <= resources && cost > 0) {
                                char tube_command[50];
                                sprintf(tube_command, "%sTUBE %d %d;", actions[0] ? "" : "", landing_id, module_id);
                                strcat(actions, tube_command);
                                resources -= cost;
                                
                                // 로컬 데이터 업데이트
                                Route* route = &routes[route_count++];
                                route->building1 = landing_id;
                                route->building2 = module_id;
                                route->capacity = 1;
                                route->length = min_distance;
                                
                                // 건물 인덱스 찾기
                                int landing_idx = find_building_index(landing_id);
                                int module_idx = find_building_index(module_id);
                                
                                if (landing_idx != -1 && module_idx != -1) {
                                    buildings[landing_idx].connected_routes[buildings[landing_idx].route_count++] = route_count - 1;
                                    buildings[module_idx].connected_routes[buildings[module_idx].route_count++] = route_count - 1;
                                }
                            }
                        }
                    }
                }
            }
        }
        
        // 초기 포드 생성 - 비용 확인
        if (resources >= POD_COST) {
            char pod_command[100];
            
            // 가장 첫 번째 착륙장에 포드 생성
            if (landing_pad_count > 0 && buildings[landing_pads[0]].route_count > 0) {
                int landing_id = buildings[landing_pads[0]].id;
                int route_idx = buildings[landing_pads[0]].connected_routes[0];
                int module_id = (routes[route_idx].building1 == landing_id) ? 
                                routes[route_idx].building2 : routes[route_idx].building1;
                
                sprintf(pod_command, "%sPOD %d %d %d %d %d", actions[0] ? ";" : "", 1, landing_id, module_id, landing_id, module_id);
                strcat(actions, pod_command);
                resources -= POD_COST;
            }
        }
    } else {
        // 이후 달: 네트워크 확장 및 업그레이드
        
        // 새 건물들을 기존 네트워크에 연결 - 비용 확인 후 연결
        for (int i = 0; i < building_count && resources > 0; i++) {
            if (buildings[i].route_count == 0) {
                // 가장 가까운 건물 찾기
                int closest_building = -1;
                double min_distance = 1e9;
                
                for (int j = 0; j < building_count; j++) {
                    if (i != j && buildings[j].route_count > 0) {
                        double dist = calculate_distance(buildings[i].x, buildings[i].y, buildings[j].x, buildings[j].y);
                        if (dist < min_distance) {
                            min_distance = dist;
                            closest_building = j;
                        }
                    }
                }
                
                // 연결 시도 전 비용 확인
                if (closest_building != -1) {
                    int id1 = buildings[i].id;
                    int id2 = buildings[closest_building].id;
                    
                    if (can_build_tube(id1, id2)) {
                        int cost = calculate_tube_cost(id1, id2);
                        if (cost <= resources && cost > 0) {
                            char command[50];
                            sprintf(command, "%sTUBE %d %d", actions[0] ? ";" : "", id1, id2);
                            strcat(actions, command);
                            resources -= cost;
                        }
                    }
                }
            }
        }
        
        // 혼잡한 튜브 업그레이드 - 비용 계산 확인 후 업그레이드
        int upgrades_done = 0; // 각 턴에 업그레이드 수 제한
        
        for (int i = 0; i < route_count && resources > 0 && upgrades_done < 2; i++) {
            if (routes[i].capacity > 0 && routes[i].capacity < 3) {  // 용량이 3 미만인 튜브만 업그레이드
                int base_cost = calculate_tube_cost(routes[i].building1, routes[i].building2);
                int upgrade_cost = base_cost * (routes[i].capacity + 1);
                
                // 충분한 자원이 있는 경우에만 업그레이드
                if (upgrade_cost <= resources && upgrade_cost > 0) {
                    char command[50];
                    sprintf(command, "%sUPGRADE %d %d", actions[0] ? ";" : "", routes[i].building1, routes[i].building2);
                    strcat(actions, command);
                    resources -= upgrade_cost;
                    upgrades_done++;
                }
            }
        }
        
        // 중요 경로에 텔레포터 설치 - 충분한 자원 확인
        if (resources >= TELEPORT_COST && month >= 5) {
            // 가장 멀리 떨어진 착륙장-모듈 쌍 찾기
            int landing_pad_id = -1;
            int module_id = -1;
            double max_distance = 0;
            
            for (int i = 0; i < building_count; i++) {
                if (buildings[i].type == LANDING_PAD && !buildings[i].has_teleporter) {
                    for (int j = 0; j < building_count; j++) {
                        if (buildings[j].type > 0 && !buildings[j].has_teleporter) {  // 모듈
                            double dist = calculate_distance(buildings[i].x, buildings[i].y, buildings[j].x, buildings[j].y);
                            if (dist > max_distance) {
                                max_distance = dist;
                                landing_pad_id = buildings[i].id;
                                module_id = buildings[j].id;
                            }
                        }
                    }
                }
            }
            
            // 텔레포터 설치
            if (landing_pad_id != -1 && module_id != -1) {
                char command[50];
                sprintf(command, "%sTELEPORT %d %d", actions[0] ? ";" : "", landing_pad_id, module_id);
                strcat(actions, command);
                resources -= TELEPORT_COST;
                
                // 데이터 업데이트
                for (int i = 0; i < building_count; i++) {
                    if (buildings[i].id == landing_pad_id || buildings[i].id == module_id) {
                        buildings[i].has_teleporter = true;
                    }
                }
            }
        }
        
        // 추가 포드 생성 - 각 턴에 생성할 포드 수 제한
        int pods_created = 0;
        while (pods_created < 2 && resources >= POD_COST) {
            // 가장 활용도가 높은 튜브 찾기
            if (route_count > 0) {
                int route_idx = pod_count % route_count;  // 단순히 고르게 분배
                int b1_id = routes[route_idx].building1;
                int b2_id = routes[route_idx].building2;
                
                // 건물 ID가 실제 존재하는지 확인
                int b1_idx = find_building_index(b1_id);
                int b2_idx = find_building_index(b2_id);
                
                if (b1_idx != -1 && b2_idx != -1) {
                    char command[100];
                    sprintf(command, "%sPOD %d %d %d %d %d", 
                           actions[0] ? ";" : "", pod_count + 1, b1_id, b2_id, b1_id, b2_id);
                    strcat(actions, command);
                    resources -= POD_COST;
                    pod_count++;
                    pods_created++;
                }
            } else {
                break;
            }
        }
    }
    
    // 디버깅용 - 자원 사용량 확인
    fprintf(stderr, "Month %d: Resources before=%d, after=%d, used=%d\n", 
            month, original_resources, resources, original_resources - resources);
    
    // 명령 실행 (또는 WAIT)
    if (actions[0]) {
        printf("%s\n", actions);
    } else {
        printf("WAIT\n");
    }
    
    month++;
}

int main()
{
    // 전역 변수 초기화
    building_count = 0;
    route_count = 0;
    pod_count = 0;
    astronaut_count = 0;
    month = 0;
    total_score = 0;
    
    for (int i = 0; i < MAX_BUILDINGS; i++) {
        buildings_with_arrivals[i] = 0;
    }
    
    // 게임 루프
    while (1) {
        int resources;
        scanf("%d", &resources);
        
        // 디버깅용 - 각 턴마다 자원 확인
        fprintf(stderr, "Starting month %d with %d resources\n", month, resources);
        
        // 이전 달이 끝났으면 이자 적용 (첫 달 제외)
        if (month > 0) {
            apply_interest(&resources);
        }
        
        // 매월 데이터 초기화
        initialize_monthly_data();
        
        int num_travel_routes;
        scanf("%d", &num_travel_routes);
        
        // 각 건물의 route_count 초기화
        for (int i = 0; i < building_count; i++) {
            buildings[i].route_count = 0;
        }
        
        route_count = 0;
        
        for (int i = 0; i < num_travel_routes; i++) {
            int building_id_1;
            int building_id_2;
            int capacity;
            scanf("%d%d%d", &building_id_1, &building_id_2, &capacity);
            
            // 경로 정보 저장
            routes[route_count].building1 = building_id_1;
            routes[route_count].building2 = building_id_2;
            routes[route_count].capacity = capacity;
            
            // 길이 계산
            int b1_idx = find_building_index(building_id_1);
            int b2_idx = find_building_index(building_id_2);
            
            if (b1_idx != -1 && b2_idx != -1) {
                routes[route_count].length = calculate_distance(
                    buildings[b1_idx].x, buildings[b1_idx].y,
                    buildings[b2_idx].x, buildings[b2_idx].y
                );
                
                // 건물의 연결 정보 업데이트
                if (buildings[b1_idx].route_count < 5) {
                    buildings[b1_idx].connected_routes[buildings[b1_idx].route_count++] = route_count;
                }
                if (buildings[b2_idx].route_count < 5) {
                    buildings[b2_idx].connected_routes[buildings[b2_idx].route_count++] = route_count;
                }
            } else {
                // 건물을 찾지 못한 경우 길이 0으로 설정
                routes[route_count].length = 0;
            }
            
            route_count++;
        }
        
        int num_pods;
        scanf("%d", &num_pods); fgetc(stdin);
        pod_count = 0;
        
        for (int i = 0; i < num_pods; i++) {
            char pod_properties[201];
            scanf("%[^\n]", pod_properties); fgetc(stdin);
            parse_pod_properties(pod_properties);
        }
        
        int num_new_buildings;
        scanf("%d", &num_new_buildings); fgetc(stdin);
        
        for (int i = 0; i < num_new_buildings; i++) {
            char building_properties[501];
            scanf("%[^\n]", building_properties); fgetc(stdin);
            parse_building_properties(building_properties);
        }
        
        // 새 우주비행사 데이터 구성 (착륙장 파싱 시)
        for (int i = 0; i < building_count; i++) {
            if (buildings[i].type == LANDING_PAD) {
                for (int type = 1; type <= MODULE_TYPE_COUNT; type++) {
                    for (int j = 0; j < buildings[i].astronaut_count[type]; j++) {
                        astronauts[astronaut_count].id = astronaut_count;
                        astronauts[astronaut_count].type = type;
                        astronauts[astronaut_count].current_building = buildings[i].id;
                        astronauts[astronaut_count].target_module = -1;  // 목표 모듈은 동적으로 결정
                        astronauts[astronaut_count].pod_id = -1;
                        astronauts[astronaut_count].arrival_day = 0;
                        astronauts[astronaut_count].arrived = false;
                        astronaut_count++;
                    }
                }
            }
        }
        
        // 게임 전략 실행
        execute_strategy(resources);
        
        // 우주비행사 이동 시뮬레이션 (텔레포터→포드 할당→우주비행사 할당→이동)
        simulate_astronaut_movement();
    }
    
    return 0;
}
