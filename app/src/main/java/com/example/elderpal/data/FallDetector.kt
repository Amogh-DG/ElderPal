package com.example.elderpal.data

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlin.math.abs
import kotlin.math.sqrt

class FallDetector(
    private val onFallDetected: () -> Unit
) : SensorEventListener {

    companion object {
        // drop below 0.5g marks near-weightlessness during a fall
        const val FREE_FALL_THRESHOLD_G = 0.5f

        // deceleration spike above 1.8g on hitting the ground
        const val IMPACT_THRESHOLD_G = 1.8f

        // impact must follow within 1.5 seconds of free fall
        const val IMPACT_WINDOW_MS = 1500L

        // required period of stillness after impact
        const val INACTIVITY_WINDOW_MS = 1000L

        // allowed movement deviation during stillness
        const val INACTIVITY_DEVIATION_THRESHOLD_G = 0.4f

        // pause after an alert or cancel to avoid re-triggering
        const val COOL_DOWN_MS = 10_000L
    }

    private enum class State {
        IDLE,
        FREE_FALL_DETECTED,
        IMPACT_DETECTED
    }

    private var currentState = State.IDLE
    private var freeFallTimestamp = 0L
    private var impactTimestamp = 0L

    var isAlertActive: Boolean = false
    var lastCancelledTimestamp: Long = 0L

    var currentStage: String = "waiting"
        private set
    var currentG: Float = 1.0f
        private set

    var onSensorSample: ((g: Float, stage: String) -> Unit)? = null

    fun notifyCancelled() {
        lastCancelledTimestamp = System.currentTimeMillis()
        reset()
    }

    fun reset() {
        currentState = State.IDLE
        freeFallTimestamp = 0L
        impactTimestamp = 0L
        currentStage = "waiting"
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null || event.sensor.type != Sensor.TYPE_ACCELEROMETER) return

        val ax = event.values[0]
        val ay = event.values[1]
        val az = event.values[2]
        val magnitudeMs2 = sqrt(ax * ax + ay * ay + az * az)
        val magnitudeG = magnitudeMs2 / SensorManager.GRAVITY_EARTH
        currentG = magnitudeG

        val now = System.currentTimeMillis()

        // ignore readings during cooldown or while an alert is active
        if (isAlertActive || (now - lastCancelledTimestamp < COOL_DOWN_MS)) {
            val cooldownRemaining = ((COOL_DOWN_MS - (now - lastCancelledTimestamp)) / 1000).coerceAtLeast(0)
            val stage = if (isAlertActive) "triggered" else "waiting"
            currentStage = stage
            onSensorSample?.invoke(magnitudeG, stage)
            return
        }

        when (currentState) {
            State.IDLE -> {
                // stage 1: free fall
                if (magnitudeG < FREE_FALL_THRESHOLD_G) {
                    currentState = State.FREE_FALL_DETECTED
                    freeFallTimestamp = now
                    currentStage = "free-fall seen"
                } else {
                    currentStage = "waiting"
                }
            }
            State.FREE_FALL_DETECTED -> {
                // stage 2: impact within window
                if (now - freeFallTimestamp > IMPACT_WINDOW_MS) {
                    currentState = State.IDLE
                    currentStage = "waiting"
                } else if (magnitudeG > IMPACT_THRESHOLD_G) {
                    currentState = State.IMPACT_DETECTED
                    impactTimestamp = now
                    currentStage = "impact seen"
                } else {
                    currentStage = "free-fall seen"
                }
            }
            State.IMPACT_DETECTED -> {
                // stage 3: stillness check
                val timeSinceImpact = now - impactTimestamp
                val deviation = abs(magnitudeG - 1.0f)

                if (deviation > INACTIVITY_DEVIATION_THRESHOLD_G) {
                    currentState = State.IDLE
                    currentStage = "waiting"
                } else if (timeSinceImpact >= INACTIVITY_WINDOW_MS) {
                    currentStage = "triggered"
                    onSensorSample?.invoke(magnitudeG, "triggered")
                    reset()
                    onFallDetected()
                    return
                } else {
                    currentStage = if (timeSinceImpact > 200L) "inactive" else "impact seen"
                }
            }
        }

        onSensorSample?.invoke(magnitudeG, currentStage)
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Sensor accuracy adjustments not required for fall detection
    }
}
