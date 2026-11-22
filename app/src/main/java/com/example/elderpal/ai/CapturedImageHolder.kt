package com.example.elderpal.ai

import android.graphics.Bitmap

object CapturedImageHolder {
    var currentBitmap: Bitmap? = null
    var latestResult: LabelCheckResult? = null

    fun clear() {
        currentBitmap = null
        latestResult = null
    }
}
