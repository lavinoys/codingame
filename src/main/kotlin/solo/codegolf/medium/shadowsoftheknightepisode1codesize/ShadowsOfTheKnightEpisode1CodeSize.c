#include <stdlib.h>
#include <stdio.h>
#include <string.h>
#include <stdbool.h>

int main()
{
    int W, H, N, X0, Y0;
    scanf("%d%d", &W, &H);
    scanf("%d", &N);
    scanf("%d%d", &X0, &Y0);

    // 검색 범위 초기화
    int x_min = 0, x_max = W - 1;
    int y_min = 0, y_max = H - 1;
    int x = X0, y = Y0;

    while (1) {
        char bomb_dir[4];
        scanf("%s", bomb_dir);

        // 방향에 따라 검색 범위 업데이트
        if (strchr(bomb_dir, 'U')) {
            y_max = y - 1;
        } else if (strchr(bomb_dir, 'D')) {
            y_min = y + 1;
        }

        if (strchr(bomb_dir, 'L')) {
            x_max = x - 1;
        } else if (strchr(bomb_dir, 'R')) {
            x_min = x + 1;
        }

        // 이진 탐색으로 다음 위치 계산
        x = x_min + (x_max - x_min) / 2;
        y = y_min + (y_max - y_min) / 2;

        printf("%d %d\n", x, y);
    }

    return 0;
}