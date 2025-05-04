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


The Game of the Amazons (in Spanish, El Juego de las Amazonas; often called Amazons for short) is a two-player abstract strategy game invented in 1988 by Walter Zamkauskas of Argentina.[1] The game is played by moving pieces and blocking the opponents from squares, and the last player able to move is the winner. It is a member of the territorial game family, a distant relative of Go and chess.

The Game of the Amazons is played on a 10x10 chessboard (or an international checkerboard). Some players prefer to use a monochromatic board. The two players are White and Black; each player has four amazons (not to be confused with the amazon fairy chess piece), which start on the board in the configuration shown at right. A supply of markers (checkers, poker chips, etc.) is also required.

Rules
White moves first, and the players alternate moves thereafter. Each move consists of two parts. First, one moves one of one's own amazons one or more empty squares in a straight line (orthogonally or diagonally), exactly as a queen moves in chess; it may not cross or enter a square occupied by an amazon of either color or an arrow. Second, after moving, the amazon shoots an arrow from its landing square to another square, using another queenlike move. This arrow may travel in any orthogonal or diagonal direction (even backwards along the same path the amazon just traveled, into or across the starting square if desired). An arrow, like an amazon, cannot cross or enter a square where another arrow has landed or an amazon of either color stands. The square where the arrow lands is marked to show that it can no longer be used. The last player to be able to make a move wins. Draws are impossible.

a	b	c	d	e	f	g	h	i	j		
10	a10	b10	c10	d10 black queen	e10	f10	g10 black queen	h10	i10	j10	10
9	a9	b9	c9	d9	e9	f9	g9 black circle	h9	i9	j9	9
8	a8	b8	c8	d8	e8	f8	g8	h8	i8	j8	8
7	a7 black queen	b7	c7	d7	e7	f7	g7	h7	i7	j7 black queen	7
6	a6	b6	c6	d6 white queen	e6	f6	g6	h6	i6	j6	6
5	a5	b5	c5	d5	e5	f5	g5	h5	i5	j5	5
4	a4 white queen	b4	c4	d4	e4	f4	g4	h4	i4	j4 white queen	4
3	a3	b3	c3	d3	e3	f3	g3	h3	i3	j3	3
2	a2	b2	c2	d2	e2	f2	g2	h2	i2	j2	2
1	a1	b1	c1	d1	e1	f1	g1 white queen	h1	i1	j1	1
a	b	c	d	e	f	g	h	i	j		
The diagram shows a possible first move by white: d1-d6/g9, i.e. amazon moved from d1 to d6 and fired arrow to g9.
Territory and scoring
a	b	c	d	e	f	g	h	i	j		
10	a10 black circle	b10	c10	d10 black queen	e10 black circle	f10	g10	h10 black circle	i10	j10	10
9	a9	b9 black circle	c9 black circle	d9 black circle	e9	f9 black circle	g9	h9	i9 black circle	j9	9
8	a8 black circle	b8 black circle	c8 white queen	d8 black circle	e8 black circle	f8 black circle	g8 black circle	h8 black circle	i8 black circle	j8 black circle	8
7	a7	b7	c7 black circle	d7 black circle	e7 black circle	f7 white queen	g7	h7	i7 black circle	j7 white queen	7
6	a6	b6	c6 black circle	d6	e6 black circle	f6 black circle	g6 black circle	h6 black circle	i6 black circle	j6 black circle	6
5	a5	b5 black circle	c5 black circle	d5 black circle	e5 black queen	f5 black circle	g5	h5 black circle	i5	j5	5
4	a4	b4 black circle	c4 black circle	d4	e4	f4 black circle	g4 black circle	h4	i4	j4	4
3	a3 black circle	b3 black circle	c3	d3 black circle	e3 black circle	f3 black circle	g3 black circle	h3 black circle	i3	j3	3
2	a2 black circle	b2 black circle	c2	d2 black circle	e2 white queen	f2 black circle	g2 black circle	h2	i2	j2	2
1	a1	b1	c1 black circle	d1 black queen	e1 black circle	f1 black circle	g1 black queen	h1	i1	j1 black circle	1
a	b	c	d	e	f	g	h	i	j		
A completed Amazons game. White has just moved f1-e2/f1. White now has 8 moves left, while Black has 31.
The strategy of the game is based on using arrows (as well as one's four amazons) to block the movement of the opponent's amazons and gradually wall off territory, trying to trap the opponents in smaller regions and gain larger areas for oneself. Each move reduces the available playing area, and eventually each amazon finds itself in a territory blocked off from all other amazons. The amazon can then move about its territory firing arrows until it no longer has any room to move. Since it would be tedious to actually play out all these moves, in practice the game usually ends when all of the amazons are in separate territories. The player with the largest amount of territory will be able to win, as the opponent will have to fill in their own territory more quickly.

Scores are sometimes used for tie-breaking purposes in Amazons tournaments. When scoring, it is important to note that although the number of moves remaining to a player is usually equal to the number of empty squares in the territories occupied by that player's amazons, it is nonetheless possible to have defective territories in which there are fewer moves left than there are empty squares. The simplest such territory is three squares of the same colour, not in a straight line, with the amazon in the middle (for example, a1+b2+c1 with the amazon at b2).

History
El Juego de las Amazonas was first published in Spanish in the Argentine puzzle magazine El Acertijo in December 1992. An approved English translation written by Michael Keller appeared in the magazine World Game Review in January 1994.[1] Other game publications also published the rules, and the game gathered a small but devoted following. The Internet spread the game more widely.

Michael Keller wrote the first known computer version of the game in VAX Fortran in 1994,[2] and an updated version with graphics in Visual Basic in 1995.[1][2] There are Amazons tournaments at the Computer Olympiad, a series of computer-versus-computer competitions.

El Juego de las Amazonas (The Game of the Amazons) is a trademark of Ediciones de Mente.

Computational complexity
Usually, in the endgame, the board is partitioned into separate "royal chambers", with queens inside each chamber. We define simple Amazons endgames to be endgames where each chamber has at most one queen. Determining who wins in a simple Amazons endgame is NP-hard.[3] This is proven by reducing it to finding the Hamiltonian path of a cubic subgraph of the square grid graph.

Generalized Amazons (that is, determining the winner of a game of Amazons played on a n x n grid, started from an arbitrary configuration) is PSPACE-complete.[4][5] This can be proved in two ways.

The first way is by reducing a generalized Hex position, which is known to be PSPACE-complete,[6] into an Amazons position.
The second way is by reducing a certain kind of generalized geography called GEOGRAPHY-BP3, which is PSPACE-complete, to an Amazons position. This Amazons position uses only one black queen and one white queen, thus showing that generalized Amazons is PSPACE-complete even if only one queen on each side is allowed.