#include <stdlib.h>
#include <stdio.h>
#include <string.h>
#include <stdbool.h>

/**
 * Challenge yourself with this classic NP-Hard optimization problem !
 **/

int main()
{
    // The number of customers
    int n;
    scanf("%d", &n);
    // The capacity of the vehicles
    int c;
    scanf("%d", &c);
    for (int i = 0; i < n; i++) {
        // The index of the customer (0 is the depot)
        int index;
        // The x coordinate of the customer
        int x;
        // The y coordinate of the customer
        int y;
        // The demand
        int demand;
        scanf("%d%d%d%d", &index, &x, &y, &demand);
    }

    // Write an action using printf(). DON'T FORGET THE TRAILING \n
    // To debug: fprintf(stderr, "Debug messages...\n");


    // A single line containing the tours separated by a semicolon (;)
    // Each tour must be the indices of the customers separated by a space ( )
    // The depot (0) should not be included in the output
    printf("1 2 3;4\n");

    return 0;
}