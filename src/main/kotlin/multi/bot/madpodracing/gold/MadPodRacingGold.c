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

// 게임 상태 변수들
int laps;
int checkpointCount;
Checkpoint checkpoints[MAX_CHECKPOINTS];
Pod myPods[2];
Pod enemyPods[2];
int totalCheckpoints; // 총 체크포인트 수 (laps * checkpointCount)
int longestStretch = 0; // 가장 긴 체크포인트 간 거리

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
bool shouldEnableShield(Pod pod, Pod myOtherPod, Pod enemies[]);
int calculateThrust(float angleDiff, int distToCheckpoint);

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

// 쉴드 사용 여부 결정
bool shouldEnableShield(Pod pod, Pod myOtherPod, Pod enemies[]) {
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

// 메인 함수
int main() {
    // 초기화 입력 처리
    scanf("%d", &laps);
    scanf("%d", &checkpointCount);
    
    totalCheckpoints = laps * checkpointCount;
    
    // 체크포인트 정보 저장
    for (int i = 0; i < checkpointCount; i++) {
        scanf("%d%d", &checkpoints[i].position.x, &checkpoints[i].position.y);
        checkpoints[i].radius = CHECKPOINT_RADIUS;
    }
    
    // 가장 긴 체크포인트 간 거리 계산
    for (int i = 0; i < checkpointCount; i++) {
        int nextIdx = (i + 1) % checkpointCount;
        float dist = distance(checkpoints[i].position, checkpoints[nextIdx].position);
        if (dist > longestStretch) {
            longestStretch = (int)dist;
        }
    }
    
    // 포드 초기화
    for (int i = 0; i < 2; i++) {
        myPods[i].boostAvailable = true;
        myPods[i].shieldCooldown = 0;
        myPods[i].checkpointsPassed = 0;
    }
    
    // 게임 루프
    bool firstTurn = true;
    
    while (1) {
        // 내 포드 정보 입력
        for (int i = 0; i < 2; i++) {
            int x, y, vx, vy, angle, nextCheckPointId;
            scanf("%d%d%d%d%d%d", &x, &y, &vx, &vy, &angle, &nextCheckPointId);
            
            // 체크포인트를 통과했는지 확인
            if (myPods[i].nextCheckpointId != nextCheckPointId && !firstTurn) {
                myPods[i].checkpointsPassed++;
            }
            
            myPods[i].position.x = x;
            myPods[i].position.y = y;
            myPods[i].velocity.x = vx;
            myPods[i].velocity.y = vy;
            myPods[i].angle = angle;
            myPods[i].nextCheckpointId = nextCheckPointId;
            
            // 쉴드 쿨다운 감소
            if (myPods[i].shieldCooldown > 0) {
                myPods[i].shieldCooldown--;
            }
        }
        
        // 적 포드 정보 입력
        for (int i = 0; i < 2; i++) {
            scanf("%d%d%d%d%d%d", 
                  &enemyPods[i].position.x, &enemyPods[i].position.y,
                  &enemyPods[i].velocity.x, &enemyPods[i].velocity.y,
                  &enemyPods[i].angle, &enemyPods[i].nextCheckpointId);
        }
        
        // 포드별 행동 결정
        for (int i = 0; i < 2; i++) {
            Pod* pod = &myPods[i];
            Checkpoint targetCP = checkpoints[pod->nextCheckpointId];
            
            // 목표 지점 (기본: 체크포인트 위치)
            Vector targetPos = targetCP.position;
            
            // 다음 체크포인트를 미리 확인
            int nextCPId = (pod->nextCheckpointId + 1) % checkpointCount;
            Checkpoint nextCP = checkpoints[nextCPId];
            
            // 의도적인 드리프트 - 체크포인트에 곧 들어갈 것 같으면 다음 체크포인트 방향으로 조절
            if (willEnterCheckpointSoon(*pod, targetCP)) {
                // 다음 체크포인트를 약간 향하도록 목표 위치 조정
                Vector toNextCP = subtract(nextCP.position, pod->position);
                toNextCP = normalize(toNextCP);
                Vector toCurrCP = subtract(targetCP.position, pod->position);
                toCurrCP = normalize(toCurrCP);
                
                Vector blendedDirection;
                blendedDirection.x = (int)(0.8 * toCurrCP.x + 0.2 * toNextCP.x);
                blendedDirection.y = (int)(0.8 * toCurrCP.y + 0.2 * toNextCP.y);
                blendedDirection = normalize(blendedDirection);
                
                // 체크포인트 반지름 밖으로 약간 확장
                targetPos.x = targetCP.position.x + (int)(blendedDirection.x * CHECKPOINT_RADIUS * 0.5);
                targetPos.y = targetCP.position.y + (int)(blendedDirection.y * CHECKPOINT_RADIUS * 0.5);
            }
            
            // 원치 않는 드리프트 보상
            Vector futurePos;
            futurePos.x = pod->position.x + pod->velocity.x;
            futurePos.y = pod->position.y + pod->velocity.y;
            
            float currDist = distance(pod->position, targetCP.position);
            float futureDist = distance(futurePos, targetCP.position);
            
            // 각도 차이 계산
            float angleToCPRad = angleBetween(pod->position, targetPos);
            float angleToCPDeg = RAD_TO_DEG(angleToCPRad);
            float angleDiff = fabsf(angleToCPDeg - pod->angle);
            
            if (angleDiff > 180) {
                angleDiff = 360 - angleDiff;
            }
            
            // 각도가 작고 미래 위치가 현재보다 체크포인트에서 더 멀면 드리프트 보상 적용
            if (angleDiff < 70 && futureDist > currDist && currDist > CHECKPOINT_RADIUS * 2) {
                Vector closestPoint = closestPointToLine(targetCP.position, pod->position, futurePos);
                Vector compensation = subtract(closestPoint, futurePos);
                compensation = scale(compensation, 1.5); // 보상 강화
                targetPos = add(targetPos, compensation);
            }
            
            // 행동 결정: 추력, 부스트 또는 쉴드
            int thrust = calculateThrust(angleDiff, (int)currDist);
            
            // 마지막 체크포인트인지 확인 (랩 완주)
            bool isLastCheckpoint = (pod->checkpointsPassed + 1) >= totalCheckpoints;
            
            // 부스트 사용 여부 결정
            bool useBoost = false;
            if (pod->boostAvailable && !isLastCheckpoint) {
                // 첫 번째 포드는 첫 턴에 부스트 사용
                if (i == 0 && firstTurn) {
                    useBoost = true;
                }
                // 두 번째 포드는 긴 거리에서 적절한 각도일 때 부스트 사용
                else if (i == 1 && angleDiff < 5 && currDist > longestStretch * 0.7) {
                    useBoost = true;
                }
            }
            
            // 쉴드 사용 여부 결정
            bool useShield = shouldEnableShield(*pod, myPods[1-i], enemyPods);
            
            // 쉴드 사용 시 쿨다운 설정
            if (useShield) {
                pod->shieldCooldown = 3;
            }
            
            // 출력
            if (useShield) {
                printf("%d %d SHIELD\n", targetPos.x, targetPos.y);
            }
            else if (useBoost) {
                printf("%d %d BOOST\n", targetPos.x, targetPos.y);
                pod->boostAvailable = false;
            }
            else {
                printf("%d %d %d\n", targetPos.x, targetPos.y, thrust);
            }
        }
        
        firstTurn = false;
    }
    
    return 0;
}