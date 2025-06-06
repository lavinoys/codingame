# <https://www.codingame.com/multiplayer/optimization/travelling-salesman>

## Learning Opportunities

This puzzle can be solved using the following concepts. Practice using these concepts and improve your skills.

- Greedy algorithms
- Simulated Annealing
- Heuristic search

## Statement
The Travelling Salesman Problem
A certain number of points is given. The goal is to visit all other points exactly once from the starting point and then return to the starting point again. The starting point is always the first given point.

Sounds not very hard at first, but it is an NP-hard problem, so bruteforce is impossible. What we are going to use are heuristic algorithms, which don't find the optimal solution but a solution close to the best.

There is a puzzle about the TSP on this website already, which features one heuristic algorithm called "Nearest neighbor".

## Rules

Rules are simple:

- You win if your output is correct!
- You lose if your output isn't correct!
  Note
  Don’t forget to run the tests by launching them from the “Test cases” window. You can submit at any time to receive a score against the training validators. You can submit as many times as you like. Your most successful submission will be used for the final ranking.

Warning: the validation tests used to compute the final score are not the same as the ones used during the event. Harcoded solutions will not score highly.


## The Travelling Salesman Problem

A certain number of points is given. The goal is to visit all other points exactly once from the starting point and then return to the starting point again. The starting point is always the first given point.

Sounds not very hard at first, but it is an NP-hard problem, so bruteforce is impossible. What we are going to use are heuristic algorithms, which don't find the optimal solution but a solution close to the best.

There is a puzzle about the TSP on this website already, which features one heuristic algorithm called "Nearest neighbor".

## Rules

Rules are simple:

- You win if your output is correct!
- You lose if your output isn't correct!

## Note

Don’t forget to run the tests by launching them from the “Test cases” window. You can submit at any time to receive a score against the training validators. You can submit as many times as you like. Your most successful submission will be used for the final ranking.

Warning: the validation tests used to compute the final score are not the same as the ones used during the event. Harcoded solutions will not score highly.

## Game Input

The program must first read the xy coordinates of the given points from standard input, then provide to the standard output one line with the indexes of the points in the order your algorithm finds best, but it has always to start and to end with index 0

### Input

Line 1: an integer N, the number of input points

N lines: two integers X and Y, the coordinates of a given point

### Output

A single line containing each index of all given points separated by a space

### Constraints

0 < N <= 300
0 < X < 1800
0 < Y < 1000

Allotted response time to output is ≤ 5 seconds.

### Example

Given following input:
4
0 0   // This is point 0
0 2   // This is point 1
2 0   // This is point 2
2 2   // This is point 3

The shortest path from start point 0 to all other points and back to 0 is 0 - 1 - 3 - 2 - 0, therefore we output 0 1 3 2 0
Also 0 1 2 3 0 would be valid, but it doesn't get scored that high, since it has a higher cost