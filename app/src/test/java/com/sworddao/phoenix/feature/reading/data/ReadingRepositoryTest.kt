package com.sworddao.phoenix.feature.reading.data

import com.sworddao.phoenix.feature.friendship.data.MockFriendshipRepository
import com.sworddao.phoenix.feature.gameplay.data.GameMilestone
import com.sworddao.phoenix.feature.gameplay.data.MockGameProgressRepository
import com.sworddao.phoenix.feature.listening.data.MockListeningRepository
import com.sworddao.phoenix.feature.passport.data.EntryType
import com.sworddao.phoenix.feature.passport.data.MockPassportRepository
import com.sworddao.phoenix.feature.pronunciation.data.MockPronunciationRepository
import com.sworddao.phoenix.feature.quest.data.MockQuestRepository
import com.sworddao.phoenix.feature.quest.data.ObjectiveType
import com.sworddao.phoenix.feature.quest.data.QuestResult
import com.sworddao.phoenix.feature.quest.data.QuestStatus
import com.sworddao.phoenix.feature.reading.data.ReadingResultStatus as Status
import com.sworddao.phoenix.feature.vocabulary.data.MockVocabularyRepository
import com.sworddao.phoenix.feature.vocabulary.data.VocabularyCategory
import com.sworddao.phoenix.feature.vocabulary.data.VocabularyDifficulty
import com.sworddao.phoenix.feature.vocabulary.data.VocabularyWord
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ReadingRepositoryTest {

    private lateinit var vocabularyRepository: MockVocabularyRepository
    private lateinit var questRepository: MockQuestRepository
    private lateinit var friendshipRepository: MockFriendshipRepository
    private lateinit var gameProgressRepository: MockGameProgressRepository
    private lateinit var passportRepository: MockPassportRepository
    private lateinit var pronunciationRepository: MockPronunciationRepository
    private lateinit var listeningRepository: MockListeningRepository
    private lateinit var repository: MockReadingRepository

    @Before
    fun setup() {
        vocabularyRepository = MockVocabularyRepository()
        questRepository = MockQuestRepository()
        friendshipRepository = MockFriendshipRepository()
        gameProgressRepository = MockGameProgressRepository()
        passportRepository = MockPassportRepository()
        pronunciationRepository = MockPronunciationRepository(
            vocabularyRepository = vocabularyRepository,
            questRepository = questRepository,
            friendshipRepository = friendshipRepository,
            gameProgressRepository = gameProgressRepository,
            passportRepository = passportRepository,
        )
        listeningRepository = MockListeningRepository(
            vocabularyRepository = vocabularyRepository,
            questRepository = questRepository,
            friendshipRepository = friendshipRepository,
            gameProgressRepository = gameProgressRepository,
            passportRepository = passportRepository,
            pronunciationRepository = pronunciationRepository,
        )
        repository = MockReadingRepository(
            vocabularyRepository = vocabularyRepository,
            questRepository = questRepository,
            friendshipRepository = friendshipRepository,
            gameProgressRepository = gameProgressRepository,
            passportRepository = passportRepository,
            pronunciationRepository = pronunciationRepository,
            listeningRepository = listeningRepository,
            hanziRenderer = MockHanziRenderer(),
        )
        runBlocking {
            vocabularyRepository.discoverWord("greet_001")
            vocabularyRepository.discoverWord("greet_003")
            vocabularyRepository.discoverWord("food_001")
        }
    }

    // ------------------------------------------------------------------
    // Exercise queries
    // ------------------------------------------------------------------

    @Test
    fun `getAllExercises returns catalog`() = runTest {
        val exercises = repository.getAllExercises().first()
        assertTrue(exercises.isNotEmpty())
        assertEquals(14, exercises.size)
    }

    @Test
    fun `getExerciseById finds and misses`() = runTest {
        assertNotNull(repository.getExerciseById("read_ex_greet_hello").first())
        assertNull(repository.getExerciseById("unknown_exercise").first())
    }

    @Test
    fun `getExercisesByType filters exercises`() = runTest {
        val characterExercises = repository.getExercisesByType(
            ReadingExerciseType.CHARACTER_RECOGNITION
        ).first()
        assertTrue(characterExercises.isNotEmpty())
        characterExercises.forEach {
            assertEquals(ReadingExerciseType.CHARACTER_RECOGNITION, it.type)
        }
    }

    @Test
    fun `getExercisesByDifficulty filters exercises`() = runTest {
        val elementary = repository.getExercisesByDifficulty(
            ReadingDifficulty.ELEMENTARY
        ).first()
        assertTrue(elementary.isNotEmpty())
        elementary.forEach { assertEquals(ReadingDifficulty.ELEMENTARY, it.difficulty) }
    }

    @Test
    fun `getExercisesByWord returns mapped exercises`() = runTest {
        val exercises = repository.getExercisesByWord("greet_001").first()
        assertTrue(exercises.isNotEmpty())
        exercises.forEach { assertEquals("greet_001", it.relatedWordId) }
    }

    @Test
    fun `getExercisesByNpc returns exercises for an npc`() = runTest {
        val exercises = repository.getExercisesByNpc("grandma_mei").first()
        assertTrue(exercises.isNotEmpty())
        exercises.forEach { assertEquals("grandma_mei", it.relatedNpcId) }
    }

    @Test
    fun `getExercisesByQuest filters quest exercises`() = runTest {
        val before = repository.getExercisesByQuest("quest_order_tea").first()
        assertTrue(before.isEmpty())

        repository.addExercises(
            listOf(
                ReadingExercise(
                    id = "read_ex_quest_menu",
                    type = ReadingExerciseType.CONTEXT_READING,
                    difficulty = ReadingDifficulty.ELEMENTARY,
                    hanzi = "喝茶",
                    pinyin = "hē chá",
                    english = "drink tea",
                    prompt = "p",
                    choices = listOf(ReadingChoice("choice_0", "喝茶")),
                    relatedQuestId = "quest_order_tea",
                )
            )
        )

        val after = repository.getExercisesByQuest("quest_order_tea").first()
        assertTrue(after.any { it.id == "read_ex_quest_menu" })
    }

    @Test
    fun `getUnlockedExercises excludes locked`() = runTest {
        val before = repository.getUnlockedExercises().first()
        assertTrue(before.none { it.id == "read_ex_npc_li" })

        repository.unlockExercise("read_ex_npc_li")

        val after = repository.getUnlockedExercises().first()
        assertTrue(after.any { it.id == "read_ex_npc_li" })
    }

    @Test
    fun `getRecommendedExercises returns unlocked ordered by order`() = runTest {
        val recommended = repository.getRecommendedExercises(3).first()
        assertEquals(3, recommended.size)
        assertTrue(recommended.all { it.isUnlocked })
        assertEquals(recommended.sortedBy { it.order }, recommended)
    }

    @Test
    fun `unlockExercise errors for unknown exercise`() = runTest {
        assertTrue(repository.unlockExercise("missing") is Status.Error)
    }

    @Test
    fun `unlockExercise is idempotent for already unlocked`() = runTest {
        val result = repository.unlockExercise("read_ex_greet_hello")
        assertTrue(result is Status.Success)
    }

    @Test
    fun `addExercises appends new and skips duplicates`() = runTest {
        val result = repository.addExercises(
            listOf(
                ReadingExercise(
                    id = "read_ex_custom_1",
                    type = ReadingExerciseType.MATCH_HANZI_TO_MEANING,
                    difficulty = ReadingDifficulty.ADVANCED,
                    hanzi = "早上好",
                    pinyin = "zǎo shang hǎo",
                    english = "Good morning",
                    prompt = "p",
                    choices = listOf(ReadingChoice("choice_0", "Good morning")),
                ),
                ReadingExercise(
                    id = "read_ex_greet_hello",
                    type = ReadingExerciseType.MATCH_PINYIN_TO_HANZI,
                    difficulty = ReadingDifficulty.BEGINNER,
                    hanzi = "你好",
                    pinyin = "nǐ hǎo",
                    english = "Hello",
                    prompt = "p",
                    choices = listOf(ReadingChoice("choice_0", "你好")),
                ),
            )
        )
        assertTrue(result is Status.Success)

        val ids = repository.getAllExercises().first().map { it.id }
        assertEquals(1, ids.count { it == "read_ex_custom_1" })
        assertEquals(1, ids.count { it == "read_ex_greet_hello" })
    }

    // ------------------------------------------------------------------
    // Session lifecycle
    // ------------------------------------------------------------------

    @Test
    fun `startSession returns a session with selected exercises`() = runTest {
        val config = ReadingSessionConfig(
            exerciseType = ReadingExerciseType.CHARACTER_RECOGNITION,
            difficulty = ReadingDifficulty.BEGINNER,
            exerciseCount = 3,
        )

        val session = repository.startSession(config)

        assertTrue(session.exerciseIds.isNotEmpty())
        assertTrue(session.exerciseIds.size <= 3)
        assertFalse(session.isCompleted)
        assertNull(session.completedAt)
    }

    @Test
    fun `startSession builds dynamic word exercise from vocabulary`() = runTest {
        vocabularyRepository.addWords(
            listOf(
                VocabularyWord(
                    id = "brand_new_word",
                    mandarin = "测试",
                    pinyin = "cè shì",
                    english = "test",
                    hanzi = "测试",
                    category = VocabularyCategory.DAILY_LIFE,
                    difficulty = VocabularyDifficulty.BEGINNER,
                    exampleSentence = "Cè shì.",
                    exampleTranslation = "Test.",
                    examplePinyin = "cè shì.",
                )
            )
        )

        val session = repository.startSession(
            ReadingSessionConfig(
                wordIds = listOf("brand_new_word"),
                exerciseCount = 1,
            )
        )

        assertEquals(1, session.exerciseIds.size)
        val exercise = repository.getExerciseById(session.exerciseIds.first()).first()
        assertNotNull(exercise)
        assertEquals("brand_new_word", exercise?.relatedWordId)
        assertEquals("测试", exercise?.hanzi)
    }

    @Test
    fun `completeSession marks session complete and returns statistics`() = runTest {
        val session = repository.startSession(ReadingSessionConfig(exerciseCount = 2))

        val result = repository.completeSession(session)

        assertTrue(result is Status.SessionCompleted)
        val completed = (result as Status.SessionCompleted).session
        assertTrue(completed.isCompleted)
        assertNotNull(completed.completedAt)

        val stats = repository.getReadingStatistics().first()
        assertEquals(1, stats.totalSessions)
    }

    @Test
    fun `completeSession without active session returns error`() = runTest {
        val result = repository.completeSession(ReadingSession(exerciseIds = listOf("a")))
        assertTrue(result is Status.Error)
    }

    @Test
    fun `completeSession with mismatched id returns error`() = runTest {
        val session = repository.startSession(ReadingSessionConfig())
        val result = repository.completeSession(session.copy(id = "different_id"))
        assertTrue(result is Status.Error)
    }

    @Test
    fun `submitAnswer without active session returns error`() = runTest {
        val result = repository.submitAnswer(attemptFor("read_ex_greet_hello"))
        assertTrue(result is Status.Error)
    }

    @Test
    fun `submitAnswer with unknown exercise returns error`() = runTest {
        repository.startSession(ReadingSessionConfig())
        val result = repository.submitAnswer(attemptFor("unknown_exercise"))
        assertTrue(result is Status.Error)
    }

    // ------------------------------------------------------------------
    // Attempts, progress, statistics
    // ------------------------------------------------------------------

    @Test
    fun `successful answer updates progress for the word`() = runTest {
        repository.startSession(ReadingSessionConfig())
        val result = repository.submitAnswer(successAttempt("read_ex_greet_hello"))

        assertTrue(result is Status.ExerciseCompleted)
        val progress = repository.getReadingProgress("greet_001").first()
        assertNotNull(progress)
        assertEquals(1, progress?.totalAttempts)
        assertEquals(1, progress?.correctAttempts)
        assertEquals(1, progress?.timesRead)
        assertTrue((progress?.bestTimeMs ?: 0) > 0)
    }

    @Test
    fun `failed answer updates attempt counts but not correct`() = runTest {
        repository.startSession(ReadingSessionConfig())
        repository.submitAnswer(failAttempt("read_ex_greet_hello"))

        val progress = repository.getReadingProgress("greet_001").first()
        assertEquals(1, progress?.totalAttempts)
        assertEquals(0, progress?.correctAttempts)
        assertEquals(0, progress?.timesRead)
    }

    @Test
    fun `statistics accumulate across attempts`() = runTest {
        repository.startSession(ReadingSessionConfig())
        repository.submitAnswer(successAttempt("read_ex_greet_hello"))
        repository.submitAnswer(failAttempt("read_ex_greet_hello"))
        repository.submitAnswer(successAttempt("read_ex_greet_thanks"))

        val stats = repository.getReadingStatistics().first()
        assertEquals(3, stats.totalAttempts)
        assertEquals(2, stats.correctAttempts)
        assertTrue(stats.exercisesByType.isNotEmpty())
        assertTrue(stats.exercisesByDifficulty.isNotEmpty())
    }

    @Test
    fun `all reading progress is observable`() = runTest {
        repository.startSession(ReadingSessionConfig())
        repository.submitAnswer(successAttempt("read_ex_greet_hello"))

        val all = repository.getAllReadingProgress().first()
        assertEquals(1, all.size)
        assertEquals("greet_001", all.first().wordId)
    }

    @Test
    fun `updateProgress recomputes mastery`() = runTest {
        val result = repository.updateProgress(
            ReadingProgress(
                itemId = "greet_001",
                wordId = "greet_001",
                totalAttempts = 20,
                correctAttempts = 15,
            )
        )

        assertTrue(result is Status.ProgressUpdated)
        val progress = (result as Status.ProgressUpdated).progress
        assertEquals(ReadingMastery.CONFIDENT, progress.masteryLevel)
    }

    @Test
    fun `perfect answers upgrade to mastered mastery`() = runTest {
        val result = repository.updateProgress(
            ReadingProgress(
                itemId = "greet_001",
                wordId = "greet_001",
                totalAttempts = 20,
                correctAttempts = 20,
            )
        )

        val progress = (result as Status.ProgressUpdated).progress
        assertEquals(ReadingMastery.MASTERED, progress.masteryLevel)
    }

    @Test
    fun `recordReveal tracks reveal counts and stats`() = runTest {
        val result = repository.recordReveal("greet_001")
        assertTrue(result is Status.RevealRecorded)
        assertEquals(1, (result as Status.RevealRecorded).revealCount)

        val progress = repository.getReadingProgress("greet_001").first()
        assertEquals(1, progress?.timesRevealed)
        assertTrue(progress?.hasRevealedHanzi == true)

        val stats = repository.getReadingStatistics().first()
        assertEquals(1, stats.totalReveals)
    }

    // ------------------------------------------------------------------
    // Streaks
    // ------------------------------------------------------------------

    @Test
    fun `streak builds over consecutive practice days`() = runTest {
        repository.startSession(ReadingSessionConfig())
        val today = System.currentTimeMillis()
        val yesterday = today - DAY_MILLIS
        val twoDaysAgo = today - 2 * DAY_MILLIS

        repository.submitAnswer(successAttempt("read_ex_greet_hello", timestamp = twoDaysAgo))
        repository.submitAnswer(successAttempt("read_ex_greet_hello", timestamp = yesterday))
        repository.submitAnswer(successAttempt("read_ex_greet_hello", timestamp = today))

        val stats = repository.getReadingStatistics().first()
        assertEquals(3, stats.currentStreak)
        assertEquals(3, stats.longestStreak)

        val result = repository.getReadingBadges().first()
        assertTrue(result.any { it.id == "read_streak_3" && it.isEarned })
    }

    @Test
    fun `streak resets when practice skipped a day`() = runTest {
        repository.startSession(ReadingSessionConfig())
        val today = System.currentTimeMillis()
        val threeDaysAgo = today - 3 * DAY_MILLIS

        repository.submitAnswer(successAttempt("read_ex_greet_hello", timestamp = threeDaysAgo))
        repository.submitAnswer(successAttempt("read_ex_greet_hello", timestamp = today))

        val stats = repository.getReadingStatistics().first()
        assertEquals(1, stats.currentStreak)
    }

    @Test
    fun `read_streak_3 badge earned after three consecutive days`() = runTest {
        repository.startSession(ReadingSessionConfig())
        val today = System.currentTimeMillis()
        val yesterday = today - DAY_MILLIS
        val twoDaysAgo = today - 2 * DAY_MILLIS

        repository.submitAnswer(successAttempt("read_ex_greet_hello", timestamp = twoDaysAgo))
        repository.submitAnswer(successAttempt("read_ex_greet_hello", timestamp = yesterday))
        val result = repository.submitAnswer(
            successAttempt("read_ex_greet_hello", timestamp = today)
        )

        assertTrue(result is Status.ExerciseCompleted)
        assertEquals(3, (result as Status.ExerciseCompleted).result.currentStreak)
        assertTrue(result.result.streakContinued)

        val badges = repository.getReadingBadges().first()
        assertTrue(badges.any { it.id == "read_streak_3" && it.isEarned })
    }

    // ------------------------------------------------------------------
    // Rewards
    // ------------------------------------------------------------------

    @Test
    fun `successful attempt grants xp reward`() = runTest {
        repository.startSession(ReadingSessionConfig())
        val result = repository.submitAnswer(
            successAttempt("read_ex_greet_hello")
        ) as Status.ExerciseCompleted

        assertEquals(10, result.result.xpEarned)
    }

    @Test
    fun `failed attempt grants no xp`() = runTest {
        repository.startSession(ReadingSessionConfig())
        val result = repository.submitAnswer(
            failAttempt("read_ex_greet_hello")
        ) as Status.ExerciseCompleted

        assertEquals(0, result.result.xpEarned)
    }

    @Test
    fun `xp streak bonus applies on later correct answers`() = runTest {
        repository.startSession(ReadingSessionConfig())
        val today = System.currentTimeMillis()
        val yesterday = today - DAY_MILLIS

        repository.submitAnswer(successAttempt("read_ex_greet_hello", timestamp = yesterday))
        val result = repository.submitAnswer(
            successAttempt("read_ex_greet_hello", timestamp = today)
        ) as Status.ExerciseCompleted

        assertEquals(15, result.result.xpEarned)
    }

    @Test
    fun `session accumulates xp across attempts`() = runTest {
        val session = repository.startSession(ReadingSessionConfig())
        repository.submitAnswer(successAttempt("read_ex_greet_hello"))
        repository.submitAnswer(successAttempt("read_ex_greet_thanks"))

        val completed = repository.completeSession(session) as Status.SessionCompleted
        assertEquals(20, completed.session.totalXpEarned)
    }

    @Test
    fun `friendship bonus is granted for npc exercises`() = runTest {
        repository.startSession(ReadingSessionConfig())
        val result = repository.submitAnswer(
            successAttempt("read_ex_greet_hello")
        ) as Status.ExerciseCompleted

        assertEquals(2, result.result.friendshipBonusEarned)
        val state = friendshipRepository.getFriendshipState("grandma_mei").first()
        assertNotNull(state)
    }

    @Test
    fun `failed attempts grant no friendship bonus`() = runTest {
        repository.startSession(ReadingSessionConfig())
        val result = repository.submitAnswer(
            failAttempt("read_ex_greet_hello")
        ) as Status.ExerciseCompleted

        assertEquals(0, result.result.friendshipBonusEarned)
    }

    @Test
    fun `first personal best flagged and slower later attempts are not`() = runTest {
        repository.startSession(ReadingSessionConfig())
        val first = repository.submitAnswer(
            successAttempt("read_ex_greet_hello")
        ) as Status.ExerciseCompleted
        assertTrue(first.result.isNewPersonalBest)

        val second = repository.submitAnswer(
            successAttempt("read_ex_greet_hello", timeTakenMs = 5000)
        ) as Status.ExerciseCompleted
        assertFalse(second.result.isNewPersonalBest)
    }

    @Test
    fun `faster later attempt flags a new personal best`() = runTest {
        repository.startSession(ReadingSessionConfig())
        repository.submitAnswer(successAttempt("read_ex_greet_hello", timeTakenMs = 5000))

        val second = repository.submitAnswer(
            successAttempt("read_ex_greet_hello", timeTakenMs = 1000)
        ) as Status.ExerciseCompleted
        assertTrue(second.result.isNewPersonalBest)
    }

    // ------------------------------------------------------------------
    // Badges
    // ------------------------------------------------------------------

    @Test
    fun `first successful answer earns read_first badge`() = runTest {
        repository.startSession(ReadingSessionConfig())
        val result = repository.submitAnswer(
            successAttempt("read_ex_greet_hello")
        ) as Status.ExerciseCompleted

        val badges = repository.getReadingBadges().first()
        val first = badges.find { it.id == "read_first" }
        assertNotNull(first)
        assertTrue(first?.isEarned == true)
        assertNotNull(first?.earnedAt)
        assertEquals(1f, result.result.badgeProgress["read_first"] ?: 0f, 0.001f)
    }

    @Test
    fun `badges and first practice are recorded in passport`() = runTest {
        repository.startSession(ReadingSessionConfig())
        repository.submitAnswer(successAttempt("read_ex_greet_hello"))

        val entries = passportRepository.getRecentEntries(10).first()
        assertTrue(entries.any { it.type == EntryType.ACHIEVEMENT_UNLOCKED })
        assertTrue(entries.any { it.type == EntryType.READING_PRACTICE })
        assertTrue(entries.any { it.title == "第一次阅读练习" })
    }

    // ------------------------------------------------------------------
    // System integration
    // ------------------------------------------------------------------

    @Test
    fun `successful answer increments vocabulary read count`() = runTest {
        repository.startSession(ReadingSessionConfig())
        val before = vocabularyRepository.getWordById("greet_001").first()
        repository.submitAnswer(successAttempt("read_ex_greet_hello"))

        val after = vocabularyRepository.getWordById("greet_001").first()
        assertEquals((before?.timesRead ?: 0) + 1, after?.timesRead)
    }

    @Test
    fun `successful answer unlocks related listening exercise`() = runTest {
        repository.startSession(ReadingSessionConfig())

        val exercise = repository.getExerciseById("read_ex_greet_hello").first()!!
        val listeningId = exercise.relatedListeningExerciseId
        assertNotNull(listeningId)

        val unlockedBefore = listeningRepository.getUnlockedExercises().first()
            .any { it.id == listeningId }

        repository.submitAnswer(successAttempt("read_ex_greet_hello"))

        val unlockedAfter = listeningRepository.getUnlockedExercises().first()
            .any { it.id == listeningId }
        assertTrue(unlockedAfter || unlockedBefore)
    }

    @Test
    fun `successful answer unlocks related speaking exercise`() = runTest {
        repository.startSession(ReadingSessionConfig())
        repository.unlockExercise("read_ex_mei_greeting")

        val exercise = repository.getExerciseById("read_ex_mei_greeting").first()!!
        val speakingId = exercise.relatedSpeakingExerciseId
        assertNotNull(speakingId)

        repository.submitAnswer(successAttempt("read_ex_mei_greeting"))

        val unlockedAfter = pronunciationRepository.getUnlockedExercises().first()
            .any { it.id == speakingId }
        assertTrue(unlockedAfter)
    }

    @Test
    fun `successful answer updates reading quests`() = runTest {
        completeQuestChain()

        val started = questRepository.startQuest("quest_order_tea")
        assertTrue(started is QuestResult.Success)

        repository.startSession(ReadingSessionConfig())
        repository.submitAnswer(successAttempt("read_ex_greet_hello"))
        repository.submitAnswer(successAttempt("read_ex_greet_thanks"))

        val quest = questRepository.getAllQuests().first().find { it.id == "quest_order_tea" }
        assertNotNull(quest)
        assertEquals(QuestStatus.ACTIVE, quest?.status)
        val objective = quest?.objectives?.find { it.id == "obj_3_5" }
        assertNotNull(objective)
        assertEquals(ObjectiveType.READ_CHARACTERS, objective?.type)
        assertTrue((objective?.currentCount ?: 0) >= 2)
    }

    @Test
    fun `successful answer updates game progress milestone`() = runTest {
        repository.startSession(ReadingSessionConfig())
        repository.submitAnswer(successAttempt("read_ex_greet_hello"))

        val gameProgress = gameProgressRepository.getGameProgress().first()
        assertEquals(1, gameProgress.totalReadingPractices)
        assertTrue(GameMilestone.FIRST_READING in gameProgress.milestonesCompleted)
    }

    @Test
    fun `failed attempts do not touch quests or game progress`() = runTest {
        repository.startSession(ReadingSessionConfig())
        repository.submitAnswer(failAttempt("read_ex_greet_hello"))

        val gameProgress = gameProgressRepository.getGameProgress().first()
        assertEquals(0, gameProgress.totalReadingPractices)
        assertFalse(GameMilestone.FIRST_READING in gameProgress.milestonesCompleted)
    }

    // ------------------------------------------------------------------
    // Recorded streak and badge API
    // ------------------------------------------------------------------

    @Test
    fun `recordStreak updates current and longest streak`() = runTest {
        val result = repository.recordStreak(4)

        assertTrue(result is Status.StreakUpdated)
        assertEquals(4, (result as Status.StreakUpdated).currentStreak)

        val stats = repository.getReadingStatistics().first()
        assertEquals(4, stats.currentStreak)
        assertEquals(4, stats.longestStreak)
    }

    @Test
    fun `awardBadge awards a badge once`() = runTest {
        val first = repository.awardBadge("read_first")
        assertTrue(first is Status.BadgeEarned)
        assertTrue((first as Status.BadgeEarned).badge.isEarned)

        val second = repository.awardBadge("read_first")
        assertTrue(second is Status.Error)
    }

    @Test
    fun `awardBadge rejects unknown badge`() = runTest {
        assertTrue(repository.awardBadge("missing_badge") is Status.Error)
    }

    // ------------------------------------------------------------------
    // Hanzi renderer
    // ------------------------------------------------------------------

    @Test
    fun `hanzi renderer tonesOf maps diacritics to tone numbers`() {
        val renderer = MockHanziRenderer()
        assertEquals(listOf(1, 0), renderer.tonesOf("mā ma"))
        assertEquals(listOf(3, 3), renderer.tonesOf("nǐ hǎo"))
        assertEquals(listOf(4, 4, 1), renderer.tonesOf("zài jiàn yī"))
        assertEquals(emptyList<Int>(), renderer.tonesOf(""))
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    private suspend fun successAttempt(
        exerciseId: String,
        timestamp: Long = System.currentTimeMillis(),
        timeTakenMs: Long = 2000,
    ): ReadingAttempt {
        val exercise = repository.getExerciseById(exerciseId).first()
        return ReadingAttempt(
            exerciseId = exerciseId,
            wordId = exercise?.relatedWordId,
            chosenChoiceId = exercise?.correctChoice?.id ?: "choice_0",
            wasCorrect = true,
            revealedHanziBeforeAnswer = false,
            timeTakenMs = timeTakenMs,
            timestamp = timestamp,
        )
    }

    private suspend fun failAttempt(
        exerciseId: String,
        timestamp: Long = System.currentTimeMillis(),
    ): ReadingAttempt {
        val exercise = repository.getExerciseById(exerciseId).first()
        val wrongChoice = exercise?.choices?.firstOrNull { choice ->
            exercise.choices.indexOf(choice) != exercise.correctChoiceIndex
        }
        return ReadingAttempt(
            exerciseId = exerciseId,
            wordId = exercise?.relatedWordId,
            chosenChoiceId = wrongChoice?.id ?: "choice_1",
            wasCorrect = false,
            revealedHanziBeforeAnswer = false,
            timeTakenMs = 3000,
            timestamp = timestamp,
        )
    }

    private fun attemptFor(exerciseId: String): ReadingAttempt =
        ReadingAttempt(
            exerciseId = exerciseId,
            chosenChoiceId = "choice_0",
        )

    private suspend fun completeQuestChain() {
        questRepository.startQuest("quest_help_grandma_mei")
        questRepository.updateObjectiveProgress("quest_help_grandma_mei", "obj_1_1", 1)
        questRepository.updateObjectiveProgress("quest_help_grandma_mei", "obj_1_2", 5)
        questRepository.updateObjectiveProgress("quest_help_grandma_mei", "obj_1_3", 3)
        questRepository.completeQuest("quest_help_grandma_mei")

        val dumplingsStarted = questRepository.startQuest("quest_buy_dumplings")
        assertTrue(dumplingsStarted is QuestResult.Success)
        questRepository.updateObjectiveProgress("quest_buy_dumplings", "obj_2_1", 1)
        questRepository.updateObjectiveProgress("quest_buy_dumplings", "obj_2_2", 5)
        questRepository.updateObjectiveProgress("quest_buy_dumplings", "obj_2_3", 3)
        questRepository.completeQuest("quest_buy_dumplings")
    }

    companion object {
        private const val DAY_MILLIS = 24 * 60 * 60 * 1000L
    }
}