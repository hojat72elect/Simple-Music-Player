package ca.hojat.smart.musicplayer.shared.ui.compose.settings

import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import ca.hojat.smart.musicplayer.shared.ui.compose.extensions.MyDevices
import ca.hojat.smart.musicplayer.shared.ui.compose.theme.AppThemeSurface
import ca.hojat.smart.musicplayer.shared.ui.compose.theme.divider_grey

@Composable
fun SettingsHorizontalDivider(
    modifier: Modifier = Modifier,
    color: Color = divider_grey,
    thickness: Dp = DividerDefaults.Thickness,
) {
    HorizontalDivider(modifier = modifier, color = color, thickness = thickness)
}


@Composable
@MyDevices
private fun SettingsHorizontalDividerPreview() {
    AppThemeSurface {
        SettingsHorizontalDivider()
    }
}
