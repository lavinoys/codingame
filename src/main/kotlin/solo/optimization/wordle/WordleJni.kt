import java.util.*
import java.io.*
import java.nio.file.*

/**
 * Wordle Solver with JNI optimization
 * All-in-one file (Kotlin + C)
 */
class WordleJni {
    companion object {
        const val WORD_LENGTH = 6
        const val MAX_TURNS = 26
        const val MAX_WORDS = 15000
        
        // 동적 라이브러리 생성 및 로드
        init {
            try {
                // C 소스 코드 (인라인으로 포함)
                val cSource = """
#include <jni.h>
#include <stdio.h>
#include <string.h>
#include <stdlib.h>
#include <stdbool.h>

#define WORD_LENGTH 6
#define MAX_WORDS 15000

// 단어 목록 저장
static char word_set[MAX_WORDS][WORD_LENGTH + 1];
static char possible_words[MAX_WORDS][WORD_LENGTH + 1];
static int word_count = 0;
static int possible_count = 0;

// 이전 추측과 상태
static char last_guess[WORD_LENGTH + 1];
static int last_state[WORD_LENGTH];
static int turn_count = 0;

// 글자별 제약조건 저장
typedef struct {
    int min_count;
    int max_count;
    bool forbidden_pos[WORD_LENGTH];
    bool required_pos[WORD_LENGTH];
} LetterConstraint;

static LetterConstraint letter_constraints[26];

// 디버그 정보
static char debug_info[1024];

// 알파벳 인덱스 반환 (A=0, B=1, ...)
static int letter_index(char c) {
    return c - 'A';
}

// 제약조건 초기화
static void init_constraints() {
    for (int i = 0; i < 26; i++) {
        letter_constraints[i].min_count = 0;
        letter_constraints[i].max_count = WORD_LENGTH;
        
        for (int j = 0; j < WORD_LENGTH; j++) {
            letter_constraints[i].forbidden_pos[j] = false;
            letter_constraints[i].required_pos[j] = false;
        }
    }
}

// 단어가 제약조건을 만족하는지 확인
static bool word_matches_constraints(const char* word) {
    // 첫 턴이면 모든 단어가 가능
    if (turn_count == 0) return true;
    
    // 글자별 개수 계산
    int letter_counts[26] = {0};
    for (int i = 0; i < WORD_LENGTH; i++) {
        letter_counts[letter_index(word[i])]++;
    }
    
    // 제약조건 검사
    for (int i = 0; i < 26; i++) {
        // 최소/최대 개수 제약 검사
        if (letter_counts[i] < letter_constraints[i].min_count || 
            letter_counts[i] > letter_constraints[i].max_count) {
            return false;
        }
        
        // 위치 제약 검사
        for (int j = 0; j < WORD_LENGTH; j++) {
            char letter = 'A' + i;
            
            // 필수 위치에 글자가 없으면 실패
            if (letter_constraints[i].required_pos[j] && word[j] != letter) {
                return false;
            }
            
            // 금지된 위치에 글자가 있으면 실패
            if (letter_constraints[i].forbidden_pos[j] && word[j] == letter) {
                return false;
            }
        }
    }
    
    return true;
}

// 상태 정보를 바탕으로 제약조건 업데이트
static void update_constraints() {
    // 글자별 개수 계산
    int guess_letter_counts[26] = {0};
    for (int i = 0; i < WORD_LENGTH; i++) {
        guess_letter_counts[letter_index(last_guess[i])]++;
    }
    
    // 확정 위치 (상태 3) 처리
    for (int i = 0; i < WORD_LENGTH; i++) {
        if (last_state[i] == 3) {
            char letter = last_guess[i];
            int idx = letter_index(letter);
            
            // 필수 위치 표시
            letter_constraints[idx].required_pos[i] = true;
            
            // 최소 개수 업데이트
            int correct_count = 0;
            for (int j = 0; j < WORD_LENGTH; j++) {
                if (last_state[j] == 3 && last_guess[j] == letter) {
                    correct_count++;
                }
            }
            
            if (correct_count > letter_constraints[idx].min_count) {
                letter_constraints[idx].min_count = correct_count;
            }
        }
    }
    
    // 다른 위치 (상태 2) 처리
    for (int i = 0; i < WORD_LENGTH; i++) {
        if (last_state[i] == 2) {
            char letter = last_guess[i];
            int idx = letter_index(letter);
            
            // 현재 위치에 같은 글자 금지
            letter_constraints[idx].forbidden_pos[i] = true;
            
            // 최소 개수 업데이트
            int min_count = 0;
            for (int j = 0; j < WORD_LENGTH; j++) {
                if ((last_state[j] == 2 || last_state[j] == 3) && last_guess[j] == letter) {
                    min_count++;
                }
            }
            
            if (min_count > letter_constraints[idx].min_count) {
                letter_constraints[idx].min_count = min_count;
            }
        }
    }
    
    // 없는 글자 (상태 1) 처리
    for (int i = 0; i < WORD_LENGTH; i++) {
        if (last_state[i] == 1) {
            char letter = last_guess[i];
            int idx = letter_index(letter);
            
            // 금지 위치 표시
            letter_constraints[idx].forbidden_pos[i] = true;
            
            // 최대 개수 업데이트 (상태 2나 3으로 확인된 개수가 최대)
            int confirmed_count = 0;
            for (int j = 0; j < WORD_LENGTH; j++) {
                if ((last_state[j] == 2 || last_state[j] == 3) && last_guess[j] == letter) {
                    confirmed_count++;
                }
            }
            
            letter_constraints[idx].max_count = confirmed_count;
        }
    }
}

// 가능한 단어 목록 필터링
static void filter_words() {
    int new_count = 0;
    
    for (int i = 0; i < possible_count; i++) {
        if (word_matches_constraints(possible_words[i])) {
            if (i != new_count) {
                strcpy(possible_words[new_count], possible_words[i]);
            }
            new_count++;
        }
    }
    
    possible_count = new_count;
}

// 단어의 정보 가치 계산 (휴리스틱)
static double calculate_word_value(const char* word) {
    // 글자 빈도 계산
    int letter_freq[26] = {0};
    bool letter_used[26] = {0};
    
    // 가능한 단어에서 글자 빈도 계산
    for (int i = 0; i < possible_count; i++) {
        memset(letter_used, 0, sizeof(letter_used));
        
        for (int j = 0; j < WORD_LENGTH; j++) {
            int idx = letter_index(possible_words[i][j]);
            if (!letter_used[idx]) {
                letter_freq[idx]++;
                letter_used[idx] = true;
            }
        }
    }
    
    // 단어의 점수 계산
    double score = 0.0;
    memset(letter_used, 0, sizeof(letter_used));
    
    for (int i = 0; i < WORD_LENGTH; i++) {
        int idx = letter_index(word[i]);
        
        // 중복되지 않은 글자의 점수만 합산
        if (!letter_used[idx]) {
            score += (double)letter_freq[idx] / possible_count;
            letter_used[idx] = true;
        }
    }
    
    // 가능한 답 목록에 있으면 가중치 증가
    for (int i = 0; i < possible_count; i++) {
        if (strcmp(word, possible_words[i]) == 0) {
            score *= 1.5;
            break;
        }
    }
    
    return score;
}

// 최적의 추측 선택
static void select_best_guess(char* guess) {
    // 첫 턴이면 고정 단어
    if (turn_count == 0) {
        strcpy(guess, "SOAPY");  // 통계적으로 좋은 시작 단어
        return;
    }
    
    // 가능한 단어가 적으면 첫번째 선택
    if (possible_count <= 2) {
        strcpy(guess, possible_words[0]);
        return;
    }
    
    // 단어 점수 계산 및 최고 점수 단어 선택
    double best_score = -1;
    int best_index = 0;
    
    // 평가할 단어 수 제한 (시간 제약 고려)
    int limit = (word_count < 500) ? word_count : 500;
    
    for (int i = 0; i < limit; i++) {
        double score = calculate_word_value(word_set[i]);
        
        if (score > best_score) {
            best_score = score;
            best_index = i;
        }
    }
    
    strcpy(guess, word_set[best_index]);
}

// 디버그 정보 생성
static void generate_debug_info() {
    sprintf(debug_info, "턴: %d, 후보단어 수: %d", turn_count + 1, possible_count);
    
    if (possible_count > 0 && possible_count <= 5) {
        strcat(debug_info, " [");
        for (int i = 0; i < possible_count; i++) {
            if (i > 0) strcat(debug_info, ", ");
            strcat(debug_info, possible_words[i]);
        }
        strcat(debug_info, "]");
    }
}

// ====== JNI 함수 구현 ======

JNIEXPORT jboolean JNICALL Java_WordleJni_00024Companion_initializeWords
  (JNIEnv *env, jobject obj, jobjectArray wordArray) {
    // 초기화
    turn_count = 0;
    init_constraints();
    memset(last_guess, 0, sizeof(last_guess));
    
    // 단어 목록 크기 확인
    jsize len = (*env)->GetArrayLength(env, wordArray);
    if (len > MAX_WORDS) {
        return JNI_FALSE;
    }
    
    word_count = (int)len;
    possible_count = word_count;
    
    // 단어 복사
    for (int i = 0; i < word_count; i++) {
        jstring jword = (jstring)(*env)->GetObjectArrayElement(env, wordArray, i);
        const char *cword = (*env)->GetStringUTFChars(env, jword, NULL);
        
        strcpy(word_set[i], cword);
        strcpy(possible_words[i], cword);
        
        (*env)->ReleaseStringUTFChars(env, jword, cword);
        (*env)->DeleteLocalRef(env, jword);
    }
    
    return JNI_TRUE;
}

JNIEXPORT jboolean JNICALL Java_WordleJni_00024Companion_processState
  (JNIEnv *env, jobject obj, jintArray stateArray) {
    // 상태 배열 복사
    jint *states = (*env)->GetIntArrayElements(env, stateArray, NULL);
    
    for (int i = 0; i < WORD_LENGTH; i++) {
        last_state[i] = (int)states[i];
    }
    
    (*env)->ReleaseIntArrayElements(env, stateArray, states, JNI_ABORT);
    
    // 제약조건 업데이트 및 단어 필터링
    update_constraints();
    filter_words();
    
    return JNI_TRUE;
}

JNIEXPORT jstring JNICALL Java_WordleJni_00024Companion_getNextGuess
  (JNIEnv *env, jobject obj) {
    // 다음 추측 선택
    select_best_guess(last_guess);
    
    // 디버그 정보 생성
    generate_debug_info();
    
    // 턴 증가
    turn_count++;
    
    return (*env)->NewStringUTF(env, last_guess);
}

JNIEXPORT jint JNICALL Java_WordleJni_00024Companion_getPossibleWordsCount
  (JNIEnv *env, jobject obj) {
    return (jint)possible_count;
}

JNIEXPORT jstring JNICALL Java_WordleJni_00024Companion_getDebugInfo
  (JNIEnv *env, jobject obj) {
    return (*env)->NewStringUTF(env, debug_info);
}
                """.trimIndent()
                
                // C 소스 코드를 임시 파일로 저장
                val tmpDir = Files.createTempDirectory("wordlejni")
                val sourceFile = tmpDir.resolve("WordleJni.c").toFile()
                sourceFile.writeText(cSource)
                
                // 헤더 파일 생성
                val headerFile = tmpDir.resolve("WordleJni.h").toFile()
                headerFile.writeText("""
#include <jni.h>

#ifndef _WORDLEJNI_H
#define _WORDLEJNI_H

#ifdef __cplusplus
extern "C" {
#endif

JNIEXPORT jboolean JNICALL Java_WordleJni_00024Companion_initializeWords
  (JNIEnv *, jobject, jobjectArray);
  
JNIEXPORT jboolean JNICALL Java_WordleJni_00024Companion_processState
  (JNIEnv *, jobject, jintArray);
  
JNIEXPORT jstring JNICALL Java_WordleJni_00024Companion_getNextGuess
  (JNIEnv *, jobject);
  
JNIEXPORT jint JNICALL Java_WordleJni_00024Companion_getPossibleWordsCount
  (JNIEnv *, jobject);
  
JNIEXPORT jstring JNICALL Java_WordleJni_00024Companion_getDebugInfo
  (JNIEnv *, jobject);

#ifdef __cplusplus
}
#endif
#endif
                """.trimIndent())
                
                // 로컬 시스템에서 컴파일하기
                val libName = if (System.getProperty("os.name").lowercase().contains("win"))
                    "wordlejni.dll" else "libwordlejni.so"
                
                val command = if (System.getProperty("os.name").lowercase().contains("win")) {
                    arrayOf("gcc", "-shared", "-o", 
                            "${tmpDir.resolve(libName)}", 
                            "-I${System.getProperty("java.home")}/../include", 
                            "-I${System.getProperty("java.home")}/../include/win32", 
                            sourceFile.absolutePath)
                } else {
                    arrayOf("gcc", "-shared", "-fPIC", "-o", 
                            "${tmpDir.resolve(libName)}", 
                            "-I${System.getProperty("java.home")}/../include", 
                            "-I${System.getProperty("java.home")}/../include/linux", 
                            sourceFile.absolutePath)
                }
                
                val process = ProcessBuilder(*command)
                    .redirectError(ProcessBuilder.Redirect.INHERIT)
                    .start()
                
                if (process.waitFor() != 0) {
                    throw RuntimeException("라이브러리 컴파일 실패")
                }
                
                // 컴파일된 라이브러리 로드
                System.load(tmpDir.resolve(libName).toString())
                System.err.println("JNI 라이브러리 로드 완료")
            } catch (e: Exception) {
                System.err.println("JNI 라이브러리 초기화 실패: ${e.message}")
                e.printStackTrace()
            }
        }
        
        // JNI 네이티브 메서드
        @JvmStatic external fun initializeWords(words: Array<String>): Boolean
        @JvmStatic external fun processState(states: IntArray): Boolean
        @JvmStatic external fun getNextGuess(): String
        @JvmStatic external fun getPossibleWordsCount(): Int
        @JvmStatic external fun getDebugInfo(): String
    }
}

// 메인 솔버 클래스
class WordleSolver {
    private var turnCount = 0
    private lateinit var wordList: Array<String>
    
    // 단어 목록 초기화
    fun initialize(words: List<String>): Boolean {
        wordList = words.toTypedArray()
        turnCount = 0
        return WordleJni.initializeWords(wordList)
    }
    
    // 상태 처리 후 다음 추측 반환
    fun getNextGuess(states: IntArray): String {
        // 첫 턴이 아니면 이전 상태 업데이트
        if (turnCount > 0) {
            if (!WordleJni.processState(states)) {
                System.err.println("상태 처리 실패")
            }
        }
        
        // 디버그 정보 출력
        val possibleCount = WordleJni.getPossibleWordsCount()
        val debugInfo = WordleJni.getDebugInfo()
        System.err.println("턴 ${turnCount + 1}: 남은 단어 수: $possibleCount")
        System.err.println(debugInfo)
        
        // 다음 추측 가져오기
        val guess = WordleJni.getNextGuess()
        turnCount++
        
        return guess
    }
}

fun main(args: Array<String>) {
    val input = Scanner(System.`in`)
    val wordCount = input.nextInt() // 단어 목록의 단어 수
    
    // 단어 목록 읽기
    val words = mutableListOf<String>()
    for (i in 0 until wordCount) {
        words.add(input.next()) // 단어 목록의 단어
    }
    
    // 솔버 초기화
    val solver = WordleSolver()
    solver.initialize(words)
    
    // 게임 루프
    while (true) {
        // 상태 정보 읽기
        val states = IntArray(WordleJni.WORD_LENGTH)
        for (i in 0 until WordleJni.WORD_LENGTH) {
            states[i] = input.nextInt() // 이전 추측의 각 위치별 글자 상태
        }
        
        // 다음 추측 계산 및 출력
        val guess = solver.getNextGuess(states)
        println(guess)
    }
}
