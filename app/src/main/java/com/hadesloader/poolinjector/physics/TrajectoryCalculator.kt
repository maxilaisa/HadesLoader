package com.hadesloader.poolinjector.physics

import android.graphics.PointF
import kotlin.math.*

/**
 * Trajectory Calculator with real 8 Ball Pool physics parameters
 * Based on actual game source code analysis
 */
class TrajectoryCalculator {
    
    data class Ball(
        val position: PointF,
        val velocity: PointF,
        val radius: Float = REAL_BALL_RADIUS
    )
    
    data class TableDimensions(
        val width: Float,
        val height: Float,
        val cushionWidth: Float = REAL_CUSHION_WIDTH,
        val pocketRadius: Float = REAL_POCKET_RADIUS
    )
    
    data class TrajectoryPoint(
        val position: PointF,
        val time: Float
    )
    
    // Real game constants from 8 Ball Pool source code
    companion object {
        // Ball radius from actual game (Ball.h: 3.800475)
        private const val REAL_BALL_RADIUS = 3.800475f
        
        // Table dimensions from actual game (Prediction.cpp)
        // Table bounds: x from -127.0 to 127.0, y from -63.5 to 63.5
        private const val REAL_TABLE_WIDTH = 254f
        private const val REAL_TABLE_HEIGHT = 127f
        private const val REAL_CUSHION_WIDTH = 25f
        
        // Pocket radius (estimated based on game mechanics)
        private const val REAL_POCKET_RADIUS = 8f
        
        // Physics time step from actual game (const.h: 0.005)
        private const val TIME_PER_TICK = 0.005f
        
        // Friction coefficient from actual game (Prediction.cpp: 0.00145772594752187)
        private const val SLIDING_FRICTION = 0.00145772594752187f
        
        // Rolling friction from actual game (Prediction.cpp: 10.878)
        private const val ROLLING_FRICTION = 10.878f
        
        // Gravity for spin physics (Prediction.cpp: 9.8)
        private const val GRAVITY = 9.8f
        
        // Cushion collision physics constants
        private const val CUSHION_FRICTION = 0.54f
        private const val CUSHION_SPIN_FACTOR = 0.804f
        
        // Minimum velocity threshold
        private const val MIN_VELOCITY = 0.01f
    }
    
    fun calculateTrajectory(
        cueBall: Ball,
        targetBalls: List<Ball>,
        table: TableDimensions,
        maxTime: Float = 5.0f,
        timeStep: Float = TIME_PER_TICK
    ): List<TrajectoryPoint> {
        val trajectory = mutableListOf<TrajectoryPoint>()
        var currentBall = cueBall.copy()
        var currentTime = 0f
        
        trajectory.add(TrajectoryPoint(currentBall.position, currentTime))
        
        while (currentTime < maxTime && currentBall.velocity.length() > MIN_VELOCITY) {
            // Apply game-accurate physics
            currentBall = applyGamePhysics(currentBall, timeStep)
            
            // Update position
            val newPosition = PointF(
                currentBall.position.x + currentBall.velocity.x * timeStep,
                currentBall.position.y + currentBall.velocity.y * timeStep
            )
            
            // Check cushion collisions
            val reflectedBall = checkCushionCollision(
                currentBall.copy(position = newPosition),
                table
            )
            
            // Check ball collisions
            val afterBallCollision = checkBallCollisions(
                reflectedBall,
                targetBalls
            )
            
            currentBall = afterBallCollision
            currentTime += timeStep
            
            trajectory.add(TrajectoryPoint(currentBall.position, currentTime))
        }
        
        return trajectory
    }
    
    /**
     * Apply game-accurate physics based on actual 8 Ball Pool source
     * Based on Prediction.cpp calcVelocity() method
     */
    private fun applyGamePhysics(ball: Ball, deltaTime: Float): Ball {
        val velocityLength = sqrt(ball.velocity.x * ball.velocity.x + ball.velocity.y * ball.velocity.y)
        
        if (velocityLength > MIN_VELOCITY) {
            // Apply sliding friction (from game: 0.00145772594752187)
            val slidingDeceleration = velocityLength * SLIDING_FRICTION
            val slidingTime = if (slidingDeceleration > 1e-11f) slidingDeceleration else deltaTime
            
            if (slidingTime < deltaTime) {
                // Apply sliding friction
                val frictionFactor = 196f * min(slidingTime, deltaTime) / velocityLength
                val newVelocityX = ball.velocity.x + ball.velocity.y * frictionFactor
                val newVelocityY = ball.velocity.y - ball.velocity.x * frictionFactor
                
                // Apply rolling friction for remaining time
                val remainingTime = deltaTime - slidingTime
                val rollingDeceleration = remainingTime * ROLLING_FRICTION
                val velocityFactor = max(0f, 1f - rollingDeceleration / velocityLength)
                
                return ball.copy(
                    velocity = PointF(
                        newVelocityX * velocityFactor,
                        newVelocityY * velocityFactor
                    )
                )
            } else {
                // Apply rolling friction
                val rollingDeceleration = deltaTime * ROLLING_FRICTION
                val velocityFactor = max(0f, 1f - rollingDeceleration / velocityLength)
                
                return ball.copy(
                    velocity = PointF(
                        ball.velocity.x * velocityFactor,
                        ball.velocity.y * velocityFactor
                    )
                )
            }
        }
        
        return ball
    }
    
    fun calculateAimTrajectory(
        cueBallPosition: PointF,
        aimDirection: Float,
        power: Float,
        targetBalls: List<Ball>,
        table: TableDimensions
    ): List<TrajectoryPoint> {
        val velocity = PointF(
            cos(aimDirection) * power,
            sin(aimDirection) * power
        )
        
        val cueBall = Ball(cueBallPosition, velocity)
        return calculateTrajectory(cueBall, targetBalls, table)
    }
    
    /**
     * Check cushion collision with game-accurate physics
     * Based on Prediction.cpp calcVelocityPostCollision() method
     */
    private fun checkCushionCollision(ball: Ball, table: TableDimensions): Ball {
        var newVelocity = ball.velocity
        var newPosition = ball.position
        
        // Calculate collision normal based on which cushion was hit
        var collisionAngle = 0f
        var collided = false
        
        // Left cushion
        if (newPosition.x - ball.radius < table.cushionWidth) {
            newPosition.x = table.cushionWidth + ball.radius
            collisionAngle = 0f // Normal points right
            collided = true
        }
        // Right cushion  
        else if (newPosition.x + ball.radius > table.width - table.cushionWidth) {
            newPosition.x = table.width - table.cushionWidth - ball.radius
            collisionAngle = PI.toFloat() // Normal points left
            collided = true
        }
        // Top cushion
        else if (newPosition.y - ball.radius < table.cushionWidth) {
            newPosition.y = table.cushionWidth + ball.radius
            collisionAngle = PI.toFloat() / 2f // Normal points down
            collided = true
        }
        // Bottom cushion
        else if (newPosition.y + ball.radius > table.height - table.cushionWidth) {
            newPosition.y = table.height - table.cushionWidth - ball.radius
            collisionAngle = -PI.toFloat() / 2f // Normal points up
            collided = true
        }
        
        if (collided) {
            newVelocity = applyCushionPhysics(ball, collisionAngle)
        }
        
        return ball.copy(position = newPosition, velocity = newVelocity)
    }
    
    /**
     * Apply cushion collision physics based on actual game code
     * Based on Prediction.cpp calcVelocityPostCollision() method
     */
    private fun applyCushionPhysics(ball: Ball, collisionAngle: Float): PointF {
        val cosAngle = cos(collisionAngle)
        val sinAngle = sin(collisionAngle)
        
        val vx = ball.velocity.x
        val vy = ball.velocity.y
        
        // Calculate velocity components relative to collision normal
        val vNormal = vx * cosAngle + vy * sinAngle
        val vTangent = -vx * sinAngle + vy * cosAngle
        
        // Apply cushion friction and spin effects (from game constants)
        val newVNormal = -vNormal * CUSHION_SPIN_FACTOR
        val newVTangent = vTangent - (CUSHION_FRICTION * ball.radius)
        
        // Transform back to world coordinates
        val newVx = newVNormal * cosAngle - newVTangent * sinAngle
        val newVy = newVNormal * sinAngle + newVTangent * cosAngle
        
        return PointF(newVx, newVy)
    }
    
    /**
     * Check ball-to-ball collisions with game-accurate physics
     * Based on Prediction.cpp handleBallBallCollision() method
     */
    private fun checkBallCollisions(cueBall: Ball, targetBalls: List<Ball>): Ball {
        var newCueBall = cueBall
        
        for (target in targetBalls) {
            val dx = target.position.x - newCueBall.position.x
            val dy = target.position.y - newCueBall.position.y
            val distance = sqrt(dx * dx + dy * dy)
            val minDist = newCueBall.radius + target.radius
            
            if (distance < minDist && distance > 0.001f) {
                // Collision detected - use game's exact collision physics
                val nx = dx / distance
                val ny = dy / distance
                
                // Relative velocity (from game code)
                val dvx = newCueBall.velocity.x - target.velocity.x
                val dvy = newCueBall.velocity.y - target.velocity.y
                
                // Velocity components along collision normal (from game)
                val v1n = -(nx * newCueBall.velocity.x + ny * newCueBall.velocity.y)
                val v2n = nx * target.velocity.x + ny * target.velocity.y
                
                // Apply elastic collision (from game: perfect elastic collision)
                // The game uses simple elastic collision physics
                val newV1n = v2n
                val newV2n = v1n
                
                // Convert back to velocity components
                val impulse1 = (newV1n - v1n)
                val impulse2 = (newV2n - v2n)
                
                newCueBall = newCueBall.copy(
                    velocity = PointF(
                        newCueBall.velocity.x - impulse1 * nx,
                        newCueBall.velocity.y - impulse1 * ny
                    )
                )
                
                // Separate balls to prevent overlap
                val overlap = minDist - distance
                newCueBall = newCueBall.copy(
                    position = PointF(
                        newCueBall.position.x - overlap * nx * 0.5f,
                        newCueBall.position.y - overlap * ny * 0.5f
                    )
                )
            }
        }
        
        return newCueBall
    }
    
    /**
     * Predict path to pocket with game-accurate physics
     * Based on actual game pocket attraction mechanics
     */
    fun predictPocketPath(
        cueBall: Ball,
        pocketPosition: PointF,
        table: TableDimensions
    ): List<TrajectoryPoint> {
        val trajectory = mutableListOf<TrajectoryPoint>()
        var currentBall = cueBall.copy()
        var currentTime = 0f
        
        // Calculate direction to pocket
        val dx = pocketPosition.x - cueBall.position.x
        val dy = pocketPosition.y - cueBall.position.y
        val distance = sqrt(dx * dx + dy * dy)
        
        if (distance > 0) {
            val direction = PointF(dx / distance, dy / distance)
            currentBall = currentBall.copy(
                velocity = PointF(
                    direction.x * cueBall.velocity.length(),
                    direction.y * cueBall.velocity.length()
                )
            )
        }
        
        trajectory.add(TrajectoryPoint(currentBall.position, currentTime))
        
        while (currentTime < 3.0f && currentBall.velocity.length() > MIN_VELOCITY) {
            // Apply game-accurate physics
            currentBall = applyGamePhysics(currentBall, TIME_PER_TICK)
            
            // Apply pocket attraction (from game: 120.0 * time)
            val pocketDx = pocketPosition.x - currentBall.position.x
            val pocketDy = pocketPosition.y - currentBall.position.y
            val pocketDist = sqrt(pocketDx * pocketDx + pocketDy * pocketDy)
            
            if (pocketDist < table.pocketRadius * 2) {
                // Apply pocket attraction force from game
                val attractionForce = 120f * TIME_PER_TICK
                currentBall = currentBall.copy(
                    velocity = PointF(
                        currentBall.velocity.x + (pocketDx / pocketDist) * attractionForce,
                        currentBall.velocity.y + (pocketDy / pocketDist) * attractionForce
                    )
                )
            }
            
            val newPosition = PointF(
                currentBall.position.x + currentBall.velocity.x * TIME_PER_TICK,
                currentBall.position.y + currentBall.velocity.y * TIME_PER_TICK
            )
            
            currentBall = currentBall.copy(position = newPosition)
            currentTime += TIME_PER_TICK
            
            trajectory.add(TrajectoryPoint(currentBall.position, currentTime))
            
            // Check if reached pocket
            val distToPocket = sqrt(
                (newPosition.x - pocketPosition.x).pow(2) +
                (newPosition.y - pocketPosition.y).pow(2)
            )
            
            if (distToPocket < table.pocketRadius) {
                break
            }
        }
        
        return trajectory
    }
    
    /**
     * Get real game ball radius
     */
    fun getRealBallRadius(): Float = REAL_BALL_RADIUS
    
    /**
     * Get real game table dimensions
     */
    fun getRealTableDimensions(): Pair<Float, Float> = Pair(REAL_TABLE_WIDTH, REAL_TABLE_HEIGHT)
}
