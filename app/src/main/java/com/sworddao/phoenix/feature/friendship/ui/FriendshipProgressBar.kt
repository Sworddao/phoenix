package com.sworddao.phoenix.feature.friendship.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sworddao.phoenix.R
import com.sworddao.phoenix.feature.friendship.data.FriendshipState
import com.sworddao.phoenix.feature.npc.data.FriendshipLevel

@Composable
fun FriendshipProgressBar(
    friendshipState: FriendshipState,
    modifier: Modifier = Modifier
) {
    val currentLevel = friendshipState.friendshipLevel
    val nextLevel = when (currentLevel) {
        FriendshipLevel.FAMILY -> null
        else -> FriendshipLevel.entries[currentLevel.ordinal + 1]
    }

    val xpInCurrentLevel = friendshipState.friendshipXp - currentLevel.xpThreshold
    val xpForNextLevel = nextLevel?.let { it.xpThreshold - currentLevel.xpThreshold } ?: 1
    val progress = if (xpForNextLevel > 0) {
        (xpInCurrentLevel.toFloat() / xpForNextLevel).coerceIn(0f, 1f)
    } else {
        1f
    }

    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = currentLevel.displayTitle,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = stringResource(
                    R.string.friendship_xp_progress_format,
                    friendshipState.friendshipXp,
                    nextLevel?.xpThreshold ?: currentLevel.xpThreshold
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp)),
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.primaryContainer,
        )

        if (nextLevel != null) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.friendship_next_level, nextLevel.displayTitle),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            )
        } else {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.friendship_max_level),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
