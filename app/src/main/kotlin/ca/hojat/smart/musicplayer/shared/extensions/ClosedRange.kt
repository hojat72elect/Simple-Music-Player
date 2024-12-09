package ca.hojat.smart.musicplayer.shared.extensions

import java.util.Random

fun ClosedRange<Int>.random() = Random().nextInt(endInclusive - start) + start
