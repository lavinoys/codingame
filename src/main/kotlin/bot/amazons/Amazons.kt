import java.util.*
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

data class Position(val row: Int, val col: Int) {
    fun toNotation(boardSize: Int): String {
        return "${'a' + col}${boardSize - row}"
    }
    
    companion object {
        // 방향 정의 - 불변 배열로 변경하여 효율성 향상
        private val directionsArray = arrayOf(
            -1 to -1, -1 to 0, -1 to 1, 
            0 to -1, 0 to 1, 
            1 to -1, 1 to 0, 1 to 1
        )
        
        // 이전 버전과의 호환성을 위해 directions 리스트 유지
        val directions = directionsArray.toList()
        
        // 방향을 직접 배열로 접근하는 함수 - 객체 생성 없이 효율적으로 방향 참조
        @JvmStatic
        fun getDirection(index: Int): Pair<Int, Int> = directionsArray[index]
        
        val directionsCount = directionsArray.size
        
        // 방향의 행과 열 변화량을 개별적으로 가져오는 함수 (객체 생성 방지)
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

// 성능을 위한 간단한 이동 표현 클래스 - 객체 생성 최소화
class MoveInt(val fromRow: Int, val fromCol: Int, val toRow: Int, val toCol: Int, val arrowRow: Int, val arrowCol: Int) {
    fun toMove(): Move = Move(Position(fromRow, fromCol), Position(toRow, toCol), Position(arrowRow, arrowCol))
    
    // 객체 재사용을 통한 성능 최적화
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
    
    // 캐싱을 위한 변수들 - 효율성 향상
    private val movesCache = mutableMapOf<Int, List<Position>>()
    private val mobilityCache = mutableMapOf<Int, Int>()
    private val moveIntList = mutableListOf<MoveInt>()
    
    // 최근 보드 해시값 캐싱
    private var currentBoardHash: Int = 0
    private var boardHashValid = false
    
    // 보드 상태를 해시로 표현하기 위한 빠른 함수 (조브리스트-피어슨 해싱)
    private fun getBoardHash(): Int {
        if (boardHashValid) return currentBoardHash
        
        var h = 0
        var index = 0
        
        for (row in 0 until size) {
            for (col in 0 until size) {
                val c = board[row][col]
                // 피어슨 해싱으로 충돌 가능성 감소
                h = ((h * 31) + c.code) and 0x7FFFFFFF
                index++
            }
        }
        
        currentBoardHash = h
        boardHashValid = true
        return h
    }
    
    // 보드 위치를 해시 키로 변환 - 캐시 효율 향상
    private fun positionToHashKey(row: Int, col: Int, dirIndex: Int): Int {
        return ((row * 31 + col) * 31 + dirIndex) * 31 + getBoardHash()
    }
    
    fun update(color: String, boardLines: List<String>) {
        myColor = color[0]
        myPieces.clear()
        opponentPieces.clear()
        movesCache.clear()
        mobilityCache.clear()
        boardHashValid = false
        
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
    
    // 빠른 유효성 검사 (인라인 처리를 위해 변수 분리)
    @JvmName("isValidPosition")
    fun isValidPosition(row: Int, col: Int): Boolean {
        return row in 0 until size && col in 0 until size && board[row][col] == '.'
    }
    
    fun isValidPosition(pos: Position): Boolean {
        return isValidPosition(pos.row, pos.col)
    }
    
    // 한 방향으로 움직일 수 있는 모든 위치 찾기 (캐싱 최적화)
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
    
    // 특정 말에 대해 가능한 모든 이동 생성 (최적화)
    fun generateMovesForPiece(piece: Position): List<MoveInt> {
        moveIntList.clear()
        
        for (dirIndex in 0 until Position.directionsCount) {
            val dirRow = Position.getDirRow(dirIndex)
            val dirCol = Position.getDirCol(dirIndex)
            var r = piece.row + dirRow
            var c = piece.col + dirCol
            
            // 말 이동 가능한 모든 위치 탐색 (객체 생성 최소화)
            while (isValidPosition(r, c)) {
                // 임시로 말 이동
                val originalPiece = board[piece.row][piece.col]
                board[piece.row][piece.col] = '.'
                board[r][c] = originalPiece
                boardHashValid = false  // 보드 해시 무효화
                
                // 화살 배치 가능한 위치 찾기 (객체 생성 최소화)
                for (arrowDirIndex in 0 until Position.directionsCount) {
                    val arrowDirRow = Position.getDirRow(arrowDirIndex)
                    val arrowDirCol = Position.getDirCol(arrowDirIndex)
                    var ar = r + arrowDirRow
                    var ac = c + arrowDirCol
                    
                    while (isValidPosition(ar, ac)) {
                        moveIntList.add(MoveInt(piece.row, piece.col, r, c, ar, ac))
                        ar += arrowDirRow
                        ac += arrowDirCol
                    }
                }
                
                // 원래대로 복구
                board[r][c] = '.'
                board[piece.row][piece.col] = originalPiece
                boardHashValid = false  // 보드 해시 무효화
                
                r += dirRow
                c += dirCol
            }
        }
        
        return moveIntList.toList()
    }
    
    // 가능한 모든 이동 생성 (최적화)
    fun generateAllMoves(): List<Move> {
        // 최대 이동 수에 도달하면 중지 (성능 최적화)
        val maxMovesToGenerate = if (myPieces.size < 3) 200 else 100
        val allMoves = ArrayList<Move>(maxMovesToGenerate)
        
        // 중앙에 가까운 말부터 고려 (일반적으로 좋은 전략)
        val centerRow = size / 2
        val centerCol = size / 2
        
        // 객체 생성을 최소화하기 위해 정렬 대신 가장 중앙에 가까운 말 먼저 처리
        val distanceMap = myPieces.associateWith { 
            abs(it.row - centerRow) + abs(it.col - centerCol) 
        }
        
        val sortedPieces = myPieces.sortedBy { distanceMap[it] }
        
        for (piece in sortedPieces) {
            val moves = generateMovesForPiece(piece)
            
            // 중앙에 가까운 목적지를 우선적으로 고려
            val sortedMoves = if (myPieces.size <= 3) {
                // 게임 후반부는 이동성이 더 중요하므로 정렬 생략
                moves
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
    
    // 이동을 시뮬레이션하고 보드 상태 변경
    fun makeMove(move: Move) {
        val from = move.from
        val to = move.to
        val arrow = move.arrow
        
        val piece = board[from.row][from.col]
        board[from.row][from.col] = '.'
        board[to.row][to.col] = piece
        board[arrow.row][arrow.col] = '-'
        
        // 보드 상태 변경으로 캐시 및 해시 무효화
        boardHashValid = false
        
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
        
        // 보드 상태 변경으로 캐시 및 해시 무효화
        boardHashValid = false
        
        // 내 말 위치 복원
        val pieceIndex = myPieces.indexOfFirst { it.row == to.row && it.col == to.col }
        if (pieceIndex != -1) {
            myPieces[pieceIndex] = from
        }
    }
    
    // 빠른 이동성 계산 - 캐싱과 방향별 최적화
    private fun calculateMobilityForPiece(row: Int, col: Int): Int {
        val key = ((row * 31 + col) * 31) + getBoardHash()
        
        return mobilityCache.getOrPut(key) {
            var mobility = 0
            
            // 8방향 이동성 계산 (객체 생성 없이)
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
            
            mobility
        }
    }
    
    // 평가 함수 최적화: 이동성과 영역 지배력 통합
    fun evaluateMove(move: Move): Int {
        val from = move.from
        val to = move.to
        val arrow = move.arrow
        
        val originalPiece = board[from.row][from.col]
        
        // 이동 시뮬레이션
        makeMove(move)
        
        // 초기화
        movesCache.clear()
        mobilityCache.clear()
        
        // 이동성 평가 (최적화)
        var myMobility = 0
        var opponentMobility = 0
        
        // 내 이동성 빠르게 계산
        for (myPiece in myPieces) {
            myMobility += calculateMobilityForPiece(myPiece.row, myPiece.col)
        }
        
        // 상대방 이동성 빠르게 계산
        for (opponentPiece in opponentPieces) {
            opponentMobility += calculateMobilityForPiece(opponentPiece.row, opponentPiece.col)
        }
        
        // 이동성 차이에 가중치 부여 (핵심 전략 요소)
        val mobilityScore = (myMobility - opponentMobility) * 3
        
        // 원래대로 복구
        undoMove(move, originalPiece)
        
        return mobilityScore
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
                abs(to.row - centerRow) + abs(to.col - centerCol)
            }
        }
        
        // 시간에 따른 적응형 탐색 깊이
        val depth = if (myPieces.size <= 3 || opponentPieces.size <= 3) {
            // 게임 후반부는 더 깊게 탐색
            2
        } else if (timeLimit > 500) {
            // 첫 턴은 더 깊게 탐색
            2
        } else {
            1
        }
        
        // 시간 관리 개선 - 전체 시간의 80%만 사용
        val safeTimeLimit = (timeLimit * 0.8).toLong()
        
        var bestMove: Move? = null
        var bestScore = Int.MIN_VALUE
        
        for (move in possibleMoves) {
            if (System.currentTimeMillis() - startTime > safeTimeLimit) {
                break
            }
            
            val originalPiece = board[move.from.row][move.from.col]
            makeMove(move)
            
            // 효율적인 평가 호출
            val score = if (depth <= 1) {
                evaluateSimplePosition()
            } else {
                -negamax(depth - 1, Int.MIN_VALUE, Int.MAX_VALUE, startTime, safeTimeLimit)
            }
            
            undoMove(move, originalPiece)
            
            if (score > bestScore) {
                bestScore = score
                bestMove = move
            }
        }
        
        return bestMove ?: possibleMoves.firstOrNull()
    }
    
    // 시간 제약을 고려한 Negamax
    private fun negamax(depth: Int, alpha: Int, beta: Int, startTime: Long, timeLimit: Long): Int {
        // 시간 초과 확인
        if (System.currentTimeMillis() - startTime > timeLimit) {
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
            if (System.currentTimeMillis() - startTime > timeLimit) {
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
        
        // 내 이동성 계산 - 객체 생성 최소화
        for (myPiece in myPieces) {
            for (dirIndex in 0 until Position.directionsCount) {
                val dirRow = Position.getDirRow(dirIndex)
                val dirCol = Position.getDirCol(dirIndex)
                var r = myPiece.row + dirRow
                var c = myPiece.col + dirCol
                
                while (isValidPosition(r, c)) {
                    myMobility++
                    r += dirRow
                    c += dirCol
                }
            }
        }
        
        // 상대방 이동성 계산 - 객체 생성 최소화
        for (opponentPiece in opponentPieces) {
            for (dirIndex in 0 until Position.directionsCount) {
                val dirRow = Position.getDirRow(dirIndex)
                val dirCol = Position.getDirCol(dirIndex)
                var r = opponentPiece.row + dirRow
                var c = opponentPiece.col + dirCol
                
                while (isValidPosition(r, c)) {
                    opponentMobility++
                    r += dirRow
                    c += dirCol
                }
            }
        }
        
        // 이동성 차이 반환 (가장 중요한 요소)
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
