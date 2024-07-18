package com.simplemobiletools.musicplayer.compose.extensions

import android.app.Activity
import android.content.Context
import com.simplemobiletools.musicplayer.R
import com.simplemobiletools.musicplayer.extensions.baseConfig
import com.simplemobiletools.musicplayer.extensions.redirectToRateUs
import com.simplemobiletools.musicplayer.extensions.toast
import com.simplemobiletools.musicplayer.helpers.BaseConfig

val Context.config: BaseConfig get() = BaseConfig.newInstance(applicationContext)

fun Activity.rateStarsRedirectAndThankYou(stars: Int) {
    if (stars == 5) {
        redirectToRateUs()
    }
    toast(R.string.thank_you)
    baseConfig.wasAppRated = true
}
