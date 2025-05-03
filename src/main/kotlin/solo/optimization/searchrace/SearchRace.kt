import java.util.*
import java.io.*
import java.lang.Math.*
import kotlin.math.*
import kotlin.system.measureTimeMillis

data class Vector2D(val x: Double, val y: Double) {
    operator fun plus(other: Vector2D) = Vector2D(x + other.x, y + other.y)
    operator fun minus(other: Vector2D) = Vector2D(x - other.x, y - other.y)
    operator fun times(scalar: Double) = Vector2D(x * scalar, y * scalar)
    fun length(): Double = sqrt(x * x + y * y)
    fun normalize(): Vector2D {
        val len = length()
        return if (len > 0) Vector2D(x / len, y / len) else this
    }
    fun rotate(angle: Double): Vector2D {
        val rad = toRadians(angle)
        val cos = cos(rad)
        val sin = sin(rad)
        return Vector2D(x * cos - y * sin, x * sin + y * cos)
    }
    fun dot(other: Vector2D): Double = x * other.x + y * other.y
}

data class Node(
    val checkpointIndex: Int,
    val checkpointDistance: Double,
    val directionValue: Double,
    val random: Double,
    val position: Vector2D,
    val velocity: Vector2D,
    val direction: Vector2D
) : Comparable<Node> {
    override fun compareTo(other: Node): Int {
        if (checkpointIndex != other.checkpointIndex) {
            return other.checkpointIndex - checkpointIndex
        }
        if (abs(checkpointDistance - other.checkpointDistance) > 0.001) {
            return (checkpointDistance - other.checkpointDistance).toInt()
        }
        if (abs(directionValue - other.directionValue) > 0.001) {
            return (directionValue - other.directionValue).toInt()
        }
        return (random - other.random).toInt()
    }
}

data class Action(val angle: Int, val thrust: Int)

fun debug(vararg msg: Any) {
    // System.err.println(msg.joinToString(" "))
}

fun apply(node: Node, action: Action, checkpoints: List<Vector2D>): Node {
    var (checkpointIndex, _, _, random, position, velocity, direction) = node
    val (angle, thrust) = action

    // 방향 업데이트
    direction = direction.rotate(angle.toDouble())
    
    // 속도 업데이트
    velocity = velocity + direction * thrust.toDouble()
    
    // 위치 업데이트
    position = position + velocity
    
    // 속도 감소
    velocity = velocity * 0.85
    
    try {
        var checkpointDistance = (position - checkpoints[-checkpointIndex]).length()
        if (checkpointDistance <= 600) {
            checkpointIndex -= 1
            checkpointDistance = (position - checkpoints[-checkpointIndex]).length()
        }
        val directionValue = (position - checkpoints[-checkpointIndex]).dot(direction)
        return Node(checkpointIndex, checkpointDistance, directionValue, Math.random(), position, velocity, direction)
    } catch (e: IndexOutOfBoundsException) {
        return Node(checkpointIndex, 0.0, 0.0, Math.random(), position, velocity, direction)
    }
}

fun getAction(checkpoints: List<Vector2D>, checkpointIndex: Int, car: IntArray): Triple<Int, Int, Int> {
    val startTime = System.currentTimeMillis() / 1000.0
    val BEAM_WIDTH = 80
    val ALLOWED_TIME = 0.045 // 약간 줄여서 시간 초과 방지
    val STOP_FACTOR = 2

    val (x, y, vx, vy, angle) = car
    val position = Vector2D(x.toDouble(), y.toDouble())
    val velocity = Vector2D(vx.toDouble(), vy.toDouble())
    val direction = Vector2D(cos(toRadians(angle.toDouble())), sin(toRadians(angle.toDouble())))
    
    debug(position, velocity, direction)

    val startNode = Node(-checkpointIndex, 0.0, 0.0, 0.0, position, velocity, direction)
    val validActions = mutableListOf<Action>()
    for (a in -18..18 step 6) {
        for (t in listOf(0, 200)) {
            validActions.add(Action(a, t))
        }
    }

    var nodes = mutableMapOf<Node, Action>()
    for (action in validActions) {
        nodes[apply(startNode, action, checkpoints)] = action
    }

    var steps = 1
    while ((System.currentTimeMillis() / 1000.0 - startTime) * (1 + STOP_FACTOR / steps) < ALLOWED_TIME) {
        val newNodes = mutableMapOf<Node, Action>()
        for (node in nodes.keys.sorted().take(BEAM_WIDTH)) {
            for (action in validActions) {
                val newNode = apply(node, action, checkpoints)
                if (!newNodes.containsKey(newNode)) {
                    newNodes[newNode] = nodes[node]!!
                }
            }
        }
        nodes = newNodes
        steps += 1
    }

    val bestNode = nodes.keys.minOrNull()!!
    val (angle, thrust) = nodes[bestNode]!!

    debug(steps, bestNode)

    var finalThrust = thrust
    if (bestNode.checkpointIndex == -checkpointIndex && 
        bestNode.checkpointDistance > (position - checkpoints[checkpointIndex]).length() - steps * velocity.length() / 2) {
        finalThrust = 0
    }

    return Triple(angle, finalThrust, steps)
}

fun main(args : Array<String>) {
    val input = Scanner(System.`in`)
    val checkpointCount = input.nextInt()
    val checkpoints = mutableListOf<Vector2D>()
    
    // 체크포인트 정보 읽기
    for (i in 0 until checkpointCount) {
        val checkpointX = input.nextInt()
        val checkpointY = input.nextInt()
        checkpoints.add(Vector2D(checkpointX.toDouble(), checkpointY.toDouble()))
    }
    
    // Python 코드와 같이 체크포인트 3번 반복 (전체 배열 생성)
    val allCheckpoints = checkpoints + checkpoints + checkpoints
    
    debug("Checkpoints:", allCheckpoints)

    // 게임 루프
    while (true) {
        val checkpointIndex = input.nextInt()
        val x = input.nextInt()
        val y = input.nextInt()
        val vx = input.nextInt()
        val vy = input.nextInt()
        val angle = input.nextInt()
        
        val car = intArrayOf(x, y, vx, vy, angle)
        debug("Checkpoint index:", checkpointIndex, "Car:", car.joinToString())
        
        val (rotationAngle, thrust, steps) = getAction(allCheckpoints, checkpointIndex, car)
        
        // EXPERT 형식으로 출력
        println("EXPERT $rotationAngle $thrust $steps")
    }
}
