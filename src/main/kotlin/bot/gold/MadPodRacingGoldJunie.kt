import java.util.*
import kotlin.math.*

/**
 * Mad Pod Racing Gold League Implementation
 * Based on the rules and strategies from MadPodRacingGold.md
 */

// Data classes for game entities
data class Point(val x: Int, val y: Int) {
    companion object {
        // Static methods to avoid creating temporary Point objects
        fun distanceBetween(x1: Int, y1: Int, x2: Int, y2: Int): Double =
            sqrt((x1 - x2).toDouble().pow(2) + (y1 - y2).toDouble().pow(2))

        fun angleBetween(x1: Int, y1: Int, x2: Int, y2: Int): Double =
            atan2((y2 - y1).toDouble(), (x2 - x1).toDouble()) * 180 / PI
    }
}

data class Checkpoint(val position: Point, val id: Int)

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

    // Calculate current speed
    fun getCurrentSpeed(): Double = 
        sqrt(velocity.first.toDouble().pow(2) + velocity.second.toDouble().pow(2))

    // Calculate relative speed between this pod and another pod
    fun getRelativeSpeed(otherPod: BasePod): Double = 
        sqrt(
            Math.pow((velocity.first - otherPod.velocity.first).toDouble(), 2.0) +
            Math.pow((velocity.second - otherPod.velocity.second).toDouble(), 2.0)
        )

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

// Abstract base class for player-controlled pods
abstract class MyPod(
    position: Point,
    velocity: Pair<Int, Int>,
    angle: Int,
    nextCheckpointId: Int,
    var shieldCooldown: Int = 0
) : BasePod(position, velocity, angle, nextCheckpointId) {
    var targetX: Int = 0
    var targetY: Int = 0
    var thrust: Int = 100
    var useShield: Boolean = false
    var shieldActive: Int = 0  // Number of turns the shield is active
    var useBoost: Boolean = false

    // Collision prediction parameters
    val collisionRadius = 400 // Radius to consider for collision (pod radius is 400)
    val collisionTimeThreshold = 3 // Number of turns to look ahead for collision prediction
    val collisionProbabilityThreshold = 0.7 // Probability threshold to activate shield

    override fun update(x: Int, y: Int, vx: Int, vy: Int, angle: Int, nextCheckpointId: Int) {
        super.update(x, y, vx, vy, angle, nextCheckpointId)

        // Update shield cooldown
        if (shieldCooldown > 0) {
            shieldCooldown--
        }

        // Update shield active duration
        if (shieldActive > 0) {
            shieldActive--
            useShield = true  // Keep shield active
        }
    }

    // Predict collision with opponent pods
    protected fun predictCollision(opponentPod: OpponentPod): Pair<Double, Double> {
        // Calculate relative position and velocity
        val relativeX = opponentPod.posX - posX
        val relativeY = opponentPod.posY - posY
        val relativeVX = opponentPod.velocity.first - velocity.first
        val relativeVY = opponentPod.velocity.second - velocity.second

        // Calculate relative speed
        val relativeSpeed = getRelativeSpeed(opponentPod)

        // Make collision radius dynamic based on relative speed
        // Higher speeds need larger collision radius to account for movement between turns
        val dynamicCollisionRadius = collisionRadius + (relativeSpeed * 0.5).toInt().coerceAtMost(400)

        // Calculate quadratic equation coefficients for collision time
        // ||p + vt|| = r, where p is relative position, v is relative velocity, r is collision radius
        val a = relativeVX.toDouble() * relativeVX.toDouble() + relativeVY.toDouble() * relativeVY.toDouble()
        val b = 2.0 * (relativeX.toDouble() * relativeVX.toDouble() + relativeY.toDouble() * relativeVY.toDouble())
        val c = relativeX.toDouble() * relativeX.toDouble() + relativeY.toDouble() * relativeY.toDouble() - dynamicCollisionRadius.toDouble() * dynamicCollisionRadius.toDouble()

        // If pods are not moving relative to each other
        if (a < 0.0001) {
            // If already colliding
            if (c <= 0) {
                return Pair(0.0, 1.0) // Immediate collision with 100% probability
            }
            return Pair(Double.MAX_VALUE, 0.0) // No collision
        }

        // Calculate discriminant
        val discriminant = b * b - 4 * a * c

        // No real solutions, no collision
        if (discriminant < 0) {
            return Pair(Double.MAX_VALUE, 0.0)
        }

        // Calculate collision times
        val t1 = (-b - sqrt(discriminant)) / (2 * a)
        val t2 = (-b + sqrt(discriminant)) / (2 * a)

        // Find the earliest positive collision time
        val collisionTime = when {
            t1 > 0 -> t1
            t2 > 0 -> t2
            else -> Double.MAX_VALUE // No future collision
        }

        // Calculate collision probability based on time
        val probability = if (collisionTime < Double.MAX_VALUE) {
            // Higher probability for closer collisions, lower for farther ones
            (1.0 - (collisionTime / collisionTimeThreshold).coerceAtMost(1.0))
        } else {
            0.0
        }

        return Pair(collisionTime, probability)
    }

    // Check if we should activate shield based on collision prediction
    protected fun shouldActivateShield(opponentPods: List<OpponentPod>): Boolean {
        // If shield is on cooldown, we can't use it
        if (shieldCooldown > 0) {
            return false
        }

        // Check collision with each opponent pod
        for (opponentPod in opponentPods) {
            val (collisionTime, probability) = predictCollision(opponentPod)

            // Log collision prediction data for debugging
            if (collisionTime < 5) {  // Only log if collision is within 5 turns
                val distance = Point.distanceBetween(posX, posY, opponentPod.posX, opponentPod.posY)
                val relativeSpeed = getRelativeSpeed(opponentPod)
                val dynamicRadius = collisionRadius + (relativeSpeed * 0.5).toInt().coerceAtMost(400)
                System.err.println("Collision prediction: Time=$collisionTime, Prob=$probability, Dist=$distance, RelSpeed=$relativeSpeed, Radius=$dynamicRadius")
            }

            // If collision is imminent and probable
            if (collisionTime < collisionTimeThreshold && probability > collisionProbabilityThreshold) {
                // Get relative speed to determine impact force
                val relativeSpeed = getRelativeSpeed(opponentPod)

                // Only activate shield for high-impact collisions
                if (relativeSpeed > 300) {
                    System.err.println("Shield activated! Collision predicted in $collisionTime turns with probability $probability, speed: $relativeSpeed")
                    return true
                }
            }
        }

        return false
    }

    // Abstract methods to be implemented by subclasses
    abstract fun calculateTarget(checkpoints: List<Checkpoint>, checkpointCount: Int)
    abstract fun calculateThrust(checkpoints: List<Checkpoint>, turn: Int, opponentPods: List<OpponentPod>, sharedBoostAvailable: Boolean)

    // Adjust thrust based on inertia to compensate for pod's momentum
    protected fun adjustThrustForInertia(baseThrust: Int, angleDiff: Double, targetCheckpoint: Checkpoint): Int {
        // Get the angle to the checkpoint in radians
        val targetAngle = Point.angleBetween(posX, posY, targetCheckpoint.position.x, targetCheckpoint.position.y) * PI / 180.0

        // Get the current speed
        val currentSpeed = getCurrentSpeed()

        // Calculate the dot product to determine if we're moving in the right direction
        val dotProduct = velocity.first * cos(targetAngle) + velocity.second * sin(targetAngle)
        val movingTowardsTarget = dotProduct > 0

        // Adjust thrust based on inertia and current movement
        return when {
            // If we're moving fast and in the wrong direction, reduce thrust to allow for turning
            currentSpeed > 200 && !movingTowardsTarget -> (baseThrust * 0.7).toInt()

            // If we're moving fast and in the right direction, maintain thrust
            currentSpeed > 200 && movingTowardsTarget -> baseThrust

            // If we're moving slowly, increase thrust to overcome inertia
            currentSpeed < 100 -> (baseThrust * 1.2).coerceAtMost(100.0).toInt()

            // Default case
            else -> baseThrust
        }
    }

    // Log information for debugging
    protected fun logInertiaInfo(podType: String) {
        val speed = getCurrentSpeed()

        System.err.println("Pod $podType - " +
                "Speed: $speed, " +
                "Velocity: (${velocity.first}, ${velocity.second}), " +
                "Thrust: $thrust")
    }

    fun getCommand(): String {
        return when {
            useShield -> "$targetX $targetY SHIELD SHIELD"
            useBoost -> "$targetX $targetY BOOST BOOST"
            else -> "$targetX $targetY $thrust Thrust:$thrust"
        }
    }

    fun activateShield() {
        if (useShield && shieldActive == 0) {  // Only activate if not already active
            shieldActive = 2  // Shield will be active for 2 turns
            shieldCooldown = 3  // Cooldown after shield expires
        }
    }
}

// RacerPod class for racing strategy
class RacerPod(
    position: Point,
    velocity: Pair<Int, Int>,
    angle: Int,
    nextCheckpointId: Int,
    shieldCooldown: Int = 0
) : MyPod(position, velocity, angle, nextCheckpointId, shieldCooldown) {

    override fun calculateTarget(checkpoints: List<Checkpoint>, checkpointCount: Int) {
        val currentCheckpoint = checkpoints[nextCheckpointId]
        val nextCheckpointId = (nextCheckpointId + 1) % checkpointCount
        val nextCheckpoint = checkpoints[nextCheckpointId]
        val distance = distanceToCheckpoint(currentCheckpoint)

        // Get checkpoint coordinates directly
        val currentX = currentCheckpoint.position.x
        val currentY = currentCheckpoint.position.y

        if (distance < 1200) {
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

    override fun calculateThrust(checkpoints: List<Checkpoint>, turn: Int, opponentPods: List<OpponentPod>, sharedBoostAvailable: Boolean) {
        val currentCheckpoint = checkpoints[nextCheckpointId]
        val distance = distanceToCheckpoint(currentCheckpoint)
        val angleDiff = Math.abs(angleToCheckpoint(currentCheckpoint))

        // Reset flags
        if (shieldActive == 0) {  // Only reset if shield is not active
            useShield = false
        }
        useBoost = false

        // Determine base thrust based on angle and distance
        val baseThrust = when {
            angleDiff > 90 -> 0
            angleDiff > 50 -> 50
            distance < 1000 -> 70
            else -> 100
        }

        // Adjust thrust based on inertia
        thrust = adjustThrustForInertia(baseThrust, angleDiff, currentCheckpoint)

        // Decide whether to use boost
        if (sharedBoostAvailable && 
            angleDiff < 10 && 
            distance > 5000 &&
            turn > 3) {
            useBoost = true
        }

        // Decide whether to use shield - use collision prediction
        useShield = shouldActivateShield(opponentPods)

        // Log inertia information for debugging
        logInertiaInfo("RACER")
    }
}

// BlockerPod class for blocking/intercepting strategy
class BlockerPod(
    position: Point,
    velocity: Pair<Int, Int>,
    angle: Int,
    nextCheckpointId: Int,
    shieldCooldown: Int = 0
) : MyPod(position, velocity, angle, nextCheckpointId, shieldCooldown) {

    override fun calculateTarget(checkpoints: List<Checkpoint>, checkpointCount: Int) {
        // For blocker, just aim at the current checkpoint
        val currentCheckpoint = checkpoints[nextCheckpointId]
        targetX = currentCheckpoint.position.x
        targetY = currentCheckpoint.position.y
    }

    override fun calculateThrust(checkpoints: List<Checkpoint>, turn: Int, opponentPods: List<OpponentPod>, sharedBoostAvailable: Boolean) {
        // This method is required by the abstract class but not used
        // The actual implementation is in calculateBlockerTarget
    }

    fun calculateBlockerTarget(leadingOpponent: OpponentPod, checkpoints: List<Checkpoint>) {
        // Reset flags
        if (shieldActive == 0) {  // Only reset if shield is not active
            useShield = false
        }
        useBoost = false

        // Aim to intercept the leading opponent - use direct coordinates
        targetX = leadingOpponent.posX + leadingOpponent.velocity.first
        targetY = leadingOpponent.posY + leadingOpponent.velocity.second
        thrust = 100

        // For blocker pod, check for imminent collision with the target opponent
        // and activate shield preemptively if we're on an intercept course
        val distanceToOpponent = Point.distanceBetween(posX, posY, leadingOpponent.posX, leadingOpponent.posY)

        // If we're close to the opponent and shield is available
        if (shieldCooldown == 0 && distanceToOpponent < 1200) {
            // Calculate time to collision more precisely for blocking
            val (collisionTime, probability) = predictCollision(leadingOpponent)

            // If collision is very likely and we're moving fast toward the opponent
            if (collisionTime < 2 && probability > 0.5) {
                val relativeSpeed = getRelativeSpeed(leadingOpponent)

                // Be more aggressive with shield usage when actively blocking
                if (relativeSpeed > 200) {
                    useShield = true
                    System.err.println("BLOCKER: Shield activated for interception! Collision in $collisionTime turns, speed: $relativeSpeed")
                }
            }
        }

        // Log information for debugging
        logInertiaInfo("BLOCKER")
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
        RacerPod(initialPoint, initialVelocity, 0, 0, 0),
        BlockerPod(initialPoint, initialVelocity, 0, 0, 0)
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
        val racerPod = myPods[0] as RacerPod


        // Calculate target and thrust for racer pod
        racerPod.calculateTarget(checkpoints, checkpointCount)
        racerPod.calculateThrust(checkpoints, turn, opponentPods, boostAvailable)

        // If boost is used, update shared boost availability
        if (racerPod.useBoost) {
            boostAvailable = false
        }

        // Strategy for second pod (Blocker/Interceptor)
        val blockerPod = myPods[1] as BlockerPod


        // Calculate target and thrust for blocker pod
        blockerPod.calculateBlockerTarget(leadingOpponent, checkpoints)


        // Output commands for both pods
        println(racerPod.getCommand())
        println(blockerPod.getCommand())

        // Update shield cooldown if shield was used
        racerPod.activateShield()
        blockerPod.activateShield()
    }
}
