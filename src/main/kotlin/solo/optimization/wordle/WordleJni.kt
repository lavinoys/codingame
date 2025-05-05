import java.util.*
import java.io.File

/**
 * Made by Tanvir
 **/

// JNI를 통해 C 함수를 호출하기 위한 외부 함수 선언
private external fun scoreWordNative(
    word: String, 
    possibleWords: Array<String>, 
    possibleWordsSize: Int
): Int

// C 코드 컴파일 및 로드를 처리하는 객체
object NativeHelper {
    private var nativeLibraryLoaded = false

    fun loadNativeLibrary(): Boolean {
        if (nativeLibraryLoaded) return true
        
        try {
            val tempDir = createTempDir("wordle_jni")
            val cCode = """
#include <jni.h>
#include <string.h>
#include <stdlib.h>

// HashMap 구현을 위한 구조체 및 함수
typedef struct {
    char key;
    int value;
} KeyValue;

typedef struct {
    KeyValue* entries;
    int size;
    int capacity;
} HashMap;

HashMap* createHashMap(int capacity) {
    HashMap* map = (HashMap*)malloc(sizeof(HashMap));
    map->entries = (KeyValue*)malloc(sizeof(KeyValue) * capacity);
    map->size = 0;
    map->capacity = capacity;
    return map;
}

void freeHashMap(HashMap* map) {
    free(map->entries);
    free(map);
}

void put(HashMap* map, char key, int value) {
    // 기존 키가 있는지 확인
    for (int i = 0; i < map->size; i++) {
        if (map->entries[i].key == key) {
            map->entries[i].value = value;
            return;
        }
    }
    
    // 새 키-값 쌍 추가
    if (map->size < map->capacity) {
        map->entries[map->size].key = key;
        map->entries[map->size].value = value;
        map->size++;
    }
}

int get(HashMap* map, char key, int defaultValue) {
    for (int i = 0; i < map->size; i++) {
        if (map->entries[i].key == key) {
            return map->entries[i].value;
        }
    }
    return defaultValue;
}

// HashSet 구현을 위한 구조체 및 함수
typedef struct {
    char* elements;
    int size;
    int capacity;
} HashSet;

HashSet* createHashSet(int capacity) {
    HashSet* set = (HashSet*)malloc(sizeof(HashSet));
    set->elements = (char*)malloc(sizeof(char) * capacity);
    set->size = 0;
    set->capacity = capacity;
    return set;
}

void freeHashSet(HashSet* set) {
    free(set->elements);
    free(set);
}

int add(HashSet* set, char element) {
    // 이미 존재하는지 확인
    for (int i = 0; i < set->size; i++) {
        if (set->elements[i] == element) {
            return 0; // 이미 존재함
        }
    }
    
    // 새 요소 추가
    if (set->size < set->capacity) {
        set->elements[set->size] = element;
        set->size++;
        return 1; // 성공적으로 추가됨
    }
    return 0; // 용량 부족
}

// Kotlin scoreWord 함수의 C 구현
JNIEXPORT jint JNICALL Java_solo_optimization_wordle_WordleJniKt_scoreWordNative(
    JNIEnv* env, 
    jclass cls, 
    jstring word, 
    jobjectArray possibleWords, 
    jint possibleWordsSize
) {
    const char* wordStr = (*env)->GetStringUTFChars(env, word, NULL);
    int wordLen = (*env)->GetStringUTFLength(env, word);
    
    // 글자 빈도수 계산
    HashMap* letterFrequency = createHashMap(26); // 알파벳 개수
    
    for (int i = 0; i < possibleWordsSize; i++) {
        jstring possibleWord = (jstring)(*env)->GetObjectArrayElement(env, possibleWords, i);
        const char* possibleWordStr = (*env)->GetStringUTFChars(env, possibleWord, NULL);
        int possibleWordLen = (*env)->GetStringUTFLength(env, possibleWord);
        
        for (int j = 0; j < possibleWordLen; j++) {
            char c = possibleWordStr[j];
            int current = get(letterFrequency, c, 0);
            put(letterFrequency, c, current + 1);
        }
        
        (*env)->ReleaseStringUTFChars(env, possibleWord, possibleWordStr);
        (*env)->DeleteLocalRef(env, possibleWord);
    }
    
    // 중복되지 않은 글자에 대한 점수 부여
    HashSet* uniqueLetters = createHashSet(26);
    int score = 0;
    
    for (int i = 0; i < wordLen; i++) {
        char c = wordStr[i];
        if (add(uniqueLetters, c)) {
            score += get(letterFrequency, c, 0);
        } else {
            // 중복 글자는 점수를 낮춤
            score -= get(letterFrequency, c, 0) / 2;
        }
    }
    
    (*env)->ReleaseStringUTFChars(env, word, wordStr);
    freeHashMap(letterFrequency);
    freeHashSet(uniqueLetters);
    
    return score;
}
"""
            
            val cFile = File(tempDir, "WordleJni.c")
            cFile.writeText(cCode)
            
            // 운영체제에 맞는 컴파일 명령어 실행
            val osName = System.getProperty("os.name").lowercase()
            val libName = if (osName.contains("win")) "WordleJni.dll" else "libWordleJni.so"
            val javaHome = System.getProperty("java.home")
            
            val compileCommand = if (osName.contains("win")) {
                "gcc -shared -o ${tempDir.absolutePath}/$libName ${cFile.absolutePath} -I\"$javaHome\\include\" -I\"$javaHome\\include\\win32\""
            } else {
                "gcc -shared -fPIC -o ${tempDir.absolutePath}/$libName ${cFile.absolutePath} -I$javaHome/include -I$javaHome/include/linux"
            }
            
            val process = Runtime.getRuntime().exec(compileCommand)
            process.waitFor()
            
            if (process.exitValue() != 0) {
                val error = process.errorStream.bufferedReader().readText()
                System.err.println("C 코드 컴파일 실패: $error")
                return false
            }
            
            // 컴파일된 라이브러리 로드
            System.load(File(tempDir, libName).absolutePath)
            nativeLibraryLoaded = true
            return true
            
        } catch (e: Exception) {
            System.err.println("네이티브 라이브러리 로드 실패: ${e.message}")
            e.printStackTrace()
            return false
        }
    }
}

fun main() {
    // 네이티브 라이브러리 로드 시도
    NativeHelper.loadNativeLibrary()
    
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
    try {
        // JNI를 통한 네이티브 구현 호출 시도
        return scoreWordNative(word, possibleWords.toTypedArray(), possibleWords.size)
    } catch (e: UnsatisfiedLinkError) {
        System.err.println("네이티브 함수 호출 실패, Kotlin 구현으로 대체: ${e.message}")
        
        // 원래 Kotlin 구현을 폴백으로 사용
        val letterFrequency = HashMap<Char, Int>()
        for (w in possibleWords) {
            for (c in w) {
                letterFrequency[c] = letterFrequency.getOrDefault(c, 0) + 1
            }
        }
        
        val uniqueLetters = HashSet<Char>()
        var score = 0
        
        for (c in word) {
            if (uniqueLetters.add(c)) {
                score += letterFrequency.getOrDefault(c, 0)
            } else {
                score -= letterFrequency.getOrDefault(c, 0) / 2
            }
        }
        
        return score
    }
}
