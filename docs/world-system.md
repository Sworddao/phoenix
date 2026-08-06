# World Map & Exploration System

## Overview

The World Map & Exploration System provides a scalable architecture for game world navigation, region progression, and exploration tracking. Players travel from Qingyuan Village to Phoenix Summit, discovering new regions and unlocking content as they progress.

## Architecture

### Data Models

#### WorldRegion
Core region data:
- `id` — Unique identifier
- `name` — English name
- `nameCn` — Chinese name
- `description` — Region description
- `status` — RegionStatus enum
- `order` — Display order
- `chapter` — Story chapter
- `unlockRequirements` — Prerequisites
- `connections` — Connected regions
- `travelMethods` — Available transport
- `npcIds` — NPCs in region
- `questIds` — Quests available
- `mapPositionX/Y` — Map coordinates
- `color` — Theme color
- `icon` — Emoji icon

#### WorldLocation
Specific locations within regions:
- `id` — Unique identifier
- `name` — English name
- `nameCn` — Chinese name
- `description` — Location description
- `regionId` — Parent region
- `type` — LandmarkType enum
- `positionX/Y` — Local coordinates
- `npcIds` — NPCs at location
- `questIds` — Quests at location
- `isDiscovered` — Discovery status
- `isAccessible` — Accessibility status

#### UnlockRequirement
Region unlock conditions:
- `questIds` — Required completed quests
- `npcFriendshipLevel` — Minimum NPC friendship
- `requiredVocabularyCount` — Known words
- `requiredLevel` — Player level
- `requiredRegions` — Prerequisite regions
- `requiredFriends` — Required NPC friends

#### TravelMethod
Transportation options:
- `WALKING` — On foot
- `BUS` — Bus travel
- `TRAIN` — Regular train
- `HIGH_SPEED_RAIL` — High-speed rail
- `TAXI` — Taxi/rideshare
- `BICYCLE` — Bicycle
- `BOAT` — Water transport

#### RegionProgress
Region completion tracking:
- `regionId` — Region identifier
- `status` — Current status
- `completionPercentage` — Progress
- `discoveredLocations` — Found locations
- `completedQuests` — Finished quests
- `collectedItems` — Gathered items
- `unlockedFastTravel` — Fast travel status

#### ExplorationProgress
Overall world progress:
- `totalRegions` — All regions
- `completedRegions` — Finished regions
- `currentRegionId` — Active region
- `totalLocations` — All locations
- `discoveredLocations` — Found locations
- `totalCollectibles` — All items
- `collectedItems` — Gathered items
- `completionPercentage` — Overall progress

#### Landmark
Notable locations:
- `id` — Unique identifier
- `name` — English name
- `nameCn` — Chinese name
- `type` — LandmarkType enum
- `description` — Description
- `regionId` — Parent region
- `positionX/Y` — Coordinates
- `isDiscovered` — Discovery status
- `isInteractable` — Can interact

#### CollectibleLocation
Collectible items:
- `id` — Unique identifier
- `name` — Item name
- `type` — CollectibleType enum
- `regionId` — Parent region
- `locationId` — Specific location
- `positionX/Y` — Coordinates
- `isCollected` — Collection status
- `isHidden` — Visibility status
- `description` — Item description
- `culturalNote` — Cultural context

#### RegionConnection
Travel paths between regions:
- `fromRegionId` — Origin region
- `toRegionId` — Destination region
- `travelMethod` — Available transport
- `travelTimeMinutes` — Journey duration
- `isUnlocked` — Path availability
- `description` — Route description

### Repository

#### WorldRepository Interface
Defines data access methods:
- `getAllRegions()` — All regions
- `getRegionById(id)` — Specific region
- `getRegionConnections(id)` — Region connections
- `getRegionProgress(id)` — Progress tracking
- `getExplorationProgress()` — Overall progress
- `getCurrentRegion()` — Active region
- `getAvailableRegions()` — Ready to visit
- `getUnlockedRegions()` — Accessible regions
- `getLocationsByRegion(id)` — Region locations
- `getLandmarksByRegion(id)` — Region landmarks
- `getCollectiblesByRegion(id)` — Region items
- `travelToRegion(id)` — Change region
- `discoverLocation(id)` — Mark discovered
- `collectItem(id)` — Gather item
- `checkRegionUnlocks()` — Check unlocks
- `completeRegion(id)` — Finish region
- `unlockFastTravel(id)` — Enable fast travel

#### MockWorldRepository
In-memory implementation with:
- 12 regions across 5 chapters
- 26 locations across all regions
- 3 collectible items
- 11 region connections
- Progress persistence
- Automatic unlock checking

### ViewModel

#### WorldViewModel
Manages UI state:
- `uiState` — Current state (WorldUiState)
- `selectRegion(region)` — Select for detail
- `clearSelectedRegion()` — Deselect
- `startTravel(region)` — Begin travel
- `confirmTravel(regionId)` — Execute travel
- `cancelTravel()` — Cancel travel
- `discoverLocation(id)` — Mark discovered
- `collectItem(id)` — Gather item
- `checkRegionUnlocks()` — Check unlocks
- `dismissUnlockedNotification()` — Close notification
- `completeRegion(id)` — Finish region

### UI Components

#### RegionCard
Displays region summary:
- Icon and name
- Chinese name
- Description
- Status badge
- Completion percentage

#### RegionStatusBadge
Color-coded status indicator:
- Gray: Locked
- Green: Available
- Blue: Current
- Light green: Visited
- Gold: Completed

#### WorldMapCanvas
Visual map display:
- Region nodes with icons
- Connection lines
- Color-coded status
- Current region highlight

#### RegionNode
Interactive map node:
- Clickable region icon
- Status-based coloring
- Name label

#### RegionConnection
Visual connection line:
- Between connected regions
- Status-based coloring

#### ExplorationProgressCard
Overall progress display:
- Regions completed
- Completion percentage

#### CurrentLocationBanner
Active region display:
- Region icon and name
- Chinese name
- Status indicator

### Screens

#### WorldMapScreen
Main world map interface:
- Current location banner
- Exploration progress
- Scrollable region list
- Region detail dialog
- Travel confirmation dialog
- Error handling

#### RegionDetailDialog
Region information:
- Icon and names
- Description
- Status badge
- Chapter information
- NPC count
- Quest count
- Travel button

#### TravelConfirmDialog
Travel confirmation:
- Target region info
- Travel method
- Confirm/cancel buttons

### Navigation

#### Routes
- `world_map` — World map screen
- `region_detail/{regionId}` — Region detail (future)

#### Integration
- Added to Screen.kt sealed class
- Integrated into PhoenixApp.kt NavHost
- Accessible from QingyuanVillageScreen

## World Regions

### Chapter 1: First Steps
1. **Qingyuan Village** (清远村) — Starting location
2. **Jade Forest** (翡翠森林) — Nature exploration
3. **Riverside Town** (河畔镇) — River community

### Chapter 2: Discovery
4. **Night Market** (夜市) — Food and culture
5. **Mountain Temple** (山中寺庙) — Spiritual journey
6. **High-Speed Railway** (高铁站) — Modern travel

### Chapter 3: Cities
7. **Historic City** (古城) — Cultural heritage
8. **Business District** (商业区) — Modern commerce

### Chapter 4: Metropolis
9. **Shanghai** (上海) — Urban exploration
10. **Beijing** (北京) — Capital city

### Chapter 5: Summit
11. **Great Wall** (长城) — Iconic landmark
12. **Phoenix Summit** (凤凰山顶) — Final destination

## Integration Points

### NPC System
- Regions contain NPC lists
- NPCs linked to specific locations
- NPC availability by region

### Dialogue System
- Regions track available dialogues
- Location-based dialogue triggers

### Quest System
- Regions contain quest lists
- Quest availability by region
- Region completion through quests

### Friendship System
- NPCs in regions affect friendship
- Regional NPC relationships

## Testing

### Unit Tests
- `WorldModelsTest` — Data model logic
- `WorldRepositoryTest` — Repository operations

### Test Coverage
- Region status transitions
- Travel mechanics
- Location discovery
- Item collection
- Progress tracking
- Unlock checking

## Future Enhancements

- Room persistence for offline storage
- Fast travel system
- Dynamic region events
- Seasonal content
- Hidden areas
- Collectible encyclopedia
- Achievement integration
- Map customization
