package com.hadesloader.poolinjector.service

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.view.View
import com.hadesloader.poolinjector.physics.TrajectoryCalculator
import com.hadesloader.poolinjector.utils.ResolutionManager
import kotlin.math.cos
import kotlin.math.sin

class TrajectoryOverlayView(
    context: Context,
    private val resolutionManager: ResolutionManager,
    private val trajectoryCalculator: TrajectoryCalculator
) : View(context) {

    private val trajectoryPaint = Paint().apply {
        color = Color.RED
        strokeWidth = 3f
        style = Paint.Style.STROKE
        alpha = 200
    }

    private val dashedPaint = Paint().apply {
        color = Color.GREEN
        strokeWidth = 2f
        style = Paint.Style.STROKE
        alpha = 150
        pathEffect = android.graphics.DashPathEffect(floatArrayOf(10f, 10f), 0f)
    }

    private val pocketPaint = Paint().apply {
        color = Color.BLUE
        style = Paint.Style.STROKE
        strokeWidth = 2f
        alpha = 100
    }

    private val ballPaint = Paint().apply {
        color = Color.WHITE
        style = Paint.Style.FILL
        alpha = 180
    }

    private var cueBallPosition = android.graphics.PointF(0f, 0f)
    private var aimAngle = 0f
    private var power = 15f
    private var showTrajectory = true

    init {
        // Initialize with default positions
        val tableMetrics = resolutionManager.calculateTableMetrics()
        val (centerX, centerY) = resolutionManager.getTableCenter()
        cueBallPosition = android.graphics.PointF(centerX, centerY - tableMetrics.tableHeight / 4f)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        if (!showTrajectory) return

        val tableMetrics = resolutionManager.calculateTableMetrics()
        val tableDimensions = TrajectoryCalculator.TableDimensions(
            tableMetrics.tableWidth,
            tableMetrics.tableHeight,
            tableMetrics.cushionWidth,
            tableMetrics.pocketRadius
        )

        // Draw pocket positions
        val pockets = resolutionManager.getPocketPositions()
        for (pocket in pockets) {
            canvas.drawCircle(
                pocket.first,
                pocket.second,
                tableMetrics.pocketRadius,
                pocketPaint
            )
        }

        // Calculate and draw trajectory
        val trajectory = trajectoryCalculator.calculateAimTrajectory(
            cueBallPosition,
            aimAngle,
            power,
            emptyList(),
            tableDimensions
        )

        // Draw trajectory line
        if (trajectory.size > 1) {
            val path = android.graphics.Path()
            path.moveTo(trajectory[0].position.x, trajectory[0].position.y)

            for (i in 1 until trajectory.size) {
                path.lineTo(trajectory[i].position.x, trajectory[i].position.y)
            }

            canvas.drawPath(path, trajectoryPaint)
        }

        // Draw cue ball
        canvas.drawCircle(
            cueBallPosition.x,
            cueBallPosition.y,
            tableMetrics.ballRadius,
            ballPaint
        )

        // Draw aim direction indicator
        val aimLength = 50f
        val endX = cueBallPosition.x + cos(aimAngle) * aimLength
        val endY = cueBallPosition.y + sin(aimAngle) * aimLength

        val aimPath = android.graphics.Path()
        aimPath.moveTo(cueBallPosition.x, cueBallPosition.y)
        aimPath.lineTo(endX, endY)
        canvas.drawPath(aimPath, dashedPaint)
    }

    fun updateAim(angle: Float, powerLevel: Float) {
        aimAngle = angle
        power = powerLevel
        invalidate()
    }

    fun setCueBallPosition(x: Float, y: Float) {
        cueBallPosition = android.graphics.PointF(x, y)
        invalidate()
    }

    fun toggleTrajectory() {
        showTrajectory = !showTrajectory
        invalidate()
    }
}