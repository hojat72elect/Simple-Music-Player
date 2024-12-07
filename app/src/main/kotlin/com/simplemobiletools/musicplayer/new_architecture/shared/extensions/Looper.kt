package com.simplemobiletools.musicplayer.new_architecture.shared.extensions

import android.os.Handler
import android.os.Looper

fun Looper.post(callback: () -> Unit) = Handler(this).post(callback)
