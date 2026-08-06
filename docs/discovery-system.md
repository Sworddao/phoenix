# Vocabulary Discovery & Conversation Rewards System

## Overview

The Vocabulary Discovery system manages how players naturally encounter and unlock new vocabulary through gameplay. Players never manually add words—vocabulary is discovered through conversations, NPC interactions, quests, exploration, and friendship milestones. Every discovery feels rewarding and meaningful.

## Architecture

### Data Models

#### VocabularyDiscovery
Core discovery record:
- `id` — Unique identifier
- `wordId` — Discovered vocabulary word ID
- `word` — Associated VocabularyWord (optional)
- `source` — Discovery source type (enum)
- `sourceId` — Source identifier (NPC ID, quest ID, etc.)
- `sourceName` — Human-readable source name
- `discoveredAt` — Discovery timestamp
- `isFirstDiscovery` — Whether this is the first time discovering this word
- `bonusXp` — Experience points earned
- `bonusFriendshipXp` — Friendship XP earned
- `relatedNpcId` — Associated NPC (optional)
- `relatedQuestId` — Associated quest (optional)
- `relatedRegionId` — Associated region (optional)
- `metadata` — Additional key-value metadata

#### DiscoverySourceType
13 discovery sources:
- `NPC` — NPC Interaction
- `DIALOGUE` — Conversation
- `QUEST` — Quest Reward
- `FRIENDSHIP` — Friendship Milestone
- `REGION` — Region Discovery
- `PASSPORT` — Passport Stamp
- `STORY` — Story Progression
- `LISTENING` — Listening Practice
- `SPEAKING` — Speaking Practice
- `MINI_GAME` — Mini Game
- `FESTIVAL` — Festival Event
- `HIDDEN` — Hidden Discovery
- `EXPLORATION` — Exploration

#### DiscoveryReward
Reward data for discoveries:
- `xp` — Experience points
- `friendshipXp` — Friendship XP
- `vocabularyWords` — Discovered word IDs
- `streakBonus` — Streak bonus multiplier
- `categoryBonus` — Whether category bonus applies
- `regionBonus` — Whether region bonus applies

#### NewlyUnlockedWord
Unlocked word with context:
- `word` — VocabularyWord data
- `source` — Discovery source
- `sourceName` — Source display name
- `discoveredAt` — Discovery timestamp
- `isFirstDiscovery` — First discovery flag
- `reward` — Associated reward

#### DiscoveryHistory
Aggregated discovery history:
- `discoveries` — List of all discoveries
- `totalCount` — Total discoveries
- `todayCount` — Today's discoveries
- `weekCount` — This week's discoveries
- `streakDays` — Current streak days
- `lastDiscoveryDate` — Last discovery timestamp
- `wordsBySource` — Count by source type
- `wordsByCategory` — Count by vocabulary category
- `wordsByRegion` — Count by region

#### DiscoveryStatistics
Comprehensive statistics:
- `totalDiscovered` — Total discoveries
- `totalAvailable` — Total available words
- `todayDiscovered` — Today's count
- `weekDiscovered` — This week's count
- `monthDiscovered` — This month's count
- `streakDays` — Current streak
- `longestStreak` — Longest streak
- `lastDiscoveryDate` — Last discovery timestamp
- `wordsBySource` — Count by source
- `wordsByCategory` — Count by category
- `wordsByMastery` — Count by mastery level
- `wordsByRegion` — Count by region
- `averageDiscoveriesPerDay` — Daily average
- `completionPercentage` — Overall progress

#### DiscoverySession
Session tracking:
- `id` — Session identifier
- `startTime` — Session start
- `endTime` — Session end (null if active)
- `discoveries` — Session discoveries
- `source` — Session source type
- `sourceId` — Session source ID
- `totalXpEarned` — Total XP in session
- `totalFriendshipXpEarned` — Total friendship XP
- `isActive` — Whether session is active

#### DailyDiscovery
Daily aggregation:
- `date` — Date timestamp
- `discoveries` — Day's discoveries
- `totalCount` — Day's count
- `streakDay` — Whether this is a streak day

#### DiscoveryAnimationState
Animation state:
- `isShowing` — Whether animation is active
- `currentWord` — Current word being shown
- `source` — Discovery source
- `sourceName` — Source display name
- `isFirstDiscovery` — First discovery flag
- `reward` — Current reward
- `animationPhase` — Current animation phase

#### AnimationPhase
Animation phases:
- `IDLE` — No animation
- `WORD_APPEARING` — Word appearing animation
- `WORD_DISPLAYING` — Word being displayed
- `REWARD_SHOWING` — Reward display
- `COMPLETING` — Animation completing

#### DiscoveryResult
Sealed class for operation results:
- `WordDiscovered` — New word discovered
- `WordAlreadyDiscovered` — Word was already known
- `BatchDiscovered` — Multiple words discovered
- `Success` — Operation succeeded
- `Error` — Operation failed

### Repository

#### DiscoveryRepository Interface
Read operations (Flow-based):
- `getAllDiscoveries()` — Get all discoveries
- `getDiscoveryById(id)` — Get single discovery
- `getDiscoveriesByWord(wordId)` — Get discoveries for word
- `getDiscoveriesBySource(source)` — Filter by source
- `getDiscoveriesByNpc(npcId)` — Filter by NPC
- `getDiscoveriesByQuest(questId)` — Filter by quest
- `getDiscoveriesByRegion(regionId)` — Filter by region
- `getDiscoveriesByCategory(category)` — Filter by category
- `getRecentDiscoveries(limit)` — Get recent discoveries
- `getTodayDiscoveries()` — Get today's discoveries
- `getWeekDiscoveries()` — Get this week's discoveries
- `getDiscoveryHistory()` — Get aggregated history
- `getDiscoveryStatistics()` — Get comprehensive stats
- `getDiscoverySessions()` — Get all sessions
- `getDailyDiscoveries()` — Get daily aggregations
- `getStreakDays()` — Get current streak

Write operations (suspend):
- `discoverWord(wordId, source, sourceId, sourceName, ...)` — Discover single word
- `discoverWords(wordIds, source, sourceId, sourceName, ...)` — Discover multiple words
- `isWordDiscovered(wordId)` — Check if word is discovered
- `getDiscoveryCount()` — Get total count
- `getDiscoveryCountBySource(source)` — Count by source
- `getDiscoveryCountByRegion(regionId)` — Count by region
- `getDiscoveryCountByCategory(category)` — Count by category
- `resetDailyStreak()` — Reset streak counter
- `recordDiscoverySession(session)` — Record session
- `clearDiscoveryHistory()` — Clear all history

#### MockDiscoveryRepository
Singleton mock implementation with:
- 8 pre-seeded discoveries across multiple sources
- VocabularyRepository dependency for word validation
- MutableStateFlow-based state management
- Daily streak tracking
- Session recording

### ViewModel

#### DiscoveryViewModel
Manages discovery UI state:
- `uiState` — Combined UI state flow
- `selectDiscovery(discovery)` — Select discovery for detail
- `clearSelectedDiscovery()` — Deselect discovery
- `filterBySource(source)` — Filter by source type
- `filterByCategory(category)` — Filter by category
- `search(query)` — Search discoveries
- `clearFilters()` — Reset all filters
- `discoverWord(wordId, source, sourceId, sourceName, ...)` — Discover word
- `discoverWords(wordIds, source, sourceId, sourceName, ...)` — Discover multiple
- `dismissDiscoveryDialog()` — Close discovery dialog
- `updateAnimationPhase(phase)` — Update animation state
- `getSourceDisplayName(source)` — Get source display name

### UI Components

#### VocabularyDiscoveryDialog
Full-screen discovery celebration:
- Word display (pinyin, English, example)
- Source attribution
- Reward display (XP, friendship, streak)
- Animated entry/exit
- Confetti animation (future)

#### NewWordCard
Compact discovery card:
- Word display
- Source attribution
- First discovery indicator
- Timestamp

#### DiscoveryTimeline
Scrollable discovery history:
- LazyColumn of NewWordCards
- Sorted by discovery time
- Grouped by date (future)

#### DiscoverySummaryCard
Statistics overview:
- Total discoveries
- Today's count
- Streak days
- Progress indicators

#### RewardBanner
Reward display:
- XP earned
- Friendship XP earned
- Streak bonus
- Animated reveal

#### DailyDiscoveryCard
Daily summary:
- Date display
- Discovery count
- Streak indicator

### Navigation

Routes:
- `discovery_history` — Discovery history screen
- `discovery_detail/{discoveryId}` — Discovery detail screen

Integration:
- Added to `Screen.kt` as `Screen.DiscoveryHistory` and `Screen.DiscoveryDetail`
- Discovery dialog can be triggered from any screen

## Discovery Logic

### First Discovery Detection
- Checks if word exists in discovery history
- Awards bonus XP for first discoveries
- Triggers celebration animation

### Duplicate Detection
- Returns `WordAlreadyDiscovered` for known words
- No duplicate entries created
- Silently handles re-discovery attempts

### Reward Calculation
Rewards vary by source:
- NPC: +5 XP, +2 Friendship XP
- Dialogue: +5 XP, +3 Friendship XP
- Quest: +15 XP, +5 Friendship XP
- Friendship: +20 XP, +10 Friendship XP, category bonus
- Region: +10 XP, region bonus
- Story: +15 XP
- Hidden: +25 XP, category + region bonus

Difficulty modifiers:
- Intermediate: +3 XP
- Upper Intermediate: +5 XP
- Advanced: +8 XP

Streak bonus: +2 XP per day (max +20)

### Daily Streak Tracking
- Tracks consecutive discovery days
- Resets if a day is missed
- Awards streak bonus XP

## Integration Points

### Dialogue System
When a conversation reaches a node containing vocabulary:
- `UNLOCK_VOCABULARY` action triggers discovery
- Source: `DIALOGUE`
- Links to dialogue ID and NPC

### Quest System
Quest rewards unlock vocabulary:
- `QuestReward.vocabulary` word IDs are discovered
- Source: `QUEST`
- Links to quest ID and NPC

### NPC System
NPC profiles display:
- Words taught by this NPC
- Words already learned
- Completion percentage

### World System
Each region tracks:
- Vocabulary available in region
- Vocabulary discovered in region
- Region completion percentage

### Passport System
Passport displays:
- Vocabulary learned in each region
- Discovery timeline
- Total vocabulary progress

### Friendship System
Higher friendship levels unlock:
- Exclusive cultural vocabulary
- Bonus XP for friendship discoveries
- Category-specific rewards

## Statistics

Tracked metrics:
- Total words discovered
- Today's discoveries
- Weekly discoveries
- Monthly discoveries
- Discovery streak
- Category completion
- Region completion
- NPC completion
- Source distribution
- Mastery distribution

## Testing

### Unit Tests

#### DiscoveryModelsTest
- Data class default values
- Enum entries and properties
- Sealed class results
- Animation state

#### DiscoveryRepositoryTest
- Read operations (getAll, getById, filter)
- Write operations (discover, batch discover)
- Duplicate detection
- Streak tracking
- Session recording
- Error handling

### Test Coverage
- 13 source types verified
- 8 initial discoveries tested
- All repository methods covered

## Future Enhancements

### Phase 2
- Confetti animations
- Sound effects for discoveries
- Haptic feedback
- Achievement integration
- Social sharing

### Phase 3
- Adaptive review scheduling
- Spaced repetition
- Voice recognition discoveries
- AR discoveries
- Multiplayer challenges

### Phase 4
- AI-powered recommendations
- Context-aware suggestions
- Language exchange
- Cultural deep dives
- Festival events
