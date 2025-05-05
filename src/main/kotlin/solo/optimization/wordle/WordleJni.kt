import java.util.*

/**
 * Made by Tanvir
 **/
fun main() {
    val input = Scanner(System.`in`)
    val wordCount = input.nextInt() // Number of words in the word set
    
    // 가능한 단어 목록 저장
    val allWords = ArrayList<String>()
    (0 until wordCount).forEach { _ ->
        val word = input.next() // Word in the word set
        allWords.add(word)
    }
    
    var possibleWords = ArrayList(allWords) // 현재 가능한 단어 목록
    var lastGuess = "" // 이전 추측 단어
    
    // game loop
    while (true) {
        val states = IntArray(6)
        for (i in 0 until 6) {
            states[i] = input.nextInt() // State of the letter of the corresponding position of previous guess
        }
        
        System.err.println("가능한 단어 수: ${possibleWords.size}")
        
        // 첫 턴이 아니라면 이전 추측의 결과를 기반으로 가능한 단어 필터링
        if (states[0] != 0) {
            possibleWords = filterPossibleWords(possibleWords, lastGuess, states)
            System.err.println("필터링 후 단어 수: ${possibleWords.size}")
        }
        
        // 다음 추측 선택
        lastGuess = if (possibleWords.size == 1) {
            possibleWords[0] // 정답을 찾은 경우
        } else if (possibleWords.size <= 3) {
            possibleWords[0] // 가능성이 적으면 그 중 하나를 시도
        } else if (states[0] == 0) {
            // 첫 턴에는 정보 수집을 위한 단어 선택 (많은 자주 사용되는 글자가 포함된 단어)
            "STARED"
        } else {
            // 정보 획득을 최대화하는 단어 선택 전략
            chooseNextGuess(possibleWords)
        }
        
        println(lastGuess.uppercase())
    }
}

// 이전 추측 결과를 바탕으로 가능한 단어를 필터링
fun filterPossibleWords(words: ArrayList<String>, guess: String, states: IntArray): ArrayList<String> {
    val filtered = ArrayList<String>()
    
    // 각 위치별 확정 글자 (상태 3)
    val confirmedLetters = CharArray(6) { ' ' }
    // 단어에 포함되어야 하는 글자들 (상태 2)
    val mustInclude = HashMap<Char, Int>()
    // 단어에 포함되지 않아야 하는 글자들 (상태 1)
    val mustExclude = HashSet<Char>()
    // 위치별 사용할 수 없는 글자 (상태 2 - 이 위치에는 올 수 없음)
    val positionExclude = Array(6) { HashSet<Char>() }
    
    // 상태 정보 분석
    for (i in 0 until 6) {
        val char = guess[i]
        when (states[i]) {
            1 -> {
                // 다른 위치에 이 글자가 상태 2나 3으로 존재하는지 확인
                val existsElsewhere = (0 until 6).any { j -> j != i && (states[j] == 2 || states[j] == 3) && guess[j] == char }
                if (!existsElsewhere) {
                    mustExclude.add(char)
                }
            }
            2 -> {
                // 글자는 단어에 있지만 이 위치는 아님
                positionExclude[i].add(char)
                mustInclude[char] = mustInclude.getOrDefault(char, 0) + 1
            }
            3 -> {
                // 글자가 정확한 위치에 있음
                confirmedLetters[i] = char
                mustInclude[char] = mustInclude.getOrDefault(char, 0) + 1
            }
        }
    }
    
    // 필터링
    for (word in words) {
        var isValid = true
        
        // 확정 글자 검사
        for (i in confirmedLetters.indices) {
            if (confirmedLetters[i] != ' ' && word[i] != confirmedLetters[i]) {
                isValid = false
                break
            }
        }
        if (!isValid) continue
        
        // 위치별 제외 글자 검사
        for (i in 0 until 6) {
            if (positionExclude[i].contains(word[i])) {
                isValid = false
                break
            }
        }
        if (!isValid) continue
        
        // 포함/제외 글자 검사
        val letterCount = HashMap<Char, Int>()
        for (c in word) {
            letterCount[c] = letterCount.getOrDefault(c, 0) + 1
        }
        
        for ((char, count) in mustInclude) {
            if (letterCount.getOrDefault(char, 0) < count) {
                isValid = false
                break
            }
        }
        if (!isValid) continue
        
        for (char in mustExclude) {
            if (letterCount.getOrDefault(char, 0) > 0) {
                isValid = false
                break
            }
        }
        if (!isValid) continue
        
        filtered.add(word)
    }
    
    return filtered
}

// 다음 추측을 위한 단어 선택
fun chooseNextGuess(possibleWords: ArrayList<String>): String {
    if (possibleWords.isEmpty()) return "ABCDEF" // 예외 처리
    
    // 최대 100개 후보만 평가 (시간 제한 때문)
    val candidates = if (possibleWords.size <= 100) possibleWords else possibleWords.shuffled().take(100) as ArrayList<String>
    
    var bestScore = -1
    var bestWord = possibleWords[0]
    
    // 각 후보마다 점수 계산
    for (word in candidates) {
        val score = scoreWord(word, possibleWords)
        if (score > bestScore) {
            bestScore = score
            bestWord = word
        }
    }
    
    return bestWord
}

// 단어의 점수 계산 (높은 정보 획득 가능성 의미)
fun scoreWord(word: String, possibleWords: ArrayList<String>): Int {
    // 글자 빈도수 계산 (가장 흔한 글자가 포함된 단어가 좋음)
    val letterFrequency = HashMap<Char, Int>()
    for (w in possibleWords) {
        for (c in w) {
            letterFrequency[c] = letterFrequency.getOrDefault(c, 0) + 1
        }
    }
    
    // 중복되지 않은 글자에 대한 점수 부여
    val uniqueLetters = HashSet<Char>()
    var score = 0
    
    for (c in word) {
        if (uniqueLetters.add(c)) {
            score += letterFrequency.getOrDefault(c, 0)
        } else {
            // 중복 글자는 점수를 낮춤
            score -= letterFrequency.getOrDefault(c, 0) / 2
        }
    }
    
    return score
}
