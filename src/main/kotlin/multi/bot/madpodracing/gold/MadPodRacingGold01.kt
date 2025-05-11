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

    // 각도 차이 계산 (반환값 범위: 0 ~ 180)
    fun getAngleDiff(x1: Int, y1: Int, x2: Int, y2:Int, angle: Int): Int {
        val angleToCheckPoint: Int = getAngle(x1, y1, x2, y2)
        val rawDiff = abs(angleToCheckPoint - angle)
        return minOf(rawDiff, 360 - rawDiff)
    }

    // 각도 차이 계산
    fun getNormalizedAngleDiff(x1: Int, y1: Int, x2: Int, y2:Int, angle: Int): Double {
        val targetAngle = getAngle(x1, y1, x2, y2).toDouble()
        var diff = targetAngle - angle

        // 각도를 -180 ~ 180 범위로 정규화
        while (diff > 180) diff -= 360
        while (diff < -180) diff += 360

        return diff
    }

    // 각도 계산 (반환값 범위: 0 ~ 359)
    private fun getAngle(x1: Int, y1: Int, x2: Int, y2: Int): Int {
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
    var opponentPods: List<OpponentPod> = emptyList(),
    var nextCheckPoint: Checkpoint = GlobalVars.checkpoints[nextCheckPointId],
    val isRacer: Boolean
): Pod {

    override fun updateInfo(x: Int, y: Int, vx: Int, vy: Int, angle: Int, nextCheckPointId: Int) {
        super.updateInfo(x, y, vx, vy, angle, nextCheckPointId)
        this.nextCheckPoint = GlobalVars.checkpoints[nextCheckPointId]  // 이 부분 추가
        this.shieldCooldown = Math.max(0, this.shieldCooldown - 1)
    }

    // 최적 경로를 위한 목표점 계산
    private fun getCalculateNextCheckpoint(): Pair<Int, Int> {
        // 체크포인트 방향으로의 기본 목표점
        var targetX = nextCheckPoint.x
        var targetY = nextCheckPoint.y

        // 다음 체크포인트 방향으로 선회를 미리 준비
        val nextIdx = (nextCheckPointId + 1) % GlobalVars.checkpointCount
        val nextNextCheckpoint = GlobalVars.checkpoints[nextIdx]

        val distanceToCheckpoint = Calculator.getDistance(x, y, nextCheckPoint.x, nextCheckPoint.y)

        // 체크포인트에 가까워지면 다음 체크포인트 방향으로 조금씩 선회
        if (distanceToCheckpoint < 1500) {
            val ratio = (1500 - distanceToCheckpoint) / 1500.0
            targetX = (nextCheckPoint.x + (nextNextCheckpoint.x - nextCheckPoint.x) * ratio * 0.5).toInt()
            targetY = (nextCheckPoint.y + (nextNextCheckpoint.y - nextCheckPoint.y) * ratio * 0.5).toInt()
        }

        return Pair(targetX, targetY)
    }

    private fun getThrustToCheckPoint(): Int {
        val (dx, dy) = getCalculateNextCheckpoint()
        val distanceToCheckPoint: Int = Calculator.getDistance(x, y, dx, dy)
        val angleDiff = Calculator.getAngleDiff(x, y, dx, dy, angle)
        System.err.println("pod: $id, angleDiff: $angleDiff")
        return when {
            30 < angleDiff -> 5
            20 < angleDiff -> 10
            10 < angleDiff -> 40
            else -> {
                when(distanceToCheckPoint) {
                    in 2000..Int.MAX_VALUE -> 100
                    in 1000..2000 -> 70
                    else -> 40
                }
            }
        }
    }

    private fun expectCollision(collisionDistance: Int = 800): Pair<Int, Int>? {
        val (nextX, nextY) = this.getNextCoordinate()
        opponentPods.forEach { opponentPod ->
            val (nextOpponentX, nextOpponentY) = opponentPod.getNextCoordinate()
            val fromOur = Calculator.getDistance(opponentPod.x, opponentPod.y, nextX, nextY)
            val fromOpponent = Calculator.getDistance(x, y, nextOpponentX, nextOpponentY)
            val fromNextMixed = Calculator.getDistance(nextX, nextY, nextOpponentX, nextOpponentY)
            if (fromOur < GlobalVars.POD_RADIUS || fromOpponent < GlobalVars.POD_RADIUS || fromNextMixed < collisionDistance) {
                return Pair(nextOpponentX, nextOpponentY)
            }
        }
        return null
    }

    private fun shouldUseShield(): Pair<Int, Int>? {
        if (shieldCooldown == 1 || shieldCooldown == 2) return null
        return expectCollision()
    }

    private fun shouldUseBoost(): Boolean {
        if (!GlobalVars.canUseBoost) return false
        if (shieldCooldown > 0) return false
        if (nextCheckPointId == 0) return false
        val angleDiff = Calculator.getAngleDiff(x, y, nextCheckPoint.x, nextCheckPoint.y, angle)
        if (angleDiff > 10) return false
        val distanceToCheckPoint: Int = Calculator.getDistance(x, y, nextCheckPoint.x, nextCheckPoint.y)
        return distanceToCheckPoint > 4000
    }

    fun updateOpponentPods(opponentPods: List<OpponentPod>) {
        this.opponentPods = opponentPods
    }

    fun commandStr(): String {
        // 목표점 계산
        val (tx, ty) = getCalculateNextCheckpoint()
        if (isRacer && shouldUseBoost()) {
            GlobalVars.canUseBoost = false
            return "$tx $ty BOOST ${getInfoStr()} BOOST"
        }
        // Shield 사용 결정
        shouldUseShield()?.let { (x, y) ->
            shieldCooldown = 4 // 현재 턴 + 3턴 쿨다운
            return "$x $y SHIELD ${getInfoStr()} SHIELD"
        }
        if (!isRacer) {
            expectCollision(2000)?.let { (x, y) ->
                return "$x $y 100 ${getInfoStr()} rush"
            }
        }
        val thrust = getThrustToCheckPoint()
        return "$tx $ty $thrust ${getInfoStr()} thrust: $thrust"
    }
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
        }
    }
}