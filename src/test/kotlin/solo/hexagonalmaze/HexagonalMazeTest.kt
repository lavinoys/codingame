package solo.hexagonalmaze

import java.io.ByteArrayOutputStream
import java.io.PrintStream
import java.util.Scanner
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.assertEquals
import java.io.ByteArrayInputStream
import solo.hexagonalmaze.solveMaze

/**
 * 간단한 테스트 함수
 * HexagonalMaze.kt의 solveMaze 함수를 직접 호출하여 테스트
 */
class HexagonalMazeTest {
    @Test
    fun testSolveMaze() {
        // 예제 입력
        val input = """
            5 6
            #####
            #S#E#
            #_#_#
            #_#_#
            #___#
            #####
        """.trimIndent()

        // 표준 출력 캡처
        val originalOut = System.out
        val outputStream = ByteArrayOutputStream()
        System.setOut(PrintStream(outputStream))

        // solveMaze 함수 직접 호출
        val scanner = Scanner(input)
        solveMaze(scanner)

        // 표준 출력 복원
        System.setOut(originalOut)

        // 출력 확인
        val output = outputStream.toString().trim().replace("\r\n", "\n")
        val expectedOutput = """
            #####
            #S#E#
            #.#.#
            #.#.#
            #_..#
            #####
        """.trimIndent().replace("\r\n", "\n")

        // 결과 검증
        assertEquals(expectedOutput, output, "미로 경로가 예상과 다릅니다")
    }
}
