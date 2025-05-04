import java.util.*
import kotlin.math.*

/**
 * Challenge yourself with this classic NP-Hard optimization problem !
 **/

// 위치와 수요를 저장하는 클래스
data class Customer(
    val index: Int,
    val x: Int,
    val y: Int,
    val demand: Int
) {
    // 유클리드 거리 계산
    fun distanceTo(other: Customer): Int {
        val dx = x - other.x
        val dy = y - other.y
        return sqrt(dx.toDouble().pow(2) + dy.toDouble().pow(2)).roundToInt()
    }
}

// 경로를 저장하는 클래스
class Route(
    var customers: MutableList<Int>,
    val capacity: Int,
    var totalDemand: Int = 0,
    var totalDistance: Int = 0
)

// k-d 트리 노드 클래스
class KDNode(
    val customer: Customer,
    val axis: Int,
    var left: KDNode? = null,
    var right: KDNode? = null
)

// 저장(savings) 쌍 클래스
data class SavingsPair(
    val i: Int,
    val j: Int,
    val saving: Double
)

// VRP 문제 해결 클래스
class VRPSolver(private val customers: List<Customer>, private val vehicleCapacity: Int) {
    
    // k-d 트리 생성 함수
    private fun createKDTree(points: List<Customer>, depth: Int = 0): KDNode? {
        if (points.isEmpty()) return null
        
        val axis = depth % 2 // 2차원이므로 0 또는 1
        
        // axis에 따라 정렬
        val sortedPoints = when (axis) {
            0 -> points.sortedBy { it.x }
            else -> points.sortedBy { it.y }
        }
        
        val median = sortedPoints.size / 2
        val node = KDNode(
            customer = sortedPoints[median],
            axis = axis
        )
        
        // 재귀적으로 좌우 자식 생성
        node.left = createKDTree(sortedPoints.subList(0, median), depth + 1)
        node.right = if (median + 1 < sortedPoints.size) {
            createKDTree(sortedPoints.subList(median + 1, sortedPoints.size), depth + 1)
        } else null
        
        return node
    }
    
    // Clarke-Wright 저장(savings) 알고리즘 구현
    private fun clarkeWrightSavings(): List<Route> {
        val depot = customers[0]
        val nonDepotCustomers = customers.subList(1, customers.size)
        
        // 초기 경로 생성 (각 고객을 개별 경로로)
        val routes = nonDepotCustomers.map { customer ->
            Route(
                customers = mutableListOf(customer.index),
                capacity = vehicleCapacity,
                totalDemand = customer.demand,
                totalDistance = 2 * depot.distanceTo(customer) // 왕복 거리
            )
        }.toMutableList()
        
        // 모든 가능한 경로 쌍에 대한 저장(savings) 계산
        val savings = mutableListOf<SavingsPair>()
        
        for (i in 1 until customers.size) {
            for (j in i + 1 until customers.size) {
                val saving = depot.distanceTo(customers[i]) + 
                           depot.distanceTo(customers[j]) - 
                           customers[i].distanceTo(customers[j])
                           
                savings.add(SavingsPair(i, j, saving.toDouble()))
            }
        }
        
        // 저장(savings)을 내림차순으로 정렬
        savings.sortByDescending { it.saving }
        
        // 경로 병합
        val merged = BooleanArray(routes.size) { false }
        val routeEnd = Array(routes.size) { BooleanArray(2) { true } }
        val routeIndex = IntArray(customers.size) { if (it > 0) it - 1 else -1 }
        
        for (savingPair in savings) {
            val cust1 = savingPair.i
            val cust2 = savingPair.j
            val route1 = routeIndex[cust1]
            val route2 = routeIndex[cust2]
            
            // 이미 같은 경로에 있거나 병합된 경로인 경우 건너뛰기
            if (route1 == route2 || merged[route1] || merged[route2]) continue
            
            // 경로 끝에 위치한 고객인지 확인
            val isCust1End = (routes[route1].customers[0] == cust1 && routeEnd[route1][0]) ||
                           (routes[route1].customers.last() == cust1 && routeEnd[route1][1])
            val isCust2End = (routes[route2].customers[0] == cust2 && routeEnd[route2][0]) ||
                           (routes[route2].customers.last() == cust2 && routeEnd[route2][1])
            
            if (!isCust1End || !isCust2End) continue
            
            // 용량 제한 확인
            if (routes[route1].totalDemand + routes[route2].totalDemand > vehicleCapacity) continue
            
            // 두 경로 병합
            val newCustomers = mutableListOf<Int>()
            
            // 첫 번째 경로 복사 (필요시 반전)
            if (routes[route1].customers.last() == cust1) {
                newCustomers.addAll(routes[route1].customers)
            } else {
                newCustomers.addAll(routes[route1].customers.reversed())
            }
            
            // 두 번째 경로 복사 (필요시 반전)
            if (routes[route2].customers[0] == cust2) {
                newCustomers.addAll(routes[route2].customers)
            } else {
                newCustomers.addAll(routes[route2].customers.reversed())
            }
            
            // 두 번째 경로를 병합됨으로 표시
            merged[route2] = true
            
            // 첫 번째 경로 업데이트
            routes[route1].customers = newCustomers
            routes[route1].totalDemand += routes[route2].totalDemand
            
            // 모든 고객의 경로 인덱스 업데이트
            for (custIdx in newCustomers) {
                routeIndex[custIdx] = route1
            }
            
            // 경로 끝 업데이트
            routeEnd[route1][0] = false
            routeEnd[route1][1] = false
            if (routes[route1].customers[0] != 0) routeEnd[route1][0] = true
            if (routes[route1].customers.last() != 0) routeEnd[route1][1] = true
        }
        
        // 결과 경로 정리 및 거리 계산
        val resultRoutes = routes.filterIndexed { index, _ -> !merged[index] }
        
        for (route in resultRoutes) {
            // 총 거리 계산
            route.totalDistance = 0
            var prev = 0 // 창고
            
            for (custIdx in route.customers) {
                val curr = custIdx
                route.totalDistance += customers[prev].distanceTo(customers[curr])
                prev = curr
            }
            route.totalDistance += customers[prev].distanceTo(customers[0]) // 창고로 돌아오기
        }
        
        return resultRoutes
    }
    
    // 2-opt 개선 방법 구현
    private fun improveRouteWith2Opt(route: Route) {
        var improvement = true
        val n = route.customers.size
        
        while (improvement) {
            improvement = false
            
            for (i in 0 until n - 1) {
                for (j in i + 1 until n) {
                    val custI = route.customers[i]
                    val custINext = route.customers[(i + 1) % n]
                    val custJ = route.customers[j]
                    val custJNext = route.customers[(j + 1) % n]
                    
                    // 현재 경로 거리
                    val currentDistance = 
                        customers[custI].distanceTo(customers[custINext]) +
                        customers[custJ].distanceTo(customers[custJNext])
                    
                    // 새 연결 거리
                    val newDistance = 
                        customers[custI].distanceTo(customers[custJ]) +
                        customers[custINext].distanceTo(customers[custJNext])
                    
                    // 거리가 개선되면 경로 변경
                    if (newDistance < currentDistance) {
                        // i+1과 j 사이의 경로를 뒤집음
                        val routeList = route.customers.toMutableList()
                        val subRoute = routeList.subList(i + 1, j + 1)
                        subRoute.reverse()
                        route.customers = routeList
                        
                        // 총 거리 재계산
                        route.totalDistance = 0
                        var prev = 0 // 창고
                        for (k in route.customers.indices) {
                            val curr = route.customers[k]
                            route.totalDistance += customers[prev].distanceTo(customers[curr])
                            prev = curr
                        }
                        route.totalDistance += customers[prev].distanceTo(customers[0]) // 창고로 돌아오기
                        
                        improvement = true
                        break
                    }
                }
                if (improvement) break
            }
        }
    }
    
    // 모든 경로에 2-opt 개선 적용
    private fun improveRoutesWith2Opt(routes: List<Route>) {
        for (route in routes) {
            improveRouteWith2Opt(route)
        }
    }
    
    // VRP 문제 해결 및 결과 반환
    fun solve(): String {
        // k-d 트리 생성 (필요시 사용)
        val kdtree = createKDTree(customers.subList(1, customers.size))
        
        // Clarke-Wright 저장 알고리즘으로 초기 경로 생성
        val routes = clarkeWrightSavings()
        
        // 2-opt 개선 방법으로 경로 최적화
        improveRoutesWith2Opt(routes)
        
        // 결과 출력 형식으로 변환
        return routes.joinToString(";") { route ->
            route.customers.joinToString(" ")
        }
    }
}

fun main(args : Array<String>) {
    val input = Scanner(System.`in`)
    val n = input.nextInt() // The number of customers
    val c = input.nextInt() // The capacity of the vehicles
    
    // 고객 데이터 저장
    val customers = mutableListOf<Customer>()
    
    for (i in 0 until n) {
        val index = input.nextInt() // The index of the customer (0 is the depot)
        val x = input.nextInt() // The x coordinate of the customer
        val y = input.nextInt() // The y coordinate of the customer
        val demand = input.nextInt() // The demand
        
        customers.add(Customer(index, x, y, demand))
    }
    
    // VRP 문제 해결
    val vrpSolver = VRPSolver(customers, c)
    val result = vrpSolver.solve()
    
    // 결과 출력
    println(result)
}
