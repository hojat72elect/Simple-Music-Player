package com.simplemobiletools.musicplayer.activities

import android.annotation.SuppressLint
import android.content.Intent

@SuppressLint("CustomSplashScreen")
class SplashActivity : BaseSplashActivity() {
    override fun initActivity() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}
