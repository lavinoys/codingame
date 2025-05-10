import java.util.*
import java.io.*
import java.math.*

/**
 * Auto-generated code below aims at helping you parse
 * the standard input according to the problem statement.
 **/
fun main(args : Array<String>) {
    val input = Scanner(System.`in`)
    val laps = input.nextInt()
    val checkpointCount = input.nextInt()
    for (i in 0 until checkpointCount) {
        val checkpointX = input.nextInt()
        val checkpointY = input.nextInt()
    }

    // game loop
    while (true) {
        for (i in 0 until 2) {
            val x = input.nextInt() // x position of your pod
            val y = input.nextInt() // y position of your pod
            val vx = input.nextInt() // x speed of your pod
            val vy = input.nextInt() // y speed of your pod
            val angle = input.nextInt() // angle of your pod
            val nextCheckPointId = input.nextInt() // next check point id of your pod
        }
        for (i in 0 until 2) {
            val x2 = input.nextInt() // x position of the opponent's pod
            val y2 = input.nextInt() // y position of the opponent's pod
            val vx2 = input.nextInt() // x speed of the opponent's pod
            val vy2 = input.nextInt() // y speed of the opponent's pod
            val angle2 = input.nextInt() // angle of the opponent's pod
            val nextCheckPointId2 = input.nextInt() // next check point id of the opponent's pod
        }

        // Write an action using println()
        // To debug: System.err.println("Debug messages...");


        // You have to output the target position
        // followed by the power (0 <= thrust <= 100)
        // i.e.: "x y thrust"
        println("8000 4500 100")
        println("8000 4500 100")
    }
}