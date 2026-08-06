package com.sworddao.phoenix.feature.dialogue.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sworddao.phoenix.R
import com.sworddao.phoenix.feature.dialogue.data.DialogueHistoryEntry
import com.sworddao.phoenix.feature.dialogue.data.Speaker

@Composable
fun DialogueBubble(
    entry: DialogueHistoryEntry,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isNpc = entry.speaker == Speaker.NPC
    val isPlayer = entry.speaker == Speaker.PLAYER

    val bubbleColor = when {
        isNpc -> MaterialTheme.colorScheme.primaryContainer
        isPlayer -> MaterialTheme.colorScheme.secondaryContainer
        else -> MaterialTheme.colorScheme.tertiaryContainer
    }

    val textColor = when {
        isNpc -> MaterialTheme.colorScheme.onPrimaryContainer
        isPlayer -> MaterialTheme.colorScheme.onSecondaryContainer
        else -> MaterialTheme.colorScheme.onTertiaryContainer
    }

    val contentDescription = context.getString(
        R.string.dialogue_bubble_accessibility,
        entry.speakerName,
        entry.text
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .semantics { this.contentDescription = contentDescription },
        contentAlignment = if (isPlayer) Alignment.CenterEnd else Alignment.CenterStart
    ) {
        Column(
            modifier = Modifier.width(300.dp)
        ) {
            if (entry.speakerName.isNotEmpty()) {
                Text(
                    text = entry.speakerName,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp)
                )
            }

            Box(
                modifier = Modifier
                    .width(300.dp)
                    .clip(
                        RoundedCornerShape(
                            topStart = if (isPlayer) 16.dp else 4.dp,
                            topEnd = if (isPlayer) 4.dp else 16.dp,
                            bottomStart = 16.dp,
                            bottomEnd = 16.dp
                        )
                    )
                    .background(bubbleColor)
                    .padding(12.dp)
            ) {
                Column {
                    Text(
                        text = entry.text,
                        style = MaterialTheme.typography.bodyLarge,
                        color = textColor,
                        lineHeight = 24.sp
                    )

                    if (entry.pinyin.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = entry.pinyin,
                            style = MaterialTheme.typography.bodySmall,
                            color = textColor.copy(alpha = 0.7f),
                            fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                        )
                    }
                }
            }
        }
    }
}
