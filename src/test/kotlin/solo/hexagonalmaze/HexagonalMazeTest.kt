package solo.hexagonalmaze

import java.io.ByteArrayOutputStream
import java.io.PrintStream
import java.util.Scanner
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import java.io.ByteArrayInputStream
import solo.hexagonalmaze.solveMaze
import solo.hexagonalmaze.main

/**
 * HexagonalMaze 테스트 클래스
 * - solveMaze 함수 직접 호출 테스트
 * - main 함수 테스트
 */
class HexagonalMazeTest {

    private lateinit var testInputContent: String

    @BeforeEach
    fun setup() {
        // 테스트 입력 설정
        testInputContent = """
            5 6
            #####
            #S#E#
            #_#_#
            #_#_#
            #___#
            #####
        """.trimIndent()
    }

    @Test
    fun testSolveMaze() {
        // 예제 입력
        val input = testInputContent

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

    @Test
    fun testMainFunction() {
        // 표준 입력 리다이렉션
        val originalIn = System.`in`
        val inputStream = ByteArrayInputStream(testInputContent.toByteArray())
        System.setIn(inputStream)

        // 표준 출력 캡처
        val originalOut = System.out
        val outputStream = ByteArrayOutputStream()
        System.setOut(PrintStream(outputStream))

        // 메인 함수 실행
        main(arrayOf())

        // 표준 입력 및 출력 복원
        System.setIn(originalIn)
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

    @Test
    fun testMainFunctionInTestMode() {
        // 표준 출력 캡처
        val originalOut = System.out
        val outputStream = ByteArrayOutputStream()
        System.setOut(PrintStream(outputStream))

        // 테스트 모드로 메인 함수 실행
        main(arrayOf("test"))

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
        assertEquals(expectedOutput, output, "테스트 모드에서 미로 경로가 예상과 다릅니다")
    }
}
