package com.me.codingame.multi.bot.madpodracing.gold

import java.util.*
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

object GameConstants {
    const val MAX_WIDTH = 16000
    const val MAX_HEIGHT = 9000
    const val POD_RADIUS = 400
    const val CHECKPOINT_RADIUS = 600
    const val MAX_THRUST = 100
    const val MAX_SPEED = 800
    const val POD_COUNT = 2
    const val MAX_ANGLE = 18 // 18 degrees
    const val FRICTION = 0.85 // 0.85
    var TOTAL_LAPS: Int = 0
    var TOTAL_CHECKPOINT_COUNT: Int = 0
    val CHECKPOINT_LIST = mutableListOf<CheckPoint>()
}

data class OpponentPod(
    val id: Int,
    var x: Int = 0,
    var y: Int = 0,
    var vx: Int = 0,
    var vy: Int = 0,
    var angle: Int = 0,
    var nextCheckPointId: Int = 1
) {
    fun update(
        x: Int,
        y: Int,
        vx: Int,
        vy: Int,
        angle: Int,
        nextCheckPointId: Int
    ) {
        this.x = x
        this.y = y
        this.vx = vx
        this.vy = vy
        this.angle = angle
        this.nextCheckPointId = nextCheckPointId
    }
}

data class CheckPoint(
    val id: Int,
    val x: Int,
    val y: Int
)

data class OurPod(
    val id: Int,
    var x: Int = 0,
    var y: Int = 0,
    var vx: Int = 0,
    var vy: Int = 0,
    var angle: Int = 0,
    var speed: Double = 0.0,
    var velocityAngle: Double = 0.0,
    var magnitudeOfVelocity: Double = 0.0,
    var angleOfVelocityVector: Double = 0.0,
    var nextCheckPointId: Int = 1,
    var nextCheckPoint: CheckPoint = GameConstants.CHECKPOINT_LIST[nextCheckPointId],
    var nextCheckPointDistance: Double = 0.0,
    var nextCheckPointAngle: Double = 0.0,
    var outputX: Int = 0,
    var outputY: Int = 0,
    var outputThrust: Int = GameConstants.MAX_THRUST
) {
    fun update(
        x: Int,
        y: Int,
        vx: Int,
        vy: Int,
        angle: Int,
        nextCheckPointId: Int
    ) {
        this.x = x
        this.y = y
        this.vx = vx
        this.vy = vy
        this.angle = angle
        this.nextCheckPointId = nextCheckPointId

        this.speed = sqrt((this.vx * this.vx + this.vy * this.vy).toDouble())
        this.velocityAngle = Math.toDegrees(
            atan2(
                this.vy.toDouble(),
                this.vx.toDouble()
            )
        ).let {
            ((it + 360) % 360).coerceIn(0.0, 360.0)
        }
        this.magnitudeOfVelocity = sqrt(
            (vx.toDouble().pow(2.0) + vy.toDouble().pow(2.0))
        ).coerceIn(0.0, GameConstants.MAX_SPEED.toDouble())
        this.angleOfVelocityVector = Math.toDegrees(
            atan2(
                vy.toDouble(),
                vx.toDouble()
            )
        ).let {
            ((it + 360) % 360).coerceIn(0.0, 360.0)
        }

        this.nextCheckPoint = GameConstants.CHECKPOINT_LIST[nextCheckPointId]
        this.nextCheckPointDistance = calculateNextCheckPointDistance()
        this.nextCheckPointAngle = calculateNextCheckPointAngle()
        calculateOutputPosition().let { (outputX, outputY) ->
            this.outputX = outputX
            this.outputY = outputY
        }
        this.outputThrust = calculateThrust()
    }

    fun calculateNextCheckPointDistance(): Double {
        return sqrt(
            (nextCheckPoint.x - x).toDouble().pow(2.0) + (nextCheckPoint.y - y).toDouble().pow(2.0)
        ).coerceIn(0.0, 18357.0)
    }

    fun calculateNextCheckPointAngle(): Double {
        val dx = nextCheckPoint.x - x
        val dy = nextCheckPoint.y - y
        val angle = Math.toDegrees(
            atan2(
                dy.toDouble(),
                dx.toDouble()
            )
        )
        return ((angle + 360) % 360).coerceIn(0.0, 360.0)
    }

    fun calculateThrust(): Int {
        val driftAngle = (angleOfVelocityVector - angle).let { ((it + 180) % 360) - 180 }
        val isDrifting = abs(driftAngle) > 30 // 드리프트 임계값 설정
        if (nextCheckPointDistance < GameConstants.CHECKPOINT_RADIUS * 2 && abs(nextCheckPointAngle) > 30) {
            // thrust를 낮춰 관성으로 미끄러지게 함 (검색 결과 [1] 참고)
            return if (abs(nextCheckPointAngle) > 45) 1 else 50
        } else {
            if (isDrifting && nextCheckPointDistance < GameConstants.CHECKPOINT_RADIUS * 3) {
                return 1
            }
        }
        val thrustByDistance = GameConstants.MAX_THRUST * (nextCheckPointDistance / (3 * GameConstants.CHECKPOINT_RADIUS))
        return thrustByDistance.roundToInt().coerceIn(10, GameConstants.MAX_THRUST)
    }

    fun calculateOutputPosition(): Pair<Int, Int> {
        if (nextCheckPointDistance < GameConstants.CHECKPOINT_RADIUS * 2 && abs(nextCheckPointAngle) > 30) {
            // 중간 각도로 보정
            val desiredAngle = Math.toRadians(
                ((angle + 360) % 360 + (nextCheckPointAngle - angle) * 0.5) % 360
            )

            // 체크포인트로부터의 방향 오프셋 계산
            val offsetX = GameConstants.CHECKPOINT_RADIUS * 0.8 * cos(desiredAngle)
            val offsetY = GameConstants.CHECKPOINT_RADIUS * 0.8 * sin(desiredAngle)

            // 체크포인트 중심 기준으로 위치 계산
            outputX = (nextCheckPoint.x + offsetX).roundToInt()
            outputY = (nextCheckPoint.y + offsetY).roundToInt()

            // 원형 영역 제한: 체크포인트 중심에서 목표 지점까지의 거리 계산
            val distanceToCenter = sqrt(
                (outputX - nextCheckPoint.x).toDouble().pow(2) +
                        (outputY - nextCheckPoint.y).toDouble().pow(2)
            )

            // 거리가 체크포인트 반경을 초과하면 원형 경계로 조정
            if (distanceToCenter > GameConstants.CHECKPOINT_RADIUS) {
                val ratio = GameConstants.CHECKPOINT_RADIUS / distanceToCenter
                outputX = (nextCheckPoint.x + (outputX - nextCheckPoint.x) * ratio).roundToInt()
                outputY = (nextCheckPoint.y + (outputY - nextCheckPoint.y) * ratio).roundToInt()
            }

            return Pair(outputX, outputY)
        }
        return nextCheckPoint.x to nextCheckPoint.y
    }

    fun outputAction(): String {
        return "${this.outputX} ${this.outputX} $outputThrust [${this.id}]x:${this.outputX}y:${this.outputY}t:${this.outputThrust}"
    }
}

fun main() {
    val input = Scanner(System.`in`)
    GameConstants.TOTAL_LAPS = input.nextInt()
    GameConstants.TOTAL_CHECKPOINT_COUNT = input.nextInt()
    (0 until GameConstants.TOTAL_CHECKPOINT_COUNT).map { index ->
        GameConstants.CHECKPOINT_LIST.add(
            CheckPoint(
                index,
                input.nextInt(),
                input.nextInt()
            )
        )
    }

    val ourPods = Array(GameConstants.POD_COUNT) { OurPod(it) }
    val opponentPods = Array(GameConstants.POD_COUNT) { OpponentPod(it) }

    // game loop
    while (true) {
        ourPods.forEach { pod ->
            pod.update(
                x = input.nextInt(), // x position of your pod
                y = input.nextInt(), // y position of your pod
                vx = input.nextInt(), // x speed of your pod
                vy = input.nextInt(), // y speed of your pod
                angle = input.nextInt(), // angle of your pod
                nextCheckPointId = input.nextInt() // next check point id of your pod
            )
        }
        opponentPods.forEach { pod ->
            pod.update(
                x = input.nextInt(), // x position of the opponent's pod
                y = input.nextInt(), // y position of the opponent's pod
                vx = input.nextInt(), // x speed of the opponent's pod
                vy = input.nextInt(), // y speed of the opponent's pod
                angle = input.nextInt(), // angle of the opponent's pod
                nextCheckPointId = input.nextInt() // next check point id of the opponent's pod
            )
        }
        ourPods.forEach { pod ->
            println(pod.outputAction())
        }
    }
}