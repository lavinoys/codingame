package training.easy

/**
 * BFS
 * DFS
 * Arrays
 * */
fun main() {
    val input = Array(readln().toInt()) { readln().toCharArray().toTypedArray() }
//        .onEach {
//            System.err.println(it.joinToString(""))
//        }

    val starts = input.flatMapIndexed { y, line ->
        line.mapIndexedNotNull { x, c ->
            if (c == 'a') y to x else null
        }
    }
//        .onEach {
//            System.err.println("${it.first} ${it.second}")
//        }

    for ((y, x) in starts) {
        val (success, chain) = input.dfs('a', y, x)
//        System.err.println("$success ${chain.joinToString(",")}")
        if (success) return input.solve(chain)
    }
}

fun Array<Array<Char>>.dfs(c: Char, y: Int, x: Int): Pair<Boolean, List<Pair<Int, Int>>> {
    if (c == 'z') return true to listOf(y to x)
    listOf(0 to 1, 0 to -1, 1 to 0, -1 to 0).forEach { (dy, dx) ->
        if (y + dy in indices && x + dx in indices && this[y + dy][x + dx] == c + 1) {
            val res = dfs(c + 1, y + dy, x + dx)
            if (res.first) return@dfs true to listOf(y to x) + res.second
        }
    }
    this[y][x] = '-'
    return false to listOf()
}

fun Array<Array<Char>>.solve(chain: List<Pair<Int, Int>>) {
    indices.forEach { a ->
        indices.forEach { b ->
            if (a to b !in chain) this[a][b] = '-'
        }
    }
    println(joinToString("\n") { it.joinToString("") })
}
