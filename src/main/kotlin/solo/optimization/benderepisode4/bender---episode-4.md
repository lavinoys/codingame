# <https://www.codingame.com/multiplayer/optimization/bender---episode-4>

```text
Learning Opportunities
This puzzle can be solved using the following concepts. Practice using these concepts and improve your skills.

Pathfinding
Optimization
Statement
Bender wants to visit his friend Fry to drink some beer. Unfortunately he's stuck in a maze and professor Farnsworth is experimenting with magnetic fields that Bender doesn't want to enter. Help Bender by giving him a plan how to escape the maze - but beware: because of his movie collection, there is only little space left on his disc for the program.
 	The Goal
Help Bender to find a path to a given point inside the maze. Compress the path as much as possible. There are switches on the ground that Bender will toggle by entering their cell. These switches make other fields passable or impassable.
 	Rules
You are given a maze with a starting and target point. Find a path through the maze. There are magnetic fields on some cells that have to be avoided. You can turn them off by hitting the corresponding switch.

Coordinate system
The top-left corner has position (0,0). x goes to the right, y increases downwards.

Switches
A switch has a location of its own as well as a location of the magnetic field it controls. Each time Bender enters the cell of the switch, he will toggle the state of the magnetic field, turning it on and off.

Garbage balls
A garbage ball is a movable wall. Bender can move them by walking into them, if there is no wall or another garbage ball behind. If he pushes a garbage ball on a switch, it will toggle the switch. If he pushes a garbage ball on Fry, it will squash Fry and kill him.

Movement
Bender can go in the four directions UP, DOWN, RIGHT and LEFT. In case he hits a wall, he just stays at the current position. An active magnetic field erases his entire harddisc.

Functions
To encode the path more efficently, you can define functions as sequences of moves. Functions can call other functions as well as themselves. Functions will be executed char by char. When there is no more char left in the called function, the program will resume in the caller function, after the function call that was just performed.
 	Advanced Details
Every map is solvable without moving any garbage balls. In this case the optimal solution without using functions will be above 150.
You can see the game's source code on https://github.com/eulerscheZahl/Bender4.

Thanks to Zerplin for helping me with the graphics
Victory Conditions
Bender reaches the cell, where Fry is
Loss Conditions
You enter a magnetic field.
You don't reach Fry's cell within 1000 turns.
You squash Fry with a garbage ball.
You do not respond in time or output an unrecognized command.
```

Bender wants to visit his friend Fry to drink some beer. Unfortunately he's stuck in a maze and professor Farnsworth is experimenting with magnetic fields that Bender doesn't want to enter. Help Bender by giving him a plan how to escape the maze - but beware: because of his movie collection, there is only little space left on his disc for the program.

## The Goal

Help Bender to find a path to a given point inside the maze. Compress the path as much as possible. There are switches on the ground that Bender will toggle by entering their cell. These switches make other fields passable or impassable.
## Rules

You are given a maze with a starting and target point. Find a path through the maze. There are magnetic fields on some cells that have to be avoided. You can turn them off by hitting the corresponding switch.

### Coordinate system

The top-left corner has position (0,0). x goes to the right, y increases downwards.

### Switches

A switch has a location of its own as well as a location of the magnetic field it controls. Each time Bender enters the cell of the switch, he will toggle the state of the magnetic field, turning it on and off.

### Garbage balls

A garbage ball is a movable wall. Bender can move them by walking into them, if there is no wall or another garbage ball behind. If he pushes a garbage ball on a switch, it will toggle the switch. If he pushes a garbage ball on Fry, it will squash Fry and kill him.

### Movement

Bender can go in the four directions UP, DOWN, RIGHT and LEFT. In case he hits a wall, he just stays at the current position. An active magnetic field erases his entire harddisc.

### Functions

To encode the path more efficently, you can define functions as sequences of moves. Functions can call other functions as well as themselves. Functions will be executed char by char. When there is no more char left in the called function, the program will resume in the caller function, after the function call that was just performed.

## Advanced Details

Every map is solvable without moving any garbage balls. In this case the optimal solution without using functions will be above 150.
You can see the game's source code on https://github.com/eulerscheZahl/Bender4.

Thanks to Zerplin for helping me with the graphics

## Victory Conditions

Bender reaches the cell, where Fry is

## Loss Conditions

You enter a magnetic field.
You don't reach Fry's cell within 1000 turns.
You squash Fry with a garbage ball.
You do not respond in time or output an unrecognized command.

## Game Input

First line: the width and height of the maze
Next height lines: The board as strings of length width. The characters can be:
.: empty cell
#: wall
+: garbage ball
Next line: startX, startY, the starting position
Next line: targetX, targetY, the target position
Next line: switchCount, the number of switches
Next switchCount lines: switchX, switchY, blockX, blockY, initialState
switchX, switchY give the location of the switch,
blockX, blockY define the location of the magnetic field.
initialState is either 1 (initially on) or 0 (initially off).

## Output

Print one single line: the path that Bender shall take.
Each character can be:
U go up
D go down
R go right
L go left
1, 2,..., 9 call functions 1,2,...,9 respectively
To define functions, just append them, separated by a semicolon: initial code; function 1; function 2.

The solution will be scored by the length of the string, the shorter the better.

## Constraints

width = height = 21 (10 for introduction levels)
8 ≤ switchCount ≤ 11 (except for introduction levels)
5 ≤ garbageBalls ≤ 10 (except for introduction levels)

Response time ≤ 1000 ms