import java.util.*

// 미로의 최대 크기 및 기타 상수 정의
const val MAX_SIZE = 25
const val MAX_SWITCHES = 15
const val MAX_QUEUE_SIZE = 1000000  // 큐 크기 증가
const val DIRECTIONS = 4
const val MAX_PATH_LENGTH = 10000

// 미로 요소 정의
const val EMPTY = '.'
const val WALL = '#'
const val GARBAGE = '+'

// 스위치 정보 클래스
class Switch(
    var x: Int = 0,
    var y: Int = 0,
    var blockX: Int = 0, 
    var blockY: Int = 0,
    var isOn: Boolean = false
)

// BFS 큐에 사용할 상태 정의
class QueueState(
    var x: Int = 0,
    var y: Int = 0,
    var switchBitmask: Int = 0,
    var parent: Int = 0,
    var move: Char = '\u0000'
)

// 전역 변수들
val dx = intArrayOf(0, 0, -1, 1)  // 상, 하, 좌, 우
val dy = intArrayOf(-1, 1, 0, 0)
val dirChars = charArrayOf('U', 'D', 'L', 'R')

val maze = Array(MAX_SIZE) { CharArray(MAX_SIZE) }
val switches = Array(MAX_SWITCHES) { Switch() }
var width = 0
var height = 0
var switchCount = 0
var startX = 0
var startY = 0
var targetX = 0
var targetY = 0
val queue = Array(MAX_QUEUE_SIZE) { QueueState() }
val visited = Array(MAX_SIZE) { Array(MAX_SIZE) { BooleanArray(1 shl MAX_SWITCHES) } }
val path = CharArray(MAX_PATH_LENGTH)

// 맨해튼 거리 계산
fun manhattanDistance(x1: Int, y1: Int, x2: Int, y2: Int): Int {
    return Math.abs(x1 - x2) + Math.abs(y1 - y2)
}

// 스위치 상태 업데이트 - 비트마스크로 작업
fun updateSwitchState(bitmask: Int, switchIndex: Int): Int {
    return bitmask xor (1 shl switchIndex)
}

// 자기장 필드 확인
fun isMagneticField(x: Int, y: Int, switchBitmask: Int): Boolean {
    for (i in 0 until switchCount) {
        if ((switchBitmask and (1 shl i) != 0) && switches[i].blockX == x && switches[i].blockY == y) {
            return true
        }
    }
    return false
}

// 유효한 이동인지 확인
fun isValidMove(x: Int, y: Int, switchBitmask: Int): Boolean {
    // 미로 경계 확인
    if (x < 0 || x >= width || y < 0 || y >= height) {
        return false
    }
    
    // 벽 확인
    if (maze[y][x] == WALL) {
        return false
    }
    
    // 쓰레기 볼 확인
    if (maze[y][x] == GARBAGE) {
        return false
    }
    
    // 자기장 필드 확인
    if (isMagneticField(x, y, switchBitmask)) {
        return false
    }
    
    return true
}

// 경로 역추적
fun reconstructPath(targetIndex: Int): String {
    var pathLen = 0
    var currentIndex = targetIndex
    
    // 경로를 역으로 추적
    while (currentIndex > 0) {  // 0은 시작 상태
        path[pathLen++] = queue[currentIndex].move
        currentIndex = queue[currentIndex].parent
    }
    
    // 경로 뒤집기
    for (i in 0 until pathLen / 2) {
        val temp = path[i]
        path[i] = path[pathLen - i - 1]
        path[pathLen - i - 1] = temp
    }
    
    return String(path, 0, pathLen)
}

// A* 알고리즘으로 최단 경로 찾기
fun findPath(): String? {
    var front = 0
    var rear = 0
    
    // 초기 상태 설정
    val initialState = queue[0]
    initialState.x = startX
    initialState.y = startY
    initialState.parent = -1  // 시작점은 부모가 없음
    initialState.move = '\u0000'  // 시작점 이동 명령 없음
    
    // 초기 스위치 상태 설정
    initialState.switchBitmask = 0
    for (i in 0 until switchCount) {
        if (switches[i].isOn) {
            initialState.switchBitmask = initialState.switchBitmask or (1 shl i)
        }
    }
    
    rear++
    
    // 방문 배열 초기화
    for (y in 0 until MAX_SIZE) {
        for (x in 0 until MAX_SIZE) {
            for (s in 0 until (1 shl MAX_SWITCHES)) {
                visited[y][x][s] = false
            }
        }
    }
    
    visited[startY][startX][initialState.switchBitmask] = true
    
    while (front < rear) {
        // 현재 상태는 큐의 맨 앞
        val current = queue[front++]
        
        // 목표 도달 확인
        if (current.x == targetX && current.y == targetY) {
            return reconstructPath(front - 1)  // 현재 상태의 인덱스
        }
        
        // 4방향 탐색
        for (dir in 0 until DIRECTIONS) {
            val nx = current.x + dx[dir]
            val ny = current.y + dy[dir]
            
            // 유효한 이동인지 확인
            if (!isValidMove(nx, ny, current.switchBitmask)) {
                continue
            }
            
            // 새 상태 생성
            val newState = queue[rear]
            newState.x = nx
            newState.y = ny
            newState.parent = front - 1  // 현재 상태가 새 상태의 부모
            newState.move = dirChars[dir]  // 이동 방향 저장
            
            // 스위치 토글 확인
            var updatedBitmask = current.switchBitmask
            for (i in 0 until switchCount) {
                if (nx == switches[i].x && ny == switches[i].y) {
                    updatedBitmask = updateSwitchState(updatedBitmask, i)
                }
            }
            newState.switchBitmask = updatedBitmask
            
            // 방문 체크
            if (!visited[ny][nx][updatedBitmask]) {
                visited[ny][nx][updatedBitmask] = true
                rear++
                
                if (rear >= MAX_QUEUE_SIZE) {
                    System.err.println("큐 오버플로우 발생! 큐 크기 증가 필요")
                    return null
                }
            }
        }
    }
    
    return null // 경로를 찾지 못함
}

fun main(args: Array<String>) {
    val scanner = Scanner(System.`in`)
    
    // 미로 크기 입력
    width = scanner.nextInt()
    height = scanner.nextInt()
    scanner.nextLine() // 개행 문자 처리
    
    // 미로 입력
    for (i in 0 until height) {
        val line = scanner.nextLine()
        for (j in 0 until width) {
            maze[i][j] = line[j]
        }
        System.err.println("Line $i: ${line}")
    }
    
    // 시작 위치와 목표 위치 입력
    startX = scanner.nextInt()
    startY = scanner.nextInt()
    targetX = scanner.nextInt()
    targetY = scanner.nextInt()
    System.err.println("Start: ($startX, $startY), Target: ($targetX, $targetY)")
    
    // 스위치 정보 입력
    switchCount = scanner.nextInt()
    for (i in 0 until switchCount) {
        val switchX = scanner.nextInt()
        val switchY = scanner.nextInt()
        val blockX = scanner.nextInt()
        val blockY = scanner.nextInt()
        val initialState = scanner.nextInt()
        
        switches[i].x = switchX
        switches[i].y = switchY
        switches[i].blockX = blockX
        switches[i].blockY = blockY
        switches[i].isOn = (initialState == 1)
        
        System.err.println("Switch $i: ($switchX, $switchY) controls ($blockX, $blockY), initial state: $initialState")
    }
    
    // 경로 찾기
    val foundPath = findPath()
    
    if (foundPath == null) {
        System.err.println("경로를 찾을 수 없습니다.")
        println("경로를 찾을 수 없습니다.")
        return
    }
    
    // 결과 출력
    System.err.println("경로 길이: ${foundPath.length}")
    System.err.println("경로: $foundPath")
    
    println(foundPath)
}
