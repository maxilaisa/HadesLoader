package com.hadesloader.poolinjector.utils

import android.content.Context
import android.graphics.Point
import android.view.Display
import android.view.WindowManager
import kotlin.math.min

/**
 * Resolution Manager with real 8 Ball Pool table dimensions
 * Based on actual game source code analysis
 */
class ResolutionManager(private val context: Context) {
    
    data class ScreenMetrics(
        val width: Int,
        val height: Int,
        val density: Float,
        val scaledDensity: Float,
        val dpi: Int
    )
    
    data class TableMetrics(
        val tableWidth: Float,
        val tableHeight: Float,
        val ballRadius: Float,
        val cushionWidth: Float,
        val pocketRadius: Float,
        val scaleX: Float,
        val scaleY: Float
    )
    
    // Base reference resolution (1080x1920 portrait)
    private val baseWidth = 1080f
    private val baseHeight = 1920f
    
    // Real game table dimensions from 8 Ball Pool source (Prediction.cpp)
    // Table bounds: x from -127.0 to 127.0 (width 254), y from -63.5 to 63.5 (height 127)
    private val baseTableWidth = 254f
    private val baseTableHeight = 127f
    
    // Real ball radius from game (Ball.h: 3.800475)
    private val baseBallRadius = 3.800475f
    
    // Cushion width from game analysis
    private val baseCushionWidth = 25f
    
    // Pocket radius (estimated based on game mechanics)
    private val basePocketRadius = 8f
    
    fun getScreenMetrics(): ScreenMetrics {
        val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val display = windowManager.defaultDisplay
        val size = Point()
        display.getSize(size)
        
        val metrics = context.resources.displayMetrics
        return ScreenMetrics(
            width = size.x,
            height = size.y,
            density = metrics.density,
            scaledDensity = metrics.scaledDensity,
            dpi = metrics.densityDpi
        )
    }
    
    fun calculateTableMetrics(): TableMetrics {
        val metrics = getScreenMetrics()
        
        // Calculate scale factors based on screen size
        val scaleX = metrics.width / baseWidth
        val scaleY = metrics.height / baseHeight
        val uniformScale = min(scaleX, scaleY)
        
        return TableMetrics(
            tableWidth = baseTableWidth * uniformScale,
            tableHeight = baseTableHeight * uniformScale,
            ballRadius = baseBallRadius * uniformScale,
            cushionWidth = baseCushionWidth * uniformScale,
            pocketRadius = basePocketRadius * uniformScale,
            scaleX = scaleX,
            scaleY = scaleY
        )
    }
    
    fun scaleCoordinate(x: Float, y: Float, fromMetrics: TableMetrics, toMetrics: TableMetrics): Pair<Float, Float> {
        val scaledX = x * (toMetrics.scaleX / fromMetrics.scaleX)
        val scaledY = y * (toMetrics.scaleY / fromMetrics.scaleY)
        return Pair(scaledX, scaledY)
    }
    
    fun getTableCenter(): Pair<Float, Float> {
        val metrics = getScreenMetrics()
        val tableMetrics = calculateTableMetrics()
        
        val centerX = (metrics.width - tableMetrics.tableWidth) / 2f + tableMetrics.tableWidth / 2f
        val centerY = (metrics.height - tableMetrics.tableHeight) / 2f + tableMetrics.tableHeight / 2f
        
        return Pair(centerX, centerY)
    }
    
    fun getPocketPositions(): List<Pair<Float, Float>> {
        val tableMetrics = calculateTableMetrics()
        val (centerX, centerY) = getTableCenter()
        
        val halfWidth = tableMetrics.tableWidth / 2f
        val halfHeight = tableMetrics.tableHeight / 2f
        val cushion = tableMetrics.cushionWidth
        
        return listOf(
            // Top-left
            Pair(centerX - halfWidth + cushion, centerY - halfHeight + cushion),
            // Top-center
            Pair(centerX, centerY - halfHeight + cushion),
            // Top-right
            Pair(centerX + halfWidth - cushion, centerY - halfHeight + cushion),
            // Bottom-left
            Pair(centerX - halfWidth + cushion, centerY + halfHeight - cushion),
            // Bottom-center
            Pair(centerX, centerY + halfHeight - cushion),
            // Bottom-right
            Pair(centerX + halfWidth - cushion, centerY + halfHeight - cushion)
        )
    }
    
    fun dpToPx(dp: Float): Float {
        val metrics = getScreenMetrics()
        return dp * metrics.density
    }
    
    fun pxToDp(px: Float): Float {
        val metrics = getScreenMetrics()
        return px / metrics.density
    }
    
    fun isPortrait(): Boolean {
        val metrics = getScreenMetrics()
        return metrics.height > metrics.width
    }
    
    fun getAspectRatio(): Float {
        val metrics = getScreenMetrics()
        return metrics.width.toFloat() / metrics.height.toFloat()
    }
}
