package com.hadesloader.poolinjector.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.Toast
import com.hadesloader.poolinjector.physics.TrajectoryCalculator
import com.hadesloader.poolinjector.utils.ResolutionManager

class TrajectoryOverlayService : Service() {
    
    private lateinit var windowManager: WindowManager
    private lateinit var overlayView: TrajectoryOverlayView
    private lateinit var resolutionManager: ResolutionManager
    private lateinit var trajectoryCalculator: TrajectoryCalculator
    
    private var isServiceRunning = false
    
    companion object {
        const val CHANNEL_ID = "TrajectoryOverlayChannel"
        const val NOTIFICATION_ID = 1
        const val ACTION_START = "com.hadesloader.poolinjector.START_OVERLAY"
        const val ACTION_STOP = "com.hadesloader.poolinjector.STOP_OVERLAY"
    }
    
    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        resolutionManager = ResolutionManager(this)
        trajectoryCalculator = TrajectoryCalculator()
        createNotificationChannel()
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> startOverlay()
            ACTION_STOP -> stopOverlay()
        }
        return START_STICKY
    }
    
    private fun startOverlay() {
        if (isServiceRunning) return
        
        try {
            overlayView = TrajectoryOverlayView(this, resolutionManager, trajectoryCalculator)
            
            val params = WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                } else {
                    @Suppress("DEPRECATION")
                    WindowManager.LayoutParams.TYPE_PHONE
                },
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT
            )
            
            params.gravity = Gravity.TOP or Gravity.START
            windowManager.addView(overlayView, params)
            
            startForeground(NOTIFICATION_ID, createNotification())
            isServiceRunning = true
            
            Toast.makeText(this, "Trajectory overlay started", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(this, "Failed to start overlay: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
    
    private fun stopOverlay() {
        if (!isServiceRunning) return
        
        try {
            windowManager.removeView(overlayView)
            isServiceRunning = false
            stopForeground(true)
            stopSelf()
            
            Toast.makeText(this, "Trajectory overlay stopped", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(this, "Failed to stop overlay: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
    
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Trajectory Overlay Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows ball trajectory prediction"
            }
            
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }
    
    private fun createNotification(): Notification {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(this, CHANNEL_ID)
                .setContentTitle("Trajectory Overlay")
                .setContentText("Ball trajectory prediction is active")
                .setSmallIcon(android.R.drawable.ic_menu_info_details)
                .setOngoing(true)
                .build()
        } else {
            @Suppress("DEPRECATION")
            Notification.Builder(this)
                .setContentTitle("Trajectory Overlay")
                .setContentText("Ball trajectory prediction is active")
                .setSmallIcon(android.R.drawable.ic_menu_info_details)
                .setOngoing(true)
                .build()
        }
    }
    
    override fun onBind(intent: Intent?): IBinder? = null
    
    override fun onDestroy() {
        super.onDestroy()
        if (isServiceRunning) {
            try {
                windowManager.removeView(overlayView)
            } catch (e: Exception) {
                // View already removed
            }
        }
    }
}

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
