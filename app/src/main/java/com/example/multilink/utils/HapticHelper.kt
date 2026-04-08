package com.example.multilink.utils

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

object HapticHelper {
    enum class Type {
        LIGHT,   // For scrolling, clock ticks
        MEDIUM,  // For standard button clicks (Confirm)
        HEAVY,   // For long presses
        ERROR    // For Reject / Failed actions
    }

    fun trigger(context: Context, type: Type) {
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager =
                context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }

        if (!vibrator.hasVibrator()) return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // Modern, punchy haptics that work great
            val effect = when (type) {
                Type.LIGHT -> VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK)
                Type.MEDIUM -> VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK)
                Type.HEAVY -> VibrationEffect.createPredefined(VibrationEffect.EFFECT_HEAVY_CLICK)
                Type.ERROR -> VibrationEffect.createPredefined(VibrationEffect.EFFECT_DOUBLE_CLICK)
            }
            vibrator.vibrate(effect)
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Fallback for Android 8 & 9 with manual amplitude control
            val effect = when (type) {
                Type.LIGHT -> VibrationEffect.createOneShot(15, 50)
                Type.MEDIUM -> VibrationEffect.createOneShot(25, 100)
                Type.HEAVY -> VibrationEffect.createOneShot(40, 200)
                Type.ERROR -> VibrationEffect.createWaveform(
                    longArrayOf(0, 30, 40, 30), intArrayOf(0, 150, 0, 150), -1
                )
            }
            vibrator.vibrate(effect)
        } else {
            // Legacy fallback for very old phones
            @Suppress("DEPRECATION")
            when (type) {
                Type.LIGHT -> vibrator.vibrate(15)
                Type.MEDIUM -> vibrator.vibrate(25)
                Type.HEAVY -> vibrator.vibrate(40)
                Type.ERROR -> vibrator.vibrate(longArrayOf(0, 30, 40, 30), -1)
            }
        }
    }
}