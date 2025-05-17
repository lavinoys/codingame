package multi.bot.madpodracing.gold

import java.util.*
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin
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
    var useBoostCheckpointId: Int = 0
}

object Calculator {
    // 좌표간 거리 계산 (반환값 범위: 최소 0 ~ 최대 약 18357.56)
    fun getDistance(x1: Int, y1: Int, x2: Int, y2: Int): Double {
        return sqrt((x1 - x2).toDouble().pow(2) + (y1 - y2).toDouble().pow(2))
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

    fun sortByMostProgressed(pods: List<Pod>): List<Pod> {
        return pods.sortedByDescending { pod ->
            // 체크포인트 0은 첫 번째이자 마지막 체크포인트이므로 특별 처리
            val adjustedCheckpointId = if (pod.nextCheckPointId == 0) GlobalVars.checkpointCount else pod.nextCheckPointId
            val nextCheckpointDistance = getDistance(
                pod.x,
                pod.y,
                GlobalVars.checkpoints[pod.nextCheckPointId].x,
                GlobalVars.checkpoints[pod.nextCheckPointId].y
            )
            val distanceRatio = nextCheckpointDistance / 18000.0  // 거리를 0~1 사이 값으로 정규화
            (pod.laps * 1000) + (adjustedCheckpointId * 100) - (distanceRatio * 50)  // 진행률 계산 (가중치 적용)
        }
    }
}

data class Checkpoint(
    val id: Int,
    val x: Int,
    val y: Int,
    val nextId: Int = (id + 1) % GlobalVars.checkpointCount,
    private val minX: Int = (x - GlobalVars.CHECKPOINT_RADIUS),
    private val maxX: Int = (x + GlobalVars.CHECKPOINT_RADIUS),
    private val minY: Int = (y - GlobalVars.CHECKPOINT_RADIUS),
    private val maxY: Int = (y + GlobalVars.CHECKPOINT_RADIUS)
)  {

    private fun getNearest(targetX: Int, targetY: Int): Pair<Int, Int> {
        // 먼저 사각형 내의 가장 가까운 지점 찾기
        val closestX = (minX..maxX).minByOrNull { abs(it - targetX) } ?: x
        val closestY = (minY..maxY).minByOrNull { abs(it - targetY) } ?: y

        // 찾은 지점이 실제 원 내부에 있는지 확인
        val distToCenter = Calculator.getDistance(x, y, closestX, closestY)

        // 원 내부에 있으면 그대로 반환
        if (distToCenter <= GlobalVars.CHECKPOINT_RADIUS) {
            return closestX to closestY
        }

        // 원 바깥에 있으면 중심에서 해당 방향으로 CHECKPOINT_RADIUS 거리에 있는 점 계산
        val ratio = GlobalVars.CHECKPOINT_RADIUS / distToCenter
        val adjustedX = x + ((closestX - x) * ratio).toInt()
        val adjustedY = y + ((closestY - y) * ratio).toInt()

        return adjustedX to adjustedY
    }

    fun getOptimize(podX: Int, podY: Int): Checkpoint {
        val distance = Calculator.getDistance(x, y, podX, podY)
        val (closestX, closestY) = when {
            distance > 2000 -> { getNearest(podX, podY) }
            else -> {
                val nextCheckpoint = GlobalVars.checkpoints[nextId]
                getNearest(nextCheckpoint.x, nextCheckpoint.y)
            }
        }
        return this.copy(
            x = closestX,
            y = closestY,
        )
    }

    private fun getAngleByPod(podX: Int, podY: Int): Int {
        val dx = (x - podX).toDouble()
        val dy = (y - podY).toDouble()
        val angle = Math.toDegrees(atan2(dy, dx)).roundToInt()
        return if (angle < 0) angle + 360 else angle
    }

    // 0 ~ 180
    fun getAngleDiff(podX: Int, podY: Int, podAngle: Int): Int {
        val angle = getAngleByPod(podX, podY)
        val diff = abs(angle - podAngle) % 360
        return minOf(diff, 360 - diff)
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
    var beforeNextCheckpointId: Int
    var laps: Int
    var isRacer: Boolean

    fun updateInfo(x: Int, y: Int, vx: Int, vy: Int, angle: Int, nextCheckPointId: Int) {
        this.x = x
        this.y = y
        this.vx = vx
        this.vy = vy
        this.angle = angle
        this.nextCheckPointId = nextCheckPointId
        if (this.beforeNextCheckpointId != nextCheckPointId) {
            this.beforeNextCheckpointId = nextCheckPointId
            if (nextCheckPointId == 1) {
                this.laps ++
            }
        }
    }

    fun getNextPosition(): Pair<Int, Int> {
        val nextVx = (vx * GlobalVars.FRICTION).toInt()
        val nextVy = (vy * GlobalVars.FRICTION).toInt()
        val nextX = x + nextVx
        val nextY = y + nextVy
        return Pair(nextX, nextY)
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
    override var beforeNextCheckpointId: Int = 0,
    override var laps: Int = 0,
    override var isRacer: Boolean = false,
    var canUseBoost: Boolean = true,
    var currentSpeed: Double = 0.0,
    var opponentPods: List<OpponentPod> = emptyList(),
    var shieldCooldown: Int = 0,
    var nextCheckpoint: Checkpoint = GlobalVars.checkpoints[nextCheckPointId],
    var thrust: Int = 100
): Pod {

    override fun updateInfo(x: Int, y: Int, vx: Int, vy: Int, angle: Int, nextCheckPointId: Int) {
        super.updateInfo(x, y, vx, vy, angle, nextCheckPointId)
        this.currentSpeed = sqrt((vx * vx + vy * vy).toDouble())
        this.shieldCooldown = (this.shieldCooldown - 1).coerceIn(0, 4)
        this.nextCheckpoint = GlobalVars.checkpoints[nextCheckPointId].getOptimize(x, y)
        this.thrust = calculateThrust()
    }

    private fun calculateThrust(): Int {
        val angleDiff = nextCheckpoint.getAngleDiff(x, y, angle)
        return when {
            angleDiff > 90 -> 5
            angleDiff > 75 -> 10
            angleDiff > 45 -> 30
            angleDiff > 30 -> 50
            angleDiff > 10 -> 70
            angleDiff > 5 -> 90
            else -> 100
        }
    }

    private fun expectCollision(): Pair<Int, Int>? {
        val podRadiusPlus = GlobalVars.POD_RADIUS * 2
        val (nextX, nextY) = this.getNextPosition()
        opponentPods.forEach { opponentPod ->
            val (nextOpponentX, nextOpponentY) = opponentPod.getNextPosition()
            val fromNextMixed = Calculator.getDistance(nextX, nextY, nextOpponentX, nextOpponentY)
            if (fromNextMixed < podRadiusPlus) {
                return Pair(nextOpponentX, nextOpponentY)
            }
            val fromOpponent = Calculator.getDistance(x, y, nextOpponentX, nextOpponentY)
            if (fromOpponent < podRadiusPlus) {
                return Pair(nextOpponentX, nextOpponentY)
            }
            if (isRacer) return null
            val fromOur = Calculator.getDistance(opponentPod.x, opponentPod.y, nextX, nextY)
            if (fromOur < podRadiusPlus) {
                return Pair(nextX, nextY)
            }
        }
        return null
    }

    private fun shouldUseShield(): Pair<Int, Int>? {
        if (shieldCooldown == 1 || shieldCooldown == 2) return null
        return expectCollision()
    }

    private fun shouldUseBoost(): Boolean {
        val isLastChance = this.laps == GlobalVars.laps && this.nextCheckPointId == GlobalVars.useBoostCheckpointId
        if (!isLastChance) return false
        if (!canUseBoost) return false
        if (shieldCooldown == 3) return false
        val angleDiff = nextCheckpoint.getAngleDiff(x, y, angle)
        if (angleDiff > 1) return false
        return true
    }

    private fun shouldUseRush(): Pair<Int, Int>? {
        val (nextX, nextY) = this.getNextPosition()
        opponentPods.forEach { opponentPod ->
            val (nextOpponentX, nextOpponentY) = opponentPod.getNextPosition()
            val distance = Calculator.getDistance(nextX, nextY, nextOpponentX, nextOpponentY)
            if (distance > 1000) return@forEach
            return nextOpponentX to nextOpponentY
        }
        return null
    }

    private fun getAlignmentCoordinate(): Pair<Int, Int> {
        val angleDiff = nextCheckpoint.getAngleDiff(this.x, this.y, this.angle)
        if (angleDiff < 5) return nextCheckpoint.x to nextCheckpoint.y
        // 각도 차이에 따른 보정 계수를 더 공격적으로 조정
        val turnFactor = when {
            angleDiff > 90 -> 0.6  // 급격한 회전 필요 - 매우 강화
            angleDiff > 60 -> 0.7  // 중간-높은 회전 - 강화
            angleDiff > 45 -> 0.8  // 중간 정도 회전 - 강화
            angleDiff > 20 -> 0.9  // 약간의 회전 - 강화
            angleDiff > 10 -> 0.95 // 미세 조정 - 강화
            else -> 1.0            // 거의 정렬됨
        }

        // 속도에 따른 목표 거리 계수 최적화
        val speedMultiplier = when {
            currentSpeed > 500 -> 0.02  // 매우 빠른 속도에서는 더 날카롭게 회전
            currentSpeed > 400 -> 0.022 // 빠른 속도 조정
            currentSpeed > 300 -> 0.025 // 중간 속도 조정
            else -> 0.03               // 느린 속도
        }
        val distanceFactor = (currentSpeed * speedMultiplier).coerceIn(1.0, 5.0) // 최대값 감소

        // 목표 지점과 현재 위치 사이의 단위 벡터 계산
        val dx = nextCheckpoint.x - this.x
        val dy = nextCheckpoint.y - this.y
        val distance = sqrt((dx * dx + dy * dy).toDouble())

        // 현재 각도 방향의 단위 벡터 계산 (라디안으로 변환)
        val angleRad = Math.toRadians(this.angle.toDouble())
        val dirX = cos(angleRad)
        val dirY = sin(angleRad)

        // 속도에 따라 기본 거리 조정
        val baseDistance = when {
            currentSpeed > 400 -> 2200
            currentSpeed > 300 -> 2000
            else -> 1800
        }

        // 최종 목표 좌표 계산 (현재 방향과 목표 방향의 가중 평균)
        val newX = this.x + ((dx / distance) * turnFactor + dirX * (1 - turnFactor)) * distanceFactor * baseDistance
        val newY = this.y + ((dy / distance) * turnFactor + dirY * (1 - turnFactor)) * distanceFactor * baseDistance

        return Pair(newX.roundToInt().coerceIn(0, GlobalVars.MAP_MAX_X), newY.roundToInt().coerceIn(0, GlobalVars.MAP_MAX_Y))
    }

    fun getInfoStr(): String {
        val role = if (isRacer) "R" else "B"
        return "[$role] b:$canUseBoost l:$laps nci:$nextCheckPointId s:$currentSpeed"
    }

    fun updateOpponentPods(opponentPods: List<OpponentPod>) {
        this.opponentPods = opponentPods
    }

    fun commandStr(): String {
        if (shouldUseBoost()) {
            canUseBoost = false
            return "${nextCheckpoint.x} ${nextCheckpoint.y} BOOST ${getInfoStr()} BOOST"
        }
        // Shield 사용 결정
        shouldUseShield()?.let { (x, y) ->
            shieldCooldown = 4 // 현재 턴 + 3턴 쿨다운
            return if (isRacer) {
                "${nextCheckpoint.x} ${nextCheckpoint.y} SHIELD ${getInfoStr()} SHIELD"
            } else {
                "$x $y SHIELD ${getInfoStr()} SHIELD"
            }
        }
        if (!isRacer) {
            shouldUseRush()?.let { (x, y) ->
                return "$x $y 100 ${getInfoStr()} rush"
            }
        }
        val (alignmentX, alignmentY) = getAlignmentCoordinate()
        return if (currentSpeed > 300) {
            "$alignmentX $alignmentY 100 ${getInfoStr()} align"
        } else {
            "${nextCheckpoint.x} ${nextCheckpoint.y} $thrust ${getInfoStr()} t:$thrust"
        }
    }
}

data class OpponentPod(
    override val id: Int,
    override var x: Int = 0,
    override var y: Int = 0,
    override var vx: Int = 0, // -667 ~ 667
    override var vy: Int = 0, // -667 ~ 667
    override var angle: Int = 0, // 0 ~ 360
    override var nextCheckPointId: Int = 0,
    override var beforeNextCheckpointId: Int = 0,
    override var laps: Int = 0,
    override var isRacer: Boolean = false
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

        Calculator.sortByMostProgressed(myPods).forEachIndexed { idx, pod ->
            pod.isRacer = idx == 0
        }

        myPods.forEach { pod ->
            pod.updateOpponentPods(opponentPods)
            println(pod.commandStr())
        }
    }
}