package com.simplemobiletools.musicplayer.extensions

import com.simplemobiletools.commons.R as commonsR
import android.annotation.SuppressLint
import android.app.Activity
import android.app.Dialog
import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.Intent.EXTRA_STREAM
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.os.TransactionTooLargeException
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.view.WindowInsetsCompat
import androidx.viewbinding.ViewBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.simplemobiletools.commons.databinding.DialogTitleBinding
import com.simplemobiletools.musicplayer.dialogs.AppSideloadedDialog
import com.simplemobiletools.musicplayer.dialogs.DonateDialog
import com.simplemobiletools.musicplayer.dialogs.RateStarsDialog
import com.simplemobiletools.musicplayer.dialogs.SecurityDialog
import com.simplemobiletools.musicplayer.dialogs.UpgradeToProDialog
import com.simplemobiletools.commons.extensions.getInternalStoragePath
import com.simplemobiletools.musicplayer.BuildConfig
import com.simplemobiletools.musicplayer.R
import com.simplemobiletools.musicplayer.activities.BaseSimpleActivity
import com.simplemobiletools.musicplayer.dialogs.ConfirmationAdvancedDialog
import com.simplemobiletools.musicplayer.dialogs.PropertiesDialog
import com.simplemobiletools.musicplayer.dialogs.SelectPlaylistDialog
import com.simplemobiletools.musicplayer.dialogs.WhatsNewDialog
import com.simplemobiletools.musicplayer.dialogs.WritePermissionDialog
import com.simplemobiletools.musicplayer.dialogs.WritePermissionDialog.WritePermissionDialogMode
import com.simplemobiletools.musicplayer.helpers.CREATE_DOCUMENT_SDK_30
import com.simplemobiletools.musicplayer.helpers.DARK_GREY
import com.simplemobiletools.musicplayer.helpers.EXTRA_SHOW_ADVANCED
import com.simplemobiletools.musicplayer.helpers.FLAG_MANUAL_CACHE
import com.simplemobiletools.musicplayer.helpers.MyContentProvider
import com.simplemobiletools.musicplayer.helpers.OPEN_DOCUMENT_TREE_FOR_ANDROID_DATA_OR_OBB
import com.simplemobiletools.musicplayer.helpers.OPEN_DOCUMENT_TREE_FOR_SDK_30
import com.simplemobiletools.musicplayer.helpers.OPEN_DOCUMENT_TREE_OTG
import com.simplemobiletools.musicplayer.helpers.OPEN_DOCUMENT_TREE_SD
import com.simplemobiletools.musicplayer.helpers.RoomHelper
import com.simplemobiletools.musicplayer.helpers.SIDELOADING_FALSE
import com.simplemobiletools.musicplayer.helpers.SIDELOADING_TRUE
import com.simplemobiletools.musicplayer.helpers.ensureBackgroundThread
import com.simplemobiletools.musicplayer.helpers.isOnMainThread
import com.simplemobiletools.musicplayer.helpers.isRPlus
import com.simplemobiletools.musicplayer.models.Android30RenameFormat
import com.simplemobiletools.musicplayer.models.FileDirItem
import com.simplemobiletools.musicplayer.models.Release
import com.simplemobiletools.musicplayer.models.SharedTheme
import com.simplemobiletools.musicplayer.models.Track
import com.simplemobiletools.musicplayer.views.MyTextView
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream


fun Activity.launchViewIntent(url: String) {
    hideKeyboard()
    ensureBackgroundThread {
        Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
            try {
                startActivity(this)
            } catch (e: ActivityNotFoundException) {
                toast(R.string.no_browser_found)
            } catch (e: Exception) {
                showErrorToast(e)
            }
        }
    }
}

const val DEVELOPER_PLAY_STORE_URL = "https://play.google.com/store/apps/dev?id=9070296388022589266"

fun Activity.launchMoreAppsFromUsIntent() {
    launchViewIntent(DEVELOPER_PLAY_STORE_URL)
}

fun BaseSimpleActivity.checkWhatsNew(releases: List<Release>, currVersion: Int) {
    if (baseConfig.lastVersion == 0) {
        baseConfig.lastVersion = currVersion
        return
    }

    val newReleases = arrayListOf<Release>()
    releases.filterTo(newReleases) { it.id > baseConfig.lastVersion }

    if (newReleases.isNotEmpty()) {
        WhatsNewDialog(this, newReleases)
    }

    baseConfig.lastVersion = currVersion
}

fun BaseSimpleActivity.isShowingOTGDialog(path: String): Boolean {
    return if (!isRPlus() && isPathOnOTG(path) && (baseConfig.OTGTreeUri.isEmpty() || !hasProperStoredTreeUri(
            true
        ))
    ) {
        showOTGPermissionDialog(path)
        true
    } else {
        false
    }
}

fun Activity.updateSharedTheme(sharedTheme: SharedTheme) {
    try {
        val contentValues = MyContentProvider.fillThemeContentValues(sharedTheme)
        applicationContext.contentResolver.update(
            MyContentProvider.MY_CONTENT_URI,
            contentValues,
            null,
            null
        )
    } catch (e: Exception) {
        showErrorToast(e)
    }
}

fun Activity.getThemeId(color: Int = baseConfig.primaryColor, showTransparentTop: Boolean = false) =
    when {
        baseConfig.isUsingSystemTheme -> if (isUsingSystemDarkTheme()) commonsR.style.AppTheme_Base_System else commonsR.style.AppTheme_Base_System_Light
        isBlackAndWhiteTheme() -> when {
            showTransparentTop -> commonsR.style.AppTheme_BlackAndWhite_NoActionBar
            baseConfig.primaryColor.getContrastColor() == DARK_GREY -> commonsR.style.AppTheme_BlackAndWhite_DarkTextColor
            else -> commonsR.style.AppTheme_BlackAndWhite
        }

        isWhiteTheme() -> when {
            showTransparentTop -> commonsR.style.AppTheme_White_NoActionBar
            baseConfig.primaryColor.getContrastColor() == Color.WHITE -> commonsR.style.AppTheme_White_LightTextColor
            else -> commonsR.style.AppTheme_White
        }

        showTransparentTop -> {
            when (color) {
                -12846 -> commonsR.style.AppTheme_Red_100_core
                -1074534 -> commonsR.style.AppTheme_Red_200_core
                -1739917 -> commonsR.style.AppTheme_Red_300_core
                -1092784 -> commonsR.style.AppTheme_Red_400_core
                -769226 -> commonsR.style.AppTheme_Red_500_core
                -1754827 -> commonsR.style.AppTheme_Red_600_core
                -2937041 -> commonsR.style.AppTheme_Red_700_core
                -3790808 -> commonsR.style.AppTheme_Red_800_core
                -4776932 -> commonsR.style.AppTheme_Red_900_core

                -476208 -> commonsR.style.AppTheme_Pink_100_core
                -749647 -> commonsR.style.AppTheme_Pink_200_core
                -1023342 -> commonsR.style.AppTheme_Pink_300_core
                -1294214 -> commonsR.style.AppTheme_Pink_400_core
                -1499549 -> commonsR.style.AppTheme_Pink_500_core
                -2614432 -> commonsR.style.AppTheme_Pink_600_core
                -4056997 -> commonsR.style.AppTheme_Pink_700_core
                -5434281 -> commonsR.style.AppTheme_Pink_800_core
                -7860657 -> commonsR.style.AppTheme_Pink_900_core

                -1982745 -> commonsR.style.AppTheme_Purple_100_core
                -3238952 -> commonsR.style.AppTheme_Purple_200_core
                -4560696 -> commonsR.style.AppTheme_Purple_300_core
                -5552196 -> commonsR.style.AppTheme_Purple_400_core
                -6543440 -> commonsR.style.AppTheme_Purple_500_core
                -7461718 -> commonsR.style.AppTheme_Purple_600_core
                -8708190 -> commonsR.style.AppTheme_Purple_700_core
                -9823334 -> commonsR.style.AppTheme_Purple_800_core
                -11922292 -> commonsR.style.AppTheme_Purple_900_core

                -3029783 -> commonsR.style.AppTheme_Deep_Purple_100_core
                -5005861 -> commonsR.style.AppTheme_Deep_Purple_200_core
                -6982195 -> commonsR.style.AppTheme_Deep_Purple_300_core
                -8497214 -> commonsR.style.AppTheme_Deep_Purple_400_core
                -10011977 -> commonsR.style.AppTheme_Deep_Purple_500_core
                -10603087 -> commonsR.style.AppTheme_Deep_Purple_600_core
                -11457112 -> commonsR.style.AppTheme_Deep_Purple_700_core
                -12245088 -> commonsR.style.AppTheme_Deep_Purple_800_core
                -13558894 -> commonsR.style.AppTheme_Deep_Purple_900_core

                -3814679 -> commonsR.style.AppTheme_Indigo_100_core
                -6313766 -> commonsR.style.AppTheme_Indigo_200_core
                -8812853 -> commonsR.style.AppTheme_Indigo_300_core
                -10720320 -> commonsR.style.AppTheme_Indigo_400_core
                -12627531 -> commonsR.style.AppTheme_Indigo_500_core
                -13022805 -> commonsR.style.AppTheme_Indigo_600_core
                -13615201 -> commonsR.style.AppTheme_Indigo_700_core
                -14142061 -> commonsR.style.AppTheme_Indigo_800_core
                -15064194 -> commonsR.style.AppTheme_Indigo_900_core

                -4464901 -> commonsR.style.AppTheme_Blue_100_core
                -7288071 -> commonsR.style.AppTheme_Blue_200_core
                -10177034 -> commonsR.style.AppTheme_Blue_300_core
                -12409355 -> commonsR.style.AppTheme_Blue_400_core
                -14575885 -> commonsR.style.AppTheme_Blue_500_core
                -14776091 -> commonsR.style.AppTheme_Blue_600_core
                -15108398 -> commonsR.style.AppTheme_Blue_700_core
                -15374912 -> commonsR.style.AppTheme_Blue_800_core
                -15906911 -> commonsR.style.AppTheme_Blue_900_core

                -4987396 -> commonsR.style.AppTheme_Light_Blue_100_core
                -8268550 -> commonsR.style.AppTheme_Light_Blue_200_core
                -11549705 -> commonsR.style.AppTheme_Light_Blue_300_core
                -14043396 -> commonsR.style.AppTheme_Light_Blue_400_core
                -16537100 -> commonsR.style.AppTheme_Light_Blue_500_core
                -16540699 -> commonsR.style.AppTheme_Light_Blue_600_core
                -16611119 -> commonsR.style.AppTheme_Light_Blue_700_core
                -16615491 -> commonsR.style.AppTheme_Light_Blue_800_core
                -16689253 -> commonsR.style.AppTheme_Light_Blue_900_core

                -5051406 -> commonsR.style.AppTheme_Cyan_100_core
                -8331542 -> commonsR.style.AppTheme_Cyan_200_core
                -11677471 -> commonsR.style.AppTheme_Cyan_300_core
                -14235942 -> commonsR.style.AppTheme_Cyan_400_core
                -16728876 -> commonsR.style.AppTheme_Cyan_500_core
                -16732991 -> commonsR.style.AppTheme_Cyan_600_core
                -16738393 -> commonsR.style.AppTheme_Cyan_700_core
                -16743537 -> commonsR.style.AppTheme_Cyan_800_core
                -16752540 -> commonsR.style.AppTheme_Cyan_900_core

                -5054501 -> commonsR.style.AppTheme_Teal_100_core
                -8336444 -> commonsR.style.AppTheme_Teal_200_core
                -11684180 -> commonsR.style.AppTheme_Teal_300_core
                -14244198 -> commonsR.style.AppTheme_Teal_400_core
                -16738680 -> commonsR.style.AppTheme_Teal_500_core
                -16742021 -> commonsR.style.AppTheme_Teal_600_core
                -16746133 -> commonsR.style.AppTheme_Teal_700_core
                -16750244 -> commonsR.style.AppTheme_Teal_800_core
                -16757440 -> commonsR.style.AppTheme_Teal_900_core

                -3610935 -> commonsR.style.AppTheme_Green_100_core
                -5908825 -> commonsR.style.AppTheme_Green_200_core
                -8271996 -> commonsR.style.AppTheme_Green_300_core
                -10044566 -> commonsR.style.AppTheme_Green_400_core
                -11751600 -> commonsR.style.AppTheme_Green_500_core
                -12345273 -> commonsR.style.AppTheme_Green_600_core
                -13070788 -> commonsR.style.AppTheme_Green_700_core
                -13730510 -> commonsR.style.AppTheme_Green_800_core
                -14983648 -> commonsR.style.AppTheme_Green_900_core

                -2298424 -> commonsR.style.AppTheme_Light_Green_100_core
                -3808859 -> commonsR.style.AppTheme_Light_Green_200_core
                -5319295 -> commonsR.style.AppTheme_Light_Green_300_core
                -6501275 -> commonsR.style.AppTheme_Light_Green_400_core
                -7617718 -> commonsR.style.AppTheme_Light_Green_500_core
                -8604862 -> commonsR.style.AppTheme_Light_Green_600_core
                -9920712 -> commonsR.style.AppTheme_Light_Green_700_core
                -11171025 -> commonsR.style.AppTheme_Light_Green_800_core
                -13407970 -> commonsR.style.AppTheme_Light_Green_900_core

                -985917 -> commonsR.style.AppTheme_Lime_100_core
                -1642852 -> commonsR.style.AppTheme_Lime_200_core
                -2300043 -> commonsR.style.AppTheme_Lime_300_core
                -2825897 -> commonsR.style.AppTheme_Lime_400_core
                -3285959 -> commonsR.style.AppTheme_Lime_500_core
                -4142541 -> commonsR.style.AppTheme_Lime_600_core
                -5983189 -> commonsR.style.AppTheme_Lime_700_core
                -6382300 -> commonsR.style.AppTheme_Lime_800_core
                -8227049 -> commonsR.style.AppTheme_Lime_900_core

                -1596 -> commonsR.style.AppTheme_Yellow_100_core
                -2672 -> commonsR.style.AppTheme_Yellow_200_core
                -3722 -> commonsR.style.AppTheme_Yellow_300_core
                -4520 -> commonsR.style.AppTheme_Yellow_400_core
                -5317 -> commonsR.style.AppTheme_Yellow_500_core
                -141259 -> commonsR.style.AppTheme_Yellow_600_core
                -278483 -> commonsR.style.AppTheme_Yellow_700_core
                -415707 -> commonsR.style.AppTheme_Yellow_800_core
                -688361 -> commonsR.style.AppTheme_Yellow_900_core

                -4941 -> commonsR.style.AppTheme_Amber_100_core
                -8062 -> commonsR.style.AppTheme_Amber_200_core
                -10929 -> commonsR.style.AppTheme_Amber_300_core
                -13784 -> commonsR.style.AppTheme_Amber_400_core
                -16121 -> commonsR.style.AppTheme_Amber_500_core
                -19712 -> commonsR.style.AppTheme_Amber_600_core
                -24576 -> commonsR.style.AppTheme_Amber_700_core
                -28928 -> commonsR.style.AppTheme_Amber_800_core
                -37120 -> commonsR.style.AppTheme_Amber_900_core

                -8014 -> commonsR.style.AppTheme_Orange_100_core
                -13184 -> commonsR.style.AppTheme_Orange_200_core
                -18611 -> commonsR.style.AppTheme_Orange_300_core
                -22746 -> commonsR.style.AppTheme_Orange_400_core
                -26624 -> commonsR.style.AppTheme_Orange_500_core
                -291840 -> commonsR.style.AppTheme_Orange_600_core
                -689152 -> commonsR.style.AppTheme_Orange_700_core
                -1086464 -> commonsR.style.AppTheme_Orange_800_core
                -1683200 -> commonsR.style.AppTheme_Orange_900_core

                -13124 -> commonsR.style.AppTheme_Deep_Orange_100_core
                -21615 -> commonsR.style.AppTheme_Deep_Orange_200_core
                -30107 -> commonsR.style.AppTheme_Deep_Orange_300_core
                -36797 -> commonsR.style.AppTheme_Deep_Orange_400_core
                -43230 -> commonsR.style.AppTheme_Deep_Orange_500_core
                -765666 -> commonsR.style.AppTheme_Deep_Orange_600_core
                -1684967 -> commonsR.style.AppTheme_Deep_Orange_700_core
                -2604267 -> commonsR.style.AppTheme_Deep_Orange_800_core
                -4246004 -> commonsR.style.AppTheme_Deep_Orange_900_core

                -2634552 -> commonsR.style.AppTheme_Brown_100_core
                -4412764 -> commonsR.style.AppTheme_Brown_200_core
                -6190977 -> commonsR.style.AppTheme_Brown_300_core
                -7508381 -> commonsR.style.AppTheme_Brown_400_core
                -8825528 -> commonsR.style.AppTheme_Brown_500_core
                -9614271 -> commonsR.style.AppTheme_Brown_600_core
                -10665929 -> commonsR.style.AppTheme_Brown_700_core
                -11652050 -> commonsR.style.AppTheme_Brown_800_core
                -12703965 -> commonsR.style.AppTheme_Brown_900_core

                -3155748 -> commonsR.style.AppTheme_Blue_Grey_100_core
                -5194811 -> commonsR.style.AppTheme_Blue_Grey_200_core
                -7297874 -> commonsR.style.AppTheme_Blue_Grey_300_core
                -8875876 -> commonsR.style.AppTheme_Blue_Grey_400_core
                -10453621 -> commonsR.style.AppTheme_Blue_Grey_500_core
                -11243910 -> commonsR.style.AppTheme_Blue_Grey_600_core
                -12232092 -> commonsR.style.AppTheme_Blue_Grey_700_core
                -13154481 -> commonsR.style.AppTheme_Blue_Grey_800_core
                -14273992 -> commonsR.style.AppTheme_Blue_Grey_900_core

                -1 -> commonsR.style.AppTheme_Grey_100_core
                -1118482 -> commonsR.style.AppTheme_Grey_200_core
                -2039584 -> commonsR.style.AppTheme_Grey_300_core
                -4342339 -> commonsR.style.AppTheme_Grey_400_core
                -6381922 -> commonsR.style.AppTheme_Grey_500_core
                -9079435 -> commonsR.style.AppTheme_Grey_600_core
                -10395295 -> commonsR.style.AppTheme_Grey_700_core
                -12434878 -> commonsR.style.AppTheme_Grey_800_core
                -16777216 -> commonsR.style.AppTheme_Grey_900_core

                else -> commonsR.style.AppTheme_Orange_700_core
            }
        }

        else -> {
            when (color) {
                -12846 -> commonsR.style.AppTheme_Red_100
                -1074534 -> commonsR.style.AppTheme_Red_200
                -1739917 -> commonsR.style.AppTheme_Red_300
                -1092784 -> commonsR.style.AppTheme_Red_400
                -769226 -> commonsR.style.AppTheme_Red_500
                -1754827 -> commonsR.style.AppTheme_Red_600
                -2937041 -> commonsR.style.AppTheme_Red_700
                -3790808 -> commonsR.style.AppTheme_Red_800
                -4776932 -> commonsR.style.AppTheme_Red_900

                -476208 -> commonsR.style.AppTheme_Pink_100
                -749647 -> commonsR.style.AppTheme_Pink_200
                -1023342 -> commonsR.style.AppTheme_Pink_300
                -1294214 -> commonsR.style.AppTheme_Pink_400
                -1499549 -> commonsR.style.AppTheme_Pink_500
                -2614432 -> commonsR.style.AppTheme_Pink_600
                -4056997 -> commonsR.style.AppTheme_Pink_700
                -5434281 -> commonsR.style.AppTheme_Pink_800
                -7860657 -> commonsR.style.AppTheme_Pink_900

                -1982745 -> commonsR.style.AppTheme_Purple_100
                -3238952 -> commonsR.style.AppTheme_Purple_200
                -4560696 -> commonsR.style.AppTheme_Purple_300
                -5552196 -> commonsR.style.AppTheme_Purple_400
                -6543440 -> commonsR.style.AppTheme_Purple_500
                -7461718 -> commonsR.style.AppTheme_Purple_600
                -8708190 -> commonsR.style.AppTheme_Purple_700
                -9823334 -> commonsR.style.AppTheme_Purple_800
                -11922292 -> commonsR.style.AppTheme_Purple_900

                -3029783 -> commonsR.style.AppTheme_Deep_Purple_100
                -5005861 -> commonsR.style.AppTheme_Deep_Purple_200
                -6982195 -> commonsR.style.AppTheme_Deep_Purple_300
                -8497214 -> commonsR.style.AppTheme_Deep_Purple_400
                -10011977 -> commonsR.style.AppTheme_Deep_Purple_500
                -10603087 -> commonsR.style.AppTheme_Deep_Purple_600
                -11457112 -> commonsR.style.AppTheme_Deep_Purple_700
                -12245088 -> commonsR.style.AppTheme_Deep_Purple_800
                -13558894 -> commonsR.style.AppTheme_Deep_Purple_900

                -3814679 -> commonsR.style.AppTheme_Indigo_100
                -6313766 -> commonsR.style.AppTheme_Indigo_200
                -8812853 -> commonsR.style.AppTheme_Indigo_300
                -10720320 -> commonsR.style.AppTheme_Indigo_400
                -12627531 -> commonsR.style.AppTheme_Indigo_500
                -13022805 -> commonsR.style.AppTheme_Indigo_600
                -13615201 -> commonsR.style.AppTheme_Indigo_700
                -14142061 -> commonsR.style.AppTheme_Indigo_800
                -15064194 -> commonsR.style.AppTheme_Indigo_900

                -4464901 -> commonsR.style.AppTheme_Blue_100
                -7288071 -> commonsR.style.AppTheme_Blue_200
                -10177034 -> commonsR.style.AppTheme_Blue_300
                -12409355 -> commonsR.style.AppTheme_Blue_400
                -14575885 -> commonsR.style.AppTheme_Blue_500
                -14776091 -> commonsR.style.AppTheme_Blue_600
                -15108398 -> commonsR.style.AppTheme_Blue_700
                -15374912 -> commonsR.style.AppTheme_Blue_800
                -15906911 -> commonsR.style.AppTheme_Blue_900

                -4987396 -> commonsR.style.AppTheme_Light_Blue_100
                -8268550 -> commonsR.style.AppTheme_Light_Blue_200
                -11549705 -> commonsR.style.AppTheme_Light_Blue_300
                -14043396 -> commonsR.style.AppTheme_Light_Blue_400
                -16537100 -> commonsR.style.AppTheme_Light_Blue_500
                -16540699 -> commonsR.style.AppTheme_Light_Blue_600
                -16611119 -> commonsR.style.AppTheme_Light_Blue_700
                -16615491 -> commonsR.style.AppTheme_Light_Blue_800
                -16689253 -> commonsR.style.AppTheme_Light_Blue_900

                -5051406 -> commonsR.style.AppTheme_Cyan_100
                -8331542 -> commonsR.style.AppTheme_Cyan_200
                -11677471 -> commonsR.style.AppTheme_Cyan_300
                -14235942 -> commonsR.style.AppTheme_Cyan_400
                -16728876 -> commonsR.style.AppTheme_Cyan_500
                -16732991 -> commonsR.style.AppTheme_Cyan_600
                -16738393 -> commonsR.style.AppTheme_Cyan_700
                -16743537 -> commonsR.style.AppTheme_Cyan_800
                -16752540 -> commonsR.style.AppTheme_Cyan_900

                -5054501 -> commonsR.style.AppTheme_Teal_100
                -8336444 -> commonsR.style.AppTheme_Teal_200
                -11684180 -> commonsR.style.AppTheme_Teal_300
                -14244198 -> commonsR.style.AppTheme_Teal_400
                -16738680 -> commonsR.style.AppTheme_Teal_500
                -16742021 -> commonsR.style.AppTheme_Teal_600
                -16746133 -> commonsR.style.AppTheme_Teal_700
                -16750244 -> commonsR.style.AppTheme_Teal_800
                -16757440 -> commonsR.style.AppTheme_Teal_900

                -3610935 -> commonsR.style.AppTheme_Green_100
                -5908825 -> commonsR.style.AppTheme_Green_200
                -8271996 -> commonsR.style.AppTheme_Green_300
                -10044566 -> commonsR.style.AppTheme_Green_400
                -11751600 -> commonsR.style.AppTheme_Green_500
                -12345273 -> commonsR.style.AppTheme_Green_600
                -13070788 -> commonsR.style.AppTheme_Green_700
                -13730510 -> commonsR.style.AppTheme_Green_800
                -14983648 -> commonsR.style.AppTheme_Green_900

                -2298424 -> commonsR.style.AppTheme_Light_Green_100
                -3808859 -> commonsR.style.AppTheme_Light_Green_200
                -5319295 -> commonsR.style.AppTheme_Light_Green_300
                -6501275 -> commonsR.style.AppTheme_Light_Green_400
                -7617718 -> commonsR.style.AppTheme_Light_Green_500
                -8604862 -> commonsR.style.AppTheme_Light_Green_600
                -9920712 -> commonsR.style.AppTheme_Light_Green_700
                -11171025 -> commonsR.style.AppTheme_Light_Green_800
                -13407970 -> commonsR.style.AppTheme_Light_Green_900

                -985917 -> commonsR.style.AppTheme_Lime_100
                -1642852 -> commonsR.style.AppTheme_Lime_200
                -2300043 -> commonsR.style.AppTheme_Lime_300
                -2825897 -> commonsR.style.AppTheme_Lime_400
                -3285959 -> commonsR.style.AppTheme_Lime_500
                -4142541 -> commonsR.style.AppTheme_Lime_600
                -5983189 -> commonsR.style.AppTheme_Lime_700
                -6382300 -> commonsR.style.AppTheme_Lime_800
                -8227049 -> commonsR.style.AppTheme_Lime_900

                -1596 -> commonsR.style.AppTheme_Yellow_100
                -2672 -> commonsR.style.AppTheme_Yellow_200
                -3722 -> commonsR.style.AppTheme_Yellow_300
                -4520 -> commonsR.style.AppTheme_Yellow_400
                -5317 -> commonsR.style.AppTheme_Yellow_500
                -141259 -> commonsR.style.AppTheme_Yellow_600
                -278483 -> commonsR.style.AppTheme_Yellow_700
                -415707 -> commonsR.style.AppTheme_Yellow_800
                -688361 -> commonsR.style.AppTheme_Yellow_900

                -4941 -> commonsR.style.AppTheme_Amber_100
                -8062 -> commonsR.style.AppTheme_Amber_200
                -10929 -> commonsR.style.AppTheme_Amber_300
                -13784 -> commonsR.style.AppTheme_Amber_400
                -16121 -> commonsR.style.AppTheme_Amber_500
                -19712 -> commonsR.style.AppTheme_Amber_600
                -24576 -> commonsR.style.AppTheme_Amber_700
                -28928 -> commonsR.style.AppTheme_Amber_800
                -37120 -> commonsR.style.AppTheme_Amber_900

                -8014 -> commonsR.style.AppTheme_Orange_100
                -13184 -> commonsR.style.AppTheme_Orange_200
                -18611 -> commonsR.style.AppTheme_Orange_300
                -22746 -> commonsR.style.AppTheme_Orange_400
                -26624 -> commonsR.style.AppTheme_Orange_500
                -291840 -> commonsR.style.AppTheme_Orange_600
                -689152 -> commonsR.style.AppTheme_Orange_700
                -1086464 -> commonsR.style.AppTheme_Orange_800
                -1683200 -> commonsR.style.AppTheme_Orange_900

                -13124 -> commonsR.style.AppTheme_Deep_Orange_100
                -21615 -> commonsR.style.AppTheme_Deep_Orange_200
                -30107 -> commonsR.style.AppTheme_Deep_Orange_300
                -36797 -> commonsR.style.AppTheme_Deep_Orange_400
                -43230 -> commonsR.style.AppTheme_Deep_Orange_500
                -765666 -> commonsR.style.AppTheme_Deep_Orange_600
                -1684967 -> commonsR.style.AppTheme_Deep_Orange_700
                -2604267 -> commonsR.style.AppTheme_Deep_Orange_800
                -4246004 -> commonsR.style.AppTheme_Deep_Orange_900

                -2634552 -> commonsR.style.AppTheme_Brown_100
                -4412764 -> commonsR.style.AppTheme_Brown_200
                -6190977 -> commonsR.style.AppTheme_Brown_300
                -7508381 -> commonsR.style.AppTheme_Brown_400
                -8825528 -> commonsR.style.AppTheme_Brown_500
                -9614271 -> commonsR.style.AppTheme_Brown_600
                -10665929 -> commonsR.style.AppTheme_Brown_700
                -11652050 -> commonsR.style.AppTheme_Brown_800
                -12703965 -> commonsR.style.AppTheme_Brown_900

                -3155748 -> commonsR.style.AppTheme_Blue_Grey_100
                -5194811 -> commonsR.style.AppTheme_Blue_Grey_200
                -7297874 -> commonsR.style.AppTheme_Blue_Grey_300
                -8875876 -> commonsR.style.AppTheme_Blue_Grey_400
                -10453621 -> commonsR.style.AppTheme_Blue_Grey_500
                -11243910 -> commonsR.style.AppTheme_Blue_Grey_600
                -12232092 -> commonsR.style.AppTheme_Blue_Grey_700
                -13154481 -> commonsR.style.AppTheme_Blue_Grey_800
                -14273992 -> commonsR.style.AppTheme_Blue_Grey_900

                -1 -> commonsR.style.AppTheme_Grey_100
                -1118482 -> commonsR.style.AppTheme_Grey_200
                -2039584 -> commonsR.style.AppTheme_Grey_300
                -4342339 -> commonsR.style.AppTheme_Grey_400
                -6381922 -> commonsR.style.AppTheme_Grey_500
                -9079435 -> commonsR.style.AppTheme_Grey_600
                -10395295 -> commonsR.style.AppTheme_Grey_700
                -12434878 -> commonsR.style.AppTheme_Grey_800
                -16777216 -> commonsR.style.AppTheme_Grey_900

                else -> commonsR.style.AppTheme_Orange_700
            }
        }
    }

fun Activity.showSideloadingDialog() {
    AppSideloadedDialog(this) {
        finish()
    }
}

fun Activity.checkAppSideloading(): Boolean {
    val isSideloaded = when (baseConfig.appSideloadingStatus) {
        SIDELOADING_TRUE -> true
        SIDELOADING_FALSE -> false
        else -> isAppSideloaded()
    }

    baseConfig.appSideloadingStatus = if (isSideloaded) SIDELOADING_TRUE else SIDELOADING_FALSE
    if (isSideloaded) {
        showSideloadingDialog()
    }

    return isSideloaded
}

@SuppressLint("UseCompatLoadingForDrawables")
fun Activity.isAppSideloaded(): Boolean {
    return try {
        getDrawable(R.drawable.ic_camera_vector)
        false
    } catch (e: Exception) {
        true
    }
}

fun Activity.handleLockedFolderOpening(path: String, callback: (success: Boolean) -> Unit) {
    if (baseConfig.isFolderProtected(path)) {
        SecurityDialog(
            this,
            baseConfig.getFolderProtectionHash(path),
            baseConfig.getFolderProtectionType(path)
        ) { _, _, success ->
            callback(success)
        }
    } else {
        callback(true)
    }
}

fun Activity.handleHiddenFolderPasswordProtection(callback: () -> Unit) {
    if (baseConfig.isHiddenPasswordProtectionOn) {
        SecurityDialog(
            this,
            baseConfig.hiddenPasswordHash,
            baseConfig.hiddenProtectionType
        ) { _, _, success ->
            if (success) {
                callback()
            }
        }
    } else {
        callback()
    }
}

fun Activity.isAppInstalledOnSDCard(): Boolean = try {
    val applicationInfo = packageManager.getPackageInfo(packageName, 0).applicationInfo
    (applicationInfo.flags and ApplicationInfo.FLAG_EXTERNAL_STORAGE) == ApplicationInfo.FLAG_EXTERNAL_STORAGE
} catch (e: Exception) {
    false
}

fun Activity.onApplyWindowInsets(callback: (WindowInsetsCompat) -> Unit) {
    window.decorView.setOnApplyWindowInsetsListener { view, insets ->
        callback(WindowInsetsCompat.toWindowInsetsCompat(insets))
        view.onApplyWindowInsets(insets)
        insets
    }
}


fun BaseSimpleActivity.showOTGPermissionDialog(path: String) {
    runOnUiThread {
        if (!isDestroyed && !isFinishing) {
            WritePermissionDialog(this, WritePermissionDialogMode.Otg) {
                Intent(Intent.ACTION_OPEN_DOCUMENT_TREE).apply {
                    try {
                        startActivityForResult(this, OPEN_DOCUMENT_TREE_OTG)
                        checkedDocumentPath = path
                        return@apply
                    } catch (e: Exception) {
                        type = "*/*"
                    }

                    try {
                        startActivityForResult(this, OPEN_DOCUMENT_TREE_OTG)
                        checkedDocumentPath = path
                    } catch (e: ActivityNotFoundException) {
                        toast(R.string.system_service_disabled, Toast.LENGTH_LONG)
                    } catch (e: Exception) {
                        toast(R.string.unknown_error_occurred)
                    }
                }
            }
        }
    }
}

@SuppressLint("InlinedApi")
fun BaseSimpleActivity.isShowingSAFDialogSdk30(path: String): Boolean {
    return if (isAccessibleWithSAFSdk30(path) && !hasProperStoredFirstParentUri(path)) {
        runOnUiThread {
            if (!isDestroyed && !isFinishing) {
                val level = getFirstParentLevel(path)
                WritePermissionDialog(
                    this,
                    WritePermissionDialogMode.OpenDocumentTreeSDK30(
                        path.getFirstParentPath(
                            this,
                            level
                        )
                    )
                ) {
                    Intent(Intent.ACTION_OPEN_DOCUMENT_TREE).apply {
                        putExtra(EXTRA_SHOW_ADVANCED, true)
                        putExtra(
                            DocumentsContract.EXTRA_INITIAL_URI,
                            createFirstParentTreeUriUsingRootTree(path)
                        )
                        try {
                            startActivityForResult(this, OPEN_DOCUMENT_TREE_FOR_SDK_30)
                            checkedDocumentPath = path
                            return@apply
                        } catch (e: Exception) {
                            type = "*/*"
                        }

                        try {
                            startActivityForResult(this, OPEN_DOCUMENT_TREE_FOR_SDK_30)
                            checkedDocumentPath = path
                        } catch (e: ActivityNotFoundException) {
                            toast(R.string.system_service_disabled, Toast.LENGTH_LONG)
                        } catch (e: Exception) {
                            toast(R.string.unknown_error_occurred)
                        }
                    }
                }
            }
        }
        true
    } else {
        false
    }
}

@SuppressLint("InlinedApi")
fun BaseSimpleActivity.isShowingSAFCreateDocumentDialogSdk30(path: String): Boolean {
    return if (!hasProperStoredDocumentUriSdk30(path)) {
        runOnUiThread {
            if (!isDestroyed && !isFinishing) {
                WritePermissionDialog(this, WritePermissionDialogMode.CreateDocumentSDK30) {
                    Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
                        type = DocumentsContract.Document.MIME_TYPE_DIR
                        putExtra(EXTRA_SHOW_ADVANCED, true)
                        addCategory(Intent.CATEGORY_OPENABLE)
                        putExtra(
                            DocumentsContract.EXTRA_INITIAL_URI,
                            buildDocumentUriSdk30(path.getParentPath())
                        )
                        putExtra(Intent.EXTRA_TITLE, path.getFilenameFromPath())
                        try {
                            startActivityForResult(this, CREATE_DOCUMENT_SDK_30)
                            checkedDocumentPath = path
                            return@apply
                        } catch (e: Exception) {
                            type = "*/*"
                        }

                        try {
                            startActivityForResult(this, CREATE_DOCUMENT_SDK_30)
                            checkedDocumentPath = path
                        } catch (e: ActivityNotFoundException) {
                            toast(R.string.system_service_disabled, Toast.LENGTH_LONG)
                        } catch (e: Exception) {
                            toast(R.string.unknown_error_occurred)
                        }
                    }
                }
            }
        }
        true
    } else {
        false
    }
}

fun BaseSimpleActivity.isShowingAndroidSAFDialog(path: String): Boolean {
    return if (isRestrictedSAFOnlyRoot(path) && (getAndroidTreeUri(path).isEmpty() || !hasProperStoredAndroidTreeUri(
            path
        ))
    ) {
        runOnUiThread {
            if (!isDestroyed && !isFinishing) {
                ConfirmationAdvancedDialog(
                    this,
                    "",
                    R.string.confirm_storage_access_android_text,
                    R.string.ok,
                    R.string.cancel
                ) { success ->
                    if (success) {
                        Intent(Intent.ACTION_OPEN_DOCUMENT_TREE).apply {
                            putExtra(EXTRA_SHOW_ADVANCED, true)
                            putExtra(
                                DocumentsContract.EXTRA_INITIAL_URI,
                                createAndroidDataOrObbUri(path)
                            )
                            try {
                                startActivityForResult(
                                    this,
                                    OPEN_DOCUMENT_TREE_FOR_ANDROID_DATA_OR_OBB
                                )
                                checkedDocumentPath = path
                                return@apply
                            } catch (e: Exception) {
                                type = "*/*"
                            }

                            try {
                                startActivityForResult(
                                    this,
                                    OPEN_DOCUMENT_TREE_FOR_ANDROID_DATA_OR_OBB
                                )
                                checkedDocumentPath = path
                            } catch (e: ActivityNotFoundException) {
                                toast(R.string.system_service_disabled, Toast.LENGTH_LONG)
                            } catch (e: Exception) {
                                toast(R.string.unknown_error_occurred)
                            }
                        }
                    }
                }
            }
        }
        true
    } else {
        false
    }
}

private fun BaseSimpleActivity.deleteSdk30(
    fileDirItem: FileDirItem,
    callback: ((wasSuccess: Boolean) -> Unit)?
) {
    val fileUris = getFileUrisFromFileDirItems(arrayListOf(fileDirItem))
    deleteSDK30Uris(fileUris) { success ->
        runOnUiThread {
            callback?.invoke(success)
        }
    }
}

private fun deleteRecursively(file: File, context: Context): Boolean {
    if (file.isDirectory) {
        val files = file.listFiles() ?: return file.delete()
        for (child in files) {
            deleteRecursively(child, context)
        }
    }

    val deleted = file.delete()
    if (deleted) {
        context.deleteFromMediaStore(file.absolutePath)
    }
    return deleted
}

fun BaseSimpleActivity.deleteFileBg(
    fileDirItem: FileDirItem,
    allowDeleteFolder: Boolean = false,
    isDeletingMultipleFiles: Boolean,
    callback: ((wasSuccess: Boolean) -> Unit)? = null,
) {
    val path = fileDirItem.path
    if (isRestrictedSAFOnlyRoot(path)) {
        deleteAndroidSAFDirectory(path, allowDeleteFolder, callback)
    } else {
        val file = File(path)
        if (!isRPlus() && file.absolutePath.startsWith(internalStoragePath) && !file.canWrite()) {
            callback?.invoke(false)
            return
        }

        var fileDeleted =
            !isPathOnOTG(path) && ((!file.exists() && file.length() == 0L) || file.delete())
        if (fileDeleted) {
            deleteFromMediaStore(path) { needsRescan ->
                if (needsRescan) {
                    rescanAndDeletePath(path) {
                        runOnUiThread {
                            callback?.invoke(true)
                        }
                    }
                } else {
                    runOnUiThread {
                        callback?.invoke(true)
                    }
                }
            }
        } else {
            if (getIsPathDirectory(file.absolutePath) && allowDeleteFolder) {
                fileDeleted = deleteRecursively(file, this)
            }

            if (!fileDeleted) {
                if (needsStupidWritePermissions(path)) {
                    handleSAFDialog(path) {
                        if (it) {
                            trySAFFileDelete(fileDirItem, allowDeleteFolder, callback)
                        }
                    }
                } else if (isAccessibleWithSAFSdk30(path)) {
                    if (canManageMedia()) {
                        deleteSdk30(fileDirItem, callback)
                    } else {
                        handleSAFDialogSdk30(path) {
                            if (it) {
                                deleteDocumentWithSAFSdk30(fileDirItem, allowDeleteFolder, callback)
                            }
                        }
                    }
                } else if (isRPlus() && !isDeletingMultipleFiles) {
                    deleteSdk30(fileDirItem, callback)
                } else {
                    callback?.invoke(false)
                }
            }
        }
    }
}

private fun BaseSimpleActivity.renameCasually(
    oldPath: String,
    newPath: String,
    isRenamingMultipleFiles: Boolean,
    callback: ((success: Boolean, android30RenameFormat: Android30RenameFormat) -> Unit)?
) {
    val oldFile = File(oldPath)
    val newFile = File(newPath)
    val tempFile = try {
        createTempFile(oldFile) ?: return
    } catch (exception: Exception) {
        if (isRPlus() && exception is java.nio.file.FileSystemException) {
            // if we are renaming multiple files at once, we should give the Android 30+ permission dialog all uris together, not one by one
            if (isRenamingMultipleFiles) {
                callback?.invoke(false, Android30RenameFormat.CONTENT_RESOLVER)
            } else {
                val fileUris =
                    getFileUrisFromFileDirItems(arrayListOf(File(oldPath).toFileDirItem(this)))
                updateSDK30Uris(fileUris) { success ->
                    if (success) {
                        val values = ContentValues().apply {
                            put(MediaStore.Images.Media.DISPLAY_NAME, newPath.getFilenameFromPath())
                        }

                        try {
                            contentResolver.update(fileUris.first(), values, null, null)
                            callback?.invoke(true, Android30RenameFormat.NONE)
                        } catch (e: Exception) {
                            showErrorToast(e)
                            callback?.invoke(false, Android30RenameFormat.NONE)
                        }
                    } else {
                        callback?.invoke(false, Android30RenameFormat.NONE)
                    }
                }
            }
        } else {
            if (exception is IOException && File(oldPath).isDirectory && isRestrictedWithSAFSdk30(
                    oldPath
                )
            ) {
                toast(R.string.cannot_rename_folder)
            } else {
                showErrorToast(exception)
            }
            callback?.invoke(false, Android30RenameFormat.NONE)
        }
        return
    }

    val oldToTempSucceeds = oldFile.renameTo(tempFile)
    val tempToNewSucceeds = tempFile.renameTo(newFile)
    if (oldToTempSucceeds && tempToNewSucceeds) {
        if (newFile.isDirectory) {
            updateInMediaStore(oldPath, newPath)
            rescanPath(newPath) {
                runOnUiThread {
                    callback?.invoke(true, Android30RenameFormat.NONE)
                }
                if (!oldPath.equals(newPath, true)) {
                    deleteFromMediaStore(oldPath)
                }
                scanPathRecursively(newPath)
            }
        } else {
            if (!baseConfig.keepLastModified) {
                newFile.setLastModified(System.currentTimeMillis())
            }
            updateInMediaStore(oldPath, newPath)
            scanPathsRecursively(arrayListOf(newPath)) {
                if (!oldPath.equals(newPath, true)) {
                    deleteFromMediaStore(oldPath)
                }
                runOnUiThread {
                    callback?.invoke(true, Android30RenameFormat.NONE)
                }
            }
        }
    } else {
        tempFile.delete()
        newFile.delete()
        if (isRPlus()) {
            // if we are renaming multiple files at once, we should give the Android 30+ permission dialog all uris together, not one by one
            if (isRenamingMultipleFiles) {
                callback?.invoke(false, Android30RenameFormat.SAF)
            } else {
                val fileUris =
                    getFileUrisFromFileDirItems(arrayListOf(File(oldPath).toFileDirItem(this)))
                updateSDK30Uris(fileUris) { success ->
                    if (!success) {
                        return@updateSDK30Uris
                    }
                    try {
                        val sourceUri = fileUris.first()
                        val sourceFile = File(oldPath).toFileDirItem(this)

                        if (oldPath.equals(newPath, true)) {
                            val tempDestination = try {
                                createTempFile(File(sourceFile.path)) ?: return@updateSDK30Uris
                            } catch (exception: Exception) {
                                showErrorToast(exception)
                                callback?.invoke(false, Android30RenameFormat.NONE)
                                return@updateSDK30Uris
                            }

                            val copyTempSuccess =
                                copySingleFileSdk30(sourceFile, tempDestination.toFileDirItem(this))
                            if (copyTempSuccess) {
                                contentResolver.delete(sourceUri, null)
                                tempDestination.renameTo(File(newPath))
                                if (!baseConfig.keepLastModified) {
                                    newFile.setLastModified(System.currentTimeMillis())
                                }
                                updateInMediaStore(oldPath, newPath)
                                scanPathsRecursively(arrayListOf(newPath)) {
                                    runOnUiThread {
                                        callback?.invoke(true, Android30RenameFormat.NONE)
                                    }
                                }
                            } else {
                                callback?.invoke(false, Android30RenameFormat.NONE)
                            }
                        } else {
                            val destinationFile = FileDirItem(
                                newPath,
                                newPath.getFilenameFromPath(),
                                sourceFile.isDirectory,
                                sourceFile.children,
                                sourceFile.size,
                                sourceFile.modified
                            )
                            val copySuccessful = copySingleFileSdk30(sourceFile, destinationFile)
                            if (copySuccessful) {
                                if (!baseConfig.keepLastModified) {
                                    newFile.setLastModified(System.currentTimeMillis())
                                }
                                contentResolver.delete(sourceUri, null)
                                updateInMediaStore(oldPath, newPath)
                                scanPathsRecursively(arrayListOf(newPath)) {
                                    runOnUiThread {
                                        callback?.invoke(true, Android30RenameFormat.NONE)
                                    }
                                }
                            } else {
                                toast(R.string.unknown_error_occurred)
                                callback?.invoke(false, Android30RenameFormat.NONE)
                            }
                        }

                    } catch (e: Exception) {
                        showErrorToast(e)
                        callback?.invoke(false, Android30RenameFormat.NONE)
                    }
                }
            }
        } else {
            toast(R.string.unknown_error_occurred)
            callback?.invoke(false, Android30RenameFormat.NONE)
        }
    }
}

fun Activity.showDonateOrUpgradeDialog() {
    if (getCanAppBeUpgraded()) {
        UpgradeToProDialog(this)
    } else if (!isOrWasThankYouInstalled()) {
        DonateDialog(this)
    }
}

fun Activity.rescanPaths(paths: List<String>, callback: (() -> Unit)? = null) {
    applicationContext.rescanPaths(paths, callback)
}

fun Activity.createTempFile(file: File): File? {
    return if (file.isDirectory) {
        createTempDir("temp", "${System.currentTimeMillis()}", file.parentFile)
    } else {
        if (isRPlus()) {
            // this can throw FileSystemException, lets catch and handle it at the place calling this function
            kotlin.io.path.createTempFile(
                file.parentFile.toPath(),
                "temp",
                "${System.currentTimeMillis()}"
            ).toFile()
        } else {
            createTempFile("temp", "${System.currentTimeMillis()}", file.parentFile)
        }
    }
}

fun Activity.rescanPath(path: String, callback: (() -> Unit)? = null) {
    applicationContext.rescanPath(path, callback)
}


fun BaseSimpleActivity.isShowingSAFDialog(path: String): Boolean {
    return if ((!isRPlus() && isPathOnSD(path) && !isSDCardSetAsDefaultStorage() && (baseConfig.sdTreeUri.isEmpty() || !hasProperStoredTreeUri(
            false
        )))
    ) {
        runOnUiThread {
            if (!isDestroyed && !isFinishing) {
                WritePermissionDialog(this, WritePermissionDialogMode.SdCard) {
                    Intent(Intent.ACTION_OPEN_DOCUMENT_TREE).apply {
                        putExtra(EXTRA_SHOW_ADVANCED, true)
                        try {
                            startActivityForResult(this, OPEN_DOCUMENT_TREE_SD)
                            checkedDocumentPath = path
                            return@apply
                        } catch (e: Exception) {
                            type = "*/*"
                        }

                        try {
                            startActivityForResult(this, OPEN_DOCUMENT_TREE_SD)
                            checkedDocumentPath = path
                        } catch (e: ActivityNotFoundException) {
                            toast(
                                R.string.system_service_disabled,
                                Toast.LENGTH_LONG
                            )
                        } catch (e: Exception) {
                            toast(R.string.unknown_error_occurred)
                        }
                    }
                }
            }
        }
        true
    } else {
        false
    }
}

fun BaseSimpleActivity.copySingleFileSdk30(source: FileDirItem, destination: FileDirItem): Boolean {
    val directory = destination.getParentPath()
    if (!createDirectorySync(directory)) {
        val error = String.format(getString(R.string.could_not_create_folder), directory)
        showErrorToast(error)
        return false
    }

    var inputStream: InputStream? = null
    var out: OutputStream? = null
    try {

        out = getFileOutputStreamSync(destination.path, source.path.getMimeType())
        inputStream = getFileInputStreamSync(source.path)!!

        var copiedSize = 0L
        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
        var bytes = inputStream.read(buffer)
        while (bytes >= 0) {
            out!!.write(buffer, 0, bytes)
            copiedSize += bytes
            bytes = inputStream.read(buffer)
        }

        out?.flush()

        return if (source.size == copiedSize && getDoesFilePathExist(destination.path)) {
            if (baseConfig.keepLastModified) {
                copyOldLastModified(source.path, destination.path)
                val lastModified = File(source.path).lastModified()
                if (lastModified != 0L) {
                    File(destination.path).setLastModified(lastModified)
                }
            }
            true
        } else {
            false
        }
    } finally {
        inputStream?.close()
        out?.close()
    }
}


fun BaseSimpleActivity.copyOldLastModified(sourcePath: String, destinationPath: String) {
    val projection =
        arrayOf(MediaStore.Images.Media.DATE_TAKEN, MediaStore.Images.Media.DATE_MODIFIED)
    val uri = MediaStore.Files.getContentUri("external")
    val selection = "${MediaStore.MediaColumns.DATA} = ?"
    var selectionArgs = arrayOf(sourcePath)
    val cursor =
        applicationContext.contentResolver.query(uri, projection, selection, selectionArgs, null)

    cursor?.use {
        if (cursor.moveToFirst()) {
            val dateTaken = cursor.getLongValue(MediaStore.Images.Media.DATE_TAKEN)
            val dateModified = cursor.getIntValue(MediaStore.Images.Media.DATE_MODIFIED)

            val values = ContentValues().apply {
                put(MediaStore.Images.Media.DATE_TAKEN, dateTaken)
                put(MediaStore.Images.Media.DATE_MODIFIED, dateModified)
            }

            selectionArgs = arrayOf(destinationPath)
            applicationContext.contentResolver.update(uri, values, selection, selectionArgs)
        }
    }
}


fun Activity.getFinalUriFromPath(path: String, applicationId: String): Uri? {
    val uri = try {
        ensurePublicUri(path, applicationId)
    } catch (e: Exception) {
        showErrorToast(e)
        return null
    }

    if (uri == null) {
        toast(R.string.unknown_error_occurred)
        return null
    }

    return uri
}

fun Activity.appLaunched(appId: String) {
    baseConfig.internalStoragePath = getInternalStoragePath()
    updateSDCardPath()
    baseConfig.appId = appId
    if (baseConfig.appRunCount == 0) {
        baseConfig.wasOrangeIconChecked = true
        checkAppIconColor()
    } else if (!baseConfig.wasOrangeIconChecked) {
        baseConfig.wasOrangeIconChecked = true
        val primaryColor = resources.getColor(R.color.color_primary)
        if (baseConfig.appIconColor != primaryColor) {
            getAppIconColors().forEachIndexed { index, color ->
                toggleAppIconColor(appId, index, color, false)
            }

            val defaultClassName =
                "${baseConfig.appId.removeSuffix(".debug")}.activities.SplashActivity"
            packageManager.setComponentEnabledSetting(
                ComponentName(baseConfig.appId, defaultClassName),
                PackageManager.COMPONENT_ENABLED_STATE_DEFAULT,
                PackageManager.DONT_KILL_APP
            )

            val orangeClassName =
                "${baseConfig.appId.removeSuffix(".debug")}.activities.SplashActivity.Orange"
            packageManager.setComponentEnabledSetting(
                ComponentName(baseConfig.appId, orangeClassName),
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                PackageManager.DONT_KILL_APP
            )

            baseConfig.appIconColor = primaryColor
            baseConfig.lastIconColor = primaryColor
        }
    }

    baseConfig.appRunCount++
    if (baseConfig.appRunCount % 30 == 0 && !isAProApp()) {
        if (!resources.getBoolean(R.bool.hide_google_relations)) {
            showDonateOrUpgradeDialog()
        }
    }

    if (baseConfig.appRunCount % 40 == 0 && !baseConfig.wasAppRated) {
        if (!resources.getBoolean(R.bool.hide_google_relations)) {
            RateStarsDialog(this)
        }
    }
}

fun BaseSimpleActivity.renameFile(
    oldPath: String,
    newPath: String,
    isRenamingMultipleFiles: Boolean,
    callback: ((success: Boolean, android30RenameFormat: Android30RenameFormat) -> Unit)? = null
) {
    if (isRestrictedSAFOnlyRoot(oldPath)) {
        handleAndroidSAFDialog(oldPath) {
            if (!it) {
                runOnUiThread {
                    callback?.invoke(false, Android30RenameFormat.NONE)
                }
                return@handleAndroidSAFDialog
            }

            try {
                ensureBackgroundThread {
                    val success = renameAndroidSAFDocument(oldPath, newPath)
                    runOnUiThread {
                        callback?.invoke(success, Android30RenameFormat.NONE)
                    }
                }
            } catch (e: Exception) {
                showErrorToast(e)
                runOnUiThread {
                    callback?.invoke(false, Android30RenameFormat.NONE)
                }
            }
        }
    } else if (isAccessibleWithSAFSdk30(oldPath)) {
        if (canManageMedia() && !File(oldPath).isDirectory && isPathOnInternalStorage(oldPath)) {
            renameCasually(oldPath, newPath, isRenamingMultipleFiles, callback)
        } else {
            handleSAFDialogSdk30(oldPath) {
                if (!it) {
                    return@handleSAFDialogSdk30
                }

                try {
                    ensureBackgroundThread {
                        val success = renameDocumentSdk30(oldPath, newPath)
                        if (success) {
                            updateInMediaStore(oldPath, newPath)
                            rescanPath(newPath) {
                                runOnUiThread {
                                    callback?.invoke(true, Android30RenameFormat.NONE)
                                }
                                if (!oldPath.equals(newPath, true)) {
                                    deleteFromMediaStore(oldPath)
                                }
                                scanPathRecursively(newPath)
                            }
                        } else {
                            runOnUiThread {
                                callback?.invoke(false, Android30RenameFormat.NONE)
                            }
                        }
                    }
                } catch (e: Exception) {
                    showErrorToast(e)
                    runOnUiThread {
                        callback?.invoke(false, Android30RenameFormat.NONE)
                    }
                }
            }
        }
    } else if (needsStupidWritePermissions(newPath)) {
        handleSAFDialog(newPath) {
            if (!it) {
                return@handleSAFDialog
            }

            val document = getSomeDocumentFile(oldPath)
            if (document == null || (File(oldPath).isDirectory != document.isDirectory)) {
                runOnUiThread {
                    toast(R.string.unknown_error_occurred)
                    callback?.invoke(false, Android30RenameFormat.NONE)
                }
                return@handleSAFDialog
            }

            try {
                ensureBackgroundThread {
                    try {
                        DocumentsContract.renameDocument(
                            applicationContext.contentResolver,
                            document.uri,
                            newPath.getFilenameFromPath()
                        )
                    } catch (ignored: FileNotFoundException) {
                        // FileNotFoundException is thrown in some weird cases, but renaming works just fine
                    } catch (e: Exception) {
                        showErrorToast(e)
                        callback?.invoke(false, Android30RenameFormat.NONE)
                        return@ensureBackgroundThread
                    }

                    updateInMediaStore(oldPath, newPath)
                    rescanPaths(arrayListOf(oldPath, newPath)) {
                        if (!baseConfig.keepLastModified) {
                            updateLastModified(newPath, System.currentTimeMillis())
                        }
                        deleteFromMediaStore(oldPath)
                        runOnUiThread {
                            callback?.invoke(true, Android30RenameFormat.NONE)
                        }
                    }
                }
            } catch (e: Exception) {
                showErrorToast(e)
                runOnUiThread {
                    callback?.invoke(false, Android30RenameFormat.NONE)
                }
            }
        }
    } else renameCasually(oldPath, newPath, isRenamingMultipleFiles, callback)
}

@SuppressLint("UseCompatLoadingForDrawables")
fun Activity.setupDialogStuff(
    view: View,
    dialog: AlertDialog.Builder,
    titleId: Int = 0,
    titleText: String = "",
    cancelOnTouchOutside: Boolean = true,
    callback: ((alertDialog: AlertDialog) -> Unit)? = null
) {
    if (isDestroyed || isFinishing) {
        return
    }

    val textColor = getProperTextColor()
    val backgroundColor = getProperBackgroundColor()
    val primaryColor = getProperPrimaryColor()
    if (view is ViewGroup) {
        updateTextColors(view)
    } else if (view is MyTextView) {
        view.setColors(textColor, primaryColor, backgroundColor)
    }

    if (dialog is MaterialAlertDialogBuilder) {
        dialog.create().apply {
            if (titleId != 0) {
                setTitle(titleId)
            } else if (titleText.isNotEmpty()) {
                setTitle(titleText)
            }

            setView(view)
            setCancelable(cancelOnTouchOutside)
            if (!isFinishing) {
                show()
            }
            getButton(Dialog.BUTTON_POSITIVE)?.setTextColor(primaryColor)
            getButton(Dialog.BUTTON_NEGATIVE)?.setTextColor(primaryColor)
            getButton(Dialog.BUTTON_NEUTRAL)?.setTextColor(primaryColor)
            callback?.invoke(this)
        }
    } else {
        var title: DialogTitleBinding? = null
        if (titleId != 0 || titleText.isNotEmpty()) {
            title = DialogTitleBinding.inflate(layoutInflater, null, false)
            title.dialogTitleTextview.apply {
                if (titleText.isNotEmpty()) {
                    text = titleText
                } else {
                    setText(titleId)
                }
                setTextColor(textColor)
            }
        }

        // if we use the same primary and background color, use the text color for dialog confirmation buttons
        val dialogButtonColor = if (primaryColor == baseConfig.backgroundColor) {
            textColor
        } else {
            primaryColor
        }

        dialog.create().apply {
            setView(view)
            requestWindowFeature(Window.FEATURE_NO_TITLE)
            setCustomTitle(title?.root)
            setCanceledOnTouchOutside(cancelOnTouchOutside)
            if (!isFinishing) {
                show()
            }
            getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(dialogButtonColor)
            getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(dialogButtonColor)
            getButton(AlertDialog.BUTTON_NEUTRAL).setTextColor(dialogButtonColor)

            val bgDrawable = when {
                isBlackAndWhiteTheme() -> resources.getDrawable(
                    R.drawable.black_dialog_background,
                    theme
                )

                baseConfig.isUsingSystemTheme -> resources.getDrawable(
                    R.drawable.dialog_you_background,
                    theme
                )

                else -> resources.getColoredDrawableWithColor(
                    R.drawable.dialog_bg,
                    baseConfig.backgroundColor
                )
            }

            window?.setBackgroundDrawable(bgDrawable)
            callback?.invoke(this)
        }
    }
}

fun Activity.sharePathIntent(path: String, applicationId: String) {
    ensureBackgroundThread {
        val newUri = getFinalUriFromPath(path, applicationId) ?: return@ensureBackgroundThread
        Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(EXTRA_STREAM, newUri)
            type = getUriMimeType(path, newUri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            grantUriPermission("android", newUri, Intent.FLAG_GRANT_READ_URI_PERMISSION)

            try {
                startActivity(Intent.createChooser(this, getString(R.string.share_via)))
            } catch (e: ActivityNotFoundException) {
                toast(R.string.no_app_found)
            } catch (e: RuntimeException) {
                if (e.cause is TransactionTooLargeException) {
                    toast(R.string.maximum_share_reached)
                } else {
                    showErrorToast(e)
                }
            } catch (e: Exception) {
                showErrorToast(e)
            }
        }
    }
}

fun Activity.sharePathsIntent(paths: List<String>, applicationId: String) {
    ensureBackgroundThread {
        if (paths.size == 1) {
            sharePathIntent(paths.first(), applicationId)
        } else {
            val uriPaths = java.util.ArrayList<String>()
            val newUris = paths.map {
                val uri = getFinalUriFromPath(it, applicationId) ?: return@ensureBackgroundThread
                uriPaths.add(uri.path!!)
                uri
            } as java.util.ArrayList<Uri>

            var mimeType = uriPaths.getMimeType()
            if (mimeType.isEmpty() || mimeType == "*/*") {
                mimeType = paths.getMimeType()
            }

            Intent().apply {
                action = Intent.ACTION_SEND_MULTIPLE
                type = mimeType
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                putParcelableArrayListExtra(EXTRA_STREAM, newUris)

                try {
                    startActivity(Intent.createChooser(this, getString(R.string.share_via)))
                } catch (e: ActivityNotFoundException) {
                    toast(R.string.no_app_found)
                } catch (e: RuntimeException) {
                    if (e.cause is TransactionTooLargeException) {
                        toast(R.string.maximum_share_reached)
                    } else {
                        showErrorToast(e)
                    }
                } catch (e: Exception) {
                    showErrorToast(e)
                }
            }
        }
    }
}

inline fun <T : ViewBinding> Activity.viewBinding(crossinline bindingInflater: (LayoutInflater) -> T) =
    lazy(LazyThreadSafetyMode.NONE) {
        bindingInflater.invoke(layoutInflater)
    }


fun Activity.getAlertDialogBuilder() = if (baseConfig.isUsingSystemTheme) {
    MaterialAlertDialogBuilder(this)
} else {
    AlertDialog.Builder(this)
}

private fun createCasualFileOutputStream(
    activity: BaseSimpleActivity,
    targetFile: File
): OutputStream? {
    if (targetFile.parentFile?.exists() == false) {
        targetFile.parentFile?.mkdirs()
    }

    return try {
        FileOutputStream(targetFile)
    } catch (e: Exception) {
        activity.showErrorToast(e)
        null
    }
}

fun BaseSimpleActivity.getFileOutputStream(
    fileDirItem: FileDirItem,
    allowCreatingNewFile: Boolean = false,
    callback: (outputStream: OutputStream?) -> Unit
) {
    val targetFile = File(fileDirItem.path)
    when {
        isRestrictedSAFOnlyRoot(fileDirItem.path) -> {
            handleAndroidSAFDialog(fileDirItem.path) {
                if (!it) {
                    return@handleAndroidSAFDialog
                }

                val uri = getAndroidSAFUri(fileDirItem.path)
                if (!getDoesFilePathExist(fileDirItem.path)) {
                    createAndroidSAFFile(fileDirItem.path)
                }
                callback.invoke(applicationContext.contentResolver.openOutputStream(uri, "wt"))
            }
        }

        needsStupidWritePermissions(fileDirItem.path) -> {
            handleSAFDialog(fileDirItem.path) {
                if (!it) {
                    return@handleSAFDialog
                }

                var document = getDocumentFile(fileDirItem.path)
                if (document == null && allowCreatingNewFile) {
                    document = getDocumentFile(fileDirItem.getParentPath())
                }

                if (document == null) {
                    showFileCreateError(fileDirItem.path)
                    callback(null)
                    return@handleSAFDialog
                }

                if (!getDoesFilePathExist(fileDirItem.path)) {
                    document = getDocumentFile(fileDirItem.path) ?: document.createFile(
                        "",
                        fileDirItem.name
                    )
                }

                if (document?.exists() == true) {
                    try {
                        callback(
                            applicationContext.contentResolver.openOutputStream(
                                document.uri,
                                "wt"
                            )
                        )
                    } catch (e: FileNotFoundException) {
                        showErrorToast(e)
                        callback(null)
                    }
                } else {
                    showFileCreateError(fileDirItem.path)
                    callback(null)
                }
            }
        }

        isAccessibleWithSAFSdk30(fileDirItem.path) -> {
            handleSAFDialogSdk30(fileDirItem.path) {
                if (!it) {
                    return@handleSAFDialogSdk30
                }

                callback.invoke(
                    try {
                        val uri = createDocumentUriUsingFirstParentTreeUri(fileDirItem.path)
                        if (!getDoesFilePathExist(fileDirItem.path)) {
                            createSAFFileSdk30(fileDirItem.path)
                        }
                        applicationContext.contentResolver.openOutputStream(uri, "wt")
                    } catch (e: Exception) {
                        null
                    } ?: createCasualFileOutputStream(this, targetFile)
                )
            }
        }

        isRestrictedWithSAFSdk30(fileDirItem.path) -> {
            callback.invoke(
                try {
                    val fileUri = getFileUrisFromFileDirItems(arrayListOf(fileDirItem))
                    applicationContext.contentResolver.openOutputStream(fileUri.first(), "wt")
                } catch (e: Exception) {
                    null
                } ?: createCasualFileOutputStream(this, targetFile)
            )
        }

        else -> {
            callback.invoke(createCasualFileOutputStream(this, targetFile))
        }
    }
}

fun Activity.hideKeyboard(view: View) {
    val inputMethodManager = getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
    inputMethodManager.hideSoftInputFromWindow(view.windowToken, 0)
}

fun Activity.hideKeyboardSync() {
    val inputMethodManager = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
    inputMethodManager.hideSoftInputFromWindow((currentFocus ?: View(this)).windowToken, 0)
    window!!.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN)
    currentFocus?.clearFocus()
}

fun Activity.hideKeyboard() {
    if (isOnMainThread()) {
        hideKeyboardSync()
    } else {
        Handler(Looper.getMainLooper()).post {
            hideKeyboardSync()
        }
    }
}

fun Activity.addTracksToPlaylist(tracks: List<Track>, callback: () -> Unit) {
    SelectPlaylistDialog(this) { playlistId ->
        val tracksToAdd = ArrayList<Track>()
        tracks.forEach {
            it.id = 0
            it.playListId = playlistId
            tracksToAdd.add(it)
        }

        ensureBackgroundThread {
            RoomHelper(this).insertTracksWithPlaylist(tracksToAdd)

            runOnUiThread {
                callback()
            }
        }
    }
}

fun Activity.maybeRescanTrackPaths(tracks: List<Track>, callback: (tracks: List<Track>) -> Unit) {
    val tracksWithoutId =
        tracks.filter { it.mediaStoreId == 0L || (it.flags and FLAG_MANUAL_CACHE != 0) }
    if (tracksWithoutId.isNotEmpty()) {
        val pathsToRescan = tracksWithoutId.map { it.path }
        rescanPaths(pathsToRescan) {
            for (track in tracks) {
                if (track.mediaStoreId == 0L || (track.flags and FLAG_MANUAL_CACHE != 0)) {
                    track.mediaStoreId = getMediaStoreIdFromPath(track.path)
                }
            }

            callback(tracks)
        }
    } else {
        callback(tracks)
    }
}

fun Activity.showTrackProperties(selectedTracks: List<Track>) {
    val selectedPaths = selectedTracks.map { track ->
        track.path.ifEmpty {
            ContentUris.withAppendedId(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                track.mediaStoreId
            ).toString()
        }
    }

    if (selectedPaths.size <= 1) {
        PropertiesDialog(this, selectedPaths.first(), false)
    } else {
        PropertiesDialog(this, selectedPaths, false)
    }
}

fun Activity.ensureActivityNotDestroyed(callback: () -> Unit) {
    if (!isFinishing && !isDestroyed) {
        callback()
    }
}

fun Activity.shareFiles(tracks: List<Track>) {
    val paths = tracks.map { it.path }
    sharePathsIntent(paths, BuildConfig.APPLICATION_ID)
}


fun Activity.launchPurchaseThankYouIntent() {
    hideKeyboard()
    try {
        launchViewIntent("market://details?id=com.simplemobiletools.thankyou")
    } catch (ignored: Exception) {
        launchViewIntent(getString(R.string.thank_you_url))
    }
}

fun Activity.launchUpgradeToProIntent() {
    hideKeyboard()
    try {
        launchViewIntent("market://details?id=${baseConfig.appId.removeSuffix(".debug")}.pro")
    } catch (ignored: Exception) {
        launchViewIntent(getStoreUrl())
    }
}

fun Activity.redirectToRateUs() {
    hideKeyboard()
    try {
        launchViewIntent("market://details?id=${packageName.removeSuffix(".debug")}")
    } catch (ignored: ActivityNotFoundException) {
        launchViewIntent(getStoreUrl())
    }
}

fun Activity.launchViewIntent(id: Int) = launchViewIntent(getString(id))
