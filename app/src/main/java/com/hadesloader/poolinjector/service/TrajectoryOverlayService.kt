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
