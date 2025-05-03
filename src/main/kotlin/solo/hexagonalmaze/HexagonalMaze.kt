package solo.hexagonalmaze

/**
 * 육각형 미로에서 최단 경로를 찾는 솔루션
 * 각 셀은 6개의 이웃을 가지며, 그리드는 주기적(좌우, 상하 연결)입니다.
 */

// 이전 셀에 대한 참조를 포함하는 연결 셀 구조
data class LinkedCell(val x: Int, val y: Int, val prev: LinkedCell? = null)

fun main() {
    // 미로 크기 입력 받기
    val (w, h) = readLine()!!.split(" ").map { it.toInt() }
    
    // 미로 그리드 읽기
    val grid = Array(h) { readLine()!!.toCharArray() }
    
    // 시작 위치 'S' 찾기
    var startX = 0
    var startY = 0
    for (y in 0 until h) {
        val x = grid[y].indexOf('S')
        if (x != -1) {
            startX = x
            startY = y
            break
        }
    }
    
    // BFS 탐색을 위한 경로 배열
    val path = Array<LinkedCell?>(512) { null }
    var len = 0
    var it = 0
    var end: LinkedCell? = null
    
    // 시작 셀 설정
    path[len++] = LinkedCell(startX, startY)
    
    // 경로 찾기
    while (end == null) {
        val prev = path[it]!!
        val x = prev.x
        val y = prev.y
        
        // 육각형 그리드에서 이웃 셀 계산
        val x1 = (x + 1) % w
        val x_1 = (x - 1 + w) % w
        val y1 = (y + 1) % h
        val y_1 = (y - 1 + h) % h
        val x_d = if (y % 2 == 1) x1 else x_1
        
        // 끝점에 도달했는지 확인
        if ('E' in listOf(grid[y][x1], grid[y][x_1], grid[y1][x], 
                         grid[y_1][x], grid[y1][x_d], grid[y_1][x_d])) {
            end = prev
        } else {
            // 가능한 모든 방향 탐색
            if (grid[y][x1] == '_') {
                path[len++] = LinkedCell(x1, y, prev)
                grid[y][x1] = ' ' // 방문 표시
            }
            if (grid[y][x_1] == '_') {
                path[len++] = LinkedCell(x_1, y, prev)
                grid[y][x_1] = ' '
            }
            if (grid[y1][x] == '_') {
                path[len++] = LinkedCell(x, y1, prev)
                grid[y1][x] = ' '
            }
            if (grid[y_1][x] == '_') {
                path[len++] = LinkedCell(x, y_1, prev)
                grid[y_1][x] = ' '
            }
            if (grid[y1][x_d] == '_') {
                path[len++] = LinkedCell(x_d, y1, prev)
                grid[y1][x_d] = ' '
            }
            if (grid[y_1][x_d] == '_') {
                path[len++] = LinkedCell(x_d, y_1, prev)
                grid[y_1][x_d] = ' '
            }
        }
        it++
    }
    
    // 경로 표시
    var current = end
    while (current?.prev != null) {
        grid[current.y][current.x] = '.'
        current = current.prev
    }
    
    // 결과 출력
    for (y in 0 until h) {
        for (x in 0 until w) {
            if (grid[y][x] == ' ') {
                grid[y][x] = '_' // 방문했지만 경로가 아닌 셀 복원
            }
        }
        println(grid[y].joinToString(""))
    }
}
