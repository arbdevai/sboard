package com.sahalos.sboard.core

import android.util.Log

object NativeCoreBridge {
    private const val TAG = "NativeCoreBridge"
    var isNativeLoaded: Boolean = false
        private set

    init {
        try {
            System.loadLibrary("sboard_core")
            isNativeLoaded = true
            Log.i(TAG, "sboard_core native Rust engine loaded successfully!")
        } catch (e: Throwable) {
            isNativeLoaded = false
            Log.w(TAG, "Native library sboard_core not available, using pure Kotlin fallback: ${e.message}")
        }
    }

    fun getVersion(): String {
        return if (isNativeLoaded) {
            try {
                nativeGetVersion()
            } catch (e: Throwable) {
                "sboard-kotlin-engine v1.0 (fallback)"
            }
        } else {
            "sboard-kotlin-engine v1.0 (fallback)"
        }
    }

    fun getBigramScore(prevChar: Char, nextChar: Char): Float {
        return if (isNativeLoaded) {
            try {
                nativeGetBigramScore(prevChar, nextChar)
            } catch (e: Throwable) {
                0.5f
            }
        } else {
            0.5f
        }
    }

    fun resolveHit(touchX: Float, touchY: Float, prevChar: Char, flatKeyData: FloatArray): Int {
        return if (isNativeLoaded) {
            try {
                nativeResolveHit(touchX, touchY, prevChar, flatKeyData)
            } catch (e: Throwable) {
                -1
            }
        } else {
            -1
        }
    }

    private external fun nativeGetVersion(): String
    private external fun nativeGetBigramScore(prevChar: Char, nextChar: Char): Float
    private external fun nativeResolveHit(touchX: Float, touchY: Float, prevChar: Char, flatKeyData: FloatArray): Int
    private external fun nativeToggleModifier(flag: Int): Int
    private external fun nativeIsModifierActive(flag: Int): Int
}
