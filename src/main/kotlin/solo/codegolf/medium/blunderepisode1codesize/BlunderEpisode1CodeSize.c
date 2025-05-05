#include <stdlib.h>
#include <stdio.h>
#include <string.h>
#include <stdbool.h>

/**
 * Auto-generated code below aims at helping you parse
 * the standard input according to the problem statement.
 **/

int main()
{
    int L;
    int C;
    scanf("%d%d", &L, &C); fgetc(stdin);
    for (int i = 0; i < L; i++) {
        char row[102];
        scanf("%[^\n]", row); fgetc(stdin);
    }

    // Write an answer using printf(). DON'T FORGET THE TRAILING \n
    // To debug: fprintf(stderr, "Debug messages...\n");

    printf("answer\n");

    return 0;
}