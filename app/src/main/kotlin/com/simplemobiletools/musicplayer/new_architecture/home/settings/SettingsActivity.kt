package com.simplemobiletools.musicplayer.new_architecture.home.settings

import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.annotation.RequiresApi
import com.simplemobiletools.musicplayer.dialogs.RadioGroupDialog
import com.simplemobiletools.musicplayer.helpers.IS_CUSTOMIZING_COLORS
import com.simplemobiletools.musicplayer.helpers.NavigationIcon
import com.simplemobiletools.musicplayer.helpers.isQPlus
import com.simplemobiletools.musicplayer.helpers.isTiramisuPlus
import com.simplemobiletools.musicplayer.models.RadioItem
import com.simplemobiletools.musicplayer.R
import com.simplemobiletools.musicplayer.new_architecture.shared.SimpleControllerActivity
import com.simplemobiletools.musicplayer.databinding.ActivitySettingsBinding
import com.simplemobiletools.musicplayer.dialogs.ManageVisibleTabsDialog
import com.simplemobiletools.musicplayer.extensions.beGone
import com.simplemobiletools.musicplayer.extensions.beVisibleIf
import com.simplemobiletools.musicplayer.extensions.config
import com.simplemobiletools.musicplayer.extensions.getCustomizeColorsString
import com.simplemobiletools.musicplayer.extensions.getProperPrimaryColor
import com.simplemobiletools.musicplayer.extensions.launchPurchaseThankYouIntent
import com.simplemobiletools.musicplayer.extensions.sendCommand
import com.simplemobiletools.musicplayer.extensions.updateTextColors
import com.simplemobiletools.musicplayer.extensions.viewBinding
import com.simplemobiletools.musicplayer.helpers.SHOW_FILENAME_ALWAYS
import com.simplemobiletools.musicplayer.helpers.SHOW_FILENAME_IF_UNAVAILABLE
import com.simplemobiletools.musicplayer.helpers.SHOW_FILENAME_NEVER
import com.simplemobiletools.musicplayer.playback.CustomCommands
import java.util.Locale
import kotlin.system.exitProcess

class SettingsActivity : SimpleControllerActivity() {

    private val binding by viewBinding(ActivitySettingsBinding::inflate)

    override fun onCreate(savedInstanceState: Bundle?) {
        isMaterialActivity = true
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        updateMaterialActivityViews(
            binding.settingsCoordinator,
            binding.settingsHolder,
            useTransparentNavigation = true,
            useTopSearchMenu = false
        )
        setupMaterialScrollListener(binding.settingsNestedScrollview, binding.settingsToolbar)
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun onResume() {
        super.onResume()
        setupToolbar(binding.settingsToolbar, NavigationIcon.Arrow)

        setupPurchaseThankYou()
        setupCustomizeColors()
        setupCustomizeWidgetColors()
        setupUseEnglish()
        setupLanguage()
        setupManageExcludedFolders()
        setupManageShownTabs()
        setupSwapPrevNext()
        setupReplaceTitle()
        setupGaplessPlayback()
        updateTextColors(binding.settingsNestedScrollview)

        arrayOf(
            binding.settingsColorCustomizationSectionLabel,
            binding.settingsGeneralSettingsLabel,
            binding.settingsPlaybackSectionLabel
        ).forEach {
            it.setTextColor(getProperPrimaryColor())
        }
    }

    private fun setupPurchaseThankYou() = binding.apply {
        settingsPurchaseThankYouHolder.beGone()
        settingsPurchaseThankYouHolder.setOnClickListener {
            launchPurchaseThankYouIntent()
        }
    }

    private fun setupCustomizeColors() = binding.apply {
        settingsColorCustomizationLabel.text = getCustomizeColorsString()
        settingsColorCustomizationHolder.setOnClickListener {
            handleCustomizeColorsClick()
        }
    }

    private fun setupCustomizeWidgetColors() {
        binding.settingsWidgetColorCustomizationHolder.setOnClickListener {
            Intent(this, WidgetConfigureActivity::class.java).apply {
                putExtra(IS_CUSTOMIZING_COLORS, true)
                startActivity(this)
            }
        }
    }

    private fun setupUseEnglish() = binding.apply {
        settingsUseEnglishHolder.beVisibleIf((config.wasUseEnglishToggled || Locale.getDefault().language != "en") && !isTiramisuPlus())
        settingsUseEnglish.isChecked = config.useEnglish
        settingsUseEnglishHolder.setOnClickListener {
            settingsUseEnglish.toggle()
            config.useEnglish = settingsUseEnglish.isChecked
            exitProcess(0)
        }
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private fun setupLanguage() = binding.apply {
        settingsLanguage.text = Locale.getDefault().displayLanguage
        settingsLanguageHolder.beVisibleIf(isTiramisuPlus())
        settingsLanguageHolder.setOnClickListener {
            launchChangeAppLanguageIntent()
        }
    }

    private fun setupSwapPrevNext() = binding.apply {
        settingsSwapPrevNext.isChecked = config.swapPrevNext
        settingsSwapPrevNextHolder.setOnClickListener {
            settingsSwapPrevNext.toggle()
            config.swapPrevNext = settingsSwapPrevNext.isChecked
        }
    }

    private fun setupReplaceTitle() = binding.apply {
        settingsShowFilename.text = getReplaceTitleText()
        settingsShowFilenameHolder.setOnClickListener {
            val items = arrayListOf(
                RadioItem(SHOW_FILENAME_NEVER, getString(R.string.never)),
                RadioItem(SHOW_FILENAME_IF_UNAVAILABLE, getString(R.string.title_is_not_available)),
                RadioItem(SHOW_FILENAME_ALWAYS, getString(R.string.always))
            )

            RadioGroupDialog(this@SettingsActivity, items, config.showFilename) {
                config.showFilename = it as Int
                settingsShowFilename.text = getReplaceTitleText()
                refreshQueueAndTracks()
            }
        }
    }

    private fun getReplaceTitleText() = getString(
        when (config.showFilename) {
            SHOW_FILENAME_NEVER -> R.string.never
            SHOW_FILENAME_IF_UNAVAILABLE -> R.string.title_is_not_available
            else -> R.string.always
        }
    )

    private fun setupManageShownTabs() = binding.apply {
        settingsManageShownTabsHolder.setOnClickListener {
            ManageVisibleTabsDialog(this@SettingsActivity) { result ->
                val tabsMask = config.showTabs
                if (tabsMask != result) {
                    config.showTabs = result
                    withPlayer {
                        sendCommand(CustomCommands.RELOAD_CONTENT)
                    }
                }
            }
        }
    }

    private fun setupManageExcludedFolders() {
        binding.settingsManageExcludedFoldersHolder.beVisibleIf(isQPlus())
        binding.settingsManageExcludedFoldersHolder.setOnClickListener {
            startActivity(Intent(this, ExcludedFoldersActivity::class.java))
        }
    }

    private fun setupGaplessPlayback() = binding.apply {
        settingsGaplessPlayback.isChecked = config.gaplessPlayback
        settingsGaplessPlaybackHolder.setOnClickListener {
            settingsGaplessPlayback.toggle()
            config.gaplessPlayback = settingsGaplessPlayback.isChecked
            withPlayer {
                sendCommand(CustomCommands.TOGGLE_SKIP_SILENCE)
            }
        }
    }
}
