#include <stdlib.h>
#include <stdio.h>
#include <string.h>
#include <stdbool.h>
#include <math.h>

#define MAX_CHECKPOINTS 8
#define PI 3.14159265358979323846

// 체크포인트 위치 저장
int checkpoint_x[MAX_CHECKPOINTS];
int checkpoint_y[MAX_CHECKPOINTS];
int checkpoint_count;
int laps;

// 부스트와 쉴드 상태 관리
bool boost_available = true;
int shield_cooldown[2] = {0, 0}; // 쉴드가 활성화된 턴 수 카운트

// 쉴드 활성화 중에 사용할 목표 위치 저장
int shield_target_x[2] = {0, 0};
int shield_target_y[2] = {0, 0};

// 거리 계산 함수
double distance(int x1, int y1, int x2, int y2) {
    return sqrt((x1 - x2) * (x1 - x2) + (y1 - y2) * (y1 - y2));
}

// 두 벡터 간 각도 계산
double angle_between(int x1, int y1, int x2, int y2) {
    return atan2(y2 - y1, x2 - x1) * 180 / PI;
}

int main()
{
    scanf("%d", &laps);
    scanf("%d", &checkpoint_count);
    
    // 체크포인트 위치 저장
    for (int i = 0; i < checkpoint_count; i++) {
        scanf("%d%d", &checkpoint_x[i], &checkpoint_y[i]);
    }

    // game loop
    while (1) {
        // 내 포드 정보
        int x[2], y[2], vx[2], vy[2], angle[2], next_check_point_id[2];
        for (int i = 0; i < 2; i++) {
            scanf("%d%d%d%d%d%d", &x[i], &y[i], &vx[i], &vy[i], &angle[i], &next_check_point_id[i]);
        }
        
        // 상대방 포드 정보
        int x_op[2], y_op[2], vx_op[2], vy_op[2], angle_op[2], next_check_point_id_op[2];
        for (int i = 0; i < 2; i++) {
            scanf("%d%d%d%d%d%d", &x_op[i], &y_op[i], &vx_op[i], &vy_op[i], &angle_op[i], &next_check_point_id_op[i]);
        }
        
        for (int i = 0; i < 2; i++) {
            // 쉴드가 활성화된 경우
            if (shield_cooldown[i] > 0) {
                shield_cooldown[i]--;
                // 3턴 동안 계속해서 SHIELD 명령을 내보냄
                if (i == 0) {
                    printf("%d %d SHIELD [RACER] SHIELD\n", shield_target_x[i], shield_target_y[i]);
                } else {
                    printf("%d %d SHIELD [BLOCKER] SHIELD\n", shield_target_x[i], shield_target_y[i]);
                }
                continue; // 다음 포드로 넘어감
            }
            
            // 다음 체크포인트 좌표
            int target_x = checkpoint_x[next_check_point_id[i]];
            int target_y = checkpoint_y[next_check_point_id[i]];
            
            // 다음 체크포인트와의 거리
            double dist = distance(x[i], y[i], target_x, target_y);
            
            // 다음 체크포인트 이후 체크포인트
            int next_next_checkpoint_id = (next_check_point_id[i] + 1) % checkpoint_count;
            int next_next_x = checkpoint_x[next_next_checkpoint_id];
            int next_next_y = checkpoint_y[next_next_checkpoint_id];
            
            // 첫 번째 포드는 레이서, 두 번째 포드는 블로커로 전략 분리
            if (i == 0) { // 레이서 (첫 번째 포드)
                // 체크포인트 간 최적 경로 계산 (레이싱 라인 최적화)
                double current_to_next_dist = distance(x[i], y[i], target_x, target_y);
                double next_to_next_next_dist = distance(target_x, target_y, next_next_x, next_next_y);
                
                // 체크포인트 간 각도 계산
                double checkpoint_angle = angle_between(target_x, target_y, next_next_x, next_next_y);
                double my_angle_to_target = angle_between(x[i], y[i], target_x, target_y);
                double angle_diff = fabs(my_angle_to_target - checkpoint_angle);
                
                // 목표 지점 조정 (체크포인트에 가까워지면 다음 체크포인트로 일부 회전)
                // 가까울수록 더 많이 다음 체크포인트 방향으로 조정
                if (dist < 1500) {
                    double adjustment_factor = 0.2 + ((1500 - dist) / 1500) * 0.3; // 0.2~0.5 사이 조정
                    target_x = target_x + (next_next_x - target_x) * adjustment_factor;
                    target_y = target_y + (next_next_y - target_y) * adjustment_factor;
                }
                
                // 속도 벡터를 활용한 목표 보정
                // 높은 속도에서는 목표 지점을 약간 앞서 조준
                double speed = sqrt(vx[i]*vx[i] + vy[i]*vy[i]);
                if (speed > 200) {
                    // 현재 진행 방향으로 약간의 관성 추가
                    target_x += vx[i] / 10;
                    target_y += vy[i] / 10;
                }
                
                int thrust = 100;
                
                // 회전이 큰 경우 감속
                angle_diff = fabs(angle[i] - angle_between(x[i], y[i], target_x, target_y));
                if (angle_diff > 90) {
                    thrust = 40; // 더 강한 감속 (이전: 50)
                } else if (angle_diff > 45) {
                    thrust = 70; // 중간 정도 감속
                }
                
                // 체크포인트에 가까우면 감속: 더 정교한 거리 기반 감속
                if (dist < 600) {
                    thrust = 60;
                } else if (dist < 1000 && speed > 400) {
                    // 속도가 빠른 상태에서 체크포인트에 접근할 때 약간 감속
                    thrust = 80;
                }
                
                // 충돌 감지 및 예측
                bool collision_imminent = false;
                double min_collision_dist = 9999999;
                int colliding_opponent = -1;
                
                for (int j = 0; j < 2; j++) {
                    double collision_dist = distance(x[i], y[i], x_op[j], y_op[j]);
                    // 상대 포드와의 상대 속도 계산
                    double rel_vx = vx[i] - vx_op[j];
                    double rel_vy = vy[i] - vy_op[j];
                    double rel_speed = sqrt(rel_vx*rel_vx + rel_vy*rel_vy);
                    
                    // 충돌 방향 벡터와 진행 방향 벡터 사이의 각도 계산
                    double collision_angle = angle_between(0, 0, rel_vx, rel_vy);
                    double movement_angle = angle_between(0, 0, vx[i], vy[i]);
                    double collision_angle_diff = fabs(collision_angle - movement_angle);
                    
                    // 정면 충돌이 예상되는 경우 (각도 차이가 작을수록 정면 충돌)
                    if (collision_dist < 900 && rel_speed > 250 && collision_angle_diff < 60) {
                        collision_imminent = true;
                        if (collision_dist < min_collision_dist) {
                            min_collision_dist = collision_dist;
                            colliding_opponent = j;
                        }
                    }
                }
                
                // 충돌이 임박한 경우 쉴드 사용
                if (collision_imminent && shield_cooldown[i] == 0) {
                    // 쉴드 활성화 시 목표 위치 저장
                    shield_target_x[i] = target_x;
                    shield_target_y[i] = target_y;
                    shield_cooldown[i] = 3; // 쉴드 3턴 유지
                    printf("%d %d SHIELD [RACER] SHIELD\n", target_x, target_y);
                }
                // 부스트 사용 조건 개선
                else if (boost_available && dist > 3500 && angle_diff < 5 && 
                         next_to_next_next_dist > 3000) { // 다음 체크포인트 이후 거리도 고려
                    printf("%d %d BOOST [RACER] BOOST\n", target_x, target_y);
                    boost_available = false;
                }
                else {
                    printf("%d %d %d [RACER] thrust: %d angle_diff: %.1f\n", 
                           target_x, target_y, thrust, thrust, angle_diff);
                }
            }
            else { // 블로커 (두 번째 포드)
                // 상대방의 리더 포드 식별 (경주 진행도 기준)
                int lead_opponent = 0;
                
                // 체크포인트 진행도를 비교하여 더 앞선 상대 포드 선택
                if (next_check_point_id_op[0] != next_check_point_id_op[1]) {
                    lead_opponent = (next_check_point_id_op[0] > next_check_point_id_op[1]) ? 0 : 1;
                } else {
                    // 같은 체크포인트를 향하고 있는 경우 거리로 판단
                    double dist0 = distance(x_op[0], y_op[0], checkpoint_x[next_check_point_id_op[0]], checkpoint_y[next_check_point_id_op[0]]);
                    double dist1 = distance(x_op[1], y_op[1], checkpoint_x[next_check_point_id_op[1]], checkpoint_y[next_check_point_id_op[1]]);
                    lead_opponent = (dist0 < dist1) ? 0 : 1;
                }
                
                // 리더 상대의 다음 위치 예측
                int intercept_x = x_op[lead_opponent] + vx_op[lead_opponent] * 1.5; // 1.5턴 후 위치 예측
                int intercept_y = y_op[lead_opponent] + vy_op[lead_opponent] * 1.5;
                
                // 상대가 향하는 체크포인트 위치
                int op_target_x = checkpoint_x[next_check_point_id_op[lead_opponent]];
                int op_target_y = checkpoint_y[next_check_point_id_op[lead_opponent]];
                
                // 상대방과 체크포인트 사이의 경로 차단 전략
                double op_to_checkpoint_dist = distance(x_op[lead_opponent], y_op[lead_opponent], op_target_x, op_target_y);
                
                // 상대가 체크포인트에 가까워지면 경로 차단 지점 조정
                if (op_to_checkpoint_dist < 2000) {
                    // 상대와 체크포인트 사이의 차단 지점 계산
                    double ratio = 0.6; // 상대와 체크포인트 사이 60% 지점에서 차단
                    intercept_x = x_op[lead_opponent] + (op_target_x - x_op[lead_opponent]) * ratio;
                    intercept_y = y_op[lead_opponent] + (op_target_y - y_op[lead_opponent]) * ratio;
                }
                
                int thrust = 100;
                double collision_dist = distance(x[i], y[i], x_op[lead_opponent], y_op[lead_opponent]);
                double my_speed = sqrt(vx[i]*vx[i] + vy[i]*vy[i]);
                
                // 블로커의 각도 차이 계산
                double blocker_angle_diff = fabs(angle[i] - angle_between(x[i], y[i], intercept_x, intercept_y));
                
                // 각도에 따른 추력 조정
                if (blocker_angle_diff > 90) {
                    thrust = 50;
                } else if (blocker_angle_diff > 45) {
                    thrust = 80;
                }
                
                // 상대방이 가까이 있고 쉴드가 사용 가능하면 쉴드 사용
                if (collision_dist < 850 && shield_cooldown[i] == 0 && my_speed > 200) {
                    // 쉴드 활성화 시 목표 위치 저장
                    shield_target_x[i] = intercept_x;
                    shield_target_y[i] = intercept_y;
                    shield_cooldown[i] = 3; // 쉴드 3턴 유지
                    printf("%d %d SHIELD [BLOCKER] SHIELD\n", intercept_x, intercept_y);
                } else {
                    printf("%d %d %d [BLOCKER] thrust: %d target: %d\n", 
                          intercept_x, intercept_y, thrust, thrust, lead_opponent);
                }
            }
        }
    }

    return 0;
}
