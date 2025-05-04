import sys
import math
import cmath
from time import perf_counter
from random import random

BEAM_WIDTH = 80
ALLOWED_TIME = 0.050
STOP_FACTOR = 2


def debug(*msg):
    return
    print(*msg, file=sys.stderr, flush=True)


def apply(node, action, checkpoints):
    checkpoint_index, _, _, _, position, velocity, direction = node
    angle, thrust = action
    direction *= cmath.rect(1, math.radians(angle))
    velocity += thrust * direction
    position += velocity
    velocity *= 0.85

    try:
        checkpoint_distance = abs(position - checkpoints[-checkpoint_index])
        if checkpoint_distance <= 600:
            checkpoint_index -= 1
            checkpoint_distance = abs(position - checkpoints[-checkpoint_index])
        direction_value = (direction.conjugate() * (position - checkpoints[-checkpoint_index])).real
        return checkpoint_index, checkpoint_distance, direction_value, random(), position, velocity, direction
    except IndexError:
        return checkpoint_index, 0, 0, random(), position, velocity, direction


def get_action(checkpoints, checkpoint_index, car):
    start_time = perf_counter()
    x, y, vx, vy, angle = car
    position = x + y * 1j
    velocity = vx + vy * 1j
    direction = cmath.rect(1, math.radians(angle))
    debug(position, velocity, direction)

    start_node = (-checkpoint_index, 0, 0, 0, position, velocity, direction)
    valid_actions = tuple((angle, thrust) for angle in range(-18, 19, 6) for thrust in range(0, 201, 200))
    nodes = {}
    for action in valid_actions:
        nodes[apply(start_node, action, checkpoints)] = action

    steps = 1
    while (perf_counter() - start_time) * (1 + STOP_FACTOR / steps) < ALLOWED_TIME:
        new_nodes = {}
        for node in sorted(nodes)[:BEAM_WIDTH]:
            for action in valid_actions:
                new_nodes[apply(node, action, checkpoints)] = nodes[node]
        nodes = new_nodes
        steps += 1

    best_node = min(nodes)
    angle, thrust = nodes[best_node]
    debug(steps, best_node)

    if best_node[0] == -checkpoint_index and best_node[1] > abs(position - checkpoints[checkpoint_index]) - steps * abs(
            velocity) / 2:
        thrust = 0

    return angle, thrust, steps


def main():
    checkpoints = tuple(x + 1j * y for x, y in (map(int, input().split()) for _ in range(int(input())))) * 3
    debug(checkpoints)

    while True:
        checkpoint_index, *car = map(int, input().split())
        debug(checkpoint_index, car)

        rotation_angle, thrust, message = get_action(checkpoints, checkpoint_index, car)
        print(f'EXPERT {rotation_angle} {thrust} {message}')


if __name__ == "__main__":
    main()