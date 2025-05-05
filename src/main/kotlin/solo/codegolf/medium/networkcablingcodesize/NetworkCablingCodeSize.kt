import java.util.*
import kotlin.math.abs

fun main() {
    val input = Scanner(System.`in`)
    val n = input.nextInt()

    val x = LongArray(n)
    val y = LongArray(n)
    var minX = Long.MAX_VALUE
    var maxX = Long.MIN_VALUE

    // 건물 위치 입력받기
    for (i in 0 until n) {
        x[i] = input.nextLong()
        y[i] = input.nextLong()

        if (x[i] < minX) minX = x[i]
        if (x[i] > maxX) maxX = x[i]
    }

    // 메인 케이블 길이 계산
    val mainCableLength = maxX - minX

    // y좌표 정렬하여 중앙값 찾기
    y.sort()
    val medianY = if (n % 2 == 1) y[n / 2] else y[n / 2 - 1]

    // 각 건물에서 메인 케이블까지의 거리 계산
    var verticalLength = 0L
    for (i in 0 until n) {
        verticalLength += abs(y[i] - medianY)
    }

    // 총 케이블 길이 출력
    println(mainCableLength + verticalLength)
}