# <https://www.codingame.com/multiplayer/optimization/snake>

## Learning Opportunities

This puzzle can be solved using the following concepts. Practice using these concepts and improve your skills.

Pathfinding
Distances
Graph theory
Statement

## The Goal

The aim of this game is catch all the rabbits.

## Rules

Your program receives the first turn only the coordinates of the rabbits. After that, you receive each turn the coordinates of the snake.
You have to catch all the rabbits.

The game zone works as follows:
The map of the game is 54x96 size (height x width)

You lose if:
You get out the map.
You do not supply a valid sequence of actions.
The snake head touch his body.
Expert Rules
The snake must always be in movement. If the coordinates of the snake head is 50 10, and his body part is 49 10, 48 10, 47 10, etc...
You have to move in 3 directions, go up, go down and go right, and not go left because the snake head will touch his body. You can move left if you don't touch the snake body part. If you go up, you have to output 50 9.
If you go down, you have to output 50 11.
If you go right, you have to output 51 10.
It's the basic rules for the snake which is a famous game in the world.
The test cases are random or static.
Example
Choose a rabbit to catch , get over it and try to get the highest score. Your score is calculated like that when you catch some rabbits :
- You have a penalty if you don't catch a rabbit in 10 turns. The penalty is turn * NB_TURN_WITHOUT_CATCH_RABBIT.
- Then the score is calculated, SCORE += 10000 + add_combo - penalty. add_combo is equal to NB_CATCH_IN_2_TURNS_MIN * 15000. After 2 turns NB_CATCH_IN_2_TURNS_MIN is reset.
  Note
  Don’t forget to run the tests by launching them from the “Test cases” window. You can submit at any time to receive a score against the training validators. You can submit as many times as you like. Your most recent submission will be used for the final ranking.

Warning: the validation tests used to compute the final score are not the same as the ones used during the event. Hardcoded solutions will not score high.

Don't hesitate to change the viewer's options to help debug your code ().


---

## The Goal

The aim of this game is catch all the rabbits.

## Rules

Your program receives the first turn only the coordinates of the rabbits. After that, you receive each turn the coordinates of the snake.
You have to catch all the rabbits.

The game zone works as follows:
The map of the game is 54x96 size (height x width)

## You lose if:

You get out the map.
You do not supply a valid sequence of actions.
The snake head touch his body.

## Expert Rules

The snake must always be in movement. If the coordinates of the snake head is 50 10, and his body part is 49 10, 48 10, 47 10, etc...
You have to move in 3 directions, go up, go down and go right, and not go left because the snake head will touch his body. You can move left if you don't touch the snake body part. If you go up, you have to output 50 9.
If you go down, you have to output 50 11.
If you go right, you have to output 51 10.
It's the basic rules for the snake which is a famous game in the world.
The test cases are random or static.

## Example

Choose a rabbit to catch , get over it and try to get the highest score. Your score is calculated like that when you catch some rabbits :
- You have a penalty if you don't catch a rabbit in 10 turns. The penalty is turn * NB_TURN_WITHOUT_CATCH_RABBIT.
- Then the score is calculated, SCORE += 10000 + add_combo - penalty. add_combo is equal to NB_CATCH_IN_2_TURNS_MIN * 15000. After 2 turns NB_CATCH_IN_2_TURNS_MIN is reset.
  Note
  Don’t forget to run the tests by launching them from the “Test cases” window. You can submit at any time to receive a score against the training validators. You can submit as many times as you like. Your most recent submission will be used for the final ranking.

Warning: the validation tests used to compute the final score are not the same as the ones used during the event. Hardcoded solutions will not score high.

Don't hesitate to change the viewer's options to help debug your code ().

## Game Input

Input
Initialization Input:
Line 1: N an integer for the number of rabbits
N lines containing 2 space-separated integers X Y the coordinates of the rabbits.

Input for one game turn:
One Line: NS an integer for the number of snake body part.
NS lines containing 2 space-separated integers X Y the coordinates of the snake body parts.
PS: The fist X Y cooordinates are the coordinates of the snake head.

Output
2 space-separated integers X Y the new coordinates of the snake head.
Constraints
Allotted response time to output is ≤ 50 ms/turn in 600 turn max.