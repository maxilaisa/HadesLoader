package com.hadesloader.poolinjector.injector

import android.content.Context
import android.os.Build
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader

private fun ByteArray.contains(subarray: ByteArray): Boolean {
    if (subarray.isEmpty()) return true
    if (this.size < subarray.size) return false
    
    for (i in 0..(this.size - subarray.size)) {
        var match = true
        for (j in subarray.indices) {
            if (this[i + j] != subarray[j]) {
                match = false
                break
            }
        }
        if (match) return true
    }
    return false
}

class GameMemoryReader(private val context: Context) {
    
    data class GameState(
        val cueBallX: Float,
        val cueBallY: Float,
        val targetBalls: List<BallPosition>,
        val tableWidth: Float,
        val tableHeight: Float,
        val currentCueId: String,
        val shotPower: Float,
        val shotAngle: Float
    )
    
    data class BallPosition(
        val x: Float,
        val y: Float,
        val ballId: Int
    )
    
    private val targetPackageName = "com.miniclip.eightballpool"
    
    fun getGameState(): GameState? {
        return try {
            val pid = getGameProcessId() ?: return null
            
            // Read memory maps to find game data locations
            val memoryRegions = getMemoryRegions(pid)
            
            // Find cue ball position in memory
            val cueBallPos = findCueBallPosition(pid, memoryRegions)
            
            // Find current cue ID
            val cueId = findCurrentCueId(pid, memoryRegions)
            
            // Find shot power and angle
            val shotPower = findShotPower(pid, memoryRegions)
            val shotAngle = findShotAngle(pid, memoryRegions)
            
            // Find table dimensions
            val tableDims = findTableDimensions(pid, memoryRegions)
            
            // Find target balls
            val targetBalls = findTargetBalls(pid, memoryRegions)
            
            GameState(
                cueBallX = cueBallPos?.first ?: 0f,
                cueBallY = cueBallPos?.second ?: 0f,
                targetBalls = targetBalls,
                tableWidth = tableDims?.first ?: 254f,
                tableHeight = tableDims?.second ?: 127f,
                currentCueId = cueId ?: "standard",
                shotPower = shotPower ?: 15f,
                shotAngle = shotAngle ?: 0f
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    
    private fun getGameProcessId(): Int? {
        return try {
            val process = Runtime.getRuntime().exec("ps")
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            
            var pid: Int? = null
            var line: String?
            
            while (reader.readLine().also { line = it } != null) {
                if (line!!.contains(targetPackageName)) {
                    val parts = line!!.trim().split("\\s+".toRegex())
                    if (parts.isNotEmpty()) {
                        pid = parts[0].toIntOrNull()
                        break
                    }
                }
            }
            
            reader.close()
            pid
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    
    private fun getMemoryRegions(pid: Int): List<MemoryRegion> {
        val regions = mutableListOf<MemoryRegion>()
        
        try {
            val process = Runtime.getRuntime().exec("cat /proc/$pid/maps")
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                val parts = line!!.trim().split("\\s+".toRegex())
                if (parts.size >= 6) {
                    val addressRange = parts[0].split("-")
                    if (addressRange.size == 2) {
                        regions.add(MemoryRegion(
                            start = addressRange[0].toLong(16),
                            end = addressRange[1].toLong(16),
                            permissions = parts[1],
                            name = if (parts.size > 5) parts[5] else ""
                        ))
                    }
                }
            }
            
            reader.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        
        return regions
    }
    
    private fun findCueBallPosition(pid: Int, regions: List<MemoryRegion>): Pair<Float, Float>? {
        // Search for cue ball position in heap memory regions
        for (region in regions) {
            if (region.permissions.contains("rw") && region.name.contains("heap")) {
                val data = readMemory(pid, region.start, region.end - region.start)
                if (data != null) {
                    // Pattern matching for ball coordinates (typical float values in valid range)
                    val positions = findFloatPairs(data, -127f..127f, -63.5f..63.5f)
                    if (positions.isNotEmpty()) {
                        return positions.first()
                    }
                }
            }
        }
        return null
    }
    
    private fun findCurrentCueId(pid: Int, regions: List<MemoryRegion>): String? {
        // Search for cue ID string in memory
        for (region in regions) {
            if (region.permissions.contains("rw")) {
                val data = readMemory(pid, region.start, region.end - region.start)
                if (data != null) {
                    // Look for known cue IDs in memory
                    val knownCues = listOf("standard", "pro", "legendary", "archangel", "galaxy", 
                                         "infinity", "blackhole", "ice", "fire", "lightning")
                    
                    for (cue in knownCues) {
                        val cueBytes = cue.toByteArray()
                        if (data.contains(cueBytes)) {
                            return cue
                        }
                    }
                }
            }
        }
        return null
    }
    
    private fun findShotPower(pid: Int, regions: List<MemoryRegion>): Float? {
        // Search for shot power value (typically 0-30 range)
        for (region in regions) {
            if (region.permissions.contains("rw")) {
                val data = readMemory(pid, region.start, region.end - region.start)
                if (data != null) {
                    val powers = findFloats(data, 0f..30f)
                    if (powers.isNotEmpty()) {
                        return powers.first()
                    }
                }
            }
        }
        return null
    }
    
    private fun findShotAngle(pid: Int, regions: List<MemoryRegion>): Float? {
        // Search for shot angle value (typically 0-2PI range)
        for (region in regions) {
            if (region.permissions.contains("rw")) {
                val data = readMemory(pid, region.start, region.end - region.start)
                if (data != null) {
                    val angles = findFloats(data, 0f..(2 * Math.PI).toFloat())
                    if (angles.isNotEmpty()) {
                        return angles.first()
                    }
                }
            }
        }
        return null
    }
    
    private fun findTableDimensions(pid: Int, regions: List<MemoryRegion>): Pair<Float, Float>? {
        // Search for table dimensions (typically 254x127 for 8 Ball Pool)
        for (region in regions) {
            if (region.permissions.contains("rw")) {
                val data = readMemory(pid, region.start, region.end - region.start)
                if (data != null) {
                    val widths = findFloats(data, 250f..260f)
                    val heights = findFloats(data, 125f..130f)
                    
                    if (widths.isNotEmpty() && heights.isNotEmpty()) {
                        return Pair(widths.first(), heights.first())
                    }
                }
            }
        }
        return null
    }
    
    private fun findTargetBalls(pid: Int, regions: List<MemoryRegion>): List<BallPosition> {
        val balls = mutableListOf<BallPosition>()
        
        for (region in regions) {
            if (region.permissions.contains("rw") && region.name.contains("heap")) {
                val data = readMemory(pid, region.start, region.end - region.start)
                if (data != null) {
                    // Find all valid ball positions
                    val positions = findFloatPairs(data, -127f..127f, -63.5f..63.5f)
                    
                    var ballId = 1
                    for (pos in positions) {
                        if (pos != Pair(0f, 0f)) { // Skip origin
                            balls.add(BallPosition(pos.first, pos.second, ballId++))
                        }
                    }
                }
            }
        }
        
        return balls
    }
    
    private fun readMemory(pid: Int, address: Long, size: Long): ByteArray? {
        return try {
            val process = Runtime.getRuntime().exec("su -c cat /proc/$pid/mem")
            // Note: This requires root access
            // For non-root, we'll need alternative approach
            null
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    
    private fun findFloats(data: ByteArray, range: ClosedFloatingPointRange<Float>): List<Float> {
        val floats = mutableListOf<Float>()
        
        for (i in data.indices step 4) {
            if (i + 4 <= data.size) {
                val bytes = data.copyOfRange(i, i + 4)
                val float = java.nio.ByteBuffer.wrap(bytes).order(java.nio.ByteOrder.LITTLE_ENDIAN).float
                
                if (float in range) {
                    floats.add(float)
                }
            }
        }
        
        return floats
    }
    
    private fun findFloatPairs(data: ByteArray, xRange: ClosedFloatingPointRange<Float>, 
                              yRange: ClosedFloatingPointRange<Float>): List<Pair<Float, Float>> {
        val pairs = mutableListOf<Pair<Float, Float>>()
        
        for (i in data.indices step 8) {
            if (i + 8 <= data.size) {
                val xBytes = data.copyOfRange(i, i + 4)
                val yBytes = data.copyOfRange(i + 4, i + 8)
                
                val x = java.nio.ByteBuffer.wrap(xBytes).order(java.nio.ByteOrder.LITTLE_ENDIAN).float
                val y = java.nio.ByteBuffer.wrap(yBytes).order(java.nio.ByteOrder.LITTLE_ENDIAN).float
                
                if (x in xRange && y in yRange) {
                    pairs.add(Pair(x, y))
                }
            }
        }
        
        return pairs
    }
    
    data class MemoryRegion(
        val start: Long,
        val end: Long,
        val permissions: String,
        val name: String
    )
}