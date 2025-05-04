package solo.hexagonalmaze

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.assertEquals
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.PrintStream

class MazeDebugTest {

    @Test
    fun testMazeOutput() {
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

            // 테스트할 함수 호출
            solo.hexagonalmaze.main(emptyArray())

            // 출력 결과 확인
            val actualOutput = outputStream.toString().trim()

            // 정규화된 출력 (줄바꿈만 유지하고 다른 공백 제거)
            val normalizedExpected = expectedOutput.lines().map { it.trim() }.joinToString("\n")
            val normalizedActual = actualOutput.lines().map { it.trim() }.joinToString("\n")

            // 디버깅을 위해 실제 출력과 예상 출력 출력
            System.err.println("원본 예상 출력:")
            System.err.println(expectedOutput)
            System.err.println("\n원본 실제 출력:")
            System.err.println(actualOutput)

            System.err.println("\n정규화된 예상 출력:")
            System.err.println(normalizedExpected)
            System.err.println("\n정규화된 실제 출력:")
            System.err.println(normalizedActual)

            // 디버깅을 위해 정규화된 출력의 문자 코드 출력
            System.err.println("\n정규화된 예상 출력 문자 코드:")
            normalizedExpected.forEach { System.err.println("'${it}': ${it.code}") }

            System.err.println("\n정규화된 실제 출력 문자 코드:")
            normalizedActual.forEach { System.err.println("'${it}': ${it.code}") }

            // 길이 비교
            System.err.println("\n정규화된 예상 출력 길이: ${normalizedExpected.length}")
            System.err.println("정규화된 실제 출력 길이: ${normalizedActual.length}")

            // 문자열 비교를 위해 16진수로 표현
            System.err.println("\n정규화된 예상 출력 (16진수):")
            normalizedExpected.forEach { System.err.print("${it.code.toString(16)} ") }
            System.err.println()

            System.err.println("\n정규화된 실제 출력 (16진수):")
            normalizedActual.forEach { System.err.print("${it.code.toString(16)} ") }
            System.err.println()

            // 문자열 비교
            if (normalizedExpected == normalizedActual) {
                System.err.println("\n정규화된 출력이 일치합니다.")
            } else {
                System.err.println("\n정규화된 출력이 일치하지 않습니다.")

                // 차이점 찾기
                System.err.println("\n차이점:")
                val minLength = minOf(normalizedExpected.length, normalizedActual.length)
                for (i in 0 until minLength) {
                    if (normalizedExpected[i] != normalizedActual[i]) {
                        System.err.println("위치 $i: 예상='${normalizedExpected[i]}' (${normalizedExpected[i].code}), 실제='${normalizedActual[i]}' (${normalizedActual[i].code})")
                    }
                }

                if (normalizedExpected.length != normalizedActual.length) {
                    System.err.println("길이가 다릅니다: 예상=${normalizedExpected.length}, 실제=${normalizedActual.length}")
                    if (normalizedExpected.length > normalizedActual.length) {
                        System.err.println("예상 출력에만 있는 문자:")
                        for (i in normalizedActual.length until normalizedExpected.length) {
                            System.err.println("위치 $i: '${normalizedExpected[i]}' (${normalizedExpected[i].code})")
                        }
                    } else {
                        System.err.println("실제 출력에만 있는 문자:")
                        for (i in normalizedExpected.length until normalizedActual.length) {
                            System.err.println("위치 $i: '${normalizedActual[i]}' (${normalizedActual[i].code})")
                        }
                    }
                }
            }

            // 실제 테스트 검증 (정규화된 문자열 사용)
            assertEquals(normalizedExpected, normalizedActual, "정규화된 미로 경로 출력이 예상과 다릅니다.")

        } finally {
            // 원래 입출력 스트림 복원
            System.setIn(originalIn)
            System.setOut(originalOut)
            System.setErr(originalErr)
        }
    }
}
