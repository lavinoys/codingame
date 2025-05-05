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

// 성능 최적화를 위한 비트마스킹 데이터 구조
val wordBitMasks = IntArray(MAX_WORDS) // 각 단어의 글자 포함 여부 마스크 (26비트 사용)
val positionMasks = Array(WORD_LENGTH) { IntArray(26) } // 각 위치별 가능한 글자 마스크
var requiredLettersMask = 0 // 단어에 반드시 포함되어야 하는 글자 마스크
var forbiddenLettersMask = 0 // 단어에 포함되면 안 되는 글자 마스크
val positionConstraints = IntArray(WORD_LENGTH) // 각 위치별 반드시 있어야 하는 글자

// 글자별 단어 인덱싱
val letterToWordIndices = Array(26) { mutableListOf<Int>() }

// 계산 캐싱
val wordValueCache = DoubleArray(MAX_WORDS) { -1.0 }
val letterFrequency = Array(WORD_LENGTH) { IntArray(26) } // 각 위치별 글자 빈도

// 단어를 비트마스크로 변환
fun wordToBitMask(word: CharArray): Int {
    var mask = 0
    for (i in 0 until WORD_LENGTH) {
        mask = mask or (1 shl (word[i] - 'A'))
    }
    return mask
}

// 위치별 글자 비트마스크 구축
fun buildPositionMasks() {
    // 초기화 - 모든 위치에 모든 글자 가능
    for (pos in 0 until WORD_LENGTH) {
        for (letter in 0 until 26) {
            positionMasks[pos][letter] = 1
        }
    }
    
    // 위치별 글자 빈도 계산
    for (i in 0 until totalWordCount) {
        for (j in 0 until WORD_LENGTH) {
            val letterIdx = wordSet[i][j] - 'A'
            letterFrequency[j][letterIdx]++
        }
    }
}

// 글자별 단어 인덱스 구축
fun buildLetterIndices() {
    for (i in 0 until totalWordCount) {
        // 비트마스크 생성 및 저장
        wordBitMasks[i] = wordToBitMask(wordSet[i])
        
        // 각 글자별로 단어 인덱스 저장
        val used = BooleanArray(26)
        for (j in 0 until WORD_LENGTH) {
            val letterIdx = wordSet[i][j] - 'A'
            if (!used[letterIdx]) {
                letterToWordIndices[letterIdx].add(i)
                used[letterIdx] = true
            }
        }
    }
}

// 제약 조건 마스크 업데이트
fun updateConstraintMasks() {
    requiredLettersMask = 0
    forbiddenLettersMask = 0
    
    // 각 위치별 제약 초기화
    for (i in 0 until WORD_LENGTH) {
        positionConstraints[i] = -1 // -1은 제약 없음
    }
    
    // 글자별 카운트 관리
    val minLetterCount = IntArray(26)
    val maxLetterCount = IntArray(26) { Int.MAX_VALUE }
    
    // 상태에 따른 마스크 업데이트
    for (i in 0 until WORD_LENGTH) {
        val letterIdx = lastGuess[i] - 'A'
        
        when (lastState[i]) {
            3 -> { // 정확한 위치
                requiredLettersMask = requiredLettersMask or (1 shl letterIdx)
                positionConstraints[i] = letterIdx
                minLetterCount[letterIdx] = maxOf(minLetterCount[letterIdx], 1)
            }
            2 -> { // 다른 위치에 있음
                requiredLettersMask = requiredLettersMask or (1 shl letterIdx)
                // 이 위치에는 이 글자가 올 수 없음
                positionMasks[i][letterIdx] = 0
                minLetterCount[letterIdx] = maxOf(minLetterCount[letterIdx], 1)
            }
            1 -> { // 단어에 없음
                // 해당 글자가 다른 위치에서 필요한지 확인
                var foundElsewhere = false
                for (j in 0 until WORD_LENGTH) {
                    if (j != i && lastGuess[j] == lastGuess[i] && 
                        (lastState[j] == 2 || lastState[j] == 3)) {
                        foundElsewhere = true
                        break
                    }
                }
                
                if (!foundElsewhere) {
                    forbiddenLettersMask = forbiddenLettersMask or (1 shl letterIdx)
                    maxLetterCount[letterIdx] = 0
                } else {
                    // 글자 개수 제한
                    var exactCount = 0
                    for (j in 0 until WORD_LENGTH) {
                        if (lastGuess[j] == lastGuess[i] && 
                            (lastState[j] == 2 || lastState[j] == 3)) {
                            exactCount++
                        }
                    }
                    maxLetterCount[letterIdx] = exactCount
                }
            }
        }
    }
}

// 단어가 제약조건에 맞는지 빠르게 확인 (비트마스크 및 최적화 적용)
fun matchesConstraints(word: CharArray, wordIndex: Int): Boolean {
    // 첫 번째 추측이라면 모든 단어가 가능함
    if (turnCount == 0) {
        return true
    }
    
    val wordMask = wordBitMasks[wordIndex]
    
    // 1. 비트마스크 기반 빠른 필터링
    // 필수 글자 확인 (AND 연산)
    if ((wordMask and requiredLettersMask) != requiredLettersMask) {
        return false
    }

    // 금지 글자 확인 (AND 연산이 0이어야 함)
    if ((wordMask and forbiddenLettersMask) != 0) {
        return false
    }
    
    // 2. 위치 기반 제약 조건 확인 (가장 제한적인 조건)
    for (i in 0 until WORD_LENGTH) {
        val posConstraint = positionConstraints[i]
        
        // 해당 위치에 특정 글자가 있어야 함
        if (posConstraint != -1 && word[i] - 'A' != posConstraint) {
            return false
        }
        
        // 해당 위치에 특정 글자가 없어야 함
        if (positionMasks[i][word[i] - 'A'] == 0) {
            return false
        }
    }
    
    // 3. 글자 개수 제약 조건 확인
    val letterCount = IntArray(26)
    for (i in 0 until WORD_LENGTH) {
        letterCount[word[i] - 'A']++
    }
    
    // 상태 1, 2, 3에 따른 글자별 개수 제약 확인
    for (i in 0 until WORD_LENGTH) {
        val letterIdx = lastGuess[i] - 'A'
        
        if (lastState[i] == 1) {
            val c = lastGuess[i]
            
            // 같은 글자가 상태 2나 3으로 있는지 확인
            var expectedCount = 0
            for (j in 0 until WORD_LENGTH) {
                if (lastGuess[j] == c && (lastState[j] == 2 || lastState[j] == 3)) {
                    expectedCount++
                }
            }
            
            // 글자 개수 제약 확인
            val actualCount = letterCount[letterIdx]
            
            if (expectedCount == 0 && actualCount > 0) {
                return false
            } else if (expectedCount > 0 && actualCount > expectedCount) {
                return false
            }
        }
    }
    
    return true
}

// 가능한 단어 목록 업데이트 (최적화 버전)
fun updatePossibleWords() {
    // 제약 조건 마스크 업데이트
    updateConstraintMasks()
    
    var newCount = 0
    
    // 가장 제한적인 조건부터 필터링 적용
    for (i in 0 until possibleCount) {
        if (matchesConstraints(possibleWords[i], i)) {
            // 그대로 유지하고 인덱스 이동
            if (newCount != i) {
                possibleWords[i].copyInto(possibleWords[newCount])
                wordBitMasks[newCount] = wordBitMasks[i]
            }
            newCount++
        }
    }
    
    possibleCount = newCount
    
    // 캐시 초기화 - 단어 목록이 변경됨
    for (i in 0 until possibleCount) {
        wordValueCache[i] = -1.0
    }
    
    System.err.println("남은 가능한 단어: ${possibleCount}개")
    
    // 디버깅: 일부 가능한 단어 출력 (최대 5개)
    val debugCount = minOf(possibleCount, 5)
    for (i in 0 until debugCount) {
        System.err.println("가능한 단어 #${i+1}: ${String(possibleWords[i], 0, WORD_LENGTH)}")
    }
}

// 정보 이론 기반 단어 가치 계산 (최적화 버전)
fun calculateWordValue(word: CharArray): Double {
    // 정보 획득량 기반 휴리스틱
    var value = 0.0
    val letterValue = DoubleArray(26)
    val used = BooleanArray(26)
    
    // 1. 빈도 기반 기본 가치 계산
    for (i in 0 until WORD_LENGTH) {
        val letterIdx = word[i] - 'A'
        if (!used[letterIdx]) {
            // 남은 가능한 단어에서의 글자 빈도
            var freq = 0
            for (j in 0 until possibleCount) {
                val hasLetter = wordBitMasks[j] and (1 shl letterIdx) != 0
                if (hasLetter) freq++
            }
            
            // 빈도에 따른 정보 가치 (50%에 가까울수록 높은 정보량)
            val ratio = freq.toDouble() / possibleCount
            letterValue[letterIdx] = ratio * (1.0 - ratio) * 4.0 // 정보 엔트로피 근사값
            used[letterIdx] = true
        }
    }
    
    // 2. 위치별 가중치 추가
    for (i in 0 until WORD_LENGTH) {
        val letterIdx = word[i] - 'A'
        // 이 위치에서의 글자 등장 빈도에 기반한 가중치
        val posFreq = letterFrequency[i][letterIdx].toDouble() / totalWordCount
        value += letterValue[letterIdx] * (1.0 + posFreq)
    }
    
    // 3. 중복 글자 페널티
    val letterCount = IntArray(26)
    var uniqueLetters = 0
    for (i in 0 until WORD_LENGTH) {
        val letterIdx = word[i] - 'A'
        letterCount[letterIdx]++
        if (letterCount[letterIdx] == 1) uniqueLetters++
    }
    
    // 독특한 글자가 많을수록 높은 가치
    value *= (0.7 + 0.3 * uniqueLetters / WORD_LENGTH)
    
    return value
}

// 캐시된 단어 가치 얻기
fun getWordValue(index: Int): Double {
    if (wordValueCache[index] >= 0) {
        return wordValueCache[index]
    }
    
    val value = calculateWordValue(possibleWords[index])
    wordValueCache[index] = value
    return value
}

// 최적의 추측 단어 선택 (최적화 버전)
fun chooseGuess(guess: CharArray) {
    if (turnCount == 0) {
        // 첫 단어는 정보 획득이 좋은 단어 선택 (사전 분석 기반)
        "FAULTS".toCharArray().copyInto(guess)
        return
    }
    
    // 가능한 단어가 1개나 2개만 남았으면 첫 번째 선택
    if (possibleCount <= 2) {
        possibleWords[0].copyInto(guess)
        return
    }
    
    // 단어 수에 따라 전략 조정 (점진적 복잡도)
    val useFullEvaluation = possibleCount < 200
    
    // 평가할 단어 수 결정
    val evalLimit = when {
        possibleCount < 50 -> possibleCount // 적은 단어는 모두 평가
        possibleCount < 500 -> minOf(possibleCount, 200) // 중간 규모는 일부만
        else -> minOf(possibleCount, 100) // 큰 규모는 더 적게
    }
    
    var bestValue = -1.0
    var bestIndex = 0
    
    // 단어 평가 (2단계 평가: 먼저 간단한 휴리스틱으로 후보군 줄이기)
    if (useFullEvaluation) {
        // 전체 평가
        for (i in 0 until possibleCount) {
            val value = getWordValue(i)
            if (value > bestValue) {
                bestValue = value
                bestIndex = i
            }
        }
    } else {
        // 빠른 휴리스틱으로 후보 선정 후 정밀 평가
        val candidates = IntArray(evalLimit)
        var candidateCount = 0
        
        // 간단한 휴리스틱으로 후보 선정
        for (i in 0 until possibleCount) {
            val uniqueLetters = countUniqueLetters(possibleWords[i])
            if (candidateCount < evalLimit) {
                candidates[candidateCount++] = i
            } else if (uniqueLetters > countUniqueLetters(possibleWords[candidates[0]])) {
                // 가장 낮은 후보와 교체
                candidates[0] = i
                // 삽입 정렬로 유지
                var j = 0
                while (j < candidateCount - 1 && 
                       countUniqueLetters(possibleWords[candidates[j]]) < 
                       countUniqueLetters(possibleWords[candidates[j+1]])) {
                    val temp = candidates[j]
                    candidates[j] = candidates[j+1]
                    candidates[j+1] = temp
                    j++
                }
            }
        }
        
        // 선정된 후보만 정밀 평가
        for (i in 0 until candidateCount) {
            val index = candidates[i]
            val value = getWordValue(index)
            if (value > bestValue) {
                bestValue = value
                bestIndex = index
            }
        }
    }
    
    possibleWords[bestIndex].copyInto(guess)
}

// 단어의 독특한 글자 수 계산 (캐싱 없는 간단한 휴리스틱)
fun countUniqueLetters(word: CharArray): Int {
    val used = BooleanArray(26)
    var count = 0
    
    for (i in 0 until WORD_LENGTH) {
        val letterIdx = word[i] - 'A'
        if (!used[letterIdx]) {
            used[letterIdx] = true
            count++
        }
    }
    
    return count
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
    
    // 최적화를 위한 사전 계산
    buildLetterIndices()
    buildPositionMasks()

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
