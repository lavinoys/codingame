import java.util.*
import kotlin.math.*

fun main(args : Array<String>) {
    val input = Scanner(System.`in`)
    var prevX: Int? = null
    var prevY: Int? = null
    var boostUsed = false
    var shieldCooldown = 0

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

        // 속도 벡터 계산
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

        // 상대방 위치 분석
        val toOpponentX = opponentX - x
        val toOpponentY = opponentY - y
        val opponentDistance = hypot(toOpponentX.toDouble(), toOpponentY.toDouble())

        // 방향 벡터 및 상대 벡터 계산
        val dirMagnitude = hypot(dx.toDouble(), dy.toDouble())
        val opponentMagnitude = hypot(toOpponentX.toDouble(), toOpponentY.toDouble())

        // 각도 계산 (코사인 법칙)
        val dotProduct = dx * toOpponentX + dy * toOpponentY
        val cosineTheta = if (dirMagnitude * opponentMagnitude != 0.0) {
            dotProduct / (dirMagnitude * opponentMagnitude)
        } else {
            0.0
        }

        // 5도 이내 판정 (cos(5°) ≈ 0.9962)
        val isFrontal = abs(cosineTheta) >= cos(Math.toRadians(5.0))
        val isClose = opponentDistance <= 800

        // SHIELD 사용 조건
        val shouldShield = shieldCooldown == 0 &&
                isClose &&
                isFrontal

        // 추력 결정 로직
        when {
            shouldShield -> thrust = 0  // SHIELD가 우선 적용
            absAngle > 90 -> thrust = 0
            nextCheckpointDist > 4000 -> thrust = 100
            nextCheckpointDist < 800 -> thrust = 30
            else -> {
                val angleFactor = (90 - absAngle)/90.0
                val distanceFactor = nextCheckpointDist/4000.0
                thrust = (100 * angleFactor * distanceFactor).toInt().coerceIn(50, 100)
            }
        }

        // 행동 실행
        when {
            shouldShield -> {
                println("$targetX $targetY SHIELD")
                shieldCooldown = 3
            }
            !boostUsed && absAngle < 5 && nextCheckpointDist > 6000 -> {
                println("$targetX $targetY BOOST")
                boostUsed = true
            }
            else -> {
                if (shieldCooldown > 0) {
                    thrust = 0
                    shieldCooldown--
                }
                println("$targetX $targetY $thrust")
            }
        }
    }
}