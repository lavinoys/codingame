import java.util.*

fun main() {
    val input = Scanner(System.`in`)
    val W = input.nextInt() // 건물의 너비
    val H = input.nextInt() // 건물의 높이
    val N = input.nextInt() // 최대 턴 수
    val X0 = input.nextInt() // 시작 X 좌표
    val Y0 = input.nextInt() // 시작 Y 좌표

    // 검색 범위 초기화
    var xMin = 0
    var xMax = W - 1
    var yMin = 0
    var yMax = H - 1
    var x = X0
    var y = Y0

    while (true) {
        val bombDir = input.next() // 폭탄 방향(U, UR, R, DR, D, DL, L, UL)

        // 방향에 따라 검색 범위 업데이트
        if (bombDir.contains('U')) {
            yMax = y - 1
        } else if (bombDir.contains('D')) {
            yMin = y + 1
        }

        if (bombDir.contains('L')) {
            xMax = x - 1
        } else if (bombDir.contains('R')) {
            xMin = x + 1
        }

        // 이진 탐색으로 다음 위치 계산
        x = xMin + (xMax - xMin) / 2
        y = yMin + (yMax - yMin) / 2

        println("$x $y")
    }
}