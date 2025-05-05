import java.util.*

fun main() {
    val input = Scanner(System.`in`)

    while (true) {
        var m = 0
        var p = 0

        for (i in 0 until 8) {
            val h = input.nextInt()
            if (h > m) {
                m = h
                p = i
            }
        }

        println(p)
    }
}