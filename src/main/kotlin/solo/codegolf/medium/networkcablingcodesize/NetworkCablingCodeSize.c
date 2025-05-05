#include <stdlib.h>
#include <stdio.h>

int compare(const void *a, const void *b) {
    return *(long long*)a - *(long long*)b;
}

int main() {
    int N;
    scanf("%d", &N);

    long long *x = malloc(N * sizeof(long long));
    long long *y = malloc(N * sizeof(long long));
    long long min_x = 1LL << 60, max_x = -(1LL << 60);

    for (int i = 0; i < N; i++) {
        scanf("%lld %lld", &x[i], &y[i]);
        if (x[i] < min_x) min_x = x[i];
        if (x[i] > max_x) max_x = x[i];
    }

    // 메인 케이블 길이 계산
    long long main_cable_length = max_x - min_x;

    // y좌표를 정렬하여 중앙값 찾기
    qsort(y, N, sizeof(long long), compare);
    long long median_y = N % 2 == 1 ? y[N/2] : y[N/2-1];

    // 각 건물에서 메인 케이블까지의 거리 계산
    long long vertical_length = 0;
    for (int i = 0; i < N; i++) {
        vertical_length += llabs(y[i] - median_y);
    }

    printf("%lld\n", main_cable_length + vertical_length);

    free(x);
    free(y);
    return 0;
}