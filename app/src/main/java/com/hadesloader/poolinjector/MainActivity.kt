package com.hadesloader.poolinjector

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.hadesloader.poolinjector.injector.ApkModifier
import com.hadesloader.poolinjector.injector.ModInstaller
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() {
    
    private lateinit var modInstaller: ModInstaller
    private lateinit var apkModifier: ApkModifier
    private lateinit var injectButton: Button
    private lateinit var installButton: Button
    private lateinit var cancelButton: Button
    private lateinit var gameStatusTextView: TextView
    private lateinit var progressTextView: TextView
    private lateinit var progressBar: ProgressBar
    
    private var injectionInProgress = false
    private var currentModdedApkPath: String? = null
    
    companion object {
        private const val INSTALL_REQUEST_CODE = 1001
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        modInstaller = ModInstaller(this)
        apkModifier = ApkModifier(this)
        
        initViews()
        checkGameInstallation()
    }
    
    private fun initViews() {
        injectButton = findViewById(R.id.injectButton)
        installButton = findViewById(R.id.installButton)
        cancelButton = findViewById(R.id.cancelButton)
        gameStatusTextView = findViewById(R.id.gameStatusTextView)
        progressTextView = findViewById(R.id.progressTextView)
        progressBar = findViewById(R.id.progressBar)
        
        injectButton.setOnClickListener {
            startInjection()
        }
        
        installButton.setOnClickListener {
            currentModdedApkPath?.let { path ->
                installModdedApk(path)
            }
        }
        
        cancelButton.setOnClickListener {
            cancelInjection()
        }
        
        updateButtonStates()
    }
    
    private fun checkGameInstallation() {
        val gameStatus = modInstaller.isGameAvailable()
        
        when (gameStatus) {
            is ModInstaller.GameStatus.Available -> {
                gameStatusTextView.text = "Game Status: 8 Ball Pool ${gameStatus.version} - Ready for injection"
                gameStatusTextView.setTextColor(android.graphics.Color.parseColor("#00FF00"))
                Toast.makeText(
                    this,
                    "8 Ball Pool ${gameStatus.version} detected - Ready for injection",
                    Toast.LENGTH_SHORT
                ).show()
            }
            is ModInstaller.GameStatus.NotInstalled -> {
                gameStatusTextView.text = "Game Status: 8 Ball Pool not found"
                gameStatusTextView.setTextColor(android.graphics.Color.parseColor("#FF0000"))
                Toast.makeText(
                    this,
                    "8 Ball Pool not found. Please install the game first.",
                    Toast.LENGTH_LONG
                ).show()
            }
            is ModInstaller.GameStatus.IncompatibleVersion -> {
                gameStatusTextView.text = "Game Status: Incompatible version (${gameStatus.currentVersion})"
                gameStatusTextView.setTextColor(android.graphics.Color.parseColor("#FFA500"))
                Toast.makeText(
                    this,
                    "Incompatible game version. Found: ${gameStatus.currentVersion}, Required: ${gameStatus.requiredVersion}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
        
        updateButtonStates()
    }
    
    private fun startInjection() {
        if (injectionInProgress) return
        
        val gameStatus = modInstaller.isGameAvailable()
        if (gameStatus !is ModInstaller.GameStatus.Available) {
            Toast.makeText(this, "Game not available for injection", Toast.LENGTH_LONG).show()
            return
        }
        
        injectionInProgress = true
        updateButtonStates()
        progressTextView.text = "Starting injection..."
        progressBar.visibility = ProgressBar.VISIBLE
        
        CoroutineScope(Dispatchers.IO).launch {
            progressTextView.text = "Extracting game APK..."
            
            val result = apkModifier.injectTrajectory()
            
            withContext(Dispatchers.Main) {
                when (result) {
                    is ApkModifier.InjectionResult.Success -> {
                        currentModdedApkPath = result.moddedApkPath
                        progressTextView.text = "Injection complete! Ready to install."
                        progressBar.visibility = ProgressBar.GONE
                        installButton.isEnabled = true
                        Toast.makeText(this@MainActivity, "Injection successful!", Toast.LENGTH_LONG).show()
                    }
                    is ApkModifier.InjectionResult.Failed -> {
                        progressTextView.text = "Injection failed: ${result.reason}"
                        progressBar.visibility = ProgressBar.GONE
                        Toast.makeText(this@MainActivity, "Injection failed: ${result.reason}", Toast.LENGTH_LONG).show()
                    }
                }
                
                injectionInProgress = false
                updateButtonStates()
            }
        }
    }
    
    private fun installModdedApk(apkPath: String) {
        progressTextView.text = "Preparing installation..."
        
        if (apkModifier.installModdedApk(apkPath)) {
            progressTextView.text = "Installation prompt shown. Complete installation to finish."
            Toast.makeText(this, "Install the modded APK when prompted", Toast.LENGTH_LONG).show()
        } else {
            progressTextView.text = "Installation failed"
            Toast.makeText(this, "Failed to start installation", Toast.LENGTH_LONG).show()
        }
    }
    
    private fun cancelInjection() {
        if (injectionInProgress) {
            injectionInProgress = false
            apkModifier.cleanup()
            progressTextView.text = "Injection cancelled"
            progressBar.visibility = ProgressBar.GONE
            updateButtonStates()
        }
    }
    
    private fun updateButtonStates() {
        val gameAvailable = modInstaller.isGameAvailable() is ModInstaller.GameStatus.Available
        injectButton.isEnabled = !injectionInProgress && gameAvailable
        installButton.isEnabled = currentModdedApkPath != null && !injectionInProgress
        cancelButton.isEnabled = injectionInProgress
    }
    
    override fun onResume() {
        super.onResume()
        checkGameInstallation()
    }
    
    override fun onDestroy() {
        super.onDestroy()
        apkModifier.cleanup()
    }
}