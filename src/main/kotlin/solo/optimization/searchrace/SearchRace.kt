import java.util.*
import kotlin.math.*
import kotlin.math.pow

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

    var lastCheckpointIndex = -1 // 마지막으로 통과한 체크포인트 인덱스
    
    // game loop
    while (true) {
        val checkpointIndex = input.nextInt() // Index of the checkpoint to lookup in the checkpoints input, initially 0
        val x = input.nextInt() // Position X
        val y = input.nextInt() // Position Y
        val vx = input.nextInt() // horizontal speed. Positive is right
        val vy = input.nextInt() // vertical speed. Positive is downwards
        val angle = input.nextInt() // facing angle of this car

        // 체크포인트 변경 감지
        val checkpointChanged = checkpointIndex != lastCheckpointIndex
        lastCheckpointIndex = checkpointIndex

        // 현재 목표 체크포인트 정보
        val target = checkpoints[checkpointIndex]
        val targetX = target.first
        val targetY = target.second
        
        // 다음 체크포인트 정보 (앞서 보기)
        val nextCheckpointIndex = (checkpointIndex + 1) % checkpoints.size
        val nextTarget = checkpoints[nextCheckpointIndex]
        
        // 체크포인트까지의 거리 계산
        val distance = calculateDistance(x, y, targetX, targetY)
        
        // 현재 위치에서 목표 지점까지의 각도 계산
        val targetAngle = calculateAngle(x, y, targetX, targetY)
        
        // 회전해야 하는 각도 계산
        val angleToTurn = calculateAngleToTurn(angle, targetAngle)
        
        // 현재 속도 계산
        val speed = sqrt((vx * vx + vy * vy).toDouble())
        
        // 목표 지점 계산 (다음 체크포인트를 향해 미리 조정)
        var adjustedTargetX = targetX
        var adjustedTargetY = targetY
        
        // 체크포인트에 가까워지면 다음 체크포인트를 향해 미리 조정
        if (distance < 1200 && abs(angleToTurn) < 30) {
            // 현재 체크포인트와 다음 체크포인트 사이의 방향 계산
            val nextAngle = calculateAngle(targetX, targetY, nextTarget.first, nextTarget.second)
            val nextDistance = calculateDistance(targetX, targetY, nextTarget.first, nextTarget.second)
            
            // 가중치 계산 (가까울수록 다음 체크포인트 방향 비중 증가)
            val weight = max(0.0, min(0.8, (1200 - distance) / 1200.0))
            
            // 가중치에 따라 목표 지점 조정
            val offsetX = cos(Math.toRadians(nextAngle)) * 600 * weight
            val offsetY = sin(Math.toRadians(nextAngle)) * 600 * weight
            
            adjustedTargetX = (targetX + offsetX).toInt()
            adjustedTargetY = (targetY + offsetY).toInt()
        }
        
        // 회전 각도와 거리에 따라 추진력 조절
        val thrustCommand = determineThrust(angleToTurn, distance, vx, vy, speed)
        
        // 출력: 조정된 목표 X, 목표 Y, 추진력, 메시지
        println("$adjustedTargetX $adjustedTargetY $thrustCommand")
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
fun determineThrust(angleToTurn: Double, distance: Double, vx: Int, vy: Int, speed: Double): String {
    val absAngle = abs(angleToTurn)
    
    // 물리 엔진의 감속 효과(0.85)를 고려한 예상 다음 속도
    val nextSpeed = speed * 0.85
    
    // 체크포인트 도달 예상 시간(턴) 계산
    val estimatedTurnsToTarget = if (speed > 0) distance / speed else 999.0
    
    // 체크포인트에 정확히 도달하기 위한 감속 계산
    if (distance < 800 && speed > 100) {
        val deceleration = speed * (1 - 0.85) // 한 턴에 감속되는 양
        val turnsToStop = speed / deceleration // 정지까지 필요한 턴 수
        val distanceToStop = speed * (1 - 0.85.pow(turnsToStop)) / 0.15 // 정지 거리
        
        // 정지 거리가 체크포인트 거리보다 크면 감속
        if (distanceToStop > distance - 100) {
            return "0"
        }
    }
    
    // 회전이 클수록 속도 감소
    if (absAngle > 90) {
        return "0"
    } else if (absAngle > 50) {
        return "50"
    } else if (absAngle > 25) {
        return "80"
    } else if (absAngle > 10) {
        return "120"
    }
    
    // 직진 시 최대 추진력
    return "200"
}
