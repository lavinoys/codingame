import java.util.*
import kotlin.math.*

/**
 * Mad Pod Racing Gold League Implementation
 * Based on the rules and strategies from MadPodRacingGold.md
 */

// Data classes for game entities
data class Point(val x: Int, val y: Int) {
    fun distance(other: Point): Double = 
        sqrt((x - other.x).toDouble().pow(2) + (y - other.y).toDouble().pow(2))

    fun angle(other: Point): Double = 
        atan2((other.y - y).toDouble(), (other.x - x).toDouble()) * 180 / PI
}

data class Checkpoint(val position: Point, val id: Int)

enum class PodRole {
    RACER, BLOCKER
}

// Base Pod class with common functionality
open class BasePod(
    var position: Point,
    var velocity: Pair<Int, Int>,
    var angle: Int,
    var nextCheckpointId: Int
) {
    fun distanceToCheckpoint(checkpoint: Checkpoint): Double = 
        position.distance(checkpoint.position)

    fun angleToCheckpoint(checkpoint: Checkpoint): Double {
        val targetAngle = position.angle(checkpoint.position)
        val angleDiff = (targetAngle - angle + 360) % 360
        return if (angleDiff > 180) angleDiff - 360 else angleDiff
    }

    open fun update(x: Int, y: Int, vx: Int, vy: Int, angle: Int, nextCheckpointId: Int) {
        position = Point(x, y)
        velocity = Pair(vx, vy)
        this.angle = angle
        this.nextCheckpointId = nextCheckpointId
    }
}

// MyPod class for player-controlled pods
class MyPod(
    position: Point,
    velocity: Pair<Int, Int>,
    angle: Int,
    nextCheckpointId: Int,
    var boostAvailable: Boolean = true,
    var shieldCooldown: Int = 0,
    var role: PodRole = PodRole.RACER
) : BasePod(position, velocity, angle, nextCheckpointId) {
    var targetX: Int = 0
    var targetY: Int = 0
    var thrust: Int = 100
    var useShield: Boolean = false
    var useBoost: Boolean = false

    override fun update(x: Int, y: Int, vx: Int, vy: Int, angle: Int, nextCheckpointId: Int) {
        super.update(x, y, vx, vy, angle, nextCheckpointId)

        // Update shield cooldown
        if (shieldCooldown > 0) {
            shieldCooldown--
        }
    }

    fun calculateTarget(checkpoints: List<Checkpoint>, checkpointCount: Int) {
        val currentCheckpoint = checkpoints[nextCheckpointId]
        val nextCheckpointId = (nextCheckpointId + 1) % checkpointCount
        val nextCheckpoint = checkpoints[nextCheckpointId]
        val distance = distanceToCheckpoint(currentCheckpoint)

        if (distance < 1200 && role == PodRole.RACER) {
            // If close to checkpoint, aim for the next one
            val nextX = currentCheckpoint.position.x + (nextCheckpoint.position.x - currentCheckpoint.position.x) * 0.3
            val nextY = currentCheckpoint.position.y + (nextCheckpoint.position.y - currentCheckpoint.position.y) * 0.3
            targetX = nextX.toInt()
            targetY = nextY.toInt()
        } else {
            targetX = currentCheckpoint.position.x
            targetY = currentCheckpoint.position.y
        }
    }

    fun calculateThrust(checkpoints: List<Checkpoint>, turn: Int, opponentPods: List<OpponentPod>, sharedBoostAvailable: Boolean) {
        val currentCheckpoint = checkpoints[nextCheckpointId]
        val distance = distanceToCheckpoint(currentCheckpoint)
        val angleDiff = Math.abs(angleToCheckpoint(currentCheckpoint))

        // Reset flags
        useShield = false
        useBoost = false

        // Determine thrust based on angle and distance
        thrust = when {
            angleDiff > 90 -> 0
            angleDiff > 50 -> 50
            distance < 1000 -> 70
            else -> 100
        }

        // Decide whether to use boost
        if (sharedBoostAvailable && 
            angleDiff < 10 && 
            distance > 5000 &&
            turn > 3 &&
            role == PodRole.RACER) {
            useBoost = true
        }

        // Decide whether to use shield
        if (shieldCooldown == 0 && 
            opponentPods.any { 
                position.distance(it.position) < 850 && 
                Math.abs(velocity.first - it.velocity.first) + Math.abs(velocity.second - it.velocity.second) > 300 
            }) {
            useShield = true
        }
    }

    fun calculateBlockerTarget(leadingOpponent: OpponentPod, checkpoints: List<Checkpoint>, blockOpponent: Boolean) {
        if (blockOpponent) {
            // Aim to intercept the leading opponent
            targetX = leadingOpponent.position.x + leadingOpponent.velocity.first
            targetY = leadingOpponent.position.y + leadingOpponent.velocity.second
            thrust = 100
        } else {
            // Race normally
            val currentCheckpoint = checkpoints[nextCheckpointId]
            targetX = currentCheckpoint.position.x
            targetY = currentCheckpoint.position.y

            val angleDiff = Math.abs(angleToCheckpoint(currentCheckpoint))
            thrust = when {
                angleDiff > 90 -> 0
                angleDiff > 50 -> 50
                else -> 100
            }
        }
    }

    fun getCommand(): String {
        return when {
            useShield -> "$targetX $targetY SHIELD"
            useBoost -> "$targetX $targetY BOOST"
            else -> "$targetX $targetY $thrust"
        }
    }

    fun activateShield() {
        if (useShield) {
            shieldCooldown = 3
        }
    }
}

// OpponentPod class for tracking opponent pods
class OpponentPod(
    position: Point,
    velocity: Pair<Int, Int>,
    angle: Int,
    nextCheckpointId: Int,
    var laps: Int = 0,
    var previousCheckpointId: Int = 0
) : BasePod(position, velocity, angle, nextCheckpointId) {

    override fun update(x: Int, y: Int, vx: Int, vy: Int, angle: Int, nextCheckpointId: Int) {
        // Track lap completion - if we've gone from last checkpoint to first checkpoint
        if (this.nextCheckpointId != nextCheckpointId && this.nextCheckpointId > nextCheckpointId) {
            laps++
            System.err.println("Opponent completed a lap! Now on lap: $laps")
        }

        previousCheckpointId = this.nextCheckpointId
        super.update(x, y, vx, vy, angle, nextCheckpointId)
    }
}

fun main(args : Array<String>) {
    val input = Scanner(System.`in`)
    val laps = input.nextInt()
    val checkpointCount = input.nextInt()

    // Store checkpoints
    val checkpoints = mutableListOf<Checkpoint>()
    for (i in 0 until checkpointCount) {
        val checkpointX = input.nextInt()
        val checkpointY = input.nextInt()
        checkpoints.add(Checkpoint(Point(checkpointX, checkpointY), i))
    }

    System.err.println("Race initialized: $laps laps, $checkpointCount checkpoints")

    // Initialize pods with default values
    val myPods = mutableListOf<MyPod>(
        MyPod(Point(0, 0), Pair(0, 0), 0, 0, true, 0, PodRole.RACER),
        MyPod(Point(0, 0), Pair(0, 0), 0, 0, true, 0, PodRole.BLOCKER)
    )

    val opponentPods = mutableListOf<OpponentPod>(
        OpponentPod(Point(0, 0), Pair(0, 0), 0, 0, 0, 0),
        OpponentPod(Point(0, 0), Pair(0, 0), 0, 0, 0, 0)
    )

    // Shared boost between pods
    var boostAvailable = true
    var turn = 0

    // game loop
    while (true) {
        turn++

        // Update my pods
        for (i in 0 until 2) {
            val podPositionX = input.nextInt()
            val podPositionY = input.nextInt()
            val podVelocityX = input.nextInt()
            val podVelocityY = input.nextInt()
            val podAngle = input.nextInt()
            val podNextCheckpointId = input.nextInt()

            myPods[i].update(podPositionX, podPositionY, podVelocityX, podVelocityY, podAngle, podNextCheckpointId)
        }

        // Update opponent pods
        for (i in 0 until 2) {
            val opponentPositionX = input.nextInt()
            val opponentPositionY = input.nextInt()
            val opponentVelocityX = input.nextInt()
            val opponentVelocityY = input.nextInt()
            val opponentAngle = input.nextInt()
            val opponentNextCheckpointId = input.nextInt()

            opponentPods[i].update(opponentPositionX, opponentPositionY, opponentVelocityX, opponentVelocityY, opponentAngle, opponentNextCheckpointId)
        }

        // Find the opponent pod with highest progress (considering laps and checkpoint ID)
        val leadingOpponentIndex = if (opponentPods[0].laps > opponentPods[1].laps || 
                                     (opponentPods[0].laps == opponentPods[1].laps && 
                                      opponentPods[0].nextCheckpointId > opponentPods[1].nextCheckpointId)) 0 else 1

        val leadingOpponent = opponentPods[leadingOpponentIndex]
        val opponentProgress = "Lap: ${leadingOpponent.laps}, CP: ${leadingOpponent.nextCheckpointId}"
        System.err.println("Targeting opponent $leadingOpponentIndex: CP $opponentProgress")

        // Strategy for first pod (Racer)
        val racerPod = myPods[0]

        // Calculate target and thrust for racer pod
        racerPod.calculateTarget(checkpoints, checkpointCount)
        racerPod.calculateThrust(checkpoints, turn, opponentPods, boostAvailable)

        // If boost is used, update shared boost availability
        if (racerPod.useBoost) {
            boostAvailable = false
        }

        // Strategy for second pod (Blocker/Interceptor)
        val blockerPod = myPods[1]

        // Always block the opponent with highest progress
        val blockOpponent = true

        // Calculate target and thrust for blocker pod
        blockerPod.calculateBlockerTarget(leadingOpponent, checkpoints, blockOpponent)

        // Output commands for both pods
        println(racerPod.getCommand())
        println(blockerPod.getCommand())

        // Update shield cooldown if shield was used
        racerPod.activateShield()
        blockerPod.activateShield()
    }
}
