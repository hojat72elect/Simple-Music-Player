package com.simplemobiletools.musicplayer.new_architecture.shared.extensions

import java.util.Random

fun ClosedRange<Int>.random() = Random().nextInt(endInclusive - start) + start
