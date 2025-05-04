import java.util.*
import java.io.*
import java.math.*

/**
 * Auto-generated code below aims at helping you parse
 * the standard input according to the problem statement.
 **/
fun main(args : Array<String>) {
    val input = Scanner(System.`in`)

    // game loop
    while (true) {
        val resources = input.nextInt()
        val numTravelRoutes = input.nextInt()
        for (i in 0 until numTravelRoutes) {
            val buildingId1 = input.nextInt()
            val buildingId2 = input.nextInt()
            val capacity = input.nextInt()
        }
        val numPods = input.nextInt()
        if (input.hasNextLine()) {
            input.nextLine()
        }
        for (i in 0 until numPods) {
            val podProperties = input.nextLine()
        }
        val numNewBuildings = input.nextInt()
        if (input.hasNextLine()) {
            input.nextLine()
        }
        for (i in 0 until numNewBuildings) {
            val buildingProperties = input.nextLine()
        }

        // Write an action using println()
        // To debug: System.err.println("Debug messages...");

        println("TUBE 0 1;TUBE 0 2;POD 42 0 1 0 2 0 1 0 2") // TUBE | UPGRADE | TELEPORT | POD | DESTROY | WAIT
    }
}