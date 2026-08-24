package com.hadesloader.poolinjector.injector

import android.content.Context
import android.content.pm.PackageManager
import android.widget.Toast
import com.hadesloader.poolinjector.Constants
import java.io.File

/**
 * Handles mod installation and injection setup for 8 Ball Pool
 * This class manages the detection of the target game and prepares the injection environment
 */
class ModInstaller(private val context: Context) {
    
    companion object {
        private const val GAME_PACKAGE_NAME = Constants.GAME_PACKAGE_NAME
        private const val MIN_GAME_VERSION = Constants.MIN_GAME_VERSION
    }
    
    /**
     * Checks if the target game is installed and meets requirements
     */
    fun isGameAvailable(): GameStatus {
        return try {
            val packageInfo = context.packageManager.getPackageInfo(GAME_PACKAGE_NAME, 0)
            val versionName = packageInfo.versionName ?: "Unknown"
            
            if (isVersionCompatible(versionName)) {
                GameStatus.Available(versionName)
            } else {
                GameStatus.IncompatibleVersion(versionName, MIN_GAME_VERSION)
            }
        } catch (e: PackageManager.NameNotFoundException) {
            GameStatus.NotInstalled
        }
    }
    
    /**
     * Verifies if the game version is compatible with the mod
     */
    private fun isVersionCompatible(versionName: String): Boolean {
        return try {
            val currentVersion = versionName.split(".").map { it.toInt() }
            val minVersion = MIN_GAME_VERSION.split(".").map { it.toInt() }
            
            // Simple version comparison
            for (i in 0 until minOf(currentVersion.size, minVersion.size)) {
                if (currentVersion[i] > minVersion[i]) return true
                if (currentVersion[i] < minVersion[i]) return false
            }
            
            true
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Gets the game's APK path for potential injection
     */
    fun getGameApkPath(): String? {
        return try {
            val packageInfo = context.packageManager.getPackageInfo(GAME_PACKAGE_NAME, 0)
            packageInfo.applicationInfo?.sourceDir
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * Prepares the injection environment
     */
    fun prepareInjectionEnvironment(): Boolean {
        val gameStatus = isGameAvailable()
        
        return when (gameStatus) {
            is GameStatus.Available -> {
                true
            }
            is GameStatus.NotInstalled -> {
                false
            }
            is GameStatus.IncompatibleVersion -> {
                false
            }
        }
    }
    
    /**
     * Shows installation status to user
     */
    fun showInstallationStatus() {
        val status = isGameAvailable()
        
        when (status) {
            is GameStatus.Available -> {
                Toast.makeText(
                    context,
                    "8 Ball Pool ${status.version} detected - Ready for injection",
                    Toast.LENGTH_LONG
                ).show()
            }
            is GameStatus.NotInstalled -> {
                Toast.makeText(
                    context,
                    "8 Ball Pool not found. Please install the game first.",
                    Toast.LENGTH_LONG
                ).show()
            }
            is GameStatus.IncompatibleVersion -> {
                Toast.makeText(
                    context,
                    "Incompatible game version. Found: ${status.currentVersion}, Required: ${status.requiredVersion}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }
    
    /**
     * Sealed class representing different game installation statuses
     */
    sealed class GameStatus {
        data class Available(val version: String) : GameStatus()
        object NotInstalled : GameStatus()
        data class IncompatibleVersion(val currentVersion: String, val requiredVersion: String) : GameStatus()
    }
}