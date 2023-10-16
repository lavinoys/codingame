import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sqrt

private fun readInt(): Int = readln().toInt()
private fun readString(): String = readln()
private fun readStrings(): List<String> = readln().trim().split(" ")

private data class Stop(val index: Int, val id: String, val name: String, val lat: Double, val lon: Double)

private infix fun Stop.distanceTo(w: Stop): Double {
    val x = (w.lon - lon) * cos((lat + w.lat) / 2)
    val y = w.lat - lat
    return sqrt(x * x + y * y) * 6371
}

private typealias Route = Triple<Int, Int, Double>

private fun shortestPath(
    v: Int,
    edges: Map<Int, List<Route>>,
    from: Int,
    to: Int,
    distances: Array<Double>
): Pair<List<Int>, Double>? {
    if (from == to) return Pair(listOf(to), 0.0)

    val myEdges = edges[from]?.map { Pair(it.second, it.third) } ?: return null

    var result: Pair<List<Int>, Double>? = null

    for ((w, d) in myEdges) {
        if (distances[from] + d >= distances[w]) continue
        distances[w] = distances[from] + d

        val tailResult = shortestPath(v, edges, w, to, distances) ?: continue
        val (tail, tailDist) = tailResult

        result = result ?: Pair(listOf(from) + tail, d + tailDist)

        result  = minOf(
            result,
            Pair(listOf(from) + tail,
                d + tailDist
            )
        ) { a: Pair<List<Int>, Double>, b: Pair<List<Int>, Double> ->
            compareValues(a.second, b.second)
        }
    }

    return result
}

/**
 * Pathfinding
 * Graphs
 * Distances
 * Trigonometry
 */
fun main() {
    val startPoint = readString()
    val endPoint = readString()
    val n = readInt()
    val stops = Array(n) {
        val (id, name, _, lat, lon) = readln().split(",")
        Stop(it, id, name.trim('"'), lat.toDouble() * PI / 180, lon.toDouble() * PI / 180)
    }

    val lookup = stops.mapIndexed { index, stop -> Pair(stop.id, index) }.toMap()

    val m = readInt()
    val routes = Array(m) {
        val (from, to) = readStrings()
        val v = stops[lookup[from]!!]
        val w = stops[lookup[to]!!]
        Route(v.index, w.index, v distanceTo w)
    }.groupBy { it.first }.mapValues { entry -> entry.value.sortedBy { route -> route.third } }

    val path = shortestPath(n,
        routes,
        lookup[startPoint]!!,
        lookup[endPoint]!!,
        Array(n) { if (it == lookup[startPoint]) 0.0 else Double.POSITIVE_INFINITY }
    )

    println(path?.first?.joinToString("\n", transform = { stops[it].name }) ?: "IMPOSSIBLE")
}
