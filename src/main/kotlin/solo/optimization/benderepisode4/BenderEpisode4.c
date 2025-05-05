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
    int width;
    int height;
    scanf("%d%d", &width, &height); fgetc(stdin);
    for (int i = 0; i < height; i++) {
        char line[51];
        scanf("%[^\n]", line); fgetc(stdin);
    }
    int start_x;
    int start_y;
    scanf("%d%d", &start_x, &start_y);
    int target_x;
    int target_y;
    scanf("%d%d", &target_x, &target_y);
    int switch_count;
    scanf("%d", &switch_count);
    for (int i = 0; i < switch_count; i++) {
        int switch_x;
        int switch_y;
        int block_x;
        int block_y;
        // 1 if blocking, 0 otherwise
        int initial_state;
        scanf("%d%d%d%d%d", &switch_x, &switch_y, &block_x, &block_y, &initial_state);
    }

    // Write an action using printf(). DON'T FORGET THE TRAILING \n
    // To debug: fprintf(stderr, "Debug messages...\n");

    printf("RRRRDDDDRRRRUUUULRUUUUUUUUUUUUUDDUDDDDDDDDDDDDDDDDLLLLUUUUUUUULLUULLRRDDRRDDDDDDDDLLLLLLUULLUUUULLUUUUUUL\n");

    return 0;
}