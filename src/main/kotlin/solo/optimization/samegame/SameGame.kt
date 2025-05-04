import java.util.*

/**
 * SameGame 구현 - 같은 색상의 연결된 블록을 제거하여 최고 점수를 얻는 게임
 **/
class SameGame {
    companion object {
        const val ROWS = 15
        const val COLS = 15
        const val EMPTY = -1
        
        // 방향 벡터 (상, 우, 하, 좌)
        val dr = intArrayOf(1, 0, -1, 0)
        val dc = intArrayOf(0, 1, 0, -1)
    }
    
    // 게임판 상태
    private val board = Array(ROWS) { IntArray(COLS) }
    private val tempBoard = Array(ROWS) { IntArray(COLS) }
    
    // 게임판을 디버그 출력
    private fun printBoard() {
        System.err.println("Board state:")
        for (r in ROWS - 1 downTo 0) {
            System.err.println(board[r].joinToString(" ") { "%2d".format(it) })
        }
    }
    
    // Flood Fill로 연결된 셀 수 계산
    private fun countConnectedCells(row: Int, col: Int, color: Int, visited: Array<BooleanArray>): Int {
        if (row < 0 || row >= ROWS || col < 0 || col >= COLS ||
            visited[row][col] || board[row][col] != color ||
            board[row][col] == EMPTY) {
            return 0
        }
        
        visited[row][col] = true
        var count = 1
        
        // 인접한 셀 세기
        for (i in 0 until 4) {
            count += countConnectedCells(row + dr[i], col + dc[i], color, visited)
        }
        
        return count
    }
    
    // 연결된 셀 제거
    private fun removeConnectedCells(row: Int, col: Int, color: Int, visited: Array<BooleanArray>) {
        if (row < 0 || row >= ROWS || col < 0 || col >= COLS ||
            visited[row][col] || board[row][col] != color ||
            board[row][col] == EMPTY) {
            return
        }
        
        visited[row][col] = true
        board[row][col] = EMPTY
        
        // 인접한 셀 제거
        for (i in 0 until 4) {
            removeConnectedCells(row + dr[i], col + dc[i], color, visited)
        }
    }
    
    // 중력 적용 - 셀이 아래로 떨어지도록 함
    private fun applyGravity() {
        for (col in 0 until COLS) {
            var writeRow = 0
            for (row in 0 until ROWS) {
                if (board[row][col] != EMPTY) {
                    board[writeRow][col] = board[row][col]
                    if (writeRow != row) {
                        board[row][col] = EMPTY
                    }
                    writeRow++
                }
            }
        }
    }
    
    // 빈 열 접기 - 오른쪽 열을 왼쪽으로 이동
    private fun collapseEmptyColumns() {
        var writeCol = 0
        for (readCol in 0 until COLS) {
            // 열이 비어있는지 확인
            var isEmpty = true
            for (row in 0 until ROWS) {
                if (board[row][readCol] != EMPTY) {
                    isEmpty = false
                    break
                }
            }
            
            if (!isEmpty) {
                // 빈 열이 아니면, 현재 쓰기 위치로 복사
                if (writeCol != readCol) {
                    for (row in 0 until ROWS) {
                        board[row][writeCol] = board[row][readCol]
                        board[row][readCol] = EMPTY
                    }
                }
                writeCol++
            }
        }
    }
    
    // 이동 실행 및 점수 계산
    private fun makeMove(row: Int, col: Int): Int {
        val color = board[row][col]
        if (color == EMPTY) {
            return -1  // 잘못된 이동
        }
        
        val visited = Array(ROWS) { BooleanArray(COLS) }
        val regionSize = countConnectedCells(row, col, color, visited)
        
        if (regionSize < 2) {
            return -1  // 잘못된 이동
        }
        
        val visited2 = Array(ROWS) { BooleanArray(COLS) }
        removeConnectedCells(row, col, color, visited2)
        applyGravity()
        collapseEmptyColumns()
        
        // 점수 계산: (n-2)²
        var score = (regionSize - 2) * (regionSize - 2)
        
        // 보드 클리어 체크
        var cleared = true
        outer@ for (r in 0 until ROWS) {
            for (c in 0 until COLS) {
                if (board[r][c] != EMPTY) {
                    cleared = false
                    break@outer
                }
            }
        }
        
        if (cleared) {
            score += 1000  // 보드 클리어 보너스
        }
        
        return score
    }
    
    // 이동 데이터 클래스
    data class Move(
        var col: Int = -1,
        var row: Int = -1, 
        var size: Int = 0, 
        var score: Int = -1
    )
    
    // 가장 좋은 이동 찾기
    private fun findBestMove(): Move {
        val bestMove = Move()
        
        for (row in 0 until ROWS) {
            for (col in 0 until COLS) {
                if (board[row][col] != EMPTY) {
                    val visited = Array(ROWS) { BooleanArray(COLS) }
                    val size = countConnectedCells(row, col, board[row][col], visited)
                    
                    if (size >= 2) {
                        // 보드 복사
                        for (r in 0 until ROWS) {
                            tempBoard[r] = board[r].copyOf()
                        }
                        
                        // 이동 시도
                        val score = makeMove(row, col)
                        
                        // 보드 복원
                        for (r in 0 until ROWS) {
                            board[r] = tempBoard[r].copyOf()
                        }
                        
                        if (score > bestMove.score) {
                            bestMove.row = row
                            bestMove.col = col
                            bestMove.size = size
                            bestMove.score = score
                        }
                    }
                }
            }
        }
        
        return bestMove
    }
    
    // 보드 상태 설정
    fun setBoard(newBoard: Array<IntArray>) {
        for (r in 0 until ROWS) {
            for (c in 0 until COLS) {
                board[r][c] = newBoard[r][c]
            }
        }
    }
    
    // 최적의 이동을 찾아 반환
    fun getNextMove(): Move {
        return findBestMove()
    }
}

fun main(args: Array<String>) {
    val input = Scanner(System.`in`)
    val game = SameGame()
    
    // 게임 루프
    while (true) {
        val boardInput = Array(SameGame.ROWS) { IntArray(SameGame.COLS) }
        
        // 보드 읽기 (위에서 아래로 입력됨)
        for (i in SameGame.ROWS - 1 downTo 0) {
            for (j in 0 until SameGame.COLS) {
                boardInput[i][j] = input.nextInt()
            }
        }
        
        game.setBoard(boardInput)
        val bestMove = game.getNextMove()
        
        if (bestMove.col != -1 && bestMove.row != -1) {
            println("${bestMove.col} ${bestMove.row} Size: ${bestMove.size}, Score: ${bestMove.score}")
        } else {
            println("0 0 No valid moves")
        }
    }
}
