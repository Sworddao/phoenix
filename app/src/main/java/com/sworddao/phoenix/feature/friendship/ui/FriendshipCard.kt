package com.sworddao.phoenix.feature.friendship.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sworddao.phoenix.R
import com.sworddao.phoenix.feature.friendship.data.FriendshipState

@Composable
fun FriendshipCard(
    friendshipState: FriendshipState,
    npcDisplayName: String,
    npcEmoji: String,
    occupation: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val level = friendshipState.friendshipLevel
    val progress = com.sworddao.phoenix.feature.npc.data.FriendshipProgress(
        level = level,
        currentXp = friendshipState.friendshipXp,
        nextLevel = when (level) {
            com.sworddao.phoenix.feature.npc.data.FriendshipLevel.FAMILY -> null
            else -> com.sworddao.phoenix.feature.npc.data.FriendshipLevel.entries[level.ordinal + 1]
        },
        progressPercentage = calculateProgress(friendshipState)
    )

    Card(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .semantics {
                contentDescription = context.getString(
                    R.string.friendship_card_accessibility,
                    npcDisplayName,
                    level.displayTitle,
                    friendshipState.friendshipXp.toString()
                )
            },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = npcEmoji,
                    style = MaterialTheme.typography.headlineSmall
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = npcDisplayName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = occupation,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    RelationshipBadge(level = level)
                    Text(
                        text = stringResource(
                            R.string.friendship_xp_format,
                            friendshipState.friendshipXp
                        ),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }
            }

            Column(
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    text = stringResource(
                        R.string.friendship_conversations_format,
                        friendshipState.totalConversations
                    ),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
            }
        }
    }
}

private fun calculateProgress(state: FriendshipState): Float {
    val currentLevel = state.friendshipLevel
    val nextLevel = when (currentLevel) {
        com.sworddao.phoenix.feature.npc.data.FriendshipLevel.FAMILY -> null
        else -> com.sworddao.phoenix.feature.npc.data.FriendshipLevel.entries[currentLevel.ordinal + 1]
    }
    if (nextLevel == null) return 1f
    val xpInCurrentLevel = state.friendshipXp - currentLevel.xpThreshold
    val xpForNextLevel = nextLevel.xpThreshold - currentLevel.xpThreshold
    return if (xpForNextLevel > 0) {
        (xpInCurrentLevel.toFloat() / xpForNextLevel).coerceIn(0f, 1f)
    } else {
        1f
    }
}
