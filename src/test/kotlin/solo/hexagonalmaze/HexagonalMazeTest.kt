package solo.hexagonalmaze

import org.junit.jupiter.api.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.PrintStream
import kotlin.test.assertEquals

class HexagonalMazeTest {

    @Test
    fun testMazeWithGivenInput() {
        // 입력 데이터 설정
        val input = """
            5 6
            #####
            #S#E#
            #_#_#
            #_#_#
            #___#
            #####
        """.trimIndent()

        // 예상 출력 설정
        val expectedOutput = """
            #####
            #S#E#
            #.#.#
            #.#.#
            #_..#
            #####
        """.trimIndent()

        // 표준 입력 및 출력 리디렉션
        val inputStream = ByteArrayInputStream(input.toByteArray())
        val outputStream = ByteArrayOutputStream()
        val originalIn = System.`in`
        val originalOut = System.out
        val originalErr = System.err

        try {
            System.setIn(inputStream)
            System.setOut(PrintStream(outputStream))
            System.setErr(PrintStream(ByteArrayOutputStream())) // 에러 출력 무시

            // 테스트할 함수 호출
            main(emptyArray())

            // 출력 결과 확인
            val actualOutput = outputStream.toString().trim()

            // 정규화된 출력 (줄바꿈만 유지하고 다른 공백 제거)
            val normalizedExpected = expectedOutput.lines().map { it.trim() }.joinToString("\n")
            val normalizedActual = actualOutput.lines().map { it.trim() }.joinToString("\n")

            // 디버깅을 위해 실제 출력과 예상 출력의 문자 코드 출력
            System.err.println("[DEBUG_LOG] 예상 출력 문자 코드:")
            expectedOutput.forEach { System.err.println("[DEBUG_LOG] '${it}': ${it.code}") }

            System.err.println("[DEBUG_LOG] 실제 출력 문자 코드:")
            actualOutput.forEach { System.err.println("[DEBUG_LOG] '${it}': ${it.code}") }

            // 길이 비교
            System.err.println("[DEBUG_LOG] 예상 출력 길이: ${expectedOutput.length}")
            System.err.println("[DEBUG_LOG] 실제 출력 길이: ${actualOutput.length}")

            // 문자열 비교를 위해 16진수로 표현
            System.err.println("[DEBUG_LOG] 예상 출력 (16진수):")
            expectedOutput.forEach { System.err.print("[DEBUG_LOG] ${it.code.toString(16)} ") }
            System.err.println()

            System.err.println("[DEBUG_LOG] 실제 출력 (16진수):")
            actualOutput.forEach { System.err.print("[DEBUG_LOG] ${it.code.toString(16)} ") }
            System.err.println()

            assertEquals(normalizedExpected, normalizedActual, "정규화된 미로 경로 출력이 예상과 다릅니다.")

        } finally {
            // 원래 입출력 스트림 복원
            System.setIn(originalIn)
            System.setOut(originalOut)
            System.setErr(originalErr)
        }
    }
}
