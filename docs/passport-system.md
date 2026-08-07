# Passport & Collectibles System

## Overview

The Passport & Collectibles System tracks the player's journey through China, recording discoveries, collectibles, stamps, and achievements. It provides a comprehensive record of the player's adventure and serves as a progress tracker across all regions.

## Architecture

### Data Models

#### Passport
Top-level model tracking the player's complete journey:
- `id` — Unique identifier
- `playerName` — Player's display name
- `createdAt` — Account creation timestamp
- `lastUpdated` — Last modification timestamp
- `totalStamps` — Number of stamps earned
- `totalCollectibles` — Total collectibles found
- `totalDiscoveries` — Total regions discovered
- `currentChapter` — Current story chapter
- `regions` — Map of region passport data
- `collectibles` — Map of collected items
- `timeline` — List of discovery events

Computed properties:
- `completionPercentage` — Overall completion across all regions
- `collectedCount` — Number of collected items
- `totalCount` — Total number of items
- `stampCount` — Number of stamps earned

#### PassportRegion
Region-specific passport data:
- `regionId` — Unique region identifier
- `regionName` — English region name
- `regionNameCn` — Chinese region name
- `isDiscovered` — Whether player has visited
- `isCompleted` — Whether region is 100% complete
- `hasStamp` — Whether stamp has been earned
- `stampRarity` — Rarity of earned stamp
- `stats` — Region statistics
- `completionPercentage` — Region completion progress
- `totalPlayTimeMinutes` — Time spent in region
- `notes` — Player notes

#### Collectible
Individual collectible item:
- `id` — Unique identifier
- `name` — English name
- `nameCn` — Chinese name
- `category` — Collectible category (enum)
- `rarity` — Item rarity (enum)
- `source` — How item was obtained (enum)
- `description` — Item description
- `culturalNote` — Cultural significance
- `region` — Region where found
- `isCollected` — Collection status
- `isHidden` — Whether item is hidden
- `isDisplayed` — Whether item is shown
- `isTradeable` — Whether item can be traded
- `xpValue` — Experience points value

#### CollectibleCategory
20 categories of collectibles:
- `TEA` — Tea-related items
- `BAMBOO` — Bamboo crafts
- `LANTERN` — Lanterns and lights
- `SOUVENIR` — Tourist souvenirs
- `FESTIVAL_TICKET` — Event tickets
- `PHOTOGRAPH` — Photos and pictures
- `INSTRUMENT` — Musical instruments
- `RECIPE_CARD` — Recipe cards
- `STORY_SCROLL` — Story scrolls
- `VOCABULARY_CARD` — Language learning cards
- `VOICE_RECORDING` — Audio recordings
- `BOOK` — Books and literature
- `POSTCARD` — Postcards
- `COIN` — Coins and currency
- `STAMP` — Postal stamps
- `SCROLL` — Ancient scrolls
- `PAINTING` — Artwork
- `CERAMIC` — Pottery and ceramics
- `TEXTILE` — Fabric and clothing
- `JADE` — Jade artifacts

#### CollectibleRarity
Rarity tiers with drop chances:
- `COMMON` — 50% drop chance
- `UNCOMMON` — 30% drop chance
- `RARE` — 15% drop chance
- `EPIC` — 4% drop chance
- `LEGENDARY` — 1% drop chance

#### CollectibleSource
How collectibles are obtained:
- `QUEST` — Completing quests
- `NPC` — From NPC interactions
- `EXPLORATION` — Discovering locations
- `DIALOGUE` — Dialogue choices
- `SHOP` — Purchasing from shops
- `FESTIVAL` — Festival events
- `DAILY` — Daily activities
- `ACHIEVEMENT` — Achievement rewards
- `HIDDEN` — Hidden discoveries

#### StampRarity
Stamp tiers with associated colors:
- `BRONZE` — Bronze stamp
- `SILVER` — Silver stamp
- `GOLD` — Gold stamp
- `PLATINUM` — Platinum stamp
- `DIAMOND` — Diamond stamp

#### PassportEntry
Individual passport entry:
- `entryId` — Unique identifier
- `entryType` — Type of entry (enum)
- `regionId` — Associated region
- `description` — Entry description
- `timestamp` — When entry was created

#### EntryType
12 types of passport entries:
- `STAMP_EARNED` — Stamp awarded
- `COLLECTIBLE_FOUND` — Item discovered
- `QUEST_COMPLETED` — Quest finished
- `NPC_MET` — New NPC encountered
- `FRIENDSHIP_LEVEL_UP` — Friendship increased
- `DIALOGUE_COMPLETED` — Conversation finished
- `REGION_DISCOVERED` — New region explored
- `REGION_COMPLETED` — Region 100% complete
- `VOCABULARY_LEARNED` — New words learned
- `SPEAKING_PRACTICE` — Speaking practice completed
- `LISTENING_PRACTICE` — Listening practice completed
- `ACHIEVEMENT_UNLOCKED` — Achievement earned

#### AchievementProgress
Achievement tracking:
- `achievementId` — Unique identifier
- `achievementName` — Achievement name
- `description` — Achievement description
- `isUnlocked` — Whether achieved
- `currentCount` — Current progress
- `requiredCount` — Required progress
- `unlockedAt` — Unlock timestamp

#### DiscoveryEvent
Timeline event:
- `eventId` — Unique identifier
- `eventType` — Event type (EntryType)
- `regionId` — Associated region
- `regionName` — Region display name
- `description` — Event description
- `timestamp` — When event occurred

#### CollectionProgress
Aggregated collection data:
- `totalCount` — Total items available
- `collectedCount` — Items collected
- `byCategory` — Progress by category
- `byRarity` — Progress by rarity
- `byRegion` — Progress by region

#### PassportStats
Aggregated passport statistics:
- `totalRegions` — Total regions available
- `discoveredRegions` — Regions discovered
- `completedRegions` — Regions completed
- `totalStamps` — Stamps earned
- `totalCollectibles` — Total collectibles
- `collectedItems` — Items collected
- `totalDiscoveries` — Discovery events
- `totalPlayTimeMinutes` — Total play time
- `favoriteRegion` — Most visited region
- `rarestCollectible` — Rarest item found
- `completionPercentage` — Overall completion

### Repository

#### PassportRepository Interface
Read operations (Flow-based):
- `getPassport()` — Get player passport
- `getPassportRegion(regionId)` — Get region data
- `getAllRegions()` — Get all regions
- `getCollectibles()` — Get all collectibles
- `getCollectiblesByRegion(regionId)` — Get region collectibles
- `getCollectiblesByCategory(category)` — Get category collectibles
- `getCollectionProgress()` — Get collection stats
- `getDiscoveryTimeline()` — Get timeline events
- `getAchievements()` — Get achievements
- `getRecentEntries(limit)` — Get recent entries

Write operations (suspend):
- `discoverRegion(regionId)` — Mark region discovered
- `completeRegion(regionId)` — Mark region complete
- `earnStamp(regionId)` — Award stamp
- `collectItem(collectibleId)` — Collect item
- `recordEntry(entry)` — Add passport entry
- `recordDiscovery(event)` — Add timeline event
- `updateRegionProgress(regionId, progress)` — Update stats
- `addVocabularyLearned(regionId, count)` — Add vocabulary
- `addFriendshipMade(regionId)` — Add friendship
- `addQuestCompleted(regionId)` — Add quest
- `checkAchievements()` — Check achievements
- `getPassportStats()` — Get aggregated stats

#### MockPassportRepository
Production binds the Room-backed `RoomPassportRepository`; the singleton mock below remains for tests and development. In-memory implementation with:
- 12 regions across 5 chapters
- ~48 collectibles across 20 categories
- 9 achievements
- Pre-seeded data for Qingyuan Village and Jade Forest
- MutableStateFlow-based state management

### ViewModel

#### PassportViewModel
Manages passport UI state:
- `uiState` — Combined UI state flow
- `selectRegion(region)` — Select region for detail
- `clearSelectedRegion()` — Deselect region
- `selectCollectible(collectible)` — Select collectible
- `clearSelectedCollectible()` — Deselect collectible
- `showCollectionGrid()` — Show collection grid
- `hideCollectionGrid()` — Hide collection grid
- `discoverRegion(regionId)` — Discover region
- `completeRegion(regionId)` — Complete region
- `earnStamp(regionId)` — Earn stamp
- `collectItem(collectibleId)` — Collect item
- `checkAchievements()` — Check achievements
- `dismissAchievementNotification()` — Dismiss notification
- `clearError()` — Clear error state

### UI Components

#### PassportStampCard
Displays region passport data:
- Circular stamp indicator (locked/discovered/stamp earned)
- Region name (English + Chinese)
- Progress stats (exploration %, collectibles, vocabulary, friends, quests)
- Stamp rarity badge if earned

#### StampRarityBadge
Small colored badge showing stamp rarity name.

#### RegionCompletionDialog
AlertDialog congratulating player on completing a region:
- Vocabulary learned
- Friendships made
- Quests completed
- Collectibles found/total

#### CollectibleDetailDialog
AlertDialog showing collectible details:
- Name (English + Chinese)
- Description
- Category, rarity, source
- Cultural note

#### CollectionGridDialog
Grid view of all collectibles:
- Category filter chips
- 3-column grid layout
- Collected items highlighted
- Click to view details

### Screens

#### PassportScreen
Main passport screen with:
- Stats card (stamps, collectibles, discoveries)
- Collection progress card
- Region passport cards in LazyColumn
- Timeline events
- Achievements list
- Dialogs for region detail, collectible detail, collection grid

### Navigation

Routes:
- `passport` — Main passport screen

Integration:
- Added to `Screen.kt` as `Screen.Passport`
- Added to bottom navigation with flight icon
- Integrated into `PhoenixApp.kt` NavHost

## Integration Points

- **World Map System** — Region discovery triggers passport updates
- **Quest System** — Quest completion adds passport entries
- **NPC System** — NPC interactions recorded in timeline
- **Dialogue System** — Dialogue completion tracked
- **Friendship System** — Friendship level-ups recorded
- **Listening System** — First listening practice records a `LISTENING_PRACTICE` entry; earned listening badges record `ACHIEVEMENT_UNLOCKED` entries. `PassportScreen`'s `getEventIcon` maps `LISTENING_PRACTICE` to `Icons.Default.Headphones`

## Testing

### Unit Tests

#### PassportModelsTest
- Data class default values
- Computed properties (completionPercentage, collectedCount, etc.)
- Enum entries and properties
- Sealed class results

#### PassportRepositoryTest
- Read operations (getPassport, getRegions, getCollectibles)
- Write operations (discoverRegion, completeRegion, earnStamp)
- State transitions and updates
- Error handling for non-existent items
- Initial state verification

### Test Coverage
- 12 regions tested
- ~48 collectibles verified
- 9 achievements checked
- All repository methods covered

## Future Enhancements

### Phase 2
- Real Room database persistence
- Cloud sync for passport data
- Photo attachments for discoveries
- Voice recordings for memories
- Social sharing features

### Phase 3
- AR passport stamps
- Location-based collectible discovery
- Time-limited festival collectibles
- Trading system between players
- Achievement leaderboards

### Phase 4
- Physical merchandise integration
- QR code collectible scanning
- NFC stamp collection
- Passport printing service
- Museum-quality display frames
