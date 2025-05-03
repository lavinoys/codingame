import java.util.*
import kotlin.math.abs

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
    }
}

data class Move(val from: Position, val to: Position, val arrow: Position) {
    fun toNotation(boardSize: Int): String {
        return "${from.toNotation(boardSize)}${to.toNotation(boardSize)}${arrow.toNotation(boardSize)}"
    }
}

class GameBoard(val size: Int) {
    var board = Array(size) { CharArray(size) { '.' } }
    val myPieces = mutableListOf<Position>()
    val opponentPieces = mutableListOf<Position>()
    var myColor: Char = 'w'
    
    fun update(color: String, boardLines: List<String>) {
        myColor = color[0]
        myPieces.clear()
        opponentPieces.clear()
        
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
    
    // 한 방향으로 움직일 수 있는 모든 위치 찾기
    fun getMovesInDirection(from: Position, dir: Pair<Int, Int>): List<Position> {
        val result = mutableListOf<Position>()
        var row = from.row + dir.first
        var col = from.col + dir.second
        
        while (row in 0 until size && col in 0 until size && board[row][col] == '.') {
            result.add(Position(row, col))
            row += dir.first
            col += dir.second
        }
        
        return result
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
    
    // 특정 말에 대해 가능한 모든 이동 생성
    fun generateMovesForPiece(piece: Position): List<Move> {
        val moves = mutableListOf<Move>()
        
        for (dir in Position.directions) {
            val possibleMoves = getMovesInDirection(piece, dir)
            
            for (to in possibleMoves) {
                // 임시로 말 이동
                val originalPiece = board[piece.row][piece.col]
                board[piece.row][piece.col] = '.'
                board[to.row][to.col] = originalPiece
                
                // 화살 배치 가능한 위치 찾기
                for (arrowDir in Position.directions) {
                    val arrowPositions = getMovesInDirection(to, arrowDir)
                    for (arrow in arrowPositions) {
                        moves.add(Move(piece, to, arrow))
                    }
                }
                
                // 원래대로 복구
                board[to.row][to.col] = '.'
                board[piece.row][piece.col] = originalPiece
            }
        }
        
        return moves
    }
    
    // 가능한 모든 이동 생성
    fun generateAllMoves(): List<Move> {
        val allMoves = mutableListOf<Move>()
        
        for (piece in myPieces) {
            allMoves.addAll(generateMovesForPiece(piece))
        }
        
        return allMoves
    }
    
    // 간단한 평가 함수: 내 이동성과 상대방 이동성의 차이
    fun evaluateMove(move: Move): Int {
        // 말 이동 시뮬레이션
        val from = move.from
        val to = move.to
        val arrow = move.arrow
        
        val originalPiece = board[from.row][from.col]
        board[from.row][from.col] = '.'
        board[to.row][to.col] = originalPiece
        board[arrow.row][arrow.col] = '-'
        
        // 이동성 평가
        var myMobility = 0
        var opponentMobility = 0
        
        for (myPiece in myPieces) {
            val currentPiece = if (myPiece == from) to else myPiece
            for (dir in Position.directions) {
                myMobility += getMovesInDirection(currentPiece, dir).size
            }
        }
        
        for (opponentPiece in opponentPieces) {
            for (dir in Position.directions) {
                opponentMobility += getMovesInDirection(opponentPiece, dir).size
            }
        }
        
        // 원래대로 복구
        board[arrow.row][arrow.col] = '.'
        board[to.row][to.col] = '.'
        board[from.row][from.col] = originalPiece
        
        return myMobility - opponentMobility
    }
}

fun main(args : Array<String>) {
    val input = Scanner(System.`in`)
    val boardSize = input.nextInt() // height and width of the board
    val gameBoard = GameBoard(boardSize)

    // game loop
    while (true) {
        val color = input.next() // current color of your pieces ("w" or "b")
        val boardLines = mutableListOf<String>()
        
        for (i in 0 until boardSize) {
            boardLines.add(input.next()) // horizontal row
        }
        
        gameBoard.update(color, boardLines)
        
        val lastAction = input.next() // last action made by the opponent ("null" if it's the first turn)
        val actionsCount = input.nextInt() // number of legal actions

        val possibleMoves = gameBoard.generateAllMoves()
        
        if (possibleMoves.isEmpty()) {
            System.err.println("No valid moves found!")
            println("random") // 이동할 수 없는 경우 (게임 종료 상황)
        } else {
            // 첫 턴에는 시간이 더 많으므로 더 깊게 탐색
            val isFirstTurn = lastAction == "null"
            val evaluatedMoves = possibleMoves.associateWith { gameBoard.evaluateMove(it) }
            val bestMove = evaluatedMoves.maxByOrNull { it.value }?.key ?: possibleMoves.random()
            
            System.err.println("Selected move: ${bestMove.toNotation(boardSize)}")
            println(bestMove.toNotation(boardSize))
        }
    }
}
