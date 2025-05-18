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
    var useBoostCheckpointId: Int = 0
}

object Calculator {
    // 좌표간 거리 계산 (반환값 범위: 최소 0 ~ 최대 약 18357.56)
    fun getDistance(x1: Int, y1: Int, x2: Int, y2: Int): Double {
        return sqrt((x1 - x2).toDouble().let { it * it} + (y1 - y2).toDouble().let { it * it})
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

    private fun getMinMaxCoordinates(): Pair<Pair<Int, Int>, Pair<Int, Int>> {
        val fixedRadius = GlobalVars.CHECKPOINT_RADIUS - 10
        val minX = (this.x - fixedRadius).coerceIn(0, GlobalVars.MAP_MAX_X)
        val maxX = (this.x + fixedRadius).coerceIn(0, GlobalVars.MAP_MAX_X)
        val minY = (this.y - fixedRadius).coerceIn(0, GlobalVars.MAP_MAX_Y)
        val maxY = (this.y + fixedRadius).coerceIn(0, GlobalVars.MAP_MAX_Y)
        return Pair(Pair(minX, maxY), Pair(maxX, minY))
    }

    private fun getNearest(targetX: Int, targetY: Int): Pair<Int, Int> {
        // 타겟 좌표와 체크포인트 중심 사이의 거리 계산
        val distToCenter = Calculator.getDistance(this.x, this.y, targetX, targetY)

        val (fixedX, fixedY) = getMinMaxCoordinates()
        val (fixedMinX, fixedMaxY) = fixedX
        val (fixedMaxX, fixedMinY) = fixedY

        // 타겟이 체크포인트 내부에 있으면 타겟 좌표 그대로 반환
        if (distToCenter <= GlobalVars.CHECKPOINT_RADIUS) {
            return targetX.coerceIn(fixedMinX, fixedMaxX) to targetY.coerceIn(fixedMinY, fixedMaxY)
        }

        // 타겟이 체크포인트 외부에 있으면 중심에서 타겟 방향으로 CHECKPOINT_RADIUS 거리에 있는 점 계산
        val ratio = GlobalVars.CHECKPOINT_RADIUS / distToCenter
        val adjustedX = this.x + ((targetX - this.x) * ratio).toInt()
        val adjustedY = this.y + ((targetY - this.y) * ratio).toInt()

        return adjustedX.coerceIn(fixedMinX, fixedMaxX) to adjustedY.coerceIn(fixedMinY, fixedMaxY)
    }

    fun getAngleByPod(podX: Int, podY: Int): Int {
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
        if (this.id == 0) return this
        val (closestX, closestY) = getNearest(podX, podY)
        return this.copy(
            x = closestX.coerceIn(0, GlobalVars.MAP_MAX_X),
            y = closestY.coerceIn(0, GlobalVars.MAP_MAX_Y),
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

    fun getNextCoordinate(): Pair<Int, Int> {
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
    var thrust: Int = 100,
    var nextCheckpoint: Checkpoint = GlobalVars.checkpoints[nextCheckPointId],
    var nextCheckpointAngleDiff: Int = 0,
    var doubleNextCheckpointId: Int = 0,
    var doubleNextCheckpoint: Checkpoint = GlobalVars.checkpoints[nextCheckPointId],
    var doubleNextCheckpointAngleDiff: Int = 0,
): Pod {

    override fun updateInfo(x: Int, y: Int, vx: Int, vy: Int, angle: Int, nextCheckPointId: Int) {
        super.updateInfo(x, y, vx, vy, angle, nextCheckPointId)
        this.currentSpeed = sqrt((vx * vx + vy * vy).toDouble()) * GlobalVars.FRICTION
        this.shieldCooldown = (this.shieldCooldown - 1).coerceIn(0, 4)
        this.nextCheckpoint = GlobalVars.checkpoints[nextCheckPointId].getOptimize(x, y)
        this.nextCheckpointAngleDiff = nextCheckpoint.getAngleDiff(x, y, angle)
        this.doubleNextCheckpointId = (nextCheckPointId + 1) % GlobalVars.checkpointCount
        this.doubleNextCheckpoint = GlobalVars.checkpoints[doubleNextCheckpointId]
        this.doubleNextCheckpointAngleDiff = doubleNextCheckpoint.getAngleDiff(x, y, angle)
        this.thrust = calculateThrust()
    }

    private fun calculateThrust(): Int {
        val (nextX, nextY) = getNextCoordinate()
        val distanceToNextCheckpoint = Calculator.getDistance(
            nextX,
            nextY,
            nextCheckpoint.x,
            nextCheckpoint.y
        )

        if (currentSpeed > 200 && distanceToNextCheckpoint < 1500 && doubleNextCheckpointAngleDiff > 15) {
            return 1
        }

        val baseThrust = when {
            nextCheckpointAngleDiff > 45 -> 40
            nextCheckpointAngleDiff > 30 -> 60
            nextCheckpointAngleDiff > 15 -> 80
            nextCheckpointAngleDiff > 5 -> 95
            else -> 100
        }

        // 최종 추력 계산 (각 요소의 최소값 사용)
        return baseThrust.coerceIn(0, 100)
    }

    private fun expectCollision(): Pair<Int, Int>? {
        val podRadiusPlus = GlobalVars.POD_RADIUS * 2
        val (nextX, nextY) = this.getNextCoordinate()
        opponentPods.forEach { opponentPod ->
            val (nextOpponentX, nextOpponentY) = opponentPod.getNextCoordinate()
            val fromNextMixed = Calculator.getDistance(nextX, nextY, nextOpponentX, nextOpponentY)
            if (fromNextMixed < podRadiusPlus) {
                return Pair(nextOpponentX, nextOpponentY)
            }
            if (isRacer) return null
            val fromOpponent = Calculator.getDistance(this.x, this.y, nextOpponentX, nextOpponentY)
            if (fromOpponent < podRadiusPlus) {
                return Pair(nextOpponentX, nextOpponentY)
            }
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
        val (nextX, nextY) = this.getNextCoordinate()
        opponentPods.forEach { opponentPod ->
            val (nextOpponentX, nextOpponentY) = opponentPod.getNextCoordinate()
            val distance = Calculator.getDistance(nextX, nextY, nextOpponentX, nextOpponentY)
            if (distance > GlobalVars.POD_RADIUS * 3) return@forEach
            return nextOpponentX to nextOpponentY
        }
        return null
    }

    private fun getInfoStr(): String {
        val role = if (isRacer) "R" else "B"
        return "[$role] l:${laps}nci:${nextCheckPointId}s:${currentSpeed.roundToInt()}ad:$nextCheckpointAngleDiff"
    }

    private fun hotFixAngle(): Pair<Int, Int> {
        val distCheckpoint = Calculator.getDistance(x, y, nextCheckpoint.x, nextCheckpoint.y)
        if (distCheckpoint < 2000) return nextCheckpoint.x to nextCheckpoint.y

        // 각도 차이가 클수록 더 급격한 선회가 필요
        val angleWeight = (nextCheckpointAngleDiff / 90.0).coerceAtMost(1.0)

        // 각도 조정 강도 - 값이 클수록 더 멀리 떨어진 지점으로 조정
        val angleAdjustStrength = 2000

        // 체크포인트 방향으로 오프셋 적용 (각도가 클수록 체크포인트 쪽으로 더 가깝게 조정)
        val dirX = (nextCheckpoint.x - x).toDouble()
        val dirY = (nextCheckpoint.y - y).toDouble()
        val len = sqrt(dirX * dirX + dirY * dirY)

        // 단위 벡터에 각도 가중치를 적용해 목표 위치 계산
        val fixedX = nextCheckpoint.x - (dirX / len * angleWeight * angleAdjustStrength).toInt()
        val fixedY = nextCheckpoint.y - (dirY / len * angleWeight * angleAdjustStrength).toInt()

        return fixedX.coerceIn(0, GlobalVars.MAP_MAX_X) to fixedY.coerceIn(0, GlobalVars.MAP_MAX_Y)
    }

    fun updateOpponentPods(opponentPods: List<OpponentPod>) {
        this.opponentPods = opponentPods
    }

    fun commandStr(): String {
        if (shouldUseBoost()) {
            val (fixedX, fixedY) = hotFixAngle()
            canUseBoost = false
            return "$fixedX $fixedY BOOST ${getInfoStr()} BOOST"
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
                return "$x $y 100 ${getInfoStr()} RUSH"
            }
        }
        val (fixedX, fixedY) = hotFixAngle()
        return "$fixedX $fixedY $thrust ${getInfoStr()}t:$thrust"
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
