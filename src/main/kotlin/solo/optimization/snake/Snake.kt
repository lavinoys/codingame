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
    private var turnsSinceLastRabbit = 0
    private var snakeBodySet = HashSet<Point>()  // 성능 향상을 위한 뱀 몸체 집합
    
    fun addRabbit(x: Int, y: Int) {
        rabbits.add(Point(x, y))
    }

    fun updateSnake(snakeParts: List<Point>) {
        snake.clear()
        snake.addAll(snakeParts)
        
        // 뱀 몸체 집합 업데이트
        snakeBodySet.clear()
        for (i in 1 until snake.size) { // 머리 제외
            snakeBodySet.add(snake[i])
        }
        
        turnsSinceLastRabbit++
    }

    private fun isCollision(point: Point): Boolean {
        // 맵 경계 체크
        if (point.x < 0 || point.x >= mapWidth || point.y < 0 || point.y >= mapHeight) {
            return true
        }

        // 뱀 몸체와 충돌 체크 - O(1) 시간 복잡도로 향상
        return point in snakeBodySet
    }

    fun getNextMove(): Point {
        val head = snake[0]

        // 토끼가 없으면 안전한 이동만 고려
        if (rabbits.isEmpty()) {
            return findSafeMove() ?: Point(head.x + 1, head.y)
        }

        // 최적의 토끼를 선택하고 경로 찾기
        val path = findPathToOptimalRabbit()
        return path ?: findSafeMove() ?: Point(head.x + 1, head.y)
    }

    private fun findSafeMove(): Point? {
        val head = snake[0]
        val moves = head.neighbors()
        
        // 안전한 이동 중에서 가장 많은 이동 옵션을 제공하는 이동 선택
        return moves
            .filter { !isCollision(it) }
            .maxByOrNull { neighbor -> 
                neighbor.neighbors().count { !isCollision(it) }
            }
    }
    
    private fun findPathToOptimalRabbit(): Point? {
        val head = snake[0]
        
        // 단순 거리가 아닌 더 복잡한 기준으로 토끼 선택
        val targetRabbit = selectOptimalRabbit() ?: return null

        // 향상된 A* 알고리즘
        val openSet = PriorityQueue<PathNode> { a, b -> a.fScore - b.fScore }
        val closedSet = HashSet<Point>()
        val gScore = HashMap<Point, Int>()
        val cameFrom = HashMap<Point, Point>()
        
        gScore[head] = 0
        openSet.add(PathNode(head, 0, head.distance(targetRabbit)))
        
        while (openSet.isNotEmpty()) {
            val current = openSet.poll().point
            
            if (current == targetRabbit) {
                // 경로 역추적
                var path = current
                while (cameFrom[path] != head) {
                    path = cameFrom[path]!!
                }
                return path
            }
            
            closedSet.add(current)
            
            for (neighbor in current.neighbors()) {
                if (neighbor in closedSet || isCollision(neighbor)) continue
                
                val tentativeGScore = gScore.getOrDefault(current, Int.MAX_VALUE) + 1
                
                if (tentativeGScore < gScore.getOrDefault(neighbor, Int.MAX_VALUE)) {
                    cameFrom[neighbor] = current
                    gScore[neighbor] = tentativeGScore
                    val fScore = tentativeGScore + neighbor.distance(targetRabbit)
                    
                    // 이미 openSet에 있는지 확인할 필요 없이 그냥 추가
                    // 우선순위 큐가 자동으로 최적의 노드를 앞으로 정렬함
                    openSet.add(PathNode(neighbor, tentativeGScore, fScore))
                }
            }
        }
        
        return null
    }
    
    private fun selectOptimalRabbit(): Point? {
        if (rabbits.isEmpty()) return null
        val head = snake[0]
        
        // 단순 거리만이 아닌 더 복잡한 기준으로 토끼 선택
        // 오래 잡지 못한 토끼일수록 우선순위 증가
        return rabbits.minByOrNull { rabbit ->
            // 기본 거리에 턴 수에 따른 가중치 추가
            val distance = rabbit.distance(head)
            val urgency = if (turnsSinceLastRabbit > 8) turnsSinceLastRabbit * 2 else 0
            
            // 위험지역 회피 (벽에 너무 가깝거나 뱀에 둘러싸인 토끼는 피함)
            val dangerFactor = calculateDangerFactor(rabbit)
            
            distance - urgency + dangerFactor
        }
    }
    
    private fun calculateDangerFactor(point: Point): Int {
        // 벽과의 거리 확인
        val wallDistance = min(
            min(point.x, mapWidth - 1 - point.x),
            min(point.y, mapHeight - 1 - point.y)
        )
        
        // 벽에 너무 가까우면 위험 요소 증가
        val wallDanger = if (wallDistance < 3) 5 else 0
        
        // 주변 뱀 몸체 부분 개수 확인
        val surroundingBodyParts = point.neighbors().count { it in snakeBodySet }
        val bodyDanger = surroundingBodyParts * 3
        
        return wallDanger + bodyDanger
    }

    // 토끼를 잡았는지 확인하고 제거
    fun checkAndRemoveRabbit() {
        val head = snake[0]
        val caughtRabbit = rabbits.find { it == head }
        if (caughtRabbit != null) {
            rabbits.remove(caughtRabbit)
            turnsSinceLastRabbit = 0
        }
    }
    
    // A* 알고리즘을 위한 경로 노드 클래스
    private data class PathNode(
        val point: Point,
        val gScore: Int,
        val fScore: Int
    )
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
