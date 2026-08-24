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
import com.hadesloader.poolinjector.cue.CueDetector
import com.hadesloader.poolinjector.injector.GameAccessibilityService
import com.hadesloader.poolinjector.service.TrajectoryOverlayService
import com.hadesloader.poolinjector.utils.ResolutionManager

class MainActivity : AppCompatActivity() {
    
    private lateinit var resolutionManager: ResolutionManager
    private lateinit var cueDetector: CueDetector
    private lateinit var startButton: Button
    private lateinit var stopButton: Button
    private lateinit var powerSeekBar: SeekBar
    private lateinit var powerTextView: TextView
    private lateinit var resolutionTextView: TextView
    private lateinit var trajectorySwitch: Switch
    private lateinit var currentCueTextView: TextView
    
    private var isServiceRunning = false
    private var currentPower = 15f
    private var currentCue = "Standard Cue"
    
    companion object {
        private const val OVERLAY_PERMISSION_REQUEST_CODE = 1234
        private const val ACCESSIBILITY_PERMISSION_REQUEST_CODE = 1235
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        resolutionManager = ResolutionManager(this)
        cueDetector = CueDetector(this)
        
        initViews()
        setupCueDetection()
        updateResolutionInfo()
    }
    
    private fun initViews() {
        startButton = findViewById(R.id.startButton)
        stopButton = findViewById(R.id.stopButton)
        powerSeekBar = findViewById(R.id.powerSeekBar)
        powerTextView = findViewById(R.id.powerTextView)
        resolutionTextView = findViewById(R.id.resolutionTextView)
        trajectorySwitch = findViewById(R.id.trajectorySwitch)
        currentCueTextView = findViewById(R.id.currentCueTextView)
        
        startButton.setOnClickListener {
            if (checkOverlayPermission() && checkAccessibilityPermission()) {
                startTrajectoryService()
            } else {
                if (!checkOverlayPermission()) {
                    requestOverlayPermission()
                } else {
                    requestAccessibilityPermission()
                }
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
            Toast.makeText(this, "Trajectory visibility: ${if (isChecked) "ON" else "OFF"}", Toast.LENGTH_SHORT).show()
        }
        
        updateButtonStates()
    }
    
    private fun setupCueDetection() {
        // Set up automatic cue detection from accessibility service
        GameAccessibilityService.setGameStateListener { gameState ->
            currentCue = gameState.currentCueName
            runOnUiThread {
                currentCueTextView.text = "Current Cue: $currentCue"
                
                // Update power based on detected cue
                val cueStats = cueDetector.getCueByName(currentCue)
                if (cueStats != null) {
                    val adjustedPower = currentPower * cueStats.powerMultiplier
                    powerTextView.text = "Power: $currentPower (Effective: ${"%.1f".format(adjustedPower)})"
                }
            }
        }
        
        cueDetector.setCueDetectionCallback { cueStats ->
            currentCue = cueStats.name
            runOnUiThread {
                currentCueTextView.text = "Current Cue: $currentCue"
                val adjustedPower = currentPower * cueStats.powerMultiplier
                powerTextView.text = "Power: $currentPower (Effective: ${"%.1f".format(adjustedPower)})"
            }
        }
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
    
    private fun checkAccessibilityPermission(): Boolean {
        val accessibilityEnabled = try {
            Settings.Secure.getInt(
                contentResolver,
                Settings.Secure.ACCESSIBILITY_ENABLED
            ) == 1
        } catch (e: Settings.SettingNotFoundException) {
            false
        }
        
        if (accessibilityEnabled) {
            val services = Settings.Secure.getString(
                contentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            )
            return services?.contains(packageName) == true
        }
        
        return false
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
    
    private fun requestAccessibilityPermission() {
        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
        startActivityForResult(intent, ACCESSIBILITY_PERMISSION_REQUEST_CODE)
    }
    
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            OVERLAY_PERMISSION_REQUEST_CODE -> {
                if (checkOverlayPermission()) {
                    if (checkAccessibilityPermission()) {
                        startTrajectoryService()
                    } else {
                        requestAccessibilityPermission()
                    }
                } else {
                    Toast.makeText(this, "Overlay permission denied", Toast.LENGTH_LONG).show()
                }
            }
            ACCESSIBILITY_PERMISSION_REQUEST_CODE -> {
                if (checkAccessibilityPermission()) {
                    if (checkOverlayPermission()) {
                        startTrajectoryService()
                    } else {
                        requestOverlayPermission()
                    }
                } else {
                    Toast.makeText(this, "Accessibility permission denied", Toast.LENGTH_LONG).show()
                }
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
