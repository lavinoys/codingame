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
    // the number of relationships of influence
    int n;
    scanf("%d", &n);
    for (int i = 0; i < n; i++) {
        // a relationship of influence between two people (x influences y)
        int x;
        int y;
        scanf("%d%d", &x, &y);
    }

    // Write an answer using printf(). DON'T FORGET THE TRAILING \n
    // To debug: fprintf(stderr, "Debug messages...\n");


    // The number of people involved in the longest succession of influences
    printf("2\n");

    return 0;
}