// In-memory holder for the captured medicine label photo and analysis result during verification.
package com.example.elderpal.ai

import android.graphics.Bitmap

object CapturedImageHolder {
    var currentBitmap: Bitmap? = null
    var latestResult: LabelCheckResult? = null

    // Clears all in-memory bitmaps and results to prevent memory leaks and protect privacy
    fun clear() {
        currentBitmap = null
        latestResult = null
    }
}
