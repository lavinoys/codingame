import java.util.*

/**
 * Number Shifting 게임 솔버
 */
fun main() {
    // 첫 레벨 코드 출력
    println("first_level")
    
    val scanner = Scanner(System.`in`)
    
    // 게임 루프
    while (true) {
        val width = scanner.nextInt()
        val height = scanner.nextInt()
        
        val grid = Array(height) { IntArray(width) { 0 } }
        for (y in 0 until height) {
            for (x in 0 until width) {
                grid[y][x] = scanner.nextInt()
            }
        }
        
        // 게임 솔버 생성
        val solver = NumberShiftingSolver(width, height, grid)
        
        // 디버깅: 초기 그리드 출력
        solver.printGrid()
        
        // 해결 시도
        val solution = solver.solve()
        
        if (solution.isNotEmpty()) {
            System.err.println("Solution found with ${solution.size} moves!")
            solution.forEach { move ->
                println("${move.x} ${move.y} ${move.dir.symbol} ${if (move.add) '+' else '-'}")
            }
        } else {
            System.err.println("Failed to find solution")
            // 실패 시 예제 출력 (실제로는 작동하지 않을 수 있음)
            println("0 0 R +")
        }
    }
}

// 방향 정의
enum class Direction(val symbol: Char, val dx: Int, val dy: Int) {
    UP('U', 0, -1),
    RIGHT('R', 1, 0),
    DOWN('D', 0, 1),
    LEFT('L', -1, 0)
}

// 이동 정의 - 원본 값 저장 필드 추가
data class Move(
    val x: Int, 
    val y: Int, 
    val dir: Direction, 
    val add: Boolean,
    val originalValue: Int,             // 이동한 원본 값
    val targetOriginalValue: Int = 0    // 이동 목적지의 원본 값
)

class NumberShiftingSolver(private val width: Int, private val height: Int, private var grid: Array<IntArray>) {
    private val solution = mutableListOf<Move>()
    
    // 그리드 출력 함수 (디버깅용)
    fun printGrid() {
        System.err.println("Grid ${width}x${height}:")
        for (y in 0 until height) {
            for (x in 0 until width) {
                System.err.print("${grid[y][x]} ")
            }
            System.err.println()
        }
    }
    
    // 안전하게 그리드 범위 체크하는 함수 추가
    private fun isInBounds(x: Int, y: Int): Boolean {
        return x >= 0 && x < width && y >= 0 && y < height
    }
    
    // 이동이 유효한지 확인 - 범위 체크 강화
    private fun isValidMove(x: Int, y: Int, dir: Direction, add: Boolean): Boolean {
        if (!isInBounds(x, y) || grid[y][x] == 0) {
            return false
        }
        
        val value = grid[y][x]
        val newX = x + dir.dx * value
        val newY = y + dir.dy * value
        
        // 범위를 벗어나거나 목표 위치가 빈 셀인 경우
        return isInBounds(newX, newY) && grid[newY][newX] != 0
    }
    
    // 이동 실행 - 원본 값 저장 기능 추가
    private fun makeMove(x: Int, y: Int, dir: Direction, add: Boolean) {
        val value = grid[y][x]
        val newX = x + dir.dx * value
        val newY = y + dir.dy * value
        
        if (!isInBounds(newX, newY)) {
            System.err.println("Warning: attempting to move to invalid position ($newX, $newY)")
            return
        }
        
        val targetOriginalValue = grid[newY][newX]
        
        // 이동한 셀 값 계산
        if (add) {
            grid[newY][newX] += value
        } else {
            grid[newY][newX] = kotlin.math.abs(grid[newY][newX] - value)
        }
        
        // 원래 위치 비우기
        grid[y][x] = 0
        
        // 이동 저장 - 원본 값도 함께 저장
        solution.add(Move(x, y, dir, add, value, targetOriginalValue))
    }
    
    // 이동 되돌리기 - 완전히 재작성
    private fun undoMove() {
        if (solution.isEmpty()) return
        
        val lastMove = solution.removeAt(solution.size - 1)
        val x = lastMove.x
        val y = lastMove.y
        val dir = lastMove.dir
        val originalValue = lastMove.originalValue
        val targetOriginalValue = lastMove.targetOriginalValue
        
        // 이동한 목적지 위치 계산
        val newX = x + dir.dx * originalValue
        val newY = y + dir.dy * originalValue
        
        // 범위 체크 추가
        if (!isInBounds(x, y) || !isInBounds(newX, newY)) {
            System.err.println("Warning: Invalid position during undo ($x, $y) -> ($newX, $newY)")
            return
        }
        
        // 원래 위치 복원
        grid[y][x] = originalValue
        
        // 목적지 위치 원래 값 복원
        grid[newY][newX] = targetOriginalValue
    }
    
    // 보드가 비어있는지 확인
    private fun isBoardEmpty(): Boolean {
        for (y in 0 until height) {
            for (x in 0 until width) {
                if (grid[y][x] != 0) {
                    return false
                }
            }
        }
        return true
    }
    
    // 숫자 갯수 세기
    private fun countNumbers(): Int {
        var count = 0
        for (y in 0 until height) {
            for (x in 0 until width) {
                if (grid[y][x] != 0) {
                    count++
                }
            }
        }
        return count
    }
    
    // 백트래킹을 통해 해결책 찾기
    private fun solveRecursive(depth: Int, maxDepth: Int): Boolean {
        // 보드가 비어있으면 성공
        if (isBoardEmpty()) {
            return true
        }
        
        // 최대 깊이에 도달했으면 실패
        if (depth >= maxDepth) {
            return false
        }
        
        // 모든 셀에 대해 가능한 모든 이동 시도
        for (y in 0 until height) {
            for (x in 0 until width) {
                if (grid[y][x] == 0) continue
                
                // 4방향 시도
                for (dir in Direction.values()) {
                    // 더하기와 빼기 모두 시도
                    for (add in arrayOf(true, false)) {
                        if (isValidMove(x, y, dir, add)) {
                            try {
                                // 이동 실행
                                makeMove(x, y, dir, add)
                                
                                // 재귀 호출로 다음 이동 시도
                                if (solveRecursive(depth + 1, maxDepth)) {
                                    return true
                                }
                                
                                // 이동 되돌리기 (백트래킹)
                                undoMove()
                            } catch (e: Exception) {
                                System.err.println("Error during move: ${e.message}")
                                // 예외 발생 시 다음 이동으로 넘어감
                            }
                        }
                    }
                }
            }
        }
        
        return false
    }
    
    // 해결책 찾기 시도
    fun solve(): List<Move> {
        solution.clear()
        
        // 숫자 개수 기준으로 최대 깊이 설정 (휴리스틱)
        val numCount = countNumbers()
        val maxDepth = numCount + 5  // 여유를 두고 설정
        
        // 그리드 복사본으로 작업 (깊은 복사 확인)
        val originalGrid = Array(height) { y -> 
            IntArray(width) { x -> grid[y][x] }
        }
        
        // 해결책 찾기 시도
        val solved = try {
            solveRecursive(0, maxDepth)
        } catch (e: Exception) {
            System.err.println("Solver error: ${e.message}")
            false
        }
        
        // 원본 그리드 복원
        grid = originalGrid
        
        return if (solved) solution.toList() else emptyList()
    }
}
