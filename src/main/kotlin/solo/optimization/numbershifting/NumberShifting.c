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

    // Write an action using printf(). DON'T FORGET THE TRAILING \n
    // To debug: fprintf(stderr, "Debug messages...\n");

    printf("first_level\n");

    // game loop
    while (1) {
        int width;
        int height;
        scanf("%d%d", &width, &height);
        for (int i = 0; i < height; i++) {
            for (int j = 0; j < width; j++) {
                int cell;
                scanf("%d", &cell);
            }
        }

        // Write an action using printf(). DON'T FORGET THE TRAILING \n
        // To debug: fprintf(stderr, "Debug messages...\n");

        printf("7 4 L +\n");
        printf("3 0 D -\n");
        printf("6 4 L -\n");
    }

    return 0;
}