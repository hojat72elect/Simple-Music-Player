package com.simplemobiletools.musicplayer.new_architecture.home.splash

import android.annotation.SuppressLint
import android.content.Intent
import com.simplemobiletools.musicplayer.activities.MainActivity

@SuppressLint("CustomSplashScreen")
class SplashActivity : BaseSplashActivity() {
    override fun initActivity() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}
