import java.util.*
import java.io.*
import java.math.*
import kotlin.math.pow
import kotlin.math.round
import kotlin.math.sqrt
import kotlin.math.exp
import kotlin.random.Random

/**
 * Challenge yourself with this classic NP-Hard optimization problem !
 **/

// 고객 또는 창고를 나타내는 데이터 클래스
data class Location(val index: Int, val x: Int, val y: Int, val demand: Int) {
    fun distanceTo(other: Location): Int {
        return round(sqrt((x - other.x).toDouble().pow(2) + (y - other.y).toDouble().pow(2))).toInt()
    }
}

// 차량 경로를 나타내는 클래스
class Route(val capacity: Int) {
    val customers = mutableListOf<Location>()
    var currentDemand = 0

    fun canAdd(customer: Location): Boolean {
        return currentDemand + customer.demand <= capacity
    }

    fun add(customer: Location) {
        customers.add(customer)
        currentDemand += customer.demand
    }

    fun getTotalDistance(depot: Location): Int {
        if (customers.isEmpty()) return 0

        var distance = depot.distanceTo(customers.first())
        
        for (i in 0 until customers.size - 1) {
            distance += customers[i].distanceTo(customers[i + 1])
        }
        
        distance += customers.last().distanceTo(depot)
        return distance
    }
    
    // 경로의 깊은 복사본 생성
    fun deepCopy(): Route {
        val copy = Route(capacity)
        copy.customers.addAll(customers)
        copy.currentDemand = currentDemand
        return copy
    }

    override fun toString(): String {
        return customers.joinToString(" ") { it.index.toString() }
    }
}

// 전체 솔루션을 나타내는 클래스
class Solution(val routes: MutableList<Route>, val depot: Location) {
    fun getTotalDistance(): Int {
        return routes.sumOf { it.getTotalDistance(depot) }
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
        
        return true
    }
    
    // 경로 내 두 고객 위치 교환
    fun swapCustomers(routeIdx: Int, customerIdx1: Int, customerIdx2: Int): Boolean {
        val route = routes[routeIdx]
        if (customerIdx1 >= route.customers.size || customerIdx2 >= route.customers.size) return false
        
        val temp = route.customers[customerIdx1]
        route.customers[customerIdx1] = route.customers[customerIdx2]
        route.customers[customerIdx2] = temp
        
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
        
        return true
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

    // 경로 계획 수립 (탐욕 알고리즘으로 초기 솔루션 생성)
    val initialRoutes = generateInitialRoutes(depot, customers, c)
    val initialSolution = Solution(initialRoutes.toMutableList(), depot)
    
    // 시뮬레이티드 어닐링으로 솔루션 최적화
    val optimizedSolution = simulatedAnnealing(initialSolution, depot)
    
    // 결과 출력
    println(optimizedSolution.toString())
}

// 탐욕 알고리즘으로 초기 경로 생성
fun generateInitialRoutes(depot: Location, customers: List<Location>, capacity: Int): MutableList<Route> {
    // 가용한 모든 고객 목록
    val remainingCustomers = customers.toMutableList()
    // 경로 목록
    val routes = mutableListOf<Route>()
    
    // 모든 고객을 방문할 때까지 반복
    while (remainingCustomers.isNotEmpty()) {
        val currentRoute = Route(capacity)
        routes.add(currentRoute)
        
        // 현재 위치 (시작은 창고)
        var currentLocation = depot
        
        // 경로에 고객 추가
        while (remainingCustomers.isNotEmpty()) {
            // 현재 위치에서 가장 가까운 고객 찾기
            val nearestCustomer = findNearestCustomer(currentLocation, remainingCustomers, currentRoute)
                ?: break // 더 이상 추가할 수 있는 고객이 없으면 종료
            
            currentRoute.add(nearestCustomer)
            currentLocation = nearestCustomer
            remainingCustomers.remove(nearestCustomer)
        }
    }
    
    return routes
}

// 현재 위치에서 가장 가까운 고객 찾기
fun findNearestCustomer(currentLocation: Location, customers: List<Location>, route: Route): Location? {
    var nearest: Location? = null
    var minDistance = Int.MAX_VALUE
    
    for (customer in customers) {
        // 용량 제한 확인
        if (!route.canAdd(customer)) continue
        
        val distance = currentLocation.distanceTo(customer)
        if (distance < minDistance) {
            minDistance = distance
            nearest = customer
        }
    }
    
    return nearest
}

// 시뮬레이티드 어닐링 알고리즘
fun simulatedAnnealing(initialSolution: Solution, depot: Location): Solution {
    var currentSolution = initialSolution
    var bestSolution = currentSolution.deepCopy()
    var bestDistance = currentSolution.getTotalDistance()
    
    // 시뮬레이티드 어닐링 파라미터
    var temperature = 100.0
    val coolingRate = 0.99
    val iterations = 1000
    val minTemperature = 0.1
    
    // 시간 제한 체크를 위한 변수
    val startTime = System.currentTimeMillis()
    val timeLimit = 9000 // 9초 (10초 제한의 안전 마진)
    
    while (temperature > minTemperature && (System.currentTimeMillis() - startTime) < timeLimit) {
        for (i in 0 until iterations) {
            // 새로운 솔루션 생성
            val newSolution = generateNeighborSolution(currentSolution)
            
            // 비용 차이 계산
            val currentDistance = currentSolution.getTotalDistance()
            val newDistance = newSolution.getTotalDistance()
            val delta = newDistance - currentDistance
            
            // 메트로폴리스 기준으로 새 솔루션 수락 여부 결정
            if (delta < 0 || Random.nextDouble() < exp(-delta / temperature)) {
                currentSolution = newSolution
                
                // 최적 솔루션 업데이트
                if (newDistance < bestDistance) {
                    bestSolution = newSolution.deepCopy()
                    bestDistance = newDistance
                }
            }
            
            // 시간 제한 초과 시 종료
            if ((System.currentTimeMillis() - startTime) > timeLimit) break
        }
        
        // 온도 감소
        temperature *= coolingRate
    }
    
    return bestSolution
}

// 이웃 솔루션 생성
fun generateNeighborSolution(currentSolution: Solution): Solution {
    val newSolution = currentSolution.deepCopy()
    val operationType = Random.nextInt(3) // 0: 고객 이동, 1: 고객 교환, 2: 2-opt
    
    when (operationType) {
        0 -> { // 고객 이동
            if (newSolution.routes.size < 2) return newSolution
            
            val fromRouteIdx = Random.nextInt(newSolution.routes.size)
            val fromRoute = newSolution.routes[fromRouteIdx]
            
            if (fromRoute.customers.isEmpty()) return newSolution
            
            val toRouteIdx = Random.nextInt(newSolution.routes.size)
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
            while (customerIdx1 == customerIdx2) {
                customerIdx2 = Random.nextInt(route.customers.size)
            }
            
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
    }
    
    return newSolution
}
