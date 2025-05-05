#include <stdlib.h>
#include <stdio.h>
#include <string.h>
#include <stdbool.h>

/**
 * Find the sum of the first n odd integers!
 * (e.g. n=4: 1+3+5+7=16)
 **/

int main()
{
    // the number of odd integers
    int n;
    scanf("%d", &n);

    // 첫 n개의 홀수의 합은 n²와 같습니다
    long long result = (long long)n * n;

    printf("%lld\n", result);

    return 0;
}