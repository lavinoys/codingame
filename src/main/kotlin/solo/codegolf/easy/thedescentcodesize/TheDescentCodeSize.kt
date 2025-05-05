import java.util.*
import java.io.*
import java.math.*

/**
 * The while loop represents the game.
 * Each iteration represents a turn of the game
 * where you are given inputs (the heights of the mountains)
 * and where you have to print an output (the index of the mountain to fire on)
 * The inputs you are given are automatically updated according to your last actions.
 **/
fun main(args : Array<String>) {
    val input = Scanner(System.`in`)

    // game loop
    while (true) {
        for (i in 0 until 8) {
            val mountainH = input.nextInt() // represents the height of one mountain.
        }

        // Write an action using println()
        // To debug: System.err.println("Debug messages...");

        println("4") // The index of the mountain to fire on.
    }
}