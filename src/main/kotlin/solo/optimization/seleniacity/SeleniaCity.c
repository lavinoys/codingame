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

    // game loop
    while (1) {
        int resources;
        scanf("%d", &resources);
        int num_travel_routes;
        scanf("%d", &num_travel_routes);
        for (int i = 0; i < num_travel_routes; i++) {
            int building_id_1;
            int building_id_2;
            int capacity;
            scanf("%d%d%d", &building_id_1, &building_id_2, &capacity);
        }
        int num_pods;
        scanf("%d", &num_pods); fgetc(stdin);
        for (int i = 0; i < num_pods; i++) {
            char pod_properties[201];
            scanf("%[^\n]", pod_properties); fgetc(stdin);
        }
        int num_new_buildings;
        scanf("%d", &num_new_buildings); fgetc(stdin);
        for (int i = 0; i < num_new_buildings; i++) {
            char building_properties[501];
            scanf("%[^\n]", building_properties); fgetc(stdin);
        }

        // Write an action using printf(). DON'T FORGET THE TRAILING \n
        // To debug: fprintf(stderr, "Debug messages...\n");

        printf("TUBE 0 1;TUBE 0 2;POD 42 0 1 0 2 0 1 0 2\n"); // TUBE | UPGRADE | TELEPORT | POD | DESTROY | WAIT
    }

    return 0;
}