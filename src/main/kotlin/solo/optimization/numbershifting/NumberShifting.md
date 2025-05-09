# <https://www.codingame.com/multiplayer/optimization/number-shifting>

## Statement

### The Goal

This is an offline optimization game. Unlike other optimization puzzles, this one has a correct solution. You will start with an introduction level. When a level is solved, you will get another (harder) level. The goal is to solve as many levels as possible.

### Rules

You are given a grid, filled with numbers. You can move a number horizontally or vertically by exactly as many cells as the value of the number. The number has to be pushed on another non-zero number. The moved number will then be added to the other number or subtracted. The absolute value will be taken on subtraction. The goal is to clear the board and not have any numbers remaining.
The top left corner has the coordinate (0,0). X increases to the right, y to the bottom.

You can find the source code of the game at https://github.com/eulerscheZahl/NumberShifting.
You can play the game interactively at https://eulerschezahl.github.io/NumberShifting.html (note: the levels are not the same as on CodinGame).
This game is inspired by Pusherboy.

For convenience you can use this python script to test your solution and download the next level. Save the files from this directory and enter your login data at the beginning of the script. Of course you can also test your solutions using the CodinGame website directly.


## The Goal

This is an offline optimization game. Unlike other optimization puzzles, this one has a correct solution. You will start with an introduction level. When a level is solved, you will get another (harder) level. The goal is to solve as many levels as possible.

## Rules

You are given a grid, filled with numbers. You can move a number horizontally or vertically by exactly as many cells as the value of the number. The number has to be pushed on another non-zero number. The moved number will then be added to the other number or subtracted. The absolute value will be taken on subtraction. The goal is to clear the board and not have any numbers remaining.
The top left corner has the coordinate (0,0). X increases to the right, y to the bottom.

You can find the source code of the game at https://github.com/eulerscheZahl/NumberShifting.
You can play the game interactively at https://eulerschezahl.github.io/NumberShifting.html (note: the levels are not the same as on CodinGame).
This game is inspired by Pusherboy.

For convenience you can use this python script to test your solution and download the next level. Save the files from this directory and enter your login data at the beginning of the script. Of course you can also test your solutions using the CodinGame website directly.

## Game Input

The program must output the level code first. The code of the first level is first_level.

Then the level will be given as follows:
Line 1: width height, the size of the level.
Next height lines: width integers cell, the values in the grid.

Then the program has to output the actions to solve the game, each in a new line:
x y dir +/-, the position of the number that shall be moved, the direction (U,D,R,L) and a + to add the numbers or a - to subtract them.

When the solution was correct, the code to play the next level will be given in the game summary.