package com.me.codingame.multi.bot.madpodracing.gold

import java.util.*

object GameConstants {
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
    var nextCheckPointId: Int = 1,
    var nextCheckPoint: CheckPoint = GameConstants.CHECKPOINT_LIST[nextCheckPointId]
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

        this.nextCheckPoint = GameConstants.CHECKPOINT_LIST[nextCheckPointId]
    }

    fun outputAction(): String {
        return "${nextCheckPoint.x} ${nextCheckPoint.y} ${GameConstants.MAX_THRUST}"
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