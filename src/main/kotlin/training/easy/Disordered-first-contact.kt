fun main() {
    val n = readLine()!!.toInt()
    val message = readLine()!!

    val result = if (n > 0) {
        decodeMessage(message)
    } else {
        encodeMessage(message)
    }

    println(result)
}

fun encodeMessage(message: String): String {
    val result = StringBuilder()
    for (i in 0 until message.length) {
        val part = message.substring(i)
        if (i % 2 == 0) {
            result.insert(0, part)
        } else {
            result.append(part)
        }
    }
    return result.toString()
}

fun decodeMessage(message: String): String {
    val mid = message.length / 2
    val part1 = message.substring(mid)
    val part2 = message.substring(0, mid)
    val result = StringBuilder()
    for (i in 0 until mid) {
        result.append(part2[i])
        result.append(part1[i])
    }
    if (message.length % 2 == 1) {
        result.append(message[mid])
    }
    return result.toString()
}
