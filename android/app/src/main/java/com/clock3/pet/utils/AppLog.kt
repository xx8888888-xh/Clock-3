package com.clock3.pet.utils

import android.util.Log

object AppLog {
    private const val TAG_PREFIX = "Clock3"

    fun d(tag: String, msg: String) {
        Log.d("$TAG_PREFIX.$tag", msg)
    }

    fun e(tag: String, msg: String, throwable: Throwable? = null) {
        if (throwable != null) {
            Log.e("$TAG_PREFIX.$tag", msg, throwable)
        } else {
            Log.e("$TAG_PREFIX.$tag", msg)
        }
    }

    fun w(tag: String, msg: String, throwable: Throwable? = null) {
        if (throwable != null) {
            Log.w("$TAG_PREFIX.$tag", msg, throwable)
        } else {
            Log.w("$TAG_PREFIX.$tag", msg)
        }
    }
}
