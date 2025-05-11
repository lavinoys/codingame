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
    val currentSpeed: Double = sqrt((vx * vx + vy * vy).toDouble()),
    var shieldCooldown: Int = 0,
    var opponentPods: List<OpponentPod> = emptyList(),
    var nextCheckPoint: Checkpoint = GlobalVars.checkpoints[nextCheckPointId],
    val isRacer: Boolean
): Pod {

    override fun updateInfo(x: Int, y: Int, vx: Int, vy: Int, angle: Int, nextCheckPointId: Int) {
        super.updateInfo(x, y, vx, vy, angle, nextCheckPointId)
        this.nextCheckPoint = GlobalVars.checkpoints[nextCheckPointId]
        this.shieldCooldown = Math.max(0, this.shieldCooldown - 1)
    }

    private fun getCalculateNextCheckpoint(): Pair<Int, Int> {
        // 기본 목표는 현재 체크포인트
        val targetX = nextCheckPoint.x
        val targetY = nextCheckPoint.y

        // 현재 체크포인트와의 거리
        val distanceToCheckpoint = Calculator.getDistance(x, y, nextCheckPoint.x, nextCheckPoint.y)

        // 체크포인트에 가까워지면(예: 1500 이내) 다음 체크포인트 방향으로 약간 선회
        if (distanceToCheckpoint < 1500) {
            val nextIdx = (nextCheckPointId + 1) % GlobalVars.checkpointCount
            val nextCheckpoint = GlobalVars.checkpoints[nextIdx]

            // 단순히 두 점 사이의 중간 지점으로 목표 수정 (선회 시작)
            return Pair(
                (nextCheckPoint.x + nextCheckpoint.x) / 2,
                (nextCheckPoint.y + nextCheckpoint.y) / 2
            )
        }

        return Pair(targetX, targetY)
    }

    private fun getThrustToCheckPoint(): Int {
        val (targetX, targetY) = getCalculateNextCheckpoint()
        val distanceToCheckPoint: Int = Calculator.getDistance(x, y, targetX, targetY)
        val angleDiff = Calculator.getAngleDiff(x, y, targetX, targetY, angle)
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

    private fun getThrustSimple(): Int {
        val (targetX, targetY) = getCalculateNextCheckpoint()
        val distanceToCheckPoint = Calculator.getDistance(x, y, targetX, targetY)
        val angleDiff = abs(Calculator.getNormalizedAngleDiff(x, y, targetX, targetY, angle))

        // 체크포인트 간 거리가 2000 이상이면 각도와 상관없이 최대 추력 사용
        if (distanceToCheckPoint > 3000) {
            return 100
        }

        // 각도에 따른 기본 추력 감소 계수 계산
        val angleFactorBase = when {
            angleDiff > 90 -> 0.1
            angleDiff > 45 -> 0.4
            angleDiff > 20 -> 0.7
            angleDiff > 10 -> 0.9
            else -> 1.0
        }

        // 거리에 따른 기본 추력 계산
        val baseThrust = when {
            distanceToCheckPoint > 2000 -> 90
            distanceToCheckPoint > 1000 -> 70
            distanceToCheckPoint > 500 -> 50
            distanceToCheckPoint > 200 -> 30
            else -> 20
        }

        // 속도와 방향이 일치하는지 확인 (코사인 유사도)
        val velocityMag = sqrt((vx * vx + vy * vy).toDouble())
        val velocityFactor = if (velocityMag > 10) {
            val targetAngleRad = atan2((targetY - y).toDouble(), (targetX - x).toDouble())
            val velocityAngleRad = atan2(vy.toDouble(), vx.toDouble())
            val alignment = Math.cos(targetAngleRad - velocityAngleRad)
            0.7 + 0.3 * Math.max(0.0, alignment)  // 0.7~1.0 범위로 제한
        } else {
            1.0  // 속도가 매우 낮은 경우 감속할 필요 없음
        }

        // 최종 추력 계산 (0.1~1.0 * 20~70 = 2~70)
        val finalThrust = (baseThrust * angleFactorBase * velocityFactor).roundToInt()

        // 추력 범위 제한 (10~100)
        return finalThrust.coerceIn(10, 100)
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
            expectCollision(1500)?.let { (x, y) ->
                return "$x $y 100 ${getInfoStr()} rush"
            }
        }
        val thrust = getThrustSimple()
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