import java.util.*

/**
 * Made by Tanvir
 * Kotlin conversion with optimizations
 */

// 글자 상태를 나타내는 열거형
enum class LetterState(val value: Int) {
    UNKNOWN(0),      // 초기 상태
    NOT_PRESENT(1),  // 단어에 없음
    WRONG_POSITION(2), // 단어에 있지만 위치가 잘못됨
    CORRECT_POSITION(3); // 정확한 위치에 있음
    
    companion object {
        fun fromValue(value: Int): LetterState = values().first { it.value == value }
    }
}

class WordleSolver {
    companion object {
        const val WORD_LENGTH = 6
        private const val FIRST_GUESS = "FAULTS" // 첫 추측 단어
    }

    // 단어 목록 저장 (동적 리스트 사용)
    private val wordList = mutableListOf<String>()
    private var possibleWords = mutableListOf<String>()

    // 이전 추측과 상태 기록
    private var lastGuess = ""
    private val lastState = mutableListOf<LetterState>()
    private var turnCount = 0

    // 글자별 제약조건 저장 (성능 개선을 위한 캐싱)
    private val letterConstraints = mutableMapOf<Char, LetterConstraint>()
    
    // 각 위치별로 확정된 글자 저장
    private val positionConstraints = Array<Char?>(WORD_LENGTH) { null }

    // 글자별 제약조건 관리 클래스
    private data class LetterConstraint(
        var minCount: Int = 0,             // 최소 등장 횟수
        var maxCount: Int = WORD_LENGTH,   // 최대 등장 횟수
        val forbiddenPositions: MutableSet<Int> = mutableSetOf() // 해당 글자가 있으면 안 되는 위치
    )

    // 단어가 주어진 제약조건과 일치하는지 확인
    private fun matchesConstraints(word: String): Boolean {
        // 첫 번째 추측이라면 모든 단어가 가능함
        if (turnCount == 0) return true
        
        // 1. 확정된 위치 검사
        for (i in word.indices) {
            positionConstraints[i]?.let { if (word[i] != it) return false }
        }
        
        // 2. 글자별 제약조건 검사
        val letterCounts = word.groupingBy { it }.eachCount()
        
        for ((letter, constraint) in letterConstraints) {
            val count = letterCounts[letter] ?: 0
            
            // 최소/최대 개수 제약조건 검사
            if (count < constraint.minCount || count > constraint.maxCount) {
                return false
            }
            
            // 금지된 위치 검사
            if (constraint.forbiddenPositions.any { pos -> word[pos] == letter }) {
                return false
            }
        }
        
        return true
    }

    // 제약조건 업데이트
    private fun updateConstraints() {
        if (lastGuess.isEmpty() || lastState.isEmpty()) return
        
        // 글자 개수 계산
        val guessLetterCount = lastGuess.groupingBy { it }.eachCount()
        val correctOrMisplaced = mutableMapOf<Char, Int>()
        
        // 첫 번째 패스: CORRECT_POSITION 처리
        lastGuess.forEachIndexed { idx, char ->
            when (lastState[idx]) {
                LetterState.CORRECT_POSITION -> {
                    positionConstraints[idx] = char
                    correctOrMisplaced[char] = (correctOrMisplaced[char] ?: 0) + 1
                    
                    letterConstraints.getOrPut(char) { LetterConstraint() }.apply {
                        minCount = maxOf(minCount, correctOrMisplaced[char] ?: 0)
                    }
                }
                else -> {}
            }
        }
        
        // 두 번째 패스: WRONG_POSITION 처리
        lastGuess.forEachIndexed { idx, char ->
            when (lastState[idx]) {
                LetterState.WRONG_POSITION -> {
                    correctOrMisplaced[char] = (correctOrMisplaced[char] ?: 0) + 1
                    
                    letterConstraints.getOrPut(char) { LetterConstraint() }.apply {
                        minCount = maxOf(minCount, correctOrMisplaced[char] ?: 0)
                        forbiddenPositions.add(idx)
                    }
                }
                else -> {}
            }
        }
        
        // 세 번째 패스: NOT_PRESENT 처리
        lastGuess.forEachIndexed { idx, char ->
            when (lastState[idx]) {
                LetterState.NOT_PRESENT -> {
                    letterConstraints.getOrPut(char) { LetterConstraint() }.apply {
                        // 다른 위치에서 발견된 동일 글자 개수가 최대 개수가 됨
                        maxCount = correctOrMisplaced[char] ?: 0
                        
                        // 처음 나오는 없는 글자인 경우 maxCount를 0으로 설정
                        if (correctOrMisplaced[char] == null) {
                            maxCount = 0
                        }
                    }
                }
                else -> {}
            }
        }
    }

    // 가능한 단어 목록 업데이트
    private fun updatePossibleWords() {
        updateConstraints()
        possibleWords = possibleWords.filter { matchesConstraints(it) }.toMutableList()
        
        System.err.println("남은 가능한 단어: ${possibleWords.size}개")
        
        // 디버깅: 일부 가능한 단어 출력 (최대 5개)
        possibleWords.take(5).forEachIndexed { index, word ->
            System.err.println("가능한 단어 #${index+1}: $word")
        }
    }

    // 단어의 정보 획득 가치 계산
    private fun calculateInformationValue(word: String): Double {
        // 이미 가능한 단어 목록에 있는 경우 우선 고려
        if (word in possibleWords) return Double.MAX_VALUE - letterFrequencyValue(word)
        
        return letterFrequencyValue(word)
    }
    
    // 글자 빈도 기반 가치 계산
    private fun letterFrequencyValue(word: String): Double {
        if (possibleWords.isEmpty()) return 0.0
        
        // 남은 가능한 단어에서 글자 빈도 계산
        val letterFrequencies = mutableMapOf<Char, Int>()
        
        possibleWords.forEach { possibleWord ->
            possibleWord.toSet().forEach { char -> 
                letterFrequencies[char] = (letterFrequencies[char] ?: 0) + 1
            }
        }
        
        // 단어에서 고유 글자의 빈도 합산 (중복 글자는 한번만 계산)
        return word.toSet().sumOf { letterFrequencies[it]?.toDouble() ?: 0.0 } / possibleWords.size
    }

    // 최적의 추측 단어 선택
    private fun chooseGuess(): String {
        // 첫 턴이면 고정된 시작 단어
        if (turnCount == 0) return FIRST_GUESS
        
        // 가능한 단어가 적으면 첫 번째 반환
        if (possibleWords.size <= 2) return possibleWords.first()
        
        // 정보 획득 가치가 가장 높은 단어 선택
        return possibleWords
            .asSequence()
            .take(minOf(possibleWords.size, 500)) // 시간 제한을 고려하여 일부만 평가
            .maxByOrNull { calculateInformationValue(it) }
            ?: possibleWords.first()
    }

    fun solve() {
        val scanner = Scanner(System.`in`)
        val wordCount = scanner.nextInt()
        
        // 단어 세트 로드
        repeat(wordCount) {
            val word = scanner.next()
            wordList.add(word)
        }
        possibleWords = wordList.toMutableList()
        
        // 게임 루프
        while (true) {
            // 상태 정보 입력 받기
            lastState.clear()
            repeat(WORD_LENGTH) {
                val state = scanner.nextInt()
                lastState.add(LetterState.fromValue(state))
            }

            // 가능한 단어 목록 업데이트 (첫 턴이 아닌 경우)
            if (turnCount > 0) {
                updatePossibleWords()
            }

            // 다음 추측 선택 및 출력
            lastGuess = chooseGuess()
            println(lastGuess)
            System.out.flush()
            
            // 디버그 정보 출력
            System.err.println("턴 ${turnCount + 1}: 추측 = $lastGuess")
            
            turnCount++
        }
    }
}

fun main() {
    WordleSolver().solve()
}
