package multi.bot.madpodracing.gold

import java.util.*
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.roundToInt
import kotlin.math.sqrt

object GlobalVars {
    const val MAP_MAX_X = 16000
    const val MAP_MAX_Y = 9000
    const val CHECKPOINT_RADIUS = 600
    const val POD_RADIUS = 400
    const val FRICTION = 0.85
    const val POD_COUNT = 2
    lateinit var checkpoints: List<Checkpoint>
    var laps: Int = 0
    var checkpointCount: Int = 0
    var canUseBoost: Boolean = true
}

object Calculator {
    // 좌표간 거리 계산 (반환값 범위: 최소 0 ~ 최대 약 18358)
    fun getDistance(x1: Int, y1: Int, x2: Int, y2: Int): Int {
        val dx = x2 - x1
        val dy = y2 - y1
        return sqrt((dx * dx + dy * dy).toDouble()).roundToInt()
    }

    // 각도 계산 (반환값 범위: 0 ~ 359)
    fun getAngle(x1: Int, y1: Int, x2: Int, y2: Int): Int {
        val dx = (x2 - x1).toDouble()
        val dy = (y2 - y1).toDouble()
        val angle = Math.toDegrees(atan2(dy, dx)).roundToInt()
        return if (angle < 0) angle + 360 else angle
    }
}

data class Checkpoint(
    val id: Int,
    val x: Int,
    val y: Int
)

interface Pod {
    val id: Int
    var x: Int
    var y: Int
    var vx: Int
    var vy: Int
    var angle: Int
    var nextCheckPointId: Int

    fun updateInfo(x: Int, y: Int, vx: Int, vy: Int, angle: Int, nextCheckPointId: Int) {
        this.x = x
        this.y = y
        this.vx = vx
        this.vy = vy
        this.angle = angle
        this.nextCheckPointId = nextCheckPointId
    }

    fun getNextCoordinate(): Pair<Int, Int> {
        val frictionVx = vx * GlobalVars.FRICTION
        val frictionVy = vy * GlobalVars.FRICTION
        val nextX = x + frictionVx
        val nextY = y + frictionVy
        return Pair(nextX.toInt(), nextY.toInt())
    }

    fun getInfoStr(): String {
        return "[$id] angle:$angle nextCheckPointId:$nextCheckPointId"
    }
}

data class MyPod(
    override val id: Int,
    override var x: Int = 0,
    override var y: Int = 0,
    override var vx: Int = 0, // -667 ~ 667
    override var vy: Int = 0, // -667 ~ 667
    override var angle: Int = 0, // 0 ~ 360
    override var nextCheckPointId: Int = 0,
    var shieldCooldown: Int = 0,
    var beforeInfo: BeforeInfo? = null,
    var opponentPods: List<OpponentPod> = emptyList(),
    var nextCheckPoint: Checkpoint = GlobalVars.checkpoints[nextCheckPointId],
    val isRacer: Boolean
): Pod {
    override fun updateInfo(x: Int, y: Int, vx: Int, vy: Int, angle: Int, nextCheckPointId: Int) {
        super.updateInfo(x, y, vx, vy, angle, nextCheckPointId)
        this.nextCheckPoint = GlobalVars.checkpoints[nextCheckPointId]  // 이 부분 추가
        this.shieldCooldown = Math.max(0, this.shieldCooldown - 1)
    }

    private fun getThrustToCheckPoint(): Int {
        val distanceToCheckPoint: Int = Calculator.getDistance(x, y, nextCheckPoint.x, nextCheckPoint.y)
        val angleToCheckPoint: Int = Calculator.getAngle(x, y, nextCheckPoint.x, nextCheckPoint.y)
        val angleDiff =  abs(angleToCheckPoint - angle)
        System.err.println("angleDiff: $angleDiff")
        return when {
            50 < angleDiff -> 5
            40 < angleDiff -> 10
            30 < angleDiff -> 20
            20 < angleDiff -> 40
            10 < angleDiff -> 60
            else -> {
                when(distanceToCheckPoint) {
                    in 0..500 -> 20
                    in 501..1000 -> 40
                    in 1001..2000 -> 60
                    in 2001..3000 -> 80
                    else -> 100
                }
            }
        }
    }

    private fun expectCollision(): Pair<Int, Int>? {
        val (nextX, nextY) = this.getNextCoordinate()
        opponentPods.forEach { opponentPod ->
            val (nextOpponentX, nextOpponentY) = opponentPod.getNextCoordinate()
            val fromOur = Calculator.getDistance(opponentPod.x, opponentPod.y, nextX, nextY)
            val fromOpponent = Calculator.getDistance(x, y, nextOpponentX, nextOpponentY)
            val fromNextMixed = Calculator.getDistance(nextX, nextY, nextOpponentX, nextOpponentY)
            if (fromOur < GlobalVars.POD_RADIUS || fromOpponent < GlobalVars.POD_RADIUS || fromNextMixed < 800) {
                return Pair(nextOpponentX, nextOpponentY)
            }
        }
        return null
    }

    private fun shouldUseShield(): Boolean {
        if (shieldCooldown in intArrayOf(1, 2)) return false
        return expectCollision() != null
    }

    private fun shouldUseBoost(): Boolean {
        if (!GlobalVars.canUseBoost) return false
        if (shieldCooldown > 0) return false
        if (nextCheckPointId == 0) return false
        val angleToCheckPoint: Int = Calculator.getAngle(x, y, nextCheckPoint.x, nextCheckPoint.y)
        val angleDiff =  abs(angleToCheckPoint - angle)
        if (angleDiff > 10) return false
        val distanceToCheckPoint: Int = Calculator.getDistance(x, y, nextCheckPoint.x, nextCheckPoint.y)
        return distanceToCheckPoint > 6000
    }

    fun updateBeforeInfo() {
        this.beforeInfo = BeforeInfo(
            x = this.x,
            y = this.y,
            vx = this.vx,
            vy = this.vy,
            angle = this.angle,
            nextCheckPointId = this.nextCheckPointId,
            thrust = this.getThrustToCheckPoint()
        )
    }

    fun updateOpponentPods(opponentPods: List<OpponentPod>) {
        this.opponentPods = opponentPods
    }

    fun commandStr(): String {
        if (isRacer && shouldUseBoost()) {
            GlobalVars.canUseBoost = false
            return "${nextCheckPoint.x} ${nextCheckPoint.y} BOOST ${getInfoStr()} BOOST"
        }
        // Shield 사용 결정
        if (shouldUseShield()) {
            shieldCooldown = 4 // 현재 턴 + 3턴 쿨다운
            return "${nextCheckPoint.x} ${nextCheckPoint.y} SHIELD ${getInfoStr()} SHIELD"
        }
        if (!isRacer) {
            expectCollision()?.let { (x, y) ->
                return "$x $y 100 ${getInfoStr()} rush"
            }
        }
        return "${nextCheckPoint.x} ${nextCheckPoint.y} ${getThrustToCheckPoint()} ${getInfoStr()} thrust: ${getThrustToCheckPoint()}"
    }

    data class BeforeInfo(
        val x: Int,
        val y: Int,
        val vx: Int,
        val vy: Int,
        val angle: Int,
        val nextCheckPointId: Int,
        val thrust: Int = 0
    )
}

data class OpponentPod(
    override val id: Int,
    override var x: Int = 0,
    override var y: Int = 0,
    override var vx: Int = 0, // -667 ~ 667
    override var vy: Int = 0, // -667 ~ 667
    override var angle: Int = 0, // 0 ~ 360
    override var nextCheckPointId: Int = 0
) :Pod

fun main() {
    val input = Scanner(System.`in`)

    GlobalVars.laps = input.nextInt()
    GlobalVars.checkpointCount = input.nextInt()
    GlobalVars.checkpoints = (0 until GlobalVars.checkpointCount).map { idx ->
        Checkpoint(
            idx,
            input.nextInt(),
            input.nextInt()
        )
    }
    // init pod
    val myPods: List<MyPod> = (0 until GlobalVars.POD_COUNT).map { idx ->
        MyPod(id = idx, isRacer = idx == 0)
    }
    val opponentPods: List<OpponentPod> = (0 until GlobalVars.POD_COUNT).map { idx -> OpponentPod(id = idx) }

    // game loop
    while (true) {
        myPods.forEach { pod ->
            pod.updateInfo(
                x = input.nextInt(), // x position of your pod
                y = input.nextInt(), // y position of your pod
                vx = input.nextInt(), // x speed of your pod
                vy = input.nextInt(), // y speed of your pod
                angle = input.nextInt(), // angle of your pod
                nextCheckPointId = input.nextInt() // next check point id of your pod
            )
        }
        opponentPods.forEach { pod ->
            pod.updateInfo(
                x = input.nextInt(), // x position of opponent pod
                y = input.nextInt(), // y position of opponent pod
                vx = input.nextInt(), // x speed of opponent pod
                vy = input.nextInt(), // y speed of opponent pod
                angle = input.nextInt(), // angle of opponent pod
                nextCheckPointId = input.nextInt() // next check point id of opponent pod
            )
        }

        myPods.forEach { pod ->
            pod.updateOpponentPods(opponentPods)
            println(pod.commandStr())
            pod.updateBeforeInfo()
        }
    }
}