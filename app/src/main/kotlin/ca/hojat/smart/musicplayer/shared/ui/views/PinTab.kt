package ca.hojat.smart.musicplayer.shared.ui.views

import android.content.Context
import android.util.AttributeSet
import android.widget.TextView
import android.widget.Toast
import androidx.biometric.auth.AuthPromptHost
import ca.hojat.smart.musicplayer.R
import ca.hojat.smart.musicplayer.databinding.TabPinBinding
import ca.hojat.smart.musicplayer.shared.data.BaseSecurityTab
import ca.hojat.smart.musicplayer.shared.data.HashListener
import ca.hojat.smart.musicplayer.shared.extensions.applyColorFilter
import ca.hojat.smart.musicplayer.shared.extensions.getProperTextColor
import ca.hojat.smart.musicplayer.shared.extensions.performHapticFeedback
import ca.hojat.smart.musicplayer.shared.extensions.updateTextColors
import ca.hojat.smart.musicplayer.shared.helpers.MINIMUM_PIN_LENGTH
import ca.hojat.smart.musicplayer.shared.helpers.PROTECTION_PIN
import ca.hojat.smart.musicplayer.shared.usecases.ShowToastUseCase
import java.math.BigInteger
import java.security.MessageDigest
import java.util.Locale

class PinTab(context: Context, attrs: AttributeSet) : BaseSecurityTab(context, attrs) {
    private var pin = ""

    private lateinit var binding: TabPinBinding

    override val protectionType = PROTECTION_PIN
    override val defaultTextRes = R.string.enter_pin
    override val wrongTextRes = R.string.wrong_pin

    override fun initTab(
        requiredHash: String,
        listener: HashListener,
        scrollView: MyScrollView,
        biometricPromptHost: AuthPromptHost,
        showBiometricAuthentication: Boolean
    ) {
        this.requiredHash = requiredHash
        computedHash = requiredHash
        hashListener = listener
    }

    override val titleTextView: TextView
        get() = binding.pinLockTitle

    override fun onFinishInflate() {
        super.onFinishInflate()
        binding = TabPinBinding.bind(this)

        val textColor = context.getProperTextColor()
        context.updateTextColors(binding.pinLockHolder)

        binding.pin0.setOnClickListener { addNumber("0") }
        binding.pin1.setOnClickListener { addNumber("1") }
        binding.pin2.setOnClickListener { addNumber("2") }
        binding.pin3.setOnClickListener { addNumber("3") }
        binding.pin4.setOnClickListener { addNumber("4") }
        binding.pin5.setOnClickListener { addNumber("5") }
        binding.pin6.setOnClickListener { addNumber("6") }
        binding.pin7.setOnClickListener { addNumber("7") }
        binding.pin8.setOnClickListener { addNumber("8") }
        binding.pin9.setOnClickListener { addNumber("9") }
        binding.pinC.setOnClickListener { clear() }
        binding.pinOk.setOnClickListener { confirmPIN() }
        binding.pinOk.applyColorFilter(textColor)
        binding.pinLockIcon.applyColorFilter(textColor)
        maybeShowCountdown()
    }

    private fun addNumber(number: String) {
        if (!isLockedOut()) {
            if (pin.length < 10) {
                pin += number
                updatePinCode()
            }
        }

        performHapticFeedback()
    }

    private fun clear() {
        if (pin.isNotEmpty()) {
            pin = pin.substring(0, pin.length - 1)
            updatePinCode()
        }
        performHapticFeedback()
    }

    private fun confirmPIN() {
        if (!isLockedOut()) {
            val newHash = getHashedPin()
            when {
                pin.isEmpty() -> ShowToastUseCase(context, R.string.please_enter_pin, Toast.LENGTH_LONG)

                computedHash.isEmpty() && pin.length < MINIMUM_PIN_LENGTH -> {
                    resetPin()
                    ShowToastUseCase(context, R.string.pin_must_be_4_digits_long, Toast.LENGTH_LONG)
                }

                computedHash.isEmpty() -> {
                    computedHash = newHash
                    resetPin()
                    binding.pinLockTitle.setText(R.string.repeat_pin)
                }

                computedHash == newHash -> {
                    onCorrectPassword()
                }

                else -> {
                    resetPin()
                    onIncorrectPassword()
                    if (requiredHash.isEmpty()) {
                        computedHash = ""
                    }
                }
            }
        }

        performHapticFeedback()
    }

    private fun resetPin() {
        pin = ""
        binding.pinLockCurrentPin.text = ""
    }

    private fun updatePinCode() {
        binding.pinLockCurrentPin.text = "*".repeat(pin.length)
    }

    private fun getHashedPin(): String {
        val messageDigest = MessageDigest.getInstance("SHA-1")
        messageDigest.update(pin.toByteArray(charset("UTF-8")))
        val digest = messageDigest.digest()
        val bigInteger = BigInteger(1, digest)
        return String.format(Locale.getDefault(), "%0${digest.size * 2}x", bigInteger)
            .lowercase(Locale.getDefault())
    }
}
