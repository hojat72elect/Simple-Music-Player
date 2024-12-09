package ca.hojat.smart.musicplayer.shared.ui.dialogs

import android.app.Activity
import android.text.Html
import android.text.method.LinkMovementMethod
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import ca.hojat.smart.musicplayer.R
import ca.hojat.smart.musicplayer.shared.ui.compose.alert_dialog.AlertDialogState
import ca.hojat.smart.musicplayer.shared.ui.compose.alert_dialog.dialogBorder
import ca.hojat.smart.musicplayer.shared.ui.compose.alert_dialog.dialogContainerColor
import ca.hojat.smart.musicplayer.shared.ui.compose.alert_dialog.dialogElevation
import ca.hojat.smart.musicplayer.shared.ui.compose.alert_dialog.dialogShape
import ca.hojat.smart.musicplayer.shared.ui.compose.alert_dialog.rememberAlertDialogState
import ca.hojat.smart.musicplayer.shared.ui.compose.components.LinkifyTextComponent
import ca.hojat.smart.musicplayer.shared.ui.compose.extensions.MyDevices
import ca.hojat.smart.musicplayer.shared.ui.compose.extensions.composeDonateIntent
import ca.hojat.smart.musicplayer.shared.ui.compose.extensions.config
import ca.hojat.smart.musicplayer.shared.ui.compose.theme.AppThemeSurface
import ca.hojat.smart.musicplayer.databinding.DialogPurchaseThankYouBinding
import ca.hojat.smart.musicplayer.shared.extensions.baseConfig
import ca.hojat.smart.musicplayer.shared.extensions.getAlertDialogBuilder
import ca.hojat.smart.musicplayer.shared.extensions.launchPurchaseThankYouIntent
import ca.hojat.smart.musicplayer.shared.extensions.removeUnderlines
import ca.hojat.smart.musicplayer.shared.extensions.setupDialogStuff
import ca.hojat.smart.musicplayer.shared.extensions.fromHtml

class PurchaseThankYouDialog(val activity: Activity) {
    init {
        val view =
            DialogPurchaseThankYouBinding.inflate(activity.layoutInflater, null, false).apply {
                var text = activity.getString(R.string.purchase_thank_you)
                if (activity.baseConfig.appId.removeSuffix(".debug").endsWith(".pro")) {
                    text += "<br><br>${activity.getString(R.string.shared_theme_note)}"
                }

                purchaseThankYou.text = Html.fromHtml(text)
                purchaseThankYou.movementMethod = LinkMovementMethod.getInstance()
                purchaseThankYou.removeUnderlines()
            }

        activity.getAlertDialogBuilder()
            .setPositiveButton(R.string.purchase) { _, _ -> activity.launchPurchaseThankYouIntent() }
            .setNegativeButton(R.string.later, null)
            .apply {
                activity.setupDialogStuff(view.root, this, cancelOnTouchOutside = false)
            }
    }
}

@Composable
fun PurchaseThankYouAlertDialog(
    alertDialogState: AlertDialogState,
    modifier: Modifier = Modifier,
) {
    val localContext = LocalContext.current
    val donateIntent = composeDonateIntent()
    val appId = remember {
        localContext.config.appId
    }
    androidx.compose.material3.AlertDialog(
        containerColor = dialogContainerColor,
        modifier = modifier
            .dialogBorder,
        properties = DialogProperties(dismissOnClickOutside = false, dismissOnBackPress = false),
        onDismissRequest = {},
        shape = dialogShape,
        tonalElevation = dialogElevation,
        dismissButton = {
            TextButton(onClick = {
                alertDialogState.hide()
            }) {
                Text(text = stringResource(id = R.string.later))
            }
        },
        confirmButton = {
            TextButton(onClick = {
                donateIntent()
                alertDialogState.hide()
            }) {
                Text(text = stringResource(id = R.string.purchase))
            }
        },
        text = {
            var text = stringResource(R.string.purchase_thank_you)
            if (appId.removeSuffix(".debug").endsWith(".pro")) {
                text += "<br><br>${stringResource(R.string.shared_theme_note)}"
            }
            LinkifyTextComponent(
                fontSize = 16.sp,
                removeUnderlines = false,
                modifier = Modifier.fillMaxWidth()
            ) {
                text.fromHtml()
            }
        }
    )
}

@Composable
@MyDevices
private fun PurchaseThankYouAlertDialogPreview() {
    AppThemeSurface {
        PurchaseThankYouAlertDialog(alertDialogState = rememberAlertDialogState())
    }
}
