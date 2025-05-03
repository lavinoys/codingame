import java.util.*
import java.io.*
import java.math.*

/**
 * Challenge yourself with this classic NP-Hard optimization problem !
 **/
fun main(args : Array<String>) {
    val input = Scanner(System.`in`)
    val n = input.nextInt() // The number of customers
    val c = input.nextInt() // The capacity of the vehicles
    for (i in 0 until n) {
        val index = input.nextInt() // The index of the customer (0 is the depot)
        val x = input.nextInt() // The x coordinate of the customer
        val y = input.nextInt() // The y coordinate of the customer
        val demand = input.nextInt() // The demand
    }

    // Write an action using println()
    // To debug: System.err.println("Debug messages...");


    // A single line containing the tours separated by a semicolon (;)
    // Each tour must be the indices of the customers separated by a space ( )
    // The depot (0) should not be included in the output
    println("1 2 3;4")
}