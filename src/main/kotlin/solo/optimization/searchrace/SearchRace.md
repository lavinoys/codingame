# <https://www.codingame.com/multiplayer/optimization/search-race>

## Learning Opportunities

This puzzle can be solved using the following concepts. Practice using these concepts and improve your skills.

## Simulation

Genetic Algorithm
Simulated Annealing
Beam Search
Statement

## The Goal

This is an optimization puzzle where you have to finish racing through a series of checkpoints as fast as possible.

## Rules

You are controlling a car racing through a series of checkpoints. Each checkpoint is located on position indicated with x and y. Which checkpoint to target is given by checkpointIndex pointing to a value in the checkpoints given as initial input.

Checkpoints are repeated when given as inputs. (in difference to CSB)
The game is played on a map 16000 units wide and 9000 units high. The coordinate X=0, Y=0 is the top left pixel.

### The checkpoints work as follows:

The checkpoints are circular, with a radius of 600 units.
The disposition of the checkpoints are set by the testcases.
No checkpoints are overlapping.


### The car work as follows:

Every turn it takes a new command given a position and a THRUST indicating where to drive.
It will at max turn 18 degrees from where the current heading are.
When the heading is set, car uses the given thrust to drive in the new direction.
To enter a checkpoint, the center of a car must be within 600 units of the checkpoints center.

### You lose if:

You use more than 600 rounds.
You do not supply a valid action.

### You win if:

You visit all checkpoints as given by inputs before the time is out!

## Expert Rules

On each turn the car movement are computed this way:
The car rotates to face the target point, with a maximum of 18 degrees.
The car's facing vector is multiplied by the given thrust value. The result is added to the current speed vector.
The speed vector is added to the position of the car.
The current speed vector is multiplied by 0.85
The speed's values are truncated, angles converted to degrees and rounded and the position's values are truncated.

Angle are provided in degrees, and relative to the x axis (0 degrees are pointing at (1.0). East = 0 degrees, South = 90 degrees.
If you're going to run local simulations, you'll need to look at the referee.


```text
 	The Goal
This is an optimization puzzle where you have to finish racing through a series of checkpoints as fast as possible.
 	Rules
You are controlling a car racing through a series of checkpoints. Each checkpoint is located on position indicated with x and y. Which checkpoint to target is given by checkpointIndex pointing to a value in the checkpoints given as initial input.

Checkpoints are repeated when given as inputs. (in difference to CSB)
The game is played on a map 16000 units wide and 9000 units high. The coordinate X=0, Y=0 is the top left pixel.

The checkpoints work as follows:
The checkpoints are circular, with a radius of 600 units.
The disposition of the checkpoints are set by the testcases.
No checkpoints are overlapping.


The car work as follows:
Every turn it takes a new command given a position and a THRUST indicating where to drive.
It will at max turn 18 degrees from where the current heading are.
When the heading is set, car uses the given thrust to drive in the new direction.
To enter a checkpoint, the center of a car must be within 600 units of the checkpoints center.

You lose if:
You use more than 600 rounds.
You do not supply a valid action.
You win if:
You visit all checkpoints as given by inputs before the time is out!

 	Expert Rules
On each turn the car movement are computed this way:
The car rotates to face the target point, with a maximum of 18 degrees.
The car's facing vector is multiplied by the given thrust value. The result is added to the current speed vector.
The speed vector is added to the position of the car.
The current speed vector is multiplied by 0.85
The speed's values are truncated, angles converted to degrees and rounded and the position's values are truncated.

Angle are provided in degrees, and relative to the x axis (0 degrees are pointing at (1.0). East = 0 degrees, South = 90 degrees.
If you're going to run local simulations, you'll need to look at the referee.
 	Game Input
The program must first read the initialization data from standard input. Then, provide to the standard output one line with X Y THRUST
Initialization input
First line: an integer checkpoints, the amount of all checkpoints to pass. (all checkpoints repeated 3 times for convenience)
Next checkpoints lines: one line per checkpoint.

Each checkpoint is represented by 2 integers: checkpointX, checkpointY.
Input for one game turn
One line of 6 integers: checkpointIndex, x, y, vx, vy and angle.
checkpointIndex indicates the index of the next checkpoint as given in initial inputs. x, y for the entity's position.
vx, vy for the entity's speed vector.
angle. Heading angle in degrees between 0 and 360 for the Car.
Output for one game turn
One line for your car: three integers X, Y and thrust
You may append the output with a message which we be displayed above the car.

Optional debugging information
If the message is debug once, the game summary will contain additional information throughout the game. The referee will provide a double with the collision time occuring the previous round (or > 1.0 if no collision occured)
Alternative output format
For convenience purposes, you may also output your actions in the following format: EXPERT rotationAngle thrust message.
Constraints
9 <= checkpoints <= 24
0 <= thrust <= 200
0 <= angle <= 360

Response time for the first turn ≤ 1000 ms
Response time per turn ≤ 50 ms
```