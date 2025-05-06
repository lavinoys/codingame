#include <stdlib.h>
#include <stdio.h>
#include <string.h>
#include <stdbool.h>
#include <math.h>

#define MAX_CHECKPOINTS 8
#define POD_COUNT 2
#define OPPONENT_COUNT 2
#define CHECKPOINT_RADIUS 600
#define SHIELD_DURATION 3
#define COLLISION_THRESHOLD 850

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
    bool boostAvailable;
    bool isRacer;  // true: 레이서, false: 방어수
} Pod;

// 유틸리티 함수
double distance(Point a, Point b) {
    return sqrt(pow(b.x - a.x, 2) + pow(b.y - a.y, 2));
}

double angle(Point a, Point b) {
    return atan2(b.y - a.y, b.x - a.x) * 180 / M_PI;
}

// 다음 체크포인트까지의 거리를 계산
double distanceToTarget(Pod pod, Point checkpoints[], int checkpointCount) {
    int nextId = pod.nextCheckpointId;
    return distance(pod.position, checkpoints[nextId]);
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

// 목표 지점 조정 (속도 벡터를 고려)
Point adjustTargetPoint(Pod pod, Point target, double distance) {
    // 거리가 가까울수록 목표점을 넘어서 다음 체크포인트를 보게 함
    Point adjusted;
    if (distance < 2000) {
        double factor = 1.0 - (distance / 3000.0);
        adjusted.x = target.x - pod.velocity.x * factor * 3;
        adjusted.y = target.y - pod.velocity.y * factor * 3;
    } else {
        adjusted = target;
    }
    return adjusted;
}

// 속도에 따른 스로틀 조절
int calculateThrust(Pod pod, Point target, double dist) {
    int thrust = 100;
    
    // 각도가 크면 감속
    int angleToTarget = (int)angle(pod.position, target);
    int angleDifference = abs(angleDiff(pod.angle, angleToTarget));
    
    if (angleDifference > 90) {
        thrust = 0;
    } else if (angleDifference > 45) {
        thrust = 50;
    } else if (dist < 1000) {
        thrust = 70; // 체크포인트에 가까우면 약간 감속
    }
    
    // 쉴드 사용 중이면 가속 불가
    if (pod.shieldCooldown > 0) {
        thrust = 0;
    }
    
    return thrust;
}

// 적 팟과의 충돌 예상 여부
bool isCollisionImminent(Pod pod, Pod opponent) {
    double dist = distance(pod.position, opponent.position);
    
    // 속도 벡터의 내적으로 접근 여부 판단
    Point relativeVelocity = {
        pod.velocity.x - opponent.velocity.x,
        pod.velocity.y - opponent.velocity.y
    };
    
    Point relativePosition = {
        opponent.position.x - pod.position.x,
        opponent.position.y - pod.position.y
    };
    
    double dotProduct = relativeVelocity.x * relativePosition.x + 
                        relativeVelocity.y * relativePosition.y;
    
    // 가까워지고 있고 거리가 충돌 임계값보다 작으면
    return (dist < COLLISION_THRESHOLD && dotProduct < 0);
}

int main()
{
    int laps;
    scanf("%d", &laps);
    int checkpointCount;
    scanf("%d", &checkpointCount);
    
    // 체크포인트 위치 저장
    Point checkpoints[MAX_CHECKPOINTS];
    for (int i = 0; i < checkpointCount; i++) {
        scanf("%d%d", &checkpoints[i].x, &checkpoints[i].y);
    }
    
    Pod myPods[POD_COUNT];
    Pod opponentPods[OPPONENT_COUNT];
    
    // 초기화
    for (int i = 0; i < POD_COUNT; i++) {
        myPods[i].shieldCooldown = 0;
        myPods[i].boostAvailable = true;
    }
    
    // Pod 역할 설정
    myPods[0].isRacer = true;  // 첫 번째 팟은 레이서
    myPods[1].isRacer = false; // 두 번째 팟은 방어수/공격수
    
    // 게임 루프
    while (1) {
        // 내 팟 정보 입력
        for (int i = 0; i < POD_COUNT; i++) {
            scanf("%d%d%d%d%d%d", 
                &myPods[i].position.x, &myPods[i].position.y, 
                &myPods[i].velocity.x, &myPods[i].velocity.y, 
                &myPods[i].angle, &myPods[i].nextCheckpointId);
            
            // 쉴드 쿨다운 감소
            if (myPods[i].shieldCooldown > 0) {
                myPods[i].shieldCooldown--;
            }
        }
        
        // 상대방 팟 정보 입력
        for (int i = 0; i < OPPONENT_COUNT; i++) {
            scanf("%d%d%d%d%d%d", 
                &opponentPods[i].position.x, &opponentPods[i].position.y, 
                &opponentPods[i].velocity.x, &opponentPods[i].velocity.y, 
                &opponentPods[i].angle, &opponentPods[i].nextCheckpointId);
        }
        
        // 각 팟에 대한 명령 결정
        for (int i = 0; i < POD_COUNT; i++) {
            Pod *pod = &myPods[i];
            
            // 기본 목표는 다음 체크포인트
            Point targetPoint = checkpoints[pod->nextCheckpointId];
            double distToCheckpoint = distance(pod->position, targetPoint);
            
            // 레이서 전략 (첫 번째 팟)
            if (pod->isRacer) {
                // 다음 체크포인트 이후를 미리 고려
                if (distToCheckpoint < 2000) {
                    int nextNextId = getNextCheckpointId(pod->nextCheckpointId, checkpointCount);
                    Point nextTarget = checkpoints[nextNextId];
                    
                    // 목표점을 다음 체크포인트 방향으로 약간 조정
                    targetPoint.x = (targetPoint.x * 2 + nextTarget.x) / 3;
                    targetPoint.y = (targetPoint.y * 2 + nextTarget.y) / 3;
                }
                
                // 속도 벡터를 고려한 목표 지점 조정
                targetPoint = adjustTargetPoint(*pod, targetPoint, distToCheckpoint);
                
                // 스로틀 계산
                int thrust = calculateThrust(*pod, targetPoint, distToCheckpoint);
                
                // BOOST 사용 결정
                bool useBoost = false;
                if (pod->boostAvailable && distToCheckpoint > 4000 && 
                    abs(angleDiff(pod->angle, (int)angle(pod->position, targetPoint))) < 10) {
                    useBoost = true;
                    pod->boostAvailable = false;
                }
                
                // 충돌 임박 시 SHIELD 사용
                bool useShield = false;
                for (int j = 0; j < OPPONENT_COUNT; j++) {
                    if (isCollisionImminent(*pod, opponentPods[j]) && pod->shieldCooldown == 0) {
                        useShield = true;
                        pod->shieldCooldown = SHIELD_DURATION;
                        break;
                    }
                }
                
                // 명령 출력
                if (useShield) {
                    printf("%d %d SHIELD\n", targetPoint.x, targetPoint.y);
                } else if (useBoost) {
                    printf("%d %d BOOST\n", targetPoint.x, targetPoint.y);
                } else {
                    printf("%d %d %d\n", targetPoint.x, targetPoint.y, thrust);
                }
            }
            // 방어수/공격수 전략 (두 번째 팟)
            else {
                // 상대방 선두 팟 방해하기
                Pod opponentLeader = opponentPods[0];
                if (opponentPods[1].nextCheckpointId > opponentLeader.nextCheckpointId) {
                    opponentLeader = opponentPods[1];
                }
                
                // 자신의 체크포인트에 더 가까운 경우 레이서 역할 수행
                if (distToCheckpoint < 3000) {
                    targetPoint = adjustTargetPoint(*pod, targetPoint, distToCheckpoint);
                    int thrust = calculateThrust(*pod, targetPoint, distToCheckpoint);
                    printf("%d %d %d\n", targetPoint.x, targetPoint.y, thrust);
                } 
                // 상대 방해 전략
                else {
                    // 상대 목표 지점
                    Point interceptPoint = opponentLeader.position;
                    
                    // 상대 속도 벡터를 고려하여 요격 지점 계산
                    interceptPoint.x += opponentLeader.velocity.x * 2;
                    interceptPoint.y += opponentLeader.velocity.y * 2;
                    
                    double distToOpponent = distance(pod->position, opponentLeader.position);
                    
                    // 충돌 임박 시 SHIELD 사용
                    if (distToOpponent < COLLISION_THRESHOLD && pod->shieldCooldown == 0) {
                        printf("%d %d SHIELD\n", interceptPoint.x, interceptPoint.y);
                        pod->shieldCooldown = SHIELD_DURATION;
                    } else {
                        int thrust = 100;
                        
                        // 각도 차이가 크면 스로틀 조절
                        int angleToTarget = (int)angle(pod->position, interceptPoint);
                        int angleDifference = abs(angleDiff(pod->angle, angleToTarget));
                        
                        if (angleDifference > 90) {
                            thrust = 0;
                        } else if (angleDifference > 45) {
                            thrust = 50;
                        }
                        
                        // 쉴드 쿨다운 중이면 가속 불가
                        if (pod->shieldCooldown > 0) {
                            thrust = 0;
                        }
                        
                        printf("%d %d %d\n", interceptPoint.x, interceptPoint.y, thrust);
                    }
                }
            }
        }
    }

    return 0;
}
