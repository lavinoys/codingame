import java.util.*
import kotlin.math.*

/**
 * Made by Illedan
 **/
fun main(args : Array<String>) {
    val input = Scanner(System.`in`)
    val checkpointCount = input.nextInt() // Count of checkpoints to read
    val checkpoints = mutableListOf<Pair<Int, Int>>()
    for (i in 0 until checkpointCount) {
        val checkpointX = input.nextInt() // Position X
        val checkpointY = input.nextInt() // Position Y
        checkpoints.add(Pair(checkpointX, checkpointY))
    }

    // game loop
    while (true) {
        val checkpointIndex = input.nextInt() // Index of the checkpoint to lookup in the checkpoints input, initially 0
        val x = input.nextInt() // Position X
        val y = input.nextInt() // Position Y
        val vx = input.nextInt() // horizontal speed. Positive is right
        val vy = input.nextInt() // vertical speed. Positive is downwards
        val angle = input.nextInt() // facing angle of this car

        // 현재 목표 체크포인트 정보
        val target = checkpoints[checkpointIndex]
        val targetX = target.first
        val targetY = target.second
        
        // 체크포인트까지의 거리 계산
        val distance = calculateDistance(x, y, targetX, targetY)
        
        // 현재 위치에서 목표 지점까지의 각도 계산
        val targetAngle = calculateAngle(x, y, targetX, targetY)
        
        // 회전해야 하는 각도 계산
        val angleToTurn = calculateAngleToTurn(angle, targetAngle)
        
        // 회전 각도와 거리에 따라 추진력 조절
        val thrust = determineThrust(angleToTurn, distance, vx, vy)
        
        // 출력: 목표 X, 목표 Y, 추진력, 메시지
        println("$targetX $targetY $thrust")
    }
}

// 두 점 사이의 거리 계산
fun calculateDistance(x1: Int, y1: Int, x2: Int, y2: Int): Double {
    val dx = x2 - x1
    val dy = y2 - y1
    return sqrt((dx * dx + dy * dy).toDouble())
}

// 두 점 사이의 각도 계산 (동쪽이 0도)
fun calculateAngle(x1: Int, y1: Int, x2: Int, y2: Int): Double {
    val dx = x2 - x1
    val dy = y2 - y1
    val radAngle = atan2(dy.toDouble(), dx.toDouble())
    var degAngle = Math.toDegrees(radAngle)
    if (degAngle < 0) degAngle += 360
    return degAngle
}

// 현재 각도에서 목표 각도로의 최소 회전 각도 계산
fun calculateAngleToTurn(currentAngle: Int, targetAngle: Double): Double {
    var angleDiff = targetAngle - currentAngle
    while (angleDiff > 180) angleDiff -= 360
    while (angleDiff < -180) angleDiff += 360
    return angleDiff
}

// 회전 각도와 거리에 따라 추진력 조절
fun determineThrust(angleToTurn: Double, distance: Double, vx: Int, vy: Int): Int {
    val absAngle = abs(angleToTurn)
    val speed = sqrt((vx * vx + vy * vy).toDouble())
    
    // 체크포인트에 가까워졌을 때 감속
    if (distance < 1000 && speed > 150) {
        return 20
    }
    
    // 회전이 클수록 속도 감소
    if (absAngle > 90) {
        return 0
    } else if (absAngle > 50) {
        return 50
    } else if (absAngle > 20) {
        return 100
    } else if (absAngle > 10) {
        return 150
    }
    
    // 체크포인트에 가까워졌을 때 제어
    if (distance < 600) {
        return min(100, (distance / 6).toInt())
    }
    
    // 직진 시 최대 추진력
    return 200
}
