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

// 좌표를 저장하는 구조체
typedef struct {
    int x;
    int y;
} Point;

// 팟 정보를 저장하는 구조체
typedef struct {
    Point position;
    Point velocity;
    int angle;
    int nextCheckpointId;
    int shieldCooldown;
    bool isRacer;  // true: 레이서, false: 방어수
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

// 유틸리티 함수
double distanceSquared(Point a, Point b) {
    return pow(b.x - a.x, 2) + pow(b.y - a.y, 2);
}

double distance(Point a, Point b) {
    return sqrt(distanceSquared(a, b));
}

double angle(Point a, Point b) {
    return atan2(b.y - a.y, b.x - a.x) * 180 / M_PI;
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

// 속도에 따른 최적 선회 반경 계산
double calculateOptimalTurningRadius(Pod pod) {
    double speed = sqrt(pod.velocity.x * pod.velocity.x + pod.velocity.y * pod.velocity.y);
    // 속도가 빠를수록 선회 반경 증가
    return 300 + speed * 2;
}

// 레이싱 라인 최적화 - 코너링을 위한 목표 지점 조정
Point optimizeRacingLine(GameState *gameState, Pod pod) {
    Point currentCP = gameState->checkpoints[pod.nextCheckpointId];
    int nextCPId = getNextCheckpointId(pod.nextCheckpointId, gameState->checkpointCount);
    Point nextCP = gameState->checkpoints[nextCPId];
    
    double distToCP = distance(pod.position, currentCP);
    
    // 체크포인트에 충분히 가까우면 다음 체크포인트를 고려한 레이싱 라인 계산
    if (distToCP < CLOSE_CHECKPOINT_DISTANCE) {
        // 현재 체크포인트와 다음 체크포인트 사이의 각도 계산
        double angleBetweenCPs = angle(currentCP, nextCP);
        double currentAngleToCP = angle(pod.position, currentCP);
        double angleDifference = angleDiff((int)currentAngleToCP, (int)angleBetweenCPs);
        
        // 코너의 안쪽을 지나도록 목표 지점 조정
        double turningFactor = abs(angleDifference) / 180.0; // 0(직선) ~ 1(급회전)
        double optimalRadius = calculateOptimalTurningRadius(pod);
        double offsetFactor = turningFactor * optimalRadius;
        
        // 조정된 목표 지점
        Point adjusted;
        double ratio = offsetFactor / distToCP;
        if (ratio > 0.5) ratio = 0.5; // 너무 크게 벗어나지 않도록 제한
        
        // 코너 바깥쪽으로 접근하여 안쪽으로 회전하는 레이싱 라인
        double offsetAngle = angleDifference > 0 ? currentAngleToCP - 90 : currentAngleToCP + 90;
        offsetAngle = offsetAngle * M_PI / 180.0; // 라디안으로 변환
        
        adjusted.x = currentCP.x + cos(offsetAngle) * offsetFactor;
        adjusted.y = currentCP.y + sin(offsetAngle) * offsetFactor;
        
        return adjusted;
    }
    
    return currentCP;
}

// 목표 지점 조정 (속도 벡터와 다음 체크포인트를 고려)
Point adjustTargetPoint(Pod pod, Point target, double distToCP, GameState *gameState) {
    Point adjusted = target;
    
    // 체크포인트에 가까워질수록 다음 체크포인트를 고려
    if (distToCP < CLOSE_CHECKPOINT_DISTANCE) {
        int nextNextId = getNextCheckpointId(pod.nextCheckpointId, gameState->checkpointCount);
        Point nextTarget = gameState->checkpoints[nextNextId];
        
        // 거리에 따라 다음 체크포인트 고려 비율 조정
        double blendFactor = 1.0 - (distToCP / CLOSE_CHECKPOINT_DISTANCE);
        adjusted.x = target.x * (1 - blendFactor * 0.3) + nextTarget.x * (blendFactor * 0.3);
        adjusted.y = target.y * (1 - blendFactor * 0.3) + nextTarget.y * (blendFactor * 0.3);
    }
    
    // 속도 벡터 고려하여 선회 최적화
    double speed = sqrt(pod.velocity.x * pod.velocity.x + pod.velocity.y * pod.velocity.y);
    if (speed > 200 && distToCP < CLOSE_CHECKPOINT_DISTANCE) {
        double factor = 1.0 - (distToCP / CLOSE_CHECKPOINT_DISTANCE);
        // 속도가 높을 때 관성을 고려하여 미리 회전 시작
        adjusted.x -= pod.velocity.x * factor * 3 / speed;
        adjusted.y -= pod.velocity.y * factor * 3 / speed;
    }
    
    return adjusted;
}

// 속도와 각도에 따른 스로틀 계산
int calculateThrust(Pod pod, Point target, double dist) {
    int thrust = THRUST_FULL;
    
    int angleToTarget = (int)angle(pod.position, target);
    int angleDifference = abs(angleDiff(pod.angle, angleToTarget));
    
    // 각도와 거리에 따른 동적 스로틀 조절
    if (angleDifference > ANGLE_LARGE_THRESHOLD) {
        thrust = THRUST_NONE;
    } else if (angleDifference > ANGLE_MEDIUM_THRESHOLD) {
        thrust = THRUST_LOW;
    } else if (dist < VERY_CLOSE_CHECKPOINT_DISTANCE) {
        // 체크포인트에 매우 가까울 때 속도 조절
        double reduction = 1.0 - (VERY_CLOSE_CHECKPOINT_DISTANCE - dist) / VERY_CLOSE_CHECKPOINT_DISTANCE;
        thrust = (int)(THRUST_MEDIUM * reduction);
        if (thrust < THRUST_LOW) thrust = THRUST_LOW;
    }
    
    // 쉴드 사용 중이면 가속 불가
    if (pod.shieldCooldown > 0) {
        thrust = THRUST_NONE;
    }
    
    return thrust;
}

// 충돌 예측 및 심각도 계산
double predictCollision(Pod pod, Pod opponent, int timeSteps) {
    Point futurePos1 = pod.position;
    Point futurePos2 = opponent.position;
    
    // 미래 위치 예측
    for (int i = 0; i < timeSteps; i++) {
        futurePos1.x += pod.velocity.x;
        futurePos1.y += pod.velocity.y;
        futurePos2.x += opponent.velocity.x;
        futurePos2.y += opponent.velocity.y;
    }
    
    double collisionDist = distance(futurePos1, futurePos2);
    
    if (collisionDist < COLLISION_THRESHOLD) {
        // 속도 차이가 클수록 충돌 심각도 증가
        Point relativeVelocity = {
            pod.velocity.x - opponent.velocity.x,
            pod.velocity.y - opponent.velocity.y
        };
        double speedDiff = sqrt(relativeVelocity.x * relativeVelocity.x + relativeVelocity.y * relativeVelocity.y);
        
        // 충돌 심각도 (낮을수록 심각)
        return collisionDist - speedDiff * 2;
    }
    
    return 9999; // 충돌 없음
}

// 충돌 회피 전략 결정
bool shouldUseShield(Pod pod, Pod opponents[], int opponentCount) {
    if (pod.shieldCooldown > 0) return false;
    
    for (int i = 0; i < opponentCount; i++) {
        double currentDist = distance(pod.position, opponents[i].position);
        
        if (currentDist < COLLISION_THRESHOLD) {
            // 속도 차이 계산
            double speedPod = sqrt(pod.velocity.x * pod.velocity.x + pod.velocity.y * pod.velocity.y);
            double speedOpponent = sqrt(opponents[i].velocity.x * opponents[i].velocity.x + 
                                       opponents[i].velocity.y * opponents[i].velocity.y);
            
            // 상대적 속도가 크고 거리가 가까우면 쉴드 사용
            if (speedPod + speedOpponent > 400 && currentDist < 500) {
                return true;
            }
        }
        
        // 미래 충돌 예측
        for (int step = 1; step <= 3; step++) {
            double collisionSeverity = predictCollision(pod, opponents[i], step);
            if (collisionSeverity < 300) { // 심각한 충돌 예상
                return true;
            }
        }
    }
    
    return false;
}

// 최적 BOOST 사용 결정
bool shouldUseBoost(GameState *gameState, Pod pod) {
    if (!gameState->boostAvailable) return false;
    
    int angleToTarget = (int)angle(pod.position, gameState->checkpoints[pod.nextCheckpointId]);
    int angleDifference = abs(angleDiff(pod.angle, angleToTarget));
    double distToCP = distance(pod.position, gameState->checkpoints[pod.nextCheckpointId]);
    
    // 직선 구간에서 거리가 충분히 멀 때 부스트 사용
    if (distToCP > FAR_CHECKPOINT_DISTANCE && angleDifference < ANGLE_SMALL_THRESHOLD) {
        // 다음 체크포인트까지 방향이 크게 바뀌지 않는지 확인
        int nextCPId = getNextCheckpointId(pod.nextCheckpointId, gameState->checkpointCount);
        Point nextCP = gameState->checkpoints[nextCPId];
        
        double angleToNextCP = angle(gameState->checkpoints[pod.nextCheckpointId], nextCP);
        double angleDiffToNextCP = abs(angleDiff((int)angleToTarget, (int)angleToNextCP));
        
        // 다음 체크포인트도 비슷한 방향에 있으면 BOOST 사용
        if (angleDiffToNextCP < 45) {
            return true;
        }
    }
    
    return false;
}

// 레이서 팟 제어 함수
void controlRacerPod(GameState *gameState, Pod *pod, int podIndex) {
    Point targetCheckpoint = gameState->checkpoints[pod->nextCheckpointId];
    double distToCheckpoint = distance(pod->position, targetCheckpoint);
    
    // 레이싱 라인 최적화
    Point racingTarget = optimizeRacingLine(gameState, *pod);
    
    // 목표 지점 조정
    Point adjustedTarget = adjustTargetPoint(*pod, racingTarget, distToCheckpoint, gameState);
    
    // 스로틀 계산
    int thrust = calculateThrust(*pod, adjustedTarget, distToCheckpoint);
    
    // BOOST 사용 결정
    bool useBoost = shouldUseBoost(gameState, *pod);
    if (useBoost) {
        gameState->boostAvailable = false;
    }
    
    // 충돌 회피 (SHIELD 사용)
    bool useShield = shouldUseShield(*pod, gameState->opponentPods, OPPONENT_COUNT);
    if (useShield) {
        pod->shieldCooldown = SHIELD_DURATION;
    }
    
    // 명령 출력
    if (useShield) {
        printf("%d %d SHIELD\n", adjustedTarget.x, adjustedTarget.y);
    } else if (useBoost) {
        printf("%d %d BOOST\n", adjustedTarget.x, adjustedTarget.y);
    } else {
        printf("%d %d %d\n", adjustedTarget.x, adjustedTarget.y, thrust);
    }
}

// 상대방 리더 팟 식별 (가장 앞선 팟 찾기)
int findOpponentLeader(GameState *gameState) {
    int leader = 0;
    int maxProgress = 0;
    
    for (int i = 0; i < OPPONENT_COUNT; i++) {
        // 체크포인트 ID와 해당 체크포인트까지의 거리로 진행 상황 평가
        int progress = gameState->opponentPods[i].nextCheckpointId * 10000;
        double distToNextCP = distance(gameState->opponentPods[i].position, 
                                     gameState->checkpoints[gameState->opponentPods[i].nextCheckpointId]);
        
        // 체크포인트에 가까울수록 높은 점수
        progress -= (int)distToNextCP;
        
        if (progress > maxProgress) {
            maxProgress = progress;
            leader = i;
        }
    }
    
    return leader;
}

// 방어수 팟 제어 함수
void controlDefenderPod(GameState *gameState, Pod *pod, int podIndex) {
    // 상대방 리더 식별
    int leaderIndex = findOpponentLeader(gameState);
    Pod opponentLeader = gameState->opponentPods[leaderIndex];
    
    // 상대의 다음 체크포인트와 현재 위치
    Point opponentTarget = gameState->checkpoints[opponentLeader.nextCheckpointId];
    
    // 상대의 최적 경로 예측 및 요격 지점 계산
    Point interceptPoint;
    double distOpponentToCP = distance(opponentLeader.position, opponentTarget);
    
    // 상대가 체크포인트에 가까울수록 체크포인트 쪽에 위치
    if (distOpponentToCP < CLOSE_CHECKPOINT_DISTANCE) {
        // 상대와 체크포인트 사이에 위치
        double ratio = 0.3; // 체크포인트 쪽에 더 가까이
        interceptPoint.x = opponentLeader.position.x * (1-ratio) + opponentTarget.x * ratio;
        interceptPoint.y = opponentLeader.position.y * (1-ratio) + opponentTarget.y * ratio;
    } else {
        // 상대의 예상 경로 위에 위치 (속도와 위치 고려)
        double timeToIntercept = 2.0; // 2초 후 위치 예측
        interceptPoint.x = opponentLeader.position.x + opponentLeader.velocity.x * timeToIntercept;
        interceptPoint.y = opponentLeader.position.y + opponentLeader.velocity.y * timeToIntercept;
        
        // 예측 위치와 체크포인트 사이에 위치하도록 조정
        double distToCP = distance(interceptPoint, opponentTarget);
        if (distToCP < CLOSE_CHECKPOINT_DISTANCE) {
            // 더 체크포인트 쪽으로 조정
            double ratio = 0.3;
            interceptPoint.x = interceptPoint.x * (1-ratio) + opponentTarget.x * ratio;
            interceptPoint.y = interceptPoint.y * (1-ratio) + opponentTarget.y * ratio;
        }
    }
    
    double distToOpponent = distance(pod->position, opponentLeader.position);
    
    // 충돌 임박 시 SHIELD 사용
    bool useShield = shouldUseShield(*pod, gameState->opponentPods, OPPONENT_COUNT);
    if (useShield) {
        pod->shieldCooldown = SHIELD_DURATION;
        printf("%d %d SHIELD\n", interceptPoint.x, interceptPoint.y);
        return;
    }
    
    int thrust = THRUST_FULL;
    
    // 각도에 따른 스로틀 조절
    int angleToTarget = (int)angle(pod->position, interceptPoint);
    int angleDifference = abs(angleDiff(pod->angle, angleToTarget));
    
    if (angleDifference > ANGLE_LARGE_THRESHOLD) {
        thrust = THRUST_NONE;
    } else if (angleDifference > ANGLE_MEDIUM_THRESHOLD) {
        thrust = THRUST_LOW;
    } else if (distToOpponent < 1000) {
        // 상대방이 가까우면 속도 조절
        thrust = THRUST_MEDIUM;
    }
    
    // 쉴드 쿨다운 중이면 가속 불가
    if (pod->shieldCooldown > 0) {
        thrust = THRUST_NONE;
    }
    
    printf("%d %d %d\n", interceptPoint.x, interceptPoint.y, thrust);
}

int main()
{
    // 게임 상태 초기화
    GameState gameState;
    gameState.boostAvailable = true;
    
    scanf("%d", &gameState.laps);
    scanf("%d", &gameState.checkpointCount);
    
    // 체크포인트 위치 저장
    for (int i = 0; i < gameState.checkpointCount; i++) {
        scanf("%d%d", &gameState.checkpoints[i].x, &gameState.checkpoints[i].y);
    }
    
    // 팟 초기화
    for (int i = 0; i < POD_COUNT; i++) {
        gameState.myPods[i].shieldCooldown = 0;
    }
    
    // Pod 역할 설정
    gameState.myPods[0].isRacer = true;  // 첫 번째 팟은 레이서
    gameState.myPods[1].isRacer = false; // 두 번째 팟은 방어수/공격수
    
    // 게임 루프
    while (1) {
        // 내 팟 정보 입력
        for (int i = 0; i < POD_COUNT; i++) {
            scanf("%d%d%d%d%d%d", 
                &gameState.myPods[i].position.x, &gameState.myPods[i].position.y, 
                &gameState.myPods[i].velocity.x, &gameState.myPods[i].velocity.y, 
                &gameState.myPods[i].angle, &gameState.myPods[i].nextCheckpointId);
            
            // 쉴드 쿨다운 감소
            if (gameState.myPods[i].shieldCooldown > 0) {
                gameState.myPods[i].shieldCooldown--;
            }
        }
        
        // 상대방 팟 정보 입력
        for (int i = 0; i < OPPONENT_COUNT; i++) {
            scanf("%d%d%d%d%d%d", 
                &gameState.opponentPods[i].position.x, &gameState.opponentPods[i].position.y, 
                &gameState.opponentPods[i].velocity.x, &gameState.opponentPods[i].velocity.y, 
                &gameState.opponentPods[i].angle, &gameState.opponentPods[i].nextCheckpointId);
        }
        
        // 각 팟에 대한 명령 결정
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
