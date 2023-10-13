package training.medium

import java.util.Scanner

/**
 * Don't let the machines win. You are humanity's last hope...
 **/
fun main(args : Array<String>) {
    val input = Scanner(System.`in`)
    val width = input.nextInt() // the number of cells on the X axis
    val height = input.nextInt() // the number of cells on the Y axis
    if (input.hasNextLine()) {
        input.nextLine()
    }
    val grid = mutableListOf<String>()
    for (i in 0 until height) {
        grid.add(input.nextLine()) // width characters, each either 0 or .
    }

    // Loop over each cell to find power nodes and their neighbors
    for (y in 0 until height) {
        for (x in 0 until width) {
            if (grid[y][x] == '0') { // Found a power node
                var x2 = -1
                var y2 = -1
                var x3 = -1
                var y3 = -1

                // Find the next node to the right
                for (i in x+1 until width) {
                    if (grid[y][i] == '0') {
                        x2 = i
                        y2 = y
                        break
                    }
                }

                // Find the next node to the bottom
                for (j in y+1 until height) {
                    if (grid[j][x] == '0') {
                        x3 = x
                        y3 = j
                        break
                    }
                }

                // Print the coordinates of the node and its neighbors
                println("$x $y $x2 $y2 $x3 $y3")
            }
        }
    }
}
