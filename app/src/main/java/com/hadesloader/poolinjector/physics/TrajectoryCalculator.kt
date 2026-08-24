package com.hadesloader.poolinjector.physics

/**
 * Physics engine for trajectory calculation
 * This will be injected into the game's physics system
 */
class TrajectoryCalculator {
    
    // Physics constants
    private val friction = 0.985f          // Ball deceleration
    private val cushionRestitution = 0.9f  // Cushion bounce
    private val ballRestitution = 0.95f    // Ball collision
    private val timeStep = 0.016f          // 60 FPS simulation
    private val minVelocity = 0.1f         // Stop threshold
    
    /**
     * Calculate trajectory path for a given shot
     * This function will be called from within the game's physics engine
     */
    fun calculateTrajectory(
        startX: Float, 
        startY: Float, 
        velocityX: Float, 
        velocityY: Float,
        tableWidth: Float,
        tableHeight: Float,
        ballRadius: Float
    ): List<TrajectoryPoint> {
        val trajectory = mutableListOf<TrajectoryPoint>()
        
        var x = startX
        var y = startY
        var vx = velocityX
        var vy = velocityY
        
        trajectory.add(TrajectoryPoint(x, y, 0f))
        
        var time = 0f
        while (true) {
            // Apply velocity
            x += vx * timeStep
            y += vy * timeStep
            
            // Apply friction
            vx *= friction
            vy *= friction
            
            // Check cushion collisions
            if (x - ballRadius < 0) {
                x = ballRadius
                vx = -vx * cushionRestitution
            } else if (x + ballRadius > tableWidth) {
                x = tableWidth - ballRadius
                vx = -vx * cushionRestitution
            }
            
            if (y - ballRadius < 0) {
                y = ballRadius
                vy = -vy * cushionRestitution
            } else if (y + ballRadius > tableHeight) {
                y = tableHeight - ballRadius
                vy = -vy * cushionRestitution
            }
            
            // Check if ball stopped
            val speed = (vx * vx + vy * vy).toFloat()
            if (speed < minVelocity * minVelocity) {
                break
            }
            
            time += timeStep
            trajectory.add(TrajectoryPoint(x, y, time))
            
            // Limit trajectory length
            if (trajectory.size > 1000) break
        }
        
        return trajectory
    }
    
    data class TrajectoryPoint(
        val x: Float,
        val y: Float,
        val time: Float
    )
}