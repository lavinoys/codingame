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
    int length;
    int width;
    int height;
    scanf("%d%d%d", &length, &width, &height);

    if (length <= 0 || width <= 0 || height <= 0) {
        printf("Invalid dimension\n");
    } else {
        int volume = length * width * height;
        printf("The quantity of water in the room is %d cubic feet.\n", volume);
    }

    return 0;
}
