package com.simplemobiletools.musicplayer.extensions

import java.util.Random

fun ClosedRange<Int>.random() = Random().nextInt(endInclusive - start) + start