import java.util.*
import kotlin.math.pow
import kotlin.math.sqrt
import kotlin.math.exp  // exp 함수 import 추가
import kotlin.random.Random

data class Point(val index: Int, val x: Int, val y: Int)

/**
 * Auto-generated code below aims at helping you parse
 * the standard input according to the problem statement.
 **/
fun main(args : Array<String>) {
    val input = Scanner(System.`in`)
    val N = input.nextInt() // This variables stores how many nodes are given
    val points = mutableListOf<Point>()

    for (i in 0 until N) {
        val X = input.nextInt() // The x coordinate of the given node
        val Y = input.nextInt() // The y coordinate of the given node
        points.add(Point(i, X, Y))
    }

    // 실행 시간 제한 설정 (5초보다 약간 적게)
    val timeLimit = System.currentTimeMillis() + 4800L

    // 먼저 Nearest Neighbor 알고리즘으로 초기 경로 생성
    var path = nearestNeighborPath(points)

    // 2-opt 최적화 적용
    path = twoOpt(path, timeLimit)
    
    // 시간이 남으면 simulated annealing 적용
    path = simulatedAnnealing(path, timeLimit)

    // 경로 출력 형식 맞추기 (마지막에 시작점 추가)
    val result = buildString {
        path.forEach { append("${it.index} ") }
        append("0") // 마지막에 출발점으로 돌아옴
    }

    println(result) // 찾은 경로 출력
}

// 두 점 사이의 거리 계산
fun distance(p1: Point, p2: Point): Double {
    return sqrt((p1.x - p2.x).toDouble().pow(2) + (p1.y - p2.y).toDouble().pow(2))
}

// 전체 경로의 길이 계산
fun pathLength(path: List<Point>): Double {
    var length = 0.0
    for (i in 0 until path.size - 1) {
        length += distance(path[i], path[i + 1])
    }
    // 마지막 점에서 시작점으로 돌아오는 거리 추가
    length += distance(path.last(), path.first())
    return length
}

// Nearest Neighbor 알고리즘
fun nearestNeighborPath(points: List<Point>): List<Point> {
    val n = points.size
    val visited = BooleanArray(n) { false }
    val path = mutableListOf<Point>()

    // 시작점은 항상 인덱스 0
    var current = points[0]
    path.add(current)
    visited[0] = true

    // 나머지 n-1개 점을 방문
    for (i in 1 until n) {
        var nearestPoint: Point? = null
        var minDistance = Double.MAX_VALUE

        for (point in points) {
            if (!visited[point.index]) {
                val dist = distance(current, point)
                if (dist < minDistance) {
                    minDistance = dist
                    nearestPoint = point
                }
            }
        }

        nearestPoint?.let {
            path.add(it)
            visited[it.index] = true
            current = it
        }
    }

    return path
}

// 2-opt 최적화 알고리즘
fun twoOpt(path: List<Point>, timeLimit: Long): List<Point> {
    val n = path.size
    var best = path.toMutableList()
    var improved = true

    while (improved && System.currentTimeMillis() < timeLimit) {
        improved = false
        var bestDistance = pathLength(best)

        for (i in 0 until n - 2) {
            for (j in i + 2 until n) {
                if (System.currentTimeMillis() >= timeLimit) return best

                // 두 구간 교환
                val newPath = best.toMutableList()
                reverse(newPath, i + 1, j)

                val newDistance = pathLength(newPath)
                if (newDistance < bestDistance) {
                    best = newPath
                    bestDistance = newDistance
                    improved = true
                    break // 개선되면 즉시 적용
                }
            }
            if (improved) break
        }
    }

    return best
}

// 리스트의 구간을 뒤집는 함수
fun reverse(list: MutableList<Point>, from: Int, to: Int) {
    var start = from
    var end = to
    while (start < end) {
        val temp = list[start]
        list[start] = list[end]
        list[end] = temp
        start++
        end--
    }
}

// Simulated Annealing 알고리즘
fun simulatedAnnealing(initialPath: List<Point>, timeLimit: Long): List<Point> {
    var currentPath = initialPath.toMutableList()
    var bestPath = initialPath.toMutableList()
    var currentEnergy = pathLength(currentPath)
    var bestEnergy = currentEnergy

    val startTemp = 1000.0
    var temperature = startTemp  // 변수명 temp에서 temperature로 변경
    val coolingRate = 0.99
    val rand = Random(System.currentTimeMillis())

    while (System.currentTimeMillis() < timeLimit && temperature > 1.0) {
        // 랜덤하게 두 점을 선택하여 교환
        val i = rand.nextInt(1, currentPath.size) // 0은 시작점이므로 바꾸지 않음
        val j = rand.nextInt(1, currentPath.size)

        if (i != j) {
            val newPath = currentPath.toMutableList()
            val tempPoint = newPath[i]  // temp에서 tempPoint로 변수명 변경
            newPath[i] = newPath[j]
            newPath[j] = tempPoint

            val newEnergy = pathLength(newPath)

            // 더 좋은 해를 발견하면 항상 받아들임
            // 더 나쁜 해라도 확률적으로 받아들임
            val acceptanceProbability = exp((currentEnergy - newEnergy) / temperature)

            if (newEnergy < currentEnergy || rand.nextDouble() < acceptanceProbability) {
                currentPath = newPath
                currentEnergy = newEnergy

                if (newEnergy < bestEnergy) {
                    bestPath = newPath
                    bestEnergy = newEnergy
                }
            }
        }

        temperature *= coolingRate  // temp에서 temperature로 변수명 변경
    }

    return bestPath
}
