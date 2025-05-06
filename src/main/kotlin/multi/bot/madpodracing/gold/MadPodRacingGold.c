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

// 포드 정보 추가 구조체
typedef struct {
    float progress;    // 레이스 진행도
    Vector racingLine; // 최적 레이싱 라인 목표점
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
float collisionNicenessScore(Pod pod, Pod other, Checkpoint target);
bool shouldEnableShield(Pod pod, Pod myOtherPod, Pod enemies[], Checkpoint checkpoints[], int checkpointCount);
int calculateThrust(float angleDiff, int distToCheckpoint);
Vector calculateRacingLine(Pod pod, Checkpoint checkpoints[], int checkpointCount, int lookAhead);
float calculateProgress(Pod pod, Checkpoint checkpoints[], int checkpointCount);
int calculateAdaptiveThrust(Pod pod, float angleDiff, int distToCheckpoint, Checkpoint checkpoints[], int checkpointCount);
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

// 충돌 예측
bool isGoingToCollideWith(Pod pod1, Pod pod2) {
    Vector futurePos1, futurePos2;
    futurePos1.x = pod1.position.x + pod1.velocity.x;
    futurePos1.y = pod1.position.y + pod1.velocity.y;
    futurePos2.x = pod2.position.x + pod2.velocity.x;
    futurePos2.y = pod2.position.y + pod2.velocity.y;
    
    return distance(futurePos1, futurePos2) < 2 * POD_RADIUS;
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

// 레이싱 라인 계산 함수 (수정: 체크포인트 배열 매개변수 추가)
Vector calculateRacingLine(Pod pod, Checkpoint checkpoints[], int checkpointCount, int lookAhead) {
    Vector result = checkpoints[pod.nextCheckpointId].position;
    
    // 앞의 여러 체크포인트를 고려하여 레이싱 라인 계산
    if (lookAhead > 0) {
        Vector nextPoints[3]; // 최대 3개 체크포인트 고려
        float weights[3] = {0.7, 0.2, 0.1}; // 가중치
        
        // 다음 체크포인트들의 위치 저장
        for (int i = 0; i < lookAhead && i < 3; i++) {
            int cpIndex = (pod.nextCheckpointId + i) % checkpointCount;
            nextPoints[i] = checkpoints[cpIndex].position;
        }
        
        // 가중 평균 계산으로 체크포인트 사이를 부드럽게 이동
        Vector blended = {0, 0};
        float totalWeight = 0;
        
        for (int i = 0; i < lookAhead && i < 3; i++) {
            blended.x += nextPoints[i].x * weights[i];
            blended.y += nextPoints[i].y * weights[i];
            totalWeight += weights[i];
        }
        
        // 정규화
        if (totalWeight > 0) {
            blended.x /= totalWeight;
            blended.y /= totalWeight;
        }
        
        // 현재 체크포인트에서 조금 벗어난 지점을 목표로
        Vector dirToCP = subtract(checkpoints[pod.nextCheckpointId].position, pod.position);
        float distToCP = sqrtf(dirToCP.x * dirToCP.x + dirToCP.y * dirToCP.y);
        
        // 체크포인트에 접근함에 따라 앞의 체크포인트 영향력 증가
        float blendFactor;
        if (distToCP < CHECKPOINT_RADIUS * 3) {
            blendFactor = 0.5 - 0.5 * (distToCP / (CHECKPOINT_RADIUS * 3));
            
            // 목표 지점 조정
            result.x = (int)((1 - blendFactor) * result.x + blendFactor * blended.x);
            result.y = (int)((1 - blendFactor) * result.y + blendFactor * blended.y);
        }
    }
    
    return result;
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