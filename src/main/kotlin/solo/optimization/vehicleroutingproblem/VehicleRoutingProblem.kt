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
    
    // 1. 탐욕 알고리즘으로 초기 해결책 생성
    val initialSolution = greedySolution(depot, actualCustomers, c)
    
    // 2. 시뮬레이티드 어닐링으로 해결책 개선
    val finalSolution = simulatedAnnealing(depot, initialSolution, c)
    
    // 결과 출력
    println(formatSolution(finalSolution))
}

// 두 지점 사이의 거리 계산 (유클리드 거리, 반올림)
fun distance(a: Customer, b: Customer): Int {
    return sqrt((a.x - b.x).toDouble().pow(2) + (a.y - b.y).toDouble().pow(2)).roundToInt()
}

// 탐욕 알고리즘으로 초기 해결책 생성
fun greedySolution(depot: Customer, customers: List<Customer>, capacity: Int): List<List<Customer>> {
    val remainingCustomers = customers.toMutableList()
    val routes = mutableListOf<MutableList<Customer>>()
    
    while (remainingCustomers.isNotEmpty()) {
        val route = mutableListOf<Customer>()
        var currentCapacity = 0
        var currentPosition = depot
        
        // 가능한 한 많은 고객을 현재 경로에 추가
        while (remainingCustomers.isNotEmpty()) {
            // 가장 가까운 고객 찾기
            val nearest = remainingCustomers.minByOrNull { distance(currentPosition, it) } ?: break
            
            // 용량 제한 확인
            if (currentCapacity + nearest.demand <= capacity) {
                route.add(nearest)
                currentCapacity += nearest.demand
                currentPosition = nearest
                remainingCustomers.remove(nearest)
            } else {
                break
            }
        }
        
        if (route.isNotEmpty()) {
            routes.add(route)
        }
    }
    
    return routes
}

// 총 거리 계산
fun calculateTotalDistance(depot: Customer, solution: List<List<Customer>>): Double {
    var totalDistance = 0.0
    
    for (route in solution) {
        if (route.isEmpty()) continue
        
        // 창고에서 첫 번째 고객까지의 거리
        totalDistance += distance(depot, route.first())
        
        // 경로를 따라가는 거리
        for (i in 0 until route.size - 1) {
            totalDistance += distance(route[i], route[i + 1])
        }
        
        // 마지막 고객에서 창고로 돌아오는 거리
        totalDistance += distance(route.last(), depot)
    }
    
    return totalDistance
}

// 시뮬레이티드 어닐링 알고리즘
fun simulatedAnnealing(depot: Customer, initialSolution: List<List<Customer>>, capacity: Int): List<List<Customer>> {
    var currentSolution = initialSolution.map { it.toMutableList() }.toMutableList()
    var bestSolution = currentSolution.map { it.toMutableList() }.toMutableList()
    var currentEnergy = calculateTotalDistance(depot, currentSolution)
    var bestEnergy = currentEnergy
    
    var temperature = 1000.0
    val coolingRate = 0.995
    val minTemperature = 0.1
    val startTime = System.currentTimeMillis()
    val timeLimit = 7000 // 7초 시간제한 (안전한 마진)
    
    while (temperature > minTemperature && System.currentTimeMillis() - startTime < timeLimit) {
        val newSolution = generateNeighbor(currentSolution, capacity)
        val newEnergy = calculateTotalDistance(depot, newSolution)
        
        // 새 해결책이 더 좋거나 확률적으로 수락
        if (acceptanceProbability(currentEnergy, newEnergy, temperature) > Math.random()) {
            currentSolution = newSolution
            currentEnergy = newEnergy
            
            if (currentEnergy < bestEnergy) {
                bestSolution = currentSolution.map { it.toMutableList() }.toMutableList()
                bestEnergy = currentEnergy
            }
        }
        
        temperature *= coolingRate
    }
    
    return bestSolution
}

// 이웃 해결책 생성
fun generateNeighbor(solution: MutableList<MutableList<Customer>>, capacity: Int): MutableList<MutableList<Customer>> {
    val newSolution = solution.map { it.toMutableList() }.toMutableList()
    
    when (Random.nextInt(3)) {
        0 -> swapCustomersWithinRoute(newSolution)
        1 -> moveCustomerBetweenRoutes(newSolution, capacity)
        2 -> reverseSubroute(newSolution)
    }
    
    return newSolution
}

// 한 경로 내에서 두 고객 위치 교환
fun swapCustomersWithinRoute(solution: MutableList<MutableList<Customer>>) {
    if (solution.isEmpty()) return
    
    val routeIdx = Random.nextInt(solution.size)
    val route = solution[routeIdx]
    
    if (route.size < 2) return
    
    val i = Random.nextInt(route.size)
    var j = Random.nextInt(route.size)
    while (i == j) j = Random.nextInt(route.size)
    
    val temp = route[i]
    route[i] = route[j]
    route[j] = temp
}

// 서로 다른 경로 간 고객 이동
fun moveCustomerBetweenRoutes(solution: MutableList<MutableList<Customer>>, capacity: Int) {
    if (solution.size < 2) return
    
    val sourceRouteIdx = Random.nextInt(solution.size)
    val sourceRoute = solution[sourceRouteIdx]
    
    if (sourceRoute.isEmpty()) return
    
    val destRouteIdx = (sourceRouteIdx + 1 + Random.nextInt(solution.size - 1)) % solution.size
    val destRoute = solution[destRouteIdx]
    
    val customerIdx = Random.nextInt(sourceRoute.size)
    val customer = sourceRoute[customerIdx]
    
    // 용량 제약 확인
    val destRouteTotalDemand = destRoute.sumOf { it.demand }
    if (destRouteTotalDemand + customer.demand <= capacity) {
        sourceRoute.removeAt(customerIdx)
        destRoute.add(customer)
        
        // 경로가 비었다면 제거
        if (sourceRoute.isEmpty()) {
            solution.removeAt(sourceRouteIdx)
        }
    }
}

// 서브루트 역전
fun reverseSubroute(solution: MutableList<MutableList<Customer>>) {
    if (solution.isEmpty()) return
    
    val routeIdx = Random.nextInt(solution.size)
    val route = solution[routeIdx]
    
    if (route.size < 2) return
    
    val start = Random.nextInt(route.size - 1)
    val end = start + 1 + Random.nextInt(route.size - start - 1)
    
    for (i in 0 until (end - start + 1) / 2) {
        val temp = route[start + i]
        route[start + i] = route[end - i]
        route[end - i] = temp
    }
}

// 해결책 수락 확률 계산
fun acceptanceProbability(currentEnergy: Double, newEnergy: Double, temperature: Double): Double {
    if (newEnergy < currentEnergy) return 1.0
    return exp((currentEnergy - newEnergy) / temperature)
}

// 해결책을 형식화된 문자열로 변환
fun formatSolution(solution: List<List<Customer>>): String {
    return solution.joinToString(";") { route ->
        route.joinToString(" ") { it.index.toString() }
    }
}
