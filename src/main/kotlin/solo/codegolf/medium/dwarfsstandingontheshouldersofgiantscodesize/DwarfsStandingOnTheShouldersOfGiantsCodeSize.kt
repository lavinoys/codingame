import java.util.*

fun main() {
    val MAX_NODES = 10000

    // 그래프를 인접 리스트로 표현 (Array<MutableList<Int>>로 구현)
    val graph = Array<MutableList<Int>>(MAX_NODES + 1) { mutableListOf() }

    // 메모이제이션을 위한 배열
    val memo = IntArray(MAX_NODES + 1) { -1 }

    // 노드 존재 여부를 저장하는 배열
    val nodesSet = BooleanArray(MAX_NODES + 1)

    val scanner = Scanner(System.`in`)
    val n = scanner.nextInt() // 영향 관계의 수

    // 입력 처리 및 그래프 구성
    for (i in 0 until n) {
        val x = scanner.nextInt() // x가 y에 영향을 미침
        val y = scanner.nextInt()

        graph[x].add(y) // 간선 추가
        nodesSet[x] = true
        nodesSet[y] = true
    }

    // DFS로 최대 경로 길이 계산 (메모이제이션 사용)
    fun dfs(node: Int): Int {
        // 이미 계산된 경우
        if (memo[node] != -1) {
            return memo[node]
        }

        var maxLength = 0
        for (nextNode in graph[node]) {
            val length = dfs(nextNode)
            if (length > maxLength) {
                maxLength = length
            }
        }

        // 현재 노드 + 최대 하위 경로
        memo[node] = 1 + maxLength
        return memo[node]
    }

    // 모든 노드에서 시작하는 경로 중 가장 긴 경로 찾기
    var maxPathLength = 0
    for (i in 1..MAX_NODES) {
        if (nodesSet[i]) {
            val length = dfs(i)
            if (length > maxPathLength) {
                maxPathLength = length
            }
        }
    }

    println(maxPathLength)
}