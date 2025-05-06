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

#define MIN_DRIFT_SPEED 250.0
#define MIN_ANGLE_THRESHOLD 30.0
#define MAX_DRIFT_DISTANCE 3000.0
#define MAX_MOMENTUM_FACTOR 0.7
#define MAX_DIRECTION_DIFF 45.0
#define MIN_CP_DISTANCE_FACTOR 1.1

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

// 값 제한 함수
double clamp(double value, double min_val, double max_val) {
    if (value < min_val) return min_val;
    if (value > max_val) return max_val;
    return value;
}

// 더 정확한 물리 시뮬레이션을 위한 개선된 함수
void predict_future_position(int x, int y, int vx, int vy, int angle, int thrust, int steps, 
                            int* future_x, int* future_y, int* future_vx, int* future_vy) {
    // 더 정밀한 계산을 위해 double로 변환
    double fx = (double)x;
    double fy = (double)y;
    double fvx = (double)vx;
    double fvy = (double)vy;
    double fangle = (double)angle;
    
    // 각도를 라디안으로 변환
    double angle_rad = fangle * PI / 180.0;
    
    // 더 작은 시간 단계로 나누어 더 정확한 시뮬레이션
    const int SUBSTEPS = 4; // 각 단계를 더 작은 시간 단계로 나눔
    const double dt = 1.0 / SUBSTEPS;
    
    for (int i = 0; i < steps; i++) {
        for (int j = 0; j < SUBSTEPS; j++) {
            // 추력을 각 서브스텝에 분배 (보다 정확한 적분)
            double step_thrust = thrust * dt;
            
            // 추력 적용 (가속도)
            fvx += cos(angle_rad) * step_thrust;
            fvy += sin(angle_rad) * step_thrust;
            
            // 위치 업데이트 (속도 * 시간)
            fx += fvx * dt;
            fy += fvy * dt;
            
            // 마찰력 적용 (지수적으로 - 더 현실적인 감쇠)
            fvx *= pow(FRICTION, dt);
            fvy *= pow(FRICTION, dt);
        }
    }
    
    // 최종 결과 반올림하여 정수로 반환
    *future_x = (int)round(fx);
    *future_y = (int)round(fy);
    *future_vx = (int)round(fvx);
    *future_vy = (int)round(fvy);
}

// 개선된 충돌 예측 - 연속적인 궤도 기반 검사
bool predict_collision(int x1, int y1, int vx1, int vy1, int x2, int y2, int vx2, int vy2, int steps, double* collision_time) {
    // 상대 벡터 계산
    double rx = x2 - x1;
    double ry = y2 - y1;
    double rvx = vx2 - vx1;
    double rvy = vy2 - vy1;
    
    // 이차 방정식 계수
    double a = rvx*rvx + rvy*rvy;
    
    // 포드가 서로에 대해 정적이거나 매우 느리게 움직이는 경우
    if (a < 1.0) {
        // 현재 거리 확인
        double dist = sqrt(rx*rx + ry*ry);
        if (dist < 2 * POD_RADIUS) {
            *collision_time = 0;
            return true;
        }
        return false;
    }
    
    double b = 2 * (rx * rvx + ry * rvy);
    double c = rx*rx + ry*ry - 4 * POD_RADIUS * POD_RADIUS; // 두 포드의 반지름 합의 제곱
    
    // 판별식
    double discriminant = b*b - 4*a*c;
    
    // 충돌이 발생하지 않음
    if (discriminant < 0) {
        return false;
    }
    
    // 충돌 시간 계산
    double t1 = (-b - sqrt(discriminant)) / (2*a);
    double t2 = (-b + sqrt(discriminant)) / (2*a);
    
    // 가장 빠른 충돌 시간 찾기
    double t = (t1 > 0 && t1 <= steps) ? t1 : ((t2 > 0 && t2 <= steps) ? t2 : -1);
    
    if (t >= 0) {
        *collision_time = t;
        return true;
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

// 향상된 레이싱 라인 최적화 함수
void optimize_racing_line(int current_x, int current_y, int current_vx, int current_vy,
                         int cp_x, int cp_y, int next_cp_x, int next_cp_y, 
                         int* target_x, int* target_y) {
    // 현재 체크포인트까지 거리
    double dist_to_cp = distance(current_x, current_y, cp_x, cp_y);
    
    // 체크포인트 간 벡터 계산 (다음 체크포인트 방향)
    double cp_vector_x = next_cp_x - cp_x;
    double cp_vector_y = next_cp_y - cp_y;
    double cp_vector_length = sqrt(cp_vector_x * cp_vector_x + cp_vector_y * cp_vector_y);
    
    // 벡터가 0이면 오류 방지
    if (cp_vector_length < 0.001) {
        *target_x = cp_x;
        *target_y = cp_y;
        return;
    }
    
    // 단위 벡터화
    double cp_unit_x = cp_vector_x / cp_vector_length;
    double cp_unit_y = cp_vector_y / cp_vector_length;
    
    // 현재 속도 벡터의 크기
    double speed = sqrt(current_vx * current_vx + current_vy * current_vy);
    
    // 현재 속도 방향 단위 벡터
    double speed_unit_x = 0;
    double speed_unit_y = 0;
    if (speed > 0.001) {
        speed_unit_x = current_vx / speed;
        speed_unit_y = current_vy / speed;
    }
    
    // 현재 속도와 체크포인트 방향 벡터의 내적 (방향 유사성)
    double dot_product = speed_unit_x * cp_unit_x + speed_unit_y * cp_unit_y;
    
    // 속도와 체크포인트 벡터 간 각도 계산 (라디안)
    double angle_between_vectors = acos(clamp(dot_product, -1.0, 1.0));
    
    // 접근 각도에 따른 최적 진입점 계산 - 더 정확한 레이싱 라인
    double approach_factor = 1.0 - (angle_between_vectors / PI);
    
    // 체크포인트에서 다음 체크포인트 방향으로 오프셋 계산
    double offset = 0;
    
    // 체크포인트 반경 안에 들어가기 시작했을 때 다음 체크포인트를 향한 준비
    if (dist_to_cp < CHECKPOINT_RADIUS * 2.5) {
        // 체크포인트에 가까울수록 다음 체크포인트 방향으로 더 많이 오프셋
        double proximity_factor = (CHECKPOINT_RADIUS * 2.5 - dist_to_cp) / (CHECKPOINT_RADIUS * 2.5);
        
        // 속도에 비례하는 오프셋 (더 빠를수록 더 앞을 보고 주행)
        double speed_factor = clamp(speed / 600.0, 0.0, 1.0);
        
        // 접근 각도와 속도를 모두 고려한 오프셋 계산
        offset = CHECKPOINT_RADIUS * 0.8 * proximity_factor * (0.3 + 0.7 * approach_factor) * (0.5 + speed_factor);
    }
    
    // 오프셋된 타겟 계산
    *target_x = cp_x + (int)(cp_unit_x * offset);
    *target_y = cp_y + (int)(cp_unit_y * offset);
}

// 더 정확한 드리프트 타겟 계산 함수
void calculate_drift_target(int current_x, int current_y, int vx, int vy, int speed,
                          int cp_x, int cp_y, int next_cp_x, int next_cp_y,
                          double angle_diff, double next_angle_diff,
                          int next_checkpoint_id, 
                          int* target_x, int* target_y) {
    // 현재 이동 방향과 목표 방향 간의 벡터 계산 (라디안 사용 - 더 정확함)
    double movement_dir = atan2(vy, vx);
    double target_dir_rad = atan2(cp_y - current_y, cp_x - current_x);
    double dir_diff_rad = fabs(movement_dir - target_dir_rad);
    while (dir_diff_rad > PI) dir_diff_rad = 2*PI - dir_diff_rad; // 최소 각도 차이
    
    // 각도를 도 단위로 변환
    double target_dir = target_dir_rad * 180 / PI;
    double dir_diff = dir_diff_rad * 180 / PI;
    
    // 현재 체크포인트까지의 거리
    double dist_to_cp = distance(current_x, current_y, cp_x, cp_y);
    
    // 체크포인트 간의 방향 벡터 분석
    double cp_to_next_dir_rad = atan2(next_cp_y - cp_y, next_cp_x - cp_x);
    double cp_to_next_dir = cp_to_next_dir_rad * 180 / PI;
    
    // 다음 체크포인트 이후의 체크포인트 ID 계산
    int next_next_cp_id = (next_checkpoint_id + 2) % checkpoint_count;
    int next_next_cp_x = checkpoint_x[next_next_cp_id];
    int next_next_cp_y = checkpoint_y[next_next_cp_id];
    
    // 다음 체크포인트에서 그 다음 체크포인트로의 방향
    double next_to_next_next_dir_rad = atan2(next_next_cp_y - next_cp_y, next_next_cp_x - next_cp_x);
    double next_to_next_next_dir = next_to_next_next_dir_rad * 180 / PI;
    
    // 연속 코너 시퀀스 분석 - S자 코너인지 확인
    double raw_diff1 = cp_to_next_dir - target_dir;
    double raw_diff2 = next_to_next_next_dir - cp_to_next_dir;
    while (raw_diff1 > 180) raw_diff1 -= 360;
    while (raw_diff1 < -180) raw_diff1 += 360;
    while (raw_diff2 > 180) raw_diff2 -= 360;
    while (raw_diff2 < -180) raw_diff2 += 360;
    bool is_s_curve = (raw_diff1 * raw_diff2) < 0; // 부호가 다르면 S자 코너
    
    // 연속 코너 각도 - 두 회전 사이의 각도 변화량
    double corner_sequence_angle = min_angle_diff(cp_to_next_dir, next_to_next_next_dir);
    
    // 현재 턴 방향 결정 (1: 시계, -1: 반시계) - 더 정확한 계산
    int turn_sign = 1;
    double cp_cross_product = (cp_x - current_x)*(next_cp_y - cp_y) - (cp_y - current_y)*(next_cp_x - cp_x);
    turn_sign = cp_cross_product > 0 ? 1 : -1;
    
    // 속도 기반 드리프트 임계값 계산 - 속도가 높을수록 더 정밀한 조정
    double speed_factor = (double)speed / 400.0; // 속도 정규화
    double speed_angle_threshold = clamp(50.0 - 25.0 * speed_factor, MIN_ANGLE_THRESHOLD, 50.0);
    
    // 속도에 비례하는 드리프트 시작 거리 계산 - 곡률 기반 조정
    double drift_start_distance = clamp(CHECKPOINT_RADIUS * 1.2 + (speed * 3.0 * (1 + next_angle_diff/180.0)), 
                                        CHECKPOINT_RADIUS, MAX_DRIFT_DISTANCE);
    
    // 드리프트 적용 여부 결정 - 보다 세분화된 조건
    bool fast_enough = speed > MIN_DRIFT_SPEED;
    bool sharp_turn = next_angle_diff > speed_angle_threshold;
    bool good_distance = dist_to_cp > CHECKPOINT_RADIUS * MIN_CP_DISTANCE_FACTOR && 
                         dist_to_cp < 4000 && 
                         dist_to_cp < drift_start_distance;
    bool good_alignment = dir_diff < MAX_DIRECTION_DIFF;
    
    bool apply_drift = fast_enough && sharp_turn && good_distance && good_alignment;
    
    // ... 기존 코드 나머지 부분 ...
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
