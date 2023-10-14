import kotlin.properties.Delegates

data class Bike(var x: Int, var y: Int, var speed: Int)

fun copy(bikes: List<Bike>): List<Bike> {
    return bikes.map { Bike(it.x, it.y, it.speed) }
}

fun findPath(original: List<Bike>, V: Int): List<Bike> {
    val results = mutableListOf<Bike>()
    val applyMove = { bikes: List<Bike>, move: String ->
        bikes.map { bike ->
            val next = bike.x + bike.speed
            var nextLane = bike.y
            var checked = roads[bike.y].slice(bike.x until next)

            if (move == "UP" || move == "DOWN") {
                if (move == "UP" && bike.y == 0 || move == "DOWN" && bike.y == 3) {
                    checked = emptyList()
                } else {
                    nextLane = bike.y + if (move == "UP") -1 else 1
                    checked += roads[nextLane].slice(bike.x until next)
                }
            }
            if (checked.any { !it } && move != "JUMP" || next < roadLength && !roads[nextLane][next]) {
                return@map null
            }
            bike.x = next
            bike.y = nextLane

            bike
        }.filterNotNull()
    }

    for (move in listOf("SPEED", "WAIT", "JUMP", "DOWN", "UP", "SLOW")) {
        var bikes = copy(original)

        if (move == "SPEED") {
            bikes.forEach { it.speed++ }
        } else if (move == "SLOW") {
            bikes.forEach { it.speed-- }
            if (bikes[0].speed == 0) {
                bikes = emptyList()
            }
        }

        bikes = applyMove(bikes, move)
        if (bikes.size >= V) {
            results.addAll(bikes)
        }
    }

    return results.toList()
}

fun backtrack(bikes: List<Bike>, V: Int): List<String> {
    val stack = mutableListOf(bikes)
    val moves = mutableListOf<String>()

    while (stack.isNotEmpty()) {
        val stackFirst = stack.first()
        if (stackFirst.size >= V) {
            return moves.reversed()
        } else {
            val next = findPath(bikes, V)
            if (next.isNotEmpty()) {
                stack.add(0, next)
                moves.add(0, next[0].toString())
            } else {
                stack.removeAt(0)
                moves.removeAt(0)
            }
        }
    }
    return emptyList()
}


var M by Delegates.notNull<Int>()
var V by Delegates.notNull<Int>()
var roads by Delegates.notNull<Array<List<Boolean>>>()
var roadLength by Delegates.notNull<Int>()

/**
 * Auto-generated code below aims at helping you parse
 * the standard input according to the problem statement.
 * Backtracking
 * DFS
 **/
fun main(args : Array<String>) {
    // M - the amount of motorbikes to control
    M = readln().toInt()
    // V -  the minimum amount of motorbikes that must survive
    V = readln().toInt()
    roads = Array(4) { readln().toCharArray().map { it == '.' } }
    roadLength = roads.first().size

    val speed = readln().toInt()
    val bikes = mutableListOf<Bike>()

    (0 .. M).forEach { _ ->
        val (x, y, active) = readln().split(" ").map { it.toInt() }
        if (active == 1) {
            bikes.add(Bike(x, y, speed))
        }
    }

    var move: List<String> = emptyList()
    var expected: Int = bikes.size
    while (move.isEmpty() && expected >= V) {
        move = backtrack(copy(bikes), V)
        expected--
    }

    println(move.lastOrNull() ?: "WAIT")
}