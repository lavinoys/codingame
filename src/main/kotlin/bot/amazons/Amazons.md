# <https://www.codingame.com/multiplayer/bot-programming/amazons>

## Rules

Learning Opportunities
This puzzle can be solved using the following concepts. Practice using these concepts and improve your skills.

Monte Carlo tree search
Minimax
Bitboarding
Statement
This is a port of the board game Amazons.
Click here for the referee.
Boss made by Astrobytes.
Rules
2 games will be played, first player will start with white pieces and second player with black pieces, once this game end, second player will play with white pieces and first player with black pieces.


Game is played on a 8x8.
Starting position is randomized.
Game goes on until a player can no longer move.


Legal action:
The piece must move in any direction (orthogonally or diagonally) any number of squares, it may not cross or go to a square occupied by a piece OR occupied by a wall. After moving it must place a wall, the wall placement must meet the same requirements as the movement (it may place a wall in the direction the piece moved from) .

Resolution of a turn:
A bot must output the coordinates of the pieces he wants to move, to where and where he wants to place the wall (e.g. d8d1d7).

Output
If the coordinates are outside of the board, the game will end and the other player will win.
If the coordinates are inside but the square at those coordinates already is occupied or the path to that square crosses any wall or piece, the game will end and the other player will win.
Expert Rules
Draws are impossible.
Victory Conditions
Be the last player to move.
Loss Conditions
Have no legal moves.
You do not respond in time or output an unrecognized command.

 	Rules
2 games will be played, first player will start with white pieces and second player with black pieces, once this game end, second player will play with white pieces and first player with black pieces.


Game is played on a 8x8.
Starting position is randomized.
Game goes on until a player can no longer move.


Legal action:
The piece must move in any direction (orthogonally or diagonally) any number of squares, it may not cross or go to a square occupied by a piece OR occupied by a wall. After moving it must place a wall, the wall placement must meet the same requirements as the movement (it may place a wall in the direction the piece moved from) .

Resolution of a turn:
A bot must output the coordinates of the pieces he wants to move, to where and where he wants to place the wall (e.g. d8d1d7).

Output
If the coordinates are outside of the board, the game will end and the other player will win.
If the coordinates are inside but the square at those coordinates already is occupied or the path to that square crosses any wall or piece, the game will end and the other player will win.
Expert Rules
Draws are impossible.
Victory Conditions
Be the last player to move.
Loss Conditions
Have no legal moves.
You do not respond in time or output an unrecognized command.
Game Input
Initial input
First line: boardSize: the number of rows and columns on the board.
Input for one game turn
Next color: the color of your pieces ("w": white, "b":black).
Next boardSize lines: a string of characters representing one horizontal row of the grid, top to bottom. ('.': empty, 'w': white, 'b': black, "-": wall).
Next lastAction: the last action made by the opponent ("null" if it's the first turn).
Next actionsCount: the number of legal actions for this turn.
Output
A single line containing the coordinates where you want to move from, to and where you want to place the wall. e.g. "d8d1d7".
You can also print messages by doing the following. e.g. "d8d1d7 msg message".
Constraints
Response time first turn is ≤ 1000 ms.
Response time per turn is ≤ 100 ms.