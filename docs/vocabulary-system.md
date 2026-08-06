# Vocabulary Learning Foundation

## Overview

The Vocabulary Learning Foundation provides a comprehensive system for managing Mandarin vocabulary, tracking learner progress through mastery levels, and integrating vocabulary learning with other game systems. It supports 100+ vocabulary entries across 12 categories with full CRUD, search/filter, mastery tracking, and region/NPC/quest integration.

## Architecture

### Data Models

#### VocabularyWord
Core vocabulary entry:
- `id` — Unique identifier (e.g., `greet_001`)
- `mandarin` — Mandarin text
- `pinyin` — Pinyin pronunciation
- `english` — English translation
- `hanzi` — Hanzi characters (optional, defaults to mandarin)
- `audioPath` — Audio file path (optional)
- `category` — Vocabulary category (enum)
- `difficulty` — Difficulty level (enum)
- `exampleSentence` — Example sentence in pinyin
- `exampleTranslation` — Example sentence translation
- `examplePinyin` — Example sentence pinyin
- `relatedNpcId` — Associated NPC (optional)
- `relatedQuestId` — Associated quest (optional)
- `relatedRegionId` — Associated region (optional)
- `discoveredAt` — Discovery timestamp (null = undiscovered)
- `mastery` — Current mastery level (enum)
- `timesReviewed` — Review count
- `timesSpoken` — Spoken count
- `timesHeard` — Heard count
- `isFavorite` — Favorited status
- `notes` — Player notes (optional)
- `tags` — Tags for filtering (optional)

Computed properties:
- `isDiscovered` — Whether word has been discovered (discoveredAt != null)
- `displayHanzi` — Hanzi or mandarin fallback

#### VocabularyCategory
12 categories of vocabulary:
- `GREETINGS` — Greetings and polite phrases
- `FAMILY` — Family members and relationships
- `FOOD` — Food and drink
- `NUMBERS` — Numbers and counting
- `DIRECTIONS` — Directions and navigation
- `TRAVEL` — Travel and transportation
- `SHOPPING` — Shopping and commerce
- `RESTAURANT` — Restaurant and dining
- `WEATHER` — Weather and seasons
- `TIME` — Time and schedules
- `TRANSPORTATION` — Vehicles and movement
- `DAILY_LIFE` — Daily activities and common words

#### VocabularyDifficulty
5 difficulty levels:
- `BEGINNER` — Level 1
- `ELEMENTARY` — Level 2
- `INTERMEDIATE` — Level 3
- `UPPER_INTERMEDIATE` — Level 4
- `ADVANCED` — Level 5

#### VocabularyMastery
5 mastery levels:
- `UNKNOWN` — Level 0 (not yet encountered)
- `SEEN` — Level 1 (discovered)
- `LEARNING` — Level 2 (actively learning)
- `FAMILIAR` — Level 3 (comfortable with)
- `MASTERED` — Level 4 (fully learned)

#### VocabularySource
10 discovery sources:
- `NPC` — NPC Interaction
- `DIALOGUE` — Dialogue
- `QUEST` — Quest Reward
- `FRIENDSHIP` — Friendship Milestone
- `EXPLORATION` — Exploration
- `SHOP` — Shop Purchase
- `FESTIVAL` — Festival Event
- `DAILY` — Daily Activity
- `ACHIEVEMENT` — Achievement Reward
- `HIDDEN` — Hidden Discovery

#### VocabularyProgress
Progress tracking per word:
- `wordId` — Associated word
- `mastery` — Current mastery level
- `timesReviewed` — Review count
- `timesSpoken` — Spoken count
- `timesHeard` — Heard count
- `lastReviewedAt` — Last review timestamp
- `discoveredAt` — Discovery timestamp
- `isFavorite` — Favorited status

#### VocabularyStatistics
Aggregated statistics:
- `totalWords` — Total vocabulary count
- `discoveredWords` — Discovered count
- `masteredWords` — Mastered count
- `favoriteWords` — Favorited count
- `wordsByCategory` — Count by category
- `wordsByMastery` — Count by mastery level
- `wordsByDifficulty` — Count by difficulty
- `totalReviewed` — Total review actions
- `totalSpoken` — Total spoken actions
- `totalHeard` — Total heard actions
- `completionPercentage` — Overall progress

#### VocabularyResult
Sealed class for operation results:
- `Success` — Operation succeeded
- `Error` — Operation failed
- `WordDiscovered` — Word discovered successfully
- `MasteryUpgraded` — Mastery level increased
- `FavoriteToggled` — Favorite status changed
- `WordsFound` — Search results

### Repository

#### VocabularyRepository Interface
Read operations (Flow-based):
- `getAllWords()` — Get all vocabulary
- `getWordById(wordId)` — Get single word
- `getWordsByCategory(category)` — Filter by category
- `getWordsByMastery(mastery)` — Filter by mastery
- `getWordsByDifficulty(difficulty)` — Filter by difficulty
- `getWordsByRegion(regionId)` — Filter by region
- `getWordsByNpc(npcId)` — Filter by NPC
- `getWordsByQuest(questId)` — Filter by quest
- `getDiscoveredWords()` — Get discovered words
- `getUndiscoveredWords()` — Get undiscovered words
- `getFavorites()` — Get favorited words
- `getRecentlyLearned(limit)` — Get recently discovered
- `searchWords(query)` — Search by pinyin/English/Mandarin
- `getStatistics()` — Get aggregated stats
- `getCategories()` — Get all categories
- `getProgress(wordId)` — Get word progress

Write operations (suspend):
- `discoverWord(wordId)` — Mark word discovered
- `updateMastery(wordId, mastery)` — Update mastery level
- `toggleFavorite(wordId)` — Toggle favorite status
- `incrementReview(wordId)` — Increment review count
- `incrementSpoken(wordId)` — Increment spoken count
- `incrementHeard(wordId)` — Increment heard count
- `recordDiscovery(wordId, source)` — Record discovery source
- `addWords(words)` — Add new vocabulary

#### MockVocabularyRepository
Singleton mock implementation with:
- 100+ vocabulary entries across 12 categories
- 3 undiscovered words for testing
- Region-specific vocabulary (qingyuan_village, jade_forest, etc.)
- NPC-specific vocabulary (grandma_mei, owner_lin, taxi_chen)
- Pre-seeded mastery levels and stats
- MutableStateFlow-based state management

### ViewModel

#### VocabularyViewModel
Manages vocabulary UI state:
- `uiState` — Combined UI state flow
- `searchQuery` — Current search query
- `selectedCategory` — Active category filter
- `selectedDifficulty` — Active difficulty filter
- `selectedMastery` — Active mastery filter
- `showFavoritesOnly` — Favorites filter toggle
- `showRecentlyLearned` — Recent filter toggle
- `updateSearchQuery(query)` — Update search
- `selectCategory(category)` — Filter by category
- `selectDifficulty(difficulty)` — Filter by difficulty
- `selectMastery(mastery)` — Filter by mastery
- `toggleFavoritesOnly()` — Toggle favorites filter
- `toggleRecentlyLearned()` — Toggle recent filter
- `clearFilters()` — Reset all filters
- `discoverWord(wordId)` — Discover a word
- `updateMastery(wordId, mastery)` — Update mastery
- `toggleFavorite(wordId)` — Toggle favorite
- `recordReview(wordId)` — Record review action

### UI Components

#### VocabularyScreen
Main vocabulary screen with:
- Search bar with real-time filtering
- Filter chips (category, difficulty, mastery)
- Favorites and recently learned toggles
- Statistics card showing progress
- LazyColumn of vocabulary cards
- Category chip selector

#### VocabularyDetailScreen
Detailed word view with:
- Word display (mandarin, pinyin, English)
- Mastery indicator
- Example sentence with translation
- Action buttons (review, speak, hear)
- Favorite toggle
- Notes section

#### VocabularyCard
Compact word card showing:
- Mandarin text
- Pinyin pronunciation
- English translation
- Mastery indicator
- Category chip
- Favorite icon

#### VocabularyStatisticsCard
Progress overview with:
- Total words / discovered / mastered
- Progress bar
- Category breakdown
- Mastery distribution

#### MasteryIndicator
Visual mastery level indicator:
- Color-coded by level
- Icon for each mastery stage
- Animated transitions

#### CategoryChip
Selectable category chip:
- Category icon
- Category name
- Selected state styling

#### DifficultyChip
Difficulty level chip:
- Difficulty name
- Level number
- Color-coded by difficulty

#### FilterChipsRow
Horizontal scrollable filter row:
- Category chips
- Difficulty chips
- Mastery chips

#### VocabularySearchBar
Search input with:
- Real-time search
- Clear button
- Search icon

### Navigation

Routes:
- `vocabulary` — Main vocabulary list screen
- `vocabulary_detail/{wordId}` — Word detail screen

Integration:
- Added to `Screen.kt` as `Screen.Vocabulary` and `Screen.VocabularyDetail`
- Added to bottom navigation with MenuBook icon
- Integrated into `PhoenixApp.kt` NavHost

## Integration Points

- **NPC System** — NPC-specific vocabulary words
- **Dialogue System** — Vocabulary learned through dialogue
- **Quest System** — Quest reward vocabulary
- **Friendship System** — Friendship milestone vocabulary
- **World Map System** — Region-specific vocabulary
- **Passport System** — Vocabulary tracking in passport

## Testing

### Unit Tests

#### VocabularyModelsTest
- Data class default values
- Computed properties (isDiscovered, displayHanzi)
- Enum entries and properties
- Sealed class results

#### VocabularyRepositoryTest
- Read operations (getAllWords, getWordById, searchWords)
- Write operations (discoverWord, updateMastery, toggleFavorite)
- Filter operations (by category, difficulty, mastery, region)
- Statistics and progress tracking
- Error handling for non-existent items
- Initial state verification

### Test Coverage
- 100+ vocabulary entries verified
- 12 categories tested
- 5 mastery levels checked
- All repository methods covered

## Future Enhancements

### Phase 2
- Real Room database persistence
- Audio playback for pronunciation
- Spaced repetition algorithm
- Writing practice (stroke order)
- Quiz mode

### Phase 3
- Voice recognition for speaking practice
- Social vocabulary sharing
- Vocabulary challenges
- Achievement integration
- Leaderboards

### Phase 4
- AI-powered vocabulary recommendations
- Context-aware word suggestions
- Multi-language support
- Offline audio caching
- Vocabulary export/import
