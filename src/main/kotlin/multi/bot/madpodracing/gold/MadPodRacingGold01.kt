package multi.bot.madpodracing.gold

import java.util.*
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sqrt

object GlobalVars {
    const val MAP_MAX_X = 16000
    const val MAP_MAX_Y = 9000
    const val CHECKPOINT_RADIUS = 600
    const val POD_RADIUS = 400
    const val FRICTION = 0.85
    const val POD_COUNT = 2
    const val SO_FAR = 2000
    lateinit var checkpoints: List<Checkpoint>
    var laps: Int = 0
    var checkpointCount: Int = 0
    var canUseBoost: Boolean = true
    var useBoostCheckpointId: Int = 0
}

object Calculator {
    // 좌표간 거리 계산 (반환값 범위: 최소 0 ~ 최대 약 18358)
    fun getDistance(x1: Int, y1: Int, x2: Int, y2: Int): Double {
        return sqrt((x1 - x2).toDouble().pow(2) + (y1 - y2).toDouble().pow(2))
    }

    // 각도 차이 계산 (반환값 범위: 0 ~ 180)
    fun getAngleDiff(x1: Int, y1: Int, x2: Int, y2:Int, angle: Int): Double {
        val angleToCheckPoint: Double = getAngleToTarget(x1, y1, x2, y2)
        val rawDiff = abs(angleToCheckPoint - angle)
        return minOf(rawDiff, 360 - rawDiff)
    }

    // 각도 계산 (반환값 범위: 0 ~ 359)
    fun getAngleToTarget(x1: Int, y1: Int, x2: Int, y2: Int): Double {
        val dx = (x2 - x1).toDouble()
        val dy = (y2 - y1).toDouble()
        val angle = Math.toDegrees(atan2(dy, dx))
        return if (angle < 0) angle + 360 else angle
    }

    // 연속된 체크포인트 중 가장 먼 거리를 가진 체크포인트의 ID 반환
    // 반환값은 해당 체크포인트의 ID이며, 이 체크포인트에서 다음 체크포인트까지가 가장 긴 거리
    fun getFurthestCheckpointId(): Int {
        var maxDistance = 0.0
        var furthestCheckpointId = 0

        for (i in 0 until GlobalVars.checkpointCount) {
            val current = GlobalVars.checkpoints[i]
            val next = GlobalVars.checkpoints[(i + 1) % GlobalVars.checkpointCount]

            val distance = getDistance(current.x, current.y, next.x, next.y)

            if (distance > maxDistance) {
                maxDistance = distance
                furthestCheckpointId = (i + 1) % GlobalVars.checkpointCount
            }
        }

        return furthestCheckpointId
    }
}

data class Checkpoint(
    val id: Int,
    val x: Int,
    val y: Int
)  {

    // 특정 좌표가 체크포인트 내에 있는지 확인
    private fun contains(pointX: Int, pointY: Int): Boolean {
        val distance = Calculator.getDistance(x, y, pointX, pointY)
        return distance <= GlobalVars.CHECKPOINT_RADIUS
    }

    // 파드의 위치와 다음 체크포인트를 고려하여 최적의 진입점을 반환
    fun getOptimize(podX: Int, podY: Int): Checkpoint {
        // 파드가 이미 체크포인트 내부에 있는 경우 체크포인트를 그대로 반환
        if (contains(podX, podY)) {
            return this
        }

        // 파드 방향 벡터 계산
        val dx = podX - x
        val dy = podY - y

        // 벡터 길이 계산
        val length = sqrt(dx * dx + dy * dy.toDouble())
        if (length < 1e-6) {
            return this
        }

        val nextCheckpoint = GlobalVars.checkpoints[(id + 1) % GlobalVars.checkpointCount]

        // 현재 -> 다음 체크포인트 벡터
        val nextDx = nextCheckpoint.x - x
        val nextDy = nextCheckpoint.y - y
        val nextLength = sqrt(nextDx * nextDx + nextDy * nextDy.toDouble())

        if (nextLength > 1e-6) {
            // 가중치 설정 (현재 70%, 다음 30%)
            val currentWeight = 0.7
            val nextWeight = 0.3

            // 가중 평균 벡터 계산
            val weightedDx = (dx / length) * currentWeight + (nextDx / nextLength) * nextWeight
            val weightedDy = (dy / length) * currentWeight + (nextDy / nextLength) * nextWeight

            // 최종 벡터 정규화
            val weightedLength = sqrt(weightedDx * weightedDx + weightedDy * weightedDy)
            if (weightedLength > 1e-6) {
                val normalizedX = weightedDx / weightedLength
                val normalizedY = weightedDy / weightedLength

                val closestX = x + (normalizedX * GlobalVars.CHECKPOINT_RADIUS).roundToInt()
                val closestY = y + (normalizedY * GlobalVars.CHECKPOINT_RADIUS).roundToInt()

                return this.copy(x = closestX, y = closestY)
            }
        } else {
            // 기본 계산법 (다음 체크포인트 정보가 없거나 계산 실패 시)
            val normalizedX = dx / length
            val normalizedY = dy / length

            val closestX = x + (normalizedX * GlobalVars.CHECKPOINT_RADIUS).roundToInt()
            val closestY = y + (normalizedY * GlobalVars.CHECKPOINT_RADIUS).roundToInt()

            return this.copy(x = closestX, y = closestY)
        }
        return this
    }
}

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

    fun getNextPosition(thrust: Int = 0): Pair<Int, Int> {
        // 추진력을 고려한 가속도 계산
        val angleRad = Math.toRadians(angle.toDouble())
        val thrustVx = thrust * kotlin.math.cos(angleRad)
        val thrustVy = thrust * kotlin.math.sin(angleRad)

        // 최종 속도 계산 (현재 속도 + 추진력으로 인한 가속)
        val finalVx = (vx + thrustVx) * GlobalVars.FRICTION
        val finalVy = (vy + thrustVy) * GlobalVars.FRICTION

        // 다음 위치 계산
        val nextX = x + finalVx
        val nextY = y + finalVy

        return Pair(nextX.roundToInt(), nextY.roundToInt())
    }

    fun getInfoStr(): String {
        return "[$id] nextCheckPointId:$nextCheckPointId"
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
    var currentSpeed: Double = 0.0,
    var opponentPods: List<OpponentPod> = emptyList(),
    var shieldCooldown: Int = 0,
    var nextCheckpoint: Checkpoint = GlobalVars.checkpoints[nextCheckPointId],
    var thrust: Int = 100,
    var beforeNextCheckpointId: Int = 0,
    var laps: Int = 0,
    val isRacer: Boolean
): Pod {

    override fun updateInfo(x: Int, y: Int, vx: Int, vy: Int, angle: Int, nextCheckPointId: Int) {
        super.updateInfo(x, y, vx, vy, angle, nextCheckPointId)
        this.nextCheckpoint = GlobalVars.checkpoints[nextCheckPointId].getOptimize(x, y)
        this.shieldCooldown = (this.shieldCooldown - 1).coerceIn(0, 4)
        this.currentSpeed = sqrt((vx * vx + vy * vy).toDouble())
        this.thrust = calculateThrust()
        if (this.beforeNextCheckpointId != nextCheckPointId) {
            this.beforeNextCheckpointId = nextCheckPointId
            if (nextCheckPointId == 1) {
                this.laps ++
            }
        }
    }

    private fun calculateThrust(): Int {
        if (shieldCooldown == 3) return 0
        val angleDiff = Calculator.getAngleDiff(x, y, nextCheckpoint.x, nextCheckpoint.y, angle)
        val distanceToCheckPoint: Double = Calculator.getDistance(x, y, nextCheckpoint.x, nextCheckpoint.y)
        if (angleDiff > 150) { return 5 }
        if (angleDiff > 120) { return 10 }
        if (angleDiff > 90) { return 20 }
        if (distanceToCheckPoint > GlobalVars.SO_FAR && currentSpeed < 200) {
            return 100
        }
        return when {
            angleDiff > 60 -> 30
            angleDiff > 45 -> 40
            angleDiff > 30 -> 50
            angleDiff > 20 -> 60
            angleDiff > 10 -> 70
            else -> 100
        }
    }

    private fun expectCollision(collisionDistance: Int = 800): Pair<Int, Int>? {
        val (nextX, nextY) = this.getNextPosition(thrust)
        opponentPods.forEach { opponentPod ->
            val (nextOpponentX, nextOpponentY) = opponentPod.getNextPosition()
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
        val isLastChance = this.laps == (GlobalVars.laps-1) && this.nextCheckPointId == GlobalVars.useBoostCheckpointId
        if (!isLastChance) return false
        if (!GlobalVars.canUseBoost) return false
        if (shieldCooldown == 3) return false
        val angleDiff = Calculator.getAngleDiff(x, y, nextCheckpoint.x, nextCheckpoint.y, angle)
        if (angleDiff > 1) return false
        return true
//        val distanceToCheckPoint: Double = Calculator.getDistance(x, y, nextCheckpoint.x, nextCheckpoint.y)
//        return distanceToCheckPoint > GlobalVars.SO_FAR
    }

    fun updateOpponentPods(opponentPods: List<OpponentPod>) {
        this.opponentPods = opponentPods
    }

    fun commandStr(): String {
        if (isRacer && shouldUseBoost()) {
            GlobalVars.canUseBoost = false
            return "${nextCheckpoint.x} ${nextCheckpoint.y} BOOST ${getInfoStr()} BOOST"
        }
        // Shield 사용 결정
        shouldUseShield()?.let { (x, y) ->
            shieldCooldown = 4 // 현재 턴 + 3턴 쿨다운
            return "$x $y SHIELD ${getInfoStr()} SHIELD"
        }
        if (!isRacer) {
            expectCollision(1000)?.let { (x, y) ->
                return "$x $y 100 ${getInfoStr()} rush"
            }
        }
        return "${nextCheckpoint.x} ${nextCheckpoint.y} $thrust ${getInfoStr()} thrust: $thrust"
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
    GlobalVars.useBoostCheckpointId = Calculator.getFurthestCheckpointId()
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