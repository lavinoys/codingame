import java.util.*
import kotlin.math.*

fun main(args : Array<String>) {
    val input = Scanner(System.`in`)
    var prevX: Int? = null
    var prevY: Int? = null
    var boostUsed = false

    // 게임 루프
    while (true) {
        // 입력 읽기
        val x = input.nextInt()
        val y = input.nextInt()
        val nextCheckpointX = input.nextInt()
        val nextCheckpointY = input.nextInt()
        val nextCheckpointDist = input.nextInt()
        val nextCheckpointAngle = input.nextInt()
        val opponentX = input.nextInt()
        val opponentY = input.nextInt()

        // 속도 계산
        var dx = 0
        var dy = 0
        if (prevX != null && prevY != null) {
            dx = x - prevX
            dy = y - prevY
        }
        prevX = x
        prevY = y

        // 목표 지점 계산 (관성 보정)
        val k = 3
        val targetX = nextCheckpointX - dx * k
        val targetY = nextCheckpointY - dy * k

        // 추력 계산
        var thrust: Int
        val absAngle = abs(nextCheckpointAngle)

        // 상대방과의 거리 계산
        val opponentDistance = sqrt(
            (opponentX - x).toDouble().pow(2) +
                    (opponentY - y).toDouble().pow(2)
        )

        // 충돌 위험 확인
        val isCollisionRisk = opponentDistance <= 800

        when {
            isCollisionRisk -> thrust = 0  // 충돌 회피
            absAngle > 90 -> thrust = 0    // 방향 정렬 필요
            nextCheckpointDist > 4000 -> thrust = 100  // 최대 가속
            nextCheckpointDist < 800 -> thrust = 30    // 감속 구간
            else -> {                      // 유동적 제어
                val angleFactor = (90 - absAngle) / 90.0
                val distanceFactor = nextCheckpointDist / 4000.0
                thrust = (100 * angleFactor * distanceFactor).toInt().coerceIn(50, 100)
            }
        }

        // 부스트 사용 조건
        if (!boostUsed && absAngle < 5 && nextCheckpointDist > 6000) {
            println("$targetX $targetY BOOST")
            boostUsed = true
        } else {
            println("$targetX $targetY $thrust")
        }
    }
}