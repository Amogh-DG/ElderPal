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
        // Lowered to 1.8g (~17.6 m/s²) to catch softer impacts such as falling onto a bed or couch,
        // while still filtering out gentle phone handling (typical resting handling stays below 1.5g).
        const val IMPACT_THRESHOLD_G = 1.8f

        // 3. Impact Window (ms):
        // Impact must follow within 1500 milliseconds of free-fall detection.
        // Widened from 1000 ms to allow for slower, softer falls (e.g. stumbling and sliding)
        // where the deceleration peak arrives a little later than a straight vertical drop.
        const val IMPACT_WINDOW_MS = 1500L

        // 4. Inactivity Window (ms):
        // Following an impact, the stillness confirmation window is 1000 ms.
        // Shortened from 1500 ms so a fallen person who stays still for just 1 second triggers the alert;
        // the 10-second countdown on-screen is the primary protection against false alarms.
        const val INACTIVITY_WINDOW_MS = 1000L

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

    var currentStage: String = "waiting"
        private set
    var currentG: Float = 1.0f
        private set

    // Real-time debug callback for live in-memory UI readout (not logged or persisted)
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

        // Compute total acceleration magnitude in units of g (Earth standard gravity)
        val ax = event.values[0]
        val ay = event.values[1]
        val az = event.values[2]
        val magnitudeMs2 = sqrt(ax * ax + ay * ay + az * az)
        val magnitudeG = magnitudeMs2 / SensorManager.GRAVITY_EARTH
        currentG = magnitudeG

        val now = System.currentTimeMillis()

        // 1. Check if an alert is already in progress or if in cool-down
        if (isAlertActive || (now - lastCancelledTimestamp < COOL_DOWN_MS)) {
            val cooldownRemaining = ((COOL_DOWN_MS - (now - lastCancelledTimestamp)) / 1000).coerceAtLeast(0)
            val stage = if (isAlertActive) "triggered" else "waiting"
            currentStage = stage
            onSensorSample?.invoke(magnitudeG, stage)
            return
        }

        when (currentState) {
            State.IDLE -> {
                // Phase 1: Check for Free-fall drop
                if (magnitudeG < FREE_FALL_THRESHOLD_G) {
                    currentState = State.FREE_FALL_DETECTED
                    freeFallTimestamp = now
                    currentStage = "free-fall seen"
                } else {
                    currentStage = "waiting"
                }
            }
            State.FREE_FALL_DETECTED -> {
                // If too much time has passed without an impact, reset to IDLE
                if (now - freeFallTimestamp > IMPACT_WINDOW_MS) {
                    currentState = State.IDLE
                    currentStage = "waiting"
                } else if (magnitudeG > IMPACT_THRESHOLD_G) {
                    // Phase 2: Impact detected within window
                    currentState = State.IMPACT_DETECTED
                    impactTimestamp = now
                    currentStage = "impact seen"
                } else {
                    currentStage = "free-fall seen"
                }
            }
            State.IMPACT_DETECTED -> {
                // Phase 3: Check for post-impact inactivity
                val timeSinceImpact = now - impactTimestamp
                val deviation = abs(magnitudeG - 1.0f)

                if (deviation > INACTIVITY_DEVIATION_THRESHOLD_G) {
                    // Active continuous motion immediately after impact -> likely normal activity (e.g. running or tossing phone)
                    currentState = State.IDLE
                    currentStage = "waiting"
                } else if (timeSinceImpact >= INACTIVITY_WINDOW_MS) {
                    // Inactivity confirmed! Suspected fall triggered
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
