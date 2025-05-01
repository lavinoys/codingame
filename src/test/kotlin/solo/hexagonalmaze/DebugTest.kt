package solo.hexagonalmaze

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.PrintStream

/**
 * 디버깅을 위한 테스트 실행기
 */
fun main() {
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
        
        // 디버깅을 위해 실제 출력과 예상 출력 출력
        println("예상 출력:")
        println(expectedOutput)
        println("\n실제 출력:")
        println(actualOutput)
        
        // 디버깅을 위해 실제 출력과 예상 출력의 문자 코드 출력
        println("\n예상 출력 문자 코드:")
        expectedOutput.forEach { println("'${it}': ${it.code}") }
        
        println("\n실제 출력 문자 코드:")
        actualOutput.forEach { println("'${it}': ${it.code}") }
        
        // 길이 비교
        println("\n예상 출력 길이: ${expectedOutput.length}")
        println("실제 출력 길이: ${actualOutput.length}")
        
        // 문자열 비교를 위해 16진수로 표현
        println("\n예상 출력 (16진수):")
        expectedOutput.forEach { print("${it.code.toString(16)} ") }
        println()
        
        println("\n실제 출력 (16진수):")
        actualOutput.forEach { print("${it.code.toString(16)} ") }
        println()
        
        // 문자열 비교
        if (expectedOutput == actualOutput) {
            println("\n출력이 일치합니다.")
        } else {
            println("\n출력이 일치하지 않습니다.")
            
            // 차이점 찾기
            println("\n차이점:")
            val minLength = minOf(expectedOutput.length, actualOutput.length)
            for (i in 0 until minLength) {
                if (expectedOutput[i] != actualOutput[i]) {
                    println("위치 $i: 예상='${expectedOutput[i]}' (${expectedOutput[i].code}), 실제='${actualOutput[i]}' (${actualOutput[i].code})")
                }
            }
            
            if (expectedOutput.length != actualOutput.length) {
                println("길이가 다릅니다: 예상=${expectedOutput.length}, 실제=${actualOutput.length}")
                if (expectedOutput.length > actualOutput.length) {
                    println("예상 출력에만 있는 문자:")
                    for (i in actualOutput.length until expectedOutput.length) {
                        println("위치 $i: '${expectedOutput[i]}' (${expectedOutput[i].code})")
                    }
                } else {
                    println("실제 출력에만 있는 문자:")
                    for (i in expectedOutput.length until actualOutput.length) {
                        println("위치 $i: '${actualOutput[i]}' (${actualOutput[i].code})")
                    }
                }
            }
        }
        
    } finally {
        // 원래 입출력 스트림 복원
        System.setIn(originalIn)
        System.setOut(originalOut)
        System.setErr(originalErr)
    }
}