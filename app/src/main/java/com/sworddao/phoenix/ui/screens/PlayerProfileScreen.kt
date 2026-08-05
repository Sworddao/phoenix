package com.sworddao.phoenix.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.sworddao.phoenix.R
import com.sworddao.phoenix.data.model.ExperienceLevel
import com.sworddao.phoenix.data.model.LearningPace
import com.sworddao.phoenix.data.model.PlayerProfile
import com.sworddao.phoenix.ui.components.BaoCharacter
import com.sworddao.phoenix.ui.components.BaoExpression

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerProfileScreen(
    onProfileCreated: (PlayerProfile) -> Unit,
    modifier: Modifier = Modifier
) {
    var displayName by remember { mutableStateOf("") }
    var nativeLanguage by remember { mutableStateOf("") }
    var experienceLevel by remember { mutableStateOf(ExperienceLevel.BEGINNER) }
    var learningPace by remember { mutableStateOf(LearningPace.STANDARD) }
    var nameError by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        BaoCharacter(size = 120.dp, expression = BaoExpression.HAPPY)

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = stringResource(R.string.profile_title),
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = stringResource(R.string.profile_subtitle),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(32.dp))

        OutlinedTextField(
            value = displayName,
            onValueChange = {
                displayName = it
                nameError = false
            },
            label = { Text(stringResource(R.string.profile_name_label)) },
            placeholder = { Text(stringResource(R.string.profile_name_placeholder)) },
            isError = nameError,
            supportingText = if (nameError) {
                { Text(stringResource(R.string.profile_name_error)) }
            } else null,
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = nativeLanguage,
            onValueChange = { nativeLanguage = it },
            label = { Text(stringResource(R.string.profile_language_label)) },
            placeholder = { Text(stringResource(R.string.profile_language_placeholder)) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(24.dp))

        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = stringResource(R.string.profile_experience_title),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(12.dp))
            ExperienceLevel.entries.forEach { level ->
                val isSelected = experienceLevel == level
                val label = when (level) {
                    ExperienceLevel.BEGINNER -> stringResource(R.string.profile_experience_beginner)
                    ExperienceLevel.SOME_MANDARIN -> stringResource(R.string.profile_experience_some)
                    ExperienceLevel.INTERMEDIATE -> stringResource(R.string.profile_experience_intermediate)
                }
                Button(
                    onClick = { experienceLevel = level },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isSelected) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                ) {
                    Text(
                        text = label,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Start
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = stringResource(R.string.profile_pace_title),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(12.dp))
            LearningPace.entries.forEach { pace ->
                val isSelected = learningPace == pace
                val label = when (pace) {
                    LearningPace.RELAXED -> stringResource(R.string.profile_pace_relaxed)
                    LearningPace.STANDARD -> stringResource(R.string.profile_pace_standard)
                    LearningPace.INTENSIVE -> stringResource(R.string.profile_pace_intensive)
                }
                Button(
                    onClick = { learningPace = pace },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isSelected) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                ) {
                    Text(
                        text = label,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Start
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = {
                if (displayName.isBlank()) {
                    nameError = true
                } else {
                    onProfileCreated(
                        PlayerProfile(
                            displayName = displayName.trim(),
                            nativeLanguage = nativeLanguage.trim(),
                            experienceLevel = experienceLevel,
                            learningPace = learningPace
                        )
                    )
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary
            )
        ) {
            Text(
                text = stringResource(R.string.profile_continue),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}
