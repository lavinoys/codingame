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

// 거리 캐시를 위한 맵 - 더 빠른 접근을 위해 Array 사용
private lateinit var distanceCache: Array<Array<Int>>
private var customerCount = 0

// 거리 캐시 초기화 - 배열 기반으로 변경
fun initDistanceCache(customers: List<Customer>) {
    customerCount = customers.size
    distanceCache = Array(customerCount) { Array(customerCount) { 0 } }
    
    val indexMap = customers.associateWith { customers.indexOf(it) }
    
    for (i in customers.indices) {
        for (j in i + 1 until customers.size) {
            val a = customers[i]
            val b = customers[j]
            val dist = sqrt((a.x - b.x).toDouble().pow(2) + (a.y - b.y).toDouble().pow(2)).roundToInt()
            
            val idxA = indexMap[a] ?: i
            val idxB = indexMap[b] ?: j
            
            distanceCache[idxA][idxB] = dist
            distanceCache[idxB][idxA] = dist
        }
    }
}

// 두 지점 사이의 거리 계산 (배열 기반 캐시 사용)
fun distance(a: Customer, b: Customer): Int {
    val idxA = a.index
    val idxB = b.index
    return if (idxA < customerCount && idxB < customerCount) {
        distanceCache[idxA][idxB]
    } else {
        sqrt((a.x - b.x).toDouble().pow(2) + (a.y - b.y).toDouble().pow(2)).roundToInt()
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

// 시뮬레이티드 어닐링 알고리즘 (추가 최적화)
fun simulatedAnnealing(depot: Customer, initialSolution: List<List<Customer>>, capacity: Int): List<List<Customer>> {
    // 객체 생성 최소화를 위한 공유 버퍼 사용
    val currentSolution = initialSolution.map { it.toMutableList() }.toMutableList()
    val bestSolution = initialSolution.map { it.toMutableList() }.toMutableList()
    val tempSolution = initialSolution.map { it.toMutableList() }.toMutableList()
    
    var currentEnergy = calculateTotalDistance(depot, currentSolution)
    var bestEnergy = currentEnergy
    
    // 시간 제약 및 최적화된 파라미터
    var temperature = 500.0 // 낮은 온도에서 시작
    val coolingRate = 0.99 // 더 빠른 냉각률
    val minTemperature = 0.1 // 더 높은 최소 온도
    val startTime = System.currentTimeMillis()
    val timeLimit = 1500 // 제한 시간 축소
    
    var iterations = 0
    var noImprovementCount = 0
    val neighborBufferA = currentSolution.map { it.toMutableList() }.toMutableList()
    val neighborBufferB = currentSolution.map { it.toMutableList() }.toMutableList()
    
    // 현재 정책을 효과적인 방향으로 강제 조정
    var swapWithinRouteWeight = 1
    var moveBetweenRouteWeight = 1
    var reverseSubrouteWeight = 1
    var relocateCustomerWeight = 1
    var swapBetweenRoutesWeight = 1
    val operationSuccessCount = IntArray(5) { 0 }
    
    while (temperature > minTemperature && System.currentTimeMillis() - startTime < timeLimit) {
        iterations++
        
        // 한 번에 여러 이웃 해결책을 생성하고 최선의 것만 선택
        if (iterations % 100 == 0) {
            // 각 연산 성공률에 따라 가중치 조정
            val totalOps = operationSuccessCount.sum().coerceAtLeast(1)
            if (totalOps > 0) {
                swapWithinRouteWeight = ((operationSuccessCount[0] / totalOps.toDouble()) * 10).toInt().coerceAtLeast(1)
                moveBetweenRouteWeight = ((operationSuccessCount[1] / totalOps.toDouble()) * 10).toInt().coerceAtLeast(1)
                reverseSubrouteWeight = ((operationSuccessCount[2] / totalOps.toDouble()) * 10).toInt().coerceAtLeast(1)
                relocateCustomerWeight = ((operationSuccessCount[3] / totalOps.toDouble()) * 10).toInt().coerceAtLeast(1)
                swapBetweenRoutesWeight = ((operationSuccessCount[4] / totalOps.toDouble()) * 10).toInt().coerceAtLeast(1)
            }
        }
        
        // 지역 최적화 주기적 적용 - 빈도 줄임
        if (iterations % 500 == 0) {
            applyLocalOptimization(currentSolution, depot)
            currentEnergy = calculateTotalDistance(depot, currentSolution)
        }
        
        // 이웃 생성 전 현재 해결책 백업
        copySolution(currentSolution, neighborBufferA)
        
        // 가중치 기반 이웃 해결책 생성
        val totalWeight = swapWithinRouteWeight + moveBetweenRouteWeight + 
                         reverseSubrouteWeight + relocateCustomerWeight + swapBetweenRoutesWeight
        val rand = Random.nextInt(totalWeight)
        var opIdx = 0
        var cumulativeWeight = 0
        
        for (i in 0 until 5) {
            cumulativeWeight += when(i) {
                0 -> swapWithinRouteWeight
                1 -> moveBetweenRouteWeight
                2 -> reverseSubrouteWeight
                3 -> relocateCustomerWeight
                else -> swapBetweenRoutesWeight
            }
            if (rand < cumulativeWeight) {
                opIdx = i
                break
            }
        }
        
        // 적용할 연산 선택
        val success = when (opIdx) {
            0 -> swapCustomersWithinRoute(neighborBufferA)
            1 -> moveCustomerBetweenRoutes(neighborBufferA, capacity)
            2 -> reverseSubroute(neighborBufferA)
            3 -> relocateCustomer(neighborBufferA)
            else -> swapCustomersBetweenRoutes(neighborBufferA)
        }
        
        if (success) {
            operationSuccessCount[opIdx]++
        }
        
        // 이웃 해결책의 에너지 계산 - 최적화된 증분 계산 가능
        val newEnergy = calculateTotalDistance(depot, neighborBufferA)
        
        // 새 해결책 수락 또는 거부
        if (acceptanceProbability(currentEnergy, newEnergy, temperature) > Random.nextDouble()) {
            // 현재 해결책 갱신 (객체 복사 최소화)
            copySolution(neighborBufferA, currentSolution)
            currentEnergy = newEnergy
            
            if (currentEnergy < bestEnergy) {
                copySolution(currentSolution, bestSolution)
                bestEnergy = currentEnergy
                noImprovementCount = 0
            } else {
                noImprovementCount++
            }
        } else {
            noImprovementCount++
        }
        
        // 조기 종료 조건 - 일정 시간 이후 개선이 없으면 종료
        if (noImprovementCount > 5000) {
            // 너무 많이 재가열하는 대신, 일정 임계치 이상에서는 종료
            if (temperature < 10.0) break
            temperature *= 1.2 // 소폭만 재가열
            noImprovementCount = 0
        } else {
            temperature *= coolingRate
        }
    }
    
    return bestSolution
}

// 효율적인 솔루션 복사 (객체 생성 없이)
fun copySolution(source: List<List<Customer>>, target: MutableList<MutableList<Customer>>) {
    target.clear()
    for (route in source) {
        val newRoute = mutableListOf<Customer>()
        newRoute.addAll(route)
        target.add(newRoute)
    }
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

// 이웃 해결책 생성 (최적화 - 객체 생성 최소화)
fun generateNeighbor(
    solution: MutableList<MutableList<Customer>>, 
    target: MutableList<MutableList<Customer>>,
    capacity: Int
): Boolean {
    // 객체 재사용
    copySolution(solution, target)
    
    // 성공 여부를 반환하는 수정된 함수들 사용
    return when (Random.nextInt(5)) {
        0 -> swapCustomersWithinRoute(target)
        1 -> moveCustomerBetweenRoutes(target, capacity)
        2 -> reverseSubroute(target)
        3 -> relocateCustomer(target)
        else -> swapCustomersBetweenRoutes(target)
    }
}

// 한 경로 내에서 두 고객 위치 교환 (성공 여부 반환)
fun swapCustomersWithinRoute(solution: MutableList<MutableList<Customer>>): Boolean {
    if (solution.isEmpty()) return false
    
    // 비어있지 않은 경로 선택
    val nonEmptyRoutes = solution.filter { it.size >= 2 }
    if (nonEmptyRoutes.isEmpty()) return false
    
    val route = nonEmptyRoutes.random()
    
    val i = Random.nextInt(route.size)
    var j = Random.nextInt(route.size)
    while (i == j) j = Random.nextInt(route.size)
    
    val temp = route[i]
    route[i] = route[j]
    route[j] = temp
    return true
}

// 서로 다른 경로 간 고객 이동 (성공 여부 반환)
fun moveCustomerBetweenRoutes(solution: MutableList<MutableList<Customer>>, capacity: Int): Boolean {
    if (solution.size < 2) return false
    
    // 비어있지 않은 경로 선택
    val nonEmptyRoutes = solution.indices.filter { solution[it].isNotEmpty() }.toList()
    if (nonEmptyRoutes.size < 2) return false
    
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
        
        // 효율 개선: 이동 전에 최적의 위치 계산 빠르게 수행
        var bestPosition = 0
        if (destRoute.isNotEmpty()) {
            var minIncrease = Double.MAX_VALUE
            for (pos in 0..destRoute.size) {
                val increase = calculateInsertionCost(destRoute, pos, customer)
                if (increase < minIncrease) {
                    minIncrease = increase
                    bestPosition = pos
                }
            }
        }
        
        destRoute.add(bestPosition, customer)
        
        // 경로가 비었다면 제거
        if (sourceRoute.isEmpty()) {
            solution.removeAt(sourceRouteIdx)
        }
        return true
    }
    return false
}

// 삽입 비용 계산 최적화 함수
fun calculateInsertionCost(route: List<Customer>, pos: Int, customer: Customer): Double {
    val before = if (pos > 0) route[pos-1] else null
    val after = if (pos < route.size) route[pos] else null
    
    val oldDistance = if (before != null && after != null) {
        distance(before, after).toDouble()
    } else 0.0
    
    val newDistance = (if (before != null) distance(before, customer).toDouble() else 0.0) +
                      (if (after != null) distance(customer, after).toDouble() else 0.0)
    
    return newDistance - oldDistance
}

// 서브루트 역전 (성공 여부 반환)
fun reverseSubroute(solution: MutableList<MutableList<Customer>>): Boolean {
    if (solution.isEmpty()) return false
    
    // 역전 가능한 경로 필터링
    val reversibleRoutes = solution.filter { it.size >= 3 }
    if (reversibleRoutes.isEmpty()) return false
    
    val route = reversibleRoutes.random()
    
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
    return true
}

// 고객 재배치 (성공 여부 반환)
fun relocateCustomer(solution: MutableList<MutableList<Customer>>): Boolean {
    if (solution.isEmpty()) return false
    
    val nonEmptyRoutes = solution.filter { it.size >= 2 }
    if (nonEmptyRoutes.isEmpty()) return false
    
    val route = nonEmptyRoutes.random()
    
    val fromPos = Random.nextInt(route.size)
    var toPos = Random.nextInt(route.size)
    
    if (fromPos != toPos) {
        val customer = route.removeAt(fromPos)
        if (toPos > fromPos) toPos--
        route.add(toPos, customer)
        return true
    }
    return false
}

// 서로 다른 경로 간 고객 교환 (성공 여부 반환)
fun swapCustomersBetweenRoutes(solution: MutableList<MutableList<Customer>>): Boolean {
    if (solution.size < 2) return false
    
    val nonEmptyRoutes = solution.indices.filter { solution[it].isNotEmpty() }.toList()
    if (nonEmptyRoutes.size < 2) return false
    
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
    return true
}

// 해결책 수락 확률 계산 (보정) - 빠른 결정을 위해 계산 간소화
fun acceptanceProbability(currentEnergy: Double, newEnergy: Double, temperature: Double): Double {
    if (newEnergy < currentEnergy) return 1.0
    // 성능 최적화: exp 함수 호출 줄이기 위한 빠른 판별
    val delta = currentEnergy - newEnergy
    if (delta < -20 * temperature) return 0.0 // 거의 0에 가까운 확률일 경우 바로 0 반환
    return exp((currentEnergy - newEnergy) / temperature)
}

// 해결책을 형식화된 문자열로 변환
fun formatSolution(solution: List<List<Customer>>): String {
    return solution.joinToString(";") { route ->
        route.joinToString(" ") { it.index.toString() }
    }
}

