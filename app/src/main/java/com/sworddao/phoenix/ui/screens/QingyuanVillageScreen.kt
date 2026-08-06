package com.sworddao.phoenix.ui.screens

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.sworddao.phoenix.R
import com.sworddao.phoenix.feature.friendship.viewmodel.FriendshipViewModel
import com.sworddao.phoenix.feature.gameplay.viewmodel.GameProgressViewModel
import com.sworddao.phoenix.feature.npc.data.Npc
import com.sworddao.phoenix.feature.npc.ui.NpcMarker
import com.sworddao.phoenix.feature.npc.viewmodel.NpcViewModel
import com.sworddao.phoenix.ui.components.BaoCharacter
import com.sworddao.phoenix.ui.components.BaoExpression
import com.sworddao.phoenix.ui.screens.village.BaoMessageDialog
import com.sworddao.phoenix.ui.screens.village.LocationButton
import com.sworddao.phoenix.ui.screens.village.LocationDialog
import com.sworddao.phoenix.ui.screens.village.VillageLocation
import com.sworddao.phoenix.ui.screens.village.drawVillageScene

@Composable
fun QingyuanVillageScreen(
    playerName: String,
    onNavigateToDialogue: (String) -> Unit = {},
    onNavigateToNpcProfile: (String) -> Unit = {},
    onNavigateToQuestList: () -> Unit = {},
    onNavigateToWorldMap: () -> Unit = {},
    onNavigateToProgression: () -> Unit = {},
    modifier: Modifier = Modifier,
    npcViewModel: NpcViewModel = hiltViewModel(),
    friendshipViewModel: FriendshipViewModel = hiltViewModel(),
    gameProgressViewModel: GameProgressViewModel = hiltViewModel()
) {
    var selectedLocation by remember { mutableStateOf<VillageLocation?>(null) }
    var showBaoMessage by remember { mutableStateOf(false) }
    var baoMessage by remember { mutableStateOf("") }

    val context = LocalContext.current
    val npcUiState by npcViewModel.uiState.collectAsState()
    val gameProgressUiState by gameProgressViewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        if (!gameProgressUiState.gameProgress.hasCompletedFirstDialogue) {
            baoMessage = "欢迎来到清远村！我是宝，你的学习伙伴。让我们先去和梅奶奶打个招呼吧！点击她的头像开始对话。"
            showBaoMessage = true
        }
    }

    val baoMessages = remember {
        listOf(
            context.getString(R.string.bao_message_1),
            context.getString(R.string.bao_message_2),
            context.getString(R.string.bao_message_3),
            context.getString(R.string.bao_message_4),
            context.getString(R.string.bao_message_5),
            context.getString(R.string.bao_message_6),
            context.getString(R.string.bao_message_7),
            context.getString(R.string.bao_message_8),
            context.getString(R.string.bao_message_9),
            context.getString(R.string.bao_message_10, playerName)
        )
    }

    val infiniteTransition = rememberInfiniteTransition(label = "village_animation")

    val floatOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 8f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 3000,
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "float"
    )

    val lanternSway by infiniteTransition.animateFloat(
        initialValue = -3f,
        targetValue = 3f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 2000,
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "lantern_sway"
    )

    val waterRipple by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 4000,
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "water_ripple"
    )

    val cloudOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 20f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 8000,
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "cloud_offset"
    )

    val treeSway by infiniteTransition.animateFloat(
        initialValue = -2f,
        targetValue = 2f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 2500,
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "tree_sway"
    )

    val locations = remember {
        listOf(
            VillageLocation(
                name = context.getString(R.string.location_bakery_name),
                description = context.getString(R.string.location_bakery_description),
                emoji = "\uD83E\uDD5F",
                positionX = 0.12f,
                positionY = 0.48f,
                widthDp = 90,
                heightDp = 60
            ),
            VillageLocation(
                name = context.getString(R.string.location_restaurant_name),
                description = context.getString(R.string.location_restaurant_description),
                emoji = "\uD83C\uDF5C",
                positionX = 0.52f,
                positionY = 0.46f,
                widthDp = 100,
                heightDp = 65
            ),
            VillageLocation(
                name = context.getString(R.string.location_teahouse_name),
                description = context.getString(R.string.location_teahouse_description),
                emoji = "\uD83C\uDF75",
                positionX = 0.33f,
                positionY = 0.38f,
                widthDp = 85,
                heightDp = 55
            ),
            VillageLocation(
                name = context.getString(R.string.location_square_name),
                description = context.getString(R.string.location_square_description),
                emoji = "\uD83C\uDFD8\uFE0F",
                positionX = 0.4f,
                positionY = 0.6f,
                widthDp = 80,
                heightDp = 50
            ),
            VillageLocation(
                name = context.getString(R.string.location_exit_name),
                description = context.getString(R.string.location_exit_description),
                emoji = "\uD83D\uDEB6",
                positionX = 0.82f,
                positionY = 0.55f,
                widthDp = 60,
                heightDp = 40
            )
        )
    }

    val npcMarkerPositions = remember {
        mapOf(
            "grandma_mei" to Pair(0.08f, 0.42f),
            "restaurant_owner_lin" to Pair(0.48f, 0.40f),
            "taxi_driver_chen" to Pair(0.36f, 0.56f),
            "university_student_wei" to Pair(0.29f, 0.33f)
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .semantics {
                contentDescription = context.getString(R.string.village_accessibility)
            },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.primary)
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = stringResource(R.string.village_title),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onPrimary,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(500.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                drawVillageScene(
                    floatOffset = floatOffset,
                    lanternSway = lanternSway,
                    waterRipple = waterRipple,
                    cloudOffset = cloudOffset,
                    treeSway = treeSway
                )
            }

            locations.forEach { location ->
                LocationButton(
                    location = location,
                    onClick = { selectedLocation = location }
                )
            }

            npcUiState.npcs.forEach { npc ->
                val position = npcMarkerPositions[npc.id]
                if (position != null) {
                    val density = androidx.compose.ui.platform.LocalDensity.current
                    val offsetX = with(density) { (position.first * 1f).toDp() }
                    val offsetY = with(density) { (position.second * 500f).toDp() }

                    NpcMarker(
                        npc = npc,
                        onClick = {
                            onNavigateToNpcProfile(npc.id)
                        },
                        modifier = Modifier.offset { IntOffset(offsetX.toPx().toInt(), offsetY.toPx().toInt()) }
                    )
                }
            }

            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(24.dp)
                    .clickable {
                        baoMessage = baoMessages.random()
                        showBaoMessage = true
                    }
                    .semantics {
                        contentDescription = context.getString(R.string.bao_companion_accessibility)
                    }
            ) {
                BaoCharacter(
                    size = 80.dp,
                    expression = BaoExpression.HAPPY
                )
            }

            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = stringResource(R.string.village_welcome),
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onBackground,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    letterSpacing = 1.sp,
                    modifier = Modifier
                        .shadow(4.dp, RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.background.copy(alpha = 0.95f))
                        .padding(horizontal = 20.dp, vertical = 10.dp)
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = stringResource(R.string.village_instruction),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.background.copy(alpha = 0.9f))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = stringResource(R.string.village_explore_prompt, playerName),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(onClick = onNavigateToQuestList) {
                        Text(text = stringResource(R.string.village_button_quests))
                    }
                    OutlinedButton(onClick = onNavigateToWorldMap) {
                        Text(text = stringResource(R.string.village_button_world))
                    }
                    Button(onClick = onNavigateToProgression) {
                        Text(text = stringResource(R.string.village_button_progression))
                    }
                }
            }
        }
    }

    selectedLocation?.let { location ->
        LocationDialog(
            location = location,
            onDismiss = { selectedLocation = null }
        )
    }

    if (showBaoMessage) {
        BaoMessageDialog(
            message = baoMessage,
            onDismiss = { showBaoMessage = false }
        )
    }
}
