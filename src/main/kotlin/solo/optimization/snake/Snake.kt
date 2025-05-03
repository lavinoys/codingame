import java.util.*
import kotlin.math.*

data class Point(val x: Int, val y: Int) {
    fun distance(other: Point): Int = abs(x - other.x) + abs(y - other.y)

    fun neighbors(): List<Point> = listOf(
        Point(x, y-1),  // 위
        Point(x+1, y),  // 오른쪽
        Point(x, y+1),  // 아래
        Point(x-1, y)   // 왼쪽
    )

    override fun equals(other: Any?): Boolean {
        if (other !is Point) return false
        return x == other.x && y == other.y
    }

    override fun hashCode(): Int = x * 1000 + y
}

class SnakeGame {
    private val rabbits = mutableListOf<Point>()
    private val snake = mutableListOf<Point>()
    private val mapWidth = 96
    private val mapHeight = 54

    fun addRabbit(x: Int, y: Int) {
        rabbits.add(Point(x, y))
    }

    fun updateSnake(snakeParts: List<Point>) {
        snake.clear()
        snake.addAll(snakeParts)
    }

    private fun isCollision(point: Point): Boolean {
        // 맵 경계 체크
        if (point.x < 0 || point.x >= mapWidth || point.y < 0 || point.y >= mapHeight) {
            return true
        }

        // 뱀 몸체와 충돌 체크 (머리 제외)
        for (i in 1 until snake.size) {
            if (snake[i].x == point.x && snake[i].y == point.y) {
                return true
            }
        }

        return false
    }

    fun getNextMove(): Point {
        val head = snake[0]

        // 토끼가 없으면 안전한 이동만 고려
        if (rabbits.isEmpty()) {
            return findSafeMove() ?: Point(head.x + 1, head.y)
        }

        // 토끼를 향한 A* 경로 찾기
        val path = findPathToNearestRabbit()
        return path ?: findSafeMove() ?: Point(head.x + 1, head.y)
    }

    private fun findSafeMove(): Point? {
        val head = snake[0]
        return head.neighbors().firstOrNull { !isCollision(it) }
    }

    private fun findPathToNearestRabbit(): Point? {
        // 가장 가까운 토끼 찾기
        val head = snake[0]
        val targetRabbit = rabbits.minByOrNull { it.distance(head) } ?: return null

        // A* 알고리즘으로 경로 찾기
        val openSet = PriorityQueue<Pair<Point, Int>> { a, b -> a.second - b.second }
        val closedSet = HashSet<Point>()
        val cameFrom = HashMap<Point, Point>()
        val gScore = HashMap<Point, Int>().withDefault { Int.MAX_VALUE }
        val fScore = HashMap<Point, Int>().withDefault { Int.MAX_VALUE }

        openSet.add(Pair(head, 0))
        gScore[head] = 0
        fScore[head] = head.distance(targetRabbit)

        while (openSet.isNotEmpty()) {
            val current = openSet.poll().first

            if (current.x == targetRabbit.x && current.y == targetRabbit.y) {
                // 토끼에 도달했으면 첫 이동 위치 반환
                var path = current
                while (cameFrom[path] != head) {
                    path = cameFrom[path]!!
                }
                return path
            }

            closedSet.add(current)

            for (neighbor in current.neighbors()) {
                if (isCollision(neighbor) || neighbor in closedSet) continue

                val tentativeGScore = gScore.getValue(current) + 1

                if (tentativeGScore < gScore.getValue(neighbor)) {
                    cameFrom[neighbor] = current
                    gScore[neighbor] = tentativeGScore
                    fScore[neighbor] = tentativeGScore + neighbor.distance(targetRabbit)

                    if (neighbor !in openSet.map { it.first }) {
                        openSet.add(Pair(neighbor, fScore.getValue(neighbor)))
                    }
                }
            }
        }

        // 경로를 찾지 못했을 때
        return null
    }

    // 토끼를 잡았는지 확인하고 제거
    fun checkAndRemoveRabbit() {
        val head = snake[0]
        val caughtRabbit = rabbits.find { it.x == head.x && it.y == head.y }
        caughtRabbit?.let { rabbits.remove(it) }
    }
}

fun main(args: Array<String>) {
    val input = Scanner(System.`in`)
    val game = SnakeGame()

    // 첫 번째 입력: 토끼 수와 위치
    val n = input.nextInt()
    for (i in 0 until n) {
        val x = input.nextInt()
        val y = input.nextInt()
        game.addRabbit(x, y)
    }

    // 게임 루프
    while (true) {
        val nSnake = input.nextInt()
        val snakeParts = mutableListOf<Point>()

        for (i in 0 until nSnake) {
            val x = input.nextInt()
            val y = input.nextInt()
            snakeParts.add(Point(x, y))
        }

        game.updateSnake(snakeParts)
        game.checkAndRemoveRabbit()

        val nextMove = game.getNextMove()
        println("${nextMove.x} ${nextMove.y}")
    }
}