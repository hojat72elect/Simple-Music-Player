package com.simplemobiletools.musicplayer.new_architecture.home.splash

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.simplemobiletools.musicplayer.extensions.baseConfig
import com.simplemobiletools.musicplayer.extensions.checkAppIconColor
import com.simplemobiletools.musicplayer.extensions.checkAppSideloading
import com.simplemobiletools.musicplayer.helpers.SIDELOADING_TRUE
import com.simplemobiletools.musicplayer.helpers.SIDELOADING_UNCHECKED
import com.simplemobiletools.musicplayer.R
import com.simplemobiletools.musicplayer.extensions.getSharedTheme
import com.simplemobiletools.musicplayer.extensions.isThankYouInstalled
import com.simplemobiletools.musicplayer.extensions.isUsingSystemDarkTheme
import com.simplemobiletools.musicplayer.extensions.showSideloadingDialog

@SuppressLint("CustomSplashScreen")
abstract class BaseSplashActivity : AppCompatActivity() {
    abstract fun initActivity()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (baseConfig.appSideloadingStatus == SIDELOADING_UNCHECKED) {
            if (checkAppSideloading()) {
                return
            }
        } else if (baseConfig.appSideloadingStatus == SIDELOADING_TRUE) {
            showSideloadingDialog()
            return
        }

        baseConfig.apply {
            if (isUsingAutoTheme) {
                val isUsingSystemDarkTheme = isUsingSystemDarkTheme()
                isUsingSharedTheme = false
                textColor =
                    resources.getColor(if (isUsingSystemDarkTheme) R.color.theme_dark_text_color else R.color.theme_light_text_color)
                backgroundColor =
                    resources.getColor(if (isUsingSystemDarkTheme) R.color.theme_dark_background_color else R.color.theme_light_background_color)
            }
        }

        if (!baseConfig.isUsingAutoTheme && !baseConfig.isUsingSystemTheme && isThankYouInstalled()) {
            getSharedTheme {
                if (it != null) {
                    baseConfig.apply {
                        wasSharedThemeForced = true
                        isUsingSharedTheme = true
                        wasSharedThemeEverActivated = true

                        textColor = it.textColor
                        backgroundColor = it.backgroundColor
                        primaryColor = it.primaryColor
                        accentColor = it.accentColor
                    }

                    if (baseConfig.appIconColor != it.appIconColor) {
                        baseConfig.appIconColor = it.appIconColor
                        checkAppIconColor()
                    }
                }
                initActivity()
            }
        } else {
            initActivity()
        }
    }
}
