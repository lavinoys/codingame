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
    int laps;
    scanf("%d", &laps);
    int checkpoint_count;
    scanf("%d", &checkpoint_count);
    for (int i = 0; i < checkpoint_count; i++) {
        int checkpoint_x;
        int checkpoint_y;
        scanf("%d%d", &checkpoint_x, &checkpoint_y);
    }

    // game loop
    while (1) {
        for (int i = 0; i < 2; i++) {
            // x position of your pod
            int x;
            // y position of your pod
            int y;
            // x speed of your pod
            int vx;
            // y speed of your pod
            int vy;
            // angle of your pod
            int angle;
            // next check point id of your pod
            int next_check_point_id;
            scanf("%d%d%d%d%d%d", &x, &y, &vx, &vy, &angle, &next_check_point_id);
        }
        for (int i = 0; i < 2; i++) {
            // x position of the opponent's pod
            int x_2;
            // y position of the opponent's pod
            int y_2;
            // x speed of the opponent's pod
            int vx_2;
            // y speed of the opponent's pod
            int vy_2;
            // angle of the opponent's pod
            int angle_2;
            // next check point id of the opponent's pod
            int next_check_point_id_2;
            scanf("%d%d%d%d%d%d", &x_2, &y_2, &vx_2, &vy_2, &angle_2, &next_check_point_id_2);
        }

        // Write an action using printf(). DON'T FORGET THE TRAILING \n
        // To debug: fprintf(stderr, "Debug messages...\n");


        // You have to output the target position
        // followed by the power (0 <= thrust <= 100)
        // i.e.: "x y thrust"
        printf("8000 4500 100\n");
        printf("8000 4500 100\n");
    }

    return 0;
}