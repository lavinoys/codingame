import java.util.*
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

data class Position(val row: Int, val col: Int) {
    fun toNotation(boardSize: Int): String {
        return "${'a' + col}${boardSize - row}"
    }
    
    companion object {
        val directions = listOf(
            -1 to -1, -1 to 0, -1 to 1, 
            0 to -1, 0 to 1, 
            1 to -1, 1 to 0, 1 to 1
        )
        
        // 방향 배열을 직접 사용하여 객체 생성 줄이기
        private val directionsArray = arrayOf(
            -1 to -1, -1 to 0, -1 to 1, 
            0 to -1, 0 to 1, 
            1 to -1, 1 to 0, 1 to 1
        )
        
        fun getDirection(index: Int): Pair<Int, Int> = directionsArray[index]
        val directionsCount = directionsArray.size
    }
}

data class Move(val from: Position, val to: Position, val arrow: Position) {
    fun toNotation(boardSize: Int): String {
        return "${from.toNotation(boardSize)}${to.toNotation(boardSize)}${arrow.toNotation(boardSize)}"
    }
}

// 성능을 위한 간단한 이동 표현 클래스
class MoveInt(val fromRow: Int, val fromCol: Int, val toRow: Int, val toCol: Int, val arrowRow: Int, val arrowCol: Int) {
    fun toMove(): Move = Move(Position(fromRow, fromCol), Position(toRow, toCol), Position(arrowRow, arrowCol))
}

class GameBoard(val size: Int) {
    var board = Array(size) { CharArray(size) { '.' } }
    val myPieces = mutableListOf<Position>()
    val opponentPieces = mutableListOf<Position>()
    var myColor: Char = 'w'
    
    // 캐싱을 위한 변수들
    private val movesCache = mutableMapOf<String, List<Position>>()
    private val mobilityCache = mutableMapOf<String, Int>()
    private val moveIntList = mutableListOf<MoveInt>()
    
    // 보드 상태를 해시로 표현하기 위한 함수
    private fun getBoardHash(): String {
        val sb = StringBuilder(size * size)
        for (row in board) {
            sb.append(row)
        }
        return sb.toString()
    }
    
    fun update(color: String, boardLines: List<String>) {
        myColor = color[0]
        myPieces.clear()
        opponentPieces.clear()
        movesCache.clear()
        mobilityCache.clear()
        
        for (row in 0 until size) {
            for (col in 0 until size) {
                board[row][col] = boardLines[row][col]
                if (board[row][col] == myColor) {
                    myPieces.add(Position(row, col))
                } else if (board[row][col] != '.' && board[row][col] != '-') {
                    opponentPieces.add(Position(row, col))
                }
            }
        }
    }
    
    fun isValidPosition(pos: Position): Boolean {
        return pos.row in 0 until size && pos.col in 0 until size && board[pos.row][pos.col] == '.'
    }
    
    // 한 방향으로 움직일 수 있는 모든 위치 찾기 (캐싱 적용)
    fun getMovesInDirection(from: Position, dir: Pair<Int, Int>): List<Position> {
        val cacheKey = "${from.row},${from.col},${dir.first},${dir.second},${getBoardHash()}"
        
        return movesCache.getOrPut(cacheKey) {
            val result = mutableListOf<Position>()
            var row = from.row + dir.first
            var col = from.col + dir.second
            
            while (row in 0 until size && col in 0 until size && board[row][col] == '.') {
                result.add(Position(row, col))
                row += dir.first
                col += dir.second
            }
            
            result
        }
    }
    
    // 특정 위치에서 다른 위치로 직선/대각선 이동이 가능한지 확인
    fun canMove(from: Position, to: Position): Boolean {
        if (to.row !in 0 until size || to.col !in 0 until size || board[to.row][to.col] != '.') {
            return false
        }
        
        val rowDiff = to.row - from.row
        val colDiff = to.col - from.col
        
        // 수직, 수평, 대각선 이동만 가능
        if (rowDiff == 0 || colDiff == 0 || abs(rowDiff) == abs(colDiff)) {
            val rowDir = if (rowDiff == 0) 0 else rowDiff / abs(rowDiff)
            val colDir = if (colDiff == 0) 0 else colDiff / abs(colDiff)
            
            var r = from.row + rowDir
            var c = from.col + colDir
            
            while (r != to.row || c != to.col) {
                if (board[r][c] != '.') return false
                r += rowDir
                c += colDir
            }
            
            return true
        }
        
        return false
    }
    
    // 특정 말에 대해 가능한 모든 이동 생성 (최적화)
    fun generateMovesForPiece(piece: Position): List<MoveInt> {
        moveIntList.clear()
        
        for (dirIndex in 0 until Position.directionsCount) {
            val dir = Position.getDirection(dirIndex)
            var r = piece.row + dir.first
            var c = piece.col + dir.second
            
            // 말 이동 가능한 모든 위치 탐색
            while (r in 0 until size && c in 0 until size && board[r][c] == '.') {
                // 임시로 말 이동
                val originalPiece = board[piece.row][piece.col]
                board[piece.row][piece.col] = '.'
                board[r][c] = originalPiece
                
                // 화살 배치 가능한 위치 찾기
                for (arrowDirIndex in 0 until Position.directionsCount) {
                    val arrowDir = Position.getDirection(arrowDirIndex)
                    var ar = r + arrowDir.first
                    var ac = c + arrowDir.second
                    
                    while (ar in 0 until size && ac in 0 until size && board[ar][ac] == '.') {
                        moveIntList.add(MoveInt(piece.row, piece.col, r, c, ar, ac))
                        ar += arrowDir.first
                        ac += arrowDir.second
                    }
                }
                
                // 원래대로 복구
                board[r][c] = '.'
                board[piece.row][piece.col] = originalPiece
                
                r += dir.first
                c += dir.second
            }
        }
        
        return moveIntList.toList()
    }
    
    // 가능한 모든 이동 생성 (최적화)
    fun generateAllMoves(): List<Move> {
        val allMoves = mutableListOf<Move>()
        
        // 첫 턴인 경우 중앙에 가까운 말부터 고려 (일반적으로 좋은 전략)
        val centerRow = size / 2
        val centerCol = size / 2
        val sortedPieces = myPieces.sortedBy { 
            abs(it.row - centerRow) + abs(it.col - centerCol) 
        }
        
        // 일정 개수 이상의 이동이 발견되면 조기 종료 (성능 최적화)
        val maxMovesToGenerate = 100
        
        for (piece in sortedPieces) {
            val moves = generateMovesForPiece(piece)
            moves.take(maxMovesToGenerate - allMoves.size).forEach { 
                allMoves.add(it.toMove())
                if (allMoves.size >= maxMovesToGenerate) return allMoves
            }
        }
        
        return allMoves
    }
    
    // 이동을 시뮬레이션하고 보드 상태 변경
    fun makeMove(move: Move) {
        val from = move.from
        val to = move.to
        val arrow = move.arrow
        
        val piece = board[from.row][from.col]
        board[from.row][from.col] = '.'
        board[to.row][to.col] = piece
        board[arrow.row][arrow.col] = '-'
        
        // 캐시 무효화
        movesCache.clear()
        
        // 내 말 위치 업데이트
        val pieceIndex = myPieces.indexOfFirst { it.row == from.row && it.col == from.col }
        if (pieceIndex != -1) {
            myPieces[pieceIndex] = to
        }
    }
    
    // 이동을 취소하고 이전 상태로 복원
    fun undoMove(move: Move, originalPiece: Char) {
        val from = move.from
        val to = move.to
        val arrow = move.arrow
        
        board[from.row][from.col] = originalPiece
        board[to.row][to.col] = '.'
        board[arrow.row][arrow.col] = '.'
        
        // 캐시 무효화
        movesCache.clear()
        
        // 내 말 위치 복원
        val pieceIndex = myPieces.indexOfFirst { it.row == to.row && it.col == to.col }
        if (pieceIndex != -1) {
            myPieces[pieceIndex] = from
        }
    }
    
    // 빠른 이동성 계산 (캐싱 적용)
    private fun calculateMobilityForPiece(piece: Position): Int {
        val key = "${piece.row},${piece.col},${getBoardHash()}"
        
        return mobilityCache.getOrPut(key) {
            var mobility = 0
            
            for (dirIndex in 0 until Position.directionsCount) {
                val dir = Position.getDirection(dirIndex)
                var r = piece.row + dir.first
                var c = piece.col + dir.second
                
                while (r in 0 until size && c in 0 until size && board[r][c] == '.') {
                    mobility++
                    r += dir.first
                    c += dir.second
                }
            }
            
            mobility
        }
    }
    
    // 개선된 평가 함수: 성능 최적화
    fun evaluateMove(move: Move): Int {
        // 말 이동 시뮬레이션
        val from = move.from
        val to = move.to
        val arrow = move.arrow
        
        val originalPiece = board[from.row][from.col]
        
        // 이동 시뮬레이션
        makeMove(move)
        
        // 이동성 평가 (최적화)
        var myMobility = 0
        var opponentMobility = 0
        
        // 내 이동성 빠르게 계산
        for (myPiece in myPieces) {
            myMobility += calculateMobilityForPiece(myPiece)
        }
        
        // 상대방 이동성 빠르게 계산
        for (opponentPiece in opponentPieces) {
            opponentMobility += calculateMobilityForPiece(opponentPiece)
        }
        
        // 초단순화된 평가 - 게임 종반에는 이동성이 가장 중요
        val score = myMobility - opponentMobility
        
        // 원래대로 복구
        undoMove(move, originalPiece)
        
        return score
    }
    
    // 맨해튼 거리 계산 (두 위치 사이의 가로 및 세로 거리의 합)
    private fun manhattanDistance(pos1: Position, pos2: Position): Int {
        return abs(pos1.row - pos2.row) + abs(pos1.col - pos2.col)
    }
    
    // 최적화된 알파-베타 탐색
    fun findBestMove(timeLimit: Long): Move? {
        val startTime = System.currentTimeMillis()
        val possibleMoves = generateAllMoves()
        if (possibleMoves.isEmpty()) return null
        
        // 첫 턴에는 간단한 휴리스틱으로 빠르게 결정
        if (possibleMoves.size > 80) {  // 초반 턴으로 판단
            // 중앙 지향 이동 선택
            val centerRow = size / 2
            val centerCol = size / 2
            return possibleMoves.minByOrNull { 
                val to = it.to
                manhattanDistance(to, Position(centerRow, centerCol))
            }
        }
        
        // 시간에 따른 적응형 탐색 깊이
        val remainingTime = timeLimit - (System.currentTimeMillis() - startTime)
        val movesPerMillisecond = if (remainingTime > 0) 10.0 / remainingTime else 0.01
        val estimatedMaxDepth = when {
            movesPerMillisecond < 0.001 -> 2
            movesPerMillisecond < 0.01 -> 1
            else -> 1
        }
        
        var bestMove: Move? = null
        var bestScore = Int.MIN_VALUE
        
        // 이터레이티브 디프닝 - 시간이 허락하는대로 깊이 증가
        for (depth in 1..estimatedMaxDepth) {
            var currentBestMove: Move? = null
            var currentBestScore = Int.MIN_VALUE
            
            for (move in possibleMoves) {
                if (System.currentTimeMillis() - startTime > timeLimit * 0.8) {
                    // 시간의 80%를 초과하면 현재 최선의 이동 반환
                    return bestMove ?: possibleMoves.first()
                }
                
                val originalPiece = board[move.from.row][move.from.col]
                makeMove(move)
                
                val score = -negamax(depth - 1, Int.MIN_VALUE, Int.MAX_VALUE, startTime, timeLimit)
                
                undoMove(move, originalPiece)
                
                if (score > currentBestScore) {
                    currentBestScore = score
                    currentBestMove = move
                }
            }
            
            // 이 깊이에서의 최선 이동 저장
            if (currentBestMove != null) {
                bestMove = currentBestMove
                bestScore = currentBestScore
            }
        }
        
        return bestMove ?: possibleMoves.first()
    }
    
    // 시간 제약을 고려한 Negamax
    private fun negamax(depth: Int, alpha: Int, beta: Int, startTime: Long, timeLimit: Long): Int {
        // 시간 초과 확인
        if (System.currentTimeMillis() - startTime > timeLimit * 0.8) {
            return 0  // 중립 점수 반환
        }
        
        if (depth == 0) return evaluateSimplePosition()
        
        val possibleMoves = generateAllMoves()
        if (possibleMoves.isEmpty()) return -1000  // 이동할 수 없으면 매우 나쁜 점수
        
        var bestScore = Int.MIN_VALUE
        var currentAlpha = alpha
        
        for (move in possibleMoves) {
            val originalPiece = board[move.from.row][move.from.col]
            makeMove(move)
            
            val score = -negamax(depth - 1, -beta, -currentAlpha, startTime, timeLimit)
            
            undoMove(move, originalPiece)
            
            // 시간 초과 확인
            if (System.currentTimeMillis() - startTime > timeLimit * 0.8) {
                return bestScore
            }
            
            bestScore = max(bestScore, score)
            currentAlpha = max(currentAlpha, score)
            if (currentAlpha >= beta) break  // 베타 컷오프
        }
        
        return bestScore
    }
    
    // 단순화된 위치 평가 (매우 빠름)
    private fun evaluateSimplePosition(): Int {
        var myMobility = 0
        var opponentMobility = 0
        
        // 샘플링된 이동성 계산 (모든 방향을 계산하지 않고 일부만 확인)
        for (myPiece in myPieces) {
            // 4방향만 확인 (성능 최적화)
            for (dirIndex in 0 until 4) {
                val dir = Position.getDirection(dirIndex)
                var r = myPiece.row + dir.first
                var c = myPiece.col + dir.second
                
                while (r in 0 until size && c in 0 until size && board[r][c] == '.') {
                    myMobility++
                    r += dir.first
                    c += dir.second
                }
            }
        }
        
        for (opponentPiece in opponentPieces) {
            // 4방향만 확인 (성능 최적화)
            for (dirIndex in 0 until 4) {
                val dir = Position.getDirection(dirIndex)
                var r = opponentPiece.row + dir.first
                var c = opponentPiece.col + dir.second
                
                while (r in 0 until size && c in 0 until size && board[r][c] == '.') {
                    opponentMobility++
                    r += dir.first
                    c += dir.second
                }
            }
        }
        
        // 이동성 차이만 반환 (가장 중요한 요소)
        return myMobility - opponentMobility
    }
}

fun main(args : Array<String>) {
    val input = Scanner(System.`in`)
    val boardSize = input.nextInt() // height and width of the board
    val gameBoard = GameBoard(boardSize)
    var turnCount = 0

    // game loop
    while (true) {
        val startTime = System.currentTimeMillis()
        val color = input.next() // current color of your pieces ("w" or "b")
        val boardLines = mutableListOf<String>()
        
        for (i in 0 until boardSize) {
            boardLines.add(input.next()) // horizontal row
        }
        
        gameBoard.update(color, boardLines)
        
        val lastAction = input.next() // last action made by the opponent ("null" if it's the first turn)
        val actionsCount = input.nextInt() // number of legal actions
        
        turnCount++
        val isFirstTurn = lastAction == "null"
        
        // 가용 시간에 따른 탐색 전략
        val timeLimit = if (isFirstTurn) 950L else 80L  // ms
        
        val bestMove = gameBoard.findBestMove(timeLimit)
        
        val elapsedTime = System.currentTimeMillis() - startTime
        System.err.println("Time taken: $elapsedTime ms")
        
        if (bestMove == null) {
            System.err.println("No valid moves found!")
            println("random") // 이동할 수 없는 경우 (게임 종료 상황)
        } else {
            val notation = bestMove.toNotation(boardSize)
            System.err.println("Selected move: $notation (Turn: $turnCount)")
            println(notation)
        }
    }
}
