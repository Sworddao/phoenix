package com.sworddao.phoenix.feature.passport.data

import kotlinx.serialization.Serializable

@Serializable
enum class CollectibleCategory(val displayName: String, val icon: String) {
    TEA("茶艺", "🍵"),
    BAMBOO("竹艺", "🎋"),
    LANTERN("灯笼", "🏮"),
    SOUVENIR("纪念品", "🎁"),
    FESTIVAL_TICKET("节日门票", "🎫"),
    PHOTOGRAPH("照片", "📷"),
    INSTRUMENT("乐器", "🎵"),
    RECIPE_CARD("食谱", "📜"),
    STORY_SCROLL("故事卷轴", "📜"),
    VOCABULARY_CARD("词汇卡", "📝"),
    VOICE_RECORDING("录音", "🎤"),
    BOOK("书籍", "📚"),
    POSTCARD("明信片", "💌"),
    COIN("古币", "🪙"),
    STAMP("印章", "📮"),
    SCROLL("卷轴", "📜"),
    PAINTING("画作", "🎨"),
    CERAMIC("陶瓷", "🏺"),
    TEXTILE("纺织品", "🧶"),
    JADE("玉石", "💚"),
}

@Serializable
enum class CollectibleRarity(val displayName: String, val dropChance: Float) {
    COMMON("普通", 0.5f),
    UNCOMMON("稀有", 0.3f),
    RARE("珍贵", 0.15f),
    EPIC("史诗", 0.04f),
    LEGENDARY("传说", 0.01f),
}

@Serializable
enum class CollectibleSource(val displayName: String) {
    QUEST("任务奖励"),
    NPC("NPC赠送"),
    EXPLORATION("探索发现"),
    DIALOGUE("对话奖励"),
    SHOP("商店购买"),
    FESTIVAL("节日活动"),
    DAILY("每日奖励"),
    ACHIEVEMENT("成就奖励"),
    HIDDEN("隐藏发现"),
}

@Serializable
enum class StampRarity(val displayName: String, val color: Long) {
    BRONZE("铜章", 0xFFCD7F32),
    SILVER("银章", 0xFFC0C0C0),
    GOLD("金章", 0xFFFFD700),
    PLATINUM("白金章", 0xFFE5E4E2),
    DIAMOND("钻石章", 0xFFB9F2FF),
}

@Serializable
data class Passport(
    val id: String = "player_passport",
    val playerName: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val lastUpdated: Long = System.currentTimeMillis(),
    val totalStamps: Int = 0,
    val totalCollectibles: Int = 0,
    val totalDiscoveries: Int = 0,
    val currentChapter: Int = 1,
    val regions: Map<String, PassportRegion> = emptyMap(),
    val collectibles: Map<String, Collectible> = emptyMap(),
    val timeline: List<DiscoveryEvent> = emptyList(),
) {
    val completionPercentage: Float
        get() = if (regions.isEmpty()) 0f
        else regions.values.count { it.isCompleted }.toFloat() / regions.size

    val collectedCount: Int
        get() = collectibles.values.count { it.isCollected }

    val totalCount: Int
        get() = collectibles.size

    val stampCount: Int
        get() = regions.values.count { it.stampEarned }
}

@Serializable
data class PassportRegion(
    val regionId: String,
    val regionName: String,
    val regionNameCn: String,
    val isDiscovered: Boolean = false,
    val isCompleted: Boolean = false,
    val stampEarned: Boolean = false,
    val stampRarity: StampRarity = StampRarity.BRONZE,
    val discoveredAt: Long? = null,
    val completedAt: Long? = null,
    val completionPercentage: Float = 0f,
    val vocabularyLearned: Int = 0,
    val friendshipsMade: Int = 0,
    val questsCompleted: Int = 0,
    val collectiblesFound: Int = 0,
    val collectiblesTotal: Int = 0,
    val totalPlayTimeMinutes: Int = 0,
    val notes: String = "",
) {
    val collectibleProgress: Float
        get() = if (collectiblesTotal > 0) collectiblesFound.toFloat() / collectiblesTotal else 0f

    val isFullyExplored: Boolean
        get() = collectiblesFound >= collectiblesTotal && isCompleted
}

@Serializable
data class PassportEntry(
    val id: String,
    val regionId: String,
    val type: EntryType,
    val title: String,
    val description: String,
    val timestamp: Long = System.currentTimeMillis(),
    val metadata: Map<String, String> = emptyMap(),
)

@Serializable
enum class EntryType {
    STAMP_EARNED,
    COLLECTIBLE_FOUND,
    QUEST_COMPLETED,
    NPC_MET,
    FRIENDSHIP_LEVEL_UP,
    DIALOGUE_COMPLETED,
    REGION_DISCOVERED,
    REGION_COMPLETED,
    VOCABULARY_LEARNED,
    SPEAKING_PRACTICE,
    ACHIEVEMENT_UNLOCKED,
}

@Serializable
data class Collectible(
    val id: String,
    val name: String,
    val nameCn: String,
    val category: CollectibleCategory,
    val rarity: CollectibleRarity = CollectibleRarity.COMMON,
    val source: CollectibleSource = CollectibleSource.EXPLORATION,
    val description: String,
    val culturalNote: String? = null,
    val regionId: String,
    val isCollected: Boolean = false,
    val collectedAt: Long? = null,
    val isHidden: Boolean = false,
    val isDisplayed: Boolean = false,
    val displayLocation: String? = null,
    val tradeable: Boolean = true,
    val xpValue: Int = 10,
) {
    val rarityColor: Long
        get() = when (rarity) {
            CollectibleRarity.COMMON -> 0xFF9E9E9E
            CollectibleRarity.UNCOMMON -> 0xFF4CAF50
            CollectibleRarity.RARE -> 0xFF2196F3
            CollectibleRarity.EPIC -> 0xFF9C27B0
            CollectibleRarity.LEGENDARY -> 0xFFFF9800
        }
}

@Serializable
data class CollectionProgress(
    val totalCollectibles: Int = 0,
    val collectedCount: Int = 0,
    val categoryProgress: Map<CollectibleCategory, Int> = emptyMap(),
    val rarityProgress: Map<CollectibleRarity, Int> = emptyMap(),
    val regionProgress: Map<String, Int> = emptyMap(),
    val completionPercentage: Float = 0f,
    val missingCollectibles: List<String> = emptyList(),
    val recentCollectibles: List<String> = emptyList(),
) {
    val categoriesComplete: Int
        get() = categoryProgress.size

    val totalCategories: Int
        get() = CollectibleCategory.entries.size

    val rarestCollected: CollectibleRarity?
        get() = rarityProgress.keys.minByOrNull { it.dropChance }
}

@Serializable
data class DiscoveryEvent(
    val id: String,
    val type: EntryType,
    val title: String,
    val description: String,
    val regionId: String?,
    val timestamp: Long = System.currentTimeMillis(),
    val metadata: Map<String, String> = emptyMap(),
)

@Serializable
data class AchievementProgress(
    val id: String,
    val name: String,
    val nameCn: String,
    val description: String,
    val icon: String,
    val isUnlocked: Boolean = false,
    val unlockedAt: Long? = null,
    val progress: Float = 0f,
    val requiredCount: Int = 1,
    val currentCount: Int = 0,
    val category: String = "",
    val xpReward: Int = 100,
) {
    val isComplete: Boolean
        get() = currentCount >= requiredCount
}

sealed class PassportResult {
    data class Success(val message: String) : PassportResult()
    data class Error(val message: String) : PassportResult()
    data class StampEarned(val regionId: String, val rarity: StampRarity) : PassportResult()
    data class CollectibleFound(val collectibleId: String) : PassportResult()
    data class AchievementUnlocked(val achievementId: String) : PassportResult()
    data class RegionCompleted(val regionId: String) : PassportResult()
}
