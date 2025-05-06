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
                          int next_checkpoint_id, // 추가된 파라미터
                          int* target_x, int* target_y) {
    
    // 현재 이동 방향과 목표 방향 간의 벡터 계산
    double movement_dir = atan2(vy, vx) * 180 / PI;
    double target_dir = angle_between(current_x, current_y, cp_x, cp_y);
    double dir_diff = min_angle_diff(movement_dir, target_dir);
    
    // 현재 체크포인트까지의 거리
    double dist_to_cp = distance(current_x, current_y, cp_x, cp_y);
    
    // 체크포인트 간의 방향 벡터 계산
    double cp_to_next_dir = angle_between(cp_x, cp_y, next_cp_x, next_cp_y);
    
    // 다음 체크포인트 이후의 체크포인트 ID 계산
    int next_next_cp_id = (next_checkpoint_id + 2) % checkpoint_count;
    int next_next_cp_x = checkpoint_x[next_next_cp_id];
    int next_next_cp_y = checkpoint_y[next_next_cp_id];
    
    // 다음 체크포인트에서 그 다음 체크포인트로의 방향
    double next_to_next_next_dir = angle_between(next_cp_x, next_cp_y, next_next_cp_x, next_next_cp_y);
    
    // 현재->다음, 다음->다다음 체크포인트 간 각도 차이 계산 (코너의 특성)
    double corner_sequence_angle = min_angle_diff(cp_to_next_dir, next_to_next_next_dir);
    
    // S자 코너인지 확인 (방향 전환이 반대로 되는지)
    bool is_s_curve = false;
    double raw_diff1 = target_dir - cp_to_next_dir;
    double raw_diff2 = cp_to_next_dir - next_to_next_next_dir;
    if (raw_diff1 > 180) raw_diff1 -= 360;
    if (raw_diff1 < -180) raw_diff1 += 360;
    if (raw_diff2 > 180) raw_diff2 -= 360;
    if (raw_diff2 < -180) raw_diff2 += 360;
    is_s_curve = (raw_diff1 * raw_diff2) < 0; // 부호가 다르면 S자 코너
    
    // 현재 턴 방향 결정 (1: 시계, -1: 반시계)
    int turn_sign = 1;
    double raw_diff = target_dir - cp_to_next_dir;
    if (raw_diff > 180) raw_diff -= 360;
    if (raw_diff < -180) raw_diff += 360;
    turn_sign = raw_diff > 0 ? -1 : 1;
    
    // 개선된 드리프트 적용 조건 (속도 기반 예측으로 개선)
    // 1. 속도가 충분히 높고 
    // 2. 다음 턴이 급격하고 (각도 > 50, 또는 속도가 높을수록 더 작은 각도에도 대응)
    // 3. 체크포인트에 너무 가깝지 않을 때 
    // 4. 현재 각도 차이가 크지 않을 때
    double speed_angle_threshold = 50.0 - (speed - 350) / 15.0; // 속도가 높을수록 더 작은 각도에도 반응
    speed_angle_threshold = speed_angle_threshold < 30.0 ? 30.0 : speed_angle_threshold;
    
    // 속도에 비례하는 드리프트 시작 거리 (더 빠를수록 더 일찍 드리프트 시작)
    double drift_start_distance = CHECKPOINT_RADIUS * 1.2 + (speed * 3.0);
    drift_start_distance = drift_start_distance > 3000 ? 3000 : drift_start_distance;
    
    // 관성 기반으로 개선된 드리프트 적용 조건
    bool apply_drift = speed > 250 && // 더 낮은 속도에서도 드리프트 허용
                       next_angle_diff > speed_angle_threshold && 
                       dist_to_cp > CHECKPOINT_RADIUS * 1.1 && // 더 가깉게 허용
                       dist_to_cp < 4000 && // 너무 멀면 드리프트 효과 감소
                       dist_to_cp < drift_start_distance && // 속도에 비례한 시작 거리
                       dir_diff < 45; // 방향 차이 여유 증가
    
    // 드리프트 계산 - 관성과 다음 체크포인트 예측 강화
    if (apply_drift) {
        // 체크포인트까지 거리와 속도를 모두 고려한 드리프트 강도 계산
        // 속도가 높을수록, 거리가 가까울수록 드리프트 시작
        double drift_urgency = (speed / 500.0) * (1.0 - (dist_to_cp / drift_start_distance));
        drift_urgency = drift_urgency < 0 ? 0 : (drift_urgency > 1.0 ? 1.0 : drift_urgency);
        
        // 관성 벡터 계산 (현재 속도 방향)
        double momentum_angle = atan2(vy, vx) * 180 / PI;
        double momentum_factor = 0.3 + (speed / 1500.0); // 속도가 높을수록 관성 영향 증가
        momentum_factor = momentum_factor > 0.7 ? 0.7 : momentum_factor;
        
        // 체크포인트까지 거리에 따른 드리프트 강도 조절
        double cp_distance_factor = fmin(1.0, dist_to_cp / 2000.0);
        
        // S자 코너인 경우 드리프트 각도 조정 (속도 반영)
        double tangent_factor;
        if (is_s_curve) {
            // S자 코너에서는 첫 번째 코너를 덜 공격적으로 진입
            tangent_factor = fmin(35.0 + (speed / 40.0), next_angle_diff / 3); 
            fprintf(stderr, "S자 코너 감지! 드리프트 각도: %.1f (속도: %d)\n", tangent_factor, (int)speed);
        } else if (corner_sequence_angle > 100) {
            // 두 코너가 매우 급격하게 방향이 바뀌는 경우 더 공격적으로
            tangent_factor = fmin(55.0 + (speed / 30.0), next_angle_diff / 2.2);
            fprintf(stderr, "급격한 연속 코너 감지! 드리프트 각도: %.1f (속도: %d)\n", tangent_factor, (int)speed);
        } else {
            // 일반적인 코너 (속도에 비례하여 각도 증가)
            tangent_factor = fmin(45.0 + (speed / 35.0), next_angle_diff / 2.8);
        }
        
        // 관성 벡터와 목표 방향을 결합한 최종 드리프트 방향 계산
        double inertia_adjusted_target = target_dir * (1.0 - momentum_factor) + momentum_angle * momentum_factor;
        double tangent_angle = inertia_adjusted_target + turn_sign * tangent_factor * cp_distance_factor;
        
        // 속도와 코너 시퀀스에 따른 드리프트 강도 조정 (속도 기반 개선)
        double base_drift_factor = 0.15 + (speed / 1500.0) + (drift_urgency * 0.15);
        
        // S자 코너나 연속된 급격한 코너에서는 드리프트 강도 조정
        if (is_s_curve) {
            base_drift_factor *= 0.8; // S자 코너에서 드리프트 강도 감소
        } else if (corner_sequence_angle < 30) {
            // 거의 일직선 코너 시퀀스에서는 드리프트 강도 증가
            base_drift_factor *= 1.2;
        }
        
        // 속도에 따른 적응형 드리프트 강도 - 높은 속도에서 더 강한 드리프트
        double drift_factor = base_drift_factor * cp_distance_factor * (1.0 + (speed / 1000.0) * 0.3);
        
        // 체크포인트에 가까워지면 더 직접적인 접근으로 전환
        double weight_to_cp = 1.0 - (dist_to_cp / 1500.0);
        weight_to_cp = weight_to_cp * weight_to_cp; // 제곱 적용으로 더 빨리 감소
        
        // 접선 각도와 체크포인트 방향 간의 가중치 혼합
        tangent_angle = tangent_angle * (1 - weight_to_cp * 0.8) + target_dir * (weight_to_cp * 0.8);
        
        // S자 코너에 접근할 때는 다음 체크포인트를 향한 각도도 약간 고려
        if (is_s_curve && dist_to_cp < 1000) {
            double next_cp_influence = 0.2 * weight_to_cp;
            tangent_angle = tangent_angle * (1 - next_cp_influence) + cp_to_next_dir * next_cp_influence;
        }
        
        // 거리가 가까울수록 드리프트 강도 감소
        drift_factor *= (1.0 - weight_to_cp * 0.9);
        
        // 드리프트 거리 계산 및 제한
        double drift_distance = dist_to_cp * drift_factor;
        
        // S자 코너에서는 드리프트 거리를 더 제한
        double max_drift_distance = is_s_curve ? 350 : 450;
        drift_distance = drift_distance > max_drift_distance ? max_drift_distance : drift_distance;
        
        // 체크포인트를 벗어나지 않도록 최소 각도 보장
        double min_cp_angle = angle_between(current_x, current_y, cp_x, cp_y);
        double angle_to_cp = min_angle_diff(tangent_angle, min_cp_angle);
        
        // 각도 차이가 너무 크면 체크포인트 방향으로 더 맞춤
        if (angle_to_cp > 45) {
            // 체크포인트 방향으로 더 강하게 조정
            tangent_angle = min_cp_angle + (tangent_angle - min_cp_angle) * 0.3;
        }
        
        // 안전장치: 드리프트가 체크포인트와 반대 방향으로 가지 않도록 확인
        double cp_vector_dot = cos((tangent_angle - min_cp_angle) * PI / 180.0);
        if (cp_vector_dot < 0.5) {  // 각도가 60도 이상 벌어지면
            // 드리프트 취소, 체크포인트로 직접 향함
            *target_x = cp_x;
            *target_y = cp_y;
            fprintf(stderr, "DRIFT 취소: 체크포인트와 각도차이 너무 큼\n");
            return;
        }
        
        // 계산된 드리프트 타겟
        *target_x = current_x + (int)(cos(tangent_angle * PI / 180.0) * drift_distance);
        *target_y = current_y + (int)(sin(tangent_angle * PI / 180.0) * drift_distance);
        
        // 계산된 타겟이 현재 체크포인트보다 멀어지지 않도록 확인
        double new_dist_to_cp = distance(*target_x, *target_y, cp_x, cp_y);
        if (new_dist_to_cp > dist_to_cp) {
            // 거리가 늘어나면 드리프트 강도 줄임
            drift_distance *= 0.6;
            *target_x = current_x + (int)(cos(tangent_angle * PI / 180.0) * drift_distance);
            *target_y = current_y + (int)(sin(tangent_angle * PI / 180.0) * drift_distance);
        }
        
        fprintf(stderr, "DRIFT: 속도:%d 각도차:%.1f S자:%s 연속각도:%.1f 드리프트강도:%.2f 거리:%.0f\n", 
                speed, next_angle_diff, is_s_curve ? "YES" : "NO", corner_sequence_angle, drift_factor, drift_distance);
    } else {
        *target_x = cp_x;
        *target_y = cp_y;
    }
}

// 고급 차단 전략 계산 함수
void calculate_advanced_interception(
    int blocker_x, int blocker_y, int blocker_vx, int blocker_vy, int blocker_angle,
    int op_x, int op_y, int op_vx, int op_vy, int op_angle, int op_next_cp_id,
    int* intercept_x, int* intercept_y, int* thrust, bool* use_shield) {
    
    // 상대가 향하는 체크포인트
    int op_target_x = checkpoint_x[op_next_cp_id];
    int op_target_y = checkpoint_y[op_next_cp_id];
    
    // 다음 체크포인트
    int op_next_next_cp_id = (op_next_cp_id + 1) % checkpoint_count;
    int op_next_target_x = checkpoint_x[op_next_next_cp_id];
    int op_next_target_y = checkpoint_y[op_next_next_cp_id];
    
    // 상대방의 다양한 시점 미래 위치 예측 (최대 6턴)
    int future_positions_x[6], future_positions_y[6];
    int future_velocities_x[6], future_velocities_y[6];
    
    // 현재 위치 초기화
    future_positions_x[0] = op_x;
    future_positions_y[0] = op_y;
    future_velocities_x[0] = op_vx;
    future_velocities_y[0] = op_vy;
    
    // 상대방의 체크포인트 도달 예상 시간 (턴 단위)
    double op_speed = sqrt(op_vx*op_vx + op_vy*op_vy);
    double dist_to_cp = distance(op_x, op_y, op_target_x, op_target_y);
    int estimated_turns_to_cp = (int)(dist_to_cp / (op_speed > 0 ? op_speed : 100));
    estimated_turns_to_cp = estimated_turns_to_cp < 1 ? 1 : estimated_turns_to_cp;
    estimated_turns_to_cp = estimated_turns_to_cp > 5 ? 5 : estimated_turns_to_cp;
    
    // 차단 시점 결정 - 상대가 체크포인트에 도달하기 직전이 효과적
    int intercept_turn = estimated_turns_to_cp - 1;
    intercept_turn = intercept_turn < 1 ? 1 : intercept_turn;
    intercept_turn = intercept_turn > 4 ? 4 : intercept_turn;
    
    // 예상되는 체크포인트 도달 시점
    bool will_reach_checkpoint = false;
    
    // 미래 위치 예측 시뮬레이션 (최대 5턴)
    for (int step = 1; step <= 5; step++) {
        // 이전 단계에서 체크포인트를 달성했는지 확인
        double prev_dist_to_cp = distance(future_positions_x[step-1], future_positions_y[step-1], op_target_x, op_target_y);
        
        if (prev_dist_to_cp < CHECKPOINT_RADIUS) {
            // 체크포인트에 도달할 것으로 예상되면 다음 체크포인트로 타겟 전환
            op_target_x = op_next_target_x;
            op_target_y = op_next_target_y;
            will_reach_checkpoint = true;
        }
        
        // 상대의 다음 위치 예측
        double target_angle = angle_between(future_positions_x[step-1], future_positions_y[step-1], op_target_x, op_target_y);
        double current_angle = op_angle; // 실제 포드 각도 사용
        double angle_diff = min_angle_diff(current_angle, target_angle);
        
        // 각도에 따른 추력 예측
        int predicted_thrust = 100;
        if (angle_diff > 90) {
            predicted_thrust = 0;
        } else if (angle_diff > 0) {
            predicted_thrust = (int)(100 - angle_diff * 0.7);
            predicted_thrust = predicted_thrust < 0 ? 0 : predicted_thrust;
        }
        
        // 다음 위치 예측
        predict_future_position(
            future_positions_x[step-1], future_positions_y[step-1],
            future_velocities_x[step-1], future_velocities_y[step-1],
            current_angle, predicted_thrust, 1,
            &future_positions_x[step], &future_positions_y[step],
            &future_velocities_x[step], &future_velocities_y[step]
        );
    }
    
    // 차단 전략 결정
    int target_step = intercept_turn;
    
    // 체크포인트 진입 시 차단이 효과적
    if (will_reach_checkpoint && estimated_turns_to_cp <= 4) {
        target_step = estimated_turns_to_cp;
        fprintf(stderr, "체크포인트 진입 차단! 예상 시간: %d턴\n", target_step);
    }
    
    // 최종 차단 위치 계산
    int future_x = future_positions_x[target_step];
    int future_y = future_positions_y[target_step];
    
    // 상대의 예상 진행 방향 벡터
    double future_vx = future_velocities_x[target_step];
    double future_vy = future_velocities_y[target_step];
    double future_speed = sqrt(future_vx*future_vx + future_vy*future_vy);
    double future_angle = atan2(future_vy, future_vx) * 180 / PI;
    
    // 차단 전략 - 상대 진행 방향에 따른 최적 차단 위치 계산
    double blocking_distance = 300 + future_speed * 0.3; // 속도에 비례하는 차단 거리
    double blocking_angle;
    
    // 체크포인트에 가까울수록 직접 경로를 차단
    if (dist_to_cp < 1500) {
        // 상대와 체크포인트 사이의 직접 경로 차단
        blocking_angle = angle_between(future_x, future_y, op_target_x, op_target_y);
        blocking_distance = dist_to_cp * 0.4; // 거리의 40% 지점
        fprintf(stderr, "체크포인트 접근 차단 전략\n");
    } else {
        // 일반적인 상황: 상대 진행 방향의 앞쪽 90도 각도에서 차단
        blocking_angle = future_angle;
        fprintf(stderr, "진행 방향 차단 전략\n");
    }
    
    // 최종 차단 위치 계산
    *intercept_x = future_x + (int)(cos(blocking_angle * PI / 180) * blocking_distance);
    *intercept_y = future_y + (int)(sin(blocking_angle * PI / 180) * blocking_distance);
    
    // 블로커와 차단 지점 사이의 거리
    double dist_to_target = distance(blocker_x, blocker_y, *intercept_x, *intercept_y);
    
    // 추력 결정 - 거리와 각도에 따라 조절
    *thrust = 100;
    if (dist_to_target < 1000) {
        // 가까울수록 정밀한 움직임을 위해 추력 조정
        *thrust = (int)(70 + 30 * (dist_to_target / 1000));
    }
    
    // 충돌 임박 여부 판단
    double collision_dist = distance(blocker_x, blocker_y, future_x, future_y);
    *use_shield = (collision_dist < 850 && target_step <= 2 && future_speed > 100);
    
    // 디버깅용 로그
    fprintf(stderr, "예측 차단: 타겟턴=%d 거리=%.0f 쉴드=%s 추력=%d\n", 
            target_step, collision_dist, *use_shield ? "YES" : "NO", *thrust);
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
                                      angle_diff, next_angle_diff, next_check_point_id[i],
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
                
                // 고급 예측 기반 차단 전략 계산
                int intercept_x, intercept_y, thrust;
                bool use_shield = false;
                
                calculate_advanced_interception(
                    x[i], y[i], vx[i], vy[i], angle[i],
                    x_op[lead_opponent], y_op[lead_opponent], 
                    vx_op[lead_opponent], vy_op[lead_opponent],
                    angle_op[lead_opponent], next_check_point_id_op[lead_opponent],
                    &intercept_x, &intercept_y, &thrust, &use_shield
                );
                
                // 블로커의 각도 차이 계산
                double target_angle = angle_between(x[i], y[i], intercept_x, intercept_y);
                double blocker_angle_diff = min_angle_diff(angle[i], target_angle);
                
                // 충돌 거리 및 코스 계산
                double collision_dist = distance(x[i], y[i], x_op[lead_opponent], y_op[lead_opponent]);
                
                // 쉴드 사용이 권장되고 쿨다운이 없으면
                if (use_shield && shield_cooldown[i] == 0) {
                    shield_target_x[i] = intercept_x;
                    shield_target_y[i] = intercept_y;
                    shield_cooldown[i] = 3;
                    printf("%d %d SHIELD [BLOCKER] 전략차단! 거리:%.0f\n", intercept_x, intercept_y, collision_dist);
                } else {
                    // 일반 추력 사용
                    printf("%d %d %d [BLOCKER] 예측차단 리더:%d 각도:%.1f\n", 
                           intercept_x, intercept_y, thrust, lead_opponent, blocker_angle_diff);
                }
            }
        }
    }

    return 0;
}
