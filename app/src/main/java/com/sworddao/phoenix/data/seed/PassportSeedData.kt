package com.sworddao.phoenix.data.seed


import com.sworddao.phoenix.feature.passport.data.AchievementProgress
import com.sworddao.phoenix.feature.passport.data.Collectible
import com.sworddao.phoenix.feature.passport.data.CollectibleCategory
import com.sworddao.phoenix.feature.passport.data.CollectibleRarity
import com.sworddao.phoenix.feature.passport.data.CollectibleSource
import com.sworddao.phoenix.feature.passport.data.DiscoveryEvent
import com.sworddao.phoenix.feature.passport.data.EntryType
import com.sworddao.phoenix.feature.passport.data.Passport
import com.sworddao.phoenix.feature.passport.data.PassportRegion
import com.sworddao.phoenix.feature.passport.data.StampRarity

object PassportSeedData {

fun createInitialPassport() = Passport(
        id = "player_passport",
        playerName = "Traveler",
        createdAt = System.currentTimeMillis(),
    )

fun createInitialRegions() = listOf(
        PassportRegion(
            regionId = "qingyuan_village",
            regionName = "Qingyuan Village",
            regionNameCn = "清远村",
            isDiscovered = true,
            discoveredAt = System.currentTimeMillis() - 86400000 * 7,
            completionPercentage = 0.75f,
            vocabularyLearned = 25,
            friendshipsMade = 3,
            questsCompleted = 4,
            collectiblesFound = 2,
            collectiblesTotal = 5,
        ),
        PassportRegion(
            regionId = "jade_forest",
            regionName = "Jade Forest",
            regionNameCn = "翡翠森林",
            isDiscovered = true,
            discoveredAt = System.currentTimeMillis() - 86400000 * 5,
            completionPercentage = 0.40f,
            vocabularyLearned = 15,
            friendshipsMade = 1,
            questsCompleted = 1,
            collectiblesFound = 1,
            collectiblesTotal = 3,
        ),
        PassportRegion(
            regionId = "riverside_town",
            regionName = "Riverside Town",
            regionNameCn = "河畔镇",
            isDiscovered = false,
            collectiblesTotal = 4,
        ),
        PassportRegion(
            regionId = "night_market",
            regionName = "Night Market",
            regionNameCn = "夜市",
            isDiscovered = false,
            collectiblesTotal = 6,
        ),
        PassportRegion(
            regionId = "mountain_temple",
            regionName = "Mountain Temple",
            regionNameCn = "山中寺庙",
            isDiscovered = false,
            collectiblesTotal = 4,
        ),
        PassportRegion(
            regionId = "high_speed_rail",
            regionName = "High-Speed Railway",
            regionNameCn = "高铁站",
            isDiscovered = false,
            collectiblesTotal = 3,
        ),
        PassportRegion(
            regionId = "historic_city",
            regionName = "Historic City",
            regionNameCn = "古城",
            isDiscovered = false,
            collectiblesTotal = 5,
        ),
        PassportRegion(
            regionId = "business_district",
            regionName = "Business District",
            regionNameCn = "商业区",
            isDiscovered = false,
            collectiblesTotal = 4,
        ),
        PassportRegion(
            regionId = "shanghai",
            regionName = "Shanghai",
            regionNameCn = "上海",
            isDiscovered = false,
            collectiblesTotal = 6,
        ),
        PassportRegion(
            regionId = "beijing",
            regionName = "Beijing",
            regionNameCn = "北京",
            isDiscovered = false,
            collectiblesTotal = 5,
        ),
        PassportRegion(
            regionId = "great_wall",
            regionName = "Great Wall",
            regionNameCn = "长城",
            isDiscovered = false,
            collectiblesTotal = 4,
        ),
        PassportRegion(
            regionId = "phoenix_summit",
            regionName = "Phoenix Summit",
            regionNameCn = "凤凰山顶",
            isDiscovered = false,
            collectiblesTotal = 3,
        ),
    )

fun createInitialCollectibles() = listOf(
        // Qingyuan Village
        Collectible("col_tea_set", "Ancient Tea Set", "古老茶具", CollectibleCategory.TEA, CollectibleRarity.RARE, CollectibleSource.EXPLORATION, "A beautiful porcelain tea set from the village tea house.", "Tea culture has been part of China for over 4,000 years.", "qingyuan_village", true, System.currentTimeMillis() - 86400000 * 6),
        Collectible("col_grandma_recipe", "Grandma Mei's Recipe", "梅奶奶的食谱", CollectibleCategory.RECIPE_CARD, CollectibleRarity.UNCOMMON, CollectibleSource.NPC, "A handwritten recipe for traditional dumplings.", null, "qingyuan_village", true, System.currentTimeMillis() - 86400000 * 5),
        Collectible("col_bamboo_frame", "Bamboo Picture Frame", "竹制相框", CollectibleCategory.BAMBOO, CollectibleRarity.COMMON, CollectibleSource.EXPLORATION, "A handcrafted bamboo frame.", "Bamboo symbolizes strength and flexibility in Chinese culture.", "qingyuan_village", false),
        Collectible("col_village_postcard", "Village Postcard", "村庄明信片", CollectibleCategory.POSTCARD, CollectibleRarity.COMMON, CollectibleSource.QUEST, "A hand-painted postcard of Qingyuan Village.", null, "qingyuan_village", false),
        Collectible("col_lucky_coin", "Lucky Coin", "幸运硬币", CollectibleCategory.COIN, CollectibleRarity.UNCOMMON, CollectibleSource.HIDDEN, "An ancient coin found near the village square.", "Coins have been used in China for over 3,000 years.", "qingyuan_village", false, null, false),

        // Jade Forest
        Collectible("col_jade_pendant", "Jade Pendant", "玉佩", CollectibleCategory.JADE, CollectibleRarity.RARE, CollectibleSource.EXPLORATION, "A small jade pendant carved into a leaf shape.", "Jade has been treasured in China for over 7,000 years.", "jade_forest", true, System.currentTimeMillis() - 86400000 * 4),
        Collectible("col_bamboo_flute", "Bamboo Flute", "竹笛", CollectibleCategory.INSTRUMENT, CollectibleRarity.UNCOMMON, CollectibleSource.NPC, "A traditional bamboo flute from the forest.", null, "jade_forest", false),
        Collectible("col_forest_photo", "Forest Photograph", "森林照片", CollectibleCategory.PHOTOGRAPH, CollectibleRarity.COMMON, CollectibleSource.EXPLORATION, "A beautiful photo of the ancient forest.", null, "jade_forest", false),

        // Riverside Town
        Collectible("col_river_scroll", "River Story Scroll", "河畔故事卷轴", CollectibleCategory.STORY_SCROLL, CollectibleRarity.RARE, CollectibleSource.QUEST, "A scroll telling the legend of the river.", null, "riverside_town", false),
        Collectible("col_fishing_hat", "Fisherman's Hat", "渔夫帽", CollectibleCategory.SOUVENIR, CollectibleRarity.COMMON, CollectibleSource.NPC, "A traditional woven hat.", null, "riverside_town", false),
        Collectible("col_boat_model", "Model Boat", "小船模型", CollectibleCategory.SOUVENIR, CollectibleRarity.UNCOMMON, CollectibleSource.EXPLORATION, "A handcrafted wooden boat model.", null, "riverside_town", false),
        Collectible("col_river_postcard", "River Postcard", "河畔明信片", CollectibleCategory.POSTCARD, CollectibleRarity.COMMON, CollectibleSource.QUEST, "A scenic postcard of the riverside.", null, "riverside_town", false),

        // Night Market
        Collectible("col_paper_lantern", "Paper Lantern", "纸灯笼", CollectibleCategory.LANTERN, CollectibleRarity.UNCOMMON, CollectibleSource.EXPLORATION, "A colorful paper lantern.", "Lanterns represent hope and good fortune.", "night_market", false),
        Collectible("col_street_food_book", "Street Food Guide", "街头美食指南", CollectibleCategory.BOOK, CollectibleRarity.COMMON, CollectibleSource.SHOP, "A guide to local street food.", null, "night_market", false),
        Collectible("col_festival_ticket", "Festival Ticket", "节日门票", CollectibleCategory.FESTIVAL_TICKET, CollectibleRarity.RARE, CollectibleSource.FESTIVAL, "A ticket to the night market festival.", null, "night_market", false),
        Collectible("col_night_photo", "Night Market Photo", "夜市照片", CollectibleCategory.PHOTOGRAPH, CollectibleRarity.COMMON, CollectibleSource.EXPLORATION, "A vibrant photo of the night market.", null, "night_market", false),
        Collectible("col_spice_jar", "Spice Jar", "香料罐", CollectibleCategory.CERAMIC, CollectibleRarity.UNCOMMON, CollectibleSource.SHOP, "A ceramic jar filled with local spices.", null, "night_market", false),
        Collectible("col_night_postcard", "Night Market Postcard", "夜市明信片", CollectibleCategory.POSTCARD, CollectibleRarity.COMMON, CollectibleSource.QUEST, "A colorful postcard of the night market.", null, "night_market", false),

        // Mountain Temple
        Collectible("col_incense_burner", "Incense Burner", "香炉", CollectibleCategory.CERAMIC, CollectibleRarity.RARE, CollectibleSource.EXPLORATION, "A small bronze incense burner.", "Incense has been used in Chinese temples for centuries.", "mountain_temple", false),
        Collectible("col_prayer_beads", "Prayer Beads", "念珠", CollectibleCategory.SOUVENIR, CollectibleRarity.UNCOMMON, CollectibleSource.NPC, "A string of wooden prayer beads.", null, "mountain_temple", false),
        Collectible("col_temple_scroll", "Temple Scripture", "佛经卷轴", CollectibleCategory.SCROLL, CollectibleRarity.EPIC, CollectibleSource.EXPLORATION, "An ancient scroll with temple teachings.", null, "mountain_temple", false),
        Collectible("col_meditation_guide", "Meditation Guide", "冥想指南", CollectibleCategory.BOOK, CollectibleRarity.COMMON, CollectibleSource.NPC, "A guide to meditation practices.", null, "mountain_temple", false),

        // High-Speed Railway
        Collectible("col_train_ticket", "First Train Ticket", "首张车票", CollectibleCategory.STAMP, CollectibleRarity.UNCOMMON, CollectibleSource.QUEST, "Your first high-speed rail ticket.", null, "high_speed_rail", false),
        Collectible("col_railway_map", "Railway Map", "铁路地图", CollectibleCategory.POSTCARD, CollectibleRarity.COMMON, CollectibleSource.EXPLORATION, "A map of the railway network.", null, "high_speed_rail", false),
        Collectible("col_travel_journal", "Travel Journal", "旅行日记", CollectibleCategory.BOOK, CollectibleRarity.UNCOMMON, CollectibleSource.SHOP, "A journal for recording your travels.", null, "high_speed_rail", false),

        // Historic City
        Collectible("col_ancient_coin", "Ancient Coin", "古币", CollectibleCategory.COIN, CollectibleRarity.RARE, CollectibleSource.EXPLORATION, "A coin from the ancient city.", "This coin dates back to the Ming Dynasty.", "historic_city", false),
        Collectible("col_city_painting", "City Painting", "城市画作", CollectibleCategory.PAINTING, CollectibleRarity.UNCOMMON, CollectibleSource.NPC, "A watercolor painting of the historic city.", null, "historic_city", false),
        Collectible("col_silk_scarf", "Silk Scarf", "丝巾", CollectibleCategory.TEXTILE, CollectibleRarity.RARE, CollectibleSource.SHOP, "A beautiful silk scarf.", "Silk has been produced in China for over 5,000 years.", "historic_city", false),
        Collectible("col_historic_stamp", "Historic Stamp", "历史印章", CollectibleCategory.STAMP, CollectibleRarity.UNCOMMON, CollectibleSource.EXPLORATION, "A stamp from the historic city gate.", null, "historic_city", false),
        Collectible("col_city_postcard", "City Postcard", "古城明信片", CollectibleCategory.POSTCARD, CollectibleRarity.COMMON, CollectibleSource.QUEST, "A postcard of the historic city walls.", null, "historic_city", false),

        // Business District
        Collectible("col_business_card", "Business Card", "名片", CollectibleCategory.VOCABULARY_CARD, CollectibleRarity.COMMON, CollectibleSource.NPC, "A professional business card.", null, "business_district", false),
        Collectible("col_modern_art", "Modern Art Piece", "现代艺术品", CollectibleCategory.PAINTING, CollectibleRarity.UNCOMMON, CollectibleSource.SHOP, "A piece of modern Chinese art.", null, "business_district", false),
        Collectible("col_coffee_book", "Coffee Table Book", "咖啡桌书", CollectibleCategory.BOOK, CollectibleRarity.COMMON, CollectibleSource.SHOP, "A book about modern China.", null, "business_district", false),
        Collectible("col_district_photo", "District Photo", "商业区照片", CollectibleCategory.PHOTOGRAPH, CollectibleRarity.COMMON, CollectibleSource.EXPLORATION, "A photo of the modern skyline.", null, "business_district", false),

        // Shanghai
        Collectible("col_pearl_tower_model", "Pearl Tower Model", "东方明珠模型", CollectibleCategory.SOUVENIR, CollectibleRarity.RARE, CollectibleSource.SHOP, "A miniature model of the Oriental Pearl Tower.", null, "shanghai", false),
        Collectible("col_shanghai_silk", "Shanghai Silk", "上海丝绸", CollectibleCategory.TEXTILE, CollectibleRarity.UNCOMMON, CollectibleSource.SHOP, "A piece of Shanghai silk.", null, "shanghai", false),
        Collectible("col_bund_postcard", "Bund Postcard", "外滩明信片", CollectibleCategory.POSTCARD, CollectibleRarity.COMMON, CollectibleSource.EXPLORATION, "A postcard of the Bund.", null, "shanghai", false),
        Collectible("col_shanghai_photo", "Shanghai Photo", "上海照片", CollectibleCategory.PHOTOGRAPH, CollectibleRarity.COMMON, CollectibleSource.EXPLORATION, "A photo of the Shanghai skyline.", null, "shanghai", false),
        Collectible("col_jade_bracelet", "Jade Bracelet", "玉镯", CollectibleCategory.JADE, CollectibleRarity.EPIC, CollectibleSource.NPC, "A beautiful jade bracelet.", "Jade is believed to bring good luck.", "shanghai", false),
        Collectible("col_shanghai_stamp", "Shanghai Stamp", "上海印章", CollectibleCategory.STAMP, CollectibleRarity.UNCOMMON, CollectibleSource.EXPLORATION, "A stamp from Shanghai.", null, "shanghai", false),

        // Beijing
        Collectible("col_forbidden_city_scale", "Forbidden City Scale", "故宫模型", CollectibleCategory.SOUVENIR, CollectibleRarity.EPIC, CollectibleSource.SHOP, "A detailed scale model of the Forbidden City.", null, "beijing", false),
        Collectible("col_imperial_scroll", "Imperial Scroll", "皇家卷轴", CollectibleCategory.SCROLL, CollectibleRarity.RARE, CollectibleSource.EXPLORATION, "A scroll with imperial calligraphy.", null, "beijing", false),
        Collectible("col_peking_opera_mask", "Peking Opera Mask", "京剧面具", CollectibleCategory.SOUVENIR, CollectibleRarity.UNCOMMON, CollectibleSource.NPC, "A traditional Peking opera mask.", "Peking opera is a traditional Chinese art form.", "beijing", false),
        Collectible("col_beijing_postcard", "Beijing Postcard", "北京明信片", CollectibleCategory.POSTCARD, CollectibleRarity.COMMON, CollectibleSource.EXPLORATION, "A postcard of the Forbidden City.", null, "beijing", false),
        Collectible("col_beijing_photo", "Beijing Photo", "北京照片", CollectibleCategory.PHOTOGRAPH, CollectibleRarity.COMMON, CollectibleSource.EXPLORATION, "A photo of Tiananmen Square.", null, "beijing", false),

        // Great Wall
        Collectible("col_wall_brick", "Wall Brick Fragment", "城墙砖块", CollectibleCategory.SOUVENIR, CollectibleRarity.RARE, CollectibleSource.EXPLORATION, "A small fragment of the Great Wall.", "The Great Wall is over 13,000 miles long.", "great_wall", false),
        Collectible("col_wall_photo", "Wall Photo", "长城照片", CollectibleCategory.PHOTOGRAPH, CollectibleRarity.COMMON, CollectibleSource.EXPLORATION, "A stunning photo of the Great Wall.", null, "great_wall", false),
        Collectible("col_wall_postcard", "Wall Postcard", "长城明信片", CollectibleCategory.POSTCARD, CollectibleRarity.COMMON, CollectibleSource.EXPLORATION, "A postcard of the Great Wall.", null, "great_wall", false),
        Collectible("col_wall_stamp", "Wall Stamp", "长城印章", CollectibleCategory.STAMP, CollectibleRarity.UNCOMMON, CollectibleSource.EXPLORATION, "A stamp from the Great Wall.", null, "great_wall", false),

        // Phoenix Summit
        Collectible("col_summit_flag", "Summit Flag", "山顶旗帜", CollectibleCategory.SOUVENIR, CollectibleRarity.LEGENDARY, CollectibleSource.ACHIEVEMENT, "A flag marking your summit achievement.", null, "phoenix_summit", false),
        Collectible("col_summit_photo", "Summit Photo", "山顶照片", CollectibleCategory.PHOTOGRAPH, CollectibleRarity.RARE, CollectibleSource.EXPLORATION, "A photo from the summit.", null, "phoenix_summit", false),
        Collectible("col_phoenix_feather", "Phoenix Feather", "凤凰羽毛", CollectibleCategory.SOUVENIR, CollectibleRarity.LEGENDARY, CollectibleSource.HIDDEN, "A mythical feather from the summit.", "The phoenix symbolizes rebirth and renewal in Chinese culture.", "phoenix_summit", false, null, false),
    )

fun createInitialTimeline() = listOf(
        DiscoveryEvent("evt_1", EntryType.REGION_DISCOVERED, "发现清远村", "到达了旅程的起点", "qingyuan_village", System.currentTimeMillis() - 86400000 * 7),
        DiscoveryEvent("evt_2", EntryType.NPC_MET, "认识梅奶奶", "在清远村认识了第一位朋友", "qingyuan_village", System.currentTimeMillis() - 86400000 * 6),
        DiscoveryEvent("evt_3", EntryType.COLLECTIBLE_FOUND, "发现古老茶具", "在茶馆发现了精美的茶具", "qingyuan_village", System.currentTimeMillis() - 86400000 * 6),
        DiscoveryEvent("evt_4", EntryType.QUEST_COMPLETED, "完成帮助梅奶奶", "帮助梅奶奶购买了食材", "qingyuan_village", System.currentTimeMillis() - 86400000 * 5),
        DiscoveryEvent("evt_5", EntryType.REGION_DISCOVERED, "发现翡翠森林", "进入了神秘的森林", "jade_forest", System.currentTimeMillis() - 86400000 * 5),
        DiscoveryEvent("evt_6", EntryType.COLLECTIBLE_FOUND, "发现玉佩", "在森林中找到了玉佩", "jade_forest", System.currentTimeMillis() - 86400000 * 4),
        DiscoveryEvent("evt_7", EntryType.FRIENDSHIP_LEVEL_UP, "与梅奶奶成为朋友", "友谊达到了新水平", "qingyuan_village", System.currentTimeMillis() - 86400000 * 3),
        DiscoveryEvent("evt_8", EntryType.VOCABULARY_LEARNED, "学会了10个新词汇", "在清远村学会了基础词汇", "qingyuan_village", System.currentTimeMillis() - 86400000 * 2),
    )

fun createInitialAchievements() = listOf(
        AchievementProgress("first_region", "初次探索", "First Steps", "发现第一个区域", "🌍", false, null, 0f, 1, 0, "exploration", 50),
        AchievementProgress("first_collectible", "收藏新手", "Collector", "获得第一个收集品", "🎁", false, null, 0f, 1, 0, "collection", 50),
        AchievementProgress("first_stamp", "印章收集者", "Stamp Collector", "获得第一个印章", "📮", false, null, 0f, 1, 0, "passport", 50),
        AchievementProgress("all_regions_discovered", "探索者", "Explorer", "发现所有区域", "🗺", false, null, 0f, 12, 0, "exploration", 500),
        AchievementProgress("all_regions_completed", "旅行大师", "Travel Master", "完成所有区域", "🏆", false, null, 0f, 12, 0, "exploration", 1000),
        AchievementProgress("collect_10", "小收藏家", "Aspiring Collector", "收集10个物品", "📦", false, null, 0f, 10, 2, "collection", 100),
        AchievementProgress("collect_50", "收藏家", "Collector", "收集50个物品", "🎒", false, null, 0f, 50, 2, "collection", 500),
        AchievementProgress("rare_find", "稀有发现", "Rare Find", "获得一个稀有物品", "💎", false, null, 0f, 1, 0, "collection", 200),
        AchievementProgress("cultural_explorer", "文化探索者", "Cultural Explorer", "收集所有茶艺物品", "🍵", false, null, 0f, 5, 0, "culture", 300),
    )

}
