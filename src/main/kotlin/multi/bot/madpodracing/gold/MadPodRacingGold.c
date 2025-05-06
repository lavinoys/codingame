#include <stdlib.h>
#include <stdio.h>
#include <string.h>
#include <stdbool.h>
#include <math.h>

// 상수 정의
#define MAX_CHECKPOINTS 8
#define POD_COUNT 2
#define OPPONENT_COUNT 2
#define CHECKPOINT_RADIUS 600
#define SHIELD_DURATION 3

// 거리 관련 상수
#define CLOSE_CHECKPOINT_DISTANCE 2000
#define VERY_CLOSE_CHECKPOINT_DISTANCE 1000
#define FAR_CHECKPOINT_DISTANCE 4000
#define COLLISION_THRESHOLD 850

// 각도 관련 상수
#define ANGLE_LARGE_THRESHOLD 90
#define ANGLE_MEDIUM_THRESHOLD 45
#define ANGLE_SMALL_THRESHOLD 10

// 추진력 관련 상수
#define THRUST_FULL 100
#define THRUST_MEDIUM 70
#define THRUST_LOW 50
#define THRUST_NONE 0

// 팟의 역할을 나타내는 상수
#define POD_ROLE_RACER 1
#define POD_ROLE_DEFENDER 2

// 삼각함수 룩업 테이블을 위한 상수
#define ANGLE_TABLE_SIZE 360
#define TO_RADIANS (M_PI / 180.0)
#define TO_DEGREES (180.0 / M_PI)

// 좌표를 저장하는 구조체
typedef struct {
    int x;
    int y;
} Point;

// 벡터 캐싱을 위한 구조체 확장
typedef struct {
    double x;
    double y;
    double magnitude;  // 벡터 크기
    double angle;      // 벡터 각도 (라디안)
} Vector;

// 팟 정보를 저장하는 구조체
typedef struct {
    Point position;
    Point velocity;
    int angle;
    int nextCheckpointId;
    int shieldCooldown;
    bool isRacer;  // true: 레이서, false: 방어수

    // 캐싱된 계산 값들
    double speed;                 // 속도 크기
    double distToNextCheckpoint;  // 다음 체크포인트까지의 거리
    int angleToNextCheckpoint;    // 다음 체크포인트까지의 각도
    Vector normalizedVelocity;    // 정규화된 속도 벡터
} Pod;

// 게임 상태를 저장하는 구조체
typedef struct {
    int laps;
    int checkpointCount;
    Point checkpoints[MAX_CHECKPOINTS];
    Pod myPods[POD_COUNT];
    Pod opponentPods[OPPONENT_COUNT];
    bool boostAvailable;
} GameState;

// 삼각함수 룩업 테이블
double sin_table[ANGLE_TABLE_SIZE];
double cos_table[ANGLE_TABLE_SIZE];

// 룩업 테이블 초기화
void initTrigTables() {
    for (int i = 0; i < ANGLE_TABLE_SIZE; i++) {
        double rad = i * TO_RADIANS;
        sin_table[i] = sin(rad);
        cos_table[i] = cos(rad);
    }
}

// 테이블을 이용한 sin, cos 계산
double fast_sin(double degrees) {
    int idx = (int)round(degrees) % ANGLE_TABLE_SIZE;
    if (idx < 0) idx += ANGLE_TABLE_SIZE;
    return sin_table[idx];
}

double fast_cos(double degrees) {
    int idx = (int)round(degrees) % ANGLE_TABLE_SIZE;
    if (idx < 0) idx += ANGLE_TABLE_SIZE;
    return cos_table[idx];
}

// 유틸리티 함수
double distanceSquared(Point a, Point b) {
    return (double)(b.x - a.x) * (b.x - a.x) + (double)(b.y - a.y) * (b.y - a.y);
}

double distance(Point a, Point b) {
    return sqrt(distanceSquared(a, b));
}

// 최적화된 각도 계산 함수
double angle(Point a, Point b) {
    double angleRad = atan2(b.y - a.y, b.x - a.x);
    return angleRad * TO_DEGREES;
}

// 다음 체크포인트 이후의 체크포인트 ID 계산
int getNextCheckpointId(int currentId, int checkpointCount) {
    return (currentId + 1) % checkpointCount;
}

// 각도 차이 계산 (-180 ~ 180)
int angleDiff(int a1, int a2) {
    int diff = ((a2 - a1 + 180) % 360) - 180;
    return diff < -180 ? diff + 360 : diff;
}

// 벡터 정규화 및 캐싱
Vector normalizeVector(Point vec) {
    Vector result;
    double magnitude = sqrt(vec.x * vec.x + vec.y * vec.y);
    
    result.x = magnitude > 0 ? vec.x / magnitude : 0;
    result.y = magnitude > 0 ? vec.y / magnitude : 0;
    result.magnitude = magnitude;
    result.angle = atan2(result.y, result.x);
    
    return result;
}

// Pod의 캐시된 값들 업데이트
void updatePodCache(Pod *pod, Point nextCheckpoint) {
    pod->speed = sqrt(pod->velocity.x * pod->velocity.x + pod->velocity.y * pod->velocity.y);
    Point velPoint = {pod->velocity.x, pod->velocity.y};
    pod->normalizedVelocity = normalizeVector(velPoint);
    pod->distToNextCheckpoint = distance(pod->position, nextCheckpoint);
    pod->angleToNextCheckpoint = (int)angle(pod->position, nextCheckpoint);
}

// 게임 상태의 모든 Pod에 대해 캐시 업데이트
void updateAllPodCaches(GameState *gameState) {
    for (int i = 0; i < POD_COUNT; i++) {
        Point nextCP = gameState->checkpoints[gameState->myPods[i].nextCheckpointId];
        updatePodCache(&gameState->myPods[i], nextCP);
    }
    
    for (int i = 0; i < OPPONENT_COUNT; i++) {
        Point nextCP = gameState->checkpoints[gameState->opponentPods[i].nextCheckpointId];
        updatePodCache(&gameState->opponentPods[i], nextCP);
    }
}

// 최적화된 속도 계산 함수 - 캐시된 값 사용
double getPodSpeed(Pod pod) {
    return pod.speed;
}

// 속도에 따른 최적 선회 반경 계산 - 최적화 버전
double calculateOptimalTurningRadius(Pod pod) {
    return 300 + pod.speed * 2;
}

// 레이싱 라인 최적화 - 코너링을 위한 목표 지점 조정 (최적화 버전)
Point optimizeRacingLine(GameState *gameState, Pod pod) {
    Point currentCP = gameState->checkpoints[pod.nextCheckpointId];
    int nextCPId = getNextCheckpointId(pod.nextCheckpointId, gameState->checkpointCount);
    Point nextCP = gameState->checkpoints[nextCPId];
    
    double distToCP = pod.distToNextCheckpoint;
    
    if (distToCP < CLOSE_CHECKPOINT_DISTANCE) {
        double angleBetweenCPs = angle(currentCP, nextCP);
        double currentAngleToCP = pod.angleToNextCheckpoint;
        double angleDifference = angleDiff((int)currentAngleToCP, (int)angleBetweenCPs);
        
        double turningFactor = abs(angleDifference) / 180.0;
        double optimalRadius = calculateOptimalTurningRadius(pod);
        double offsetFactor = turningFactor * optimalRadius;
        
        Point adjusted;
        double ratio = offsetFactor / distToCP;
        if (ratio > 0.5) ratio = 0.5;
        
        double offsetAngle = angleDifference > 0 ? currentAngleToCP - 90 : currentAngleToCP + 90;
        double offsetAngleRad = offsetAngle * TO_RADIANS;
        
        adjusted.x = currentCP.x + (int)(cos(offsetAngleRad) * offsetFactor);
        adjusted.y = currentCP.y + (int)(sin(offsetAngleRad) * offsetFactor);
        
        return adjusted;
    }
    
    return currentCP;
}

// 목표 지점 조정 (속도 벡터와 다음 체크포인트를 고려) - 최적화 버전
Point adjustTargetPoint(Pod pod, Point target, GameState *gameState) {
    Point adjusted = target;
    double distToCP = pod.distToNextCheckpoint;
    
    if (distToCP < CLOSE_CHECKPOINT_DISTANCE) {
        int nextNextId = getNextCheckpointId(pod.nextCheckpointId, gameState->checkpointCount);
        Point nextTarget = gameState->checkpoints[nextNextId];
        
        double blendFactor = 1.0 - (distToCP / CLOSE_CHECKPOINT_DISTANCE);
        adjusted.x = target.x * (1 - blendFactor * 0.3) + nextTarget.x * (blendFactor * 0.3);
        adjusted.y = target.y * (1 - blendFactor * 0.3) + nextTarget.y * (blendFactor * 0.3);
    }
    
    double speed = pod.speed;
    if (speed > 200 && distToCP < CLOSE_CHECKPOINT_DISTANCE) {
        double factor = 1.0 - (distToCP / CLOSE_CHECKPOINT_DISTANCE);
        adjusted.x -= (int)(pod.normalizedVelocity.x * factor * 3 * speed);
        adjusted.y -= (int)(pod.normalizedVelocity.y * factor * 3 * speed);
    }
    
    return adjusted;
}

// 속도와 각도에 따른 스로틀 계산 (최적화 버전)
int calculateThrust(Pod pod, Point target, int podRole, double opponentDist) {
    int thrust = THRUST_FULL;
    int angleToTarget = (int)angle(pod.position, target);
    int angleDifference = abs(angleDiff(pod.angle, angleToTarget));
    
    if (angleDifference > ANGLE_LARGE_THRESHOLD) {
        thrust = THRUST_NONE;
    } else if (angleDifference > ANGLE_MEDIUM_THRESHOLD) {
        thrust = THRUST_LOW;
    } else {
        if (podRole == POD_ROLE_RACER) {
            double dist = pod.distToNextCheckpoint;
            if (dist < VERY_CLOSE_CHECKPOINT_DISTANCE) {
                double reduction = 1.0 - (VERY_CLOSE_CHECKPOINT_DISTANCE - dist) / VERY_CLOSE_CHECKPOINT_DISTANCE;
                thrust = (int)(THRUST_MEDIUM * reduction);
                if (thrust < THRUST_LOW) thrust = THRUST_LOW;
            }
        } else if (podRole == POD_ROLE_DEFENDER) {
            if (opponentDist < 1000) {
                thrust = THRUST_MEDIUM;
            }
        }
    }
    
    if (pod.shieldCooldown > 0) {
        thrust = THRUST_NONE;
    }
    
    return thrust;
}

// 명령 출력 공통 함수 (디버깅 메시지 추가)
void executeCommand(Point target, int thrust, bool useShield, bool useBoost, int podIndex, bool isRacer) {
    const char* role = isRacer ? "RACER" : "BLOCKER";
    
    if (useShield) {
        printf("%d %d SHIELD [Pod %d: %s] SHIELD\n", target.x, target.y, podIndex, role);
    } else if (useBoost) {
        printf("%d %d BOOST [Pod %d: %s] BOOST\n", target.x, target.y, podIndex, role);
    } else {
        printf("%d %d %d [Pod %d: %s] thrust: %d\n", target.x, target.y, thrust, podIndex, role, thrust);
    }
}

// 충돌 예측 및 심각도 계산 (최적화 버전)
double predictCollision(Pod pod, Pod opponent, int timeSteps) {
    Point futurePos1 = pod.position;
    Point futurePos2 = opponent.position;
    
    for (int i = 0; i < timeSteps; i++) {
        futurePos1.x += pod.velocity.x;
        futurePos1.y += pod.velocity.y;
        futurePos2.x += opponent.velocity.x;
        futurePos2.y += opponent.velocity.y;
    }
    
    double collisionDistSquared = distanceSquared(futurePos1, futurePos2);
    double thresholdSquared = COLLISION_THRESHOLD * COLLISION_THRESHOLD;
    if (collisionDistSquared < thresholdSquared) {
        double collisionDist = sqrt(collisionDistSquared);
        double speedDiff = sqrt(
            (pod.velocity.x - opponent.velocity.x) * (pod.velocity.x - opponent.velocity.x) +
            (pod.velocity.y - opponent.velocity.y) * (pod.velocity.y - opponent.velocity.y)
        );
        
        return collisionDist - speedDiff * 2;
    }
    
    return 9999;
}

// 충돌 회피 전략 결정 (최적화 버전)
bool shouldUseShield(Pod pod, Pod opponents[], int opponentCount) {
    if (pod.shieldCooldown > 0) return false;
    
    for (int i = 0; i < opponentCount; i++) {
        double currentDistSquared = distanceSquared(pod.position, opponents[i].position);
        double thresholdSquared = COLLISION_THRESHOLD * COLLISION_THRESHOLD;
        
        if (currentDistSquared < thresholdSquared) {
            double speedPod = pod.speed;
            double speedOpponent = opponents[i].speed;
            
            if (speedPod + speedOpponent > 400 && currentDistSquared < 500 * 500) {
                return true;
            }
        }
        
        for (int step = 1; step <= 3; step++) {
            double collisionSeverity = predictCollision(pod, opponents[i], step);
            if (collisionSeverity < 300) {
                return true;
            }
        }
    }
    
    return false;
}

// 최적 BOOST 사용 결정 (최적화 버전)
bool shouldUseBoost(GameState *gameState, Pod pod) {
    if (!gameState->boostAvailable) return false;
    
    int angleToTarget = pod.angleToNextCheckpoint;
    int angleDifference = abs(angleDiff(pod.angle, angleToTarget));
    double distToCP = pod.distToNextCheckpoint;
    
    if (distToCP > FAR_CHECKPOINT_DISTANCE && angleDifference < ANGLE_SMALL_THRESHOLD) {
        int nextCPId = getNextCheckpointId(pod.nextCheckpointId, gameState->checkpointCount);
        Point nextCP = gameState->checkpoints[nextCPId];
        
        double angleToNextCP = angle(gameState->checkpoints[pod.nextCheckpointId], nextCP);
        double angleDiffToNextCP = abs(angleDiff(angleToTarget, (int)angleToNextCP));
        
        if (angleDiffToNextCP < 45) {
            return true;
        }
    }
    
    return false;
}

// 어떤 팟이 더 앞서 있는지 비교 (최적화 버전)
int compareRaceProgress(Pod pod1, Pod pod2, Point checkpoints[], int checkpointCount) {
    int progress1 = pod1.nextCheckpointId * 10000;
    int progress2 = pod2.nextCheckpointId * 10000;
    
    double distToNextCP1 = pod1.distToNextCheckpoint;
    double distToNextCP2 = pod2.distToNextCheckpoint;
    
    progress1 -= (int)distToNextCP1;
    progress2 -= (int)distToNextCP2;
    
    return progress1 - progress2;
}

// 동적 역할 결정
void determineRoles(GameState *gameState) {
    gameState->myPods[0].isRacer = true;
    gameState->myPods[1].isRacer = false;
    
    Pod myRacer = gameState->myPods[0];
    Pod myDefender = gameState->myPods[1];
    
    int leaderIndex = findOpponentLeader(gameState);
    Pod opponentLeader = gameState->opponentPods[leaderIndex];
    
    int progressDifference = compareRaceProgress(myRacer, opponentLeader, 
                                                gameState->checkpoints, 
                                                gameState->checkpointCount);
    
    if (progressDifference < -5000) {
        int defenderProgress = compareRaceProgress(myDefender, myRacer, 
                                                  gameState->checkpoints, 
                                                  gameState->checkpointCount);
        
        if (defenderProgress > 0) {
            gameState->myPods[0].isRacer = false;
            gameState->myPods[1].isRacer = true;
        }
    }
}

// 레이서 팟 제어 함수 (최적화 버전)
void controlRacerPod(GameState *gameState, Pod *pod, int podIndex) {
    Point targetCheckpoint = gameState->checkpoints[pod->nextCheckpointId];
    double distToCheckpoint = pod->distToNextCheckpoint;
    
    Point racingTarget = optimizeRacingLine(gameState, *pod);
    Point adjustedTarget = adjustTargetPoint(*pod, racingTarget, gameState);
    
    int thrust = calculateThrust(*pod, adjustedTarget, POD_ROLE_RACER, 0);
    
    bool useBoost = shouldUseBoost(gameState, *pod);
    if (useBoost) {
        gameState->boostAvailable = false;
    }
    
    bool useShield = shouldUseShield(*pod, gameState->opponentPods, OPPONENT_COUNT);
    if (useShield) {
        pod->shieldCooldown = SHIELD_DURATION;
    }
    
    executeCommand(adjustedTarget, thrust, useShield, useBoost, podIndex, true);
}

// 상대방 리더 팟 식별 (최적화 버전)
int findOpponentLeader(GameState *gameState) {
    int leader = 0;
    int maxProgress = -99999;
    
    for (int i = 0; i < OPPONENT_COUNT; i++) {
        if (i == 0) {
            maxProgress = 0;
            leader = 0;
        } else {
            int progress = compareRaceProgress(gameState->opponentPods[i], 
                                             gameState->opponentPods[leader], 
                                             gameState->checkpoints, 
                                             gameState->checkpointCount);
            if (progress > 0) {
                leader = i;
            }
        }
    }
    
    return leader;
}

// 방어수 팟 제어 함수 (최적화 버전)
void controlDefenderPod(GameState *gameState, Pod *pod, int podIndex) {
    int leaderIndex = findOpponentLeader(gameState);
    Pod opponentLeader = gameState->opponentPods[leaderIndex];
    
    Point opponentTarget = gameState->checkpoints[opponentLeader.nextCheckpointId];
    Point interceptPoint;
    double distOpponentToCP = opponentLeader.distToNextCheckpoint;
    
    if (distOpponentToCP < CLOSE_CHECKPOINT_DISTANCE) {
        double ratio = 0.3;
        interceptPoint.x = opponentLeader.position.x * (1-ratio) + opponentTarget.x * ratio;
        interceptPoint.y = opponentLeader.position.y * (1-ratio) + opponentTarget.y * ratio;
    } else {
        double timeToIntercept = 2.0;
        interceptPoint.x = opponentLeader.position.x + (int)(opponentLeader.velocity.x * timeToIntercept);
        interceptPoint.y = opponentLeader.position.y + (int)(opponentLeader.velocity.y * timeToIntercept);
    }
    
    double distToOpponent = distance(pod->position, opponentLeader.position);
    
    bool useShield = shouldUseShield(*pod, gameState->opponentPods, OPPONENT_COUNT);
    if (useShield) {
        pod->shieldCooldown = SHIELD_DURATION;
    }
    
    int thrust = calculateThrust(*pod, interceptPoint, POD_ROLE_DEFENDER, distToOpponent);
    
    executeCommand(interceptPoint, thrust, useShield, false, podIndex, false);
}

int main()
{
    initTrigTables();
    
    GameState gameState;
    gameState.boostAvailable = true;
    
    scanf("%d", &gameState.laps);
    scanf("%d", &gameState.checkpointCount);
    
    for (int i = 0; i < gameState.checkpointCount; i++) {
        scanf("%d%d", &gameState.checkpoints[i].x, &gameState.checkpoints[i].y);
    }
    
    for (int i = 0; i < POD_COUNT; i++) {
        gameState.myPods[i].shieldCooldown = 0;
        gameState.myPods[i].speed = 0;
    }
    
    gameState.myPods[0].isRacer = true;
    gameState.myPods[1].isRacer = false;
    
    while (1) {
        for (int i = 0; i < POD_COUNT; i++) {
            scanf("%d%d%d%d%d%d", 
                &gameState.myPods[i].position.x, &gameState.myPods[i].position.y, 
                &gameState.myPods[i].velocity.x, &gameState.myPods[i].velocity.y, 
                &gameState.myPods[i].angle, &gameState.myPods[i].nextCheckpointId);
            
            if (gameState.myPods[i].shieldCooldown > 0) {
                gameState.myPods[i].shieldCooldown--;
            }
        }
        
        for (int i = 0; i < OPPONENT_COUNT; i++) {
            scanf("%d%d%d%d%d%d", 
                &gameState.opponentPods[i].position.x, &gameState.opponentPods[i].position.y, 
                &gameState.opponentPods[i].velocity.x, &gameState.opponentPods[i].velocity.y, 
                &gameState.opponentPods[i].angle, &gameState.opponentPods[i].nextCheckpointId);
        }
        
        updateAllPodCaches(&gameState);
        
        determineRoles(&gameState);
        
        for (int i = 0; i < POD_COUNT; i++) {
            if (gameState.myPods[i].isRacer) {
                controlRacerPod(&gameState, &gameState.myPods[i], i);
            } else {
                controlDefenderPod(&gameState, &gameState.myPods[i], i);
            }
        }
    }

    return 0;
}
