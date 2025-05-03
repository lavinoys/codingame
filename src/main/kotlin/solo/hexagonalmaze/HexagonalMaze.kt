package solo.hexagonalmaze

import java.util.ArrayDeque

/**
 * 육각형 미로에서 최단 경로를 찾는 솔루션
 * 각 셀은 6개의 이웃을 가지며, 그리드는 주기적(좌우, 상하 연결)입니다.
 */

// 이전 셀에 대한 참조를 포함하는 연결 셀 구조
data class LinkedCell(val x: Int, val y: Int, val prev: LinkedCell? = null)

// 육각형 그리드의 방향 정의
data class Direction(val dx: Int, val dy: Int, val diagonalShift: Boolean = false)

fun main() {
    // 미로 크기 입력 받기
    val (w, h) = readLine()!!.split(" ").map { it.toInt() }
    
    // 미로 그리드 읽기
    val grid = Array(h) { readLine()!!.toCharArray() }
    
    // 시작 위치 찾기
    val start = findStartPosition(grid, h)
    
    // BFS로 경로 찾기
    val end = findPathWithBFS(grid, w, h, start.x, start.y)
    
    // 경로 표시 및 결과 출력
    markPathAndPrint(grid, end, h)
}

/**
 * 미로에서 시작 위치('S')를 찾는 함수
 */
private fun findStartPosition(grid: Array<CharArray>, h: Int): LinkedCell {
    for (y in 0 until h) {
        val x = grid[y].indexOf('S')
        if (x != -1) {
            return LinkedCell(x, y)
        }
    }
    throw IllegalStateException("시작점을 찾을 수 없습니다")
}

/**
 * BFS 알고리즘을 사용하여 시작점에서 끝점까지의 경로를 찾는 함수
 */
private fun findPathWithBFS(grid: Array<CharArray>, w: Int, h: Int, startX: Int, startY: Int): LinkedCell {
    // 육각형 그리드의 여섯 방향 정의
    val directions = getHexagonalDirections()
    
    // BFS를 위한 큐
    val queue = ArrayDeque<LinkedCell>()
    queue.add(LinkedCell(startX, startY))
    
    // BFS 탐색
    while (queue.isNotEmpty()) {
        val current = queue.removeFirst()
        val x = current.x
        val y = current.y
        
        // 현재 셀의 모든 이웃을 확인
        for (dir in directions) {
            // 이웃 셀의 좌표 계산
            val nx = calculateNeighborX(x, y, w, dir)
            val ny = (y + dir.dy + h) % h
            
            // 끝점에 도달했는지 확인
            if (grid[ny][nx] == 'E') {
                return current
            }
            
            // 빈 셀인 경우 큐에 추가하고 방문 표시
            if (grid[ny][nx] == '_') {
                queue.add(LinkedCell(nx, ny, current))
                grid[ny][nx] = ' ' // 방문 표시
            }
        }
    }
    
    throw IllegalStateException("경로를 찾을 수 없습니다")
}

/**
 * 육각형 그리드에서 6개 방향을 정의하는 함수
 */
private fun getHexagonalDirections(): List<Direction> {
    return listOf(
        Direction(1, 0), // 오른쪽
        Direction(-1, 0), // 왼쪽
        Direction(0, 1), // 아래
        Direction(0, -1), // 위
        Direction(0, 1, true), // 대각선 아래
        Direction(0, -1, true) // 대각선 위
    )
}

/**
 * 주어진 방향에 따라 이웃 셀의 X 좌표를 계산하는 함수
 */
private fun calculateNeighborX(x: Int, y: Int, w: Int, dir: Direction): Int {
    // 기본 x 좌표 계산
    val nx = (x + dir.dx + w) % w
    
    // 대각선 이동의 경우 y의 홀짝에 따라 x 좌표 조정
    return if (dir.diagonalShift) {
        val shift = if (y % 2 == 1) 1 else -1
        (nx + shift + w) % w
    } else {
        nx
    }
}

/**
 * 찾은 경로를 표시하고 결과를 출력하는 함수
 */
private fun markPathAndPrint(grid: Array<CharArray>, end: LinkedCell, h: Int) {
    // 경로 표시
    var current: LinkedCell? = end
    while (current?.prev != null) {
        grid[current.y][current.x] = '.'
        current = current.prev
    }
    
    // 결과 출력
    for (y in 0 until h) {
        for (x in 0 until grid[y].size) {
            if (grid[y][x] == ' ') {
                grid[y][x] = '_' // 방문했지만 경로가 아닌 셀 복원
            }
        }
        println(grid[y].joinToString(""))
    }
}
