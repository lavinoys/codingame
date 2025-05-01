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

        // Squared distance for comparison purposes (optimization)
        fun squaredDistanceBetween(x1: Int, y1: Int, x2: Int, y2: Int): Double =
            (x1 - x2).toDouble().pow(2) + (y1 - y2).toDouble().pow(2)

        fun angleBetween(x1: Int, y1: Int, x2: Int, y2: Int): Double =
            atan2((y2 - y1).toDouble(), (x2 - x1).toDouble()) * 180 / PI

        // Calculate the closest point on a line (defined by points a and b) to point p
        fun closestPointToLine(a: Point, b: Point, p: Point): Point {
            val aToP = Point(p.x - a.x, p.y - a.y)
            val aToB = Point(b.x - a.x, b.y - a.y)

            val atb2 = distanceBetween(a.x, a.y, b.x, b.y)
            val atb2Squared = atb2 * atb2

            if (atb2Squared < 0.0001) {
                return p
            }

            val atpDotAtb = aToP.x * aToB.x + aToP.y * aToB.y
            val t = atpDotAtb / atb2Squared

            return Point(
                (a.x + aToB.x * t).toInt(),
                (a.y + aToB.y * t).toInt()
            )
        }

        // Check if a point is inside a circle with center (cx, cy) and radius r
        fun isInsideCircle(x: Int, y: Int, cx: Int, cy: Int, r: Double): Boolean {
            return squaredDistanceBetween(x, y, cx, cy) <= r * r
        }
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

    // Constants
    companion object {
        const val FRICTION = 0.85
        const val POD_SIZE = 400.0
        const val CHECKPOINT_RADIUS = 600.0
    }

    // Squared distance for comparison purposes (optimization)
    fun squaredDistanceToCheckpoint(checkpoint: Checkpoint): Double = 
        Point.squaredDistanceBetween(posX, posY, checkpoint.position.x, checkpoint.position.y)

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

    // Predict if the pod will enter a checkpoint in the next few turns
    fun isGoingToEnterCheckpointSoon(checkpoints: List<Checkpoint>): Boolean {
        val currentCheckpoint = checkpoints[nextCheckpointId]
        var velocity = Pair(this.velocity.first, this.velocity.second)
        var approxPosition = Point(posX, posY)

        // Simulate movement for the next 6 turns
        (0 until 6).forEach { _ ->
            // Apply friction to velocity
            velocity = Pair(
                (velocity.first * FRICTION).toInt(),
                (velocity.second * FRICTION).toInt()
            )

            // Update position based on velocity
            approxPosition = Point(
                approxPosition.x + velocity.first,
                approxPosition.y + velocity.second
            )

            // Check if pod is inside checkpoint
            if (Point.isInsideCircle(
                    approxPosition.x,
                    approxPosition.y,
                    currentCheckpoint.position.x,
                    currentCheckpoint.position.y,
                    CHECKPOINT_RADIUS
                )) {
                return true
            }
        }

        return false
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
    val collisionRadius = POD_SIZE.toInt() // Radius to consider for collision
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

    // Predict collision with any pod
    protected fun predictCollision(otherPod: BasePod): Pair<Double, Double> {
        // Calculate relative position and velocity
        val relativeX = otherPod.posX - posX
        val relativeY = otherPod.posY - posY
        val relativeVX = otherPod.velocity.first - velocity.first
        val relativeVY = otherPod.velocity.second - velocity.second

        // Calculate relative speed
        val relativeSpeed = getRelativeSpeed(otherPod)

        // Make collision radius dynamic based on relative speed
        // Higher speeds need larger collision radius to account for movement between turns
        val dynamicCollisionRadius = collisionRadius + (relativeSpeed * 0.5).toInt().coerceAtMost(POD_SIZE.toInt())

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
                val squaredDistance = Point.squaredDistanceBetween(posX, posY, opponentPod.posX, opponentPod.posY)
                val distance = sqrt(squaredDistance)  // Only calculate actual distance for logging
                val relativeSpeed = getRelativeSpeed(opponentPod)
                val dynamicRadius = collisionRadius + (relativeSpeed * 0.5).toInt().coerceAtMost(POD_SIZE.toInt())
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

    // Methods that can be overridden by subclasses
    open fun calculateTarget(checkpoints: List<Checkpoint>, checkpointCount: Int) {
        // Default implementation - does nothing
        // Subclasses should override this method if they need to calculate targets
        System.err.println("MyPod: Default calculateTarget implementation")
    }

    open fun calculateThrust(checkpoints: List<Checkpoint>, turn: Int, opponentPods: List<OpponentPod>, sharedBoostAvailable: Boolean) {
        // Default implementation - does nothing
        // Subclasses should override this method if they need to calculate thrust
        System.err.println("MyPod: Default calculateThrust implementation")
    }

    // Calculate a target point ahead of the checkpoint based on inertia
    protected fun calculateInertiaAdjustedTarget(checkpoint: Checkpoint): Pair<Int, Int> {
        // Get the current speed and velocity
        val currentSpeed = getCurrentSpeed()

        // Only apply inertia correction if we're moving fast enough
        if (currentSpeed < 100) {
            return Pair(checkpoint.position.x, checkpoint.position.y)
        }

        // Calculate how far ahead to aim based on speed
        // Higher speeds need to aim further ahead
        val lookAheadFactor = (currentSpeed / 100.0).coerceAtMost(3.0)

        // Calculate the direction vector of our velocity
        val velocityMagnitude = sqrt(velocity.first.toDouble().pow(2) + velocity.second.toDouble().pow(2))
        val velocityDirX = if (velocityMagnitude > 0) velocity.first / velocityMagnitude else 0.0
        val velocityDirY = if (velocityMagnitude > 0) velocity.second / velocityMagnitude else 0.0

        // Calculate the target point ahead of the checkpoint
        val targetX = (checkpoint.position.x + velocityDirX * lookAheadFactor * CHECKPOINT_RADIUS).toInt()
        val targetY = (checkpoint.position.y + velocityDirY * lookAheadFactor * CHECKPOINT_RADIUS).toInt()

        return Pair(targetX, targetY)
    }

    // Adjust thrust based on inertia to compensate for pod's momentum
    protected fun adjustThrustForInertia(baseThrust: Int, targetCheckpoint: Checkpoint): Int {
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
            shieldActive = 3  // Shield will be active for 3 turns
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

    // Track checkpoint count for home run detection
    private var checkpointCount = 0
    private var lastCheckpointId = 0

    override fun update(x: Int, y: Int, vx: Int, vy: Int, angle: Int, nextCheckpointId: Int) {
        // Track checkpoint changes to count laps
        if (this.nextCheckpointId != nextCheckpointId) {
            checkpointCount++
            lastCheckpointId = this.nextCheckpointId
        }

        super.update(x, y, vx, vy, angle, nextCheckpointId)
    }

    override fun calculateTarget(checkpoints: List<Checkpoint>, checkpointCount: Int) {
        val currentCheckpoint = checkpoints[nextCheckpointId]
        val nextCheckpointIndex = (nextCheckpointId + 1) % checkpointCount
        val nextCheckpoint = checkpoints[nextCheckpointIndex]

        // Get checkpoint coordinates directly
        val currentX = currentCheckpoint.position.x
        val currentY = currentCheckpoint.position.y

        // Check if we're going for the home run (final lap, final checkpoint)
        val isGoingForHomeRun = this.checkpointCount == (checkpointCount * 3)

        if (isGoingForHomeRun) {
            System.err.println("RacerPod: Going for the home run!")
            targetX = currentX
            targetY = currentY
            return
        }

        // If we're about to enter the checkpoint, aim for the next one
        if (isGoingToEnterCheckpointSoon(checkpoints)) {
            val nextCheckpointX = nextCheckpoint.position.x
            val nextCheckpointY = nextCheckpoint.position.y

            targetX = nextCheckpointX
            targetY = nextCheckpointY

            val angleDiff = Math.abs(angleToCheckpoint(nextCheckpoint))
            System.err.println("RacerPod: Drifting towards next checkpoint, angle diff: $angleDiff")
            return
        }

        // Calculate position delta and future position
        val positionDelta = Pair(
            (velocity.first * FRICTION).toInt(),
            (velocity.second * FRICTION).toInt()
        )
        val distanceTravelledOnPreviousFrame = sqrt(
            positionDelta.first.toDouble().pow(2) + 
            positionDelta.second.toDouble().pow(2)
        )
        val futurePos = Point(posX + positionDelta.first, posY + positionDelta.second)

        // Optimize trajectory using closestPointToLine if we're moving fast enough
        val angleDiff = Math.abs(angleToCheckpoint(currentCheckpoint))
        val futureDistanceSquared = Point.squaredDistanceBetween(
            futurePos.x, futurePos.y, 
            currentCheckpoint.position.x, currentCheckpoint.position.y
        )
        val currentDistanceSquared = Point.squaredDistanceBetween(
            posX, posY, 
            currentCheckpoint.position.x, currentCheckpoint.position.y
        )

        if (distanceTravelledOnPreviousFrame > 50 && 
            angleDiff < 70 && 
            futureDistanceSquared < currentDistanceSquared) {

            // Use closestPointToLine to optimize trajectory
            val currentPos = Point(posX, posY)
            val aux = Point.closestPointToLine(currentPos, currentCheckpoint.position, futurePos)
            val aux2 = Point(
                aux.x + (aux.x - futurePos.x),
                aux.y + (aux.y - futurePos.y)
            )

            targetX = aux2.x
            targetY = aux2.y
            System.err.println("RacerPod: Optimizing trajectory using closestPointToLine")
            return
        }

        // Default behavior - use inertia correction to aim ahead of the checkpoint
        val squaredDistance = squaredDistanceToCheckpoint(currentCheckpoint)
        if (squaredDistance < 1440000) { // 1200^2 = 1440000
            // If close to checkpoint, aim for the next one with inertia correction
            val nextCheckpointX = nextCheckpoint.position.x
            val nextCheckpointY = nextCheckpoint.position.y

            // Calculate vector directly with some bias towards the next checkpoint
            targetX = (currentX + (nextCheckpointX - currentX) * 0.3).toInt()
            targetY = (currentY + (nextCheckpointY - currentY) * 0.3).toInt()
        } else {
            // Apply inertia correction - aim ahead of the checkpoint based on our velocity
            val (adjustedX, adjustedY) = calculateInertiaAdjustedTarget(currentCheckpoint)
            targetX = adjustedX
            targetY = adjustedY
            System.err.println("RacerPod: Using inertia correction, aiming at ($targetX, $targetY) instead of (${currentCheckpoint.position.x}, ${currentCheckpoint.position.y})")
        }
    }

    // Detect if we're approaching a hairpin turn
    private fun isHairpinTurn(checkpoints: List<Checkpoint>, checkpointCount: Int): Boolean {
        val currentCheckpoint = checkpoints[nextCheckpointId]
        val nextCheckpointIndex = (nextCheckpointId + 1) % checkpointCount
        val nextCheckpoint = checkpoints[nextCheckpointIndex]

        // Calculate the angle between current checkpoint and next checkpoint
        val vectorToCurrent = Point(
            currentCheckpoint.position.x - posX,
            currentCheckpoint.position.y - posY
        )
        val vectorToNext = Point(
            nextCheckpoint.position.x - currentCheckpoint.position.x,
            nextCheckpoint.position.y - currentCheckpoint.position.y
        )

        // Calculate the angle between these vectors
        val dotProduct = vectorToCurrent.x * vectorToNext.x + vectorToCurrent.y * vectorToNext.y
        val magnitudeCurrent = sqrt(vectorToCurrent.x.toDouble().pow(2) + vectorToCurrent.y.toDouble().pow(2))
        val magnitudeNext = sqrt(vectorToNext.x.toDouble().pow(2) + vectorToNext.y.toDouble().pow(2))

        // Avoid division by zero
        if (magnitudeCurrent < 0.001 || magnitudeNext < 0.001) {
            return false
        }

        val cosAngle = dotProduct / (magnitudeCurrent * magnitudeNext)
        // Clamp to avoid domain errors with acos
        val clampedCosAngle = cosAngle.coerceIn(-1.0, 1.0)
        val angleBetweenCheckpoints = Math.toDegrees(acos(clampedCosAngle))

        // If the angle is large (> 90 degrees) and we're close to the current checkpoint, it's a hairpin
        val squaredDistance = squaredDistanceToCheckpoint(currentCheckpoint)
        val isCloseToCheckpoint = squaredDistance < 4000000 // 2000^2 = 4000000
        val isLargeAngle = angleBetweenCheckpoints > 90

        if (isLargeAngle && isCloseToCheckpoint) {
            // For logging, get the actual distance
            val distance = sqrt(squaredDistance)
            System.err.println("Detected hairpin turn! Angle between checkpoints: $angleBetweenCheckpoints, Distance: $distance")
            return true
        }

        return false
    }

    override fun calculateThrust(checkpoints: List<Checkpoint>, turn: Int, opponentPods: List<OpponentPod>, sharedBoostAvailable: Boolean) {
        val currentCheckpoint = checkpoints[nextCheckpointId]
        val squaredDistance = squaredDistanceToCheckpoint(currentCheckpoint)
        val angleDiff = Math.abs(angleToCheckpoint(currentCheckpoint))

        // Reset flags
        if (shieldActive == 0) {  // Only reset if shield is not active
            useShield = false
        }
        useBoost = false

        // Check if we're in a hairpin turn
        val isHairpin = isHairpinTurn(checkpoints, checkpoints.size)

        // Cornering sequence for hairpin turns
        if (isHairpin) {
            // If we're in a sharp turn and the angle is large, turn off the engine
            if (angleDiff > 70) {
                thrust = 0
                System.err.println("Hairpin turn: Engine off, rotating only. Angle diff: $angleDiff")
            } 
            // Once we've rotated enough, gradually increase thrust
            else if (angleDiff > 40) {
                thrust = 30
                System.err.println("Hairpin turn: Minimal thrust during turn. Angle diff: $angleDiff")
            }
            // When angle is good enough, resume normal thrust
            else {
                thrust = 100
                System.err.println("Hairpin turn: Resuming thrust. Angle diff: $angleDiff")
            }
        } 
        // Normal thrust calculation for non-hairpin turns
        else {
            // Determine base thrust based on angle and distance
            val baseThrust = when {
                angleDiff > 90 -> 0
                angleDiff > 50 -> 50
                squaredDistance < 1000000 -> 70 // 1000^2 = 1000000
                else -> 100
            }

            // Adjust thrust based on inertia
            thrust = adjustThrustForInertia(baseThrust, currentCheckpoint)
        }

        // Decide whether to use boost - never use boost in hairpin turns
        if (sharedBoostAvailable && 
            angleDiff < 10 && 
            squaredDistance > 25000000 && // 5000^2 = 25000000
            turn > 3 &&
            !isHairpin) {
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

    // Track checkpoint count for home run detection
    private var checkpointCount = 0
    private var lastCheckpointId = 0

    override fun update(x: Int, y: Int, vx: Int, vy: Int, angle: Int, nextCheckpointId: Int) {
        // Track checkpoint changes to count laps
        if (this.nextCheckpointId != nextCheckpointId) {
            checkpointCount++
            lastCheckpointId = this.nextCheckpointId
        }

        super.update(x, y, vx, vy, angle, nextCheckpointId)
    }


    // BlockerPod uses the default implementations of calculateTarget and calculateThrust from MyPod
    // The actual blocking logic is implemented in calculateBlockerTarget

    fun calculateBlockerTarget(leadingOpponent: OpponentPod, racerPod: RacerPod) {
        // Reset flags
        if (shieldActive == 0) {  // Only reset if shield is not active
            useShield = false
        }
        useBoost = false

        // First rule: Check if we're going to interfere with our racer pod
        val (collisionTimeWithRacer, probabilityWithRacer) = predictCollision(racerPod)
        val distanceToRacerSquared = Point.squaredDistanceBetween(posX, posY, racerPod.posX, racerPod.posY)

        // If we're likely to collide with our racer pod, avoid it
        if (collisionTimeWithRacer < 3 && probabilityWithRacer > 0.5 && distanceToRacerSquared < 4000000) { // 2000^2 = 4000000
            System.err.println("BLOCKER: Avoiding collision with racer pod! Time=$collisionTimeWithRacer, Prob=$probabilityWithRacer")

            // Calculate a target that avoids the racer pod
            // Move perpendicular to the line connecting the two pods
            val dirX = racerPod.posX - posX
            val dirY = racerPod.posY - posY
            val length = sqrt(dirX.toDouble().pow(2) + dirY.toDouble().pow(2))

            if (length > 0) {
                // Calculate perpendicular vector (rotate 90 degrees)
                val perpX = -dirY / length
                val perpY = dirX / length

                // Set target away from racer pod
                targetX = posX + (perpX * 1000).toInt()
                targetY = posY + (perpY * 1000).toInt()
                thrust = 100
                return
            }
        }

        // Second rule: Normal blocking behavior - aim to intercept the leading opponent
        // Use inertia correction to predict where the opponent will be
        val opponentSpeed = leadingOpponent.getCurrentSpeed()
        val lookAheadFactor = (opponentSpeed / 100.0).coerceAtMost(2.0)

        targetX = leadingOpponent.posX + (leadingOpponent.velocity.first * lookAheadFactor).toInt()
        targetY = leadingOpponent.posY + (leadingOpponent.velocity.second * lookAheadFactor).toInt()
        thrust = 100

        // For blocker pod, check for imminent collision with the target opponent
        // and activate shield preemptively if we're on an intercept course
        val distanceToOpponentSquared = Point.squaredDistanceBetween(posX, posY, leadingOpponent.posX, leadingOpponent.posY)

        // If we're close to the opponent and shield is available
        if (shieldCooldown == 0 && distanceToOpponentSquared < 1440000) { // 1200^2 = 1440000
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

fun main() {
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

        // BlockerPod only focuses on blocking opponents, not checkpoint racing
        blockerPod.calculateBlockerTarget(leadingOpponent, racerPod)


        // Output commands for both pods
        println(racerPod.getCommand())
        println(blockerPod.getCommand())

        // Update shield cooldown if shield was used
        racerPod.activateShield()
        blockerPod.activateShield()
    }
}
