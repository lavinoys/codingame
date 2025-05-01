package solo.hexagonalmaze

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import java.io.ByteArrayOutputStream
import java.io.PrintStream
import solo.hexagonalmaze.main

/**
 * 테스트 실행 클래스
 * HexagonalMaze.kt의 main 함수를 "test" 인자와 함께 호출하여 테스트
 */
class RunTest {

    @Test
    fun testMainWithTestArgument() {
        // 표준 출력 캡처
        val originalOut = System.out
        val outputStream = ByteArrayOutputStream()
        System.setOut(PrintStream(outputStream))

        // 테스트 모드로 main 함수 호출
        assertDoesNotThrow {
            main(arrayOf("test"))
        }

        // 표준 출력 복원
        System.setOut(originalOut)

        // 테스트가 예외 없이 완료되었는지 확인
        println("테스트 완료!")
    }
}
