package com.simplemobiletools.musicplayer.new_architecture.shared.data

import androidx.biometric.auth.AuthPromptHost
import com.simplemobiletools.musicplayer.new_architecture.shared.ui.views.MyScrollView


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
