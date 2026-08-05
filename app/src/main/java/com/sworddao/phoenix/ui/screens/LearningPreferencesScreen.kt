package com.sworddao.phoenix.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.sworddao.phoenix.R
import com.sworddao.phoenix.data.model.AccessibilityPreferences
import com.sworddao.phoenix.ui.components.BaoCharacter
import com.sworddao.phoenix.ui.components.BaoExpression

@Composable
fun LearningPreferencesScreen(
    onPreferencesSaved: (AccessibilityPreferences) -> Unit,
    modifier: Modifier = Modifier
) {
    var dadMode by remember { mutableStateOf(false) }
    var reducedMotion by remember { mutableStateOf(false) }
    var largeText by remember { mutableStateOf(false) }
    var highContrast by remember { mutableStateOf(false) }
    var slowAudio by remember { mutableStateOf(false) }
    var showHanzi by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        BaoCharacter(size = 100.dp, expression = BaoExpression.EXCITED)

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = stringResource(R.string.preferences_title),
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = stringResource(R.string.preferences_subtitle),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(32.dp))

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            SettingSwitch(
                title = stringResource(R.string.preferences_dad_mode),
                description = stringResource(R.string.preferences_dad_mode_description),
                checked = dadMode,
                onCheckedChange = { dadMode = it }
            )
            SettingSwitch(
                title = stringResource(R.string.preferences_reduced_motion),
                description = stringResource(R.string.preferences_reduced_motion_description),
                checked = reducedMotion,
                onCheckedChange = { reducedMotion = it }
            )
            SettingSwitch(
                title = stringResource(R.string.preferences_large_text),
                description = stringResource(R.string.preferences_large_text_description),
                checked = largeText,
                onCheckedChange = { largeText = it }
            )
            SettingSwitch(
                title = stringResource(R.string.preferences_high_contrast),
                description = stringResource(R.string.preferences_high_contrast_description),
                checked = highContrast,
                onCheckedChange = { highContrast = it }
            )
            SettingSwitch(
                title = stringResource(R.string.preferences_slow_audio),
                description = stringResource(R.string.preferences_slow_audio_description),
                checked = slowAudio,
                onCheckedChange = { slowAudio = it }
            )
            SettingSwitch(
                title = stringResource(R.string.preferences_show_hanzi),
                description = stringResource(R.string.preferences_show_hanzi_description),
                checked = showHanzi,
                onCheckedChange = { showHanzi = it }
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = {
                onPreferencesSaved(
                    AccessibilityPreferences(
                        dadMode = dadMode,
                        reducedMotion = reducedMotion,
                        largeText = largeText,
                        highContrast = highContrast,
                        slowAudio = slowAudio,
                        showHanzi = showHanzi
                    )
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary
            )
        ) {
            Text(
                text = stringResource(R.string.preferences_continue),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun SettingSwitch(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onBackground,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedTrackColor = MaterialTheme.colorScheme.primary
            )
        )
    }
}
