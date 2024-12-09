package ca.hojat.smart.musicplayer.shared.ui.dialogs

import android.app.Activity
import androidx.appcompat.app.AlertDialog
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import ca.hojat.smart.musicplayer.R
import ca.hojat.smart.musicplayer.shared.ui.compose.alert_dialog.AlertDialogState
import ca.hojat.smart.musicplayer.shared.ui.compose.alert_dialog.dialogBorder
import ca.hojat.smart.musicplayer.shared.ui.compose.alert_dialog.dialogContainerColor
import ca.hojat.smart.musicplayer.shared.ui.compose.alert_dialog.dialogElevation
import ca.hojat.smart.musicplayer.shared.ui.compose.alert_dialog.dialogShape
import ca.hojat.smart.musicplayer.shared.ui.compose.alert_dialog.dialogTextColor
import ca.hojat.smart.musicplayer.shared.ui.compose.alert_dialog.rememberAlertDialogState
import ca.hojat.smart.musicplayer.shared.ui.compose.extensions.MyDevices
import ca.hojat.smart.musicplayer.shared.ui.compose.extensions.andThen
import ca.hojat.smart.musicplayer.shared.ui.compose.theme.AppThemeSurface
import ca.hojat.smart.musicplayer.shared.ui.compose.theme.SimpleTheme
import ca.hojat.smart.musicplayer.databinding.DialogUpgradeToProBinding
import ca.hojat.smart.musicplayer.shared.extensions.getAlertDialogBuilder
import ca.hojat.smart.musicplayer.shared.extensions.launchUpgradeToProIntent
import ca.hojat.smart.musicplayer.shared.extensions.launchViewIntent
import ca.hojat.smart.musicplayer.shared.extensions.setupDialogStuff

class UpgradeToProDialog(val activity: Activity) {

    init {
        val view = DialogUpgradeToProBinding.inflate(activity.layoutInflater, null, false).apply {
            upgradeToPro.text = activity.getString(R.string.upgrade_to_pro_long)
        }

        activity.getAlertDialogBuilder()
            .setPositiveButton(R.string.upgrade) { _, _ -> upgradeApp() }
            .setNeutralButton(
                R.string.more_info,
                null
            )     // do not dismiss the dialog on pressing More Info
            .setNegativeButton(R.string.later, null)
            .apply {
                activity.setupDialogStuff(
                    view.root,
                    this,
                    R.string.upgrade_to_pro,
                    cancelOnTouchOutside = false
                ) { alertDialog ->
                    alertDialog.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener {
                        moreInfo()
                    }
                }
            }
    }

    private fun upgradeApp() {
        activity.launchUpgradeToProIntent()
    }

    private fun moreInfo() {
        activity.launchViewIntent("https://simplemobiletools.com/upgrade_to_pro")
    }
}

@Composable
fun UpgradeToProAlertDialog(
    alertDialogState: AlertDialogState,
    modifier: Modifier = Modifier,
    onMoreInfoClick: () -> Unit,
    onUpgradeClick: () -> Unit
) {
    AlertDialog(
        onDismissRequest = {},
        text = {
            Text(
                text = stringResource(id = R.string.upgrade_to_pro_long),
                color = dialogTextColor,
                fontSize = 16.sp,
            )
        },
        title = {
            Text(
                text = stringResource(id = R.string.upgrade_to_pro),
                color = dialogTextColor,
                fontSize = 21.sp,
                fontWeight = FontWeight.Bold,
            )
        },
        tonalElevation = dialogElevation,
        shape = dialogShape,
        containerColor = dialogContainerColor,
        confirmButton = {
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(
                    onClick = onMoreInfoClick, // do not dismiss the dialog on pressing More Info
                    modifier = Modifier.weight(0.2f)
                ) {
                    Text(text = stringResource(id = R.string.more_info))
                }
                TextButton(
                    onClick = alertDialogState::hide,
                    modifier = Modifier.padding(horizontal = SimpleTheme.dimens.padding.medium)
                ) {
                    Text(text = stringResource(id = R.string.later))
                }
                TextButton(
                    onClick = onUpgradeClick andThen alertDialogState::hide,
                ) {
                    Text(text = stringResource(id = R.string.upgrade))
                }
            }
        },
        modifier = modifier.dialogBorder,
        properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false)
    )
}

@MyDevices
@Composable
private fun UpgradeToProAlertDialogPreview() {
    AppThemeSurface {
        UpgradeToProAlertDialog(
            alertDialogState = rememberAlertDialogState(),
            onMoreInfoClick = {},
            onUpgradeClick = {},
        )
    }
}
