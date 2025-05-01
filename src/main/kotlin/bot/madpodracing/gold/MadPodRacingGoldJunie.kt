import java.util.*
import kotlin.math.*

/**
 * 매드 포드 레이싱 골드 리그 구현
 * MadPodRacingGold.md의 규칙과 전략을 기반으로 함
 * 
 * 클린 아키텍처 원칙에 따라 구성됨:
 * 1. 엔티티 계층 (Entity Layer): 핵심 비즈니스 규칙과 데이터 구조
 *    - 게임의 기본 객체(Point, Checkpoint, BasePod 등)와 그들의 관계를 정의
 *    - 다른 계층에 의존하지 않는 독립적인 객체들
 * 
 * 2. 유스케이스 계층 (Use Case Layer): 애플리케이션 특화 비즈니스 규칙
 *    - 엔티티를 조작하는 비즈니스 로직(충돌 예측, 체크포인트 진입 예측 등)
 *    - 확장 함수를 통해 엔티티의 기능을 확장하여 관심사 분리
 * 
 * 3. 인터페이스 어댑터 계층 (Interface Adapters Layer): 외부 시스템과의 통신 변환
 *    - 유스케이스와 엔티티를 외부 프레임워크와 연결하는 어댑터
 *    - 데이터 변환 및 포맷팅 담당
 * 
 * 4. 프레임워크 및 드라이버 계층 (Frameworks & Drivers Layer): 외부 프레임워크와의 통합
 *    - 입출력, 외부 라이브러리와의 상호작용
 *    - 메인 함수와 게임 루프 포함
 * 
 * 이 구조는 다음과 같은 이점을 제공합니다:
 * - 관심사 분리: 각 계층은 특정 책임만 가짐
 * - 테스트 용이성: 비즈니스 로직을 외부 의존성과 분리하여 테스트 가능
 * - 유지보수성: 코드 변경이 다른 부분에 미치는 영향 최소화
 * - 확장성: 새로운 기능 추가가 용이함
 */

//================================================================
// 1. 엔티티 계층 (Entity Layer)
//================================================================
// 이 계층은 핵심 비즈니스 규칙과 데이터 구조를 포함합니다.
// 다른 계층에 의존하지 않는 독립적인 객체들로 구성됩니다.

// 애플리케이션 전체에서 공유되는 상수
object GameConstants {
    const val FRICTION = 0.85
    const val POD_SIZE = 400.0
    const val CHECKPOINT_RADIUS = 600.0

    // 궤적 최적화를 위한 상수
    const val MIN_DISTANCE_FOR_OPTIMIZATION = 50.0
    const val MAX_ANGLE_FOR_OPTIMIZATION = 70.0

    // 헤어핀 턴 처리를 위한 상수
    const val HAIRPIN_HIGH_ANGLE = 70
    const val HAIRPIN_MID_ANGLE = 40
    const val HAIRPIN_HIGH_THRUST = 100
    const val HAIRPIN_MID_THRUST = 30
    const val HAIRPIN_LOW_THRUST = 0
}

// 중복을 피하기 위한 벡터 유틸리티 함수
object VectorUtils {
    // 벡터의 크기 계산
    fun magnitude(x: Int, y: Int): Double = 
        sqrt(x.toDouble().pow(2) + y.toDouble().pow(2))

    // 더블 컴포넌트를 가진 벡터의 크기 계산
    fun magnitude(x: Double, y: Double): Double = 
        sqrt(x.pow(2) + y.pow(2))

    // 두 벡터의 내적 계산
    fun dotProduct(x1: Int, y1: Int, x2: Int, y2: Int): Int =
        x1 * x2 + y1 * y2

    // 더블 컴포넌트를 가진 두 벡터의 내적 계산
    fun dotProduct(x1: Double, y1: Double, x2: Double, y2: Double): Double =
        x1 * x2 + y1 * y2

    // 벡터 정규화
    fun normalize(x: Int, y: Int): Pair<Double, Double> {
        val mag = magnitude(x, y)
        return if (mag > 0.001) Pair(x / mag, y / mag) else Pair(0.0, 0.0)
    }

    // 더블 컴포넌트를 가진 벡터 정규화
    fun normalize(x: Double, y: Double): Pair<Double, Double> {
        val mag = magnitude(x, y)
        return if (mag > 0.001) Pair(x / mag, y / mag) else Pair(0.0, 0.0)
    }

    // 두 점 사이의 제곱 거리 계산 (비교를 위한 최적화)
    fun squaredDistance(x1: Int, y1: Int, x2: Int, y2: Int): Double =
        (x1 - x2).toDouble().pow(2) + (y1 - y2).toDouble().pow(2)
}

// 게임 엔티티를 위한 데이터 클래스
data class Point(val x: Int, val y: Int) {
    companion object {
        // 임시 Point 객체 생성을 피하기 위한 정적 메서드
        fun distanceBetween(x1: Int, y1: Int, x2: Int, y2: Int): Double =
            sqrt(VectorUtils.squaredDistance(x1, y1, x2, y2))

        // 비교 목적을 위한 제곱 거리 (최적화)
        fun squaredDistanceBetween(x1: Int, y1: Int, x2: Int, y2: Int): Double =
            VectorUtils.squaredDistance(x1, y1, x2, y2)

        fun angleBetween(x1: Int, y1: Int, x2: Int, y2: Int): Double =
            atan2((y2 - y1).toDouble(), (x2 - x1).toDouble()) * 180 / PI

        // 선 위의 가장 가까운 점 계산 (점 a와 b로 정의된 선에서 점 p까지)
        fun closestPointToLine(a: Point, b: Point, p: Point): Point {
            val vectorAtoP = Point(p.x - a.x, p.y - a.y)
            val vectorAtoB = Point(b.x - a.x, b.y - a.y)

            val distanceAtoB = distanceBetween(a.x, a.y, b.x, b.y)
            val distanceAtoBSquared = distanceAtoB * distanceAtoB

            if (distanceAtoBSquared < 0.0001) {
                return p
            }

            // 내적 계산을 위해 VectorUtils 사용
            val dotProduct = VectorUtils.dotProduct(vectorAtoP.x, vectorAtoP.y, vectorAtoB.x, vectorAtoB.y)
            val t = dotProduct / distanceAtoBSquared

            return Point(
                (a.x + vectorAtoB.x * t).toInt(),
                (a.y + vectorAtoB.y * t).toInt()
            )
        }

        // 점이 중심 (cx, cy)와 반지름 r을 가진 원 안에 있는지 확인
        fun isInsideCircle(x: Int, y: Int, cx: Int, cy: Int, r: Double): Boolean {
            return squaredDistanceBetween(x, y, cx, cy) <= r * r
        }
    }
}

data class Checkpoint(val position: Point, val id: Int)

// 공통 기능을 가진 기본 포드 클래스
open class BasePod(
    var position: Point,
    velocityPair: Pair<Int, Int>,
    var angle: Int,
    var nextCheckpointId: Int
) {
    // Point 객체 생성을 피하기 위해 위치 좌표를 직접 저장
    var posX: Int = position.x
    var posY: Int = position.y

    // Pair 객체 생성을 피하기 위해 속도 구성 요소를 직접 저장
    var velocityX: Int = velocityPair.first
    var velocityY: Int = velocityPair.second

    // 이전 버전과의 호환성을 위해 속도를 Pair로 유지
    var velocity: Pair<Int, Int> = velocityPair
        get() = Pair(velocityX, velocityY)
        set(value) {
            field = value
            velocityX = value.first
            velocityY = value.second
        }

    // 비교 목적을 위한 제곱 거리 (최적화)
    fun squaredDistanceToCheckpoint(checkpoint: Checkpoint): Double = 
        Point.squaredDistanceBetween(posX, posY, checkpoint.position.x, checkpoint.position.y)

    fun angleToCheckpoint(checkpoint: Checkpoint): Double {
        val targetAngle = Point.angleBetween(posX, posY, checkpoint.position.x, checkpoint.position.y)
        val angleDiff = (targetAngle - angle + 360) % 360
        return if (angleDiff > 180) angleDiff - 360 else angleDiff
    }

    // 위치가 체크포인트 내부에 있는지 확인
    fun isPodInsideCheckpoint(x: Int, y: Int, checkpoint: Checkpoint): Boolean {
        return Point.isInsideCircle(
            x,
            y,
            checkpoint.position.x,
            checkpoint.position.y,
            GameConstants.CHECKPOINT_RADIUS
        )
    }

    // 현재 속도 계산
    fun getCurrentSpeed(): Double = 
        VectorUtils.magnitude(velocityX, velocityY)

    // 이 포드와 다른 포드 사이의 상대 속도 계산
    fun getRelativeSpeed(otherPod: BasePod): Double = 
        VectorUtils.magnitude(
            velocityX - otherPod.velocityX,
            velocityY - otherPod.velocityY
        )

    // 포드가 다음 몇 턴 안에 체크포인트에 들어갈지 예측
    fun isGoingToEnterCheckpointSoon(checkpoints: List<Checkpoint>): Boolean {
        // 유스케이스 계층의 확장 함수 호출
        return this.predictCheckpointEntry(checkpoints)
    }

    open fun update(x: Int, y: Int, vx: Int, vy: Int, angle: Int, nextCheckpointId: Int) {
        // 위치 좌표 직접 업데이트
        posX = x
        posY = y
        // 가능하다면 필드를 수정하여 Point 객체를 제자리에서 업데이트
        // Point는 불변이므로 새로운 객체를 생성해야 하지만, 이는 최적화할 수 있음
        // 향후 리팩토링에서 Point를 가변으로 만들거나 다른 접근 방식을 사용하여 최적화 가능
        position = Point(x, y)
        // 속도 구성 요소 직접 업데이트
        velocityX = vx
        velocityY = vy
        // Update velocity Pair for backward compatibility
        // We don't create a new Pair object here because the getter already does that
        this.angle = angle
        this.nextCheckpointId = nextCheckpointId
    }
}

// Abstract base class for player-controlled pods
abstract class MyPod(
    position: Point,
    velocity: Pair<Int, Int>,
    angle: Int,
    nextCheckpointId: Int,
    var shieldCooldown: Int = 0
) : BasePod(position, velocity, angle, nextCheckpointId) {
    var targetX: Int = 0
    var targetY: Int = 0
    var thrust: Int = 100
    var useShield: Boolean = false
    var shieldActive: Int = 0  // Number of turns the shield is active
    var useBoost: Boolean = false

    // Track checkpoint count for home run detection
    protected var checkpointCount = 0
    protected var lastCheckpointId = 0

    // Collision prediction parameters
    val collisionRadius = GameConstants.POD_SIZE.toInt() // Radius to consider for collision
    val collisionTimeThreshold = 3 // Number of turns to look ahead for collision prediction
    val collisionProbabilityThreshold = 0.7 // Probability threshold to activate shield

    // 상대 속도에 기반한 동적 충돌 반경 계산
    fun calculateDynamicCollisionRadius(relativeSpeed: Double): Int {
        return collisionRadius + (relativeSpeed * 0.5).toInt().coerceAtMost(GameConstants.POD_SIZE.toInt())
    }

    // Calculate dot product between pod's velocity and a direction vector
    protected fun calculateVelocityDirectionDotProduct(directionX: Double, directionY: Double): Double {
        return VectorUtils.dotProduct(
            velocityX.toDouble(), velocityY.toDouble(),
            directionX, directionY
        )
    }

    // Calculate angle between two vectors
    protected fun calculateAngleBetweenVectors(v1x: Int, v1y: Int, v2x: Int, v2y: Int): Double {
        val dotProduct = VectorUtils.dotProduct(v1x, v1y, v2x, v2y)
        val magnitude1 = VectorUtils.magnitude(v1x, v1y)
        val magnitude2 = VectorUtils.magnitude(v2x, v2y)

        // Avoid division by zero
        if (magnitude1 < 0.001 || magnitude2 < 0.001) {
            return 0.0
        }

        val cosAngle = dotProduct / (magnitude1 * magnitude2)
        // Clamp to avoid domain errors with acos
        val clampedCosAngle = cosAngle.coerceIn(-1.0, 1.0)
        return Math.toDegrees(acos(clampedCosAngle))
    }

    // Optimize trajectory using closestPointToLine if conditions are met
    protected fun optimizeTrajectory(checkpoint: Checkpoint, podType: String): Boolean {
        // Calculate position delta and future position
        val deltaX = (velocityX * GameConstants.FRICTION).toInt()
        val deltaY = (velocityY * GameConstants.FRICTION).toInt()

        val distanceTravelledOnPreviousFrame = VectorUtils.magnitude(deltaX, deltaY)

        val futurePositionX = posX + deltaX
        val futurePositionY = posY + deltaY

        // Calculate angle to checkpoint
        val angleDiff = Math.abs(angleToCheckpoint(checkpoint))

        // Calculate distances
        val futureDistanceSquared = Point.squaredDistanceBetween(
            futurePositionX, futurePositionY, 
            checkpoint.position.x, checkpoint.position.y
        )
        val currentDistanceSquared = Point.squaredDistanceBetween(
            posX, posY, 
            checkpoint.position.x, checkpoint.position.y
        )

        // Check if we should optimize trajectory
        if (distanceTravelledOnPreviousFrame > GameConstants.MIN_DISTANCE_FOR_OPTIMIZATION && 
            angleDiff < GameConstants.MAX_ANGLE_FOR_OPTIMIZATION && 
            futureDistanceSquared < currentDistanceSquared) {

            // We need to create Point objects for the closestPointToLine calculation
            // since it's a complex geometric operation
            // Note: This is a performance bottleneck that could be optimized in the future
            // by implementing closestPointToLine to work with raw coordinates instead of Point objects
            val currentPosition = Point(posX, posY)
            val futurePosition = Point(futurePositionX, futurePositionY)
            val closestPoint = Point.closestPointToLine(currentPosition, checkpoint.position, futurePosition)

            // Calculate target point directly without creating an intermediate Point object
            targetX = closestPoint.x + (closestPoint.x - futurePositionX)
            targetY = closestPoint.y + (closestPoint.y - futurePositionY)

            System.err.println("$podType: Optimizing trajectory using closestPointToLine")
            return true
        }

        return false
    }

    // Detect if we're approaching a hairpin turn
    protected fun isHairpinTurn(checkpoints: List<Checkpoint>, checkpointCount: Int): Boolean {
        val currentCheckpoint = checkpoints[nextCheckpointId]
        val nextCheckpointIndex = (nextCheckpointId + 1) % checkpointCount
        val nextCheckpoint = checkpoints[nextCheckpointIndex]

        // Calculate the angle between current checkpoint and next checkpoint
        val vectorToCurrentX = currentCheckpoint.position.x - posX
        val vectorToCurrentY = currentCheckpoint.position.y - posY
        val vectorToNextX = nextCheckpoint.position.x - currentCheckpoint.position.x
        val vectorToNextY = nextCheckpoint.position.y - currentCheckpoint.position.y

        // Calculate the angle between these vectors using our helper function
        val angleBetweenCheckpoints = calculateAngleBetweenVectors(
            vectorToCurrentX, vectorToCurrentY, 
            vectorToNextX, vectorToNextY
        )

        // If the calculation returned 0, it means one of the vectors had near-zero magnitude
        if (angleBetweenCheckpoints == 0.0) {
            return false
        }

        // If the angle is large (> 90 degrees) and we're close to the current checkpoint, it's a hairpin
        val squaredDistance = squaredDistanceToCheckpoint(currentCheckpoint)
        val isCloseToCheckpoint = squaredDistance < 4000000 // 2000^2 = 4000000
        val isLargeAngle = angleBetweenCheckpoints > 90

        if (isLargeAngle && isCloseToCheckpoint) {
            // For logging, get the actual distance
            val distance = sqrt(squaredDistance)
            System.err.println("Detected hairpin turn! Angle between checkpoints: $angleBetweenCheckpoints, Distance: $distance")
            return true
        }

        return false
    }

    override fun update(x: Int, y: Int, vx: Int, vy: Int, angle: Int, nextCheckpointId: Int) {
        // Track checkpoint changes to count laps
        if (this.nextCheckpointId != nextCheckpointId) {
            checkpointCount++
            lastCheckpointId = this.nextCheckpointId
        }

        super.update(x, y, vx, vy, angle, nextCheckpointId)

        // Update shield cooldown
        if (shieldCooldown > 0) {
            shieldCooldown--
        }

        // Update shield active duration
        if (shieldActive > 0) {
            shieldActive--
            useShield = true  // Keep shield active
        }
    }

    // 다른 포드와의 충돌 예측
    protected fun predictCollision(otherPod: BasePod): Pair<Double, Double> {
        // 유스케이스 계층의 확장 함수 호출
        return this.calculateCollisionProbability(otherPod)
    }

    // 충돌 예측에 기반하여 쉴드 활성화 여부 확인
    protected fun shouldActivateShield(opponentPods: List<OpponentPod>): Boolean {
        // 유스케이스 계층의 확장 함수 호출
        return this.evaluateShieldActivation(opponentPods)
    }

    // 서브클래스에서 오버라이드할 수 있는 메서드
    open fun calculateTarget(checkpoints: List<Checkpoint>, checkpointCount: Int) {
        // 기본 구현 - 아무것도 하지 않음
        // 서브클래스는 타겟 계산이 필요한 경우 이 메서드를 오버라이드해야 함
        System.err.println("MyPod: 기본 calculateTarget 구현")
    }

    open fun calculateThrust(checkpoints: List<Checkpoint>, turn: Int, opponentPods: List<OpponentPod>, sharedBoostAvailable: Boolean) {
        // 기본 구현 - 아무것도 하지 않음
        // 서브클래스는 추진력 계산이 필요한 경우 이 메서드를 오버라이드해야 함
        System.err.println("MyPod: 기본 calculateThrust 구현")
    }

    // 관성에 기반하여 체크포인트 앞의 목표 지점 계산
    protected fun calculateInertiaAdjustedTarget(checkpoint: Checkpoint): Pair<Int, Int> {
        // 현재 속도 가져오기
        val currentSpeed = getCurrentSpeed()

        // 충분히 빠르게 움직이는 경우에만 관성 보정 적용
        if (currentSpeed < 100) {
            return Pair(checkpoint.position.x, checkpoint.position.y)
        }

        // 속도에 기반하여 얼마나 앞을 목표로 할지 계산
        // 높은 속도는 더 앞을 목표로 해야 함
        val lookAheadFactor = (currentSpeed / 100.0).coerceAtMost(3.0)

        // VectorUtils를 사용하여 속도의 정규화된 방향 벡터 계산
        val velocityMagnitude = VectorUtils.magnitude(velocityX, velocityY)

        // 정규화된 속도 방향 구성 요소 계산
        // 성능 향상을 위해 Pair 객체 생성 방지
        val velocityDirX: Double
        val velocityDirY: Double

        if (velocityMagnitude > 0.001) {
            velocityDirX = velocityX / velocityMagnitude
            velocityDirY = velocityY / velocityMagnitude
        } else {
            velocityDirX = 0.0
            velocityDirY = 0.0
        }

        // 체크포인트 앞의 목표 지점 계산
        val targetX = (checkpoint.position.x + velocityDirX * lookAheadFactor * GameConstants.CHECKPOINT_RADIUS).toInt()
        val targetY = (checkpoint.position.y + velocityDirY * lookAheadFactor * GameConstants.CHECKPOINT_RADIUS).toInt()

        // 여기서는 Pair를 반환해야 하지만, 계산 중에 임시 Pair를 생성하는 대신
        // 하나의 Pair 객체만 생성
        return Pair(targetX, targetY)
    }

    // 포드의 관성을 보상하기 위해 관성에 기반한 추진력 조정
    protected fun adjustThrustForInertia(baseThrust: Int, targetCheckpoint: Checkpoint): Int {
        // 라디안 단위로 체크포인트까지의 각도 가져오기
        val targetAngleRadians = Point.angleBetween(posX, posY, targetCheckpoint.position.x, targetCheckpoint.position.y) * PI / 180.0

        // 현재 속도 가져오기
        val currentSpeed = getCurrentSpeed()

        // 체크포인트를 향한 방향 벡터 계산
        val directionX = cos(targetAngleRadians)
        val directionY = sin(targetAngleRadians)

        // 올바른 방향으로 움직이고 있는지 확인하기 위한 내적 계산
        val dotProduct = calculateVelocityDirectionDotProduct(directionX, directionY)
        val movingTowardsTarget = dotProduct > 0

        // 관성과 현재 움직임에 기반한 추진력 조정
        return when {
            // 빠르게 움직이고 있고 잘못된 방향으로 가고 있다면, 회전을 위해 추진력 감소
            currentSpeed > 200 && !movingTowardsTarget -> (baseThrust * 0.7).toInt()

            // 빠르게 움직이고 있고 올바른 방향으로 가고 있다면, 추진력 유지
            currentSpeed > 200 && movingTowardsTarget -> baseThrust

            // 천천히 움직이고 있다면, 관성을 극복하기 위해 추진력 증가
            currentSpeed < 100 -> (baseThrust * 1.2).coerceAtMost(100.0).toInt()

            // 기본 케이스
            else -> baseThrust
        }
    }

    // 디버깅을 위한 관성 정보 로깅
    protected fun logInertiaInfo(podType: String) {
        val speed = getCurrentSpeed()

        System.err.println("포드 $podType - " +
                "속도: $speed, " +
                "벡터: (${velocityX}, ${velocityY}), " +
                "추진력: $thrust")
    }

    // 헤어핀 턴 로직을 처리하고 적절한 추진력 반환
    protected fun handleHairpinTurn(angleDiff: Double, podType: String): Int {
        // 급격한 턴에서 각도가 크면 엔진을 끔
        if (angleDiff > GameConstants.HAIRPIN_HIGH_ANGLE) {
            System.err.println("$podType: 헤어핀 턴: 엔진 꺼짐, 회전만. 각도 차이: $angleDiff")
            return GameConstants.HAIRPIN_LOW_THRUST
        } 
        // 충분히 회전했으면 점진적으로 추진력 증가
        else if (angleDiff > GameConstants.HAIRPIN_MID_ANGLE) {
            System.err.println("$podType: 헤어핀 턴: 턴 중 최소 추진력. 각도 차이: $angleDiff")
            return GameConstants.HAIRPIN_MID_THRUST
        }
        // 각도가 충분히 좋으면 정상 추진력 재개
        else {
            System.err.println("$podType: 헤어핀 턴: 추진력 재개. 각도 차이: $angleDiff")
            return GameConstants.HAIRPIN_HIGH_THRUST
        }
    }

    // 각도와 거리에 기반한 기본 추진력 계산
    protected fun calculateBaseThrust(angleDiff: Double, squaredDistance: Double): Int {
        return when {
            angleDiff > 90 -> 0
            angleDiff > 50 -> 50
            squaredDistance < 1000000 -> 70 // 1000^2 = 1000000
            else -> 100
        }
    }

    // Check if the pod is currently inside a checkpoint
    protected fun isInsideCheckpoint(checkpoint: Checkpoint): Boolean {
        return isPodInsideCheckpoint(posX, posY, checkpoint)
    }

    // Check if the pod is at the exact x,y position of the checkpoint and has near-zero velocity
    protected fun isAtExactCheckpointPosition(checkpoint: Checkpoint): Boolean {
        // Define a tolerance for "exact" position (within 20 units)
        val positionTolerance = 20

        // Define a tolerance for "zero" velocity (within 10 units per axis)
        val velocityTolerance = 10

        // Check if position is within tolerance
        val isPositionExact = abs(posX - checkpoint.position.x) <= positionTolerance && 
                              abs(posY - checkpoint.position.y) <= positionTolerance

        // Check if velocity is near zero
        val isVelocityNearZero = abs(velocityX) <= velocityTolerance && 
                                abs(velocityY) <= velocityTolerance

        // Log the exact position check for debugging
        if (isPositionExact) {
            System.err.println("POSITION CHECK: At position (${posX}, ${posY}), target (${checkpoint.position.x}, ${checkpoint.position.y}), velocity (${velocityX}, ${velocityY})")
        }

        // Both position and velocity conditions must be met
        return isPositionExact && isVelocityNearZero
    }

    // Check if the pod is approaching the checkpoint (moving towards it)
    protected fun isApproachingCheckpoint(checkpoint: Checkpoint): Boolean {
        // Calculate direction vector from pod to checkpoint
        val directionX = checkpoint.position.x - posX
        val directionY = checkpoint.position.y - posY

        // Get normalized direction vector using VectorUtils
        val (normalizedDirX, normalizedDirY) = VectorUtils.normalize(directionX.toDouble(), directionY.toDouble())

        // Calculate dot product with velocity and check if it's positive
        return calculateVelocityDirectionDotProduct(normalizedDirX, normalizedDirY) > 0
    }

    fun getCommand(): String {
        return when {
            useShield -> "$targetX $targetY SHIELD SHIELD"
            useBoost -> "$targetX $targetY BOOST BOOST"
            else -> "$targetX $targetY $thrust Thrust:$thrust"
        }
    }

    fun activateShield() {
        if (useShield && shieldActive == 0) {  // Only activate if not already active
            shieldActive = 3  // Shield will be active for 3 turns
            shieldCooldown = 3  // Cooldown after shield expires
        }
    }
}

// RacerPod class for racing strategy
class RacerPod(
    position: Point,
    velocity: Pair<Int, Int>,
    angle: Int,
    nextCheckpointId: Int,
    shieldCooldown: Int = 0
) : MyPod(position, velocity, angle, nextCheckpointId, shieldCooldown) {

    override fun calculateTarget(checkpoints: List<Checkpoint>, checkpointCount: Int) {
        val currentCheckpoint = checkpoints[nextCheckpointId]
        val nextCheckpointIndex = (nextCheckpointId + 1) % checkpointCount
        val nextCheckpoint = checkpoints[nextCheckpointIndex]

        // Get checkpoint coordinates directly
        val currentX = currentCheckpoint.position.x
        val currentY = currentCheckpoint.position.y

        // Check if we're going for the home run (final lap, final checkpoint)
        val isGoingForHomeRun = this.checkpointCount == (checkpointCount * 3)

        if (isGoingForHomeRun) {
            System.err.println("RacerPod: Going for the home run!")
            targetX = currentX
            targetY = currentY
            return
        }

        // If we're about to enter the checkpoint, aim for the next one
        if (isGoingToEnterCheckpointSoon(checkpoints)) {
            val nextCheckpointX = nextCheckpoint.position.x
            val nextCheckpointY = nextCheckpoint.position.y

            targetX = nextCheckpointX
            targetY = nextCheckpointY

            val angleDiff = Math.abs(angleToCheckpoint(nextCheckpoint))
            System.err.println("RacerPod: Drifting towards next checkpoint, angle diff: $angleDiff")
            return
        }

        // Try to optimize trajectory using the shared method
        if (optimizeTrajectory(currentCheckpoint, "RacerPod")) {
            return
        }

        // Calculate angle for later use
        val angleDiff = Math.abs(angleToCheckpoint(currentCheckpoint))

        // Default behavior - use inertia correction to aim ahead of the checkpoint
        val squaredDistance = squaredDistanceToCheckpoint(currentCheckpoint)
        if (squaredDistance < 1440000) { // 1200^2 = 1440000
            // If close to checkpoint, aim for the next one with inertia correction
            val nextCheckpointX = nextCheckpoint.position.x
            val nextCheckpointY = nextCheckpoint.position.y

            // Calculate vector directly with some bias towards the next checkpoint
            targetX = (currentX + (nextCheckpointX - currentX) * 0.3).toInt()
            targetY = (currentY + (nextCheckpointY - currentY) * 0.3).toInt()
        } else {
            // Apply inertia correction - aim ahead of the checkpoint based on our velocity
            val (adjustedX, adjustedY) = calculateInertiaAdjustedTarget(currentCheckpoint)
            targetX = adjustedX
            targetY = adjustedY
            System.err.println("RacerPod: Using inertia correction, aiming at ($targetX, $targetY) instead of (${currentCheckpoint.position.x}, ${currentCheckpoint.position.y})")
        }
    }

    // Using isHairpinTurn from parent class

    // Combined strategy method that handles both targeting and thrust calculation
    fun calculateRacerStrategy(checkpoints: List<Checkpoint>, checkpointCount: Int, turn: Int, opponentPods: List<OpponentPod>, sharedBoostAvailable: Boolean) {
        // First calculate the target
        calculateTarget(checkpoints, checkpointCount)

        // Then calculate the thrust
        calculateThrust(checkpoints, turn, opponentPods, sharedBoostAvailable)
    }

    override fun calculateThrust(checkpoints: List<Checkpoint>, turn: Int, opponentPods: List<OpponentPod>, sharedBoostAvailable: Boolean) {
        val currentCheckpoint = checkpoints[nextCheckpointId]
        val squaredDistance = squaredDistanceToCheckpoint(currentCheckpoint)
        val angleDiff = Math.abs(angleToCheckpoint(currentCheckpoint))

        // Reset flags
        if (shieldActive == 0) {  // Only reset if shield is not active
            useShield = false
        }
        useBoost = false

        // Check if we're in a hairpin turn
        val isHairpin = isHairpinTurn(checkpoints, checkpoints.size)

        // Cornering sequence for hairpin turns
        if (isHairpin) {
            thrust = handleHairpinTurn(angleDiff, "RACER")
        } 
        // Normal thrust calculation for non-hairpin turns
        else {
            // Determine base thrust based on angle and distance
            val baseThrust = calculateBaseThrust(angleDiff, squaredDistance)

            // Adjust thrust based on inertia
            thrust = adjustThrustForInertia(baseThrust, currentCheckpoint)
        }

        // Decide whether to use boost - never use boost in hairpin turns
        if (sharedBoostAvailable && 
            angleDiff < 10 && 
            squaredDistance > 25000000 && // 5000^2 = 25000000
            turn > 3 &&
            !isHairpin) {
            useBoost = true
        }

        // Decide whether to use shield - use collision prediction
        useShield = shouldActivateShield(opponentPods)

        // Log inertia information for debugging
        logInertiaInfo("RACER")
    }
}

// BlockerPod class for blocking/intercepting strategy
class BlockerPod(
    position: Point,
    velocity: Pair<Int, Int>,
    angle: Int,
    nextCheckpointId: Int,
    shieldCooldown: Int = 0
) : MyPod(position, velocity, angle, nextCheckpointId, shieldCooldown) {

    // Using isHairpinTurn from parent class


    // BlockerPod uses the default implementations of calculateTarget and calculateThrust from MyPod
    // The actual blocking logic is implemented in calculateBlockerStrategy

    fun calculateBlockerStrategy(leadingOpponent: OpponentPod, racerPod: RacerPod) {
        // Reset flags
        if (shieldActive == 0) {  // Only reset if shield is not active
            useShield = false
        }
        useBoost = false


        // First rule: Check if we're going to interfere with our racer pod
        val (collisionTimeWithRacer, probabilityWithRacer) = predictCollision(racerPod)
        val distanceToRacerSquared = Point.squaredDistanceBetween(posX, posY, racerPod.posX, racerPod.posY)

        // If we're likely to collide with our racer pod, avoid it
        if (collisionTimeWithRacer < 3 && probabilityWithRacer > 0.5 && distanceToRacerSquared < 4000000) { // 2000^2 = 4000000
            System.err.println("BLOCKER: Avoiding collision with racer pod! Time=$collisionTimeWithRacer, Prob=$probabilityWithRacer")

            // Calculate a target that avoids the racer pod
            // Move perpendicular to the line connecting the two pods
            val dirX = racerPod.posX - posX
            val dirY = racerPod.posY - posY
            val length = sqrt(dirX.toDouble().pow(2) + dirY.toDouble().pow(2))

            if (length > 0) {
                // Calculate perpendicular vector (rotate 90 degrees)
                val perpX = -dirY / length
                val perpY = dirX / length

                // Set target away from racer pod
                targetX = posX + (perpX * 1000).toInt()
                targetY = posY + (perpY * 1000).toInt()
                thrust = 100
                return
            }
        }

        // Second rule: Normal blocking behavior - aim to intercept the leading opponent
        // Use inertia correction to predict where the opponent will be
        val opponentSpeed = leadingOpponent.getCurrentSpeed()
        val lookAheadFactor = (opponentSpeed / 100.0).coerceAtMost(2.0)

        targetX = leadingOpponent.posX + (leadingOpponent.velocity.first * lookAheadFactor).toInt()
        targetY = leadingOpponent.posY + (leadingOpponent.velocity.second * lookAheadFactor).toInt()
        thrust = 100

        // For blocker pod, check for imminent collision with the target opponent
        // and activate shield preemptively if we're on an intercept course
        val distanceToOpponentSquared = Point.squaredDistanceBetween(posX, posY, leadingOpponent.posX, leadingOpponent.posY)

        // If we're close to the opponent and shield is available
        if (shieldCooldown == 0 && distanceToOpponentSquared < 1440000) { // 1200^2 = 1440000
            // Calculate time to collision more precisely for blocking
            val (collisionTime, probability) = predictCollision(leadingOpponent)

            // If collision is very likely and we're moving fast toward the opponent
            if (collisionTime < 2 && probability > 0.5) {
                val relativeSpeed = getRelativeSpeed(leadingOpponent)

                // Be more aggressive with shield usage when actively blocking
                if (relativeSpeed > 200) {
                    useShield = true
                    System.err.println("BLOCKER: Shield activated for interception! Collision in $collisionTime turns, speed: $relativeSpeed")
                }
            }
        }

        // Log information for debugging
        logInertiaInfo("BLOCKER")
    }
}

// OpponentPod class for tracking opponent pods
class OpponentPod(
    position: Point,
    velocity: Pair<Int, Int>,
    angle: Int,
    nextCheckpointId: Int,
    var laps: Int = 0,
    var previousCheckpointId: Int = 0
) : BasePod(position, velocity, angle, nextCheckpointId) {

    override fun update(x: Int, y: Int, vx: Int, vy: Int, angle: Int, nextCheckpointId: Int) {
        // Track lap completion - if we've gone from last checkpoint to first checkpoint
        if (this.nextCheckpointId != nextCheckpointId && this.nextCheckpointId > nextCheckpointId) {
            laps++
            System.err.println("Opponent completed a lap! Now on lap: $laps")
        }

        previousCheckpointId = this.nextCheckpointId
        super.update(x, y, vx, vy, angle, nextCheckpointId)
    }
}

//================================================================
// 2. 유스케이스 계층 (Use Case Layer)
//================================================================
// 이 계층은 애플리케이션 특화 비즈니스 규칙을 포함합니다.
// 엔티티 계층의 객체들을 조작하고 비즈니스 로직을 구현합니다.

/**
 * 포드 움직임 예측 및 충돌 감지 기능
 */
// BasePod 클래스에 확장 메서드로 추가
fun BasePod.predictCheckpointEntry(checkpoints: List<Checkpoint>): Boolean {
    val currentCheckpoint = checkpoints[nextCheckpointId]
    var velocityX = this.velocityX
    var velocityY = this.velocityY
    var approxPositionX = posX
    var approxPositionY = posY

    // 다음 6턴 동안의 움직임 시뮬레이션
    repeat(6) {
        // 속도에 마찰 적용
        velocityX = (velocityX * GameConstants.FRICTION).toInt()
        velocityY = (velocityY * GameConstants.FRICTION).toInt()

        // 속도를 기반으로 위치 업데이트
        approxPositionX += velocityX
        approxPositionY += velocityY

        // 포드가 체크포인트 내부에 있는지 확인
        if (isPodInsideCheckpoint(approxPositionX, approxPositionY, currentCheckpoint)) {
            return true
        }
    }

    return false
}

// MyPod 클래스에 확장 메서드로 추가
fun MyPod.calculateCollisionProbability(otherPod: BasePod): Pair<Double, Double> {
    // 상대적 위치와 속도 계산
    val relativeX = otherPod.posX - posX
    val relativeY = otherPod.posY - posY
    val relativeVX = otherPod.velocityX - velocityX
    val relativeVY = otherPod.velocityY - velocityY

    // VectorUtils를 사용하여 상대 속도 계산
    val relativeSpeed = getRelativeSpeed(otherPod)

    // 상대 속도에 기반한 동적 충돌 반경 설정
    // 높은 속도는 턴 사이의 움직임을 고려하기 위해 더 큰 충돌 반경 필요
    val dynamicCollisionRadius = calculateDynamicCollisionRadius(relativeSpeed)

    // 충돌 시간에 대한 이차 방정식 계수 계산
    // ||p + vt|| = r, 여기서 p는 상대 위치, v는 상대 속도, r은 충돌 반경

    // 계수 a = |v|²
    val a = VectorUtils.magnitude(relativeVX, relativeVY).pow(2)

    // 계수 b = 2(p·v)
    val b = 2.0 * VectorUtils.dotProduct(
        relativeX.toDouble(), relativeY.toDouble(),
        relativeVX.toDouble(), relativeVY.toDouble()
    )

    // 계수 c = |p|² - r²
    val c = VectorUtils.squaredDistance(0, 0, relativeX, relativeY) - 
            dynamicCollisionRadius.toDouble() * dynamicCollisionRadius.toDouble()

    // 포드가 서로에 대해 상대적으로 움직이지 않는 경우
    if (a < 0.0001) {
        // 이미 충돌 중인 경우
        if (c <= 0) {
            return Pair(0.0, 1.0) // 100% 확률로 즉시 충돌
        }
        return Pair(Double.MAX_VALUE, 0.0) // 충돌 없음
    }

    // 판별식 계산
    val discriminant = b * b - 4 * a * c

    // 실수 해가 없으면 충돌 없음
    if (discriminant < 0) {
        return Pair(Double.MAX_VALUE, 0.0)
    }

    // 충돌 시간 계산
    val t1 = (-b - sqrt(discriminant)) / (2 * a)
    val t2 = (-b + sqrt(discriminant)) / (2 * a)

    // 가장 빠른 양수 충돌 시간 찾기
    val collisionTime = when {
        t1 > 0 -> t1
        t2 > 0 -> t2
        else -> Double.MAX_VALUE // 미래 충돌 없음
    }

    // 시간에 기반한 충돌 확률 계산
    val probability = if (collisionTime < Double.MAX_VALUE) {
        // 가까운 충돌은 높은 확률, 먼 충돌은 낮은 확률
        (1.0 - (collisionTime / collisionTimeThreshold).coerceAtMost(1.0))
    } else {
        0.0
    }

    return Pair(collisionTime, probability)
}

// MyPod 클래스에 확장 메서드로 추가
fun MyPod.evaluateShieldActivation(opponentPods: List<OpponentPod>): Boolean {
    // 쉴드가 쿨다운 중이면 사용할 수 없음
    if (shieldCooldown > 0) {
        return false
    }

    // 각 상대 포드와의 충돌 확인
    for (opponentPod in opponentPods) {
        val (collisionTime, probability) = calculateCollisionProbability(opponentPod)

        // 디버깅을 위한 충돌 예측 데이터 로깅
        if (collisionTime < 5) {  // 충돌이 5턴 이내인 경우에만 로깅
            val squaredDistance = Point.squaredDistanceBetween(posX, posY, opponentPod.posX, opponentPod.posY)
            val distance = sqrt(squaredDistance)  // 로깅을 위해서만 실제 거리 계산
            val relativeSpeed = getRelativeSpeed(opponentPod)
            val dynamicRadius = calculateDynamicCollisionRadius(relativeSpeed)
            System.err.println("충돌 예측: 시간=$collisionTime, 확률=$probability, 거리=$distance, 상대속도=$relativeSpeed, 반경=$dynamicRadius")
        }

        // 충돌이 임박하고 확률이 높은 경우
        if (collisionTime < collisionTimeThreshold && probability > collisionProbabilityThreshold) {
            // 충격력을 결정하기 위한 상대 속도 확인
            val relativeSpeed = getRelativeSpeed(opponentPod)

            // 고충격 충돌에 대해서만 쉴드 활성화
            if (relativeSpeed > 300) {
                System.err.println("쉴드 활성화! $collisionTime 턴 내 충돌 예측, 확률: $probability, 속도: $relativeSpeed")
                return true
            }
        }
    }

    return false
}

/**
 * 가장 진행이 많이 된 상대 포드의 인덱스를 찾습니다.
 * 진행 상황은 완료된 랩 수와 체크포인트 ID로 결정됩니다.
 */
fun findLeadingOpponentIndex(opponentPods: List<OpponentPod>): Int {
    return if (opponentPods[0].laps > opponentPods[1].laps || 
              (opponentPods[0].laps == opponentPods[1].laps && 
               opponentPods[0].nextCheckpointId > opponentPods[1].nextCheckpointId)) 0 else 1
}

//================================================================
// 3. 인터페이스 어댑터 계층 (Interface Adapters Layer)
//================================================================
// 이 계층은 외부 시스템과의 통신을 변환하는 역할을 합니다.
// 유스케이스와 엔티티를 외부 프레임워크와 연결합니다.

//================================================================
// 4. 프레임워크 및 드라이버 계층 (Frameworks & Drivers Layer)
//================================================================
// 이 계층은 외부 프레임워크와의 통합을 담당합니다.
// 입출력, 데이터베이스, 웹 등의 외부 시스템과 상호작용합니다.

/**
 * 게임 상태를 초기화하고 게임 루프를 실행하는 메인 함수입니다.
 * 게임 루프:
 * 1. 입력을 기반으로 포드 위치 업데이트
 * 2. 선두 상대 포드 찾기
 * 3. 레이서 및 블로커 포드에 대한 전략 계산
 * 4. 두 포드에 대한 명령 출력
 * 5. 쉴드 상태 업데이트
 */
fun main() {
    val input = Scanner(System.`in`)
    val laps = input.nextInt()
    val checkpointCount = input.nextInt()

    // Store checkpoints - create Point objects only once during initialization
    val checkpoints = mutableListOf<Checkpoint>()
    for (i in 0 until checkpointCount) {
        val checkpointX = input.nextInt()
        val checkpointY = input.nextInt()
        // Point objects for checkpoints are created only once at the beginning
        checkpoints.add(Checkpoint(Point(checkpointX, checkpointY), i))
    }

    System.err.println("Race initialized: $laps laps, $checkpointCount checkpoints")

    // Initialize pods with default values - create Point objects only once
    // These Point objects will be reused and updated in-place
    val initialPoint = Point(0, 0)
    val initialVelocity = Pair(0, 0)

    val myPods = mutableListOf<MyPod>(
        RacerPod(initialPoint, initialVelocity, 0, 0, 0),
        BlockerPod(initialPoint, initialVelocity, 0, 0, 0)
    )

    val opponentPods = mutableListOf<OpponentPod>(
        OpponentPod(initialPoint, initialVelocity, 0, 0, 0, 0),
        OpponentPod(initialPoint, initialVelocity, 0, 0, 0, 0)
    )

    // Shared boost between pods
    var boostAvailable = true
    var turn = 0


    // game loop
    while (true) {
        turn++

        // Update my pods
        for (i in 0 until 2) {
            val podPositionX = input.nextInt()
            val podPositionY = input.nextInt()
            val podVelocityX = input.nextInt()
            val podVelocityY = input.nextInt()
            val podAngle = input.nextInt()
            val podNextCheckpointId = input.nextInt()

            myPods[i].update(podPositionX, podPositionY, podVelocityX, podVelocityY, podAngle, podNextCheckpointId)
        }

        // Update opponent pods
        for (i in 0 until 2) {
            val opponentPositionX = input.nextInt()
            val opponentPositionY = input.nextInt()
            val opponentVelocityX = input.nextInt()
            val opponentVelocityY = input.nextInt()
            val opponentAngle = input.nextInt()
            val opponentNextCheckpointId = input.nextInt()

            opponentPods[i].update(opponentPositionX, opponentPositionY, opponentVelocityX, opponentVelocityY, opponentAngle, opponentNextCheckpointId)
        }

        // Find the opponent pod with highest progress
        val leadingOpponentIndex = findLeadingOpponentIndex(opponentPods)

        val leadingOpponent = opponentPods[leadingOpponentIndex]
        val opponentProgress = "Lap: ${leadingOpponent.laps}, CP: ${leadingOpponent.nextCheckpointId}"
        System.err.println("Targeting opponent $leadingOpponentIndex: CP $opponentProgress")

        // Strategy for first pod (Racer)
        val racerPod = myPods[0] as RacerPod


        // Calculate strategy for racer pod
        racerPod.calculateRacerStrategy(checkpoints, checkpointCount, turn, opponentPods, boostAvailable)

        // If boost is used, update shared boost availability
        if (racerPod.useBoost) {
            boostAvailable = false
        }

        // Strategy for second pod (Blocker/Interceptor)
        val blockerPod = myPods[1] as BlockerPod

        // BlockerPod only focuses on blocking opponents, not checkpoint racing
        blockerPod.calculateBlockerStrategy(leadingOpponent, racerPod)


        // Output commands for both pods
        println(racerPod.getCommand())
        println(blockerPod.getCommand())

        // Update shield cooldown if shield was used
        racerPod.activateShield()
        blockerPod.activateShield()
    }
}
