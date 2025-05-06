#include <stdlib.h>
#include <stdio.h>
#include <string.h>
#include <stdbool.h>
#include <math.h>

#define MAX_CHECKPOINTS 8
#define PI 3.14159265358979323846
#define FRICTION 0.85
#define CHECKPOINT_RADIUS 600
#define POD_RADIUS 400
#define MAX_ROTATION_PER_TURN 18.0

// 체크포인트 위치 저장
int checkpoint_x[MAX_CHECKPOINTS];
int checkpoint_y[MAX_CHECKPOINTS];
int checkpoint_count;
int laps;
int current_lap[2] = {0, 0};
int last_checkpoint_id[2] = {0, 0};

// 부스트와 쉴드 상태 관리
bool boost_available = true;
int shield_cooldown[2] = {0, 0}; // 쉴드가 활성화된 턴 수 카운트

// 쉴드 활성화 중에 사용할 목표 위치 저장
int shield_target_x[2] = {0, 0};
int shield_target_y[2] = {0, 0};

// 트랙 정보 저장 (최장 직선 구간 식별용)
typedef struct {
    int start_cp;
    int end_cp;
    double distance;
    double angle_diff;
} TrackSegment;

TrackSegment track_segments[MAX_CHECKPOINTS];
int best_boost_segment = -1;

// 거리 계산 함수
double distance(int x1, int y1, int x2, int y2) {
    return sqrt((x1 - x2) * (x1 - x2) + (y1 - y2) * (y1 - y2));
}

// 두 벡터 간 각도 계산
double angle_between(int x1, int y1, int x2, int y2) {
    return atan2(y2 - y1, x2 - x1) * 180 / PI;
}

// 두 각도 사이의 최소 차이 계산 (0-360 범위에서)
double min_angle_diff(double angle1, double angle2) {
    // 각도를 0-360 범위로 정규화
    angle1 = fmod(angle1, 360);
    if (angle1 < 0) angle1 += 360;
    
    angle2 = fmod(angle2, 360);
    if (angle2 < 0) angle2 += 360;
    
    // 두 각도 사이의 최소 차이 계산
    double diff = fabs(angle1 - angle2);
    if (diff > 180) {
        diff = 360 - diff;
    }
    return diff;
}

// 미래 위치 예측 함수 - 더 정확한 물리 시뮬레이션
void predict_future_position(int x, int y, int vx, int vy, int angle, int thrust, int steps, 
                            int* future_x, int* future_y, int* future_vx, int* future_vy) {
    double fx = x;
    double fy = y;
    double fvx = vx;
    double fvy = vy;
    double fangle = angle;
    
    // 각도를 라디안으로 변환
    double angle_rad = fangle * PI / 180.0;
    
    for (int i = 0; i < steps; i++) {
        // 추력 적용
        fvx += cos(angle_rad) * thrust;
        fvy += sin(angle_rad) * thrust;
        
        // 위치 업데이트
        fx += fvx;
        fy += fvy;
        
        // 마찰력 적용
        fvx *= FRICTION;
        fvy *= FRICTION;
    }
    
    *future_x = (int)round(fx);
    *future_y = (int)round(fy);
    *future_vx = (int)fvx;
    *future_vy = (int)fvy;
}

// 두 포드 간의 충돌 예측
bool predict_collision(int x1, int y1, int vx1, int vy1, int x2, int y2, int vx2, int vy2, int steps, double* collision_time) {
    for (int i = 1; i <= steps; i++) {
        // 각 포드의 미래 위치 계산
        double pred_x1 = x1 + vx1 * i * FRICTION;
        double pred_y1 = y1 + vy1 * i * FRICTION;
        double pred_x2 = x2 + vx2 * i * FRICTION;
        double pred_y2 = y2 + vy2 * i * FRICTION;
        
        // 두 포드 사이의 거리
        double dist = sqrt((pred_x1 - pred_x2) * (pred_x1 - pred_x2) + 
                          (pred_y1 - pred_y2) * (pred_y1 - pred_y2));
        
        // 충돌 발생 시
        if (dist < 2 * POD_RADIUS) {
            *collision_time = i;
            return true;
        }
    }
    return false;
}

// 트랙 분석 - 최적 부스트 지점 찾기
void analyze_track() {
    double max_straight_distance = 0;
    int best_segment = -1;
    
    for (int i = 0; i < checkpoint_count; i++) {
        int next_i = (i + 1) % checkpoint_count;
        double segment_dist = distance(checkpoint_x[i], checkpoint_y[i], 
                                     checkpoint_x[next_i], checkpoint_y[next_i]);
        
        // 이전 체크포인트에서 현재 체크포인트로의 방향
        int prev_i = (i - 1 + checkpoint_count) % checkpoint_count;
        double angle1 = angle_between(checkpoint_x[prev_i], checkpoint_y[prev_i], 
                                    checkpoint_x[i], checkpoint_y[i]);
        
        // 현재 체크포인트에서 다음 체크포인트로의 방향
        double angle2 = angle_between(checkpoint_x[i], checkpoint_y[i], 
                                    checkpoint_x[next_i], checkpoint_y[next_i]);
        
        // 방향 전환의 각도
        double turn_angle = min_angle_diff(angle1, angle2);
        
        // 세그먼트 정보 저장
        track_segments[i].start_cp = i;
        track_segments[i].end_cp = next_i;
        track_segments[i].distance = segment_dist;
        track_segments[i].angle_diff = turn_angle;
        
        // 직선 세그먼트를 찾습니다 (적은 방향 전환과 긴 거리)
        double straight_score = segment_dist * (1 - turn_angle / 180.0);
        if (straight_score > max_straight_distance) {
            max_straight_distance = straight_score;
            best_segment = i;
        }
    }
    
    // 최적 부스트 세그먼트 설정
    best_boost_segment = best_segment;
    fprintf(stderr, "Best boost segment: %d->%d, score: %.1f\n", 
            best_segment, (best_segment + 1) % checkpoint_count, max_straight_distance);
}

// 레이싱 라인 최적화 - 체크포인트 통과를 위한 최적 지점 계산
void optimize_racing_line(int current_x, int current_y, int current_vx, int current_vy,
                         int cp_x, int cp_y, int next_cp_x, int next_cp_y, 
                         int* target_x, int* target_y) {
    // 현재 체크포인트까지 거리
    double dist_to_cp = distance(current_x, current_y, cp_x, cp_y);
    
    // 체크포인트 간 벡터 계산
    double cp_vector_x = next_cp_x - cp_x;
    double cp_vector_y = next_cp_y - cp_y;
    double cp_vector_length = sqrt(cp_vector_x * cp_vector_x + cp_vector_y * cp_vector_y);
    
    // 단위 벡터화
    double cp_unit_x = cp_vector_x / cp_vector_length;
    double cp_unit_y = cp_vector_y / cp_vector_length;
    
    // 현재 속도 벡터의 크기
    double speed = sqrt(current_vx * current_vx + current_vy * current_vy);
    
    // 현재 속도와 체크포인트 방향 벡터의 내적 (방향 유사성)
    double dot_product = 0;
    if (speed > 0) {
        dot_product = (current_vx * cp_unit_x + current_vy * cp_unit_y) / speed;
    }
    
    // 체크포인트에서 다음 체크포인트 방향으로 오프셋 계산 
    double offset = 0;
    if (dist_to_cp < CHECKPOINT_RADIUS * 2) {
        // 체크포인트에 가까울수록 다음 체크포인트 방향으로 더 많이 오프셋
        offset = (CHECKPOINT_RADIUS * 2 - dist_to_cp) / (CHECKPOINT_RADIUS * 2);
        // 속도가 체크포인트 방향과 일치할수록 오프셋이 더 큼
        offset *= (dot_product + 1) / 2; // dot_product 범위를 [0,1]로 정규화
        offset *= 400; // 최대 오프셋 크기
    }
    
    // 오프셋된 타겟 계산
    *target_x = cp_x + (int)(cp_unit_x * offset);
    *target_y = cp_y + (int)(cp_unit_y * offset);
}

// 드리프트 계산 - 속도와 방향 분석으로 최적 주행점 계산
void calculate_drift_target(int current_x, int current_y, int vx, int vy, int speed,
                          int cp_x, int cp_y, int next_cp_x, int next_cp_y,
                          double angle_diff, double next_angle_diff,
                          int* target_x, int* target_y) {
    
    // 현재 이동 방향과 목표 방향 간의 벡터 계산
    double movement_dir = atan2(vy, vx) * 180 / PI;
    double target_dir = angle_between(current_x, current_y, cp_x, cp_y);
    double dir_diff = min_angle_diff(movement_dir, target_dir);
    
    // 현재 체크포인트에서 다음 체크포인트로의 회전 방향 결정
    double cp_to_next_dir = angle_between(cp_x, cp_y, next_cp_x, next_cp_y);
    double turn_dir = min_angle_diff(target_dir, cp_to_next_dir);
    int turn_sign = 1;
    
    // 시계 방향 또는 반시계 방향 회전인지 확인
    double raw_diff = target_dir - cp_to_next_dir;
    if (raw_diff > 180) raw_diff -= 360;
    if (raw_diff < -180) raw_diff += 360;
    turn_sign = raw_diff > 0 ? -1 : 1;
    
    // 드리프트 적용 조건
    bool apply_drift = speed > 350 && next_angle_diff > 50;
    double dist_to_cp = distance(current_x, current_y, cp_x, cp_y);
    
    // 드리프트 계산
    if (apply_drift && dist_to_cp < 2500) {
        // 현재 체크포인트를 통과하기 위한 접선 계산
        double tangent_angle = target_dir + turn_sign * (90 - next_angle_diff/4);
        double drift_factor = 0.3 + (speed / 1000.0); // 속도에 비례하는 드리프트 강도
        
        if (dist_to_cp < 1000) {
            // 체크포인트에 가까울수록 다음 체크포인트 방향으로 더 많이 조정
            double blend = 1.0 - (dist_to_cp / 1000.0);
            tangent_angle = tangent_angle * (1-blend) + cp_to_next_dir * blend;
        }
        
        // 드리프트 각도로 목표 위치 조정
        double drift_distance = dist_to_cp * drift_factor;
        drift_distance = drift_distance > 800 ? 800 : drift_distance;
        
        *target_x = current_x + (int)(cos(tangent_angle * PI / 180.0) * drift_distance);
        *target_y = current_y + (int)(sin(tangent_angle * PI / 180.0) * drift_distance);
        
        fprintf(stderr, "DRIFT: 속도:%d 각도차:%.1f 드리프트강도:%.2f\n", 
                speed, next_angle_diff, drift_factor);
    } else {
        // 드리프트가 필요 없으면 원래 타겟 사용
        *target_x = cp_x;
        *target_y = cp_y;
    }
}

int main()
{
    scanf("%d", &laps);
    scanf("%d", &checkpoint_count);
    
    // 체크포인트 위치 저장
    for (int i = 0; i < checkpoint_count; i++) {
        scanf("%d%d", &checkpoint_x[i], &checkpoint_y[i]);
    }

    // 트랙 분석 및 최적 부스트 구간 계산
    analyze_track();
    
    // game loop
    while (1) {
        // 내 포드 정보
        int x[2], y[2], vx[2], vy[2], angle[2], next_check_point_id[2];
        for (int i = 0; i < 2; i++) {
            scanf("%d%d%d%d%d%d", &x[i], &y[i], &vx[i], &vy[i], &angle[i], &next_check_point_id[i]);
            
            // 랩 카운트 업데이트
            if (next_check_point_id[i] == 0 && last_checkpoint_id[i] == checkpoint_count - 1) {
                current_lap[i]++;
                fprintf(stderr, "Pod %d completed lap %d\n", i, current_lap[i]);
            }
            last_checkpoint_id[i] = next_check_point_id[i];
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
                // 레이싱 라인 최적화
                optimize_racing_line(x[i], y[i], vx[i], vy[i], 
                               target_x, target_y, 
                               next_next_x, next_next_y,
                               &target_x, &target_y);
            
                // 정교한 추력 계산
                double target_angle = angle_between(x[i], y[i], target_x, target_y);
                double angle_diff = min_angle_diff(angle[i], target_angle);
                int thrust = 100; // 추력 변수 초기화
                
                // 다음 체크포인트로의 방향 전환 각도 계산
                double next_angle_diff = min_angle_diff(
                    angle_between(target_x, target_y, next_next_x, next_next_y),
                    angle_between(x[i], y[i], target_x, target_y)
                );
                
                // 현재 속도 계산
                double speed = sqrt(vx[i]*vx[i] + vy[i]*vy[i]);
                
                // 드리프트 주행 타겟 계산
                int drift_target_x, drift_target_y;
                calculate_drift_target(x[i], y[i], vx[i], vy[i], (int)speed,
                                      target_x, target_y, next_next_x, next_next_y,
                                      angle_diff, next_angle_diff,
                                      &drift_target_x, &drift_target_y);
                
                // 급격한 회전이 필요할 때는 드리프트 타겟 사용
                if (next_angle_diff > 50 && speed > 350) {
                    target_x = drift_target_x;
                    target_y = drift_target_y;
                    
                    // 드리프트 중 추력 조정 (관성 활용)
                    double dist_to_cp = distance(x[i], y[i], checkpoint_x[next_check_point_id[i]], checkpoint_y[next_check_point_id[i]]);
                    if (dist_to_cp < 1200) {
                        // 체크포인트에 가까울수록 추력 감소
                        thrust = (int)(70 - (1200 - dist_to_cp) / 20);
                        thrust = thrust < 30 ? 30 : thrust;
                    }
                    
                    // 드리프트 중 목표 방향 재계산
                    target_angle = angle_between(x[i], y[i], target_x, target_y);
                    angle_diff = min_angle_diff(angle[i], target_angle);
                }
                else {
                    // 회전 각도에 따른 연속적인 추력 조정
                    thrust = 100;
                    if (angle_diff > 90) {
                        thrust = 30; // 급격한 회전 시 최소 추력 증가
                    } else if (angle_diff > 0) {
                        // 0~90도 사이에서 각도에 비례한 추력 (부드러운 감소)
                        thrust = (int)(100 - angle_diff * 0.7);
                        thrust = thrust < 30 ? 30 : thrust; // 최소 추력 보장
                    }
                    
                    // 체크포인트 거리에 따른 조정
                    double dist = distance(x[i], y[i], target_x, target_y);
                    double breaking_distance = speed * 1.5; // 현재 속도에 비례하는 제동 거리
                    
                    if (dist < CHECKPOINT_RADIUS / 2) {
                        thrust = 50; // 체크포인트 내부에서는 감속
                    } else if (dist < breaking_distance && speed > 400) {
                        // 제동 거리 이내에서 속도를 줄이되, 속도에 따라 부드럽게 조정
                        double deceleration_factor = (dist / breaking_distance);
                        int reduced_thrust = (int)(100 * deceleration_factor * 0.7);
                        thrust = thrust < reduced_thrust ? thrust : reduced_thrust;
                    }
                    
                    // 급격한 회전 구간 감지 및 조기 감속
                    if (next_angle_diff > 80 && dist < 2000) {
                        thrust = (int)(thrust * (0.5 + 0.5 * (dist / 2000))); // 거리에 따른 점진적 감속
                        fprintf(stderr, "Reducing speed for sharp turn: %.1f degrees, thrust: %d\n", next_angle_diff, thrust);
                    }
                }

                // 충돌 예측 및 대응
                bool collision_imminent = false;
                double collision_time = 0;
                int colliding_opponent = -1;
                
                for (int j = 0; j < 2; j++) {
                    if (predict_collision(x[i], y[i], vx[i], vy[i], x_op[j], y_op[j], vx_op[j], vy_op[j], 5, &collision_time)) {
                        collision_imminent = true;
                        colliding_opponent = j;
                        break;
                    }
                }
                
                // 최종 랩 마지막 체크포인트 근처면 공격적 주행
                bool final_sprint = (current_lap[i] == laps - 1) && (next_check_point_id[i] == checkpoint_count - 1);
                if (final_sprint) {
                    fprintf(stderr, "RACER: Final sprint activated!\n");
                    thrust = 100; // 무조건 최대 추력
                }
                
                // 부스트 사용 전략 개선
                bool use_boost = boost_available && 
                               angle_diff < 5.0 && // 거의 직선 방향이어야 함
                               dist > 3000 && // 충분히 멀어야 함
                               speed < 600 && // 최고 속도에 도달하지 않았어야 함
                               !collision_imminent; // 충돌 예상 상황이 아니어야 함
                
                // 최적 부스트 세그먼트에 있는지 확인
                if (next_check_point_id[i] == best_boost_segment) {
                    use_boost = use_boost && (dist > track_segments[best_boost_segment].distance * 0.7);
                    fprintf(stderr, "On optimal boost segment: %d, eligible: %s\n", 
                            best_boost_segment, use_boost ? "YES" : "NO");
                }
                
                // 충돌이 임박한 경우 쉴드 사용
                if (collision_imminent && shield_cooldown[i] == 0 && collision_time < 2) {
                    // 쉴드 활성화
                    shield_target_x[i] = target_x;
                    shield_target_y[i] = target_y;
                    shield_cooldown[i] = 3; // 쉴드 3턴 유지
                    printf("%d %d SHIELD [RACER] 충돌회피! 시간:%.1f\n", target_x, target_y, collision_time);
                }
                // 부스트 사용
                else if (use_boost) { 
                    printf("%d %d BOOST [RACER] 부스트! 거리:%.0f 각도:%.1f\n", target_x, target_y, dist, angle_diff);
                    boost_available = false;
                }
                else {
                    printf("%d %d %d [RACER] 추진력:%d 각도:%.1f 속도:%.0f\n", 
                           target_x, target_y, thrust, thrust, angle_diff, speed);
                }
            }
            else { // 블로커 (두 번째 포드)
                // 상대방의 리더 포드 식별
                int lead_opponent = 0;
                
                // 랩 수로 리더 확인
                if (next_check_point_id_op[0] != next_check_point_id_op[1]) {
                    // 체크포인트 ID가 다르면 더 앞선 체크포인트에 있는 포드가 리더
                    lead_opponent = (next_check_point_id_op[0] > next_check_point_id_op[1]) ? 0 : 1;
                } else {
                    // 같은 체크포인트를 향하고 있는 경우 거리로 판단
                    double dist0 = distance(x_op[0], y_op[0], checkpoint_x[next_check_point_id_op[0]], checkpoint_y[next_check_point_id_op[0]]);
                    double dist1 = distance(x_op[1], y_op[1], checkpoint_x[next_check_point_id_op[1]], checkpoint_y[next_check_point_id_op[1]]);
                    lead_opponent = (dist0 < dist1) ? 0 : 1;
                }
                
                // 리더의 미래 위치 예측 (2턴 이후)
                int future_lead_x, future_lead_y, future_lead_vx, future_lead_vy;
                predict_future_position(
                    x_op[lead_opponent], y_op[lead_opponent], 
                    vx_op[lead_opponent], vy_op[lead_opponent],
                    angle_op[lead_opponent], 100, 2,
                    &future_lead_x, &future_lead_y, &future_lead_vx, &future_lead_vy
                );
                
                // 상대가 향하는 체크포인트
                int op_target_x = checkpoint_x[next_check_point_id_op[lead_opponent]];
                int op_target_y = checkpoint_y[next_check_point_id_op[lead_opponent]];
                
                // 상대방과 체크포인트 사이 경로 차단 지점 계산
                // 체크포인트에 가까워질수록 차단율 증가
                double op_to_cp_dist = distance(future_lead_x, future_lead_y, op_target_x, op_target_y);
                double blocking_ratio = 0.7; // 기본 차단 지점은 70% 지점
                
                if (op_to_cp_dist < 2000) {
                    blocking_ratio = 0.5 + (2000 - op_to_cp_dist) / 4000; // 더 가까울수록 더 가까운 지점에서 차단
                    blocking_ratio = blocking_ratio > 0.8 ? 0.8 : blocking_ratio; // 최대 80%
                }
                
                int intercept_x = future_lead_x + (int)((op_target_x - future_lead_x) * blocking_ratio);
                int intercept_y = future_lead_y + (int)((op_target_y - future_lead_y) * blocking_ratio);
                
                // 블로커의 각도 차이 계산
                double target_angle = angle_between(x[i], y[i], intercept_x, intercept_y);
                double blocker_angle_diff = min_angle_diff(angle[i], target_angle);
                
                // 추력 조정 - 더 부드럽게
                int thrust = 100;
                if (blocker_angle_diff > 90) {
                    thrust = 20;
                } else if (blocker_angle_diff > 0) {
                    thrust = (int)(100.0 * (1.0 - blocker_angle_diff / 100.0));
                    thrust = thrust < 20 ? 20 : thrust;
                }
                
                // 충돌 거리에 따른 추력 조정
                double collision_dist = distance(x[i], y[i], future_lead_x, future_lead_y);
                bool on_intercept_course = false;
                
                if (collision_dist < 1400) {
                    // 상대방 상대 속도 계산
                    double rel_vx = vx[i] - vx_op[lead_opponent];
                    double rel_vy = vy[i] - vy_op[lead_opponent];
                    double rel_speed = sqrt(rel_vx*rel_vx + rel_vy*rel_vy);
                    
                    // 충돌 예상 벡터와 실제 방향 벡터의 각도
                    double movement_direction = angle_between(0, 0, vx[i], vy[i]);
                    double collision_direction = angle_between(0, 0, future_lead_x - x[i], future_lead_y - y[i]);
                    double intersection_angle = min_angle_diff(movement_direction, collision_direction);
                    
                    // 충돌 코스에 있는지 확인
                    on_intercept_course = intersection_angle < 30 && rel_speed > 200;
                    
                    if (on_intercept_course) {
                        // 근접 시 최대 추력으로 충돌 임팩트 강화
                        thrust = 100;
                    }
                }
                
                // 쉴드 사용 여부 결정
                bool use_shield = false;
                
                // 상대에 매우 가깝고 충돌 코스에 있으면 쉴드 사용
                if (collision_dist < 800 && on_intercept_course && shield_cooldown[i] == 0) {
                    use_shield = true;
                    shield_target_x[i] = intercept_x;
                    shield_target_y[i] = intercept_y;
                    shield_cooldown[i] = 3;
                    printf("%d %d SHIELD [BLOCKER] 차단충돌! 거리:%.0f\n", intercept_x, intercept_y, collision_dist);
                } else {
                    // 일반 추력 사용
                    printf("%d %d %d [BLOCKER] 리더:%d 각도:%.1f 거리:%.0f\n", 
                           intercept_x, intercept_y, thrust, lead_opponent, blocker_angle_diff, collision_dist);
                }
            }
        }
    }

    return 0;
}
