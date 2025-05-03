import java.util.*
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

// 상수 정의 - 매직 넘버 제거
object GameConstants {
    const val MAX_MOVES_EARLY_GAME = 100
    const val MAX_MOVES_LATE_GAME = 200
    const val MOBILITY_WEIGHT = 3
    const val FIRST_TURN_TIME_LIMIT = 950L
    const val NORMAL_TURN_TIME_LIMIT = 80L
    const val SAFE_TIME_FACTOR = 0.8
}

data class Position(val row: Int, val col: Int) {
    fun toNotation(boardSize: Int): String {
        return "${'a' + col}${boardSize - row}"
    }
    
    companion object {
        // 방향 정의 - 불변 배열로 효율성 향상
        private val directionsArray = arrayOf(
            -1 to -1, -1 to 0, -1 to 1, 
            0 to -1, 0 to 1, 
            1 to -1, 1 to 0, 1 to 1
        )
        
        val directions = directionsArray.toList()
        
        @JvmStatic
        fun getDirection(index: Int): Pair<Int, Int> = directionsArray[index]
        
        val directionsCount = directionsArray.size
        
        @JvmStatic
        fun getDirRow(index: Int): Int = directionsArray[index].first
        
        @JvmStatic
        fun getDirCol(index: Int): Int = directionsArray[index].second
    }
}

data class Move(val from: Position, val to: Position, val arrow: Position) {
    fun toNotation(boardSize: Int): String {
        return "${from.toNotation(boardSize)}${to.toNotation(boardSize)}${arrow.toNotation(boardSize)}"
    }
}

// 성능을 위한 경량 이동 표현 클래스
class MoveInt(val fromRow: Int, val fromCol: Int, val toRow: Int, val toCol: Int, val arrowRow: Int, val arrowCol: Int) {
    fun toMove(): Move = Move(Position(fromRow, fromCol), Position(toRow, toCol), Position(arrowRow, arrowCol))
    
    fun toNotation(boardSize: Int): String {
        val fromCol = 'a' + fromCol
        val fromRow = boardSize - fromRow
        val toCol = 'a' + toCol
        val toRow = boardSize - toRow
        val arrowCol = 'a' + arrowCol
        val arrowRow = boardSize - arrowRow
        
        return "$fromCol$fromRow$toCol$toRow$arrowCol$arrowRow"
    }
}

class GameBoard(val size: Int) {
    var board = Array(size) { CharArray(size) { '.' } }
    val myPieces = mutableListOf<Position>()
    val opponentPieces = mutableListOf<Position>()
    var myColor: Char = 'w'
    
    // 캐싱을 위한 변수들
    private val movesCache = mutableMapOf<Int, List<Position>>()
    private val mobilityCache = mutableMapOf<Int, Int>()
    private val moveIntList = mutableListOf<MoveInt>()
    
    // 보드 해시 캐싱
    private var currentBoardHash: Int = 0
    private var boardHashValid = false
    
    // 게임 단계 확인 (초반, 중반, 후반)
    private fun isEndgame(): Boolean = myPieces.size <= 3 || opponentPieces.size <= 3
    
    private fun isEarlyGame(movesCount: Int): Boolean = movesCount > 80
    
    // 보드 해시 계산 (Zobrist 해싱 기반)
    private fun getBoardHash(): Int {
        if (boardHashValid) return currentBoardHash
        
        var h = 0
        
        for (row in 0 until size) {
            for (col in 0 until size) {
                val c = board[row][col]
                h = ((h * 31) + c.code) and 0x7FFFFFFF
            }
        }
        
        currentBoardHash = h
        boardHashValid = true
        return h
    }
    
    // 효율적인 해시 키 생성
    private fun positionToHashKey(row: Int, col: Int, dirIndex: Int): Int {
        return ((row * 31 + col) * 31 + dirIndex) * 31 + getBoardHash()
    }
    
    // 보드 상태 업데이트
    fun update(color: String, boardLines: List<String>) {
        myColor = color[0]
        myPieces.clear()
        opponentPieces.clear()
        clearCaches()
        
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
    
    // 캐시 초기화
    private fun clearCaches() {
        movesCache.clear()
        mobilityCache.clear()
        boardHashValid = false
    }
    
    // 유효한 위치 확인
    @JvmName("isValidPosition")
    fun isValidPosition(row: Int, col: Int): Boolean {
        return row in 0 until size && col in 0 until size && board[row][col] == '.'
    }
    
    fun isValidPosition(pos: Position): Boolean {
        return isValidPosition(pos.row, pos.col)
    }
    
    // 한 방향으로 움직일 수 있는 위치 찾기
    fun getMovesInDirection(from: Position, dirIndex: Int): List<Position> {
        val cacheKey = positionToHashKey(from.row, from.col, dirIndex)
        
        return movesCache.getOrPut(cacheKey) {
            val result = mutableListOf<Position>()
            val dirRow = Position.getDirRow(dirIndex)
            val dirCol = Position.getDirCol(dirIndex)
            
            var row = from.row + dirRow
            var col = from.col + dirCol
            
            while (isValidPosition(row, col)) {
                result.add(Position(row, col))
                row += dirRow
                col += dirCol
            }
            
            result
        }
    }
    
    // 특정 위치에서의 이동 가능성 탐색
    private fun exploreFromPosition(startRow: Int, startCol: Int, tempPiece: Char, 
                                   callback: (r: Int, c: Int, ar: Int, ac: Int) -> Unit) {
        for (dirIndex in 0 until Position.directionsCount) {
            val dirRow = Position.getDirRow(dirIndex)
            val dirCol = Position.getDirCol(dirIndex)
            var r = startRow + dirRow
            var c = startCol + dirCol
            
            while (isValidPosition(r, c)) {
                // 임시로 말 이동
                val originalPiece = board[startRow][startCol]
                board[startRow][startCol] = '.'
                board[r][c] = tempPiece
                boardHashValid = false
                
                // 화살 배치 가능한 위치 탐색
                exploreArrowPlacements(r, c, callback)
                
                // 원래대로 복구
                board[r][c] = '.'
                board[startRow][startCol] = originalPiece
                boardHashValid = false
                
                r += dirRow
                c += dirCol
            }
        }
    }
    
    // 화살 배치 위치 탐색
    private fun exploreArrowPlacements(startRow: Int, startCol: Int, 
                                      callback: (r: Int, c: Int, ar: Int, ac: Int) -> Unit) {
        for (arrowDirIndex in 0 until Position.directionsCount) {
            val arrowDirRow = Position.getDirRow(arrowDirIndex)
            val arrowDirCol = Position.getDirCol(arrowDirIndex)
            var ar = startRow + arrowDirRow
            var ac = startCol + arrowDirCol
            
            while (isValidPosition(ar, ac)) {
                callback(startRow, startCol, ar, ac)
                ar += arrowDirRow
                ac += arrowDirCol
            }
        }
    }
    
    // 특정 말에 대해 가능한 모든 이동 생성
    fun generateMovesForPiece(piece: Position): List<MoveInt> {
        moveIntList.clear()
        
        exploreFromPosition(piece.row, piece.col, board[piece.row][piece.col]) { startRow, startCol, ar, ac ->
            moveIntList.add(MoveInt(piece.row, piece.col, startRow, startCol, ar, ac))
        }
        
        return moveIntList.toList()
    }
    
    // 가능한 모든 이동 생성
    fun generateAllMoves(): List<Move> {
        val maxMovesToGenerate = if (isEndgame()) 
            GameConstants.MAX_MOVES_LATE_GAME 
        else 
            GameConstants.MAX_MOVES_EARLY_GAME
            
        val allMoves = ArrayList<Move>(maxMovesToGenerate)
        
        val centerRow = size / 2
        val centerCol = size / 2
        
        // 중앙에 가까운 말부터 처리
        val sortedPieces = getSortedPiecesByDistanceToCenter(centerRow, centerCol)
        
        for (piece in sortedPieces) {
            val moves = generateMovesForPiece(piece)
            
            // 게임 단계에 따라 이동 우선순위 조정
            val sortedMoves = if (isEndgame()) {
                moves // 게임 후반부는 정렬 생략
            } else {
                moves.sortedBy { 
                    abs(it.toRow - centerRow) + abs(it.toCol - centerCol) 
                }
            }
            
            for (move in sortedMoves) {
                allMoves.add(move.toMove())
                if (allMoves.size >= maxMovesToGenerate) return allMoves
            }
        }
        
        return allMoves
    }
    
    // 중앙까지의 거리로 말 정렬
    private fun getSortedPiecesByDistanceToCenter(centerRow: Int, centerCol: Int): List<Position> {
        val distanceMap = myPieces.associateWith { 
            abs(it.row - centerRow) + abs(it.col - centerCol) 
        }
        
        return myPieces.sortedBy { distanceMap[it] }
    }
    
    // 이동 실행
    fun makeMove(move: Move) {
        val from = move.from
        val to = move.to
        val arrow = move.arrow
        
        val piece = board[from.row][from.col]
        board[from.row][from.col] = '.'
        board[to.row][to.col] = piece
        board[arrow.row][arrow.col] = '-'
        
        boardHashValid = false
        
        // 내 말 위치 업데이트
        updatePiecePosition(from, to)
    }
    
    // 말 위치 업데이트
    private fun updatePiecePosition(from: Position, to: Position) {
        val pieceIndex = myPieces.indexOfFirst { it.row == from.row && it.col == from.col }
        if (pieceIndex != -1) {
            myPieces[pieceIndex] = to
        }
    }
    
    // 이동 취소
    fun undoMove(move: Move, originalPiece: Char) {
        val from = move.from
        val to = move.to
        val arrow = move.arrow
        
        board[from.row][from.col] = originalPiece
        board[to.row][to.col] = '.'
        board[arrow.row][arrow.col] = '.'
        
        boardHashValid = false
        
        // 내 말 위치 복원
        updatePiecePosition(to, from)
    }
    
    // 이동성 계산
    private fun calculateMobilityForPiece(row: Int, col: Int): Int {
        val key = ((row * 31 + col) * 31) + getBoardHash()
        
        return mobilityCache.getOrPut(key) {
            calculateRawMobility(row, col)
        }
    }
    
    // 캐시 없이 직접 이동성 계산
    private fun calculateRawMobility(row: Int, col: Int): Int {
        var mobility = 0
        
        for (dirIndex in 0 until Position.directionsCount) {
            val dirRow = Position.getDirRow(dirIndex)
            val dirCol = Position.getDirCol(dirIndex)
            
            var r = row + dirRow
            var c = col + dirCol
            
            while (isValidPosition(r, c)) {
                mobility++
                r += dirRow
                c += dirCol
            }
        }
        
        return mobility
    }
    
    // 이동 평가
    fun evaluateMove(move: Move): Int {
        val from = move.from
        val to = move.to
        val originalPiece = board[from.row][from.col]
        
        makeMove(move)
        clearCaches()
        
        // 이동성 비교
        val mobilityScore = evaluateMobilityDifference()
        
        undoMove(move, originalPiece)
        
        return mobilityScore * GameConstants.MOBILITY_WEIGHT
    }
    
    // 이동성 차이 평가
    private fun evaluateMobilityDifference(): Int {
        var myMobility = 0
        var opponentMobility = 0
        
        // 내 이동성 계산
        for (myPiece in myPieces) {
            myMobility += calculateMobilityForPiece(myPiece.row, myPiece.col)
        }
        
        // 상대방 이동성 계산
        for (opponentPiece in opponentPieces) {
            opponentMobility += calculateMobilityForPiece(opponentPiece.row, opponentPiece.col)
        }
        
        return myMobility - opponentMobility
    }
    
    // 최적의 이동 찾기
    fun findBestMove(timeLimit: Long): Move? {
        val startTime = System.currentTimeMillis()
        val possibleMoves = generateAllMoves()
        if (possibleMoves.isEmpty()) return null
        
        // 초반 턴 처리
        if (isEarlyGame(possibleMoves.size)) {
            return findCentralMove(possibleMoves)
        }
        
        // 적응형 탐색 깊이
        val depth = determineSearchDepth(timeLimit)
        
        // 안전 시간 한계
        val safeTimeLimit = (timeLimit * GameConstants.SAFE_TIME_FACTOR).toLong()
        
        // 최선의 이동 검색
        return findBestMoveWithinTimeLimit(possibleMoves, depth, startTime, safeTimeLimit)
    }
    
    // 중앙으로 향하는 이동 찾기
    private fun findCentralMove(moves: List<Move>): Move? {
        val centerRow = size / 2
        val centerCol = size / 2
        return moves.minByOrNull { 
            val to = it.to
            abs(to.row - centerRow) + abs(to.col - centerCol)
        }
    }
    
    // 탐색 깊이 결정
    private fun determineSearchDepth(timeLimit: Long): Int {
        return when {
            isEndgame() -> 2  // 게임 후반부
            timeLimit > 500 -> 2  // 첫 턴
            else -> 1  // 일반 턴
        }
    }
    
    // 시간 제한 내에서 최선의 이동 찾기
    private fun findBestMoveWithinTimeLimit(
        moves: List<Move>,
        depth: Int,
        startTime: Long,
        timeLimit: Long
    ): Move? {
        var bestMove: Move? = null
        var bestScore = Int.MIN_VALUE
        
        for (move in moves) {
            if (System.currentTimeMillis() - startTime > timeLimit) {
                break
            }
            
            val originalPiece = board[move.from.row][move.from.col]
            makeMove(move)
            
            val score = if (depth <= 1) {
                evaluateSimplePosition()
            } else {
                -negamax(depth - 1, Int.MIN_VALUE, Int.MAX_VALUE, startTime, timeLimit)
            }
            
            undoMove(move, originalPiece)
            
            if (score > bestScore) {
                bestScore = score
                bestMove = move
            }
        }
        
        return bestMove ?: moves.firstOrNull()
    }
    
    // 네가맥스 알고리즘 (미니맥스의 대칭 버전)
    private fun negamax(depth: Int, alpha: Int, beta: Int, startTime: Long, timeLimit: Long): Int {
        if (System.currentTimeMillis() - startTime > timeLimit) {
            return 0
        }
        
        if (depth == 0) return evaluateSimplePosition()
        
        val possibleMoves = generateAllMoves()
        if (possibleMoves.isEmpty()) return -1000
        
        var bestScore = Int.MIN_VALUE
        var currentAlpha = alpha
        
        for (move in possibleMoves) {
            val originalPiece = board[move.from.row][move.from.col]
            makeMove(move)
            
            val score = -negamax(depth - 1, -beta, -currentAlpha, startTime, timeLimit)
            
            undoMove(move, originalPiece)
            
            if (System.currentTimeMillis() - startTime > timeLimit) {
                return bestScore
            }
            
            bestScore = max(bestScore, score)
            currentAlpha = max(currentAlpha, score)
            if (currentAlpha >= beta) break
        }
        
        return bestScore
    }
    
    // 간단한 위치 평가
    private fun evaluateSimplePosition(): Int {
        return evaluateMobilityDifference()
    }
}

fun main(args : Array<String>) {
    val input = Scanner(System.`in`)
    val boardSize = input.nextInt()
    val gameBoard = GameBoard(boardSize)
    var turnCount = 0

    while (true) {
        val startTime = System.currentTimeMillis()
        val color = input.next()
        val boardLines = mutableListOf<String>()
        
        for (i in 0 until boardSize) {
            boardLines.add(input.next())
        }
        
        gameBoard.update(color, boardLines)
        
        val lastAction = input.next()
        val actionsCount = input.nextInt()
        
        turnCount++
        val isFirstTurn = lastAction == "null"
        
        // 턴에 따른 시간 제한 설정
        val timeLimit = if (isFirstTurn) 
            GameConstants.FIRST_TURN_TIME_LIMIT 
        else 
            GameConstants.NORMAL_TURN_TIME_LIMIT
        
        val bestMove = gameBoard.findBestMove(timeLimit)
        
        val elapsedTime = System.currentTimeMillis() - startTime
        System.err.println("Time taken: $elapsedTime ms")
        
        if (bestMove == null) {
            System.err.println("No valid moves found!")
            println("random")
        } else {
            val notation = bestMove.toNotation(boardSize)
            System.err.println("Selected move: $notation (Turn: $turnCount)")
            println(notation)
        }
    }
}
