#include <stdlib.h>
#include <stdio.h>
#include <string.h>
#include <stdbool.h>

/**
 * Made by Tanvir
 **/

#define MAX_WORDS 12000
#define WORD_LENGTH 6

// 단어 목록 저장
char word_set[MAX_WORDS][WORD_LENGTH + 1];
char possible_words[MAX_WORDS][WORD_LENGTH + 1];
int possible_count = 0;

// 이전 추측과 상태 기록
char last_guess[WORD_LENGTH + 1];
int last_state[WORD_LENGTH];

// 우선 시도할 초기 단어들 (좋은 출발점)
const char* starter_words[] = {"RAISE", "OCEAN", "LUCKY", "BLITZ", "PHONY"};

// 단어가 주어진 상태 정보와 일치하는지 확인
bool matches_constraints(const char* word) {
    // 첫 번째 추측이라면 모든 단어가 가능함
    if (last_guess[0] == '\0') {
        return true;
    }
    
    // 주어진 단어가 이전 추측과 상태 정보에 맞는지 검사
    int letter_counts[26] = {0}; // 대상 단어의 각 알파벳 개수
    
    // 단어에 있는 각 문자의 개수 계산
    for (int i = 0; i < WORD_LENGTH; i++) {
        letter_counts[word[i] - 'A']++;
    }
    
    for (int i = 0; i < WORD_LENGTH; i++) {
        char c = last_guess[i];
        int idx = c - 'A';
        
        // 상태 3: 해당 위치의 글자가 동일해야 함
        if (last_state[i] == 3) {
            if (word[i] != c) return false;
            letter_counts[idx]--;
        }
    }
    
    for (int i = 0; i < WORD_LENGTH; i++) {
        char c = last_guess[i];
        int idx = c - 'A';
        
        // 상태 2: 다른 위치에 해당 글자가 있어야 함
        if (last_state[i] == 2) {
            if (word[i] == c) return false; // 같은 위치에 있으면 안 됨
            if (letter_counts[idx] <= 0) return false; // 단어에 해당 글자가 없으면 안 됨
            letter_counts[idx]--;
        }
        // 상태 1: 해당 글자가 단어에 없어야 함 (같은 글자가 다른 위치에서 이미 확인된 경우는 예외)
        else if (last_state[i] == 1) {
            // 같은 글자가 다른 위치에서 상태 2나 3으로 확인된 경우를 처리
            bool exception = false;
            for (int j = 0; j < WORD_LENGTH; j++) {
                if (i != j && last_guess[j] == c && (last_state[j] == 2 || last_state[j] == 3)) {
                    exception = true;
                    break;
                }
            }
            
            if (!exception && letter_counts[idx] > 0) return false;
        }
    }
    
    return true;
}

// 가능한 단어 목록 업데이트
void update_possible_words(int word_count) {
    possible_count = 0;
    
    for (int i = 0; i < word_count; i++) {
        if (matches_constraints(word_set[i])) {
            strcpy(possible_words[possible_count++], word_set[i]);
        }
    }
}

// 최적의 추측 선택 (현재는 가능한 첫 번째 단어 선택)
void choose_optimal_guess(char* guess) {
    // 첫 번째 추측이라면 미리 정의된 시작 단어 사용
    if (last_guess[0] == '\0') {
        strcpy(guess, "RAISE");
        return;
    }
    
    // 가능한 단어가 하나만 남았다면 바로 선택
    if (possible_count == 1) {
        strcpy(guess, possible_words[0]);
        return;
    }
    
    // 가능한 단어 중 첫 번째 선택 (더 복잡한 전략으로 개선 가능)
    strcpy(guess, possible_words[0]);
}

int main()
{
    // 초기화
    memset(last_guess, 0, sizeof(last_guess));
    
    // Number of words in the word set
    int word_count;
    scanf("%d", &word_count);
    for (int i = 0; i < word_count; i++) {
        // Word in the word set
        scanf("%s", word_set[i]);
    }
    
    // 초기 가능한 단어 세트는 전체 단어 목록
    possible_count = word_count;
    for (int i = 0; i < word_count; i++) {
        strcpy(possible_words[i], word_set[i]);
    }
    
    // game loop
    while (1) {
        for (int i = 0; i < WORD_LENGTH; i++) {
            // State of the letter of the corresponding position of previous guess
            scanf("%d", &last_state[i]);
        }
        
        // 가능한 단어 업데이트
        update_possible_words(word_count);
        
        // 최적의 추측 선택
        char guess[WORD_LENGTH + 1];
        choose_optimal_guess(guess);
        
        // 추측 저장 및 출력
        strcpy(last_guess, guess);
        printf("%s\n", guess);
        
        // 디버그 정보 출력
        fprintf(stderr, "Remaining possible words: %d\n", possible_count);
    }

    return 0;
}
