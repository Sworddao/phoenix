package com.sworddao.phoenix.feature.world.data

import com.sworddao.phoenix.feature.world.domain.WorldRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MockWorldRepository @Inject constructor() : WorldRepository {

    private val _regions = MutableStateFlow(createAllRegions())
    private val _progress = MutableStateFlow<Map<String, RegionProgress>>(emptyMap())
    private val _connections = MutableStateFlow(createConnections())
    private val _locations = MutableStateFlow(createLocations())
    private val _landmarks = MutableStateFlow(createLandmarks())
    private val _collectibles = MutableStateFlow(createCollectibles())

    override fun getAllRegions(): Flow<List<WorldRegion>> = _regions

    override fun getRegionById(regionId: String): Flow<WorldRegion?> =
        _regions.map { regions -> regions.find { it.id == regionId } }

    override fun getRegionConnections(regionId: String): Flow<List<RegionConnection>> =
        _connections.map { connections ->
            connections.filter { it.fromRegionId == regionId || it.toRegionId == regionId }
        }

    override fun getRegionProgress(regionId: String): Flow<RegionProgress?> =
        _progress.map { it[regionId] }

    override fun getExplorationProgress(): Flow<ExplorationProgress> = _regions.map { regions ->
        val progressMap = _progress.value
        val totalLocations = _locations.value.size
        val discoveredLocations = progressMap.values.sumOf { it.discoveredLocations.size }
        val totalCollectibles = _collectibles.value.size
        val collectedItems = progressMap.values.sumOf { it.collectedItems.size }

        ExplorationProgress(
            totalRegions = regions.size,
            completedRegions = regions.count { it.status == RegionStatus.COMPLETED },
            currentRegionId = regions.find { it.status == RegionStatus.CURRENT }?.id,
            totalLocations = totalLocations,
            discoveredLocations = discoveredLocations,
            totalCollectibles = totalCollectibles,
            collectedItems = collectedItems,
            completionPercentage = if (regions.isNotEmpty()) {
                regions.count { it.status == RegionStatus.COMPLETED }.toFloat() / regions.size
            } else 0f,
            regions = progressMap,
        )
    }

    override fun getCurrentRegion(): Flow<WorldRegion?> =
        _regions.map { regions -> regions.find { it.status == RegionStatus.CURRENT } }

    override fun getAvailableRegions(): Flow<List<WorldRegion>> =
        _regions.map { regions -> regions.filter { it.status == RegionStatus.AVAILABLE } }

    override fun getUnlockedRegions(): Flow<List<WorldRegion>> =
        _regions.map { regions -> regions.filter { it.isUnlocked } }

    override fun getLocationsByRegion(regionId: String): Flow<List<WorldLocation>> =
        _locations.map { locations -> locations.filter { it.regionId == regionId } }

    override fun getLandmarksByRegion(regionId: String): Flow<List<Landmark>> =
        _landmarks.map { landmarks -> landmarks.filter { it.regionId == regionId } }

    override fun getCollectiblesByRegion(regionId: String): Flow<List<CollectibleLocation>> =
        _collectibles.map { collectibles -> collectibles.filter { it.regionId == regionId } }

    override suspend fun travelToRegion(regionId: String): WorldResult {
        val regions = _regions.value
        val targetRegion = regions.find { it.id == regionId }
            ?: return WorldResult.Error("Region not found")

        if (targetRegion.status == RegionStatus.LOCKED) {
            return WorldResult.Error("Region is locked")
        }

        val currentRegion = regions.find { it.status == RegionStatus.CURRENT }

        _regions.update { regionList ->
            regionList.map { region ->
                when {
                    region.id == regionId -> region.copy(status = RegionStatus.CURRENT)
                    region.status == RegionStatus.CURRENT -> region.copy(status = RegionStatus.VISITED)
                    else -> region
                }
            }
        }

        val progress = _progress.value[regionId] ?: RegionProgress(
            regionId = regionId,
            status = RegionStatus.CURRENT,
        )
        _progress.update { it + (regionId to progress.copy(
            status = RegionStatus.CURRENT,
            lastVisitedAt = System.currentTimeMillis(),
            firstVisitedAt = progress.firstVisitedAt ?: System.currentTimeMillis(),
        )) }

        return WorldResult.TravelStarted(currentRegion?.id ?: "", regionId)
    }

    override suspend fun discoverLocation(locationId: String): WorldResult {
        val locations = _locations.value
        val location = locations.find { it.id == locationId }
            ?: return WorldResult.Error("Location not found")

        _locations.update { locationList ->
            locationList.map { loc ->
                if (loc.id == locationId) loc.copy(isDiscovered = true) else loc
            }
        }

        val regionId = location.regionId
        val progress = _progress.value[regionId] ?: RegionProgress(
            regionId = regionId,
            status = RegionStatus.CURRENT,
        )
        _progress.update { it + (regionId to progress.copy(
            discoveredLocations = progress.discoveredLocations + locationId,
        )) }

        return WorldResult.LocationDiscovered(locationId)
    }

    override suspend fun collectItem(collectibleId: String): WorldResult {
        val collectibles = _collectibles.value
        val collectible = collectibles.find { it.id == collectibleId }
            ?: return WorldResult.Error("Collectible not found")

        if (collectible.isCollected) {
            return WorldResult.Error("Already collected")
        }

        _collectibles.update { collectibleList ->
            collectibleList.map { c ->
                if (c.id == collectibleId) c.copy(isCollected = true) else c
            }
        }

        val regionId = collectible.regionId
        val progress = _progress.value[regionId] ?: RegionProgress(
            regionId = regionId,
            status = RegionStatus.CURRENT,
        )
        _progress.update { it + (regionId to progress.copy(
            collectedItems = progress.collectedItems + collectibleId,
        )) }

        return WorldResult.CollectibleFound(collectibleId)
    }

    override suspend fun checkRegionUnlocks(): List<String> {
        val regions = _regions.value
        val newlyUnlocked = mutableListOf<String>()

        regions.forEach { region ->
            if (region.status == RegionStatus.LOCKED) {
                val requirements = region.unlockRequirements
                val allMet = checkRequirements(requirements, regions)
                if (allMet) {
                    newlyUnlocked.add(region.id)
                }
            }
        }

        if (newlyUnlocked.isNotEmpty()) {
            _regions.update { regionList ->
                regionList.map { region ->
                    if (newlyUnlocked.contains(region.id)) {
                        region.copy(status = RegionStatus.AVAILABLE)
                    } else region
                }
            }
        }

        return newlyUnlocked
    }

    override suspend fun completeRegion(regionId: String): WorldResult {
        val regions = _regions.value
        val region = regions.find { it.id == regionId }
            ?: return WorldResult.Error("Region not found")

        _regions.update { regionList ->
            regionList.map { r ->
                if (r.id == regionId) r.copy(
                    status = RegionStatus.COMPLETED,
                    completionPercentage = 100f,
                ) else r
            }
        }

        val progress = _progress.value[regionId] ?: RegionProgress(
            regionId = regionId,
            status = RegionStatus.COMPLETED,
        )
        _progress.update { it + (regionId to progress.copy(
            status = RegionStatus.COMPLETED,
            completionPercentage = 100f,
        )) }

        // Check for newly unlocked regions
        checkRegionUnlocks()

        return WorldResult.RegionUnlocked(regionId)
    }

    override suspend fun unlockFastTravel(regionId: String): WorldResult {
        val progress = _progress.value[regionId] ?: RegionProgress(
            regionId = regionId,
            status = RegionStatus.VISITED,
        )
        _progress.update { it + (regionId to progress.copy(unlockedFastTravel = true)) }

        return WorldResult.Success("Fast travel unlocked for $regionId")
    }

    override suspend fun getRegionNpcs(regionId: String): Flow<List<String>> =
        _regions.map { regions ->
            regions.find { it.id == regionId }?.npcIds ?: emptyList()
        }

    override suspend fun getRegionQuests(regionId: String): Flow<List<String>> =
        _regions.map { regions ->
            regions.find { it.id == regionId }?.questIds ?: emptyList()
        }

    private fun checkRequirements(
        requirements: UnlockRequirement,
        regions: List<WorldRegion>,
    ): Boolean {
        if (requirements.questIds.isNotEmpty()) {
            val completedQuests = _progress.value.values.flatMap { it.completedQuests }
            if (!requirements.questIds.all { it in completedQuests }) return false
        }

        if (requirements.requiredRegions.isNotEmpty()) {
            val completedRegions = regions.filter { it.status == RegionStatus.COMPLETED }.map { it.id }
            if (!requirements.requiredRegions.all { it in completedRegions }) return false
        }

        return true
    }

    private fun createAllRegions(): List<WorldRegion> = listOf(
        WorldRegion(
            id = "qingyuan_village",
            name = "Qingyuan Village",
            nameCn = "清远村",
            description = "A peaceful village where your adventure begins. Meet friendly locals and learn the basics of Mandarin.",
            status = RegionStatus.CURRENT,
            order = 1,
            chapter = 1,
            connections = listOf("jade_forest"),
            travelMethods = listOf(TravelMethod.WALKING),
            npcIds = listOf("npc_mei", "npc_li", "npc_cha_guan", "npc_wei"),
            questIds = listOf("quest_help_grandma_mei", "quest_buy_dumplings", "quest_order_tea", "quest_meet_wei"),
            mapPositionX = 0.15f,
            mapPositionY = 0.45f,
            color = 0xFF4CAF50,
            icon = "🏡",
        ),
        WorldRegion(
            id = "jade_forest",
            name = "Jade Forest",
            nameCn = "翡翠森林",
            description = "A lush forest filled with ancient trees and hidden paths. Discover nature vocabulary and meet forest dwellers.",
            status = RegionStatus.AVAILABLE,
            order = 2,
            chapter = 1,
            connections = listOf("qingyuan_village", "riverside_town"),
            travelMethods = listOf(TravelMethod.WALKING),
            unlockRequirements = UnlockRequirement(questIds = listOf("quest_help_grandma_mei")),
            npcIds = listOf("npc_forest_guide"),
            questIds = listOf("quest_forest_exploration"),
            mapPositionX = 0.25f,
            mapPositionY = 0.35f,
            color = 0xFF2E7D32,
            icon = "🌲",
        ),
        WorldRegion(
            id = "riverside_town",
            name = "Riverside Town",
            nameCn = "河畔镇",
            description = "A charming town along the river. Practice transportation vocabulary and explore local markets.",
            status = RegionStatus.LOCKED,
            order = 3,
            chapter = 1,
            connections = listOf("jade_forest", "night_market"),
            travelMethods = listOf(TravelMethod.WALKING, TravelMethod.BUS),
            unlockRequirements = UnlockRequirement(questIds = listOf("quest_forest_exploration")),
            npcIds = listOf("npc_boat_driver", "npc_fisherman"),
            questIds = listOf("quest_river_crossing"),
            mapPositionX = 0.35f,
            mapPositionY = 0.40f,
            color = 0xFF1565C0,
            icon = "🌊",
        ),
        WorldRegion(
            id = "night_market",
            name = "Night Market",
            nameCn = "夜市",
            description = "A vibrant night market filled with food stalls and vendors. Learn food vocabulary and bargaining phrases.",
            status = RegionStatus.LOCKED,
            order = 4,
            chapter = 2,
            connections = listOf("riverside_town", "mountain_temple"),
            travelMethods = listOf(TravelMethod.WALKING, TravelMethod.BUS),
            unlockRequirements = UnlockRequirement(questIds = listOf("quest_river_crossing")),
            npcIds = listOf("npc_street_vendor", "npc_food_master"),
            questIds = listOf("quest_night_market_food"),
            mapPositionX = 0.45f,
            mapPositionY = 0.35f,
            color = 0xFFFF6F00,
            icon = "🏮",
        ),
        WorldRegion(
            id = "mountain_temple",
            name = "Mountain Temple",
            nameCn = "山中寺庙",
            description = "An ancient temple in the mountains. Learn about Chinese culture, traditions, and spiritual vocabulary.",
            status = RegionStatus.LOCKED,
            order = 5,
            chapter = 2,
            connections = listOf("night_market", "high_speed_rail"),
            travelMethods = listOf(TravelMethod.WALKING),
            unlockRequirements = UnlockRequirement(questIds = listOf("quest_night_market_food")),
            npcIds = listOf("npc_monk", "npc_temple_keeper"),
            questIds = listOf("quest_temple_visit"),
            mapPositionX = 0.55f,
            mapPositionY = 0.30f,
            color = 0xFF8D6E63,
            icon = "⛩",
        ),
        WorldRegion(
            id = "high_speed_rail",
            name = "High-Speed Railway",
            nameCn = "高铁站",
            description = "Modern high-speed rail station connecting regions. Master transportation vocabulary and travel etiquette.",
            status = RegionStatus.LOCKED,
            order = 6,
            chapter = 2,
            connections = listOf("mountain_temple", "historic_city"),
            travelMethods = listOf(TravelMethod.HIGH_SPEED_RAIL, TravelMethod.TRAIN),
            unlockRequirements = UnlockRequirement(questIds = listOf("quest_temple_visit")),
            npcIds = listOf("npc_ticket_agent", "npc_train_conductor"),
            questIds = listOf("quest_train_journey"),
            mapPositionX = 0.65f,
            mapPositionY = 0.40f,
            color = 0xFF78909C,
            icon = "🚄",
        ),
        WorldRegion(
            id = "historic_city",
            name = "Historic City",
            nameCn = "古城",
            description = "An ancient city with rich history. Explore cultural landmarks and learn historical vocabulary.",
            status = RegionStatus.LOCKED,
            order = 7,
            chapter = 3,
            connections = listOf("high_speed_rail", "business_district"),
            travelMethods = listOf(TravelMethod.HIGH_SPEED_RAIL, TravelMethod.BUS, TravelMethod.TAXI),
            unlockRequirements = UnlockRequirement(questIds = listOf("quest_train_journey")),
            npcIds = listOf("npc_history_professor", "npc_artisan"),
            questIds = listOf("quest_city_tour"),
            mapPositionX = 0.72f,
            mapPositionY = 0.45f,
            color = 0xFFAFB42B,
            icon = "🏯",
        ),
        WorldRegion(
            id = "business_district",
            name = "Business District",
            nameCn = "商业区",
            description = "A modern business hub. Practice professional vocabulary and business etiquette.",
            status = RegionStatus.LOCKED,
            order = 8,
            chapter = 3,
            connections = listOf("historic_city", "shanghai"),
            travelMethods = listOf(TravelMethod.TAXI, TravelMethod.HIGH_SPEED_RAIL),
            unlockRequirements = UnlockRequirement(questIds = listOf("quest_city_tour")),
            npcIds = listOf("npc_businessman", "npc_office_worker"),
            questIds = listOf("quest_business_meeting"),
            mapPositionX = 0.78f,
            mapPositionY = 0.50f,
            color = 0xFF546E7A,
            icon = "🏢",
        ),
        WorldRegion(
            id = "shanghai",
            name = "Shanghai",
            nameCn = "上海",
            description = "The vibrant metropolis of Shanghai. Experience modern China and advanced conversations.",
            status = RegionStatus.LOCKED,
            order = 9,
            chapter = 4,
            connections = listOf("business_district", "beijing"),
            travelMethods = listOf(TravelMethod.HIGH_SPEED_RAIL, TravelMethod.TAXI),
            unlockRequirements = UnlockRequirement(questIds = listOf("quest_business_meeting")),
            npcIds = listOf("npc_shanghai_guide", "npc_modern_artist"),
            questIds = listOf("quest_shanghai_exploration"),
            mapPositionX = 0.82f,
            mapPositionY = 0.55f,
            color = 0xFFE91E63,
            icon = "🌃",
        ),
        WorldRegion(
            id = "beijing",
            name = "Beijing",
            nameCn = "北京",
            description = "The capital city of China. Master complex conversations and cultural nuances.",
            status = RegionStatus.LOCKED,
            order = 10,
            chapter = 4,
            connections = listOf("shanghai", "great_wall"),
            travelMethods = listOf(TravelMethod.HIGH_SPEED_RAIL, TravelMethod.TAXI),
            unlockRequirements = UnlockRequirement(questIds = listOf("quest_shanghai_exploration")),
            npcIds = listOf("npc_beijing_native", "npc_peking_expert"),
            questIds = listOf("quest_beijing_adventure"),
            mapPositionX = 0.75f,
            mapPositionY = 0.25f,
            color = 0xFFD32F2F,
            icon = "🏛",
        ),
        WorldRegion(
            id = "great_wall",
            name = "Great Wall",
            nameCn = "长城",
            description = "The iconic Great Wall of China. Challenge yourself with advanced vocabulary and cultural understanding.",
            status = RegionStatus.LOCKED,
            order = 11,
            chapter = 5,
            connections = listOf("beijing", "phoenix_summit"),
            travelMethods = listOf(TravelMethod.BUS, TravelMethod.TAXI),
            unlockRequirements = UnlockRequirement(questIds = listOf("quest_beijing_adventure")),
            npcIds = listOf("npc_wall_keeper", "npc_mountain_guide"),
            questIds = listOf("quest_great_wall_climb"),
            mapPositionX = 0.70f,
            mapPositionY = 0.18f,
            color = 0xFF795548,
            icon = "🏔",
        ),
        WorldRegion(
            id = "phoenix_summit",
            name = "Phoenix Summit",
            nameCn = "凤凰山顶",
            description = "The final destination. Prove your Mandarin mastery in an extended conversation at the summit.",
            status = RegionStatus.LOCKED,
            order = 12,
            chapter = 5,
            connections = listOf("great_wall"),
            travelMethods = listOf(TravelMethod.WALKING),
            unlockRequirements = UnlockRequirement(questIds = listOf("quest_great_wall_climb")),
            npcIds = listOf("npc_summit_sage"),
            questIds = listOf("quest_final_conversation"),
            mapPositionX = 0.60f,
            mapPositionY = 0.10f,
            color = 0xFFFFD700,
            icon = "⛰",
        ),
    )

    private fun createConnections(): List<RegionConnection> = listOf(
        RegionConnection("qingyuan_village", "jade_forest", TravelMethod.WALKING, 30),
        RegionConnection("jade_forest", "riverside_town", TravelMethod.WALKING, 45),
        RegionConnection("riverside_town", "night_market", TravelMethod.BUS, 20),
        RegionConnection("night_market", "mountain_temple", TravelMethod.WALKING, 60),
        RegionConnection("mountain_temple", "high_speed_rail", TravelMethod.WALKING, 40),
        RegionConnection("high_speed_rail", "historic_city", TravelMethod.HIGH_SPEED_RAIL, 90),
        RegionConnection("historic_city", "business_district", TravelMethod.TAXI, 25),
        RegionConnection("business_district", "shanghai", TravelMethod.HIGH_SPEED_RAIL, 120),
        RegionConnection("shanghai", "beijing", TravelMethod.HIGH_SPEED_RAIL, 180),
        RegionConnection("beijing", "great_wall", TravelMethod.BUS, 60),
        RegionConnection("great_wall", "phoenix_summit", TravelMethod.WALKING, 90),
    )

    private fun createLocations(): List<WorldLocation> = listOf(
        // Qingyuan Village
        WorldLocation("loc_mei_house", "Grandma Mei's House", "梅奶奶家", "A warm, cozy home filled with the smell of fresh bread.", "qingyuan_village", LandmarkType.VILLAGE, 0.12f, 0.48f, listOf("npc_mei"), listOf("quest_help_grandma_mei"), true),
        WorldLocation("loc_dumpling_shop", "Dumpling Shop", "饺子店", "Uncle Li's famous dumpling restaurant.", "qingyuan_village", LandmarkType.RESTAURANT, 0.52f, 0.46f, listOf("npc_li"), listOf("quest_buy_dumplings"), true),
        WorldLocation("loc_tea_house", "Tea House", "茶馆", "A peaceful place for tea and conversation.", "qingyuan_village", LandmarkType.SHOP, 0.33f, 0.38f, listOf("npc_cha_guan"), listOf("quest_order_tea"), true),
        WorldLocation("loc_village_square", "Village Square", "村广场", "The heart of the village where neighbors gather.", "qingyuan_village", LandmarkType.PARK, 0.40f, 0.60f, emptyList(), emptyList(), true),

        // Jade Forest
        WorldLocation("loc_forest_entrance", "Forest Entrance", "森林入口", "The path leading into the ancient jade forest.", "jade_forest", LandmarkType.PARK, 0.22f, 0.38f, listOf("npc_forest_guide"), listOf("quest_forest_exploration"), false),
        WorldLocation("loc_ancient_tree", "Ancient Tree", "古树", "A massive tree said to be over 500 years old.", "jade_forest", LandmarkType.GARDEN, 0.28f, 0.32f, emptyList(), emptyList(), false),

        // Riverside Town
        WorldLocation("loc_river_bridge", "River Bridge", "河桥", "A stone bridge crossing the gentle river.", "riverside_town", LandmarkType.BRIDGE, 0.33f, 0.42f, emptyList(), emptyList(), false),
        WorldLocation("loc_fish_market", "Fish Market", "鱼市", "Fresh fish from the river, sold daily.", "riverside_town", LandmarkType.MARKET, 0.37f, 0.38f, listOf("npc_fisherman"), emptyList(), false),

        // Night Market
        WorldLocation("loc_food_street", "Food Street", "美食街", "A lively street filled with food stalls.", "night_market", LandmarkType.MARKET, 0.43f, 0.37f, listOf("npc_street_vendor"), listOf("quest_night_market_food"), false),
        WorldLocation("loc_lantern_row", "Lantern Row", "灯笼巷", "A beautiful alley lit by colorful lanterns.", "night_market", LandmarkType.SHOP, 0.47f, 0.33f, emptyList(), emptyList(), false),

        // Mountain Temple
        WorldLocation("loc_temple_entrance", "Temple Entrance", "寺庙入口", "The grand entrance to the mountain temple.", "mountain_temple", LandmarkType.TEMPLE, 0.53f, 0.32f, listOf("npc_temple_keeper"), listOf("quest_temple_visit"), false),
        WorldLocation("loc_meditation_garden", "Meditation Garden", "冥想花园", "A serene garden for quiet reflection.", "mountain_temple", LandmarkType.GARDEN, 0.57f, 0.28f, listOf("npc_monk"), emptyList(), false),

        // High-Speed Railway
        WorldLocation("loc_train_platform", "Train Platform", "站台", "Where modern trains arrive and depart.", "high_speed_rail", LandmarkType.STATION, 0.63f, 0.42f, listOf("npc_ticket_agent"), listOf("quest_train_journey"), false),
        WorldLocation("loc_ticket_office", "Ticket Office", "售票处", "Purchase tickets for your journey.", "high_speed_rail", LandmarkType.STATION, 0.67f, 0.38f, listOf("npc_train_conductor"), emptyList(), false),

        // Historic City
        WorldLocation("loc_city_gate", "City Gate", "城门", "The ancient gate to the historic city.", "historic_city", LandmarkType.CITY_GATE, 0.70f, 0.47f, emptyList(), listOf("quest_city_tour"), false),
        WorldLocation("loc_museum", "City Museum", "城市博物馆", "Discover the city's rich history.", "historic_city", LandmarkType.MUSEUM, 0.74f, 0.43f, listOf("npc_history_professor"), emptyList(), false),

        // Business District
        WorldLocation("loc_office_tower", "Office Tower", "办公大楼", "A modern skyscraper in the business district.", "business_district", LandmarkType.TOWER, 0.76f, 0.52f, listOf("npc_businessman"), listOf("quest_business_meeting"), false),
        WorldLocation("loc_cafe", "Business Cafe", "商务咖啡厅", "A popular spot for business meetings.", "business_district", LandmarkType.RESTAURANT, 0.80f, 0.48f, listOf("npc_office_worker"), emptyList(), false),

        // Shanghai
        WorldLocation("loc_bund", "The Bund", "外滩", "Shanghai's iconic waterfront promenade.", "shanghai", LandmarkType.RIVER, 0.80f, 0.57f, listOf("npc_shanghai_guide"), listOf("quest_shanghai_exploration"), false),
        WorldLocation("loc_pearl_tower", "Pearl Tower", "东方明珠", "The iconic Oriental Pearl Tower.", "shanghai", LandmarkType.TOWER, 0.84f, 0.53f, emptyList(), emptyList(), false),

        // Beijing
        WorldLocation("loc_forbidden_city", "Forbidden City", "故宫", "The imperial palace of ancient China.", "beijing", LandmarkType.PALACE, 0.73f, 0.27f, listOf("npc_beijing_native"), listOf("quest_beijing_adventure"), false),
        WorldLocation("loc_tiananmen", "Tiananmen Square", "天安门广场", "The largest public square in the world.", "beijing", LandmarkType.PARK, 0.77f, 0.23f, listOf("npc_peking_expert"), emptyList(), false),

        // Great Wall
        WorldLocation("loc_wall_watchtower", "Watchtower", "烽火台", "An ancient watchtower on the Great Wall.", "great_wall", LandmarkType.WALL, 0.68f, 0.20f, listOf("npc_wall_keeper"), listOf("quest_great_wall_climb"), false),
        WorldLocation("loc_wall_peak", "Wall Peak", "长城峰", "The highest point accessible on the wall.", "great_wall", LandmarkType.MOUNTAIN, 0.72f, 0.16f, listOf("npc_mountain_guide"), emptyList(), false),

        // Phoenix Summit
        WorldLocation("loc_summit_peak", "Summit Peak", "山顶", "The final destination of your journey.", "phoenix_summit", LandmarkType.MOUNTAIN, 0.58f, 0.12f, listOf("npc_summit_sage"), listOf("quest_final_conversation"), false),
    )

    private fun createLandmarks(): List<Landmark> = listOf(
        Landmark("lmnt_mei_house", "Grandma Mei's House", "梅奶奶家", LandmarkType.VILLAGE, "A warm, cozy home.", "qingyuan_village", 0.12f, 0.48f, true, true, listOf("npc_mei")),
        Landmark("lmnt_dumpling_shop", "Dumpling Shop", "饺子店", LandmarkType.RESTAURANT, "Uncle Li's restaurant.", "qingyuan_village", 0.52f, 0.46f, true, true, listOf("npc_li")),
        Landmark("lmnt_tea_house", "Tea House", "茶馆", LandmarkType.SHOP, "A peaceful tea house.", "qingyuan_village", 0.33f, 0.38f, true, true, listOf("npc_cha_guan")),
    )

    private fun createCollectibles(): List<CollectibleLocation> = listOf(
        CollectibleLocation("col_tea_set", "Ancient Tea Set", CollectibleType.TEA, "qingyuan_village", "loc_tea_house", 0.34f, 0.39f, false, false, "A beautiful porcelain tea set.", "Tea culture has been part of China for over 4,000 years."),
        CollectibleLocation("col_bamboo_frame", "Bamboo Picture Frame", CollectibleType.BAMBOO, "jade_forest", "loc_ancient_tree", 0.29f, 0.31f, false, false, "A handcrafted bamboo frame.", "Bamboo symbolizes strength and flexibility in Chinese culture."),
        CollectibleLocation("col_paper_lantern", "Paper Lantern", CollectibleType.LANTERN, "night_market", "loc_lantern_row", 0.48f, 0.34f, false, false, "A colorful paper lantern.", "Lanterns represent hope and good fortune."),
    )
}
