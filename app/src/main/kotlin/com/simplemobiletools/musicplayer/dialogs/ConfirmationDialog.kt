package com.simplemobiletools.musicplayer.dialogs

import android.app.Activity
import androidx.appcompat.app.AlertDialog
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import com.simplemobiletools.musicplayer.R
import com.simplemobiletools.musicplayer.compose.alert_dialog.AlertDialogState
import com.simplemobiletools.musicplayer.compose.alert_dialog.dialogBorder
import com.simplemobiletools.musicplayer.compose.alert_dialog.dialogContainerColor
import com.simplemobiletools.musicplayer.compose.alert_dialog.dialogElevation
import com.simplemobiletools.musicplayer.compose.alert_dialog.dialogShape
import com.simplemobiletools.musicplayer.compose.alert_dialog.dialogTextColor
import com.simplemobiletools.musicplayer.compose.alert_dialog.rememberAlertDialogState
import com.simplemobiletools.musicplayer.compose.extensions.MyDevices
import com.simplemobiletools.musicplayer.compose.theme.AppThemeSurface
import com.simplemobiletools.musicplayer.databinding.DialogMessageBinding
import com.simplemobiletools.musicplayer.extensions.getAlertDialogBuilder
import com.simplemobiletools.musicplayer.extensions.setupDialogStuff

/**
 * A simple dialog without any view, just a messageId, a positive button and optionally a negative button
 *
 * @param activity has to be activity context to avoid some Theme.AppCompat issues
 * @param message the dialogs message, can be any String. If empty, messageId is used
 * @param messageId the dialogs messageId ID. Used only if message is empty
 * @param positive positive buttons text ID
 * @param negative negative buttons text ID (optional)
 * @param callback an anonymous function
 */
class ConfirmationDialog(
    activity: Activity,
    message: String = "",
    messageId: Int = R.string.proceed_with_deletion, positive: Int = R.string.yes,
    negative: Int = R.string.no,
    private val cancelOnTouchOutside: Boolean = true,
    dialogTitle: String = "",
    val callback: () -> Unit
) {
    private var dialog: AlertDialog? = null

    init {
        val view = DialogMessageBinding.inflate(activity.layoutInflater, null, false)
        view.message.text = message.ifEmpty { activity.resources.getString(messageId) }

        val builder = activity.getAlertDialogBuilder()
            .setPositiveButton(positive) { _, _ -> dialogConfirmed() }

        if (negative != 0) {
            builder.setNegativeButton(negative, null)
        }

        builder.apply {
            activity.setupDialogStuff(
                view.root,
                this,
                titleText = dialogTitle,
                cancelOnTouchOutside = cancelOnTouchOutside
            ) { alertDialog ->
                dialog = alertDialog
            }
        }
    }

    private fun dialogConfirmed() {
        dialog?.dismiss()
        callback()
    }
}

@Composable
fun ConfirmationAlertDialog(
    alertDialogState: AlertDialogState,
    modifier: Modifier = Modifier,
    message: String = "",
    messageId: Int? = R.string.proceed_with_deletion,
    positive: Int? = R.string.yes,
    negative: Int? = R.string.no,
    cancelOnTouchOutside: Boolean = true,
    dialogTitle: String = "",
    callback: () -> Unit
) {

    androidx.compose.material3.AlertDialog(
        containerColor = dialogContainerColor,
        modifier = modifier
            .dialogBorder,
        properties = DialogProperties(dismissOnClickOutside = cancelOnTouchOutside),
        onDismissRequest = {
            alertDialogState.hide()
            callback()
        },
        shape = dialogShape,
        tonalElevation = dialogElevation,
        dismissButton = {
            if (negative != null) {
                TextButton(onClick = {
                    alertDialogState.hide()
                    callback()
                }) {
                    Text(text = stringResource(id = negative))
                }
            }
        },
        confirmButton = {
            if (positive != null) {
                TextButton(onClick = {
                    alertDialogState.hide()
                    callback()
                }) {
                    Text(text = stringResource(id = positive))
                }
            }
        },
        title = {
            if (dialogTitle.isNotBlank() || dialogTitle.isNotEmpty()) {
                Text(
                    text = dialogTitle,
                    color = dialogTextColor,
                    fontSize = 21.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
        },
        text = {
            Text(
                modifier = Modifier.fillMaxWidth(),
                text = message.ifEmpty { messageId?.let { stringResource(id = it) }.orEmpty() },
                fontSize = 16.sp,
                color = dialogTextColor,
            )
        }
    )
}

@Composable
@MyDevices
private fun ConfirmationAlertDialogPreview() {
    AppThemeSurface {
        ConfirmationAlertDialog(
            alertDialogState = rememberAlertDialogState()
        ) {}
    }
}
