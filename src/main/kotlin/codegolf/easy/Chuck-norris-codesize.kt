package codegolf.easy

import java.util.Scanner

/**
 * Auto-generated code below aims at helping you parse
 * the standard input according to the problem statement.
 **/
fun main(args : Array<String>) {
    // Write an answer using println()
    // To debug: System.err.println("Debug messages...");
    val input = Scanner(System.`in`)
    val message = input.nextLine()
    var b = ""

    message.indices.forEach { i ->
        var m = 1
        var a = ""

        (0 until 7).forEach { _ ->
            a = if ((m and message[i].code) >= 1) {
                "1$a"
            } else {
                "0$a"
            }
            m = m shl 1
        }

        b += a
    }

    var a = ""
    var l = ""

    b.indices.forEach { j ->
        val afterMessage = b[j].toString()

        if (afterMessage != l) {
            if (j > 0) {
                a += " "
            }

            if (afterMessage == "0") {
                a += "0"
            }

            a += "0 "
        }

        a += "0"
        l = afterMessage
    }

    println(a)
}