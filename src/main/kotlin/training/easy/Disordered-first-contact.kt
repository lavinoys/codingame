package training.easy


import java.util.Scanner
import kotlin.math.abs

/**
 *
 * Encoding
 */
fun main() {
    val input = Scanner(System.`in`)
    val n: Int = input.nextInt()

    var absN = abs(n.toDouble()).toInt()

    if (input.hasNextLine()) {
        input.nextLine()
    }
    val message: String = input.nextLine()
    var messageResult = message.toCharArray()

    val mappingList = createMappingList(message)

    while (absN > 0) {
        messageResult = if (n < 0) {
            convert(messageResult) { i: Int -> mappingList[i] }
        } else {
            convert(messageResult) { i: Int -> mappingList.indexOf(i) }
        }
        absN--
    }

    println(messageResult)

}

fun createMappingList(message: String): List<Int> {
    val res: MutableList<Int> = ArrayList()
    val mpCheck = MappingCheck()

    message.indices.forEach { i: Int ->
        if (mpCheck.add) {
            res.add(i)
        } else {
            res.add(mpCheck.index++, i)
        }
        mpCheck.switchSide(i)
    }
    return res
}

fun convert(
    message: CharArray,
    mappingFunc: (i: Int) -> Int
): CharArray = message.indices.map { i ->
    message[mappingFunc(i)]
}.toCharArray()

internal class MappingCheck(
    var add: Boolean = true,
    private var npCharToAdd: Int = 1,
    var index: Int = 0,
    private var nbSwitch: Int = 0
) {
    fun switchSide(i: Int) {
        if (i == nbSwitch) {
            add = !add
            nbSwitch += ++npCharToAdd
            index = 0
        }
    }
}
