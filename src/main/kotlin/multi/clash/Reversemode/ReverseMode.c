#include <stdlib.h>
#include <stdio.h>
#include <string.h>
#include <stdbool.h>

int main()
{
    char n[257];
    scanf("%[^\n]", n);

    int len = strlen(n);
    char result[257] = {0};

    // 각 자릿수를 9에서 빼기
    for (int i = 0; i < len; i++) {
        int digit = n[i] - '0';
        result[len - 1 - i] = '0' + (9 - digit);
    }
    result[len] = '\0';

    // 맨 앞의 0 제거 (첫 번째 0이 아닌 숫자 찾기)
    int j = 0;
    while (result[j] == '0' && j < len - 1) {
        j++;
    }

    printf("%s\n", &result[j]);

    return 0;
}