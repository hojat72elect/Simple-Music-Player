package ca.hojat.smart.musicplayer.shared.ui.compose.extensions

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import ca.hojat.smart.musicplayer.R
import ca.hojat.smart.musicplayer.shared.ui.compose.alert_dialog.rememberAlertDialogState
import ca.hojat.smart.musicplayer.shared.ui.dialogs.ConfirmationAlertDialog
import ca.hojat.smart.musicplayer.shared.extensions.launchViewIntent

@Composable
fun FakeVersionCheck() {
    val context = LocalContext.current
    val confirmationDialogAlertDialogState = rememberAlertDialogState().apply {
        DialogMember {
            ConfirmationAlertDialog(
                alertDialogState = this,
                message = FAKE_VERSION_APP_LABEL,
                positive = R.string.ok,
                negative = null
            ) {
                context.getActivity().launchViewIntent(DEVELOPER_PLAY_STORE_URL)
            }
        }
    }
    LaunchedEffect(Unit) {
        context.fakeVersionCheck(confirmationDialogAlertDialogState::show)
    }
}
