package com.simplemobiletools.musicplayer.objects

import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

object MyExecutor {
    val myExecutor: ExecutorService = Executors.newSingleThreadExecutor()
}
