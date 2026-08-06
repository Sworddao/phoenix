package com.sworddao.phoenix.feature.friendship.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sworddao.phoenix.feature.npc.data.FriendshipLevel

@Composable
fun RelationshipBadge(
    level: FriendshipLevel,
    modifier: Modifier = Modifier
) {
    val (backgroundColor, textColor) = when (level) {
        FriendshipLevel.STRANGER -> Pair(
            MaterialTheme.colorScheme.surfaceVariant,
            MaterialTheme.colorScheme.onSurfaceVariant
        )
        FriendshipLevel.VISITOR -> Pair(
            MaterialTheme.colorScheme.secondaryContainer,
            MaterialTheme.colorScheme.onSecondaryContainer
        )
        FriendshipLevel.FRIEND -> Pair(
            MaterialTheme.colorScheme.primaryContainer,
            MaterialTheme.colorScheme.onPrimaryContainer
        )
        FriendshipLevel.CLOSE_FRIEND -> Pair(
            Color(0xFFE8F5E9),
            Color(0xFF2E7D32)
        )
        FriendshipLevel.TRUSTED_FRIEND -> Pair(
            Color(0xFFFFF3E0),
            Color(0xFFE65100)
        )
        FriendshipLevel.FAMILY -> Pair(
            Color(0xFFFCE4EC),
            Color(0xFFC62828)
        )
    }

    Text(
        text = level.displayTitle,
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.Bold,
        color = textColor,
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(backgroundColor)
            .padding(horizontal = 8.dp, vertical = 2.dp)
    )
}
