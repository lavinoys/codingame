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

// 전역 변수
Building buildings[MAX_BUILDINGS];
Route routes[MAX_TUBES];
Pod pods[MAX_PODS];
int building_count = 0;
int route_count = 0;
int pod_count = 0;
int month = 0;

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

// 튜브 비용 계산
int calculate_tube_cost(int building_id1, int building_id2) {
    Building *b1 = &buildings[building_id1];
    Building *b2 = &buildings[building_id2];
    double distance = calculate_distance(b1->x, b1->y, b2->x, b2->y);
    
    // 0.1km당 1 자원, 내림
    return (int)(distance / 100);
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
        for (int i = 0; i < astronaut_count && i + 5 < count; i++) {
            int astronaut_type = values[i + 5];
            buildings[id].astronaut_count[astronaut_type]++;
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

// 게임 전략 실행
void execute_strategy(int resources) {
    char actions[1024] = "";
    
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
        for (int i = 0; i < landing_pad_count && resources > 1000; i++) {
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
                            if (cost <= resources) {
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
                                
                                buildings[landing_pads[i]].connected_routes[buildings[landing_pads[i]].route_count++] = route_count - 1;
                                buildings[closest_module].connected_routes[buildings[closest_module].route_count++] = route_count - 1;
                            }
                        }
                    }
                }
            }
        }
        
        // 초기 포드 생성
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
        // 새 건물들을 기존 네트워크에 연결
        for (int i = 0; i < building_count && resources > 1000; i++) {
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
                
                // 연결 시도
                if (closest_building != -1) {
                    int id1 = buildings[i].id;
                    int id2 = buildings[closest_building].id;
                    
                    if (can_build_tube(id1, id2)) {
                        int cost = calculate_tube_cost(id1, id2);
                        if (cost <= resources) {
                            char command[50];
                            sprintf(command, "%sTUBE %d %d", actions[0] ? ";" : "", id1, id2);
                            strcat(actions, command);
                            resources -= cost;
                        }
                    }
                }
            }
        }
        
        // 혼잡한 튜브 업그레이드
        for (int i = 0; i < route_count && resources > 1000; i++) {
            if (routes[i].capacity > 0 && routes[i].capacity < 3) {  // 용량이 3 미만인 튜브만 업그레이드
                int cost = calculate_tube_cost(routes[i].building1, routes[i].building2) * (routes[i].capacity + 1);
                if (cost <= resources) {
                    char command[50];
                    sprintf(command, "%sUPGRADE %d %d", actions[0] ? ";" : "", routes[i].building1, routes[i].building2);
                    strcat(actions, command);
                    resources -= cost;
                }
            }
        }
        
        // 중요 경로에 텔레포터 설치
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
        
        // 추가 포드 생성
        while (pod_count < 5 && resources >= POD_COST) {
            // 가장 활용도가 높은 튜브 찾기
            if (route_count > 0) {
                int route_idx = pod_count % route_count;  // 단순히 고르게 분배
                int b1 = routes[route_idx].building1;
                int b2 = routes[route_idx].building2;
                
                char command[100];
                sprintf(command, "%sPOD %d %d %d %d %d", 
                       actions[0] ? ";" : "", pod_count + 1, b1, b2, b1, b2);
                strcat(actions, command);
                resources -= POD_COST;
                pod_count++;
            } else {
                break;
            }
        }
    }
    
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
    // 게임 루프
    while (1) {
        int resources;
        scanf("%d", &resources);
        int num_travel_routes;
        scanf("%d", &num_travel_routes);
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
            for (int j = 0; j < building_count; j++) {
                if (buildings[j].id == building_id_1) {
                    for (int k = 0; k < building_count; k++) {
                        if (buildings[k].id == building_id_2) {
                            routes[route_count].length = calculate_distance(
                                buildings[j].x, buildings[j].y,
                                buildings[k].x, buildings[k].y
                            );
                            break;
                        }
                    }
                    break;
                }
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
        
        // 게임 전략 실행
        execute_strategy(resources);
    }
    
    return 0;
}
