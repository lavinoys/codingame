import sys
import math

# Auto-generated code below aims at helping you parse
# the standard input according to the problem statement.

n = input()

# Write an answer using print
# To debug: print("Debug messages...", file=sys.stderr, flush=True)
for c in n[::-1]:
    print(int(c)-1, end='')
