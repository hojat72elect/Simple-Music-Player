package com.simplemobiletools.musicplayer.extensions

import android.graphics.Paint
import android.widget.TextView

fun TextView.underlineText() {
    paintFlags = paintFlags or Paint.UNDERLINE_TEXT_FLAG
}

val TextView.value: String get() = text.toString().trim()