package ca.hojat.smart.musicplayer.shared.ui.compose.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.ripple.LocalRippleTheme
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxColors
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ca.hojat.smart.musicplayer.shared.ui.compose.extensions.MyDevices
import ca.hojat.smart.musicplayer.shared.ui.compose.extensions.NoRippleTheme
import ca.hojat.smart.musicplayer.shared.ui.compose.extensions.rememberMutableInteractionSource
import ca.hojat.smart.musicplayer.shared.ui.compose.theme.AppThemeSurface
import ca.hojat.smart.musicplayer.shared.ui.compose.theme.SimpleTheme
import ca.hojat.smart.musicplayer.shared.ui.compose.theme.preferenceLabelColor
import ca.hojat.smart.musicplayer.shared.ui.compose.theme.preferenceValueColor

@Composable
fun SettingsCheckBoxComponent(
    modifier: Modifier = Modifier,
    label: String,
    value: String? = null,
    initialValue: Boolean = false,
    isPreferenceEnabled: Boolean = true,
    onChange: ((Boolean) -> Unit)? = null,
    checkboxColors: CheckboxColors = CheckboxDefaults.colors(
        checkedColor = SimpleTheme.colorScheme.primary,
        checkmarkColor = SimpleTheme.colorScheme.surface,
    )
) {
    val interactionSource = rememberMutableInteractionSource()
    val indication = LocalIndication.current

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .clickable(
                onClick = { onChange?.invoke(!initialValue) },
                interactionSource = interactionSource,
                indication = indication
            )
            .padding(horizontal = 20.dp, vertical = 6.dp),
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(end = SimpleTheme.dimens.padding.extraLarge),
                text = label,
                color = preferenceLabelColor(isEnabled = isPreferenceEnabled),
                fontSize = 14.sp
            )
            AnimatedVisibility(visible = !value.isNullOrBlank()) {
                Text(
                    text = value.toString(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(end = SimpleTheme.dimens.padding.extraLarge),
                    color = preferenceValueColor(isEnabled = isPreferenceEnabled),
                )
            }
        }
        CompositionLocalProvider(LocalRippleTheme provides NoRippleTheme) {
            Checkbox(
                checked = initialValue,
                onCheckedChange = { onChange?.invoke(it) },
                enabled = isPreferenceEnabled,
                colors = checkboxColors,
                interactionSource = interactionSource
            )
        }
    }
}

@MyDevices
@Composable
private fun SettingsCheckBoxComponentPreview() {
    AppThemeSurface {
        SettingsCheckBoxComponent(
            label = "Some label",
            value = "Some value",
        )
    }
}
