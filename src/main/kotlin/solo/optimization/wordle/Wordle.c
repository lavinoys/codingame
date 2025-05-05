#include <stdlib.h>
#include <stdio.h>
#include <string.h>
#include <stdbool.h>

/**
 * Made by Tanvir
 **/

#define MAX_WORDS 15000
#define WORD_LENGTH 6

// 단어 목록 저장
char word_set[MAX_WORDS][WORD_LENGTH + 1];
char possible_words[MAX_WORDS][WORD_LENGTH + 1];
int possible_count = 0;
int total_word_count = 0;

// 이전 추측과 상태 기록
char last_guess[WORD_LENGTH + 1];
int last_state[WORD_LENGTH];
int turn_count = 0;

// 글자 빈도수를 계산하는 함수
typedef struct {
    char letter;
    int frequency;
} LetterFreq;

// 단어가 주어진 상태 정보와 일치하는지 확인
bool matches_constraints(const char* word) {
    // 첫 번째 추측이라면 모든 단어가 가능함
    if (turn_count == 0) {
        return true;
    }
    
    // 주어진 단어가 이전 추측과 상태 정보에 맞는지 검사
    
    // 1. 상태 3(정확한 위치)부터 검사
    for (int i = 0; i < WORD_LENGTH; i++) {
        if (last_state[i] == 3 && word[i] != last_guess[i]) {
            return false;  // 정확한 위치에 맞는 글자가 없으면 제외
        }
    }
    
    // 2. 상태 2(다른 위치에 있음) 검사
    for (int i = 0; i < WORD_LENGTH; i++) {
        if (last_state[i] == 2) {
            // 같은 위치에 같은 글자가 있으면 안됨
            if (word[i] == last_guess[i]) {
                return false;
            }
            
            // 단어에 해당 글자가 있는지 확인
            bool found = false;
            for (int j = 0; j < WORD_LENGTH; j++) {
                if (j != i && word[j] == last_guess[i]) {
                    found = true;
                    break;
                }
            }
            
            if (!found) {
                return false;  // 해당 글자가 단어에 없으면 제외
            }
        }
    }
    
    // 3. 상태 1(단어에 없음) 검사 - 글자 개수 고려
    for (int i = 0; i < WORD_LENGTH; i++) {
        if (last_state[i] == 1) {
            char c = last_guess[i];
            
            // 같은 글자가 상태 2나 3으로 있는지 확인
            int expected_count = 0;
            for (int j = 0; j < WORD_LENGTH; j++) {
                if (last_guess[j] == c && (last_state[j] == 2 || last_state[j] == 3)) {
                    expected_count++;
                }
            }
            
            // 실제 단어에서 해당 글자의 개수 확인
            int actual_count = 0;
            for (int j = 0; j < WORD_LENGTH; j++) {
                if (word[j] == c) {
                    actual_count++;
                }
            }
            
            // 글자가 없어야 하거나(expected_count=0), 정확히 expected_count만큼만 있어야 함
            if (expected_count == 0 && actual_count > 0) {
                return false;
            } else if (expected_count > 0 && actual_count > expected_count) {
                return false;
            }
        }
    }
    
    return true;
}

// 가능한 단어 목록 업데이트
void update_possible_words() {
    int new_count = 0;
    
    for (int i = 0; i < possible_count; i++) {
        if (matches_constraints(possible_words[i])) {
            // 그대로 유지하고 인덱스 이동
            if (new_count != i) {
                strcpy(possible_words[new_count], possible_words[i]);
            }
            new_count++;
        }
    }
    
    possible_count = new_count;
    fprintf(stderr, "남은 가능한 단어: %d개\n", possible_count);
    
    // 디버깅: 일부 가능한 단어 출력 (최대 5개)
    int debug_count = possible_count < 5 ? possible_count : 5;
    for (int i = 0; i < debug_count; i++) {
        fprintf(stderr, "가능한 단어 #%d: %s\n", i+1, possible_words[i]);
    }
}

// 단어에서 각 글자의 정보 획득 가치 계산
double calculate_word_value(const char* word) {
    // 간단한 휴리스틱: 남은 가능한 단어에 있는 글자의 빈도수를 기준으로 점수 계산
    int letter_count[26] = {0};
    
    // 남은 가능한 단어들에서 각 글자의 빈도수 계산
    for (int i = 0; i < possible_count; i++) {
        bool used[26] = {false};
        for (int j = 0; j < WORD_LENGTH; j++) {
            int idx = possible_words[i][j] - 'A';
            if (!used[idx]) {
                letter_count[idx]++;
                used[idx] = true;
            }
        }
    }
    
    // 단어의 가치 계산: 단어에 있는 독특한 글자의 빈도 합
    double value = 0;
    bool used[26] = {false};
    
    for (int i = 0; i < WORD_LENGTH; i++) {
        int idx = word[i] - 'A';
        if (!used[idx]) {
            value += (double)letter_count[idx] / possible_count;
            used[idx] = true;
        }
    }
    
    return value;
}

// 최적의 추측 단어 선택
void choose_guess(char* guess) {
    if (turn_count == 0) {
        // 첫 단어는 정보 획득이 좋은 단어 선택
        strcpy(guess, "FAULTS");  // 많은 자음과 모음이 포함된 6글자 단어
        return;
    }
    
    // 가능한 단어가 1개만 남았으면 그 단어 선택
    if (possible_count == 1) {
        strcpy(guess, possible_words[0]);
        return;
    }
    
    // 가능한 단어가 2개만 남았으면 첫 번째 선택
    if (possible_count == 2) {
        strcpy(guess, possible_words[0]);
        return;
    }
    
    // 정보 획득이 최대인 단어 선택
    double best_value = -1;
    int best_index = 0;
    
    // 가능한 단어 중 일부만 평가 (시간 제한 고려)
    int eval_limit = possible_count < 1000 ? possible_count : 1000;
    
    for (int i = 0; i < eval_limit; i++) {
        double value = calculate_word_value(possible_words[i]);
        if (value > best_value) {
            best_value = value;
            best_index = i;
        }
    }
    
    strcpy(guess, possible_words[best_index]);
}

int main()
{
    // Number of words in the word set
    int word_count;
    scanf("%d", &word_count);
    total_word_count = word_count;
    
    for (int i = 0; i < word_count; i++) {
        // Word in the word set
        scanf("%s", word_set[i]);
        strcpy(possible_words[i], word_set[i]);  // 초기에는 모든 단어가 가능함
    }
    
    possible_count = word_count;
    memset(last_guess, 0, sizeof(last_guess));
    turn_count = 0;

    // game loop
    while (1) {
        for (int i = 0; i < WORD_LENGTH; i++) {
            // State of the letter of the corresponding position of previous guess
            scanf("%d", &last_state[i]);
        }

        // 가능한 단어 목록 업데이트 (첫 턴이 아닌 경우)
        if (turn_count > 0) {
            update_possible_words();
        }

        // 다음 추측 선택
        char guess[WORD_LENGTH + 1];
        choose_guess(guess);
        
        // 추측 저장 및 출력
        strcpy(last_guess, guess);
        printf("%s\n", guess);
        fflush(stdout); // 출력 버퍼 비우기
        
        // 디버그 정보 출력
        fprintf(stderr, "턴 %d: 추측 = %s\n", turn_count + 1, guess);
        
        turn_count++;
    }

    return 0;
}
