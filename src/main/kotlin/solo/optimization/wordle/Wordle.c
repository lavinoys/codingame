#include <stdlib.h>
#include <stdio.h>
#include <string.h>
#include <stdbool.h>

/**
 * Made by Tanvir
 **/

int main()
{
    // Number of words in the word set
    int word_count;
    scanf("%d", &word_count);
    for (int i = 0; i < word_count; i++) {
        // Word in the word set
        char word[7];
        scanf("%s", word);
    }

    // game loop
    while (1) {
        for (int i = 0; i < 6; i++) {
            // State of the letter of the corresponding position of previous guess
            int state;
            scanf("%d", &state);
        }

        // Write an action using printf(). DON'T FORGET THE TRAILING \n
        // To debug: fprintf(stderr, "Debug messages...\n");

        printf("ANSWER\n");
    }

    return 0;
}