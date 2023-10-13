package codegolf.medium

/**
 * Auto-generated code below aims at helping you parse
 * the standard input according to the problem statement.
 **/
fun main() {
    val n = readln().toInt()
    val t = List(n) { readln() }.sorted()
    var a = 0


    for (i in 0 until n - 1) {
        var c = 0
        val l = t[i].length

        for (j in 0 until l) {
            if (t[i][j] == t[i + 1][j]) {
                c++
            } else {
                break
            }
        }

        a += l - c
    }

    a += t[n - 1].length
    println(a)
}