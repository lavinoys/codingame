//Gold League 3rd place, score of 40.78
import kotlin.math.*
import kotlin.collections.*

const val PI_FLOAT = 3.14159265359f

data class Vec2(var x: Float = 0f, var y: Float = 0f) {
    fun distanceTo(p: Vec2): Float {
        return sqrt((x - p.x).pow(2) + (y - p.y).pow(2))
    }

    operator fun plus(o: Vec2): Vec2 {
        return Vec2(x + o.x, y + o.y)
    }

    operator fun minus(o: Vec2): Vec2 {
        return Vec2(x - o.x, y - o.y)
    }

    operator fun times(a: Float): Vec2 {
        return Vec2(x * a, y * a)
    }

    fun getMagnitude(): Float {
        return hypot(x, y)
    }

    fun getAngleInDegrees(): Float {
        val radians = atan2(y, x)
        return if (radians >= 0) {
            180 * radians / PI_FLOAT
        } else {
            180 * (radians + PI_FLOAT) / PI_FLOAT + 180
        }
    }

    fun isInsideCircle(cx: Float, cy: Float, r: Float): Boolean {
        return hypot(cx - x, cy - y) <= r
    }
}

data class PodState(
    var position: Vec2 = Vec2(),
    var velocity: Vec2 = Vec2(),
    var degrees: Int = 0,
    var nextCheckPointId: Int = 0
)

fun closestPointToLine(a: Vec2, b: Vec2, p: Vec2): Vec2 {
    val a_to_p = p - a
    val a_to_b = b - a
    var atb2 = a.distanceTo(b)
    atb2 *= atb2
    if (atb2 == 0f) {
        return p
    }
    val atp_dot_atb = a_to_p.x * a_to_b.x + a_to_p.y * a_to_b.y
    val t = atp_dot_atb / atb2
    return Vec2(a.x + a_to_b.x * t, a.y + a_to_b.y * t)
}

const val FRICTION = 0.85f
const val POD_SIZE = 400f
const val CHECKPOINT_RADIUS = 600f

class Pod(
    private val raceCoordinates: List<Vec2>?,
    private val tag: String,
    private val doBoostOnFirstFrame: Boolean = false,
    private val doShieldToBoostAlly: Boolean = false
) {
    private var currentState = PodState()
    private var isBoostAvailable = true
    private var numberOfFramesToWaitBeforeBoost = 30
    private var numberOfTurnsSinceLastShield = -1
    private var longestStretchCandidate = 0f
    private var checkpointCount = 0
    private var lastCheckpointId = 0

    init {
        raceCoordinates?.let { checkpoints ->
            longestStretchCandidate = 0f
            val n = checkpoints.size
            for (i in 0 until n) {
                val aux = checkpoints[i].distanceTo(checkpoints[(i + 1) % n])
                if (aux > longestStretchCandidate) {
                    longestStretchCandidate = aux
                }
            }
            longestStretchCandidate *= 0.70f
        }
    }

    fun readNewState(stateInput: List<Any>) {
        currentState = PodState(
            position = stateInput[0] as Vec2,
            velocity = stateInput[1] as Vec2,
            degrees = stateInput[2] as Int,
            nextCheckPointId = stateInput[3] as Int
        )
        if (lastCheckpointId != currentState.nextCheckPointId) {
            checkpointCount++
            lastCheckpointId = currentState.nextCheckPointId
        }
    }

    fun writeCommand(): String {
        if (numberOfTurnsSinceLastShield != -1) {
            numberOfTurnsSinceLastShield++
        }
        raceCoordinates?.let { checkpoints ->
            val absoluteDegreesDiffToCurrentCheckpoint = getAbsoluteDegreesTowardsCurrentCheckpoint()
            val currentCheckpoint = getCurrentCheckpointPosition()
            var outputPos = currentCheckpoint
            val currentPos = currentState.position
            val positionDelta = currentState.velocity * FRICTION
            val futurePos = currentPos + positionDelta

            if (!isGoingForHomeRun() && isGoingToEnterCheckpointSoon()) {
                outputPos = getNextCheckpointPosition()
            } else if (futurePos.distanceTo(currentCheckpoint) < currentPos.distanceTo(currentCheckpoint)) {
                val aux = closestPointToLine(currentPos, currentCheckpoint, futurePos)
                outputPos = aux + (aux - futurePos)
            }

            val thrust = if (absoluteDegreesDiffToCurrentCheckpoint < 90) {
                (100f * cos(absoluteDegreesDiffToCurrentCheckpoint / 180f)).toInt()
            } else 0

            if (isBoostAvailable && shouldBoost(absoluteDegreesDiffToCurrentCheckpoint)) {
                isBoostAvailable = false
                return "${outputPos.x.toInt()} ${outputPos.y.toInt()} BOOST"
            }

            if (shouldActivateShield()) {
                numberOfTurnsSinceLastShield = 0
                return "${outputPos.x.toInt()} ${outputPos.y.toInt()} SHIELD"
            }

            return "${outputPos.x.toInt()} ${outputPos.y.toInt()} $thrust"
        }

        return "0 0 100"
    }

    private fun isGoingForHomeRun(): Boolean {
        return checkpointCount == raceCoordinates!!.size * 3
    }

    private fun getCurrentCheckpointPosition(): Vec2 {
        return raceCoordinates!![currentState.nextCheckPointId]
    }

    private fun getNextCheckpointPosition(): Vec2 {
        return raceCoordinates!![(currentState.nextCheckPointId + 1) % raceCoordinates.size]
    }

    private fun getAbsoluteDegreesTowardsCurrentCheckpoint(): Float {
        return absoluteDegreesDiff(
            currentState.degrees.toFloat(),
            getVectorToCurrentCheckpoint().getAngleInDegrees()
        )
    }

    private fun getVectorToCurrentCheckpoint(): Vec2 {
        return getCurrentCheckpointPosition() - currentState.position
    }

    private fun absoluteDegreesDiff(a: Float, b: Float): Float {
        var d = b - a
        if (d < 0) d += 360
        return if (d > 180) 360 - d else d
    }

    private fun isGoingToEnterCheckpointSoon(): Boolean {
        val currentCheckpoint = getCurrentCheckpointPosition()
        var velocity = currentState.velocity
        var approxPosition = currentState.position
        repeat(6) {
            velocity *= FRICTION
            approxPosition += velocity
            if (approxPosition.isInsideCircle(currentCheckpoint.x, currentCheckpoint.y, CHECKPOINT_RADIUS)) {
                return true
            }
        }
        return false
    }

    private fun shouldBoost(absoluteDegrees: Float): Boolean {
        return doBoostOnFirstFrame || (numberOfFramesToWaitBeforeBoost < 0 && absoluteDegrees < 5f)
    }

    private fun shouldActivateShield(): Boolean {
        return numberOfTurnsSinceLastShield == -1 || numberOfTurnsSinceLastShield > 3
    }
}

fun main() {
    val laps = readLine()!!.toInt()
    val checkpointCount = readLine()!!.toInt()
    val checkpoints = List(checkpointCount) {
        val (x, y) = readLine()!!.split(" ").map { it.toFloat() }
        Vec2(x, y)
    }

    val allyPod1 = Pod(checkpoints, "Pod_1", true)
    val allyPod2 = Pod(checkpoints, "Pod_2", false, true)

    while (true) {
        allyPod1.readNewState(List(4) { readLine()!!.toFloat() })
        allyPod2.readNewState(List(4) { readLine()!!.toFloat() })

        println(allyPod1.writeCommand())
        println(allyPod2.writeCommand())
    }
}