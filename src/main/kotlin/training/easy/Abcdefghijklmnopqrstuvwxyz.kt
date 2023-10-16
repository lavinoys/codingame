//package training.easy


fun main() {
    val n = readLine()!!.toInt()
    val multiLineString = mutableListOf<String>()

    repeat(n) {
        multiLineString.add(readLine()!!)
    }

    val visited = Array(n) { BooleanArray(n) }
    val alphabet = "abcdefghijklmnopqrstuvwxyz"

    fun isValid(x: Int, y: Int) = x in 0 until n && y in 0 until n

    fun dfs(x: Int, y: Int, charIndex: Int): Boolean {
        if (charIndex == alphabet.length) {
            return true
        }

        if (!isValid(x, y) || visited[x][y] || multiLineString[x][y] != alphabet[charIndex]) {
            return false
        }

        visited[x][y] = true

        val dx = intArrayOf(1, -1, 0, 0)
        val dy = intArrayOf(0, 0, 1, -1)

        for (i in 0 until 4) {
            val newX = x + dx[i]
            val newY = y + dy[i]

            if (dfs(newX, newY, charIndex + 1)) {
                return true
            }
        }

        visited[x][y] = false
        return false
    }

    for (i in 0 until n) {
        for (j in 0 until n) {
            if (multiLineString[i][j] == 'a' && dfs(i, j, 0)) {
                for (x in 0 until n) {
                    for (y in 0 until n) {
                        if (!visited[x][y]) {
                            multiLineString[x] =
                                multiLineString[x].substring(0, y) + '-' + multiLineString[x].substring(y + 1)
                        }
                    }
                }
            }
        }
    }

    for (line in multiLineString) {
        println(line)
    }
}


