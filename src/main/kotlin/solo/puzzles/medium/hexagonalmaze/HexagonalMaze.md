# <https://www.codingame.com/training/medium/hexagonal-maze>


## Learning Opportunities

This puzzle can be solved using the following concepts. Practice using these concepts and improve your skills.


## Statement

Goal
You are in a maze, made of hexagonal cells.
The following grid :
4 4
ABCD
EFGH
IJKL
MNOP

has to be understood like this :
/ \   / \   / \   / \
/   \ /   \ /   \ /   \
|     |     |     |     |
|  A  |  B  |  C  |  D  |    Line 0
|     |     |     |     |
\   / \   / \   / \   / \
\ /   \ /   \ /   \ /   \
|     |     |     |     |
|  E  |  F  |  G  |  H  | Line 1
|     |     |     |     |
/ \   / \   / \   / \   /
/   \ /   \ /   \ /   \ /
|     |     |     |     |
|  I  |  J  |  K  |  L  |    Line 2
|     |     |     |     |
\   / \   / \   / \   / \
\ /   \ /   \ /   \ /   \
|     |     |     |     |
|  M  |  N  |  O  |  P  | Line 3
|     |     |     |     |
\   / \   / \   / \   /
\ /   \ /   \ /   \ /

This means each cell has 6 neighbours : for example, cell F is surrounded by B, C, E, G, J, K.
The grid is periodic, if you go left you appear on the right if there is no wall, and vice versa, similarly with up/down.
So cell B also has 6 neighbours : M, N, A, C, E, F.
Even lines are left-aligned, odd lines are right-aligned.

You are given a grid made by walls and free spaces, you have to draw the shortest path to go from the start to the end.
There may be more than one path, but only one shortest path.
There is always a solution.

The grid contains following symbols :
```text
# : wall
_ : free space
S : start
E : end
You must output the same grid with symbols . on cells which are on the shortest way.
```
 	Goal
You are in a maze, made of hexagonal cells.
The following grid :
4 4
ABCD
EFGH
IJKL
MNOP

has to be understood like this :
/ \   / \   / \   / \
/   \ /   \ /   \ /   \
|     |     |     |     |
|  A  |  B  |  C  |  D  |    Line 0
|     |     |     |     |
\   / \   / \   / \   / \
\ /   \ /   \ /   \ /   \
|     |     |     |     |
|  E  |  F  |  G  |  H  | Line 1
|     |     |     |     |
/ \   / \   / \   / \   /
/   \ /   \ /   \ /   \ /
|     |     |     |     |
|  I  |  J  |  K  |  L  |    Line 2
|     |     |     |     |
\   / \   / \   / \   / \
\ /   \ /   \ /   \ /   \
|     |     |     |     |
|  M  |  N  |  O  |  P  | Line 3
|     |     |     |     |
\   / \   / \   / \   /
\ /   \ /   \ /   \ /

This means each cell has 6 neighbours : for example, cell F is surrounded by B, C, E, G, J, K.
The grid is periodic, if you go left you appear on the right if there is no wall, and vice versa, similarly with up/down.
So cell B also has 6 neighbours : M, N, A, C, E, F.
Even lines are left-aligned, odd lines are right-aligned.

You are given a grid made by walls and free spaces, you have to draw the shortest path to go from the start to the end.
There may be more than one path, but only one shortest path.
There is always a solution.

The grid contains following symbols :
``` text
# : wall
_ : free space
S : start
E : end
You must output the same grid with symbols . on cells which are on the shortest way.
Input
First line: two integers w and h, width and height of the grid.

h following lines: the grid.
Output
h lines : the grid with the answer.
Constraints
4 ≤ w, h ≤ 25
h is always even.
Example
Input
5 6
#####
#S#E#
#_#_#
#_#_#
#___#
#####
Output
#####
#S#E#
#.#.#
#.#.#
#_..#
```
