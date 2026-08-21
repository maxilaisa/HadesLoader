package com.hadesloader.poolinjector

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.SeekBar
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.hadesloader.poolinjector.service.TrajectoryOverlayService
import com.hadesloader.poolinjector.utils.ResolutionManager

class MainActivity : AppCompatActivity() {
    
    private lateinit var resolutionManager: ResolutionManager
    private lateinit var startButton: Button
    private lateinit var stopButton: Button
    private lateinit var powerSeekBar: SeekBar
    private lateinit var powerTextView: TextView
    private lateinit var resolutionTextView: TextView
    private lateinit var trajectorySwitch: Switch
    
    private var isServiceRunning = false
    private var currentPower = 15f
    
    companion object {
        private const val OVERLAY_PERMISSION_REQUEST_CODE = 1234
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        resolutionManager = ResolutionManager(this)
        initViews()
        updateResolutionInfo()
    }
    
    private fun initViews() {
        startButton = findViewById(R.id.startButton)
        stopButton = findViewById(R.id.stopButton)
        powerSeekBar = findViewById(R.id.powerSeekBar)
        powerTextView = findViewById(R.id.powerTextView)
        resolutionTextView = findViewById(R.id.resolutionTextView)
        trajectorySwitch = findViewById(R.id.trajectorySwitch)
        
        startButton.setOnClickListener {
            if (checkOverlayPermission()) {
                startTrajectoryService()
            } else {
                requestOverlayPermission()
            }
        }
        
        stopButton.setOnClickListener {
            stopTrajectoryService()
        }
        
        powerSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                currentPower = progress.toFloat() / 10f
                powerTextView.text = "Power: $currentPower"
            }
            
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
        
        trajectorySwitch.setOnCheckedChangeListener { _, isChecked ->
            // This will be used to toggle trajectory visibility
            Toast.makeText(this, "Trajectory visibility: ${if (isChecked) "ON" else "OFF"}", Toast.LENGTH_SHORT).show()
        }
        
        updateButtonStates()
    }
    
    private fun updateResolutionInfo() {
        val metrics = resolutionManager.getScreenMetrics()
        val tableMetrics = resolutionManager.calculateTableMetrics()
        
        val info = """
            Screen: ${metrics.width}x${metrics.height}
            DPI: ${metrics.dpi}
            Density: ${metrics.density}
            Table: ${tableMetrics.tableWidth.toInt()}x${tableMetrics.tableHeight.toInt()}
            Ball Radius: ${tableMetrics.ballRadius.toInt()}px
            Aspect Ratio: ${"%.2f".format(resolutionManager.getAspectRatio())}
            Orientation: ${if (resolutionManager.isPortrait()) "Portrait" else "Landscape"}
        """.trimIndent()
        
        resolutionTextView.text = info
    }
    
    private fun checkOverlayPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Settings.canDrawOverlays(this)
        } else {
            true
        }
    }
    
    private fun requestOverlayPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")
            )
            startActivityForResult(intent, OVERLAY_PERMISSION_REQUEST_CODE)
        }
    }
    
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == OVERLAY_PERMISSION_REQUEST_CODE) {
            if (checkOverlayPermission()) {
                startTrajectoryService()
            } else {
                Toast.makeText(this, "Overlay permission denied", Toast.LENGTH_LONG).show()
            }
        }
    }
    
    private fun startTrajectoryService() {
        val intent = Intent(this, TrajectoryOverlayService::class.java).apply {
            action = TrajectoryOverlayService.ACTION_START
        }
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
        
        isServiceRunning = true
        updateButtonStates()
        Toast.makeText(this, "Trajectory service started", Toast.LENGTH_SHORT).show()
    }
    
    private fun stopTrajectoryService() {
        val intent = Intent(this, TrajectoryOverlayService::class.java).apply {
            action = TrajectoryOverlayService.ACTION_STOP
        }
        startService(intent)
        
        isServiceRunning = false
        updateButtonStates()
        Toast.makeText(this, "Trajectory service stopped", Toast.LENGTH_SHORT).show()
    }
    
    private fun updateButtonStates() {
        startButton.isEnabled = !isServiceRunning
        stopButton.isEnabled = isServiceRunning
    }
    
    override fun onResume() {
        super.onResume()
        updateResolutionInfo()
    }
}
