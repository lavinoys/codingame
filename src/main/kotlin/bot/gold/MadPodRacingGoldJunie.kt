import java.util.*
import kotlin.math.*

/**
 * Mad Pod Racing Gold League Implementation
 * Based on the rules and strategies from MadPodRacingGold.md
 */

// Data classes for game entities
data class Point(val x: Int, val y: Int) {
    fun distance(other: Point): Double = 
        distanceBetween(x, y, other.x, other.y)

    fun angle(other: Point): Double = 
        angleBetween(x, y, other.x, other.y)

    companion object {
        // Static methods to avoid creating temporary Point objects
        fun distanceBetween(x1: Int, y1: Int, x2: Int, y2: Int): Double =
            sqrt((x1 - x2).toDouble().pow(2) + (y1 - y2).toDouble().pow(2))

        fun angleBetween(x1: Int, y1: Int, x2: Int, y2: Int): Double =
            atan2((y2 - y1).toDouble(), (x2 - x1).toDouble()) * 180 / PI
    }
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
    // Store position coordinates directly to avoid creating Point objects
    var posX: Int = position.x
    var posY: Int = position.y

    fun distanceToCheckpoint(checkpoint: Checkpoint): Double = 
        Point.distanceBetween(posX, posY, checkpoint.position.x, checkpoint.position.y)

    fun angleToCheckpoint(checkpoint: Checkpoint): Double {
        val targetAngle = Point.angleBetween(posX, posY, checkpoint.position.x, checkpoint.position.y)
        val angleDiff = (targetAngle - angle + 360) % 360
        return if (angleDiff > 180) angleDiff - 360 else angleDiff
    }

    open fun update(x: Int, y: Int, vx: Int, vy: Int, angle: Int, nextCheckpointId: Int) {
        // Update position coordinates directly
        posX = x
        posY = y
        // Update Point object only once
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

        // Get checkpoint coordinates directly
        val currentX = currentCheckpoint.position.x
        val currentY = currentCheckpoint.position.y

        if (distance < 1200 && role == PodRole.RACER) {
            // If close to checkpoint, aim for the next one - calculate directly without creating temporary objects
            val nextCheckpointX = nextCheckpoint.position.x
            val nextCheckpointY = nextCheckpoint.position.y

            // Calculate vector directly
            targetX = (currentX + (nextCheckpointX - currentX) * 0.3).toInt()
            targetY = (currentY + (nextCheckpointY - currentY) * 0.3).toInt()
        } else {
            targetX = currentX
            targetY = currentY
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
                // Use static method to avoid creating temporary objects
                Point.distanceBetween(posX, posY, it.posX, it.posY) < 850 && 
                Math.abs(velocity.first - it.velocity.first) + Math.abs(velocity.second - it.velocity.second) > 300 
            }) {
            useShield = true
        }
    }

    fun calculateBlockerTarget(leadingOpponent: OpponentPod, checkpoints: List<Checkpoint>, blockOpponent: Boolean) {
        if (blockOpponent) {
            // Aim to intercept the leading opponent - use direct coordinates
            targetX = leadingOpponent.posX + leadingOpponent.velocity.first
            targetY = leadingOpponent.posY + leadingOpponent.velocity.second
            thrust = 100
        } else {
            // Race normally - use direct coordinates
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

    // Store checkpoints - create Point objects only once during initialization
    val checkpoints = mutableListOf<Checkpoint>()
    for (i in 0 until checkpointCount) {
        val checkpointX = input.nextInt()
        val checkpointY = input.nextInt()
        // Point objects for checkpoints are created only once at the beginning
        checkpoints.add(Checkpoint(Point(checkpointX, checkpointY), i))
    }

    System.err.println("Race initialized: $laps laps, $checkpointCount checkpoints")

    // Initialize pods with default values - create Point objects only once
    // These Point objects will be reused and updated in-place
    val initialPoint = Point(0, 0)
    val initialVelocity = Pair(0, 0)

    val myPods = mutableListOf<MyPod>(
        MyPod(initialPoint, initialVelocity, 0, 0, true, 0, PodRole.RACER),
        MyPod(initialPoint, initialVelocity, 0, 0, true, 0, PodRole.BLOCKER)
    )

    val opponentPods = mutableListOf<OpponentPod>(
        OpponentPod(initialPoint, initialVelocity, 0, 0, 0, 0),
        OpponentPod(initialPoint, initialVelocity, 0, 0, 0, 0)
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
