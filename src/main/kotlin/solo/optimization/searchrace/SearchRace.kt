import java.util.*

/**
 * Made by Illedan
 **/
fun main(args : Array<String>) {
    val input = Scanner(System.`in`)
    val checkpoints = input.nextInt() // Count of checkpoints to read
    for (i in 0 until checkpoints) {
        val checkpointX = input.nextInt() // Position X
        val checkpointY = input.nextInt() // Position Y
    }

    // game loop
    while (true) {
        val checkpointIndex = input.nextInt() // Index of the checkpoint to lookup in the checkpoints input, initially 0
        val x = input.nextInt() // Position X
        val y = input.nextInt() // Position Y
        val vx = input.nextInt() // horizontal speed. Positive is right
        val vy = input.nextInt() // vertical speed. Positive is downwards
        val angle = input.nextInt() // facing angle of this car

        // Write an action using println()
        // To debug: System.err.println("Debug messages...");

        println("5000 5000 200 message") // X Y THRUST MESSAGE
    }
}