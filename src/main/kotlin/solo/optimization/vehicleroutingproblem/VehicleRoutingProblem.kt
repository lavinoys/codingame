import java.util.*
import java.io.*
import java.math.*
import kotlin.math.pow
import kotlin.math.round
import kotlin.math.sqrt

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

    override fun toString(): String {
        return customers.joinToString(" ") { it.index.toString() }
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

    // 경로 계획 수립 (탐욕 알고리즘)
    val routes = planRoutes(depot, customers, c)

    // 결과 출력
    println(routes.joinToString(";"))
}

// 탐욕 알고리즘으로 경로 계획 수립
fun planRoutes(depot: Location, customers: List<Location>, capacity: Int): List<String> {
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
    
    // 최적화: 빈 경로 제거 및 포맷팅
    return routes.filter { it.customers.isNotEmpty() }
                .map { it.toString() }
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
