import java.util.*

/**
 * Auto-generated code below aims at helping you parse
 * the standard input according to the problem statement.
 **/
fun main(args : Array<String>) {
//    val input = Scanner(System.`in`)
//    val N = input.nextInt()
//    for (i in 0 until N) {
//        val telephone = input.next()
//    }
//
//    // Write an answer using println()
//    // To debug: System.err.println("Debug messages...");
//
//
//    // The number of elements (referencing a number) stored in the structure.
//    println("number")

    val d = mutableMapOf<Char, Any>()
    var r = 0

    val input = readLine() ?: ""
    val lines = input.split("\n").drop(1)

    for (line in lines) {
        var u: MutableMap<Char, Any> = d
        for (char in line.trim()) {
            r += if (u.get(char) == 1) 1 else 0
            u = u.getOrPut(char, { mutableMapOf<Char, Any>() }) as MutableMap<Char, Any>
        }
    }

    println(r)
}