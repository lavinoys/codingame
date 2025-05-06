#include <stdlib.h>
#include <stdio.h>
#include <string.h>
#include <stdbool.h>

int main()
{
    int N;
    scanf("%d", &N);
    
    int evenSum = 0;  // 짝수의 합
    int oddSum = 0;   // 홀수의 합
    
    for (int i = 0; i < N; i++) {
        int M;
        scanf("%d", &M);
        
        // 2로 나눈 나머지로 짝수/홀수 판별
        if (M % 2 == 0) {
            evenSum += M;  // 짝수인 경우
        } else {
            oddSum += M;   // 홀수인 경우
        }
    }

    printf("%d %d\n", evenSum, oddSum);

    return 0;
}