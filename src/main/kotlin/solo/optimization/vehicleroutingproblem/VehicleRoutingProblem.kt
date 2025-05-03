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
    
    // 2. 시뮬레이티드 어닐링으로 해결책 개선
    val finalSolution = simulatedAnnealing(depot, initialSolution, c)
    
    // 결과 출력
    println(formatSolution(finalSolution))
}

// 거리 캐시를 위한 맵
private val distanceCache = mutableMapOf<Pair<Int, Int>, Int>()

// 거리 캐시 초기화
fun initDistanceCache(customers: List<Customer>) {
    for (i in customers.indices) {
        for (j in i + 1 until customers.size) {
            val a = customers[i]
            val b = customers[j]
            val dist = sqrt((a.x - b.x).toDouble().pow(2) + (a.y - b.y).toDouble().pow(2)).roundToInt()
            distanceCache[Pair(a.index, b.index)] = dist
            distanceCache[Pair(b.index, a.index)] = dist
        }
    }
}

// 두 지점 사이의 거리 계산 (캐시 이용)
fun distance(a: Customer, b: Customer): Int {
    return distanceCache[Pair(a.index, b.index)] ?: run {
        val dist = sqrt((a.x - b.x).toDouble().pow(2) + (a.y - b.y).toDouble().pow(2)).roundToInt()
        distanceCache[Pair(a.index, b.index)] = dist
        distanceCache[Pair(b.index, a.index)] = dist
        dist
    }
}

// 탐욕 알고리즘으로 초기 해결책 생성 (개선)
fun greedySolution(depot: Customer, customers: List<Customer>, capacity: Int): List<MutableList<Customer>> {
    val remainingCustomers = customers.toMutableList()
    val routes = mutableListOf<MutableList<Customer>>()
    
    while (remainingCustomers.isNotEmpty()) {
        val route = mutableListOf<Customer>()
        var currentCapacity = 0
        var currentPosition = depot
        
        // 가능한 한 많은 고객을 현재 경로에 추가
        while (remainingCustomers.isNotEmpty()) {
            // 가장 가까운 고객 찾기 (성능 개선)
            var nearest: Customer? = null
            var minDistance = Int.MAX_VALUE
            
            for (customer in remainingCustomers) {
                val dist = distance(currentPosition, customer)
                if (dist < minDistance) {
                    minDistance = dist
                    nearest = customer
                }
            }
            
            nearest ?: break
            
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

// 총 거리 계산 (캐싱 활용)
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

// 시뮬레이티드 어닐링 알고리즘 (개선)
fun simulatedAnnealing(depot: Customer, initialSolution: List<List<Customer>>, capacity: Int): List<List<Customer>> {
    // 깊은 복사 대신 효율적인 변환
    val currentSolution = initialSolution.map { it.toMutableList() }.toMutableList()
    var bestSolution = initialSolution.map { it.toMutableList() }
    var currentEnergy = calculateTotalDistance(depot, currentSolution)
    var bestEnergy = currentEnergy
    
    // 개선된 온도 스케줄링
    var temperature = 1000.0
    val coolingRate = 0.997 // 더 천천히 냉각
    val minTemperature = 0.01
    val startTime = System.currentTimeMillis()
    val timeLimit = 7800 // 더 많은 시간 활용 (안전하게)
    
    // 개선책 없이 지난 반복 횟수 카운트
    var noImprovementCount = 0
    
    while (temperature > minTemperature && System.currentTimeMillis() - startTime < timeLimit) {
        // 주기적으로 지역 최적화 적용
        if (Random.nextInt(100) < 5) {
            applyLocalOptimization(currentSolution, depot)
            currentEnergy = calculateTotalDistance(depot, currentSolution)
        }
        
        val newSolution = generateNeighbor(currentSolution, capacity)
        val newEnergy = calculateTotalDistance(depot, newSolution)
        
        // 새 해결책이 더 좋거나 확률적으로 수락
        if (acceptanceProbability(currentEnergy, newEnergy, temperature) > Random.nextDouble()) {
            // 깊은 복사 대신 참조 변경
            currentSolution.clear()
            currentSolution.addAll(newSolution)
            currentEnergy = newEnergy
            
            if (currentEnergy < bestEnergy) {
                bestSolution = currentSolution.map { it.toMutableList() }
                bestEnergy = currentEnergy
                noImprovementCount = 0
            } else {
                noImprovementCount++
            }
        } else {
            noImprovementCount++
        }
        
        // 일정 횟수 개선이 없으면 온도를 높임 (재가열)
        if (noImprovementCount > 1000) {
            temperature *= 1.5
            noImprovementCount = 0
        } else {
            temperature *= coolingRate
        }
    }
    
    return bestSolution
}

// 지역 최적화 적용 (2-opt)
fun applyLocalOptimization(solution: MutableList<MutableList<Customer>>, depot: Customer) {
    for (route in solution) {
        if (route.size >= 4) {
            var improved = true
            while (improved) {
                improved = false
                
                for (i in 0 until route.size - 1) {
                    for (j in i + 2 until route.size) {
                        // 2-opt swap 시도
                        val currentDistance = 
                            (if (i > 0) distance(route[i-1], route[i]) else distance(depot, route[i])) +
                            (if (j < route.size - 1) distance(route[j], route[j+1]) else distance(route[j], depot))
                        
                        val newDistance = 
                            (if (i > 0) distance(route[i-1], route[j]) else distance(depot, route[j])) +
                            (if (j < route.size - 1) distance(route[i], route[j+1]) else distance(route[i], depot))
                        
                        if (newDistance < currentDistance) {
                            // 개선되면 reverse 적용
                            route.subList(i, j + 1).reverse()
                            improved = true
                            break
                        }
                    }
                    if (improved) break
                }
            }
        }
    }
}

// 이웃 해결책 생성 (개선)
fun generateNeighbor(solution: MutableList<MutableList<Customer>>, capacity: Int): MutableList<MutableList<Customer>> {
    val newSolution = solution.map { it.toMutableList() }.toMutableList()
    
    // 더 다양한 이웃 생성 전략
    when (Random.nextInt(5)) {
        0 -> swapCustomersWithinRoute(newSolution)
        1 -> moveCustomerBetweenRoutes(newSolution, capacity)
        2 -> reverseSubroute(newSolution)
        3 -> relocateCustomer(newSolution) // 새로운 전략
        4 -> swapCustomersBetweenRoutes(newSolution) // 새로운 전략
    }
    
    return newSolution
}

// 한 경로 내에서 두 고객 위치 교환
fun swapCustomersWithinRoute(solution: MutableList<MutableList<Customer>>) {
    if (solution.isEmpty()) return
    
    // 비어있지 않은 경로 선택
    val nonEmptyRoutes = solution.filter { it.size >= 2 }
    if (nonEmptyRoutes.isEmpty()) return
    
    val route = nonEmptyRoutes.random()
    val routeIdx = solution.indexOf(route)
    
    val i = Random.nextInt(route.size)
    var j = Random.nextInt(route.size)
    while (i == j) j = Random.nextInt(route.size)
    
    val temp = route[i]
    route[i] = route[j]
    route[j] = temp
}

// 서로 다른 경로 간 고객 이동 (개선)
fun moveCustomerBetweenRoutes(solution: MutableList<MutableList<Customer>>, capacity: Int) {
    if (solution.size < 2) return
    
    // 비어있지 않은 경로 선택
    val nonEmptyRoutes = solution.indices.filter { solution[it].isNotEmpty() }.toList()
    if (nonEmptyRoutes.size < 2) return
    
    val sourceRouteIdx = nonEmptyRoutes.random()
    val sourceRoute = solution[sourceRouteIdx]
    
    val otherRoutes = nonEmptyRoutes.filter { it != sourceRouteIdx }
    val destRouteIdx = otherRoutes.random()
    val destRoute = solution[destRouteIdx]
    
    val customerIdx = Random.nextInt(sourceRoute.size)
    val customer = sourceRoute[customerIdx]
    
    // 용량 제약 확인
    val destRouteTotalDemand = destRoute.sumOf { it.demand }
    if (destRouteTotalDemand + customer.demand <= capacity) {
        sourceRoute.removeAt(customerIdx)
        // 가장 좋은 위치 찾기
        var bestPosition = 0
        var minIncrease = Double.MAX_VALUE
        
        for (pos in 0..destRoute.size) {
            val before = if (pos > 0) destRoute[pos-1] else null
            val after = if (pos < destRoute.size) destRoute[pos] else null
            
            val oldDistance = if (before != null && after != null) {
                distance(before, after).toDouble()
            } else 0.0
            
            val newDistance = (if (before != null) distance(before, customer).toDouble() else 0.0) +
                              (if (after != null) distance(customer, after).toDouble() else 0.0)
            
            val increase = newDistance - oldDistance
            if (increase < minIncrease) {
                minIncrease = increase
                bestPosition = pos
            }
        }
        
        destRoute.add(bestPosition, customer)
        
        // 경로가 비었다면 제거
        if (sourceRoute.isEmpty()) {
            solution.removeAt(sourceRouteIdx)
        }
    }
}

// 서브루트 역전 (개선)
fun reverseSubroute(solution: MutableList<MutableList<Customer>>) {
    if (solution.isEmpty()) return
    
    // 역전 가능한 경로 필터링
    val reversibleRoutes = solution.filter { it.size >= 3 }
    if (reversibleRoutes.isEmpty()) return
    
    val route = reversibleRoutes.random()
    val routeIdx = solution.indexOf(route)
    
    val start = Random.nextInt(route.size - 2)
    val end = start + 1 + Random.nextInt(route.size - start - 1)
    
    var i = start
    var j = end
    while (i < j) {
        val temp = route[i]
        route[i] = route[j]
        route[j] = temp
        i++
        j--
    }
}

// 새로운 이웃 생성 전략: 고객 재배치
fun relocateCustomer(solution: MutableList<MutableList<Customer>>) {
    if (solution.isEmpty()) return
    
    // 비어있지 않은 경로 선택
    val nonEmptyRoutes = solution.filter { it.size >= 2 }
    if (nonEmptyRoutes.isEmpty()) return
    
    val route = nonEmptyRoutes.random()
    val routeIdx = solution.indexOf(route)
    
    val fromPos = Random.nextInt(route.size)
    var toPos = Random.nextInt(route.size)
    
    if (fromPos != toPos) {
        val customer = route.removeAt(fromPos)
        if (toPos > fromPos) toPos--
        route.add(toPos, customer)
    }
}

// 새로운 이웃 생성 전략: 서로 다른 경로 간 고객 교환
fun swapCustomersBetweenRoutes(solution: MutableList<MutableList<Customer>>) {
    if (solution.size < 2) return
    
    // 비어있지 않은 경로 선택
    val nonEmptyRoutes = solution.indices.filter { solution[it].isNotEmpty() }.toList()
    if (nonEmptyRoutes.size < 2) return
    
    val routeIdx1 = nonEmptyRoutes.random()
    var routeIdx2 = nonEmptyRoutes.random()
    while (routeIdx1 == routeIdx2) {
        routeIdx2 = nonEmptyRoutes.random()
    }
    
    val route1 = solution[routeIdx1]
    val route2 = solution[routeIdx2]
    
    val custIdx1 = Random.nextInt(route1.size)
    val custIdx2 = Random.nextInt(route2.size)
    
    val temp = route1[custIdx1]
    route1[custIdx1] = route2[custIdx2]
    route2[custIdx2] = temp
}

// 해결책 수락 확률 계산 (보정)
fun acceptanceProbability(currentEnergy: Double, newEnergy: Double, temperature: Double): Double {
    if (newEnergy < currentEnergy) return 1.0
    return exp(min(-1.0, (currentEnergy - newEnergy) / temperature))
}

// 해결책을 형식화된 문자열로 변환
fun formatSolution(solution: List<List<Customer>>): String {
    return solution.joinToString(";") { route ->
        route.joinToString(" ") { it.index.toString() }
    }
}
