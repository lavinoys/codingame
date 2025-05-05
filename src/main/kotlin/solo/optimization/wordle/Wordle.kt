import java.util.*

/**
 * Made by Tanvir
 * Kotlin conversion
 */

class WordleSolver {
    companion object {
        const val MAX_WORDS = 15000
        const val WORD_LENGTH = 6
    }

    // 단어 목록 저장
    private val wordSet = Array(MAX_WORDS) { "" }
    private val possibleWords = Array(MAX_WORDS) { "" }
    private var possibleCount = 0
    private var totalWordCount = 0

    // 이전 추측과 상태 기록
    private var lastGuess = ""
    private val lastState = IntArray(WORD_LENGTH)
    private var turnCount = 0

    // 글자 빈도수를 나타내는 데이터 클래스
    data class LetterFreq(val letter: Char, val frequency: Int)

    // 단어가 주어진 상태 정보와 일치하는지 확인
    private fun matchesConstraints(word: String): Boolean {
        // 첫 번째 추측이라면 모든 단어가 가능함
        if (turnCount == 0) {
            return true
        }
        
        // 1. 상태 3(정확한 위치)부터 검사
        for (i in 0 until WORD_LENGTH) {
            if (lastState[i] == 3 && word[i] != lastGuess[i]) {
                return false  // 정확한 위치에 맞는 글자가 없으면 제외
            }
        }
        
        // 2. 상태 2(다른 위치에 있음) 검사
        for (i in 0 until WORD_LENGTH) {
            if (lastState[i] == 2) {
                // 같은 위치에 같은 글자가 있으면 안됨
                if (word[i] == lastGuess[i]) {
                    return false
                }
                
                // 단어에 해당 글자가 있는지 확인
                if (!word.indices.any { j -> j != i && word[j] == lastGuess[i] }) {
                    return false  // 해당 글자가 단어에 없으면 제외
                }
            }
        }
        
        // 3. 상태 1(단어에 없음) 검사 - 글자 개수 고려
        for (i in 0 until WORD_LENGTH) {
            if (lastState[i] == 1) {
                val c = lastGuess[i]
                
                // 같은 글자가 상태 2나 3으로 있는지 확인
                val expectedCount = (0 until WORD_LENGTH).count { j -> 
                    lastGuess[j] == c && (lastState[j] == 2 || lastState[j] == 3) 
                }
                
                // 실제 단어에서 해당 글자의 개수 확인
                val actualCount = word.count { it == c }
                
                // 글자가 없어야 하거나(expectedCount=0), 정확히 expectedCount만큼만 있어야 함
                if (expectedCount == 0 && actualCount > 0) {
                    return false
                } else if (expectedCount > 0 && actualCount > expectedCount) {
                    return false
                }
            }
        }
        
        return true
    }

    // 가능한 단어 목록 업데이트
    private fun updatePossibleWords() {
        val newPossibleWords = mutableListOf<String>()
        
        for (i in 0 until possibleCount) {
            if (matchesConstraints(possibleWords[i])) {
                newPossibleWords.add(possibleWords[i])
            }
        }
        
        possibleCount = newPossibleWords.size
        newPossibleWords.forEachIndexed { index, word ->
            possibleWords[index] = word
        }
        
        System.err.println("남은 가능한 단어: ${possibleCount}개")
        
        // 디버깅: 일부 가능한 단어 출력 (최대 5개)
        val debugCount = minOf(possibleCount, 5)
        for (i in 0 until debugCount) {
            System.err.println("가능한 단어 #${i+1}: ${possibleWords[i]}")
        }
    }

    // 단어에서 각 글자의 정보 획득 가치 계산
    private fun calculateWordValue(word: String): Double {
        // 간단한 휴리스틱: 남은 가능한 단어에 있는 글자의 빈도수를 기준으로 점수 계산
        val letterCount = IntArray(26)
        
        // 남은 가능한 단어들에서 각 글자의 빈도수 계산
        for (i in 0 until possibleCount) {
            val used = BooleanArray(26)
            for (j in 0 until WORD_LENGTH) {
                val idx = possibleWords[i][j] - 'A'
                if (!used[idx]) {
                    letterCount[idx]++
                    used[idx] = true
                }
            }
        }
        
        // 단어의 가치 계산: 단어에 있는 독특한 글자의 빈도 합
        var value = 0.0
        val used = BooleanArray(26)
        
        for (i in 0 until WORD_LENGTH) {
            val idx = word[i] - 'A'
            if (!used[idx]) {
                value += letterCount[idx].toDouble() / possibleCount
                used[idx] = true
            }
        }
        
        return value
    }

    // 최적의 추측 단어 선택
    private fun chooseGuess(): String {
        if (turnCount == 0) {
            // 첫 단어는 정보 획득이 좋은 단어 선택
            return "FAULTS"  // 많은 자음과 모음이 포함된 6글자 단어
        }
        
        // 가능한 단어가 1개나 2개만 남았으면 첫 번째 선택
        if (possibleCount <= 2) {
            return possibleWords[0]
        }
        
        // 정보 획득이 최대인 단어 선택
        var bestValue = -1.0
        var bestIndex = 0
        
        // 가능한 단어 중 일부만 평가 (시간 제한 고려)
        val evalLimit = minOf(possibleCount, 1000)
        
        for (i in 0 until evalLimit) {
            val value = calculateWordValue(possibleWords[i])
            if (value > bestValue) {
                bestValue = value
                bestIndex = i
            }
        }
        
        return possibleWords[bestIndex]
    }

    fun solve() {
        // 단어 수 입력 받기
        val scanner = Scanner(System.`in`)
        val wordCount = scanner.nextInt()
        totalWordCount = wordCount
        
        // 단어 세트 로드
        for (i in 0 until wordCount) {
            val word = scanner.next()
            wordSet[i] = word
            possibleWords[i] = word  // 초기에는 모든 단어가 가능함
        }
        
        possibleCount = wordCount
        lastGuess = ""
        turnCount = 0

        // 게임 루프
        while (true) {
            // 이전 추측 결과 상태 입력 받기
            for (i in 0 until WORD_LENGTH) {
                lastState[i] = scanner.nextInt()
            }

            // 가능한 단어 목록 업데이트 (첫 턴이 아닌 경우)
            if (turnCount > 0) {
                updatePossibleWords()
            }

            // 다음 추측 선택
            val guess = chooseGuess()
            
            // 추측 저장 및 출력
            lastGuess = guess
            println(guess)
            System.out.flush() // 출력 버퍼 비우기
            
            // 디버그 정보 출력
            System.err.println("턴 ${turnCount + 1}: 추측 = $guess")
            
            turnCount++
        }
    }
}

fun main() {
    val solver = WordleSolver()
    solver.solve()
}
