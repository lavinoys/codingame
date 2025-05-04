import java.util.*
import java.io.*
import java.math.*
import kotlin.math.pow
import kotlin.math.round
import kotlin.math.sqrt
import kotlin.math.exp
import kotlin.math.ln
import kotlin.random.Random
import kotlin.collections.HashMap
import java.util.concurrent.ConcurrentHashMap

/**
 * Challenge yourself with this classic NP-Hard optimization problem !
 **/

// 고객 또는 창고를 나타내는 데이터 클래스
data class Location(val index: Int, val x: Int, val y: Int, val demand: Int)

// 성능 최적화: 거리 계산 캐시
object DistanceCache {
    private val cache = ConcurrentHashMap<Pair<Location, Location>, Int>()
    
    fun distanceBetween(a: Location, b: Location): Int {
        val key = if (a.index <= b.index) Pair(a, b) else Pair(b, a)
        return cache.computeIfAbsent(key) { (loc1, loc2) ->
            round(sqrt((loc1.x - loc2.x).toDouble().pow(2) + (loc1.y - loc2.y).toDouble().pow(2))).toInt()
        }
    }
    
    fun clear() {
        cache.clear()
    }
}

// 차량 경로를 나타내는 클래스
class Route(val capacity: Int) {
    val customers = mutableListOf<Location>()
    var currentDemand = 0
    private var totalDistanceCache: Int? = null
    
    fun canAdd(customer: Location): Boolean {
        return currentDemand + customer.demand <= capacity
    }

    fun add(customer: Location) {
        customers.add(customer)
        currentDemand += customer.demand
        totalDistanceCache = null // 캐시 무효화
    }

    fun getTotalDistance(depot: Location): Int {
        totalDistanceCache?.let { return it }
        
        if (customers.isEmpty()) return 0

        var distance = DistanceCache.distanceBetween(depot, customers.first())
        
        for (i in 0 until customers.size - 1) {
            distance += DistanceCache.distanceBetween(customers[i], customers[i + 1])
        }
        
        distance += DistanceCache.distanceBetween(customers.last(), depot)
        totalDistanceCache = distance
        return distance
    }
    
    // 경로의 깊은 복사본 생성
    fun deepCopy(): Route {
        val copy = Route(capacity)
        copy.customers.addAll(customers)
        copy.currentDemand = currentDemand
        copy.totalDistanceCache = totalDistanceCache
        return copy
    }

    override fun toString(): String {
        return customers.joinToString(" ") { it.index.toString() }
    }
}

// 절약값 저장을 위한 클래스
data class Saving(val i: Location, val j: Location, val saving: Int)

// 전체 솔루션을 나타내는 클래스
class Solution(val routes: MutableList<Route>, val depot: Location) {
    private var totalDistanceCache: Int? = null
    
    fun getTotalDistance(): Int {
        totalDistanceCache?.let { return it }
        
        val distance = routes.sumOf { it.getTotalDistance(depot) }
        totalDistanceCache = distance
        return distance
    }
    
    // 솔루션의 깊은 복사본 생성
    fun deepCopy(): Solution {
        return Solution(routes.map { it.deepCopy() }.toMutableList(), depot)
    }
    
    // 경로 간 고객 이동
    fun moveCustomer(fromRouteIdx: Int, fromCustomerIdx: Int, toRouteIdx: Int, toCustomerIdx: Int): Boolean {
        if (fromRouteIdx == toRouteIdx) return false
        
        val fromRoute = routes[fromRouteIdx]
        val toRoute = routes[toRouteIdx]
        
        // 옮길 고객
        val customer = fromRoute.customers[fromCustomerIdx]
        
        // 용량 체크
        if (!toRoute.canAdd(customer)) return false
        
        // 고객 이동
        fromRoute.customers.removeAt(fromCustomerIdx)
        fromRoute.currentDemand -= customer.demand
        
        if (toCustomerIdx <= toRoute.customers.size) {
            toRoute.customers.add(toCustomerIdx, customer)
        } else {
            toRoute.customers.add(customer)
        }
        toRoute.currentDemand += customer.demand
        
        // 빈 경로 제거
        if (fromRoute.customers.isEmpty()) {
            routes.removeAt(fromRouteIdx)
        }
        
        totalDistanceCache = null // 캐시 무효화
        return true
    }
    
    // 경로 내 두 고객 위치 교환
    fun swapCustomers(routeIdx: Int, customerIdx1: Int, customerIdx2: Int): Boolean {
        val route = routes[routeIdx]
        if (customerIdx1 >= route.customers.size || customerIdx2 >= route.customers.size) return false
        
        val temp = route.customers[customerIdx1]
        route.customers[customerIdx1] = route.customers[customerIdx2]
        route.customers[customerIdx2] = temp
        
        totalDistanceCache = null // 캐시 무효화
        return true
    }
    
    // 2-opt 연산: 경로 내 교차점 제거
    fun apply2Opt(routeIdx: Int, i: Int, j: Int): Boolean {
        val route = routes[routeIdx]
        if (i >= route.customers.size || j >= route.customers.size || i >= j) return false
        
        // i와 j 사이의 경로 세그먼트 반전
        var left = i
        var right = j
        while (left < right) {
            val temp = route.customers[left]
            route.customers[left] = route.customers[right]
            route.customers[right] = temp
            left++
            right--
        }
        
        totalDistanceCache = null // 캐시 무효화
        return true
    }
    
    // 3-opt 연산: 경로의 3개 지점에서 최적화
    fun apply3Opt(routeIdx: Int): Boolean {
        val route = routes[routeIdx]
        if (route.customers.size < 6) return false // 최소 6개 이상의 고객이 필요
        
        val bestGain = findBest3OptMove(routeIdx)
        if (bestGain.first <= 0) return false
        
        val (i, j, k) = bestGain.second
        apply3OptMove(routeIdx, i, j, k)
        
        totalDistanceCache = null // 캐시 무효화
        return true
    }
    
    // 최적의 3-opt 이동 찾기
    private fun findBest3OptMove(routeIdx: Int): Pair<Int, Triple<Int, Int, Int>> {
        val route = routes[routeIdx]
        val customers = route.customers
        val n = customers.size
        
        var bestGain = 0
        var bestMove = Triple(0, 0, 0)
        
        // 모든 가능한 3-opt 이동 탐색
        for (i in 0 until n - 4) {
            for (j in i + 2 until n - 2) {
                for (k in j + 2 until n) {
                    val a = customers[i]
                    val b = customers[i + 1]
                    val c = customers[j]
                    val d = customers[j + 1]
                    val e = customers[k]
                    val f = if (k + 1 < n) customers[k + 1] else customers[0]
                    
                    // 제거할 간선들
                    val removed = DistanceCache.distanceBetween(a, b) +
                                  DistanceCache.distanceBetween(c, d) +
                                  DistanceCache.distanceBetween(e, f)
                    
                    // 케이스 1: a-c b-e d-f
                    val case1 = DistanceCache.distanceBetween(a, c) +
                                DistanceCache.distanceBetween(b, e) +
                                DistanceCache.distanceBetween(d, f)
                    val gain1 = removed - case1
                    
                    // 케이스 2: a-d c-b e-f
                    val case2 = DistanceCache.distanceBetween(a, d) +
                                DistanceCache.distanceBetween(c, b) +
                                DistanceCache.distanceBetween(e, f)
                    val gain2 = removed - case2
                    
                    // 케이스 3: a-c b-d e-f
                    val case3 = DistanceCache.distanceBetween(a, c) +
                                DistanceCache.distanceBetween(b, d) +
                                DistanceCache.distanceBetween(e, f)
                    val gain3 = removed - case3
                    
                    // 케이스 4: a-e d-b c-f
                    val case4 = DistanceCache.distanceBetween(a, e) +
                                DistanceCache.distanceBetween(d, b) +
                                DistanceCache.distanceBetween(c, f)
                    val gain4 = removed - case4
                    
                    val maxGain = maxOf(gain1, gain2, gain3, gain4)
                    if (maxGain > bestGain) {
                        bestGain = maxGain
                        bestMove = Triple(i, j, k)
                    }
                }
            }
        }
        
        return Pair(bestGain, bestMove)
    }
    
    // 3-opt 이동 적용
    private fun apply3OptMove(routeIdx: Int, i: Int, j: Int, k: Int) {
        val route = routes[routeIdx]
        val newRoute = route.deepCopy()
        newRoute.customers.clear()
        
        // 적용 로직 (간소화된 버전)
        val segment1 = route.customers.subList(0, i + 1)
        val segment2 = route.customers.subList(i + 1, j + 1)
        val segment3 = route.customers.subList(j + 1, k + 1)
        val segment4 = route.customers.subList(k + 1, route.customers.size)
        
        newRoute.customers.addAll(segment1)
        newRoute.customers.addAll(segment3.reversed())
        newRoute.customers.addAll(segment2.reversed())
        newRoute.customers.addAll(segment4)
        
        route.customers.clear()
        route.customers.addAll(newRoute.customers)
    }
    
    override fun toString(): String {
        return routes.filter { it.customers.isNotEmpty() }
                    .joinToString(";") { it.toString() }
    }
}

fun main() {
    val input = Scanner(System.`in`)
    val n = input.nextInt() // The number of customers
    val c = input.nextInt() // The capacity of the vehicles
    
    val locations = mutableListOf<Location>()

    (0..n).forEach { _ ->
        val index = input.nextInt() // The index of the customer (0 is the depot)
        val x = input.nextInt() // The x coordinate of the customer
        val y = input.nextInt() // The y coordinate of the customer
        val demand = input.nextInt() // The demand
        locations.add(Location(index, x, y, demand))
    }

    // 창고와 고객 분리
    val depot = locations.find { it.index == 0 }!!
    val customers = locations.filter { it.index != 0 }
    
    // 최적화된 초기 솔루션 생성 (Clarke-Wright 절약 알고리즘)
    val initialRoutes = clarkeWrightSavings(depot, customers, c)
    val initialSolution = Solution(initialRoutes, depot)
    
    // 시간 제한 설정
    val timeLimit = if (n < 50) 5000L else if (n < 100) 7500L else 9500L
    
    // 시뮬레이티드 어닐링으로 솔루션 최적화
    val optimizedSolution = simulatedAnnealing(initialSolution, depot, timeLimit)
    
    // 결과 출력
    println(optimizedSolution.toString())
}

// Clarke-Wright 절약 알고리즘으로 초기 경로 생성
fun clarkeWrightSavings(depot: Location, customers: List<Location>, capacity: Int): MutableList<Route> {
    // 각 고객을 별도 경로로 초기화
    val routes = mutableListOf<Route>()
    val customerToRoute = HashMap<Location, Route>()
    
    for (customer in customers) {
        val route = Route(capacity)
        route.add(customer)
        routes.add(route)
        customerToRoute[customer] = route
    }
    
    // 절약값 계산 및 정렬
    val savings = mutableListOf<Saving>()
    for (i in customers.indices) {
        for (j in i + 1 until customers.size) {
            val customer1 = customers[i]
            val customer2 = customers[j]
            
            val saving = DistanceCache.distanceBetween(depot, customer1) +
                         DistanceCache.distanceBetween(depot, customer2) -
                         DistanceCache.distanceBetween(customer1, customer2)
            
            savings.add(Saving(customer1, customer2, saving))
        }
    }
    
    savings.sortByDescending { it.saving }
    
    // 경로 병합
    for ((i, j, _) in savings) {
        val routeI = customerToRoute[i]
        val routeJ = customerToRoute[j]
        
        if (routeI != routeJ && 
            routeI != null && 
            routeJ != null && 
            routeI.currentDemand + routeJ.currentDemand <= capacity) {
            
            // i가 routeI의 마지막 요소인지 확인
            val iIsLast = routeI.customers.last() == i
            // j가 routeJ의 첫 번째 요소인지 확인
            val jIsFirst = routeJ.customers.first() == j
            
            if (iIsLast && jIsFirst) {
                // i가 routeI의 마지막이고 j가 routeJ의 첫 번째면 병합
                for (customer in routeJ.customers) {
                    routeI.add(customer)
                    customerToRoute[customer] = routeI
                }
                routes.remove(routeJ)
            }
        }
    }
    
    return routes
}

// 시뮬레이티드 어닐링 알고리즘 (개선된 버전)
fun simulatedAnnealing(initialSolution: Solution, depot: Location, timeLimit: Long = 9000L): Solution {
    var currentSolution = initialSolution
    var bestSolution = currentSolution.deepCopy()
    var bestDistance = currentSolution.getTotalDistance()
    
    // 변수 설정
    var temperature = 100.0
    var alpha = 0.99  // 냉각률
    val startTime = System.currentTimeMillis()
    var improvement = 0
    var noImprovement = 0
    
    // 적응형 이웃 생성 확률
    var pMove = 0.4
    var pSwap = 0.3
    var p2Opt = 0.2
    var p3Opt = 0.1
    
    // 시간이 허용하는 한 계속 최적화
    while ((System.currentTimeMillis() - startTime) < timeLimit) {
        val iterations = minOf(100, (timeLimit - (System.currentTimeMillis() - startTime)) / 10)
        
        var iterationImprovement = 0
        for (i in 0 until iterations) {
            // 이웃 솔루션 생성
            val operationType = when {
                Random.nextDouble() < pMove -> 0  // 고객 이동
                Random.nextDouble() < pMove + pSwap -> 1  // 고객 교환
                Random.nextDouble() < pMove + pSwap + p2Opt -> 2  // 2-opt
                else -> 3  // 3-opt
            }
            
            val newSolution = generateNeighborSolution(currentSolution, operationType)
            
            // 비용 차이 계산
            val currentDistance = currentSolution.getTotalDistance()
            val newDistance = newSolution.getTotalDistance()
            val delta = newDistance - currentDistance
            
            // 메트로폴리스 기준으로 새 솔루션 수락 여부 결정
            if (delta < 0 || Random.nextDouble() < exp(-delta / temperature)) {
                currentSolution = newSolution
                
                // 개선 시 최적 솔루션 업데이트
                if (newDistance < bestDistance) {
                    bestSolution = newSolution.deepCopy()
                    bestDistance = newDistance
                    iterationImprovement++
                    improvement++
                    noImprovement = 0
                } else {
                    noImprovement++
                }
            } else {
                noImprovement++
            }
        }
        
        // 온도 감소
        temperature *= alpha
        
        // 온도가 너무 낮아지면 재가열
        if (temperature < 0.1) {
            temperature = 10.0
        }
        
        // 개선사항이 없으면 연산자 가중치 조정
        if (iterationImprovement == 0 && noImprovement > 1000) {
            // 3-opt 확률 증가
            p3Opt = minOf(0.4, p3Opt + 0.05)
            p2Opt = minOf(0.3, p2Opt + 0.02)
            pSwap = maxOf(0.15, pSwap - 0.02)
            pMove = maxOf(0.15, pMove - 0.05)
            
            // 학습률 조정
            alpha = maxOf(0.95, alpha - 0.005)
            
            noImprovement = 0
        }
    }
    
    // 최종 솔루션에 대해 경로 최적화 수행
    optimizeRoutes(bestSolution)
    
    return bestSolution
}

// 이웃 솔루션 생성 (지정된 연산자 사용)
fun generateNeighborSolution(currentSolution: Solution, operationType: Int): Solution {
    val newSolution = currentSolution.deepCopy()
    
    when (operationType) {
        0 -> { // 고객 이동
            if (newSolution.routes.size < 2) return newSolution
            
            val fromRouteIdx = Random.nextInt(newSolution.routes.size)
            val fromRoute = newSolution.routes[fromRouteIdx]
            
            if (fromRoute.customers.isEmpty()) return newSolution
            
            var toRouteIdx = Random.nextInt(newSolution.routes.size)
            // fromRouteIdx와 다른 toRouteIdx 선택
            var attempts = 0
            while (fromRouteIdx == toRouteIdx && attempts < 5) {
                toRouteIdx = Random.nextInt(newSolution.routes.size)
                attempts++
            }
            if (fromRouteIdx == toRouteIdx) return newSolution
            
            val fromCustomerIdx = Random.nextInt(fromRoute.customers.size)
            val toCustomerIdx = if (newSolution.routes[toRouteIdx].customers.isEmpty()) 0 
                               else Random.nextInt(newSolution.routes[toRouteIdx].customers.size + 1)
            
            newSolution.moveCustomer(fromRouteIdx, fromCustomerIdx, toRouteIdx, toCustomerIdx)
        }
        1 -> { // 고객 교환
            if (newSolution.routes.isEmpty()) return newSolution
            
            val routeIdx = Random.nextInt(newSolution.routes.size)
            val route = newSolution.routes[routeIdx]
            
            if (route.customers.size < 2) return newSolution
            
            val customerIdx1 = Random.nextInt(route.customers.size)
            var customerIdx2 = Random.nextInt(route.customers.size)
            // customerIdx1과 다른 customerIdx2 선택
            var attempts = 0
            while (customerIdx1 == customerIdx2 && attempts < 5) {
                customerIdx2 = Random.nextInt(route.customers.size)
                attempts++
            }
            if (customerIdx1 == customerIdx2) return newSolution
            
            newSolution.swapCustomers(routeIdx, customerIdx1, customerIdx2)
        }
        2 -> { // 2-opt
            if (newSolution.routes.isEmpty()) return newSolution
            
            val routeIdx = Random.nextInt(newSolution.routes.size)
            val route = newSolution.routes[routeIdx]
            
            if (route.customers.size < 3) return newSolution
            
            val i = Random.nextInt(route.customers.size - 1)
            val j = i + 1 + Random.nextInt(route.customers.size - i - 1)
            
            newSolution.apply2Opt(routeIdx, i, j)
        }
        3 -> { // 3-opt
            if (newSolution.routes.isEmpty()) return newSolution
            
            val routesWithEnoughCustomers = newSolution.routes.indices.filter { 
                newSolution.routes[it].customers.size >= 6 
            }
            
            if (routesWithEnoughCustomers.isEmpty()) return newSolution
            
            val routeIdx = routesWithEnoughCustomers.random()
            newSolution.apply3Opt(routeIdx)
        }
    }
    
    return newSolution
}

// 최종 경로 최적화
fun optimizeRoutes(solution: Solution) {
    for (routeIdx in solution.routes.indices) {
        // 각 경로에 2-opt 적용
        var improved = true
        while (improved) {
            improved = false
            val route = solution.routes[routeIdx]
            
            for (i in 0 until route.customers.size - 1) {
                for (j in i + 1 until route.customers.size) {
                    val before = route.getTotalDistance(solution.depot)
                    if (solution.apply2Opt(routeIdx, i, j)) {
                        val after = route.getTotalDistance(solution.depot)
                        if (after < before) {
                            improved = true
                            break
                        } else {
                            // 개선되지 않으면 되돌리기
                            solution.apply2Opt(routeIdx, i, j)
                        }
                    }
                }
                if (improved) break
            }
        }
    }
}
