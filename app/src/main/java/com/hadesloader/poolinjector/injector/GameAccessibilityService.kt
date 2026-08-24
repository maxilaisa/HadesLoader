package com.hadesloader.poolinjector.injector

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.graphics.Rect
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

class GameAccessibilityService : AccessibilityService() {
    
    companion object {
        private var instance: GameAccessibilityService? = null
        private var gameStateListener: ((GameState) -> Unit)? = null
        
        fun getInstance(): GameAccessibilityService? = instance
        
        fun setGameStateListener(listener: (GameState) -> Unit) {
            gameStateListener = listener
        }
    }
    
    data class GameState(
        val cueBallX: Float,
        val cueBallY: Float,
        val targetBalls: List<BallPosition>,
        val currentCueName: String,
        val shotPower: Float,
        val isAiming: Boolean
    )
    
    data class BallPosition(
        val x: Float,
        val y: Float,
        val color: String
    )
    
    private val targetPackage = "com.miniclip.eightballpool"
    private var currentGameState: GameState? = null
    
    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
    }
    
    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        event?.let {
            if (it.packageName == targetPackage) {
                when (it.eventType) {
                    AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED,
                    AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> {
                        analyzeGameScreen()
                    }
                }
            }
        }
    }
    
    override fun onInterrupt() {
        // Service interrupted
    }
    
    private fun analyzeGameScreen() {
        try {
            val rootNode = rootInActiveWindow ?: return
            
            // Detect if we're in game mode or menu
            val isGameActive = isGameActive(rootNode)
            
            if (isGameActive) {
                // Extract game state from UI elements
                val cueBallPos = findCueBallPosition(rootNode)
                val cueName = findCurrentCueName(rootNode)
                val power = findShotPower(rootNode)
                val targetBalls = findTargetBalls(rootNode)
                val isAiming = isAiming(rootNode)
                
                val gameState = GameState(
                    cueBallX = cueBallPos?.first ?: 0f,
                    cueBallY = cueBallPos?.second ?: 0f,
                    targetBalls = targetBalls,
                    currentCueName = cueName ?: "Standard Cue",
                    shotPower = power ?: 15f,
                    isAiming = isAiming
                )
                
                currentGameState = gameState
                gameStateListener?.invoke(gameState)
            }
            
            rootNode.recycle()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    private fun isGameActive(rootNode: AccessibilityNodeInfo): Boolean {
        // Check for game-specific UI elements
        return findNodeByText(rootNode, "8 Ball Pool") != null ||
               findNodeByContentDescription(rootNode, "table") != null
    }
    
    private fun findCueBallPosition(rootNode: AccessibilityNodeInfo): Pair<Float, Float>? {
        // Try to find cue ball based on position and visual characteristics
        val screenBounds = Rect()
        rootNode.getBoundsInScreen(screenBounds)
        
        // Search for white circular elements in the lower portion of screen
        val whiteElements = findNodesByColor(rootNode, "white")
        
        for (element in whiteElements) {
            val bounds = Rect()
            element.getBoundsInScreen(bounds)
            
            // Check if it's in the table area (typically center-lower portion)
            val tableArea = Rect(
                screenBounds.width() / 4,
                screenBounds.height() / 3,
                screenBounds.width() * 3 / 4,
                screenBounds.height() * 2 / 3
            )
            
            if (tableArea.contains(bounds.centerX(), bounds.centerY())) {
                // Calculate relative position (0-1 range)
                val relativeX = (bounds.centerX().toFloat() - screenBounds.left) / screenBounds.width()
                val relativeY = (bounds.centerY().toFloat() - screenBounds.top) / screenBounds.height()
                
                return Pair(relativeX, relativeY)
            }
        }
        
        return null
    }
    
    private fun findCurrentCueName(rootNode: AccessibilityNodeInfo): String? {
        // Look for cue name in equipment/cue selection UI
        val equipmentNode = findNodeByText(rootNode, "Cue") ?: 
                          findNodeByText(rootNode, "Equipment")
        
        if (equipmentNode != null) {
            // Search for cue name in nearby nodes
            val parent = equipmentNode.parent
            if (parent != null) {
                for (i in 0 until parent.childCount) {
                    val child = parent.getChild(i)
                    val text = child.text?.toString()
                    if (text != null && isCueName(text)) {
                        return text
                    }
                }
            }
        }
        
        // Try to find cue in stats panel
        val statsNodes = findNodesContainingText(rootNode, "Power", "Spin", "Force")
        for (node in statsNodes) {
            val parent = node.parent
            if (parent != null) {
                for (i in 0 until parent.childCount) {
                    val child = parent.getChild(i)
                    val text = child.text?.toString()
                    if (text != null && isCueName(text)) {
                        return text
                    }
                }
            }
        }
        
        return null
    }
    
    private fun isCueName(text: String): Boolean {
        val knownCues = listOf("Standard", "Pro", "Legendary", "Archangel", "Galaxy", 
                              "Infinity", "Black Hole", "Ice", "Fire", "Lightning",
                              "Platinum", "Golden", "Diamond", "Dragon", "Phoenix")
        return knownCues.any { text.contains(it, ignoreCase = true) }
    }
    
    private fun findShotPower(rootNode: AccessibilityNodeInfo): Float? {
        // Look for power indicator/slider
        val powerNode = findNodeByText(rootNode, "Power") ?: 
                       findNodeByContentDescription(rootNode, "power")
        
        if (powerNode != null) {
            // Try to get power value from nearby progress bar or text
            val parent = powerNode.parent
            if (parent != null) {
                for (i in 0 until parent.childCount) {
                    val child = parent.getChild(i)
                    // Check if it's a progress bar by checking className
                    if (child.className?.toString()?.contains("ProgressBar") == true) {
                        // Get progress value
                        val range = child.rangeInfo
                        if (range != null) {
                            val current = range.current
                            val max = range.max
                            return (current.toFloat() / max.toFloat() * 30f) // Scale to 0-30
                        }
                    }
                    
                    val text = child.text?.toString()
                    if (text != null && text.matches(Regex("\\d+"))) {
                        return text.toFloatOrNull()
                    }
                }
            }
        }
        
        return null
    }
    
    private fun findTargetBalls(rootNode: AccessibilityNodeInfo): List<BallPosition> {
        val balls = mutableListOf<BallPosition>()
        
        // Find colored circular elements in table area
        val screenBounds = Rect()
        rootNode.getBoundsInScreen(screenBounds)
        
        val tableArea = Rect(
            screenBounds.width() / 4,
            screenBounds.height() / 3,
            screenBounds.width() * 3 / 4,
            screenBounds.height() * 2 / 3
        )
        
        // Search for common ball colors
        val ballColors = listOf("yellow", "blue", "red", "purple", "orange", "green", "maroon", "black")
        
        for (color in ballColors) {
            val colorNodes = findNodesByColor(rootNode, color)
            for (node in colorNodes) {
                val bounds = Rect()
                node.getBoundsInScreen(bounds)
                
                if (tableArea.contains(bounds.centerX(), bounds.centerY())) {
                    val relativeX = (bounds.centerX().toFloat() - screenBounds.left) / screenBounds.width()
                    val relativeY = (bounds.centerY().toFloat() - screenBounds.top) / screenBounds.height()
                    
                    balls.add(BallPosition(relativeX, relativeY, color))
                }
            }
        }
        
        return balls
    }
    
    private fun isAiming(rootNode: AccessibilityNodeInfo): Boolean {
        // Check if aiming UI is visible (cue stick, aim line, etc.)
        return findNodeByContentDescription(rootNode, "cue") != null ||
               findNodeByContentDescription(rootNode, "aim") != null ||
               findNodeByText(rootNode, "Aim") != null
    }
    
    private fun findNodeByText(rootNode: AccessibilityNodeInfo, text: String): AccessibilityNodeInfo? {
        val nodes = rootNode.findAccessibilityNodeInfosByText(text)
        return if (nodes.isNotEmpty()) nodes[0] else null
    }
    
    private fun findNodesContainingText(rootNode: AccessibilityNodeInfo, vararg texts: String): List<AccessibilityNodeInfo> {
        val result = mutableListOf<AccessibilityNodeInfo>()
        
        for (text in texts) {
            val nodes = rootNode.findAccessibilityNodeInfosByText(text)
            result.addAll(nodes)
        }
        
        return result
    }
    
    private fun findNodeByContentDescription(rootNode: AccessibilityNodeInfo, description: String): AccessibilityNodeInfo? {
        val nodes = rootNode.findAccessibilityNodeInfosByViewId(description)
        // Alternative: iterate through nodes and check contentDescription
        return if (nodes.isNotEmpty()) nodes[0] else null
    }
    
    private fun findNodesByColor(rootNode: AccessibilityNodeInfo, color: String): List<AccessibilityNodeInfo> {
        // This is a simplified version - real implementation would need image analysis
        // For now, return empty list as accessibility services don't directly provide color info
        return emptyList()
    }
    
    override fun onDestroy() {
        super.onDestroy()
        instance = null
    }
}