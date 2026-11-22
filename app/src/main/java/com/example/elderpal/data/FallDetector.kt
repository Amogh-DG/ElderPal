// Sensor event listener using explainable multi-phase accelerometer thresholds to detect suspected falls.
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
        // --- Tuneable Physical Threshold Constants with Explanations ---

        // 1. Free-Fall Threshold (g):
        // In normal gravity, the phone measures ~1.0g (9.8 m/s²). As an individual starts to fall,
        // the body and phone experience a brief period of near-zero gravity (weightlessness).
        // A drop below 0.5g (< ~4.9 m/s²) flags the start of a free-fall phase.
        const val FREE_FALL_THRESHOLD_G = 0.5f

        // 2. Impact Threshold (g):
        // Upon striking the ground, rapid deceleration causes a high-G kinetic spike.
        // A force magnitude > 2.6g (> ~25.5 m/s²) indicates a sudden hard impact.
        const val IMPACT_THRESHOLD_G = 2.6f

        // 3. Impact Window (ms):
        // An impact must follow within 1000 milliseconds of free-fall for it to be a fall event.
        const val IMPACT_WINDOW_MS = 1000L

        // 4. Inactivity Window (ms):
        // Following an impact, an injured elder will typically remain motionless for at least 1.5 seconds.
        const val INACTIVITY_WINDOW_MS = 1500L

        // 5. Inactivity Variance (g):
        // During the stillness window, deviation from resting 1.0g must stay below 0.4g.
        const val INACTIVITY_DEVIATION_THRESHOLD_G = 0.4f

        // 6. Cool-down Period (ms):
        // Prevents duplicate triggers after a cancellation or completed alert dispatch.
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

    fun notifyCancelled() {
        lastCancelledTimestamp = System.currentTimeMillis()
        reset()
    }

    fun reset() {
        currentState = State.IDLE
        freeFallTimestamp = 0L
        impactTimestamp = 0L
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null || event.sensor.type != Sensor.TYPE_ACCELEROMETER) return

        // 1. Check if an alert is already in progress or if in cool-down
        val now = System.currentTimeMillis()
        if (isAlertActive || (now - lastCancelledTimestamp < COOL_DOWN_MS)) {
            return
        }

        // 2. Compute total acceleration magnitude in units of g (Earth standard gravity)
        val ax = event.values[0]
        val ay = event.values[1]
        val az = event.values[2]
        val magnitudeMs2 = sqrt(ax * ax + ay * ay + az * az)
        val magnitudeG = magnitudeMs2 / SensorManager.GRAVITY_EARTH

        when (currentState) {
            State.IDLE -> {
                // Phase 1: Check for Free-fall drop
                if (magnitudeG < FREE_FALL_THRESHOLD_G) {
                    currentState = State.FREE_FALL_DETECTED
                    freeFallTimestamp = now
                }
            }
            State.FREE_FALL_DETECTED -> {
                // If too much time has passed without an impact, reset to IDLE
                if (now - freeFallTimestamp > IMPACT_WINDOW_MS) {
                    currentState = State.IDLE
                } else if (magnitudeG > IMPACT_THRESHOLD_G) {
                    // Phase 2: Impact detected within window
                    currentState = State.IMPACT_DETECTED
                    impactTimestamp = now
                }
            }
            State.IMPACT_DETECTED -> {
                // Phase 3: Check for post-impact inactivity
                val timeSinceImpact = now - impactTimestamp
                val deviation = abs(magnitudeG - 1.0f)

                if (deviation > INACTIVITY_DEVIATION_THRESHOLD_G) {
                    // Active continuous motion immediately after impact -> likely normal activity (e.g. running or tossing phone)
                    currentState = State.IDLE
                } else if (timeSinceImpact >= INACTIVITY_WINDOW_MS) {
                    // Inactivity confirmed! Suspected fall triggered
                    reset()
                    onFallDetected()
                }
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Sensor accuracy adjustments not required for fall detection
    }
}
