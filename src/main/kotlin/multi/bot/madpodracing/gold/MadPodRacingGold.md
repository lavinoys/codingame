# <https://www.codingame.com/multiplayer/bot-programming/mad-pod-racing>

### What will I learn?

This puzzle game starts with a step by step tutorial that will help you get familiar with CodinGame’s multiplayer games. It provides an easy introduction to bot programming through a starship race.
The aim of the game is of course to win the race against other players! To succeed in this challenge, you will be able to use different mathematical concepts such as trajectory calculation, collisions, speed vector, or inertia.
The game is very simple to start. Rules are easy to understand and it only requires a few lines of code to move your ship around.

However, it has near-infinite possibilities of evolution as you can improve your artificial intelligence step by step, while sharpening your coding skills.

LEARN ALGORITHMS ASSOCIATED WITH THIS PUZZLE

Genetic Algorithms by Sablier
Smitsimax par MSmits
External resources PID ControllerMulti-agent systemSteering Behaviors (Seek)Neural Network ResourcesGenetic Algorithm post-mortem by MagusGenetic Algorithm post-mortem by Jeff06Genetic Algorithm post-mortem by pb4608
Learning Opportunities
This puzzle can be solved using the following concepts. Practice using these concepts and improve your skills.

Optimization
Neural network
Multi-agent
Distances
Trigonometry
Statement

This puzzle is divided into two distinct parts:

From Wood to Silver League: A tutorial containing several missions for newcomers to the multiplayer mode with a simple goal: your program must win the race. You'll unlock new rules at every league.

From Gold league upwards: You will be given a large number of parameters to manage (list of waypoints, vector speed, remaining boosts ...) in order to improve your AI.

## Summary of new rules

You now control two pods instead of one. Additionally, you will receive exhaustive game information to create a powerful AI.
The i/o protocol has been modified (Pro tip: you can refresh the default code). Please refer to the updated statement section for details.

## The Goal

### Win the race.

#### Rules

The players each control a team of two pods during a race. As soon as a pod completes the race, that pod's team is declared the winner.
The circuit of the race is made up of checkpoints. To complete one lap, your vehicle (pod) must pass through each one in order and back through the start. The first player to reach the start on the final lap wins.

The game is played on a map 16000 units wide and 9000 units high. The coordinate X=0, Y=0 is the top left pixel.

The checkpoints work as follows:
The checkpoints are circular, with a radius of 600 units.
Checkpoints are numbered from 0 to N where 0 is the start and N-1 is the last checkpoint.
The disposition of the checkpoints is selected randomly for each race.
The pods work as follows:
To pass a checkpoint, the center of a pod must be inside the radius of the checkpoint.
To move a pod, you must print a target destination point followed by a thrust value. Details of the protocol can be found further down.
The thrust value of a pod is its acceleration and must be between 0 and 100.
The pod will pivot to face the destination point by a maximum of 18 degrees per turn and will then accelerate in that direction.
You can use 1 acceleration boost in the race, you only need to replace the thrust value by the BOOST keyword.
You may activate a pod's shields with the SHIELD command instead of accelerating. This will give the pod much more weight if it collides with another. However, the pod will not be able to accelerate for the next 3 turns.
The pods have a circular force-field around their center, with a radius of 400 units, which activates in case of collisions with other pods.
The pods may move normally outside the game area.
If none of your pods make it to their next checkpoint in under 100 turns, you are eliminated and lose the game. Only one pod need to complete the race.

Note: You may activate debug mode in the settings panel () to view additional game data.

쉴드는 3턴간 유지해야 유의미하다.

example

``` text
x y shield
x y shield
x y shield
```
 
#### Victory Conditions

Be the first to complete all the laps of the circuit with one pod.
 
#### Lose Conditions

Your program provides incorrect output.
Your program times out.
None of your pods reach their next checkpoint in time.
Somebody else wins.

### Expert Rules

On each turn the pods movements are computed this way:
Rotation: the pod rotates to face the target point, with a maximum of 18 degrees (except for the 1rst round).
Acceleration: the pod's facing vector is multiplied by the given thrust value. The result is added to the current speed vector.
Movement: The speed vector is added to the position of the pod. If a collision would occur at this point, the pods rebound off each other.
Friction: the current speed vector of each pod is multiplied by 0.85
The speed's values are truncated and the position's values are rounded to the nearest integer.
Collisions are elastic. The minimum impulse of a collision is 120.
A boost is in fact an acceleration of 650. The number of boost available is common between pods. If no boost is available, the maximum thrust is used.
A shield multiplies the Pod mass by 10.
The provided angle is absolute. 0° means facing EAST while 90° means facing SOUTH.

### Note

The program must first read the initialization data from standard input. Then, within an infinite loop, read the contextual data from the standard input and provide to the standard output the desired instructions.

### Game Input

#### Initialization input

Line 1: laps : the number of laps to complete the race.
Line 2: checkpointCount : the number of checkpoints in the circuit.
Next checkpointCount lines : 2 integers checkpointX , checkpointY for the coordinates of checkpoint.
Input for one game turn
First 2 lines: Your two pods.
Next 2 lines: The opponent's pods.
Each pod is represented by: 6 integers, x & y for the position. vx & vy for the speed vector. angle for the rotation angle in degrees. nextCheckPointId for the number of the next checkpoint the pod must go through.

#### Output for one game turn

Two lines: 2 integers for the target coordinates of your pod followed by thrust , the acceleration to give your pod, or by SHIELD to activate the shields, or by BOOST for an acceleration burst. One line per pod.

x y thrust 뒤에 디버그 메시지를 넣을 수 있다. (ex. x y 100 racer thrust: 100)
ex. x y sheid racer shield
ex. x y boost racer boost
ex. x y shield blocker shield

### Constraints

0 ≤ thrust ≤ 100
2 ≤ checkpointCount ≤ 8
Response time first turn ≤ 1000ms
Response time per turn ≤ 75ms

## Log

``` text
Initialization:
GoodGame001 rank: 1
DarthBoss rank: 1
Standard Error Stream:
Race initialized: 3 laps, 6 checkpoints
Targeting opponent 0: CP 1, Progress: 1.747382623197651
Standard Output Stream:
7794 830 50
6357 2252 100
Game information:
Pod 1 of player GoodGame001 moves towards (7794, 830) at power 50
Pod 2 of player GoodGame001 moves towards (6357, 2252) at power 100
001
270
Standard Output Stream:
7794 830 100
7794 830 100
Game information:
Pod 1 of player DarthBoss moves towards (7794, 830) at power 100
Pod 2 of player DarthBoss moves towards (7794, 830) at power 100
Game Summary:
GoodGame001 rank: 1
DarthBoss rank: 2
Standard Error Stream:
Targeting opponent 0: CP 1, Progress: 1.75361414756271
Standard Output Stream:
7794 830 100
6423 2187 100
Game information:
Pod 1 of player GoodGame001 moves towards (7794, 830) at power 100
Pod 2 of player GoodGame001 moves towards (6423, 2187) at power 100
002
270
Standard Output Stream:
7554 1066 100
7786 1166 100
Game information:
Pod 1 of player DarthBoss moves towards (7554, 1066) at power 100
Pod 2 of player DarthBoss moves towards (7786, 1166) at power 100
Game Summary:
GoodGame001 rank: 1
DarthBoss rank: 2
Standard Error Stream:
Targeting opponent 0: CP 1, Progress: 1.7651049353301564
Standard Output Stream:
7794 830 100
6514 2098 100
Game information:
Pod 1 of player GoodGame001 moves towards (7794, 830) at power 100
Pod 2 of player GoodGame001 moves towards (6514, 2098) at power 100
003
270
Standard Output Stream:
7350 1266 100
7778 1454 100
Game information:
Pod 1 of player DarthBoss moves towards (7350, 1266) at power 100
Pod 2 of player DarthBoss moves towards (7778, 1454) at power 100
Game Summary:
GoodGame001 rank: 1
DarthBoss rank: 2
Standard Error Stream:
Targeting opponent 0: CP 1, Progress: 1.779111550706584
Standard Output Stream:
7794 830 100
6482 1904 100
Game information:
Pod 1 of player GoodGame001 moves towards (7794, 830) at power 100
Pod 2 of player GoodGame001 moves towards (6482, 1904) at power 100
004
270
Standard Output Stream:
7942 1878 100
7770 1698 100
Game information:
Pod 1 of player DarthBoss moves towards (7942, 1878) at power 100
Pod 2 of player DarthBoss moves towards (7770, 1698) at power 100
Game Summary:
GoodGame001 rank: 1
DarthBoss rank: 2
Standard Error Stream:
Targeting opponent 0: CP 1, Progress: 1.794228205893033
Standard Output Stream:
7794 830 100
6549 1751 SHIELD
Game information:
Pod 1 of player GoodGame001 moves towards (7794, 830) at power 100
Pod 2 of player GoodGame001 enabled its shield
005
270
Standard Output Stream:
7618 1870 100
7190 1694 100
Game information:
Pod 1 of player DarthBoss moves towards (7618, 1870) at power 100
Pod 2 of player DarthBoss moves towards (7190, 1694) at power 100
Game Summary:
GoodGame001 rank: 1
DarthBoss rank: 2
Standard Error Stream:
Targeting opponent 0: CP 1, Progress: 1.8119320269157984
Standard Output Stream:
7794 830 SHIELD
6535 1499 100
Game information:
Pod 1 of player GoodGame001 enabled its shield
Pod 2 of player GoodGame001 waits for its engine to reload
006
270
Standard Output Stream:
8138 2570 100
7358 1894 100
Game information:
Pod 1 of player DarthBoss moves towards (8138, 2570) at power 100
Pod 2 of player DarthBoss moves towards (7358, 1894) at power 100
Game Summary:
GoodGame001 rank: 2
DarthBoss rank: 1
Standard Error Stream:
Targeting opponent 0: CP 1, Progress: 1.8275879186911486
Standard Output Stream:
7794 830 100
6590 1304 100
Game information:
Pod 1 of player GoodGame001 waits for its engine to reload
Pod 2 of player GoodGame001 waits for its engine to reload
007
270
Standard Output Stream:
7750 2342 100
7514 2062 100
Game information:
Pod 1 of player DarthBoss moves towards (7750, 2342) at power 100
Pod 2 of player DarthBoss moves towards (7514, 2062) at power 100
Game Summary:
GoodGame001 rank: 2
DarthBoss rank: 1
Standard Error Stream:
Targeting opponent 0: CP 1, Progress: 1.8438650423511762
Standard Output Stream:
7794 830 100
6465 1216 100
Game information:
Pod 1 of player GoodGame001 waits for its engine to reload
Pod 2 of player GoodGame001 waits for its engine to reload
008
270
Standard Output Stream:
7418 2086 90
7662 2198 100
Game information:
Pod 1 of player DarthBoss moves towards (7418, 2086) at power 90
Pod 2 of player DarthBoss moves towards (7662, 2198) at power 100
Game Summary:
GoodGame001 rank: 2
DarthBoss rank: 1
Standard Error Stream:
Targeting opponent 0: CP 1, Progress: 1.8610030210535136
Standard Output Stream:
7794 830 100
6612 1071 100
Game information:
Pod 1 of player GoodGame001 waits for its engine to reload
Pod 2 of player GoodGame001 moves towards (6612, 1071) at power 100
009
270
Standard Output Stream:
7174 1850 91
7664 7439 0
Game information:
Pod 1 of player DarthBoss moves towards (7174, 1850) at power 91
Pod 2 of player DarthBoss moves towards (7664, 7439) at power 0
Game Summary:
GoodGame001 rank: 2
DarthBoss rank: 1
Standard Error Stream:
Targeting opponent 0: CP 1, Progress: 1.8794542358686959
Standard Output Stream:
7794 830 100
6788 965 100
Game information:
Pod 1 of player GoodGame001 moves towards (7794, 830) at power 100
Pod 2 of player GoodGame001 moves towards (6788, 965) at power 100
010
270
Standard Output Stream:
6970 1622 90
7660 7215 0
Game information:
Pod 1 of player DarthBoss moves towards (6970, 1622) at power 90
Pod 2 of player DarthBoss moves towards (7660, 7215) at power 0
Game Summary:
GoodGame001 rank: 2
DarthBoss rank: 1
Standard Error Stream:
Targeting opponent 0: CP 1, Progress: 1.899303267027922
Standard Output Stream:
7794 830 100
6987 897 100
Game information:
Pod 1 of player GoodGame001 moves towards (7794, 830) at power 100
Pod 2 of player GoodGame001 moves towards (6987, 897) at power 100
011
270
Standard Output Stream:
6806 1402 85
7656 7027 100
Game information:
Pod 1 of player DarthBoss moves towards (6806, 1402) at power 85
Pod 2 of player DarthBoss moves towards (7656, 7027) at power 100
Game Summary:
GoodGame001 rank: 2
DarthBoss rank: 1
Standard Error Stream:
Targeting opponent 0: CP 1, Progress: 1.9201279832325489
Standard Output Stream:
7794 830 100
7196 869 100
Game information:
Pod 1 of player GoodGame001 moves towards (7794, 830) at power 100
Pod 2 of player GoodGame001 moves towards (7196, 869) at power 100
012
270
Standard Output Stream:
6560 6311 100
7984 6947 100
Game information:
Pod 1 of player DarthBoss moves towards (6560, 6311) at power 100
Pod 2 of player DarthBoss moves towards (7984, 6947) at power 100
Game Summary:
GoodGame001 rank: 2
DarthBoss rank: 1
Standard Error Stream:
Targeting opponent 1: CP 2, Progress: 2.7056361135325018
Standard Output Stream:
7794 830 100
7727 3449 100
Game information:
Pod 1 of player GoodGame001 moves towards (7794, 830) at power 100
Pod 2 of player GoodGame001 moves towards (7727, 3449) at power 100
013
270
Standard Output Stream:
7130 2434 53
7408 7235 100
Game information:
Pod 1 of player DarthBoss moves towards (7130, 2434) at power 53
Pod 2 of player DarthBoss moves towards (7408, 7235) at power 100
Game Summary:
GoodGame001 rank: 2
DarthBoss rank: 1
Standard Error Stream:
Targeting opponent 1: CP 2, Progress: 2.6881053613397627
Standard Output Stream:
7794 830 100
7667 3349 100
Game information:
Pod 1 of player GoodGame001 moves towards (7794, 830) at power 100
Pod 2 of player GoodGame001 moves towards (7667, 3349) at power 100
014
270
Standard Output Stream:
7154 2030 62
7760 6915 100
Game information:
Pod 1 of player DarthBoss moves towards (7154, 2030) at power 62
Pod 2 of player DarthBoss moves towards (7760, 6915) at power 100
Game Summary:
GoodGame001 rank: 2
DarthBoss rank: 1
Standard Error Stream:
Targeting opponent 1: CP 2, Progress: 2.6820545386292642
Standard Output Stream:
7794 830 100
7687 3403 100
Game information:
Pod 1 of player GoodGame001 moves towards (7794, 830) at power 100
Pod 2 of player GoodGame001 moves towards (7687, 3403) at power 100
015
270
Standard Output Stream:
7714 2098 64
7516 6095 100
Game information:
Pod 1 of player DarthBoss moves towards (7714, 2098) at power 64
Pod 2 of player DarthBoss moves towards (7516, 6095) at power 100
Game Summary:
GoodGame001 rank: 2
DarthBoss rank: 1
Standard Error Stream:
Targeting opponent 1: CP 2, Progress: 2.6732887719062903
Standard Output Stream:
7794 830 100
7701 3268 100
Game information:
Pod 1 of player GoodGame001 moves towards (7794, 830) at power 100
Pod 2 of player GoodGame001 moves towards (7701, 3268) at power 100
016
270
Standard Output Stream:
7662 1698 100
7516 6615 100
Game information:
Pod 1 of player DarthBoss moves towards (7662, 1698) at power 100
Pod 2 of player DarthBoss moves towards (7516, 6615) at power 100
Game Summary:
GoodGame001 rank: 2
DarthBoss rank: 1
Standard Error Stream:
Targeting opponent 1: CP 2, Progress: 2.6691560389511393
Standard Output Stream:
7747 2541 50
7693 3288 100
Game information:
Pod 1 of player GoodGame001 moves towards (7747, 2541) at power 50
Pod 2 of player GoodGame001 moves towards (7693, 3288) at power 100
017
270
Standard Output Stream:
7590 1238 100
7620 6187 BOOST
Game information:
Pod 1 of player DarthBoss moves towards (7590, 1238) at power 100
Pod 2 of player DarthBoss boosts towards (7620, 6187)
Game Summary:
GoodGame001 rank: 2
DarthBoss rank: 1
```

CodinGame Mad Pod Racing 골드 리그 참고 GitHub 레포지토리
Mad Pod Racing(또는 Coders Strike Back)은 CodinGame의 인기 있는 멀티플레이어 레이싱 게임입니다. 골드 리그에서 경쟁하기 위해 참고할 수 있는 좋은 GitHub 레포지토리들을 소개해 드립니다:

1. reCurse's Coders Strike Back
언어: C++
특징: 레전드 리그까지 도달한 최적화된 구현체
주요 기술: 유전 알고리즘, 몬테카를로 시뮬레이션, 물리 엔진
세부적인 물리 시뮬레이션과 수학적 최적화가 인상적인 구현
2. MSmits' Mad Pod Racing
언어: C#
특징: 다양한 CodinGame 챌린지 솔루션 모음 (Mad Pod Racing 포함)
주요 기술: 빔 서치(Beam Search), 발전된 시뮬레이션
게임 트리 탐색과 휴리스틱 평가 함수를 잘 설명
3. Magus' CSB
언어: JavaScript
특징: 골드 리그에 최적화된 레이싱 AI
주요 기술: 유전 알고리즘, 지역 탐색, 충돌 회피
JavaScript로 구현된 고성능 레이싱 AI의 좋은 예시
4. pb4608's Coders Strike Back
언어: Python
특징: 알고리즘 단계별 구현 과정 설명
주요 기술: 유전 알고리즘, 시뮬레이션 기반 주행
초보자도 이해하기 쉽게 단계별로 발전시킨 알고리즘
5. inoryy's CSB AI
언어: Python
특징: 강화학습 기반 접근법
주요 기술: 심층 강화학습(DRL), 정책 그래디언트(Policy Gradient)
머신러닝 접근 방식으로 문제를 해결하는 독특한 방법 제시
6. Jakobovski's CSB Bot
언어: Java
특징: 레전드 리그 달성 솔루션
주요 기술: 몬테카를로 트리 탐색(MCTS), 물리 시뮬레이션
자세한 주석과 알고리즘 설명이 포함된 코드
7. Nmahoude's CSB Solution
언어: Java
특징: 골드/레전드 리그용 최적화 코드
주요 기술: 유전 알고리즘, MCTS, 효율적인 시뮬레이션
로컬 테스트와 벤치마킹을 위한 도구도 포함
추가 자료:
CSB Physics Explained - 게임 물리 엔진 상세 분석
CSB Postmortem - 레전드 리그 진출자의 접근 방식 해설 블로그
CSB Arena - 로컬에서 AI를 테스트할 수 있는 도구
이 레포지토리들을 통해 다양한 언어와 알고리즘 접근 방식을 확인하실 수 있으며, 골드 리그 이상의 성능을 달성하는 데 필요한 기술적 인사이트를 얻을 수 있습니다.