import java.util.*
import kotlin.math.*
import kotlin.random.Random

/**
 * Challenge yourself with this classic NP-Hard optimization problem !
 **/

data class Customer(val index: Int, val x: Int, val y: Int, val demand: Int)

fun main() {
    val input = Scanner(System.`in`)
    val n = input.nextInt() // The number of customers
    val c = input.nextInt() // The capacity of the vehicles
    
    val customers = mutableListOf<Customer>()
    
    // 모든 고객 정보를 읽어오기
    (0..n).forEach { _ ->
        val index = input.nextInt() // The index of the customer (0 is the depot)
        val x = input.nextInt() // The x coordinate of the customer
        val y = input.nextInt() // The y coordinate of the customer
        val demand = input.nextInt() // The demand
        customers.add(Customer(index, x, y, demand))
    }
    
    // 창고는 항상 인덱스 0
    val depot = customers.first { it.index == 0 }
    // 실제 고객들만 분리 (창고 제외)
    val actualCustomers = customers.filter { it.index != 0 }
    
    // 거리 캐시 초기화
    initDistanceCache(customers)
    
    // 1. 탐욕 알고리즘으로 초기 해결책 생성
    val initialSolution = greedySolution(depot, actualCustomers, c)
    
    // 빠른 첫번째 응답을 위해 초기 해결책 즉시 출력
    println(formatSolution(initialSolution))
    System.out.flush()
    
    // 2. 시뮬레이티드 어닐링으로 해결책 개선
    val finalSolution = simulatedAnnealing(depot, initialSolution, c)
    
    // 최종 결과 출력
    println(formatSolution(finalSolution))
}

// 거리 캐시를 위한 맵 - 더 빠른 접근을 위해 Array 사용
private lateinit var distanceCache: Array<Array<Int>>
private var customerCount = 0
private lateinit var customerIndexToArrayIndex: Map<Int, Int> // 추가: 인덱스 매핑

// 거리 캐시 초기화 함수 수정
fun initDistanceCache(customers: List<Customer>) {
    customerCount = customers.size
    distanceCache = Array(customerCount) { Array(customerCount) { 0 } }
    
    // Customer.index와 배열 인덱스 간의 매핑 생성
    customerIndexToArrayIndex = customers.mapIndexed { arrayIndex, customer -> customer.index to arrayIndex }.toMap()
}

// 해결책을 출력 형식으로 변환하는 함수 수정 - depot(index 0)을 제외함
fun formatSolution(routes: List<List<Customer>>): String {
    return buildString {
        append(routes.size) // 경로 수
        append('\n')
        
        routes.forEach { route ->
            // depot(인덱스 0)을 제외하고 고객 인덱스만 출력
            val customerIndices = route.map { it.index }
            append(customerIndices.size) // 해당 경로의 고객 수
            customerIndices.forEach { index ->
                append(' ')
                append(index)
            }
            append('\n')
        }
    }
}

// 경로의 총 거리 계산 함수 (depot에서 출발해서 depot로 돌아오는 경로 가정)
fun calculateRouteDistance(depot: Customer, route: List<Customer>): Int {
    if (route.isEmpty()) return 0
    
    var distance = getDistance(depot, route.first()) // depot에서 첫 고객까지
    
    // 고객 간 이동 거리
    for (i in 0 until route.size - 1) {
        distance += getDistance(route[i], route[i + 1])
    }
    
    // 마지막 고객에서 depot로 돌아가는 거리
    distance += getDistance(route.last(), depot)
    
    return distance
}

// 두 고객 간의 거리 계산 (캐시 활용)
fun getDistance(a: Customer, b: Customer): Int {
    val aIndex = customerIndexToArrayIndex[a.index] ?: error("Invalid customer index: ${a.index}")
    val bIndex = customerIndexToArrayIndex[b.index] ?: error("Invalid customer index: ${b.index}")
    
    if (distanceCache[aIndex][bIndex] == 0 && aIndex != bIndex) {
        val dx = a.x - b.x
        val dy = a.y - b.y
        val distance = sqrt(dx.toDouble().pow(2) + dy.toDouble().pow(2)).toInt()
        distanceCache[aIndex][bIndex] = distance
        distanceCache[bIndex][aIndex] = distance // 대칭성 활용
    }
    
    return distanceCache[aIndex][bIndex]
}
