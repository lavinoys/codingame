# 코딩게임(CodinGame) 솔루션 모음

이 저장소는 [CodinGame](https://www.codingame.com/) 플랫폼의 다양한 프로그래밍 챌린지에 대한 솔루션을 모아놓은 프로젝트입니다.

## 프로젝트 개요

CodinGame은 게임 형식으로 프로그래밍 기술을 연습하고 향상시킬 수 있는 플랫폼입니다. 이 저장소에는 다양한 유형의 CodinGame 챌린지에 대한 Kotlin 솔루션이 포함되어 있습니다:

- **솔로(Solo) 퍼즐**: 알고리즘 문제 해결 능력을 테스트하는 단일 플레이어 챌린지
- **봇 프로그래밍(Bot Programming)**: AI 봇을 작성하여 다른 플레이어와 경쟁하는 멀티플레이어 게임
- **코드 골프(Code Golf)**: 최소한의 코드로 문제를 해결하는 챌린지
- **트레이닝(Training)**: 다양한 난이도의 연습 문제

## 프로젝트 구조

```
src/
├── main/
│   └── kotlin/
│       ├── bot/                  # 봇 프로그래밍 솔루션
│       │   └── madpodracing/     # Mad Pod Racing 게임 솔루션
│       │       └── gold/         # 골드 리그 솔루션
│       ├── solo/                 # 솔로 퍼즐 솔루션
│       │   ├── codegolf/         # 코드 골프 챌린지
│       │   │   ├── easy/         # 쉬운 난이도
│       │   │   └── medium/       # 중간 난이도
│       │   └── hexagonalmaze/    # 육각형 미로 챌린지
│       └── training/             # 트레이닝 문제 솔루션
│           ├── easy/             # 쉬운 난이도
│           ├── medium/           # 중간 난이도
│           └── hard/             # 어려운 난이도
└── test/
    └── kotlin/
        └── solo/                 # 솔로 챌린지 테스트
            └── hexagonalmaze/    # 육각형 미로 테스트
```

## 주요 솔루션 설명

### 1. 육각형 미로 (Hexagonal Maze)

육각형 그리드에서 최단 경로를 찾는 알고리즘 구현. BFS(너비 우선 탐색)를 사용하여 시작점에서 끝점까지의 경로를 찾습니다.

- 파일: `src/main/kotlin/solo/hexagonalmaze/HexagonalMaze.kt`
- 특징:
  - 육각형 그리드에서의 이웃 셀 계산
  - 주기적 그리드 처리 (좌우, 상하 연결)
  - 최단 경로 찾기 알고리즘

### 2. 매드 포드 레이싱 (Mad Pod Racing)

레이싱 게임에서 포드를 제어하는 AI 봇 구현.

- 기본 버전: `src/main/kotlin/bot/madpodracing/MadPodRacing.kt`
- 실버 리그: `src/main/kotlin/bot/madpodracing/MadPodRacingSilver.kt`
- 골드 리그: `src/main/kotlin/bot/madpodracing/gold/MadPodRacingGold.kt`
- 특징:
  - 체크포인트 탐색 알고리즘
  - 충돌 회피 로직
  - 속도 및 방향 최적화

## 빌드 및 실행 방법

이 프로젝트는 Gradle을 사용하여 빌드됩니다.

### 요구 사항

- JDK 8 이상
- Kotlin 2.1.10 이상

### 빌드 방법

```bash
# 프로젝트 빌드
./gradlew build

# 테스트 실행
./gradlew test
```

### 솔루션 실행 방법

각 솔루션은 독립적으로 실행할 수 있습니다. CodinGame 플랫폼에 제출하기 위해서는 해당 파일의 내용을 복사하여 CodinGame 에디터에 붙여넣으면 됩니다.

로컬에서 테스트하려면 다음과 같이 실행할 수 있습니다:

```bash
# 특정 솔루션 실행 (예: HexagonalMaze)
./gradlew run --args="solo.hexagonalmaze.HexagonalMazeKt"
```

## 코드 스타일 가이드라인

이 프로젝트는 다음과 같은 코딩 스타일 가이드라인을 따릅니다:

- 모든 주석은 반드시 한글로 작성합니다.
- 변수, 함수, 클래스 등에는 목적이 분명하게 드러나는 이름을 사용합니다.
- 함수는 하나의 일만 하도록 짧고 명확하게 작성합니다.
- 인자는 최소화하고, 부작용(side effect)을 피합니다.
- 동일하거나 유사한 코드는 함수나 클래스로 추출하여 재사용합니다(DRY 원칙).
- 들여쓰기, 공백, 네이밍 등 코드 스타일을 일관되게 유지합니다.

## 기여 방법

1. 이 저장소를 포크(Fork)합니다.
2. 새로운 브랜치를 생성합니다: `git checkout -b feature/새로운-솔루션`
3. 변경사항을 커밋합니다: `git commit -m '새로운 솔루션 추가: 문제 이름'`
4. 포크한 저장소에 푸시합니다: `git push origin feature/새로운-솔루션`
5. Pull Request를 제출합니다.

## 라이센스

이 프로젝트는 MIT 라이센스 하에 배포됩니다. 자세한 내용은 LICENSE 파일을 참조하세요.