package com.nhlstenden.appdev.features.rewards.components

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import kotlin.math.sqrt

class ShakeDetector(
    context: Context,
    private val onShakeDetected: () -> Unit,
    private val shakeThreshold: Float = 12f,
    private val shakeCooldownMs: Long = 1000L
) : SensorEventListener, DefaultLifecycleObserver {
    
    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    
    private var lastShakeTime: Long = 0
    private var lastX = 0f
    private var lastY = 0f
    private var lastZ = 0f
    
    override fun onResume(owner: LifecycleOwner) {
        super.onResume(owner)
        accelerometer?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
        }
    }
    
    override fun onPause(owner: LifecycleOwner) {
        super.onPause(owner)
        sensorManager.unregisterListener(this)
    }
    
    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type == Sensor.TYPE_ACCELEROMETER) {
            val currentTime = System.currentTimeMillis()
            
            if (isFirstReading()) {
                updateLastValues(event.values)
                return
            }
            
            val deltaVector = calculateDeltaVector(event.values)
            
            if (isShakeDetected(deltaVector, currentTime)) {
                lastShakeTime = currentTime
                onShakeDetected()
            }
            
            updateLastValues(event.values)
        }
    }
    
    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // No implementation needed
    }
    
    private fun isFirstReading(): Boolean {
        return lastX == 0f && lastY == 0f && lastZ == 0f
    }
    
    private fun calculateDeltaVector(values: FloatArray): Float {
        val deltaX = values[0] - lastX
        val deltaY = values[1] - lastY
        val deltaZ = values[2] - lastZ
        
        return sqrt((deltaX * deltaX + deltaY * deltaY + deltaZ * deltaZ).toDouble()).toFloat()
    }
    
    private fun isShakeDetected(deltaVector: Float, currentTime: Long): Boolean {
        return deltaVector > shakeThreshold && currentTime - lastShakeTime > shakeCooldownMs
    }
    
    private fun updateLastValues(values: FloatArray) {
        lastX = values[0]
        lastY = values[1]
        lastZ = values[2]
    }
} 