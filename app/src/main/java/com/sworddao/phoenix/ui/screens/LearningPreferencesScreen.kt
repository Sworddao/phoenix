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
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
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

        // Bao character
        BaoCharacter(
            size = 100.dp,
            expression = BaoExpression.EXCITED
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Title
        Text(
            text = "Accessibility Settings",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Customize your learning experience",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Settings list
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Dad Mode
            SettingSwitch(
                title = "Dad Mode",
                description = "Reduced pressure, larger UI, gentle encouragement",
                checked = dadMode,
                onCheckedChange = { dadMode = it }
            )

            // Reduced Motion
            SettingSwitch(
                title = "Reduced Motion",
                description = "Minimize animations throughout the app",
                checked = reducedMotion,
                onCheckedChange = { reducedMotion = it }
            )

            // Large Text
            SettingSwitch(
                title = "Large Text",
                description = "Increase text size for better readability",
                checked = largeText,
                onCheckedChange = { largeText = it }
            )

            // High Contrast
            SettingSwitch(
                title = "High Contrast",
                description = "Enhanced color contrast for visibility",
                checked = highContrast,
                onCheckedChange = { highContrast = it }
            )

            // Slow Audio
            SettingSwitch(
                title = "Slow Audio",
                description = "Play audio at a slower pace",
                checked = slowAudio,
                onCheckedChange = { slowAudio = it }
            )

            // Show Hanzi
            SettingSwitch(
                title = "Show Hanzi",
                description = "Display Chinese characters alongside Pinyin",
                checked = showHanzi,
                onCheckedChange = { showHanzi = it }
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Continue Button
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
                text = "Continue",
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
        Column(
            modifier = Modifier.weight(1f)
        ) {
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
