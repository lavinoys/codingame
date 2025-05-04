import java.util.*
import kotlin.math.sqrt
import kotlin.math.exp
import kotlin.random.Random

data class Point(val index: Int, val x: Int, val y: Int)

/**
 * Auto-generated code below aims at helping you parse
 * the standard input according to the problem statement.
 **/
fun main() {
    val input = Scanner(System.`in`)
    val N = input.nextInt()
    val points = Array(N) { 
        val X = input.nextInt()
        val Y = input.nextInt()
        Point(it, X, Y)
    }

    // 실행 시간 제한 설정 (5초보다 약간 적게)
    val timeLimit = System.currentTimeMillis() + 4800L
    
    // 거리 행렬 미리 계산 (캐싱)
    val distanceMatrix = precomputeDistances(points)
    
    // 먼저 Nearest Neighbor 알고리즘으로 초기 경로 생성
    var path = nearestNeighborPath(points, distanceMatrix)
    
    // 초기 경로 개선을 위한 빠른 local search
    path = fastLocalSearch(path, distanceMatrix, timeLimit)
    
    // 2-opt 최적화 적용
    path = twoOpt(path, distanceMatrix, timeLimit)
    
    // 시간이 남으면 simulated annealing 적용
    path = simulatedAnnealing(path, distanceMatrix, timeLimit)
    
    // 경로 출력 형식 맞추기
    println(path.joinToString(" ") { it.index.toString() } + " 0")
}

// 모든 점 쌍 사이의 거리를 미리 계산
fun precomputeDistances(points: Array<Point>): Array<DoubleArray> {
    val n = points.size
    val distances = Array(n) { DoubleArray(n) }
    
    for (i in 0 until n) {
        for (j in i+1 until n) {
            val dist = distance(points[i], points[j])
            distances[i][j] = dist
            distances[j][i] = dist
        }
    }
    
    return distances
}

// 두 점 사이의 거리 계산
fun distance(p1: Point, p2: Point): Double {
    val dx = p1.x - p2.x
    val dy = p1.y - p2.y
    return sqrt((dx * dx + dy * dy).toDouble())
}

// 전체 경로의 길이 계산 (캐싱된 거리 사용)
fun pathLength(path: List<Point>, distanceMatrix: Array<DoubleArray>): Double {
    var length = 0.0
    for (i in 0 until path.size - 1) {
        length += distanceMatrix[path[i].index][path[i+1].index]
    }
    // 마지막 점에서 시작점으로 돌아오는 거리 추가
    length += distanceMatrix[path.last().index][path.first().index]
    return length
}

// Nearest Neighbor 알고리즘 (캐싱 사용)
fun nearestNeighborPath(points: Array<Point>, distanceMatrix: Array<DoubleArray>): List<Point> {
    val n = points.size
    val visited = BooleanArray(n) { false }
    val path = ArrayList<Point>(n)
    
    // 시작점은 항상 인덱스 0
    var current = points[0]
    path.add(current)
    visited[0] = true
    
    // 나머지 n-1개 점을 방문
    (1..n).forEach { _ ->
        var nearestIdx = -1
        var minDistance = Double.MAX_VALUE
        
        for (j in 0 until n) {
            if (!visited[j]) {
                val dist = distanceMatrix[current.index][j]
                if (dist < minDistance) {
                    minDistance = dist
                    nearestIdx = j
                }
            }
        }
        
        if (nearestIdx != -1) {
            current = points[nearestIdx]
            path.add(current)
            visited[nearestIdx] = true
        }
    }
    
    return path
}

// 빠른 지역 검색 최적화
fun fastLocalSearch(initialPath: List<Point>, distanceMatrix: Array<DoubleArray>, timeLimit: Long): List<Point> {
    val path = initialPath.toMutableList()
    val n = path.size
    var improved = true
    
    while (improved && System.currentTimeMillis() < timeLimit) {
        improved = false
        
        // 2-교환(swap)을 시도
        for (i in 1 until n-1) { // 첫 점(0)은 고정
            if (System.currentTimeMillis() >= timeLimit) break
            
            val beforeDist = distanceMatrix[path[i-1].index][path[i].index] + 
                             distanceMatrix[path[i].index][path[i+1].index]
            
            for (j in i+1 until n) {
                if (j == i+1) continue // 인접한 노드는 건너뜀
                
                val beforeJDist = distanceMatrix[path[j-1].index][path[j].index] + 
                                 (if (j < n-1) distanceMatrix[path[j].index][path[j+1].index] else distanceMatrix[path[j].index][path[0].index])
                
                val afterDist = distanceMatrix[path[i-1].index][path[j].index] + 
                                (if (j < n-1) distanceMatrix[path[i].index][path[j+1].index] else distanceMatrix[path[i].index][path[0].index])
                
                val afterJDist = distanceMatrix[path[j-1].index][path[i].index] + 
                                distanceMatrix[path[i].index][path[i+1].index]
                
                // 개선이 있는지 확인
                if (afterDist + afterJDist < beforeDist + beforeJDist) {
                    val temp = path[i]
                    path[i] = path[j]
                    path[j] = temp
                    improved = true
                    break
                }
            }
            
            if (improved) break
        }
    }
    
    return path
}

// 2-opt 최적화 알고리즘 (캐싱 사용)
fun twoOpt(path: List<Point>, distanceMatrix: Array<DoubleArray>, timeLimit: Long): List<Point> {
    val n = path.size
    val best = path.toMutableList()
    var improved = true
    
    val checkInterval = 100 // 시간 체크 주기 설정
    var iterations = 0
    
    while (improved && System.currentTimeMillis() < timeLimit) {
        improved = false
        var bestDistance = pathLength(best, distanceMatrix)
        
        outer@ for (i in 0 until n - 2) {
            for (j in i + 2 until n) {
                iterations++
                
                // 주기적으로만 시간 체크 (매번 체크하면 오버헤드)
                if (iterations % checkInterval == 0 && System.currentTimeMillis() >= timeLimit) {
                    break@outer
                }
                
                // 현재 연결: i→i+1, j→j+1
                // 새 연결: i→j, i+1→j+1
                val gain = distanceMatrix[best[i].index][best[i+1].index] +
                           distanceMatrix[best[j].index][if (j == n-1) best[0].index else best[j+1].index] -
                           distanceMatrix[best[i].index][best[j].index] -
                           distanceMatrix[best[i+1].index][if (j == n-1) best[0].index else best[j+1].index]
                
                if (gain > 0) { // 경로가 개선됨
                    // 경로의 i+1부터 j까지 뒤집기
                    reverse(best, i + 1, j)
                    bestDistance -= gain
                    improved = true
                    break
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

// Simulated Annealing 알고리즘 (캐싱 사용)
fun simulatedAnnealing(initialPath: List<Point>, distanceMatrix: Array<DoubleArray>, timeLimit: Long): List<Point> {
    val currentPath = initialPath.toMutableList()
    val bestPath = initialPath.toMutableList()
    var currentEnergy = pathLength(currentPath, distanceMatrix)
    var bestEnergy = currentEnergy
    
    val startTemp = 100.0
    var temperature = startTemp
    val coolingRate = 0.995  // 조금 더 천천히 온도 감소
    val rand = Random(System.currentTimeMillis())
    
    // 더 효율적인 반복 처리를 위한 변수들
    val checkInterval = 500
    var iterations = 0
    val endTime = timeLimit
    
    while (System.currentTimeMillis() < endTime && temperature > 0.1) {
        iterations++
        
        // 2개의 랜덤 지점 선택
        val i = rand.nextInt(1, currentPath.size)
        val j = rand.nextInt(1, currentPath.size)
        
        if (i != j) {
            // 경로 변경 - 두 점 교환
            val temp = currentPath[i]
            currentPath[i] = currentPath[j]
            currentPath[j] = temp
            
            val newEnergy = pathLength(currentPath, distanceMatrix)
            val delta = newEnergy - currentEnergy
            
            // 더 나은 해결책이거나 확률에 따라 나쁜 해결책 수용
            if (delta < 0 || rand.nextDouble() < exp(-delta / temperature)) {
                currentEnergy = newEnergy
                
                if (currentEnergy < bestEnergy) {
                    bestPath.clear()
                    bestPath.addAll(currentPath)
                    bestEnergy = currentEnergy
                }
            } else {
                // 변경 취소
                val temp = currentPath[i]
                currentPath[i] = currentPath[j]
                currentPath[j] = temp
            }
        }
        
        // 주기적으로만 시간 체크
        if (iterations % checkInterval == 0) {
            if (System.currentTimeMillis() >= endTime) break
            temperature *= coolingRate
        }
    }
    
    return bestPath
}
