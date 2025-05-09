# <https://www.codingame.com/multiplayer/optimization/samegame>

## Learning Opportunities

This puzzle can be solved using the following concepts. Practice using these concepts and improve your skills.

Flood fill
Simulation
Beam Search

## Statement

This is a port of the tile-matching puzzle SameGame. The rules are the same as used for benchmarks in AI publications. Human-playable version is available here.

## The Goal

The goal of the game is to maximize your score, which can be achieved by removing as large as possible regions of connected cells of the same color.

## Rules

SameGame is a puzzle composed of a rectangular grid containing cells of different colors. A move removes connected cells of the same color. The cells of other colors fall to fill the void created by a move down, and when a column is empty the columns on the right are moved to the left. At least two cells have to be removed for a move to be legal. The score of a move is the square of the number of removed cells minus two. A bonus of one thousand is credited for completely clearing the board.

## Detailed rules

Game board has 15 rows and 15 columns, with 0 0 (column row) being the bottom left corner and 14 14 the upper right corner.
An action consist of choosing a tile on the bord, giving its coordinates (also using column row notation).
An action is legal if the pointed cell has at least one neighbour (in cardinal direction) of the same color.
There are five colors, encoded as integers: 0, 1, 2, 3, 4, and the empty cells are encoded as -1.
A legal action removes the entire region of the same color connected with the chosen tile.
In every column, all blocks that were above removed cells fall down, so that there is no free space between them and the row 0.
Then, for every empty column, all columns that are on the right are moved maximally to the left, so that the row 0 has no gaps.
Choosing illegal action i.e. pointing out an empty cell or a region of size 1 ends the game with an error.
The game ends naturally when there are no legal actions, i.e. the tile 0 0 is empty or all remaining regions have size 1.
The score of the game is a cumulative score of each action.
The score of an action removing a region of size n is (n-2)2.
When the board is cleared (tile 0 0 is empty), the score is additionally increased by 1000.

### Source code

You can find the source code of the contribution at:
https://github.com/acatai/SameGame
Contribution page at CodinGame


## details rules

```text
 	The Goal
The goal of the game is to maximize your score, which can be achieved by removing as large as possible regions of connected cells of the same color.
 	Rules
SameGame is a puzzle composed of a rectangular grid containing cells of different colors. A move removes connected cells of the same color. The cells of other colors fall to fill the void created by a move down, and when a column is empty the columns on the right are moved to the left. At least two cells have to be removed for a move to be legal. The score of a move is the square of the number of removed cells minus two. A bonus of one thousand is credited for completely clearing the board.

Detailed rules
Game board has 15 rows and 15 columns, with 0 0 (column row) being the bottom left corner and 14 14 the upper right corner.
An action consist of choosing a tile on the bord, giving its coordinates (also using column row notation).
An action is legal if the pointed cell has at least one neighbour (in cardinal direction) of the same color.
There are five colors, encoded as integers: 0, 1, 2, 3, 4, and the empty cells are encoded as -1.
A legal action removes the entire region of the same color connected with the chosen tile.
In every column, all blocks that were above removed cells fall down, so that there is no free space between them and the row 0.
Then, for every empty column, all columns that are on the right are moved maximally to the left, so that the row 0 has no gaps.
Choosing illegal action i.e. pointing out an empty cell or a region of size 1 ends the game with an error.
The game ends naturally when there are no legal actions, i.e. the tile 0 0 is empty or all remaining regions have size 1.
The score of the game is a cumulative score of each action.
The score of an action removing a region of size n is (n-2)2.
When the board is cleared (tile 0 0 is empty), the score is additionally increased by 1000.

Source code
You can find the source code of the contribution at:
https://github.com/acatai/SameGame
Contribution page at CodinGame
 	Game Input
Input for one game turn
First 15 lines: rows of the board, top (row 14) to bottom (row 0).

Each line: a single row consisting of 15 space-separated integers representing colors of each cell. Possible color values are: 0, 1, 2, 3, 4 and empty cell is -1.

Output for one game turn
A single line containing an action: two space-separated integers column row optionally followed by any text printed on-screen as a debug message.

Example: 0 1 this is optional\\nmessage
Constraints
Response time for first turn ≤ 20s
Response time for one turn ≤ 50ms
```