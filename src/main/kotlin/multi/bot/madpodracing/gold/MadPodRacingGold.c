#include <stdlib.h>
#include <stdio.h>
#include <string.h>
#include <stdbool.h>
#include <math.h>

#define MAX_CHECKPOINTS 10
#define CHECKPOINT_RADIUS 600
#define POD_RADIUS 400
#define FRICTION 0.85
#define PI 3.14159265358979323846
#define DEG_TO_RAD(x) ((x) * PI / 180.0)
#define RAD_TO_DEG(x) ((x) * 180.0 / PI)

typedef struct {
    int x;
    int y;
} Vector;

typedef struct {
    Vector position;
    Vector velocity;
    int angle;
    int nextCheckpointId;
    bool boostAvailable;
    int shieldCooldown;
    int checkpointsPassed;
} Pod;

typedef struct {
    Vector position;
    int radius;
} Checkpoint;

// 베지어 곡선 구조체 추가
typedef struct {
    Vector points[4]; // 3차 베지어 곡선을 위한 4개의 제어점
    int numPoints;    // 실제 사용되는 제어점 수
} BezierCurve;

// 포드 정보 추가 구조체
typedef struct {
    float progress;    // 레이스 진행도
    Vector racingLine; // 최적 레이싱 라인 목표점
    BezierCurve racingCurve; // 베지어 곡선 경로
} PodInfo;

// 포드 명령어 구조체 추가
typedef struct {
    Vector targetPos;
    int thrust;
    bool useShield;
    bool useBoost;
    int podId;
} PodCommand;

// 게임 상태 구조체 추가
typedef struct {
    int laps;
    int checkpointCount;
    Checkpoint checkpoints[MAX_CHECKPOINTS];
    Pod myPods[2];
    Pod enemyPods[2];
    PodInfo myPodsInfo[2];
    float enemyProgress[2];
    int totalCheckpoints;
    int longestStretch;
    bool firstTurn;
} GameState;

// PID 컨트롤러 구조체 추가
typedef struct {
    float kp;           // 비례 상수
    float ki;           // 적분 상수
    float kd;           // 미분 상수
    float previousError; // 이전 오차
    float integral;     // 오차 누적값
    float output;       // 컨트롤러 출력값
    int maxOutput;      // 최대 출력값 제한
    int minOutput;      // 최소 출력값 제한
} PidController;

// 전역 변수 대신 GameState 인스턴스 사용
GameState gameState;

// 함수 선언부
float distance(Vector a, Vector b);
float angleBetween(Vector a, Vector b);
Vector normalize(Vector v);
Vector scale(Vector v, float scalar);
Vector add(Vector a, Vector b);
Vector subtract(Vector a, Vector b);
float dotProduct(Vector a, Vector b);
Vector closestPointToLine(Vector point, Vector lineStart, Vector lineEnd);
bool willEnterCheckpointSoon(Pod pod, Checkpoint cp);
bool isGoingToCollideWith(Pod pod1, Pod pod2);
Vector predictCollisionWithPid(Pod pod, Pod other, PidController* pid, float deltaTime);
float collisionNicenessScore(Pod pod, Pod other, Checkpoint target);
bool shouldEnableShield(Pod pod, Pod myOtherPod, Pod enemies[], Checkpoint checkpoints[], int checkpointCount);
int calculateThrust(float angleDiff, int distToCheckpoint);

// 베지어 곡선 관련 함수 선언 추가
BezierCurve createBezierCurve(Vector p0, Vector p1, Vector p2, Vector p3);
Vector evaluateBezierCurve(BezierCurve curve, float t);
BezierCurve calculateRacingCurve(Pod pod, Checkpoint checkpoints[], int checkpointCount);
Vector getBezierPoint(BezierCurve curve, float t);
Vector getOptimalTargetFromCurve(Pod pod, BezierCurve curve, float lookAheadTime);

Vector calculateRacingLine(Pod pod, Checkpoint checkpoints[], int checkpointCount, int lookAhead);
float calculateProgress(Pod pod, Checkpoint checkpoints[], int checkpointCount);
int calculateAdaptiveThrust(Pod pod, float angleDiff, int distToCheckpoint, Checkpoint checkpoints[], int checkpointCount);
PidController initPidController(float kp, float ki, float kd, int minOutput, int maxOutput);
float updatePidController(PidController* pid, float error, float deltaTime);
void initializeGame(GameState* state);
void updateGameState(GameState* state);
PodCommand determinePodStrategy(Pod pod, Pod otherPod, Pod enemies[], GameState* state, int podIndex);
void executePodCommand(PodCommand cmd);

// 거리 계산 함수
float distance(Vector a, Vector b) {
    return sqrtf((a.x - b.x) * (a.x - b.x) + (a.y - b.y) * (a.y - b.y));
}

// 두 벡터 사이의 각도 계산 (라디안)
float angleBetween(Vector a, Vector b) {
    return atan2f(b.y - a.y, b.x - a.x);
}

// 벡터 정규화
Vector normalize(Vector v) {
    float len = sqrtf(v.x * v.x + v.y * v.y);
    Vector result;
    if (len > 0) {
        result.x = v.x / len;
        result.y = v.y / len;
    } else {
        result.x = 0;
        result.y = 0;
    }
    return result;
}

// 벡터 스케일링
Vector scale(Vector v, float scalar) {
    Vector result;
    result.x = (int)(v.x * scalar);
    result.y = (int)(v.y * scalar);
    return result;
}

// 벡터 덧셈
Vector add(Vector a, Vector b) {
    Vector result;
    result.x = a.x + b.x;
    result.y = a.y + b.y;
    return result;
}

// 벡터 뺄셈
Vector subtract(Vector a, Vector b) {
    Vector result;
    result.x = a.x - b.x;
    result.y = a.y - b.y;
    return result;
}

// 내적
float dotProduct(Vector a, Vector b) {
    return a.x * b.x + a.y * b.y;
}

// 점과 직선 사이의 가장 가까운 점을 찾음
Vector closestPointToLine(Vector point, Vector lineStart, Vector lineEnd) {
    Vector line = subtract(lineEnd, lineStart);
    float len = sqrtf(line.x * line.x + line.y * line.y);
    line = normalize(line);
    Vector v = subtract(point, lineStart);
    float d = dotProduct(v, line);
    d = fmaxf(0, fminf(len, d));
    Vector result;
    result.x = lineStart.x + (int)(line.x * d);
    result.y = lineStart.y + (int)(line.y * d);
    return result;
}

// 곧 체크포인트에 들어갈지 예측
bool willEnterCheckpointSoon(Pod pod, Checkpoint cp) {
    Vector futurePos;
    futurePos.x = pod.position.x + pod.velocity.x;
    futurePos.y = pod.position.y + pod.velocity.y;
    return distance(futurePos, cp.position) < CHECKPOINT_RADIUS;
}

// PID 컨트롤러 초기화 함수
PidController initPidController(float kp, float ki, float kd, int minOutput, int maxOutput) {
    PidController pid;
    pid.kp = kp;
    pid.ki = ki;
    pid.kd = kd;
    pid.previousError = 0.0f;
    pid.integral = 0.0f;
    pid.output = 0.0f;
    pid.maxOutput = maxOutput;
    pid.minOutput = minOutput;
    return pid;
}

// PID 컨트롤러 업데이트 함수
float updatePidController(PidController* pid, float error, float deltaTime) {
    // 비례 항 계산
    float proportional = pid->kp * error;
    
    // 적분 항 계산 (적분 누적 및 와인드업 방지)
    pid->integral += error * deltaTime;
    float integral = pid->ki * pid->integral;
    
    // 미분 항 계산
    float derivative = pid->kd * (error - pid->previousError) / deltaTime;
    pid->previousError = error;
    
    // 총 출력값 계산 및 제한
    pid->output = proportional + integral + derivative;
    
    // 출력값 제한
    if (pid->output > pid->maxOutput) pid->output = pid->maxOutput;
    if (pid->output < pid->minOutput) pid->output = pid->minOutput;
    
    return pid->output;
}

// 개선된 충돌 예측 함수 (PID 컨트롤러 사용)
bool isGoingToCollideWith(Pod pod1, Pod pod2) {
    // 기본 충돌 감지 (현재 코드 유지)
    Vector futurePos1, futurePos2;
    futurePos1.x = pod1.position.x + pod1.velocity.x;
    futurePos1.y = pod1.position.y + pod1.velocity.y;
    futurePos2.x = pod2.position.x + pod2.velocity.x;
    futurePos2.y = pod2.position.y + pod2.velocity.y;
    
    return distance(futurePos1, futurePos2) < 2 * POD_RADIUS;
}

// PID 기반 충돌 예측 및 회피 함수
Vector predictCollisionWithPid(Pod pod, Pod other, PidController* pid, float deltaTime) {
    // 현재 거리 계산
    float currentDist = distance(pod.position, other.position);
    
    // 미래 위치에서의 거리 예측 (여러 타임스텝 시뮬레이션)
    Vector podFuturePos = pod.position;
    Vector podFutureVel = pod.velocity;
    Vector otherFuturePos = other.position;
    Vector otherFutureVel = other.velocity;
    
    // 최대 8단계까지 예측
    bool willCollide = false;
    int collisionStep = -1;
    float collisionDist = 0;
    
    for (int step = 1; step <= 8; step++) {
        // 속도 적용 및 마찰 계산
        podFuturePos.x += podFutureVel.x;
        podFuturePos.y += podFutureVel.y;
        podFutureVel.x *= FRICTION;
        podFutureVel.y *= FRICTION;
        
        otherFuturePos.x += otherFutureVel.x;
        otherFuturePos.y += otherFutureVel.y;
        otherFutureVel.x *= FRICTION;
        otherFutureVel.y *= FRICTION;
        
        float futureDist = distance(podFuturePos, otherFuturePos);
        
        // 충돌 감지
        if (futureDist < 2 * POD_RADIUS) {
            willCollide = true;
            collisionStep = step;
            collisionDist = futureDist;
            break;
        }
    }
    
    // 충돌이 예측되면 PID 컨트롤러를 이용해 회피 동작 계산
    Vector avoidanceVector = {0, 0};
    
    if (willCollide) {
        // 안전거리를 2.2 * POD_RADIUS로 설정 (약간의 여유)
        float safeDistance = 2.2f * POD_RADIUS;
        float error = safeDistance - collisionDist;
        
        // PID 컨트롤러로 회피 강도 계산
        float avoidanceStrength = updatePidController(pid, error, deltaTime);
        
        // 회피 방향 계산 (다른 포드로부터 멀어지는 방향)
        Vector avoidDir = subtract(pod.position, other.position);
        avoidDir = normalize(avoidDir);
        
        // 회피 벡터 계산
        avoidanceVector.x = (int)(avoidDir.x * avoidanceStrength);
        avoidanceVector.y = (int)(avoidDir.y * avoidanceStrength);
        
        // 충돌까지 시간이 짧을수록 더 강한 회피
        float urgencyFactor = 1.0f / (collisionStep * 0.5f + 0.5f);
        avoidanceVector.x = (int)(avoidanceVector.x * urgencyFactor);
        avoidanceVector.y = (int)(avoidanceVector.y * urgencyFactor);
    }
    
    return avoidanceVector;
}

// 충돌이 얼마나 유리한지 점수 계산
float collisionNicenessScore(Pod pod, Pod other, Checkpoint target) {
    if (!isGoingToCollideWith(pod, other)) {
        return 0;
    }
    
    float baseDistance = distance(pod.position, target.position);
    
    Vector futureVel;
    futureVel.x = (int)((pod.velocity.x * FRICTION) + (other.velocity.x * FRICTION * 0.5));
    futureVel.y = (int)((pod.velocity.y * FRICTION) + (other.velocity.y * FRICTION * 0.5));
    
    Vector futurePos;
    futurePos.x = pod.position.x + futureVel.x;
    futurePos.y = pod.position.y + futureVel.y;
    
    float newDistance = distance(futurePos, target.position);
    
    // 충돌 후 더 가까워지면 양수, 멀어지면 음수
    return baseDistance - newDistance;
}

// 쉴드 사용 여부 결정 함수 (수정: 체크포인트 배열 매개변수 추가)
bool shouldEnableShield(Pod pod, Pod myOtherPod, Pod enemies[], Checkpoint checkpoints[], int checkpointCount) {
    // 쉴드 쿨다운 중이면 사용 불가
    if (pod.shieldCooldown > 0) {
        return false;
    }
    
    Checkpoint target = checkpoints[pod.nextCheckpointId];
    
    // 내 다른 포드와의 충돌이 있을 경우
    float allyScore = collisionNicenessScore(pod, myOtherPod, target);
    if (allyScore < -200) { // 불리한 충돌이면 쉴드 사용
        return true;
    }
    
    // 적 포드와의 충돌이 있을 경우
    for (int i = 0; i < 2; i++) {
        float enemyScore = collisionNicenessScore(pod, enemies[i], target);
        if (enemyScore > 200) { // 유리한 충돌이면 쉴드 사용
            return true;
        }
        if (enemyScore < -300) { // 매우 불리한 충돌이면 쉴드 사용
            return true;
        }
    }
    
    return false;
}

// 추력(thrust) 계산
int calculateThrust(float angleDiff, int distToCheckpoint) {
    // 각도가 커지면 추력을 줄임 (Anti-spinning 휴리스틱)
    if (angleDiff > 90) {
        return 0;
    }
    
    int thrust = (int)(100.0f * cosf(DEG_TO_RAD(angleDiff)));
    
    // 체크포인트에 가까워지면 추력 줄임
    if (distToCheckpoint < CHECKPOINT_RADIUS * 2) {
        thrust = (thrust * 2 * distToCheckpoint) / (CHECKPOINT_RADIUS * 4);
        thrust = thrust < 20 ? 20 : thrust; // 최소 추력 보장
    }
    
    return thrust;
}

// 베지어 곡선 생성 함수
BezierCurve createBezierCurve(Vector p0, Vector p1, Vector p2, Vector p3) {
    BezierCurve curve;
    curve.points[0] = p0;
    curve.points[1] = p1;
    curve.points[2] = p2;
    curve.points[3] = p3;
    curve.numPoints = 4;
    return curve;
}

// 베지어 곡선 평가 함수
Vector evaluateBezierCurve(BezierCurve curve, float t) {
    Vector result;
    float u = 1 - t;
    float tt = t * t;
    float uu = u * u;
    float uuu = uu * u;
    float ttt = tt * t;

    result.x = (int)(uuu * curve.points[0].x +
                     3 * uu * t * curve.points[1].x +
                     3 * u * tt * curve.points[2].x +
                     ttt * curve.points[3].x);
    result.y = (int)(uuu * curve.points[0].y +
                     3 * uu * t * curve.points[1].y +
                     3 * u * tt * curve.points[2].y +
                     ttt * curve.points[3].y);
    return result;
}

// 베지어 곡선 기반 레이싱 경로 계산
BezierCurve calculateRacingCurve(Pod pod, Checkpoint checkpoints[], int checkpointCount) {
    Vector p0 = pod.position;
    Vector p1 = checkpoints[pod.nextCheckpointId].position;
    Vector p2 = checkpoints[(pod.nextCheckpointId + 1) % checkpointCount].position;
    Vector p3 = checkpoints[(pod.nextCheckpointId + 2) % checkpointCount].position;
    return createBezierCurve(p0, p1, p2, p3);
}

// 베지어 곡선에서 최적 목표 지점 계산
Vector getOptimalTargetFromCurve(Pod pod, BezierCurve curve, float lookAheadTime) {
    return evaluateBezierCurve(curve, lookAheadTime);
}

// 레이싱 라인 계산 함수 (수정: 베지어 곡선 기반으로 변경)
Vector calculateRacingLine(Pod pod, Checkpoint checkpoints[], int checkpointCount, int lookAhead) {
    BezierCurve curve = calculateRacingCurve(pod, checkpoints, checkpointCount);
    return getOptimalTargetFromCurve(pod, curve, 0.5f); // lookAheadTime은 0.5로 설정
}

// 포드 진행상황 계산 (수정: 체크포인트 배열 매개변수 추가)
float calculateProgress(Pod pod, Checkpoint checkpoints[], int checkpointCount) {
    return pod.checkpointsPassed + 
           (1.0f - fminf(1.0f, distance(pod.position, checkpoints[pod.nextCheckpointId].position) / 
           (2 * CHECKPOINT_RADIUS))) / checkpointCount;
}

// 속도 기반 추력 계산 함수 (수정: 더 정밀한 계산 적용)
int calculateAdaptiveThrust(Pod pod, float angleDiff, int distToCheckpoint, Checkpoint checkpoints[], int checkpointCount) {
    // 각도가 매우 큰 경우 점진적으로 추력 감소
    if (angleDiff > 90) {
        // 90도 초과 시 부드러운 감소 (180도에 가까울수록 더 낮아짐)
        return fmaxf(0, (int)(-100 * (angleDiff - 90) / 90));
    }
    
    // 기본 추력 계산 (각도 기반, 코사인 함수 적용)
    float baseThrust = 100.0f * cosf(DEG_TO_RAD(angleDiff));
    
    // 현재 속도 벡터의 크기 및 방향
    float currentSpeed = sqrtf(pod.velocity.x * pod.velocity.x + pod.velocity.y * pod.velocity.y);
    
    // 목표 지점 방향의 단위 벡터 계산
    Vector targetPos = checkpoints[pod.nextCheckpointId].position;
    Vector dirVector = subtract(targetPos, pod.position);
    float dirLength = sqrtf(dirVector.x * dirVector.x + dirVector.y * dirVector.y);
    
    Vector targetDir = normalize(dirVector);
    Vector velocityDir = normalize(pod.velocity);
    
    // 속도 벡터와 목표 방향 벡터의 내적 (방향 일치도, -1.0 ~ 1.0)
    float directionAlignment = dotProduct(velocityDir, targetDir);
    
    // 체크포인트에 가까워지면 적응형 추력 조절
    if (distToCheckpoint < CHECKPOINT_RADIUS * 3) {
        // 접근 속도 (속도 벡터의 체크포인트 방향 성분)
        float approachSpeed = currentSpeed * directionAlignment;
        
        // 안전 정지 거리 계산 (속도에 비례, 현재 감속률 고려)
        float deceleration = (1.0f - FRICTION) * 100.0f;
        float stoppingDistance = (approachSpeed * approachSpeed) / (2.0f * deceleration);
        
        // 정밀한 감속 요인 계산
        float speedRatio = stoppingDistance / (float)distToCheckpoint;
        float decelFactor = fminf(1.0f, speedRatio * 1.2f); // 안전 마진 20% 추가
        
        // 더 정밀한 추력 계산
        float thrustMultiplier = fmaxf(0.3f, 1.0f - decelFactor * (0.6f + 0.4f * (currentSpeed / 200.0f)));
        baseThrust *= thrustMultiplier;
        
        // 방향이 맞지 않을 때 추가 감속
        if (directionAlignment < 0.5f) {
            baseThrust *= (0.5f + directionAlignment);
        }
        
        baseThrust = fmaxf(20.0f, baseThrust);
    }
    // 코너링을 위한 추력 조절 (다음 체크포인트 각도 차이가 큰 경우)
    else if (distToCheckpoint < CHECKPOINT_RADIUS * 5) {
        int nextCpId = (pod.nextCheckpointId + 1) % checkpointCount;
        Vector currentCp = checkpoints[pod.nextCheckpointId].position;
        Vector nextCp = checkpoints[nextCpId].position;
        
        // 정밀한 각도 계산을 위해 atan2 사용
        float currentAngle = atan2f(dirVector.y, dirVector.x);
        float nextAngle = atan2f(nextCp.y - currentCp.y, nextCp.x - currentCp.x);
        
        // 각도 차이 계산 (라디안)
        float angleDiffRad = nextAngle - currentAngle;
        
        // 각도 정규화 (-PI ~ PI 범위로)
        while (angleDiffRad > PI) angleDiffRad -= 2 * PI;
        while (angleDiffRad < -PI) angleDiffRad += 2 * PI;
        
        // 절대 각도 차이를 도(degree)로 변환
        float turnAngle = fabsf(RAD_TO_DEG(angleDiffRad));
        
        // 더 정밀한 코너링 계산
        if (turnAngle > 30) {
            // 회전 각도에 따른 감속 요인 (지수 함수 사용으로 더 부드러운 전환)
            float turnFactor = 1.0f - expf(-turnAngle / 60.0f);
            
            // 속도를 고려한 코너링 거리 조정
            float corneringStartDist = CHECKPOINT_RADIUS * (3.0f + turnAngle / 36.0f);
            float distFactor = (distToCheckpoint - CHECKPOINT_RADIUS * 3) / (corneringStartDist - CHECKPOINT_RADIUS * 3);
            distFactor = fmaxf(0.0f, fminf(1.0f, distFactor));
            
            // 속도를 고려한 추가 감속
            float speedFactor = fminf(1.0f, currentSpeed / 150.0f);
            float corneringDecel = turnFactor * (1.0f - distFactor) * (0.7f + 0.3f * speedFactor);
            
            // 추력 감소 적용
            baseThrust *= (1.0f - corneringDecel * 0.6f);
        }
    }
    
    // 최종 추력은 항상 0 이상 100 이하로 보장 (정밀 반올림 적용)
    return (int)(fmaxf(0.0f, fminf(100.0f, baseThrust)) + 0.5f);
}

// 게임 초기화 함수
void initializeGame(GameState* state) {
    // 초기화 입력 처리
    scanf("%d", &state->laps);
    scanf("%d", &state->checkpointCount);
    
    state->totalCheckpoints = state->laps * state->checkpointCount;
    state->firstTurn = true;
    
    // 체크포인트 정보 저장
    for (int i = 0; i < state->checkpointCount; i++) {
        scanf("%d%d", &state->checkpoints[i].position.x, &state->checkpoints[i].position.y);
        state->checkpoints[i].radius = CHECKPOINT_RADIUS;
    }
    
    // 가장 긴 체크포인트 간 거리 계산
    state->longestStretch = 0;
    for (int i = 0; i < state->checkpointCount; i++) {
        int nextIdx = (i + 1) % state->checkpointCount;
        float dist = distance(state->checkpoints[i].position, state->checkpoints[nextIdx].position);
        if (dist > state->longestStretch) {
            state->longestStretch = (int)dist;
        }
    }
    
    // 포드 초기화
    for (int i = 0; i < 2; i++) {
        state->myPods[i].boostAvailable = true;
        state->myPods[i].shieldCooldown = 0;
        state->myPods[i].checkpointsPassed = 0;
    }
}

// 게임 상태 업데이트 함수
void updateGameState(GameState* state) {
    // 내 포드 정보 입력
    for (int i = 0; i < 2; i++) {
        int x, y, vx, vy, angle, nextCheckPointId;
        scanf("%d%d%d%d%d%d", &x, &y, &vx, &vy, &angle, &nextCheckPointId);
        
        // 체크포인트를 통과했는지 확인
        if (state->myPods[i].nextCheckpointId != nextCheckPointId && !state->firstTurn) {
            state->myPods[i].checkpointsPassed++;
        }
        
        state->myPods[i].position.x = x;
        state->myPods[i].position.y = y;
        state->myPods[i].velocity.x = vx;
        state->myPods[i].velocity.y = vy;
        state->myPods[i].angle = angle;
        state->myPods[i].nextCheckpointId = nextCheckPointId;
        
        // 쉴드 쿨다운 감소
        if (state->myPods[i].shieldCooldown > 0) {
            state->myPods[i].shieldCooldown--;
        }
        
        // 진행 상황 계산
        state->myPodsInfo[i].progress = calculateProgress(
            state->myPods[i], 
            state->checkpoints, 
            state->checkpointCount
        );
        
        // 베지어 곡선 경로 계산
        state->myPodsInfo[i].racingCurve = calculateRacingCurve(
            state->myPods[i], 
            state->checkpoints, 
            state->checkpointCount
        );
    }
    
    // 적 포드 정보 입력 및 진행 상황 계산
    for (int i = 0; i < 2; i++) {
        scanf("%d%d%d%d%d%d", 
              &state->enemyPods[i].position.x, &state->enemyPods[i].position.y,
              &state->enemyPods[i].velocity.x, &state->enemyPods[i].velocity.y,
              &state->enemyPods[i].angle, &state->enemyPods[i].nextCheckpointId);
              
        // 적 진행 상황 계산
        state->enemyProgress[i] = calculateProgress(
            state->enemyPods[i], 
            state->checkpoints, 
            state->checkpointCount
        );
    }
}

// 포드 전략 결정 함수
PodCommand determinePodStrategy(Pod pod, Pod otherPod, Pod enemies[], GameState* state, int podIndex) {
    PodCommand cmd;
    cmd.podId = podIndex;
    cmd.useShield = false;
    cmd.useBoost = false;
    
    Checkpoint targetCP = state->checkpoints[pod.nextCheckpointId];
    
    // 최적의 레이싱 라인 계산
    int lookAhead = 2; // 앞의 2개 체크포인트까지 고려
    cmd.targetPos = calculateRacingLine(pod, state->checkpoints, state->checkpointCount, lookAhead);
    
    // 원치 않는 드리프트 보상
    Vector futurePos;
    futurePos.x = pod.position.x + pod.velocity.x;
    futurePos.y = pod.position.y + pod.velocity.y;
    
    float currDist = distance(pod.position, targetCP.position);
    float futureDist = distance(futurePos, targetCP.position);
    
    // 각도 차이 계산
    float angleToCPRad = angleBetween(pod.position, cmd.targetPos);
    float angleToCPDeg = RAD_TO_DEG(angleToCPRad);
    float angleDiff = fabsf(angleToCPDeg - pod.angle);
    
    if (angleDiff > 180) {
        angleDiff = 360 - angleDiff;
    }
    
    // 체크포인트에 가까워졌을 때 다음 체크포인트와의 각도가 크면 외곽을 돌아가도록 수정
    if (currDist < CHECKPOINT_RADIUS * 2) {
        int nextCpId = (pod.nextCheckpointId + 1) % state->checkpointCount;
        Vector currCpPos = state->checkpoints[pod.nextCheckpointId].position;
        Vector nextCpPos = state->checkpoints[nextCpId].position;
        
        float turnAngleRad = angleBetween(currCpPos, nextCpPos) - angleBetween(pod.position, currCpPos);
        float turnAngleDeg = RAD_TO_DEG(turnAngleRad);
        
        // 각도 정규화 (-180 ~ 180)
        while (turnAngleDeg > 180) turnAngleDeg -= 360;
        while (turnAngleDeg < -180) turnAngleDeg += 360;
        
        if (fabsf(turnAngleDeg) > 90) {
            // 회전 방향 결정 (시계/반시계)
            int turnDirection = turnAngleDeg > 0 ? 1 : -1;
            
            // 현재 체크포인트에서 90도 방향으로 오프셋 지점 계산
            float offsetAngle = angleBetween(pod.position, currCpPos) + (PI/2) * turnDirection;
            Vector offsetPoint;
            offsetPoint.x = currCpPos.x + (int)(CHECKPOINT_RADIUS * 1.5 * cosf(offsetAngle));
            offsetPoint.y = currCpPos.y + (int)(CHECKPOINT_RADIUS * 1.5 * sinf(offsetAngle));
            
            // 수정된 목표 지점 설정
            cmd.targetPos = offsetPoint;
        }
    }
    
    // 각도가 작고 미래 위치가 현재보다 체크포인트에서 더 멀면 드리프트 보상 적용
    if (angleDiff < 70 && futureDist > currDist && currDist > CHECKPOINT_RADIUS * 2) {
        Vector closestPoint = closestPointToLine(targetCP.position, pod.position, futurePos);
        Vector compensation = subtract(closestPoint, futurePos);
        compensation = scale(compensation, 1.5); // 보상 강화
        cmd.targetPos = add(cmd.targetPos, compensation);
    }
    
    // 속도와 거리에 기반한 적응형 추력 계산
    cmd.thrust = calculateAdaptiveThrust(pod, angleDiff, (int)currDist, state->checkpoints, state->checkpointCount);
    
    // 마지막 체크포인트인지 확인 (랩 완주)
    bool isLastCheckpoint = (pod.checkpointsPassed + 1) >= state->totalCheckpoints;
    
    // 부스트 사용 여부 결정
    if (pod.boostAvailable && angleDiff < 10 && !isLastCheckpoint) {
        // 첫 턴 또는 긴 직선 구간에서 부스트 사용
        if ((podIndex == 0 && state->firstTurn) || currDist > state->longestStretch * 0.6) {
            cmd.useBoost = true;
        }
    }
    
    // 쉴드 사용 여부 결정
    cmd.useShield = shouldEnableShield(pod, otherPod, enemies, state->checkpoints, state->checkpointCount);
    
    return cmd;
}

// 포드 명령어 실행 함수
void executePodCommand(PodCommand cmd) {
    if (cmd.useShield) {
        printf("%d %d SHIELD POD%d\n", cmd.targetPos.x, cmd.targetPos.y, cmd.podId);
    }
    else if (cmd.useBoost) {
        printf("%d %d BOOST POD%d\n", cmd.targetPos.x, cmd.targetPos.y, cmd.podId);
    }
    else {
        printf("%d %d %d POD%d thrust:%d\n", cmd.targetPos.x, cmd.targetPos.y, cmd.thrust, cmd.podId, cmd.thrust);
    }
}

// 메인 함수 (간소화됨)
int main() {
    // 게임 초기화
    initializeGame(&gameState);
    
    // PID 컨트롤러 초기화
    PidController pid = initPidController(0.5f, 0.1f, 0.2f, -100, 100);
    
    // 게임 루프
    while (1) {
        // 게임 상태 업데이트
        updateGameState(&gameState);
        
        // 각 포드별 전략 결정 및 명령어 실행
        for (int i = 0; i < 2; i++) {
            // 포드 전략 결정
            PodCommand cmd = determinePodStrategy(
                gameState.myPods[i], 
                gameState.myPods[1-i], 
                gameState.enemyPods, 
                &gameState, 
                i
            );
            
            // 충돌 예측 및 회피
            Vector avoidance = predictCollisionWithPid(
                gameState.myPods[i], 
                gameState.enemyPods[0], 
                &pid, 
                0.1f
            );
            
            // 회피 벡터 적용
            cmd.targetPos = add(cmd.targetPos, avoidance);
            
            // 쉴드 사용 시 쿨다운 설정
            if (cmd.useShield) {
                gameState.myPods[i].shieldCooldown = 3;
            }
            
            // 부스트 사용 시 부스트 소진
            if (cmd.useBoost) {
                gameState.myPods[i].boostAvailable = false;
            }
            
            // 명령어 실행
            executePodCommand(cmd);
        }
        
        gameState.firstTurn = false;
    }
    
    return 0;
}