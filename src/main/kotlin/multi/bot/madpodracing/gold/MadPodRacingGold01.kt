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
    val nextId: Int = (id + 1) % GlobalVars.checkpointCount
)  {

    private fun getNearest(targetX: Int, targetY: Int): Pair<Int, Int> {
        // 타겟 좌표와 체크포인트 중심 사이의 거리 계산
        val distToCenter = Calculator.getDistance(x, y, targetX, targetY)

        // 타겟이 체크포인트 내부에 있으면 타겟 좌표 그대로 반환
        if (distToCenter <= (GlobalVars.CHECKPOINT_RADIUS-50)) {
            return targetX to targetY
        }

        // 타겟이 체크포인트 외부에 있으면 중심에서 타겟 방향으로 CHECKPOINT_RADIUS 거리에 있는 점 계산
        val ratio = (GlobalVars.CHECKPOINT_RADIUS-50) / distToCenter
        val adjustedX = x + ((targetX - x) * ratio).toInt()
        val adjustedY = y + ((targetY - y) * ratio).toInt()

        return adjustedX to adjustedY
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

    fun getOptimize2(pod: MyPod): Checkpoint {
        // 체크포인트를 향해갈때 각도의 차이만큼 좌표를 이동시킨다.
        // 좌표 이동의 목적은 체크포인트로 향하는 각도 차이를 빠르게 0으로 만드는것이 목적

        // 각도 차이에 따른 보정 계수를 더 공격적으로 조정 (각도 차이가 클수록 더 강한 보정)
        val turnFactor = when {
            pod.nextCheckpointAngleDiff > 90 -> 0.95  // 급격한 회전 필요 - 매우 강화
            pod.nextCheckpointAngleDiff > 60 -> 0.90  // 중간-높은 회전 - 강화
            pod.nextCheckpointAngleDiff > 45 -> 0.85  // 중간 정도 회전 - 강화
            pod.nextCheckpointAngleDiff > 20 -> 0.80  // 약간의 회전 - 강화
            pod.nextCheckpointAngleDiff > 10 -> 0.75  // 미세 조정 - 강화
            pod.nextCheckpointAngleDiff > 5 -> 0.70   // 더 미세한 조정
            else -> 0.65            // 매우 정밀한 조정
        }

        val nearestCheckpoint = getOptimize(pod.x, pod.y)
        val closestX = nearestCheckpoint.x
        val closestY = nearestCheckpoint.y

        // 목표 지점과 현재 위치 사이의 단위 벡터 계산
        val dx = closestX - pod.x
        val dy = closestY - pod.y
        val distance = sqrt((dx * dx + dy * dy).toDouble())

        // 현재 각도 방향의 단위 벡터 계산 (라디안으로 변환)
        val angleRad = Math.toRadians(pod.angle.toDouble())
        val dirX = cos(angleRad)
        val dirY = sin(angleRad)

        // 기본 거리 설정 (각도 차이가 클수록 더 짧게)
        val baseDistance = when {
            pod.nextCheckpointAngleDiff > 90 -> 1000
            pod.nextCheckpointAngleDiff > 60 -> 1200
            pod.nextCheckpointAngleDiff > 30 -> 1500
            pod.nextCheckpointAngleDiff > 10 -> 1800
            else -> 2000
        }

        // 최종 목표 좌표 계산 (현재 방향과 목표 방향의 가중 평균)
        // 각도 차이가 클수록 목표 방향의 가중치를 높임
        val targetWeight = turnFactor  // 목표 방향의 가중치 강화
        val currentWeight = 1 - targetWeight  // 현재 방향의 가중치

        // 각도 차이에 따라 더 강한 보정을 적용
        val angleCorrection = pod.nextCheckpointAngleDiff / 90.0  // 0 ~ 1 사이 값
        val correctionFactor = 1.0 + angleCorrection  // 1 ~ 2 사이 값

        val newX = closestX + ((dx / distance) * targetWeight + dirX * currentWeight) * baseDistance * correctionFactor
        val newY = closestY + ((dy / distance) * targetWeight + dirY * currentWeight) * baseDistance * correctionFactor

        return this.copy(
            x = newX.roundToInt().coerceIn(0, GlobalVars.MAP_MAX_X),
            y = newY.roundToInt().coerceIn(0, GlobalVars.MAP_MAX_Y)
        )
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
    var thrust: Int = 100,
    var nextCheckpointAngleDiff: Int = 0
): Pod {

    override fun updateInfo(x: Int, y: Int, vx: Int, vy: Int, angle: Int, nextCheckPointId: Int) {
        super.updateInfo(x, y, vx, vy, angle, nextCheckPointId)
        this.currentSpeed = sqrt((vx * vx + vy * vy).toDouble())
        this.shieldCooldown = (this.shieldCooldown - 1).coerceIn(0, 4)
        this.nextCheckpoint = GlobalVars.checkpoints[nextCheckPointId]
        this.nextCheckpointAngleDiff = nextCheckpoint.getAngleDiff(x, y, angle)
        this.nextCheckpoint = GlobalVars.checkpoints[nextCheckPointId].getOptimize2(this)
        this.thrust = calculateThrust()
    }

    private fun calculateThrust(): Int {
        // 체크포인트까지의 거리 계산
        val distanceToCheckpoint = Calculator.getDistance(x, y, nextCheckpoint.x, nextCheckpoint.y)

        // 다음 체크포인트 이후의 체크포인트 정보 (급격한 방향 전환 필요 여부 확인)
        val nextNextCheckpointId = (nextCheckPointId + 1) % GlobalVars.checkpointCount
        val nextNextCheckpoint = GlobalVars.checkpoints[nextNextCheckpointId]

        // 현재 체크포인트와 다음 체크포인트 사이의 각도 계산
        val nextAngle = atan2(
            (nextNextCheckpoint.y - nextCheckpoint.y).toDouble(),
            (nextNextCheckpoint.x - nextCheckpoint.x).toDouble()
        )
        val currentAngle = atan2(
            (nextCheckpoint.y - y).toDouble(),
            (nextCheckpoint.x - x).toDouble()
        )
        val turnAngle = abs(Math.toDegrees(nextAngle - currentAngle).roundToInt()) % 180

        // 기본 추력 설정 (각도 차이에 따라 조정)
        val baseThrust = when {
            nextCheckpointAngleDiff > 90 -> 1   // 매우 큰 각도 차이 - 완전히 감속
            nextCheckpointAngleDiff > 60 -> 20  // 큰 각도 차이 - 강하게 감속
            nextCheckpointAngleDiff > 45 -> 40  // 중간 각도 차이 - 중간 감속
            nextCheckpointAngleDiff > 30 -> 60  // 작은 각도 차이 - 약간 감속
            nextCheckpointAngleDiff > 15 -> 80  // 미세한 각도 차이 - 약간만 감속
            nextCheckpointAngleDiff > 5 -> 90   // 거의 직선 - 거의 최대 속도
            else -> 100                         // 직선 - 최대 속도
        }

        // 거리에 따른 추력 조정 (가까울수록 감속)
        val distanceThrust = when {
            distanceToCheckpoint < 600 && turnAngle > 60 -> 20  // 체크포인트 가깝고 다음 방향 전환이 큰 경우 강하게 감속
            distanceToCheckpoint < 800 && turnAngle > 45 -> 40  // 체크포인트 가깝고 다음 방향 전환이 중간인 경우 중간 감속
            distanceToCheckpoint < 1000 && turnAngle > 30 -> 60 // 체크포인트 가깝고 다음 방향 전환이 작은 경우 약간 감속
            distanceToCheckpoint < 1200 -> 80                   // 체크포인트 가까운 경우 약간만 감속
            else -> 100                                         // 체크포인트 충분히 멀리 있는 경우 최대 속도
        }

        // 속도에 따른 추력 조정 (이미 빠른 경우 각도 차이에 더 민감하게 반응)
        val speedFactor = when {
            currentSpeed > 400 && nextCheckpointAngleDiff > 30 -> 1   // 매우 빠르고 각도 차이가 큰 경우 완전히 감속
            currentSpeed > 300 && nextCheckpointAngleDiff > 45 -> 20  // 빠르고 각도 차이가 큰 경우 강하게 감속
            currentSpeed > 200 && nextCheckpointAngleDiff > 60 -> 40  // 중간 속도에 각도 차이가 큰 경우 중간 감속
            else -> 100                                               // 그 외의 경우 최대 속도
        }

        // 최종 추력 계산 (각 요소의 최소값 사용)
        return minOf(baseThrust, distanceThrust, speedFactor)
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
        if (nextCheckpointAngleDiff > 1) return false
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

    fun getInfoStr(): String {
        val role = if (isRacer) "R" else "B"
        return "[$role] b:$canUseBoost l:$laps nci:$nextCheckPointId s:${currentSpeed.roundToInt()}"
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
//        val distance = Calculator.getDistance(x, y, nextCheckpoint.x, nextCheckpoint.y)
//        return if (distance > 2500) {
//            val (alignmentX, alignmentY) = getAlignmentCoordinate()
//            "$alignmentX $alignmentY $thrust ${getInfoStr()} t:$thrust"
//        } else {
//            "${nextCheckpoint.x} ${nextCheckpoint.y} $thrust ${getInfoStr()} t:$thrust"
//        }
        return "${nextCheckpoint.x} ${nextCheckpoint.y} $thrust ${getInfoStr()} t:$thrust"
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
