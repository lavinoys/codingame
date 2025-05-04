import java.util.*
import java.io.*
import java.math.*

/**
 * Auto-generated code below aims at helping you parse
 * the standard input according to the problem statement.
 **/
fun main(args : Array<String>) {
    val input = Scanner(System.`in`)

    // Write an action using println()
    // To debug: System.err.println("Debug messages...");

    println("first_level")

    // game loop
    while (true) {
        val width = input.nextInt()
        val height = input.nextInt()
        for (i in 0 until height) {
            for (j in 0 until width) {
                val cell = input.nextInt()
            }
        }

        // Write an action using println()
        // To debug: System.err.println("Debug messages...");

        println("7 4 L +")
        println("3 0 D -")
        println("6 4 L -")
    }
}