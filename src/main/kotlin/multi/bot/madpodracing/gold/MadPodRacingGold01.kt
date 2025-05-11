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

class PIDController(
    private var kp: Double,     // 비례 게인
    private var ki: Double,     // 적분 게인
    private var kd: Double,     // 미분 게인
    private var maxOutput: Double = 100.0,  // 최대 출력값
    private var maxIntegral: Double = 50.0  // 적분항 최대값 (windup 방지)
) {
    private var previousError: Double = 0.0
    private var integral: Double = 0.0
    private var firstRun: Boolean = true

    // 턴 기반 게임에 최적화된 계산 메서드
    fun calculate(setpoint: Double, currentValue: Double): Double {
        // 오차 계산
        val error = setpoint - currentValue

        // 첫 실행 시 이전 오차 초기화
        if (firstRun) {
            previousError = error
            firstRun = false
        }

        // 적분항 업데이트 (windup 방지)
        integral += error
        integral = integral.coerceIn(-maxIntegral, maxIntegral)

        // 미분항 계산 (오차 변화량)
        val derivative = error - previousError
        previousError = error

        // PID 출력 계산
        val output = kp * error + ki * integral + kd * derivative

        // 출력 제한 (-100 ~ 100)
        return output.coerceIn(-maxOutput, maxOutput)
    }

    // 컨트롤러 초기화
    fun reset() {
        previousError = 0.0
        integral = 0.0
        firstRun = true
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
    val isRacer: Boolean,
    // PID 컨트롤러 추가
    private val anglePID: PIDController = PIDController(0.8, 0.01, 0.4),
    private val thrustPID: PIDController = PIDController(0.5, 0.0, 0.2, 100.0, 30.0)
): Pod {

    override fun updateInfo(x: Int, y: Int, vx: Int, vy: Int, angle: Int, nextCheckPointId: Int) {
        super.updateInfo(x, y, vx, vy, angle, nextCheckPointId)
        this.nextCheckPoint = GlobalVars.checkpoints[nextCheckPointId]
        this.shieldCooldown = Math.max(0, this.shieldCooldown - 1)
        // 체크포인트가 변경되면 PID 리셋
        if (this.nextCheckPointId != nextCheckPointId) {
            anglePID.reset()
            thrustPID.reset()
        }
    }

    private fun getCalculateNextCheckpoint(): Pair<Int, Int> {
        // 체크포인트 방향으로의 기본 목표점
        var targetX = nextCheckPoint.x
        var targetY = nextCheckPoint.y

        // 다음 체크포인트 방향으로 선회를 미리 준비
        val nextIdx = (nextCheckPointId + 1) % GlobalVars.checkpointCount
        val nextNextCheckpoint = GlobalVars.checkpoints[nextIdx]

        // 속도에 따라 선회 시작 거리 동적 조정 (최소 800 ~ 최대 1500)
        val turnStartDistance = (800 + currentSpeed * 2.5).coerceIn(800.0, 1500.0)

        val distanceToCheckpoint = Calculator.getDistance(x, y, nextCheckPoint.x, nextCheckPoint.y)

        // 체크포인트에 가까워지면 다음 체크포인트 방향으로 선회
        if (distanceToCheckpoint < turnStartDistance) {
            // 속도가 빠를수록 강하게 선회, 느릴수록 약하게 선회
            val baseRatio = (turnStartDistance - distanceToCheckpoint) / turnStartDistance
            val speedFactor = (currentSpeed / 400.0).coerceIn(0.5, 1.5)
            val ratio = baseRatio * speedFactor

            // 선회 비율 제한 (최대 0.8까지)
            val cappedRatio = ratio.coerceIn(0.0, 0.8)

            targetX = (nextCheckPoint.x + (nextNextCheckpoint.x - nextCheckPoint.x) * cappedRatio * 0.5).toInt()
            targetY = (nextCheckPoint.y + (nextNextCheckpoint.y - nextCheckPoint.y) * cappedRatio * 0.5).toInt()
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

    private fun getThrustByPID(): Int {
        val (targetX, targetY) = getCalculateNextCheckpoint()
        val distanceToCheckPoint = Calculator.getDistance(x, y, targetX, targetY)
        val angleDiff = Calculator.getNormalizedAngleDiff(x, y, targetX, targetY, angle)

        val baseThrust = when {
            distanceToCheckPoint > 2000 -> 100.0
            // 2000 이내 세분화된 구간
            distanceToCheckPoint > 1500 -> 70.0 + 10.0 * ((distanceToCheckPoint - 1500) / 500.0)
            distanceToCheckPoint > 1000 -> 60.0 + 10.0 * ((distanceToCheckPoint - 1000) / 500.0)
            distanceToCheckPoint > 750 -> 50.0 + 10.0 * ((distanceToCheckPoint - 750) / 250.0)
            distanceToCheckPoint > 500 -> 40.0 + 10.0 * ((distanceToCheckPoint - 500) / 250.0)
            distanceToCheckPoint > 300 -> 35.0 + 5.0 * ((distanceToCheckPoint - 300) / 200.0)
            distanceToCheckPoint > 150 -> 30.0 + 5.0 * ((distanceToCheckPoint - 150) / 150.0)
            else -> 20.0 + 10.0 * (distanceToCheckPoint / 150.0)
        }

        // 목표 속도 계산 (baseThrust에 비례)
        val targetSpeed = baseThrust * 6.5  // 추력을 속도로 변환한 근사치

        // thrustPID 컨트롤러로 목표 속도에 도달하기 위한 추력 보정값 계산
        val speedAdjustment = thrustPID.calculate(targetSpeed, currentSpeed)

        // 기존 각도 계산 로직
        val angleControl = anglePID.calculate(0.0, angleDiff)

        // 나머지 기존 로직
        val targetAngleRad = atan2((targetY - y).toDouble(), (targetX - x).toDouble())
        val velocityAngleRad = if (currentSpeed > 10) atan2(vy.toDouble(), vx.toDouble()) else targetAngleRad
        val velocityAlignmentFactor = Math.cos(targetAngleRad - velocityAngleRad)

        // 각도에 따른 추력 조정 계수
        val angleControlFactor = if (currentSpeed < 50) {
            Math.max(0.3, 1.0 - Math.min(0.7, Math.abs(angleControl) / 100.0))
        } else {
            1.0 - Math.min(0.8, Math.abs(angleControl) / 100.0)
        }

        // 속도 일치도에 따른 계수
        val velocityFactor = 0.6 + 0.4 * Math.max(0.0, velocityAlignmentFactor)

        // PID 보정을 적용한 최종 추력 계산
        val finalThrust = (baseThrust + speedAdjustment) * angleControlFactor * velocityFactor

        // 각도별 최소 추력 보장
        val thrustResult = when {
            Math.abs(angleDiff) > 90 -> if (currentSpeed < 50) 20 else 10
            Math.abs(angleDiff) > 45 -> Math.max(25, finalThrust.toInt() / 2)
            else -> finalThrust.toInt()
        }

        return thrustResult.coerceIn(10, 100)
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
        val thrust = getThrustByPID()
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