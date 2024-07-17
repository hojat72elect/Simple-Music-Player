package com.simplemobiletools.musicplayer.interfaces

import androidx.biometric.auth.AuthPromptHost
import com.simplemobiletools.musicplayer.views.MyScrollView


interface SecurityTab {
    fun initTab(
        requiredHash: String,
        listener: HashListener,
        scrollView: MyScrollView,
        biometricPromptHost: AuthPromptHost,
        showBiometricAuthentication: Boolean
    )

    fun visibilityChanged(isVisible: Boolean)
}
