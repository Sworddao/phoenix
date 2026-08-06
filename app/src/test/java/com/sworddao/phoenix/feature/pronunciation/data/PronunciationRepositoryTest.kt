package com.sworddao.phoenix.feature.pronunciation.data

import com.sworddao.phoenix.feature.friendship.data.MockFriendshipRepository
import com.sworddao.phoenix.feature.gameplay.data.GameMilestone
import com.sworddao.phoenix.feature.gameplay.data.MockGameProgressRepository
import com.sworddao.phoenix.feature.passport.data.EntryType
import com.sworddao.phoenix.feature.passport.data.MockPassportRepository
import com.sworddao.phoenix.feature.quest.data.MockQuestRepository
import com.sworddao.phoenix.feature.quest.data.QuestResult
import com.sworddao.phoenix.feature.quest.data.QuestStatus
import com.sworddao.phoenix.feature.pronunciation.data.PronunciationResultStatus as Status
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

class PronunciationRepositoryTest {

    private lateinit var vocabularyRepository: MockVocabularyRepository
    private lateinit var questRepository: MockQuestRepository
    private lateinit var friendshipRepository: MockFriendshipRepository
    private lateinit var gameProgressRepository: MockGameProgressRepository
    private lateinit var passportRepository: MockPassportRepository
    private lateinit var repository: MockPronunciationRepository

    @Before
    fun setup() {
        vocabularyRepository = MockVocabularyRepository()
        questRepository = MockQuestRepository()
        friendshipRepository = MockFriendshipRepository()
        gameProgressRepository = MockGameProgressRepository()
        passportRepository = MockPassportRepository()
        repository = MockPronunciationRepository(
            vocabularyRepository = vocabularyRepository,
            questRepository = questRepository,
            friendshipRepository = friendshipRepository,
            gameProgressRepository = gameProgressRepository,
            passportRepository = passportRepository,
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
    }

    @Test
    fun `getExerciseById finds and misses`() = runTest {
        assertNotNull(repository.getExerciseById("pron_ex_greet_hello").first())
        assertNull(repository.getExerciseById("unknown_exercise").first())
    }

    @Test
    fun `getExercisesByType filters exercises`() = runTest {
        val vocabularyExercises = repository.getExercisesByType(
            SpeakingExerciseType.VOCABULARY_WORD
        ).first()
        assertTrue(vocabularyExercises.isNotEmpty())
        vocabularyExercises.forEach {
            assertEquals(SpeakingExerciseType.VOCABULARY_WORD, it.type)
        }
    }

    @Test
    fun `getExercisesByWord returns mapped exercises`() = runTest {
        val exercises = repository.getExercisesByWord("greet_001").first()
        assertTrue(exercises.isNotEmpty())
        exercises.forEach { assertEquals("greet_001", it.wordId) }
    }

    @Test
    fun `getExercisesByNpc returns exercises for an npc`() = runTest {
        val exercises = repository.getExercisesByNpc("grandma_mei").first()
        assertTrue(exercises.isNotEmpty())
        exercises.forEach { assertEquals("grandma_mei", it.relatedNpcId) }
    }

    @Test
    fun `getExercisesByQuest returns quest-linked exercises`() = runTest {
        val exercises = repository.getExercisesByQuest("quest_order_tea").first()
        assertTrue(exercises.isNotEmpty())
        exercises.forEach { assertEquals("quest_order_tea", it.relatedQuestId) }
    }

    @Test
    fun `getUnlockedExercises excludes locked`() = runTest {
        val before = repository.getUnlockedExercises().first()
        assertTrue(before.none { it.id == "pron_ex_dlg_meet" })

        repository.unlockExercise("pron_ex_dlg_meet")

        val after = repository.getUnlockedExercises().first()
        assertTrue(after.any { it.id == "pron_ex_dlg_meet" })
    }

    @Test
    fun `getRecommendedExercises returns unlocked ordered by order score`() = runTest {
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
    fun `addExercises appends new and skips duplicates`() = runTest {
        val result = repository.addExercises(
            listOf(
                SpeakingExercise(
                    id = "custom_ex_1",
                    type = SpeakingExerciseType.FREESTYLE,
                    difficulty = SpeakingDifficulty.ADVANCED,
                    expectedText = "test",
                    expectedPinyin = "test",
                ),
                SpeakingExercise(
                    id = "pron_ex_greet_hello",
                    type = SpeakingExerciseType.VOCABULARY_WORD,
                    difficulty = SpeakingDifficulty.BEGINNER,
                    expectedText = "dup",
                    expectedPinyin = "dup",
                ),
            )
        )
        assertTrue(result is Status.Success)

        val ids = repository.getAllExercises().first().map { it.id }
        assertEquals(1, ids.count { it == "custom_ex_1" })
        assertEquals(1, ids.count { it == "pron_ex_greet_hello" })
    }

    // ------------------------------------------------------------------
    // Session lifecycle
    // ------------------------------------------------------------------

    @Test
    fun `startSession returns a session with selected exercises`() = runTest {
        val config = PronunciationSessionConfig(
            exerciseType = SpeakingExerciseType.VOCABULARY_WORD,
            difficulty = SpeakingDifficulty.BEGINNER,
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
                    mandarin = "cè shì",
                    pinyin = "cè shì",
                    english = "test",
                    category = VocabularyCategory.DAILY_LIFE,
                    difficulty = VocabularyDifficulty.BEGINNER,
                    exampleSentence = "Cè shì.",
                    exampleTranslation = "Test.",
                    examplePinyin = "cè shì.",
                )
            )
        )

        val session = repository.startSession(
            PronunciationSessionConfig(
                wordIds = listOf("brand_new_word"),
                exerciseCount = 1,
            )
        )

        assertEquals(1, session.exerciseIds.size)
        val exercise = repository.getExerciseById(session.exerciseIds.first()).first()
        assertNotNull(exercise)
        assertEquals("brand_new_word", exercise?.wordId)
    }

    @Test
    fun `completeSession marks session complete and returns statistics`() = runTest {
        val session = repository.startSession(PronunciationSessionConfig(exerciseCount = 2))

        val result = repository.completeSession(session)

        assertTrue(result is Status.SessionCompleted)
        val completed = (result as Status.SessionCompleted).session
        assertTrue(completed.isCompleted)
        assertNotNull(completed.completedAt)

        val stats = repository.getSpeakingStatistics().first()
        assertEquals(1, stats.totalSessions)
    }

    @Test
    fun `completeSession without active session returns error`() = runTest {
        val result = repository.completeSession(PronunciationSession(exerciseIds = listOf("a")))
        assertTrue(result is Status.Error)
    }

    @Test
    fun `completeSession with mismatched id returns error`() = runTest {
        val session = repository.startSession(PronunciationSessionConfig())
        val result = repository.completeSession(session.copy(id = "different_id"))
        assertTrue(result is Status.Error)
    }

    @Test
    fun `submitAttempt without active session returns error`() = runTest {
        val result = repository.submitAttempt(attemptFor("pron_ex_greet_hello", "nǐ hǎo"))
        assertTrue(result is Status.Error)
    }

    @Test
    fun `submitAttempt with unknown exercise returns error`() = runTest {
        repository.startSession(PronunciationSessionConfig())
        val result = repository.submitAttempt(attemptFor("unknown_exercise", "nǐ hǎo"))
        assertTrue(result is Status.Error)
    }

    // ------------------------------------------------------------------
    // Attempts, progress, statistics
    // ------------------------------------------------------------------

    @Test
    fun `successful attempt updates progress for the word`() = runTest {
        repository.startSession(PronunciationSessionConfig())
        val result = repository.submitAttempt(
            successAttempt("pron_ex_greet_hello", "nǐ hǎo", confidence = 0.9f)
        )

        assertTrue(result is Status.ExerciseCompleted)
        val progress = repository.getPronunciationProgress("greet_001").first()
        assertNotNull(progress)
        assertEquals(1, progress?.totalAttempts)
        assertEquals(1, progress?.successfulAttempts)
        assertEquals(0.9f, progress?.bestConfidence ?: 0f, 0.001f)
    }

    @Test
    fun `failed attempt updates attempt counts but not successes`() = runTest {
        repository.startSession(PronunciationSessionConfig())
        repository.submitAttempt(failAttempt("pron_ex_greet_hello", "zzz"))

        val progress = repository.getPronunciationProgress("greet_001").first()
        assertEquals(1, progress?.totalAttempts)
        assertEquals(0, progress?.successfulAttempts)
    }

    @Test
    fun `statistics accumulate across attempts`() = runTest {
        repository.startSession(PronunciationSessionConfig())
        repository.submitAttempt(successAttempt("pron_ex_greet_hello", "nǐ hǎo", confidence = 0.9f))
        repository.submitAttempt(failAttempt("pron_ex_greet_hello", "wrong wrong"))
        repository.submitAttempt(successAttempt("pron_ex_tone_hello", "nǐ hǎo ma", confidence = 1.0f))

        val stats = repository.getSpeakingStatistics().first()
        assertEquals(3, stats.totalAttempts)
        assertEquals(2, stats.successfulAttempts)
        assertTrue(stats.averageConfidence > 0f)
        assertTrue(stats.exercisesByType.isNotEmpty())
        assertTrue(stats.exercisesByDifficulty.isNotEmpty())
    }

    @Test
    fun `all pronunciation progress is observable`() = runTest {
        repository.startSession(PronunciationSessionConfig())
        repository.submitAttempt(successAttempt("pron_ex_greet_hello", "nǐ hǎo", confidence = 0.9f))

        val all = repository.getAllPronunciationProgress().first()
        assertEquals(1, all.size)
        assertEquals("greet_001", all.first().wordId)
    }

    @Test
    fun `updateProgress recomputes mastery`() = runTest {
        val result = repository.updateProgress(
            PronunciationProgress(
                wordId = "greet_001",
                totalAttempts = 20,
                successfulAttempts = 15,
            )
        )

        assertTrue(result is Status.ProgressUpdated)
        val progress = (result as Status.ProgressUpdated).progress
        assertEquals(SpeakingMastery.CONFIDENT, progress.masteryLevel)
    }

    @Test
    fun `perfect attempts upgrade to mastered mastery`() = runTest {
        val result = repository.updateProgress(
            PronunciationProgress(
                wordId = "greet_001",
                totalAttempts = 20,
                successfulAttempts = 20,
                bestConfidence = 1.0f,
            )
        )

        val progress = (result as Status.ProgressUpdated).progress
        assertEquals(SpeakingMastery.MASTERED, progress.masteryLevel)
    }

    // ------------------------------------------------------------------
    // Streaks
    // ------------------------------------------------------------------

    @Test
    fun `streak builds over consecutive practice days`() = runTest {
        repository.startSession(PronunciationSessionConfig())
        val today = System.currentTimeMillis()
        val yesterday = today - DAY_MILLIS
        val twoDaysAgo = today - 2 * DAY_MILLIS

        repository.submitAttempt(
            successAttempt("pron_ex_greet_hello", "nǐ hǎo", timestamp = twoDaysAgo)
        )
        repository.submitAttempt(
            successAttempt("pron_ex_greet_hello", "nǐ hǎo", timestamp = yesterday)
        )
        repository.submitAttempt(
            successAttempt("pron_ex_greet_hello", "nǐ hǎo", timestamp = today)
        )

        val stats = repository.getSpeakingStatistics().first()
        assertEquals(3, stats.currentStreak)
        assertEquals(3, stats.longestStreak)
    }

    @Test
    fun `streak resets when practice skipped a day`() = runTest {
        repository.startSession(PronunciationSessionConfig())
        val today = System.currentTimeMillis()
        val threeDaysAgo = today - 3 * DAY_MILLIS

        repository.submitAttempt(
            successAttempt("pron_ex_greet_hello", "nǐ hǎo", timestamp = threeDaysAgo)
        )
        repository.submitAttempt(
            successAttempt("pron_ex_greet_hello", "nǐ hǎo", timestamp = today)
        )

        val stats = repository.getSpeakingStatistics().first()
        assertEquals(1, stats.currentStreak)
    }

    @Test
    fun `streak_3 badge earned after three consecutive days`() = runTest {
        repository.startSession(PronunciationSessionConfig())
        val today = System.currentTimeMillis()
        val yesterday = today - DAY_MILLIS
        val twoDaysAgo = today - 2 * DAY_MILLIS

        repository.submitAttempt(
            successAttempt("pron_ex_greet_hello", "nǐ hǎo", timestamp = twoDaysAgo)
        )
        repository.submitAttempt(
            successAttempt("pron_ex_greet_hello", "nǐ hǎo", timestamp = yesterday)
        )
        val result = repository.submitAttempt(
            successAttempt("pron_ex_greet_hello", "nǐ hǎo", timestamp = today)
        )

        assertTrue(result is Status.ExerciseCompleted)
        assertEquals(3, (result as Status.ExerciseCompleted).result.currentStreak)
        assertTrue((result).result.streakContinued)

        val badges = repository.getPronunciationBadges().first()
        assertTrue(badges.any { it.id == "streak_3" && it.isEarned })
    }

    // ------------------------------------------------------------------
    // Rewards
    // ------------------------------------------------------------------

    @Test
    fun `successful attempt grants xp reward`() = runTest {
        repository.startSession(PronunciationSessionConfig())
        val result = repository.submitAttempt(
            successAttempt("pron_ex_greet_hello", "nǐ hǎo", confidence = 0.9f)
        ) as Status.ExerciseCompleted

        assertEquals(10, result.result.xpEarned)
    }

    @Test
    fun `failed attempt grants no xp`() = runTest {
        repository.startSession(PronunciationSessionConfig())
        val result = repository.submitAttempt(
            failAttempt("pron_ex_greet_hello", "zzz")
        ) as Status.ExerciseCompleted

        assertEquals(0, result.result.xpEarned)
    }

    @Test
    fun `session accumulates xp across attempts`() = runTest {
        val session = repository.startSession(PronunciationSessionConfig())
        repository.submitAttempt(successAttempt("pron_ex_greet_hello", "nǐ hǎo", confidence = 0.9f))
        repository.submitAttempt(successAttempt("pron_ex_greet_thanks", "xiè xie", confidence = 0.9f))

        val completed = repository.completeSession(session) as Status.SessionCompleted
        assertEquals(20, completed.session.totalXpEarned)
    }

    @Test
    fun `friendship bonus is granted for npc exercises`() = runTest {
        repository.startSession(PronunciationSessionConfig())
        val result = repository.submitAttempt(
            successAttempt("pron_ex_greet_hello", "nǐ hǎo", confidence = 0.95f)
        ) as Status.ExerciseCompleted

        assertEquals(2, result.result.friendshipBonusEarned)
        val state = friendshipRepository.getFriendshipState("grandma_mei").first()
        assertNotNull(state)
    }

    @Test
    fun `failed attempts grant no friendship bonus`() = runTest {
        repository.startSession(PronunciationSessionConfig())
        val result = repository.submitAttempt(
            failAttempt("pron_ex_greet_hello", "zzz")
        ) as Status.ExerciseCompleted

        assertEquals(0, result.result.friendshipBonusEarned)
    }

    @Test
    fun `first personal best flagged and later attempts are not`() = runTest {
        repository.startSession(PronunciationSessionConfig())
        val first = repository.submitAttempt(
            successAttempt("pron_ex_greet_hello", "nǐ hǎo", confidence = 0.9f)
        ) as Status.ExerciseCompleted
        assertTrue(first.result.isNewPersonalBest)

        val second = repository.submitAttempt(
            successAttempt("pron_ex_greet_hello", "nǐ hǎo", confidence = 0.8f)
        ) as Status.ExerciseCompleted
        assertFalse(second.result.isNewPersonalBest)
    }

    // ------------------------------------------------------------------
    // Badges
    // ------------------------------------------------------------------

    @Test
    fun `first successful practice earns first_word badge`() = runTest {
        repository.startSession(PronunciationSessionConfig())
        val result = repository.submitAttempt(
            successAttempt("pron_ex_greet_hello", "nǐ hǎo", confidence = 0.9f)
        ) as Status.ExerciseCompleted

        val badges = repository.getPronunciationBadges().first()
        val firstWord = badges.find { it.id == "first_word" }
        assertNotNull(firstWord)
        assertTrue(firstWord?.isEarned == true)
        assertNotNull(firstWord?.earnedAt)
        assertEquals(1f, result.result.badgeProgress["first_word"] ?: 0f, 0.001f)
    }

    @Test
    fun `badges and first practice are recorded in passport`() = runTest {
        repository.startSession(PronunciationSessionConfig())
        repository.submitAttempt(
            successAttempt("pron_ex_greet_hello", "nǐ hǎo", confidence = 0.9f)
        )

        val entries = passportRepository.getRecentEntries(10).first()
        assertTrue(entries.any { it.type == EntryType.ACHIEVEMENT_UNLOCKED })
        assertTrue(entries.any { it.type == EntryType.SPEAKING_PRACTICE })
    }

    // ------------------------------------------------------------------
    // System integration
    // ------------------------------------------------------------------

    @Test
    fun `successful practice increments vocabulary spoken count`() = runTest {
        repository.startSession(PronunciationSessionConfig())
        val before = vocabularyRepository.getWordById("greet_001").first()
        repository.submitAttempt(
            successAttempt("pron_ex_greet_hello", "nǐ hǎo", confidence = 0.9f)
        )

        val after = vocabularyRepository.getWordById("greet_001").first()
        assertEquals((before?.timesSpoken ?: 0) + 1, after?.timesSpoken)
    }

    @Test
    fun `successful practice updates speaking quests`() = runTest {
        repository.startSession(PronunciationSessionConfig())

        questRepository.startQuest("quest_help_grandma_mei")
        questRepository.updateObjectiveProgress("quest_help_grandma_mei", "obj_1_1", 1)
        questRepository.updateObjectiveProgress("quest_help_grandma_mei", "obj_1_2", 5)
        questRepository.updateObjectiveProgress("quest_help_grandma_mei", "obj_1_3", 3)
        questRepository.completeQuest("quest_help_grandma_mei")
        val started = questRepository.startQuest("quest_buy_dumplings")
        assertTrue(started is QuestResult.Success)

        repository.submitAttempt(
            successAttempt("pron_ex_greet_hello", "nǐ hǎo", confidence = 0.9f)
        )

        val quest = questRepository.getQuestById("quest_buy_dumplings").first()!!
        val objective = quest.objectives.find { it.id == "obj_2_3" }
        assertNotNull(objective)
        assertTrue((objective?.currentCount ?: 0) >= 1)
        assertEquals(QuestStatus.ACTIVE, quest.status)
    }

    @Test
    fun `successful practice updates game progress milestone`() = runTest {
        repository.startSession(PronunciationSessionConfig())
        repository.submitAttempt(
            successAttempt("pron_ex_greet_hello", "nǐ hǎo", confidence = 0.9f)
        )

        val gameProgress = gameProgressRepository.getGameProgress().first()
        assertEquals(1, gameProgress.totalSpeakingPractices)
        assertTrue(GameMilestone.FIRST_SPEAKING in gameProgress.milestonesCompleted)
    }

    @Test
    fun `failed attempts do not touch quests or game progress`() = runTest {
        repository.startSession(PronunciationSessionConfig())
        repository.submitAttempt(failAttempt("pron_ex_greet_hello", "zzz"))

        val gameProgress = gameProgressRepository.getGameProgress().first()
        assertEquals(0, gameProgress.totalSpeakingPractices)
        assertFalse(GameMilestone.FIRST_SPEAKING in gameProgress.milestonesCompleted)
    }

    // ------------------------------------------------------------------
    // Offline evaluation
    // ------------------------------------------------------------------

    @Test
    fun `evaluatePronunciationOffline scores a perfect match`() = runTest {
        val attempt = repository.evaluatePronunciationOffline("nǐ hǎo", "nǐ hǎo", "nǐ hǎo")

        assertEquals(1.0f, attempt.confidence, 0.001f)
        assertEquals(PronunciationFeedbackType.EXCELLENT, attempt.feedbackType)
        assertTrue(attempt.wasSuccessful)
    }

    @Test
    fun `evaluatePronunciationOffline scores a partial match`() = runTest {
        val attempt = repository.evaluatePronunciationOffline("nǐ hǎo ma", "nǐ hǎo ma", "nǐ hǎo")

        assertTrue(attempt.wasSuccessful)
        assertTrue(attempt.confidence < 1.0f)
    }

    @Test
    fun `evaluatePronunciationOffline scores a poor match`() = runTest {
        val attempt = repository.evaluatePronunciationOffline("nǐ hǎo", "nǐ hǎo", "zzz")

        assertFalse(attempt.wasSuccessful)
    }

    @Test
    fun `evaluatePronunciationOffline detects improvement`() = runTest {
        repository.evaluatePronunciationOffline("hǎo chī", "hǎo chī", "hào chī")
        val improved = repository.evaluatePronunciationOffline("hǎo chī", "hǎo chī", "hǎo chī")

        assertEquals(PronunciationFeedbackType.NICE_IMPROVEMENT, improved.feedbackType)
    }

    @Test
    fun `evaluatePronunciation with audio path returns an attempt`() = runTest {
        val attempt = repository.evaluatePronunciation("nǐ hǎo", "nǐ hǎo", "/dev/null/audio.wav")

        assertNotNull(attempt)
        assertEquals(1.0f, attempt.confidence, 0.001f)
    }

    // ------------------------------------------------------------------
    // Recorded streak API
    // ------------------------------------------------------------------

    @Test
    fun `recordStreak updates current and longest streak`() = runTest {
        val result = repository.recordStreak(4)

        assertTrue(result is Status.StreakUpdated)
        assertEquals(4, (result as Status.StreakUpdated).currentStreak)

        val stats = repository.getSpeakingStatistics().first()
        assertEquals(4, stats.currentStreak)
        assertEquals(4, stats.longestStreak)
    }

    @Test
    fun `awardBadge awards a badge once`() = runTest {
        val first = repository.awardBadge("tone_master")
        assertTrue(first is Status.BadgeEarned)
        assertTrue((first as Status.BadgeEarned).badge.isEarned)

        val second = repository.awardBadge("tone_master")
        assertTrue(second is Status.Error)
    }

    @Test
    fun `awardBadge rejects unknown badge`() = runTest {
        assertTrue(repository.awardBadge("missing_badge") is Status.Error)
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    private suspend fun successAttempt(
        exerciseId: String,
        spoken: String,
        confidence: Float = 0.9f,
        timestamp: Long = System.currentTimeMillis(),
    ): PronunciationAttempt {
        val exercise = repository.getExerciseById(exerciseId).first()
        return PronunciationAttempt(
            exerciseId = exerciseId,
            wordId = exercise?.wordId,
            phraseId = exercise?.phraseId,
            expectedText = exercise?.expectedText ?: spoken,
            expectedPinyin = exercise?.expectedPinyin ?: spoken,
            spokenText = spoken,
            confidence = confidence,
            toneAccuracy = confidence * 0.9f,
            fluencyScore = confidence * 0.85f,
            wasSuccessful = true,
            timestamp = timestamp,
            durationMs = 2000,
        )
    }

    private suspend fun failAttempt(
        exerciseId: String,
        spoken: String,
        timestamp: Long = System.currentTimeMillis(),
    ): PronunciationAttempt {
        val exercise = repository.getExerciseById(exerciseId).first()
        return PronunciationAttempt(
            exerciseId = exerciseId,
            wordId = exercise?.wordId,
            phraseId = exercise?.phraseId,
            expectedText = exercise?.expectedText ?: spoken,
            expectedPinyin = exercise?.expectedPinyin ?: spoken,
            spokenText = spoken,
            confidence = 0.2f,
            toneAccuracy = 0.1f,
            fluencyScore = 0.1f,
            wasSuccessful = false,
            timestamp = timestamp,
            durationMs = 1200,
        )
    }

    private fun attemptFor(exerciseId: String, spoken: String): PronunciationAttempt =
        PronunciationAttempt(
            exerciseId = exerciseId,
            wordId = null,
            phraseId = null,
            expectedText = spoken,
            expectedPinyin = spoken,
            spokenText = spoken,
        )

    companion object {
        private const val DAY_MILLIS = 24 * 60 * 60 * 1000L
    }
}