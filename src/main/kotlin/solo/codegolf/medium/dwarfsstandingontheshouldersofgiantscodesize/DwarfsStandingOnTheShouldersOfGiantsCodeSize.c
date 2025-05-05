#include <stdlib.h>
#include <stdio.h>
#include <string.h>
#include <stdbool.h>

#define MAX_NODES 10000

typedef struct Node {
    int id;
    struct Node* next;
} Node;

Node* graph[MAX_NODES + 1];
int memo[MAX_NODES + 1];
bool visited[MAX_NODES + 1];
int nodes_set[MAX_NODES + 1];

// 그래프에 간선 추가
void add_edge(int x, int y) {
    Node* newNode = (Node*)malloc(sizeof(Node));
    newNode->id = y;
    newNode->next = graph[x];
    graph[x] = newNode;

    // 노드 집합에 추가
    nodes_set[x] = 1;
    nodes_set[y] = 1;
}

// DFS로 최대 경로 길이 계산 (메모이제이션 사용)
int dfs(int node) {
    if (memo[node] != -1) {
        return memo[node];
    }

    int max_length = 0;
    Node* current = graph[node];

    while (current) {
        int length = dfs(current->id);
        if (length > max_length) {
            max_length = length;
        }
        current = current->next;
    }

    memo[node] = 1 + max_length; // 현재 노드 + 최대 하위 경로
    return memo[node];
}

int main() {
    // 초기화
    memset(graph, 0, sizeof(graph));
    memset(memo, -1, sizeof(memo));
    memset(nodes_set, 0, sizeof(nodes_set));

    // 입력 처리
    int n;
    scanf("%d", &n);

    for (int i = 0; i < n; i++) {
        int x, y;
        scanf("%d%d", &x, &y);
        add_edge(x, y);
    }

    // 모든 노드에서 시작하는 경로 중 가장 긴 경로 찾기
    int max_path_length = 0;
    for (int i = 1; i <= MAX_NODES; i++) {
        if (nodes_set[i]) {
            int length = dfs(i);
            if (length > max_path_length) {
                max_path_length = length;
            }
        }
    }

    printf("%d\n", max_path_length);

    // 메모리 해제
    for (int i = 1; i <= MAX_NODES; i++) {
        Node* current = graph[i];
        while (current) {
            Node* temp = current;
            current = current->next;
            free(temp);
        }
    }

    return 0;
}