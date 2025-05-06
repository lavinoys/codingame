#include <stdlib.h>
#include <stdio.h>
#include <string.h>
#include <stdbool.h>

int main()
{
    int n;
    scanf("%d", &n);

    // n * (n * 100) 계산
    int result = n * (n * 100);

    printf("%d\n", result);

    return 0;
}