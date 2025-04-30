import java.util.*
import kotlin.math.*

// 게임 상수들을 더 체계적으로 그룹화
object GameConstants {
    // 물리적 상수
    object Physics {
        const val FRICTION = 0.85
        const val POD_SIZE = 400
        const val CHECKPOINT_RADIUS = 600
        const val COLLISION_IMPULSE = 120
        const val MAX_TURN_ANGLE = 18.0  // 최대 회전 각도
        const val THRUST_FACTOR = 1.0    // 추력 계수
    }
    
    // 실드 관련
    object Shield {
        const val COOLDOWN = 3
        const val DISTANCE = 850
        const val PREDICTION_DISTANCE = 1500  // 예측 계산용 거리 (더 멀리서 판단)
        const val COLLISION_PREDICTION_TURNS = 2  // 몇 턴 앞에서 충돌 예측할지
        const val MASS_MULTIPLIER = 10
        const val AGGRESSION_FACTOR = 1.2  // 인터셉터의 공격성 증가 계수
    }

    // 체크포인트 관련
    object Checkpoint {
        const val APPROACH_DISTANCE = 1500
        const val LOOKAHEAD_RATIO = 3
        const val APPROACH_ANGLE = 20.0
        const val PREDICTION_TURNS = 6  // 체크포인트 도달 예측용 턴 수
        const val DRIFT_COMPENSATION = 0.8  // 드리프트 상쇄 계수
        const val MIN_ANGLE_FOR_DRIFT = 30.0  // 드리프트가 발생하는 최소 각도
    }

    // 부스트 관련
    object Boost {
        const val MIN_DISTANCE = 4000
        const val MAX_ANGLE_DIFF = 10
        const val INTERCEPT_DISTANCE = 3000
        const val SAVE_THRESHOLD = 0.4
        const val POWER = 650
        const val MIN_CHECKPOINT_DISTANCE = 4000  // 체크포인트 간 최소 거리
        const val STRATEGIC_SAVE_RATIO = 0.6  // 전략적으로 부스트를 남겨둘 레이스 진행 비율
    }
    
    // 적응형 시뮬레이션 관련
    object Simulation {
        const val MAX_DEPTH = 6  // 최대 시뮬레이션 깊이
        const val TIME_WEIGHT = 0.7  // 시간 가중치
        const val ANGLE_WEIGHT = 0.3  // 각도 가중치
    }

    // 전략적 분석 관련
    object Strategy {
        const val PATH_OPTIMIZATION_FACTOR = 0.15  // 경로 최적화 계수
        const val EARLY_RACE_AGGRESSION = 1.2  // 초반 레이스 공격성
        const val LATE_RACE_PRECISION = 1.5  // 후반 레이스 정밀도
    }
    
    // 각도 관련
    object Angle {
        const val THRESHOLD_STOP = 90
        const val THRESHOLD_SLOW = 45
        const val MAX_ROTATION = 18  // 한 턴에 최대 회전 가능 각도
    }

    // 추진력 관련
    object Thrust {
        const val SLOW = 50
        const val MAX = 100
    }

    // 인터셉트 관련
    object Intercept {
        const val CLOSE_RATIO = 0.3
        const val MEDIUM_RATIO = 0.4
        const val FAR_RATIO = 0.5
        const val CLOSE_THRESHOLD = 1000
        const val MEDIUM_THRESHOLD = 2500
        const val COLLISION_THRESHOLD = 850
        const val SWITCH_TARGET_THRESHOLD = 0.15
        const val OPTIMAL_INTERCEPT_MIN_DISTANCE = 1500  // 부스트에 적합한 최소 거리
        const val OPTIMAL_INTERCEPT_MAX_DISTANCE = 3000  // 부스트에 적합한 최대 거리
    }

    // 충돌 관련
    object Collision {
        const val BENEFIT_THRESHOLD = 10.0  // 충돌 이득 판단 기준값
    }
    
    // 맵 관련
    const val MAX_MAP_DISTANCE = 16000.0
    const val MAX_MAP_WIDTH = 16000
    const val MAX_MAP_HEIGHT = 9000
}

// 벡터 연산 최적화 및 메서드 추가
data class Vector2D(val x: Int, val y: Int) {
    private var magnitudeCache: Double? = null

    constructor(x: Double, y: Double) : this(x.toInt(), y.toInt())

    fun distanceSquaredTo(other: Vector2D): Double =
        (x - other.x).toDouble().pow(2) + (y - other.y).toDouble().pow(2)

    fun distanceTo(other: Vector2D): Double = sqrt(distanceSquaredTo(other))

    operator fun plus(other: Vector2D): Vector2D = Vector2D(x + other.x, y + other.y)
    operator fun minus(other: Vector2D): Vector2D = Vector2D(x - other.x, y - other.y)
    operator fun div(n: Int): Vector2D = Vector2D(x / n, y / n)
    operator fun div(d: Double): Vector2D = Vector2D(x / d, y / d)
    operator fun times(d: Double): Vector2D = Vector2D((x * d).toInt(), (y * d).toInt())
    operator fun times(i: Int): Vector2D = Vector2D(x * i, y * i)

    fun dotProduct(other: Vector2D): Double {
        val magnitude = other.magnitude()
        return if (magnitude > 0.001) {
            (x * other.x + y * other.y) / magnitude
        } else {
            x * other.x + y * other.y.toDouble()
        }
    }

    fun isInsideCircle(centerX: Int, centerY: Int, radius: Double): Boolean {
        return hypot((x - centerX).toDouble(), (y - centerY).toDouble()) <= radius
    }

    fun magnitude(): Double {
        return magnitudeCache ?: hypot(x.toDouble(), y.toDouble()).also { magnitudeCache = it }
    }

    fun normalized(): Vector2D {
        val mag = magnitude()
        return if (mag > 0) this * (1.0 / mag) else ZERO
    }

    fun angleBetween(other: Vector2D): Double {
        val dot = x * other.x + y * other.y
        val det = x * other.y - y * other.x
        return atan2(det.toDouble(), dot.toDouble()) * 180 / PI
    }

    fun angle(): Double = atan2(y.toDouble(), x.toDouble())
    fun angleDegrees(): Double = angle() * 180 / PI

    fun rotate(angleRad: Double): Vector2D {
        val cos = cos(angleRad)
        val sin = sin(angleRad)
        return Vector2D(
            (x * cos - y * sin).toInt(),
            (x * sin + y * cos).toInt()
        )
    }

    fun rotateDegrees(angleDeg: Double): Vector2D =
        rotate(angleDeg * PI / 180)

    fun withMagnitude(length: Double): Vector2D {
        val normalized = normalized()
        return Vector2D(
            (normalized.x * length).toInt(),
            (normalized.y * length).toInt()
        )
    }

    fun truncate(maxLength: Double): Vector2D {
        val mag = magnitude()
        return if (mag > maxLength) {
            withMagnitude(maxLength)
        } else {
            this
        }
    }

    fun projectOnto(other: Vector2D): Vector2D {
        val otherNormalized = other.normalized()
        val dotProduct = this.x * otherNormalized.x + this.y * otherNormalized.y
        return Vector2D(
            (otherNormalized.x * dotProduct).toInt(),
            (otherNormalized.y * dotProduct).toInt()
        )
    }

    fun angleTo(target: Vector2D): Double {
        val dx = target.x - x
        val dy = target.y - y
        var angle = atan2(dy.toDouble(), dx.toDouble()) * 180 / PI
        if (angle < 0) angle += 360
        return angle
    }

    companion object {
        val ZERO = Vector2D(0, 0)

        fun lerp(start: Vector2D, end: Vector2D, t: Double): Vector2D {
            val tClamped = t.coerceIn(0.0, 1.0)
            return Vector2D(
                start.x + ((end.x - start.x) * tClamped).toInt(),
                start.y + ((end.y - start.y) * tClamped).toInt()
            )
        }

        fun fromAngleAndMagnitude(angleDegrees: Double, magnitude: Double): Vector2D {
            val angleRadians = angleDegrees * PI / 180
            return Vector2D(
                (cos(angleRadians) * magnitude).toInt(),
                (sin(angleRadians) * magnitude).toInt()
            )
        }
    }
}

// Pod 클래스에 확장 기능 추가
data class Pod(
    val position: Vector2D,
    val velocity: Vector2D,
    val angle: Int,
    val nextCheckpointId: Int,
    var currentLap: Int = 1
) {
    private val angleCache = mutableMapOf<Vector2D, Double>()
    private val angleDiffCache = mutableMapOf<Double, Double>()
    private val speedCache: Double by lazy { velocity.magnitude() }

    fun angleTo(target: Vector2D): Double {
        return angleCache.getOrPut(target) {
            val dx = target.x - position.x
            val dy = target.y - position.y
            var angle = atan2(dy.toDouble(), dx.toDouble()) * 180 / PI
            if (angle < 0) angle += 360
            angle
        }
    }

    fun angleDiff(targetAngle: Double): Double {
        return angleDiffCache.getOrPut(targetAngle) {
            var diff = targetAngle - angle
            while (diff > 180) diff -= 360
            while (diff < -180) diff += 360
            diff
        }
    }

    fun calculateThrust(target: Vector2D): Int {
        val angleToTarget = angleTo(target)
        val angleDiff = angleDiff(angleToTarget)
        val absDiff = abs(angleDiff)
        return when {
            absDiff > GameConstants.Angle.THRESHOLD_STOP -> 0
            absDiff > GameConstants.Angle.THRESHOLD_SLOW -> GameConstants.Thrust.SLOW
            else -> GameConstants.Thrust.MAX
        }
    }

    fun calculateRaceProgress(checkpoints: List<Checkpoint>, totalLaps: Int = 1): Double {
        if (checkpoints.isEmpty()) {
            return nextCheckpointId.toDouble()
        }
        val targetCP = checkpoints[nextCheckpointId]
        val distanceToNextCP = position.distanceTo(targetCP.position)
        val checkpointRadius = GameConstants.Physics.CHECKPOINT_RADIUS.toDouble()
        val normalizedDistance = (1.0 - (distanceToNextCP - checkpointRadius) /
                (GameConstants.MAX_MAP_DISTANCE - checkpointRadius)).coerceIn(0.0, 1.0)
        val totalCheckpoints = checkpoints.size * totalLaps
        val completedCheckpoints = (currentLap - 1) * checkpoints.size + nextCheckpointId
        return (completedCheckpoints.toDouble() + normalizedDistance) / totalCheckpoints
    }

    fun speed(): Double = velocity.magnitude()

    // 회전 관성을 고려한 예상 각도 계산
    fun predictedAngle(target: Any, steps: Int = 1): Double {
        var currentAngle = angle.toDouble()
        val targetAngle = when(target) {
            is Vector2D -> angleTo(target)
            is Double -> target
            else -> throw IllegalArgumentException("Target must be Vector2D or Double")
        }
        
        repeat(steps) {
            val angleDiffValue = angleDiff(targetAngle)
            val maxRotation = GameConstants.Physics.MAX_TURN_ANGLE
            val rotationAmount = angleDiffValue.coerceIn(-maxRotation, maxRotation)
            currentAngle += rotationAmount
        }
        return currentAngle
    }

    // 다중 스텝 위치 예측 - 회전 관성 고려
    fun predictPosition(targetAngle: Double, thrust: Int, steps: Int): Vector2D {
        var simulatedPos = position
        var simulatedVel = velocity
        var simulatedAngle = angle.toDouble()
        
        for (step in 1..steps) {
            // 각도 업데이트 - 관성 고려
            val angleDiff = angleDiff(targetAngle)
            val maxRotation = GameConstants.Physics.MAX_TURN_ANGLE
            val rotationAmount = angleDiff.coerceIn(-maxRotation, maxRotation)
            simulatedAngle += rotationAmount
            
            // 새 속도 계산
            val angleRad = simulatedAngle * Math.PI / 180
            val accelerationX = cos(angleRad) * thrust
            val accelerationY = sin(angleRad) * thrust
            
            // 가속도 적용
            simulatedVel = Vector2D(
                (simulatedVel.x + accelerationX).toInt(),
                (simulatedVel.y + accelerationY).toInt()
            )
            
            // 마찰 적용
            simulatedVel = Vector2D(
                (simulatedVel.x * GameConstants.Physics.FRICTION).toInt(),
                (simulatedVel.y * GameConstants.Physics.FRICTION).toInt()
            )
            
            // 위치 업데이트
            simulatedPos += simulatedVel
        }
        
        return simulatedPos
    }

    // 드리프트 보정 타겟 계산
    fun calculateDriftCompensationTarget(target: Vector2D): Vector2D {
        val targetAngle = angleTo(target)
        val angleDifference = angleDiff(targetAngle)
        
        // 각도 차이가 클 때만 드리프트 보정 적용
        if (abs(angleDifference) > GameConstants.Checkpoint.MIN_ANGLE_FOR_DRIFT) {
            // 현재 속도의 크기
            val speedMagnitude = speed()
            
            // 드리프트 계수 (속도가 빠를수록 더 많이 보정)
            val driftFactor = (speedMagnitude / 100.0) * GameConstants.Checkpoint.DRIFT_COMPENSATION
            
            // 현재 진행 방향
            val headingRad = angle * PI / 180
            val headingVector = Vector2D(cos(headingRad).toInt(), sin(headingRad).toInt())
            
            // 속도 방향 (정규화)
            val velocityNorm = if (velocity.magnitude() > 0) velocity.normalized() else headingVector
            
            // 속도 벡터를 사용하여 드리프트 보정
            val compensationX = position.x + (velocityNorm.x * speedMagnitude * driftFactor).toInt()
            val compensationY = position.y + (velocityNorm.y * speedMagnitude * driftFactor).toInt()
            val compensationPoint = Vector2D(compensationX, compensationY)
            
            // 원래 타겟과 보정 포인트 사이의 중간점 계산
            return Vector2D(
                (target.x + (compensationPoint.x - target.x) * 0.3).toInt(),
                (target.y + (compensationPoint.y - target.y) * 0.3).toInt()
            )
        }
        
        return target
    }

    companion object {
        fun fromInput(input: Scanner): Pod {
            val x = input.nextInt()
            val y = input.nextInt()
            val vx = input.nextInt()
            val vy = input.nextInt()
            val angle = input.nextInt()
            val nextCheckpointId = input.nextInt()
            return Pod(Vector2D(x, y), Vector2D(vx, vy), angle, nextCheckpointId)
        }
    }
}

// 체크포인트 데이터 클래스
data class Checkpoint(val position: Vector2D, val index: Int)

// 명령 계층 구조 - 불변 객체로 구현
sealed class Command {
    abstract val target: Vector2D
    abstract val action: String
    
    data class Thrust(override val target: Vector2D, val power: Int) : Command() {
        override val action: String get() = power.toString()
        override fun toString(): String = super.toString()
    }
    
    data class Shield(override val target: Vector2D) : Command() {
        override val action: String get() = "SHIELD"
        override fun toString(): String = super.toString()
    }
    
    data class Boost(override val target: Vector2D) : Command() {
        override val action: String get() = "BOOST"
        override fun toString(): String = super.toString()
    }
    
    // 명령 출력 형식 표준화
    override fun toString(): String {
        return "${target.x} ${target.y} $action"
    }
}

// 전략 인터페이스
interface PodStrategy {
    fun execute(
        pod: Pod,
        checkpoints: List<Checkpoint>,
        opponentPods: List<Pod>,
        numberOfCheckpoints: Int,
        boostAvailable: Boolean,
        shieldCooldown: Int,
        raceProgress: Double
    ): Pair<Command, Boolean>
}

// 레이서 전략 최적화
class RacerStrategy : PodStrategy {
    // PID 컨트롤러 인스턴스 추가 - 체크포인트 접근 속도 제어용
    private val speedController = PIDController(
        kP = 0.05,  // 비례 게인
        kI = 0.001, // 적분 게인
        kD = 0.3,   // 미분 게인
        minOutput = 0.0,
        maxOutput = GameConstants.Thrust.MAX.toDouble()
    )
    
    private var lastCheckpointId = -1
    private val shieldController = ShieldController()
    
    // 경로 최적화 - 체크포인트 사이의 이상적인 경로 계산
    private fun optimizePathToCheckpoint(
        pod: Pod,
        currentCP: Checkpoint,
        nextCP: Checkpoint,
        distanceToCP: Double
    ): Vector2D {
        // 기존 최적화 타겟 계산
        val baseTarget = PodUtils.calculateOptimizedTarget(pod, currentCP.position, nextCP.position)
        
        // 드리프트 보정 적용
        val driftCompensatedTarget = pod.calculateDriftCompensationTarget(baseTarget)
        
        // 접근 거리에 따른 접근 각도 최적화
        if (distanceToCP < GameConstants.Checkpoint.APPROACH_DISTANCE * 1.5) {
            // 속도와 각도를 고려한 최적 접근 지점 계산
            val podSpeed = pod.speed()
            val approachAngle = pod.angleTo(currentCP.position)
            val podAngleDiff = abs(pod.angleDiff(approachAngle))
            
            // 속도가 빠르고 각도 차이가 크면 더 넓게 접근
            val angleAdjustment = (podSpeed / 100.0) * (podAngleDiff / 45.0) * GameConstants.Strategy.PATH_OPTIMIZATION_FACTOR
            
            // 각도 조정 계산
            val approachVector = Vector2D.fromAngleAndMagnitude(
                approachAngle + angleAdjustment * 15.0, 
                podSpeed * 0.8
            )
            
            // 최종 타겟 계산 - 원래 타겟과 조정된 접근 벡터의 가중 평균
            val optimizedX = (driftCompensatedTarget.x * 0.7 + (pod.position.x + approachVector.x) * 0.3).toInt()
            val optimizedY = (driftCompensatedTarget.y * 0.7 + (pod.position.y + approachVector.y) * 0.3).toInt()
            
            return Vector2D(optimizedX, optimizedY)
        }
        
        return driftCompensatedTarget
    }
    
    override fun execute(
        pod: Pod,
        checkpoints: List<Checkpoint>,
        opponentPods: List<Pod>,
        numberOfCheckpoints: Int,
        boostAvailable: Boolean,
        shieldCooldown: Int,
        raceProgress: Double
    ): Pair<Command, Boolean> {
        val currentCP = checkpoints[pod.nextCheckpointId]
        val nextCPId = (pod.nextCheckpointId + 1) % numberOfCheckpoints
        val nextCP = checkpoints[nextCPId]
        
        updatePIDController(pod)
        
        // 체크포인트까지 거리 계산
        val distanceToCP = pod.position.distanceTo(currentCP.position)
        
        // 경로 최적화를 통한 타겟 계산
        val target = optimizePathToCheckpoint(pod, currentCP, nextCP, distanceToCP)
        
        logDebugInfo(pod, currentCP)
        
        // 레이스 진행도에 따른 전략 조정
        val strategicFactor = if (raceProgress < 0.3)
            GameConstants.Strategy.EARLY_RACE_AGGRESSION
        else
            GameConstants.Strategy.LATE_RACE_PRECISION
        
        // 명령 결정 순서: 실드 -> 부스트 -> 일반 추진
        return when {
            shouldUseShield(pod, opponentPods, target, shieldCooldown) -> 
                Command.Shield(target) to false
                
            shouldUseBoost(pod, target, currentCP, nextCP, boostAvailable, raceProgress, numberOfCheckpoints) -> 
                Command.Boost(target) to true
                
            else -> Command.Thrust(target, calculateOptimalThrust(pod, currentCP, target)) to false
        }
    }
    
    // PID 컨트롤러 상태 업데이트
    private fun updatePIDController(pod: Pod) {
        if (pod.nextCheckpointId != lastCheckpointId) {
            speedController.reset()
            lastCheckpointId = pod.nextCheckpointId
        }
    }
    
    // 디버깅 정보 출력
    private fun logDebugInfo(pod: Pod, currentCP: Checkpoint) {
        System.err.println("Racer: CP ${pod.nextCheckpointId}, Distance: ${pod.position.distanceTo(currentCP.position)}, Speed: ${pod.speed()}")
    }
    
    // 실드 사용 결정 로직을 ShieldController를 사용하도록 변경
    private fun shouldUseShield(
        pod: Pod, 
        opponentPods: List<Pod>, 
        target: Vector2D, 
        shieldCooldown: Int
    ): Boolean {
        return shieldController.shouldUseShield(
            pod = pod,
            opponentPods = opponentPods,
            target = target,
            shieldCooldown = shieldCooldown,
            isInterceptor = false
        )
    }
    
    // 부스트 사용 결정 로직
    private fun shouldUseBoost(
        pod: Pod, 
        target: Vector2D, 
        currentCP: Checkpoint, 
        nextCP: Checkpoint,
        boostAvailable: Boolean, 
        raceProgress: Double,
        numberOfCheckpoints: Int
    ): Boolean {
        if (!PodUtils.shouldUseBoost(pod, target, boostAvailable) || 
            !(raceProgress > GameConstants.Boost.SAVE_THRESHOLD || numberOfCheckpoints <= 3)) {
            return false
        }
        
        val distanceToNext = currentCP.position.distanceTo(nextCP.position)
        val angleDiffToTarget = abs(pod.angleDiff(pod.angleTo(target)))
        
        // 체크포인트 간 거리가 충분히 멀고 각도 차이가 작을 때만 부스트 사용
        if (distanceToNext > GameConstants.Boost.MIN_CHECKPOINT_DISTANCE && 
            angleDiffToTarget < GameConstants.Boost.MAX_ANGLE_DIFF) {
            System.err.println("Using BOOST at CP ${pod.nextCheckpointId}")
            return true
        }
        
        return false
    }
    
    // 최적 추진력 계산 로직 개선
    private fun calculateOptimalThrust(pod: Pod, currentCP: Checkpoint, target: Vector2D): Int {
        val distanceToCP = pod.position.distanceTo(currentCP.position)
        val currentSpeed = pod.speed()
        val angleToTarget = abs(pod.angleDiff(pod.angleTo(target)))
        
        // 다중 스텝 시뮬레이션으로 최적의 추력 계산
        if (distanceToCP < GameConstants.Physics.CHECKPOINT_RADIUS * 2.5) {
            // 체크포인트 근처에서는 속도 조절
            val desiredSpeed = ((distanceToCP - GameConstants.Physics.CHECKPOINT_RADIUS) / 
                              GameConstants.Physics.CHECKPOINT_RADIUS).coerceIn(0.5, 2.0) * 70
            
            // 목표 속도와 실제 속도의 차이에 비례한 추력 계산
            val speedDiff = desiredSpeed - currentSpeed
            val thrustBySpeed = (GameConstants.Thrust.MAX * (0.5 + speedDiff / 100.0)).toInt().coerceIn(0, GameConstants.Thrust.MAX)
            
            // 각도에 따른 추력 감소
            val angleFactor = (1.0 - angleToTarget / GameConstants.Angle.THRESHOLD_STOP).coerceIn(0.0, 1.0)
            
            return (thrustBySpeed * angleFactor).toInt()
        } else {
            // 기본 각도 기반 추력 계산
            return when {
                angleToTarget > GameConstants.Angle.THRESHOLD_STOP -> 0
                angleToTarget > GameConstants.Angle.THRESHOLD_SLOW -> GameConstants.Thrust.SLOW
                else -> GameConstants.Thrust.MAX
            }
        }
    }
}

// 인터셉터 전략 최적화
class InterceptorStrategy : PodStrategy {
    private var currentTargetId = 0 // 현재 타겟팅 중인 적 포드 ID
    private var lastTargetPosition = Vector2D(0, 0) // 이전 턴의 타겟 위치
    private var predictionQuality = 1.0 // 예측의 정확도를 조정하는 계수
    private var consecutiveTargetFrames = 0 // 같은 타겟을 추적한 프레임 수
    private val shieldController = ShieldController()
    
    override fun execute(
        pod: Pod,
        checkpoints: List<Checkpoint>,
        opponentPods: List<Pod>,
        numberOfCheckpoints: Int,
        boostAvailable: Boolean,
        shieldCooldown: Int,
        raceProgress: Double
    ): Pair<Command, Boolean> {
        // 최적의 타겟 선택 - 특정 조건에서 타겟 유지 로직 추가
        val targetOpponent = selectOptimalTarget(pod, opponentPods, checkpoints)
        val targetIndex = opponentPods.indexOf(targetOpponent)
        
        // 타겟 변경 감지 및 추적 프레임 카운트 업데이트
        if (targetIndex != currentTargetId) {
            System.err.println("Interceptor changed target from $currentTargetId to $targetIndex")
            currentTargetId = targetIndex
            consecutiveTargetFrames = 0
            predictionQuality = 1.0
        } else {
            consecutiveTargetFrames++
            
            // 타겟 추적이 계속되면 예측의 정확도 향상
            if (consecutiveTargetFrames > 5) {
                predictionQuality = (predictionQuality * 1.05).coerceAtMost(2.0)
            }
        }
        
        // 적 포드의 이동 경로 예측 정확도 계산
        calculatePredictionAccuracy(targetOpponent)
        
        // 적응형 인터셉트 포인트 계산
        val interceptPoint = calculateAdaptiveInterceptPoint(pod, targetOpponent, checkpoints)
        
        // 디버깅 정보
        System.err.println("Interceptor targeting opponent $currentTargetId at CP ${targetOpponent.nextCheckpointId}, " + 
                         "PredQ: ${"%.2f".format(predictionQuality)}, Frames: $consecutiveTargetFrames")

        // 충돌 이득을 위한 실드 사용 결정 - ShieldController 사용
        if (shieldController.shouldUseShield(
                pod = pod,
                opponentPods = opponentPods,
                target = interceptPoint,
                shieldCooldown = shieldCooldown,
                isInterceptor = true
            )) {
            return Command.Shield(interceptPoint) to false
        }
        
        // 부스트 사용 결정 - 전략적 상황에 맞게 최적화
        if (PodUtils.shouldUseBoostForIntercept(pod, targetOpponent, interceptPoint, boostAvailable, raceProgress)) {
            return Command.Boost(interceptPoint) to true
        }

        // 적 포드와의 거리에 따른 동적 추진력 조정
        val thrust = calculateDynamicInterceptThrust(pod, targetOpponent, interceptPoint)
        return Command.Thrust(interceptPoint, thrust) to false
    }
    
    // 현재 상황을 고려한 최적의 타겟 선택 로직
    private fun selectOptimalTarget(myPod: Pod, opponentPods: List<Pod>, checkpoints: List<Checkpoint>): Pod {
        // 랩 수가 가장 높은 적 파드를 우선 타겟팅
        val opponentsWithHigherLap = opponentPods.filter { it.currentLap > opponentPods.minOf { p -> p.currentLap } }
        
        // 랩 수가 더 높은 적 파드가 있다면 그 중에서 선택
        if (opponentsWithHigherLap.isNotEmpty()) {
            // 선행 랩의 파드 중 가장 진행도가 높은 파드 선택
            val leadingPod = opponentsWithHigherLap.maxByOrNull { 
                it.calculateRaceProgress(checkpoints, it.currentLap) 
            } ?: opponentsWithHigherLap.first()
            
            System.err.println("Targeting opponent with higher lap: ${opponentPods.indexOf(leadingPod)}, " +
                               "Lap: ${leadingPod.currentLap}, CP: ${leadingPod.nextCheckpointId}")
            return leadingPod
        }
        
        // 모든 파드가 같은 랩이면 진행률이 가장 높은 적 파드를 타겟팅
        val leadingPod = opponentPods.maxByOrNull { it.calculateRaceProgress(checkpoints) } ?: opponentPods[0]
        
        // 타겟 유지 로직 - 이전 타겟을 계속 추적하는 것이 효율적일 수 있음
        if (consecutiveTargetFrames > 10 && currentTargetId < opponentPods.size) {
            val currentTarget = opponentPods[currentTargetId]
            // 현재 타겟과 선두 타겟의 진행률 차이가 크지 않다면 타겟 유지
            val progressDiff = leadingPod.calculateRaceProgress(checkpoints) - 
                               currentTarget.calculateRaceProgress(checkpoints)
            if (progressDiff < 0.05) { // 진행률 차이가 5% 미만이면 타겟 유지
                return currentTarget
            }
        }
        
        // 진행률이 가장 높은 적 파드 반환
        return leadingPod
    }

    // 예측의 정확도 계산
    private fun calculatePredictionAccuracy(target: Pod) {
        if (lastTargetPosition != Vector2D.ZERO) {
            val predictedX = lastTargetPosition.x + target.velocity.x
            val predictedY = lastTargetPosition.y + target.velocity.y
            
            val actualPosition = target.position
            val predictionError = hypot(
                (predictedX - actualPosition.x).toDouble(), 
                (predictedY - actualPosition.y).toDouble()
            )
            
            // 예측 오차에 따라 예측 품질 조정
            if (predictionError > 300) {
                // 예측 오차가 크면 품질 감소
                predictionQuality = (predictionQuality * 0.95).coerceAtLeast(0.7)
            } else if (predictionError < 100) {
                // 예측이 정확하면 품질 증가
                predictionQuality = (predictionQuality * 1.03).coerceAtMost(2.0)
            }
        }
        
        // 현재 위치 기록 (다음 턴에 사용)
        lastTargetPosition = target.position
    }

    // 더 정확한 인터셉트 포인트 계산을 위한 적응형 알고리즘
    private fun calculateAdaptiveInterceptPoint(
        myPod: Pod, 
        opponent: Pod, 
        checkpoints: List<Checkpoint>
    ): Vector2D {
        var targetCP = checkpoints[opponent.nextCheckpointId]
        
        // 적의 예상 경로 효율 분석
        val opponentToTarget = targetCP.position - opponent.position
        val opponentSpeed = opponent.speed()
        val opponentDirection = opponent.velocity.normalized()
        val opponentEfficiency = opponentDirection.dotProduct(opponentToTarget.normalized())
        
        // 시뮬레이션 깊이 결정 - 적의 효율성이 높을수록 더 깊게 예측
        val simulationDepth = (GameConstants.Simulation.MAX_DEPTH * opponentEfficiency).toInt().coerceIn(1, GameConstants.Simulation.MAX_DEPTH)
        
        // 적의 다중 미래 위치 시뮬레이션
        val predictedPositions = mutableListOf<Vector2D>()
        var simPos = opponent.position
        var simVel = opponent.velocity
        
        for (i in 1..simulationDepth) {
            // 적이 목표 체크포인트로 향하는 각도 계산
            val angleToCP = atan2(
                (targetCP.position.y - simPos.y).toDouble(),
                (targetCP.position.x - simPos.x).toDouble()
            ) * 180 / PI
            
            // 적의 회전 시뮬레이션 (점진적 회전)
            // 보다 정확한 각도 계산 - reCurse 알고리즘 차용
            val currentAngle = if (i == 0) opponent.angle.toDouble() else simPos.angleTo(targetCP.position)
            val exactAngleToCP = atan2(
                (targetCP.position.y - simPos.y).toDouble(), 
                (targetCP.position.x - simPos.x).toDouble()
            ) * 180 / PI
            
            // 각도 차이 계산 개선 (항상 -180 ~ 180 범위를 유지)
            var angleDiff = exactAngleToCP - currentAngle
            while (angleDiff > 180) angleDiff -= 360
            while (angleDiff < -180) angleDiff += 360
            
            // 회전 물리학 최적화
            val rotation = angleDiff.coerceIn(
                -GameConstants.Physics.MAX_TURN_ANGLE, 
                GameConstants.Physics.MAX_TURN_ANGLE
            )
            
            val newAngle = (currentAngle + rotation + 360) % 360
            val angleRad = newAngle * PI / 180
            
            // 적의 추력 추정 - 물리 기반 인공지능 결정
            val distToCP = simPos.distanceTo(targetCP.position)
            val podToCheckpointAngleDiff = abs(angleDiff)
            val estimatedThrust = when {
                distToCP < GameConstants.Physics.CHECKPOINT_RADIUS * 1.2 -> 
                    // 체크포인트에 가까울 때 감속 로직 개선 
                    (GameConstants.Thrust.MAX * (distToCP / (GameConstants.Physics.CHECKPOINT_RADIUS * 2))).toInt()
                        .coerceIn(GameConstants.Thrust.SLOW / 2, GameConstants.Thrust.MAX)
                podToCheckpointAngleDiff > GameConstants.Angle.THRESHOLD_SLOW -> 
                    // 각도에 따른 추력 감소를 더 세밀하게 조정
                    (GameConstants.Thrust.MAX * (1 - (podToCheckpointAngleDiff - GameConstants.Angle.THRESHOLD_SLOW) / 
                     (GameConstants.Angle.THRESHOLD_STOP - GameConstants.Angle.THRESHOLD_SLOW))).toInt()
                        .coerceIn(0, GameConstants.Thrust.SLOW)
                else -> GameConstants.Thrust.MAX
            }
            
            // 가속도 계산 및 적용 - 더 정확한 물리 모델
            val accX = cos(angleRad) * estimatedThrust
            val accY = sin(angleRad) * estimatedThrust
            
            // 속도 업데이트
            simVel = Vector2D(
                ((simVel.x + accX) * GameConstants.Physics.FRICTION).toInt(),
                ((simVel.y + accY) * GameConstants.Physics.FRICTION).toInt()
            )
            
            // 위치 업데이트
            simPos += simVel
            
            // 체크포인트 도달 검사 - 시뮬레이션 최적화
            if (simPos.distanceTo(targetCP.position) < GameConstants.Physics.CHECKPOINT_RADIUS) {
                // 체크포인트에 도달했다면 시뮬레이션 정확도 향상을 위해 다음 체크포인트 향해 움직이도록 설정
                val nextCPIndex = (opponent.nextCheckpointId + 1) % checkpoints.size
                if (nextCPIndex < checkpoints.size) {
                    targetCP = checkpoints[nextCPIndex]
                }
            }
            
            // 예측 위치 저장
            predictedPositions.add(simPos)
        }
        
        // 몬테카를로 접근법을 적용한 인터셉트 계산
        // 1. 다양한 시뮬레이션 데이터를 기반으로 최적 지점 결정
        var bestInterceptPoint = opponent.position
        var bestInterceptScore = Double.MAX_VALUE
        
        for (i in predictedPositions.indices) {
            val predictedPos = predictedPositions[i]
            val timeToReach = i + 1 // 시뮬레이션 스텝 = 시간 단위
            
            // 내가 그 위치에 도달하는데 걸리는 시간 추정 - 더 정확한 계산
            val distance = myPod.position.distanceTo(predictedPos)
            val myCurrentSpeed = myPod.speed()
            val expectedAcceleration = GameConstants.Thrust.MAX * 
                cos(abs(myPod.angleDiff(myPod.angleTo(predictedPos))) * PI / 180)
            
            // 가속도를 고려한 속도 예측
            val estimatedSpeed = (myCurrentSpeed + expectedAcceleration / 2)
                .coerceAtLeast(100.0) // 최소 속도 가정
            val myTimeToReach = distance / estimatedSpeed
            
            // 각도 계산 개선 - 현재 방향과 목표 방향 간의 전환 시간 고려
            val angleDiffToTarget = abs(myPod.angleDiff(myPod.angleTo(predictedPos)))
            val turnTime = angleDiffToTarget / GameConstants.Physics.MAX_TURN_ANGLE
            
            // 점수 계산 - 더 복합적인 요소 고려
            val score = (abs(timeToReach - (myTimeToReach + turnTime)) * GameConstants.Simulation.TIME_WEIGHT + 
                      (angleDiffToTarget / 180) * GameConstants.Simulation.ANGLE_WEIGHT +
                      abs(myPod.velocity.magnitude() - predictedPos.magnitude()) / 100.0 * 0.3 -
                      (1.0 - (distance / GameConstants.MAX_MAP_DISTANCE).coerceIn(0.0, 0.9)) * 0.2) // 가까울수록 유리
            
            // 최고 점수 갱신
            if (score < bestInterceptScore) {
                bestInterceptScore = score
                bestInterceptPoint = predictedPos
            }
        }
        
        // 최종 인터셉트 지점 미세 조정 - 충돌 최적화
        val finalInterceptPoint = if (bestInterceptPoint.distanceTo(myPod.position) < GameConstants.Shield.DISTANCE * 1.5) {
            // 가까운 거리에서는 충돌 각도를 최적화
            val directionToOpponent = (bestInterceptPoint - myPod.position).normalized()
            val opponentVelocity = opponent.velocity.normalized()
            
            // 직각에 가까운 충돌이 더 효과적
            val collisionAngle = directionToOpponent.angleBetween(opponentVelocity)
            val optimalCollisionVector = if (abs(collisionAngle) < 90) {
                // 상대 진행방향에 직각으로 충돌 시도
                opponentVelocity.rotateDegrees(90.toDouble()).withMagnitude(GameConstants.Physics.POD_SIZE.toDouble())
            } else {
                // 이미 좋은 충돌 각도면 그대로 유지
                directionToOpponent.withMagnitude(GameConstants.Physics.POD_SIZE.toDouble())
            }
            
            bestInterceptPoint + optimalCollisionVector
        } else {
            bestInterceptPoint
        }
        
        // 디버그 정보
        System.err.println("Adaptive intercept: simulated ${predictedPositions.size} steps, " +
                         "score: ${"%.2f".format(bestInterceptScore)}")
        
        return finalInterceptPoint
    }
    
    // 적 포드에 대한 동적 추진력 계산 - 물리 기반 최적화
    private fun calculateDynamicInterceptThrust(pod: Pod, targetOpponent: Pod, interceptPoint: Vector2D): Int {
        val angleToTarget = pod.angleTo(interceptPoint)
        val angleDiff = abs(pod.angleDiff(angleToTarget))
        val distanceToTarget = pod.position.distanceTo(interceptPoint)
        
        // reCurse의 접근법 적용: 속도와 거리에 따른 추진력 동적 조절
        val desiredSpeed = when {
            distanceToTarget < GameConstants.Physics.POD_SIZE * 2 -> 
                targetOpponent.speed() * 1.2 // 가까이서는 적보다 약간 빠르게
            distanceToTarget < GameConstants.Intercept.CLOSE_THRESHOLD -> 
                min(distanceToTarget, 600.0) // 가까울 때는 거리에 비례
            else -> GameConstants.Thrust.MAX.toDouble() // 멀리 있을 때는 최대 속도
        }
        
        // 현재 속도와 목표 속도의 차이에 따라 추진력 조절
        val speedDiff = desiredSpeed - pod.speed()
        
        // 각도에 따른 추진력 페널티
        val angleFactor = when {
            angleDiff > GameConstants.Angle.THRESHOLD_STOP -> 0.0
            angleDiff > GameConstants.Angle.THRESHOLD_SLOW -> 
                1.0 - (angleDiff - GameConstants.Angle.THRESHOLD_SLOW) / 
                      (GameConstants.Angle.THRESHOLD_STOP - GameConstants.Angle.THRESHOLD_SLOW)
            else -> 1.0
        }
        
        // 공격성 계수와 거리, 각도, 속도 차이를 고려한 추진력 계산
        val baseThrust = (GameConstants.Thrust.MAX * angleFactor * 
                         (0.5 + speedDiff / 200.0).coerceIn(0.5, 1.5)).toInt()
                         .coerceIn(0, GameConstants.Thrust.MAX)
        
        // 충돌 임박 시 추진력 조정 (충돌 효과 최대화)
        if (distanceToTarget < GameConstants.Physics.POD_SIZE * 3 && angleDiff < 30) {
            return GameConstants.Thrust.MAX // 충돌 직전 최대 추진력
        }
        
        // 적 속도와 방향을 고려한 추가 조정
        val approachingSpeed = -targetOpponent.velocity.dotProduct(
            (pod.position - targetOpponent.position).normalized()
        )
        val finalThrust = (baseThrust * (1 + approachingSpeed / 1000)).toInt()
        
        return finalThrust.coerceIn(0, GameConstants.Thrust.MAX)
    }
}

// 물리 시뮬레이션 클래스 추가
object PhysicsSimulator {
    // 일정 시간 후의 위치 예측 (등속도 모델)
    fun predictPosition(currentPos: Vector2D, velocity: Vector2D, timeSteps: Int): Vector2D {
        var pos = currentPos
        var vel = velocity
        
        for (i in 0 until timeSteps) {
            // 마찰 적용
            vel = Vector2D(
                (vel.x * GameConstants.Physics.FRICTION).toInt(), 
                (vel.y * GameConstants.Physics.FRICTION).toInt()
            )
            pos += vel
        }
        
        return pos
    }
    
    // 가속도를 고려한 위치 예측 (등가속도 모델)
    fun predictPositionWithAcceleration(
        currentPos: Vector2D, 
        velocity: Vector2D, 
        acceleration: Vector2D, 
        timeSteps: Int
    ): Vector2D {
        var pos = currentPos
        var vel = velocity
        
        for (i in 0 until timeSteps) {
            // 가속도 적용
            vel += acceleration
            
            // 마찰 적용
            vel = Vector2D(
                (vel.x * GameConstants.Physics.FRICTION).toInt(), 
                (vel.y * GameConstants.Physics.FRICTION).toInt()
            )
            
            pos += vel
        }
        
        return pos
    }
    
    // 추력(thrust)을 각도와 크기로 변환하여 속도 계산
    fun calculateVelocityWithThrust(
        currentVelocity: Vector2D, 
        angleDegrees: Double, 
        thrust: Int
    ): Vector2D {
        // 각도 방향으로의 가속도 벡터 계산
        val accelerationVector = Vector2D.fromAngleAndMagnitude(angleDegrees, thrust.toDouble())
        
        // 현재 속도에 가속도 적용
        val newVelocity = currentVelocity + accelerationVector
        
        // 마찰 적용
        return Vector2D(
            (newVelocity.x * GameConstants.Physics.FRICTION).toInt(),
            (newVelocity.y * GameConstants.Physics.FRICTION).toInt()
        )
    }
    
    // 두 포드 간의 충돌 시뮬레이션
    fun simulateCollision(pod1Pos: Vector2D, pod1Vel: Vector2D, pod2Pos: Vector2D, pod2Vel: Vector2D): Pair<Vector2D, Vector2D> {
        // 충돌 벡터 계산
        val collisionVector = (pod2Pos - pod1Pos).normalized()
        
        // 충돌 임펄스 적용
        val impulse = GameConstants.Physics.COLLISION_IMPULSE.toDouble()
        val pod1NewVel = pod1Vel - collisionVector * impulse
        val pod2NewVel = pod2Vel + collisionVector * impulse
        
        return pod1NewVel to pod2NewVel
    }
}

// PodUtils 클래스에 피지컬 시뮬레이션 메서드 추가
object PodUtils {
    // 부스트 사용 결정 로직
    fun shouldUseBoost(
        pod: Pod, 
        target: Vector2D, 
        boostAvailable: Boolean, 
        minDistance: Int = GameConstants.Boost.MIN_DISTANCE
    ): Boolean {
        if (!boostAvailable) return false

        val distanceToTarget = pod.position.distanceTo(target)
        val angleToTarget = pod.angleTo(target)
        val angleDiff = pod.angleDiff(angleToTarget)

        return distanceToTarget > minDistance &&
                abs(angleDiff) < GameConstants.Boost.MAX_ANGLE_DIFF
    }

    // 부스트 사용 결정 로직 - 인터셉터용 
    fun shouldUseBoostForIntercept(
        pod: Pod,
        target: Pod,
        interceptPoint: Vector2D,
        boostAvailable: Boolean,
        raceProgress: Double
    ): Boolean {
        if (!boostAvailable) return false
        
        val distanceToTarget = pod.position.distanceTo(target.position)
        val angleToTarget = pod.angleTo(interceptPoint)
        val angleDiff = abs(pod.angleDiff(angleToTarget))
        
        // 적절한 거리와 각도에서만 부스트 사용
        return distanceToTarget > GameConstants.Boost.INTERCEPT_DISTANCE &&
                distanceToTarget < GameConstants.Intercept.OPTIMAL_INTERCEPT_MAX_DISTANCE &&
                angleDiff < GameConstants.Boost.MAX_ANGLE_DIFF
    }
    
    // 동적 추진력 계산
    fun calculateDynamicThrust(
        pod: Pod,
        target: Pod,
        interceptPoint: Vector2D
    ): Int {
        val angleToTarget = pod.angleTo(interceptPoint)
        val angleDiff = abs(pod.angleDiff(angleToTarget))
        
        // 각도에 따른 기본 추진력 결정
        return when {
            angleDiff > GameConstants.Angle.THRESHOLD_STOP -> 0
            angleDiff > GameConstants.Angle.THRESHOLD_SLOW -> GameConstants.Thrust.SLOW
            else -> GameConstants.Thrust.MAX
        }
    }
    
    // 최적화된 타겟 계산
    fun calculateOptimizedTarget(
        pod: Pod,
        currentCP: Vector2D,
        nextCP: Vector2D
    ): Vector2D {
        // 현재 체크포인트까지의 거리
        val distToCP = pod.position.distanceTo(currentCP)
        
        // 체크포인트에 가까울 때는 다음 체크포인트 방향으로 약간 선회
        return if (distToCP < GameConstants.Checkpoint.APPROACH_DISTANCE) {
            // 현재와 다음 체크포인트 사이의 중간점 계산
            val nextWeight = (GameConstants.Checkpoint.APPROACH_DISTANCE - distToCP) / 
                             GameConstants.Checkpoint.APPROACH_DISTANCE * 0.3
            
            // 가중 평균 계산 - Vector2D의 lerp 메서드 사용
            Vector2D.lerp(currentCP, nextCP, nextWeight)
        } else {
            // 충분히 멀면 현재 체크포인트만 겨냥
            currentCP
        }
    }

    // 특정 속도로 포드가 이동할 때 타겟에 도달하는데 필요한 시간 계산
    fun timeToReachTarget(podPos: Vector2D, podVel: Vector2D, targetPos: Vector2D): Double {
        // 포드와 타겟 간의 거리
        val distance = podPos.distanceTo(targetPos)
        
        // 속도의 크기
        val speed = podVel.magnitude()
        
        // 속도가 0에 가까우면 큰 값 반환 (무한대 대신)
        return if (speed < 0.001) Double.MAX_VALUE else distance / speed
    }
    
    // 최적의 인터셉트 포인트 계산 (이동 중인 타겟을 가로채는 지점)
    fun calculateInterceptPoint(
        podPos: Vector2D, 
        podSpeed: Double, 
        targetPos: Vector2D, 
        targetVelocity: Vector2D, 
        maxPredictionSteps: Int = 20
    ): Vector2D {
        var bestInterceptPoint = targetPos
        var minTimeGap = Double.MAX_VALUE
        
        // 여러 시간 단계에서의 인터셉트 포인트 검사
        for (step in 1..maxPredictionSteps) {
            // 타겟의 예측 위치
            val predictedTargetPos = PhysicsSimulator.predictPosition(targetPos, targetVelocity, step)
            
            // 포드가 해당 위치에 도달하는데 걸리는 시간
            val distanceToPrediction = podPos.distanceTo(predictedTargetPos)
            val mySpeed = podSpeed.coerceAtLeast(100.0) // 최소 속도 가정
            val timeForPod = distanceToPrediction / mySpeed
            
            // 시간 차이 (포드와 타겟이 같은 지점에 도달하는 시간 차이)
            val timeGap = abs(timeForPod - step)
            
            // 더 나은 인터셉트 지점을 찾으면 업데이트
            if (timeGap < minTimeGap) {
                minTimeGap = timeGap
                bestInterceptPoint = predictedTargetPos
            }
        }
        
        return bestInterceptPoint
    }
}

// 실드 컨트롤러 클래스 - 모든 파드의 실드 사용 판단 로직을 통합
class ShieldController {
    // 실드 사용 여부를 판단하는 통합 메서드
    fun shouldUseShield(
        pod: Pod,
        opponentPods: List<Pod>,
        teammate: Pod? = null,
        target: Vector2D,
        shieldCooldown: Int,
        isInterceptor: Boolean = false
    ): Boolean {
        if (shieldCooldown > 0) return false
        
        // 가장 가까운 적 포드 찾기
        val nearestOpponent = opponentPods.minByOrNull { pod.position.distanceTo(it.position) } ?: return false
        val distanceToNearestOpponent = pod.position.distanceTo(nearestOpponent.position)
        
        // 충돌 임계값 내의 거리이거나 충돌이 예측되는 경우
        if (distanceToNearestOpponent < GameConstants.Shield.PREDICTION_DISTANCE || 
            willCollideWithin(pod, nearestOpponent, GameConstants.Shield.COLLISION_PREDICTION_TURNS)) {
            
            // 충돌 이득 점수 계산
            val collisionScore = calculateCollisionBenefitScore(pod, nearestOpponent, target)
            
            // 충돌이 이득이 될 때만 실드 사용, 인터셉터는 더 적극적으로 실드 사용
            val benefitThreshold = if (isInterceptor) 
                GameConstants.Collision.BENEFIT_THRESHOLD * 0.7 
                else GameConstants.Collision.BENEFIT_THRESHOLD
            
            if (collisionScore > benefitThreshold) {
                System.err.println("${if (isInterceptor) "Interceptor" else "Racer"} activating SHIELD - " +
                                  "Collision predicted with benefit score: ${"%.2f".format(collisionScore)}")
                return true
            }
        }
        
        return false
    }
    
    // 충돌 예측 메서드
    fun willCollideWithin(pod1: Pod, pod2: Pod, turns: Int): Boolean {
        val minDistance = GameConstants.Physics.POD_SIZE * 2
        
        // 현재 포지션과 속도 복사
        var pos1 = pod1.position
        var vel1 = pod1.velocity
        var pos2 = pod2.position
        var vel2 = pod2.velocity
        
        // 여러 턴에 걸쳐 충돌 예측 검사
        for (i in 0 until turns) {
            // 마찰 적용
            vel1 = Vector2D(
                (vel1.x * GameConstants.Physics.FRICTION).toInt(),
                (vel1.y * GameConstants.Physics.FRICTION).toInt()
            )
            vel2 = Vector2D(
                (vel2.x * GameConstants.Physics.FRICTION).toInt(),
                (vel2.y * GameConstants.Physics.FRICTION).toInt()
            )
            
            // 위치 업데이트
            pos1 += vel1
            pos2 += vel2
            
            // 충돌 검사
            if (pos1.distanceTo(pos2) < minDistance) {
                return true
            }
        }
        
        return false
    }
    
    // 충돌 이득 점수 계산
    fun calculateCollisionBenefitScore(pod: Pod, opponent: Pod, target: Vector2D): Double {
        val myToTarget = target - pod.position
        val myVelDotTarget = pod.velocity.dotProduct(myToTarget.normalized())
        val otherToTarget = target - opponent.position
        val otherVelDotTarget = opponent.velocity.dotProduct(otherToTarget.normalized())
        
        // 내 속도 벡터가 타겟 방향과 얼마나 일치하는지 대비 상대가 어떤지 비교
        return myVelDotTarget - otherVelDotTarget
    }
}

// 게임 컨트롤러 개선
class GameController(private val input: Scanner) {
    private val racerStrategy = RacerStrategy()
    private val interceptorStrategy = InterceptorStrategy()
    private var boostAvailable = true
    private val boostUsed = booleanArrayOf(false, false)
    private val shieldCooldown = intArrayOf(0, 0)
    private val checkpoints = mutableListOf<Checkpoint>()
    private var numberOfCheckpoints = 0
    private var maxLaps = 1
    private val podLaps = intArrayOf(1, 1)  // 각 파드별 현재 랩 정보
    private val lastCheckpointId = intArrayOf(0, 0)
    private val opPodLaps = intArrayOf(1, 1)  // 각 적 파드별 현재 랩 정보
    private val opLastCheckpointId = intArrayOf(0, 0)  // 각 적 파드의 이전 체크포인트 ID
    
    // 게임 초기화
    fun init() {
        maxLaps = input.nextInt()
        numberOfCheckpoints = input.nextInt()

        for (i in 0 until numberOfCheckpoints) {
            val x = input.nextInt()
            val y = input.nextInt()
            checkpoints.add(Checkpoint(Vector2D(x, y), i))
        }
        
        System.err.println("Race initialized: $maxLaps laps, $numberOfCheckpoints checkpoints")
    }

    // 레이스 상태 업데이트 - 각 파드별 랩 카운트 처리
    private fun updateRaceStatus(pod: Pod, podIndex: Int) {
        if (pod.nextCheckpointId != lastCheckpointId[podIndex]) {
            // 마지막 체크포인트를 지나 첫번째 체크포인트로 돌아오면 랩 증가
            if (pod.nextCheckpointId == 0 && lastCheckpointId[podIndex] == numberOfCheckpoints - 1) {
                podLaps[podIndex]++
                pod.currentLap = podLaps[podIndex]  // Pod 객체에도 랩 정보 업데이트
                System.err.println("Pod $podIndex completed lap ${podLaps[podIndex]}/$maxLaps")
            }
            lastCheckpointId[podIndex] = pod.nextCheckpointId
        }
    }
    
    // 레이스 진행도 계산 - 현재 랩 정보 사용
    private fun calculateRaceProgress(pod: Pod): Double {
        return pod.calculateRaceProgress(checkpoints, maxLaps)
    }

    // 게임 턴 실행
    fun runGameTurn() {
        val myPods = List(2) { Pod.fromInput(input) }
        val opPods = List(2) { Pod.fromInput(input) }
        
        // 각 포드에 현재 랩 정보 설정
        for (i in 0..1) {
            myPods[i].currentLap = podLaps[i]
        }
        
        // 적 파드의 랩 정보 추적
        for (i in 0..1) {
            val opPod = opPods[i]
            // 체크포인트 변경 감지
            if (opPod.nextCheckpointId != opLastCheckpointId[i]) {
                // 마지막 체크포인트를 지나 첫번째 체크포인트로 돌아오면 랩 증가
                if (opPod.nextCheckpointId == 0 && opLastCheckpointId[i] == numberOfCheckpoints - 1) {
                    opPodLaps[i]++
                    System.err.println("Opponent Pod $i completed lap ${opPodLaps[i]}/$maxLaps")
                }
                opLastCheckpointId[i] = opPod.nextCheckpointId
            }
            // 적 파드에 랩 정보 설정
            opPods[i].currentLap = opPodLaps[i]
        }
        
        // 레이스 상태 업데이트
        updateRaceStatus(myPods[0], 0)
        updateRaceStatus(myPods[1], 1) // 두 번째 포드도 업데이트 추가
        val racerProgress = calculateRaceProgress(myPods[0])
        
        // 디버깅 정보 출력 - 적 파드 랩 정보 포함
        System.err.println("Racer: Lap ${podLaps[0]}/$maxLaps, Interceptor: Lap ${podLaps[1]}/$maxLaps, " +
                           "Opponents: Lap ${opPodLaps[0]}/$maxLaps, ${opPodLaps[1]}/$maxLaps, " +
                           "Progress: $racerProgress")
        
        // 두 포드의 명령을 저장할 배열
        val podCommands = arrayOfNulls<Command>(2)
        
        // 각 포드 명령 계산 
        for (i in 0 until 2) {
            val pod = myPods[i]
            if (i > 0) updateRaceStatus(pod, i)
            
            // 전략 선택 및 실행
            val strategy = if (i == 0) racerStrategy else interceptorStrategy
            
            // 타겟팅 디버깅 정보
            if (i == 1) {
                val targetOpponent = opPods.maxByOrNull { 
                    val estimatedLap = if (it.nextCheckpointId < opLastCheckpointId[1] && opLastCheckpointId[1] > numberOfCheckpoints/2) 
                                          opPodLaps[1] + 1 else opPodLaps[1]
                    it.calculateRaceProgress(checkpoints, estimatedLap) 
                }
                targetOpponent?.let {
                    System.err.println("Targeting opponent ${opPods.indexOf(it)}: " +
                                     "CP ${it.nextCheckpointId}, Progress: ${it.calculateRaceProgress(checkpoints, maxLaps)}")
                }
            }

            // 명령 계산 및 저장
            val (command, usedBoost) = strategy.execute(
                pod,
                checkpoints,
                opPods,
                numberOfCheckpoints,
                !boostUsed[i],
                shieldCooldown[i],
                racerProgress
            )

            // 포드 상태 업데이트
            updatePodStatus(i, command, usedBoost)
            
            // 명령 저장
            podCommands[i] = command
        }
        
        // 모든 포드 명령을 한번에 출력
        outputCommands(podCommands)
    }
    
    // 모든 포드 명령을 한번에 출력하는 메서드
    private fun outputCommands(commands: Array<Command?>) {
        for (command in commands) {
            command?.let {
                println(it.toString())
            }
        }
    }
    
    // 기존 outputCommand 메서드 제거, 포드 상태 업데이트 메서드만 유지
    private fun updatePodStatus(podIndex: Int, command: Command, usedBoost: Boolean) {
        // 실드 쿨다운 관리
        when (command) {
            is Command.Shield -> shieldCooldown[podIndex] = GameConstants.Shield.COOLDOWN
            else -> if (shieldCooldown[podIndex] > 0) shieldCooldown[podIndex]--
        }

        // 부스트 사용 관리
        if (usedBoost) {
            boostUsed[podIndex] = true
            if (boostUsed.all { it }) boostAvailable = false
        }
    }
}

// PID 컨트롤러 클래스 추가
class PIDController(
    private val kP: Double,
    private val kI: Double,
    private val kD: Double,
    private val minOutput: Double,
    private val maxOutput: Double
) {
    private var previousError: Double = 0.0
    private var integral: Double = 0.0
    private var lastTime: Long = System.currentTimeMillis()
    
    fun calculate(setpoint: Double, processVariable: Double): Double {
        val currentTime = System.currentTimeMillis()
        val deltaTime = (currentTime - lastTime).coerceAtLeast(1) / 1000.0 // 초 단위 변환
        lastTime = currentTime
        
        // 오차 계산
        val error = setpoint - processVariable
        
        // 적분항 계산
        integral += error * deltaTime
        
        // 적분 윈드업 방지
        integral = integral.coerceIn(-10.0, 10.0)
        
        // 미분항 계산
        val derivative = if (deltaTime > 0) (error - previousError) / deltaTime else 0.0
        previousError = error
        
        // PID 출력 계산
        val output = kP * error + kI * integral + kD * derivative
        
        // 출력 제한
        return output.coerceIn(minOutput, maxOutput)
    }
    
    // 컨트롤러 상태 리셋
    fun reset() {
        previousError = 0.0
        integral = 0.0
        lastTime = System.currentTimeMillis()
    }
}

// 메인 함수
fun main() {
    val input = Scanner(System.`in`)
    val game = GameController(input)
    game.init()
    while (true) {
        game.runGameTurn()
    }
}
