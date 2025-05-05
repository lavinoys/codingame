import java.util.*

/**
 * Made by Tanvir, converted to Kotlin with performance optimization
 **/

// 상수 정의
const val MAX_WORDS = 15000
const val WORD_LENGTH = 6

// 단어 목록 저장 - CharArray 사용하여 성능 최적화
val wordSet = Array(MAX_WORDS) { CharArray(WORD_LENGTH + 1) }
val possibleWords = Array(MAX_WORDS) { CharArray(WORD_LENGTH + 1) }
var possibleCount = 0
var totalWordCount = 0

// 이전 추측과 상태 기록
val lastGuess = CharArray(WORD_LENGTH + 1)
val lastState = IntArray(WORD_LENGTH)
var turnCount = 0

// 단어가 주어진 상태 정보와 일치하는지 확인
inline fun matchesConstraints(word: CharArray): Boolean {
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
            var found = false
            for (j in 0 until WORD_LENGTH) {
                if (j != i && word[j] == lastGuess[i]) {
                    found = true
                    break
                }
            }
            
            if (!found) {
                return false  // 해당 글자가 단어에 없으면 제외
            }
        }
    }
    
    // 3. 상태 1(단어에 없음) 검사 - 글자 개수 고려
    for (i in 0 until WORD_LENGTH) {
        if (lastState[i] == 1) {
            val c = lastGuess[i]
            
            // 같은 글자가 상태 2나 3으로 있는지 확인
            var expectedCount = 0
            for (j in 0 until WORD_LENGTH) {
                if (lastGuess[j] == c && (lastState[j] == 2 || lastState[j] == 3)) {
                    expectedCount++
                }
            }
            
            // 실제 단어에서 해당 글자의 개수 확인
            var actualCount = 0
            for (j in 0 until WORD_LENGTH) {
                if (word[j] == c) {
                    actualCount++
                }
            }
            
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
fun updatePossibleWords() {
    var newCount = 0
    
    for (i in 0 until possibleCount) {
        if (matchesConstraints(possibleWords[i])) {
            // 그대로 유지하고 인덱스 이동
            if (newCount != i) {
                possibleWords[i].copyInto(possibleWords[newCount])
            }
            newCount++
        }
    }
    
    possibleCount = newCount
    System.err.println("남은 가능한 단어: ${possibleCount}개")
    
    // 디버깅: 일부 가능한 단어 출력 (최대 5개)
    val debugCount = minOf(possibleCount, 5)
    for (i in 0 until debugCount) {
        System.err.println("가능한 단어 #${i+1}: ${String(possibleWords[i], 0, WORD_LENGTH)}")
    }
}

// 단어에서 각 글자의 정보 획득 가치 계산
inline fun calculateWordValue(word: CharArray): Double {
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
fun chooseGuess(guess: CharArray) {
    if (turnCount == 0) {
        // 첫 단어는 정보 획득이 좋은 단어 선택
        "FAULTS".toCharArray().copyInto(guess)
        return
    }
    
    // 가능한 단어가 1개만 남았으면 그 단어 선택
    if (possibleCount == 1) {
        possibleWords[0].copyInto(guess)
        return
    }
    
    // 가능한 단어가 2개만 남았으면 첫 번째 선택
    if (possibleCount == 2) {
        possibleWords[0].copyInto(guess)
        return
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
    
    possibleWords[bestIndex].copyInto(guess)
}

fun main() {
    val input = Scanner(System.`in`)
    val wordCount = input.nextInt()
    totalWordCount = wordCount
    
    for (i in 0 until wordCount) {
        val word = input.next()
        for (j in word.indices) {
            wordSet[i][j] = word[j]
            possibleWords[i][j] = word[j]
        }
        // 문자열 끝을 표시하는 null 문자
        wordSet[i][word.length] = '\u0000'
        possibleWords[i][word.length] = '\u0000'
    }
    
    possibleCount = wordCount
    for (i in lastGuess.indices) {
        lastGuess[i] = '\u0000'
    }
    turnCount = 0

    // game loop
    while (true) {
        for (i in 0 until WORD_LENGTH) {
            lastState[i] = input.nextInt()
        }

        // 가능한 단어 목록 업데이트 (첫 턴이 아닌 경우)
        if (turnCount > 0) {
            updatePossibleWords()
        }

        // 다음 추측 선택
        val guess = CharArray(WORD_LENGTH + 1)
        chooseGuess(guess)
        
        // 추측 저장 및 출력
        guess.copyInto(lastGuess)
        println(String(guess, 0, WORD_LENGTH))
        
        // 디버그 정보 출력
        System.err.println("턴 ${turnCount + 1}: 추측 = ${String(guess, 0, WORD_LENGTH)}")
        
        turnCount++
    }
}
