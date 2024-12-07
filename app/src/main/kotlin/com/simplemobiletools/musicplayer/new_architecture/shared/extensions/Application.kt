package com.simplemobiletools.musicplayer.new_architecture.shared.extensions

import android.app.Application
import com.simplemobiletools.musicplayer.new_architecture.shared.helpers.isNougatPlus
import java.util.Locale

fun Application.checkUseEnglish() {
    if (baseConfig.useEnglish && !isNougatPlus()) {
        val conf = resources.configuration
        conf.locale = Locale.ENGLISH
        resources.updateConfiguration(conf, resources.displayMetrics)
    }
}
