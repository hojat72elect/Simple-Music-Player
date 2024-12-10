package ca.hojat.smart.musicplayer.shared.ui.compose.extensions

import android.app.Activity
import android.content.Context
import ca.hojat.smart.musicplayer.R
import ca.hojat.smart.musicplayer.shared.extensions.baseConfig
import ca.hojat.smart.musicplayer.shared.extensions.redirectToRateUs
import ca.hojat.smart.musicplayer.shared.helpers.BaseConfig
import ca.hojat.smart.musicplayer.shared.usecases.ShowToastUseCase

val Context.config: BaseConfig get() = BaseConfig.newInstance(applicationContext)

fun Activity.rateStarsRedirectAndThankYou(stars: Int) {
    if (stars == 5) {
        redirectToRateUs()
    }
    ShowToastUseCase(this ,R.string.thank_you)
    baseConfig.wasAppRated = true
}
