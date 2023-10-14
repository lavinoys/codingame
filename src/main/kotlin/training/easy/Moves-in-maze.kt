package training.easy

/**
 * Auto-generated code below aims at helping you parse
 * the standard input according to the problem statement.
 * Flood fill
 * BFS
 * Maze
 **/
fun main() {
    val (w, h) = readln().split(" ").map { it.toInt() }
    val map = mutableListOf<String>()
    var x0 = 0
    var y0 = 0

    repeat(h) {
        val line = readln()
        map.add(line)
        if ('S' in line) {
            x0 = map.size - 1
            y0 = line.indexOf('S')
        }
    }

    val stk = mutableListOf(mutableListOf(x0, y0, 0))
    val fill = ('0'..'9').toList() + ('A'..'Z').toList()
    map[x0] = map[x0].replaceRange(y0, y0 + 1, fill[0].toString())
    val dir = listOf(Pair(1, 0), Pair(-1, 0), Pair(0, -1), Pair(0, 1))

    while (stk.isNotEmpty()) {
        val pos = stk.removeAt(stk.size - 1)
        for (i in dir) {
            var x = pos[0] + i.first
            var y = pos[1] + i.second
            val idx = pos[2]

            if (x == h) x = 0
            if (x == -1) x = h - 1
            if (y == w) y = 0
            if (y == -1) y = w - 1

            if (map[x][y] == '.') {
                map[x] = map[x].replaceRange(y, y + 1, fill[idx + 1].toString())
                stk.add(mutableListOf(x, y, idx + 1))
            }
        }
        stk.sortByDescending { it[2] }
    }

    map.forEach { println(it) }
}