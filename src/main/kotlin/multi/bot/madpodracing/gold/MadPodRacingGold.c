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
                // 목표 지점 조정 (체크포인트에 가까워지면 다음 체크포인트로 일부 회전)
                if (dist < 1500) {
                    target_x = target_x + (next_next_x - target_x) / 5;
                    target_y = target_y + (next_next_y - target_y) / 5;
                }
                
                int thrust = 100;
                
                // 쉴드가 활성화된 경우
                if (shield_cooldown[i] > 0) {
                    shield_cooldown[i]--;
                    thrust = 0; // 엔진 사용 불가
                    printf("%d %d %d\n", target_x, target_y, thrust);
                    continue;
                }
                
                // 충돌 감지
                bool collision_imminent = false;
                double min_collision_dist = 9999999;
                
                for (int j = 0; j < 2; j++) {
                    double collision_dist = distance(x[i], y[i], x_op[j], y_op[j]);
                    double rel_speed = sqrt(pow(vx[i] - vx_op[j], 2) + pow(vy[i] - vy_op[j], 2));
                    
                    // 충돌이 임박한 경우
                    if (collision_dist < 800 && rel_speed > 250) {
                        collision_imminent = true;
                        if (collision_dist < min_collision_dist) {
                            min_collision_dist = collision_dist;
                        }
                    }
                }
                
                // 회전이 큰 경우 감속
                double angle_diff = fabs(angle[i] - angle_between(x[i], y[i], target_x, target_y));
                if (angle_diff > 90) {
                    thrust = 50;
                }
                
                // 체크포인트에 가까우면 감속
                if (dist < 600) {
                    thrust = 60;
                }
                
                // 충돌이 임박한 경우 쉴드 사용
                if (collision_imminent && shield_cooldown[i] == 0) {
                    printf("%d %d SHIELD\n", target_x, target_y);
                    shield_cooldown[i] = 3; // 쉴드 3턴 유지
                }
                // 부스트 사용
                else if (boost_available && dist > 4000 && angle_diff < 10) {
                    printf("%d %d BOOST\n", target_x, target_y);
                    boost_available = false;
                }
                else {
                    printf("%d %d %d\n", target_x, target_y, thrust);
                }
            }
            else { // 블로커 (두 번째 포드)
                // 상대방의 리더 포드가 향하는 체크포인트 차단 전략
                int lead_opponent = (next_check_point_id_op[0] >= next_check_point_id_op[1]) ? 0 : 1;
                int intercept_x = x_op[lead_opponent];
                int intercept_y = y_op[lead_opponent];
                
                // 쉴드가 활성화된 경우
                if (shield_cooldown[i] > 0) {
                    shield_cooldown[i]--;
                    printf("%d %d 0\n", intercept_x, intercept_y);
                    continue;
                }
                
                int thrust = 100;
                double collision_dist = distance(x[i], y[i], x_op[lead_opponent], y_op[lead_opponent]);
                
                // 상대방이 가까이 있고 쉴드가 사용 가능하면 쉴드 사용
                if (collision_dist < 850 && shield_cooldown[i] == 0) {
                    printf("%d %d SHIELD\n", intercept_x, intercept_y);
                    shield_cooldown[i] = 3; // 쉴드 3턴 유지
                } else {
                    printf("%d %d %d\n", intercept_x, intercept_y, thrust);
                }
            }
        }
    }

    return 0;
}