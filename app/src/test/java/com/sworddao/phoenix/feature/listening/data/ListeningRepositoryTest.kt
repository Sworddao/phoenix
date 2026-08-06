package com.sworddao.phoenix.feature.listening.data

import com.sworddao.phoenix.feature.friendship.data.MockFriendshipRepository
import com.sworddao.phoenix.feature.gameplay.data.GameMilestone
import com.sworddao.phoenix.feature.gameplay.data.MockGameProgressRepository
import com.sworddao.phoenix.feature.passport.data.EntryType
import com.sworddao.phoenix.feature.passport.data.MockPassportRepository
import com.sworddao.phoenix.feature.pronunciation.data.MockPronunciationRepository
import com.sworddao.phoenix.feature.quest.data.MockQuestRepository
import com.sworddao.phoenix.feature.quest.data.ObjectiveType
import com.sworddao.phoenix.feature.quest.data.QuestResult
import com.sworddao.phoenix.feature.quest.data.QuestStatus
import com.sworddao.phoenix.feature.listening.data.ListeningResultStatus as Status
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

class ListeningRepositoryTest {

    private lateinit var vocabularyRepository: MockVocabularyRepository
    private lateinit var questRepository: MockQuestRepository
    private lateinit var friendshipRepository: MockFriendshipRepository
    private lateinit var gameProgressRepository: MockGameProgressRepository
    private lateinit var passportRepository: MockPassportRepository
    private lateinit var pronunciationRepository: MockPronunciationRepository
    private lateinit var repository: MockListeningRepository

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
        repository = MockListeningRepository(
            vocabularyRepository = vocabularyRepository,
            questRepository = questRepository,
            friendshipRepository = friendshipRepository,
            gameProgressRepository = gameProgressRepository,
            passportRepository = passportRepository,
            pronunciationRepository = pronunciationRepository,
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
        assertNotNull(repository.getExerciseById("listen_ex_greet_hello").first())
        assertNull(repository.getExerciseById("unknown_exercise").first())
    }

    @Test
    fun `getExercisesByType filters exercises`() = runTest {
        val numberExercises = repository.getExercisesByType(
            ListeningExerciseType.HEAR_NUMBERS
        ).first()
        assertTrue(numberExercises.isNotEmpty())
        numberExercises.forEach {
            assertEquals(ListeningExerciseType.HEAR_NUMBERS, it.type)
        }
    }

    @Test
    fun `getExercisesByDifficulty filters exercises`() = runTest {
        val beginner = repository.getExercisesByDifficulty(
            ListeningDifficulty.BEGINNER
        ).first()
        assertTrue(beginner.isNotEmpty())
        beginner.forEach { assertEquals(ListeningDifficulty.BEGINNER, it.difficulty) }
    }

    @Test
    fun `getExercisesByWord returns mapped exercises`() = runTest {
        val exercises = repository.getExercisesByWord("greet_001").first()
        assertTrue(exercises.isNotEmpty())
        exercises.forEach {
            assertTrue(it.relatedWordId == "greet_001" || it.clip.wordId == "greet_001")
        }
    }

    @Test
    fun `getExercisesByNpc returns exercises for an npc`() = runTest {
        val exercises = repository.getExercisesByNpc("grandma_mei").first()
        assertTrue(exercises.isNotEmpty())
        exercises.forEach {
            assertTrue(it.relatedNpcId == "grandma_mei" || it.clip.npcId == "grandma_mei")
        }
    }

    @Test
    fun `getUnlockedExercises excludes locked`() = runTest {
        val before = repository.getUnlockedExercises().first()
        assertTrue(before.none { it.id == "listen_ex_mei_greeting" })

        repository.unlockExercise("listen_ex_mei_greeting")

        val after = repository.getUnlockedExercises().first()
        assertTrue(after.any { it.id == "listen_ex_mei_greeting" })
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
    fun `addExercises appends new and skips duplicates`() = runTest {
        val result = repository.addExercises(
            listOf(
                ListeningExercise(
                    id = "custom_listen_ex_1",
                    type = ListeningExerciseType.HEAR_GREETINGS,
                    difficulty = ListeningDifficulty.ADVANCED,
                    clip = AudioClip(
                        id = "clip_custom_1",
                        text = "zǎo shang hǎo",
                        hanzi = "早上好",
                        english = "Good morning",
                    ),
                    prompt = "听一听，选出正确答案",
                    choices = listOf(
                        ListeningChoice("choice_0", "Good morning", "zǎo shang hǎo"),
                        ListeningChoice("choice_1", "Good night", "wǎn ān"),
                    ),
                    correctChoiceIndex = 0,
                ),
                ListeningExercise(
                    id = "listen_ex_greet_hello",
                    type = ListeningExerciseType.HEAR_AND_IDENTIFY_VOCABULARY,
                    difficulty = ListeningDifficulty.BEGINNER,
                    clip = AudioClip(
                        id = "clip_dup",
                        text = "nǐ hǎo",
                        hanzi = "你好",
                        english = "Hello",
                    ),
                    prompt = "重复",
                    choices = listOf(ListeningChoice("choice_0", "nǐ hǎo")),
                ),
            )
        )
        assertTrue(result is Status.Success)

        val ids = repository.getAllExercises().first().map { it.id }
        assertEquals(1, ids.count { it == "custom_listen_ex_1" })
        assertEquals(1, ids.count { it == "listen_ex_greet_hello" })
    }

    // ------------------------------------------------------------------
    // Session lifecycle
    // ------------------------------------------------------------------

    @Test
    fun `startSession returns a session with selected exercises`() = runTest {
        val config = ListeningSessionConfig(
            exerciseType = ListeningExerciseType.HEAR_NUMBERS,
            difficulty = ListeningDifficulty.BEGINNER,
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
            ListeningSessionConfig(
                wordIds = listOf("brand_new_word"),
                exerciseCount = 1,
            )
        )

        assertEquals(1, session.exerciseIds.size)
        val exercise = repository.getExerciseById(session.exerciseIds.first()).first()
        assertNotNull(exercise)
        assertEquals("brand_new_word", exercise?.relatedWordId)
    }

    @Test
    fun `completeSession marks session complete and returns statistics`() = runTest {
        val session = repository.startSession(ListeningSessionConfig(exerciseCount = 2))

        val result = repository.completeSession(session)

        assertTrue(result is Status.SessionCompleted)
        val completed = (result as Status.SessionCompleted).session
        assertTrue(completed.isCompleted)
        assertNotNull(completed.completedAt)

        val stats = repository.getListeningStatistics().first()
        assertEquals(1, stats.totalSessions)
    }

    @Test
    fun `completeSession without active session returns error`() = runTest {
        val result = repository.completeSession(ListeningSession(exerciseIds = listOf("a")))
        assertTrue(result is Status.Error)
    }

    @Test
    fun `completeSession with mismatched id returns error`() = runTest {
        val session = repository.startSession(ListeningSessionConfig())
        val result = repository.completeSession(session.copy(id = "different_id"))
        assertTrue(result is Status.Error)
    }

    @Test
    fun `submitAnswer without active session returns error`() = runTest {
        val result = repository.submitAnswer(attemptFor("listen_ex_greet_hello"))
        assertTrue(result is Status.Error)
    }

    @Test
    fun `submitAnswer with unknown exercise returns error`() = runTest {
        repository.startSession(ListeningSessionConfig())
        val result = repository.submitAnswer(attemptFor("unknown_exercise"))
        assertTrue(result is Status.Error)
    }

    // ------------------------------------------------------------------
    // Attempts, progress, statistics
    // ------------------------------------------------------------------

    @Test
    fun `successful answer updates progress for the word`() = runTest {
        repository.startSession(ListeningSessionConfig())
        val result = repository.submitAnswer(successAttempt("listen_ex_greet_hello"))

        assertTrue(result is Status.ExerciseCompleted)
        val progress = repository.getListeningProgress("greet_001").first()
        assertNotNull(progress)
        assertEquals(1, progress?.totalAttempts)
        assertEquals(1, progress?.correctAttempts)
        assertTrue((progress?.bestTimeMs ?: 0) > 0)
    }

    @Test
    fun `failed answer updates attempt counts but not correct`() = runTest {
        repository.startSession(ListeningSessionConfig())
        repository.submitAnswer(failAttempt("listen_ex_greet_hello"))

        val progress = repository.getListeningProgress("greet_001").first()
        assertEquals(1, progress?.totalAttempts)
        assertEquals(0, progress?.correctAttempts)
    }

    @Test
    fun `statistics accumulate across attempts`() = runTest {
        repository.startSession(ListeningSessionConfig())
        repository.submitAnswer(successAttempt("listen_ex_greet_hello"))
        repository.submitAnswer(failAttempt("listen_ex_greet_hello"))
        repository.submitAnswer(successAttempt("listen_ex_greet_thanks"))

        val stats = repository.getListeningStatistics().first()
        assertEquals(3, stats.totalAttempts)
        assertEquals(2, stats.correctAttempts)
        assertTrue(stats.exercisesByType.isNotEmpty())
        assertTrue(stats.exercisesByDifficulty.isNotEmpty())
    }

    @Test
    fun `all listening progress is observable`() = runTest {
        repository.startSession(ListeningSessionConfig())
        repository.submitAnswer(successAttempt("listen_ex_greet_hello"))

        val all = repository.getAllListeningProgress().first()
        assertEquals(1, all.size)
        assertEquals("greet_001", all.first().wordId)
    }

    @Test
    fun `updateProgress recomputes mastery`() = runTest {
        val result = repository.updateProgress(
            ListeningProgress(
                itemId = "greet_001",
                wordId = "greet_001",
                totalAttempts = 20,
                correctAttempts = 15,
            )
        )

        assertTrue(result is Status.ProgressUpdated)
        val progress = (result as Status.ProgressUpdated).progress
        assertEquals(ListeningMastery.CONFIDENT, progress.masteryLevel)
    }

    @Test
    fun `perfect answers upgrade to mastered mastery`() = runTest {
        val result = repository.updateProgress(
            ListeningProgress(
                itemId = "greet_001",
                wordId = "greet_001",
                totalAttempts = 20,
                correctAttempts = 20,
            )
        )

        val progress = (result as Status.ProgressUpdated).progress
        assertEquals(ListeningMastery.MASTERED, progress.masteryLevel)
    }

    // ------------------------------------------------------------------
    // Streaks
    // ------------------------------------------------------------------

    @Test
    fun `streak builds over consecutive practice days`() = runTest {
        repository.startSession(ListeningSessionConfig())
        val today = System.currentTimeMillis()
        val yesterday = today - DAY_MILLIS
        val twoDaysAgo = today - 2 * DAY_MILLIS

        repository.submitAnswer(successAttempt("listen_ex_greet_hello", timestamp = twoDaysAgo))
        repository.submitAnswer(successAttempt("listen_ex_greet_hello", timestamp = yesterday))
        repository.submitAnswer(successAttempt("listen_ex_greet_hello", timestamp = today))

        val stats = repository.getListeningStatistics().first()
        assertEquals(3, stats.currentStreak)
        assertEquals(3, stats.longestStreak)
    }

    @Test
    fun `streak resets when practice skipped a day`() = runTest {
        repository.startSession(ListeningSessionConfig())
        val today = System.currentTimeMillis()
        val threeDaysAgo = today - 3 * DAY_MILLIS

        repository.submitAnswer(successAttempt("listen_ex_greet_hello", timestamp = threeDaysAgo))
        repository.submitAnswer(successAttempt("listen_ex_greet_hello", timestamp = today))

        val stats = repository.getListeningStatistics().first()
        assertEquals(1, stats.currentStreak)
    }

    @Test
    fun `listen_streak_3 badge earned after three consecutive days`() = runTest {
        repository.startSession(ListeningSessionConfig())
        val today = System.currentTimeMillis()
        val yesterday = today - DAY_MILLIS
        val twoDaysAgo = today - 2 * DAY_MILLIS

        repository.submitAnswer(successAttempt("listen_ex_greet_hello", timestamp = twoDaysAgo))
        repository.submitAnswer(successAttempt("listen_ex_greet_hello", timestamp = yesterday))
        val result = repository.submitAnswer(
            successAttempt("listen_ex_greet_hello", timestamp = today)
        )

        assertTrue(result is Status.ExerciseCompleted)
        assertEquals(3, (result as Status.ExerciseCompleted).result.currentStreak)
        assertTrue((result).result.streakContinued)

        val badges = repository.getListeningBadges().first()
        assertTrue(badges.any { it.id == "listen_streak_3" && it.isEarned })
    }

    // ------------------------------------------------------------------
    // Rewards
    // ------------------------------------------------------------------

    @Test
    fun `successful attempt grants xp reward`() = runTest {
        repository.startSession(ListeningSessionConfig())
        val result = repository.submitAnswer(
            successAttempt("listen_ex_greet_hello")
        ) as Status.ExerciseCompleted

        assertEquals(10, result.result.xpEarned)
    }

    @Test
    fun `failed attempt grants no xp`() = runTest {
        repository.startSession(ListeningSessionConfig())
        val result = repository.submitAnswer(
            failAttempt("listen_ex_greet_hello")
        ) as Status.ExerciseCompleted

        assertEquals(0, result.result.xpEarned)
    }

    @Test
    fun `session accumulates xp across attempts`() = runTest {
        val session = repository.startSession(ListeningSessionConfig())
        repository.submitAnswer(successAttempt("listen_ex_greet_hello"))
        repository.submitAnswer(successAttempt("listen_ex_greet_thanks"))

        val completed = repository.completeSession(session) as Status.SessionCompleted
        assertEquals(20, completed.session.totalXpEarned)
    }

    @Test
    fun `friendship bonus is granted for npc exercises`() = runTest {
        repository.startSession(ListeningSessionConfig())
        val result = repository.submitAnswer(
            successAttempt("listen_ex_greet_hello")
        ) as Status.ExerciseCompleted

        assertEquals(2, result.result.friendshipBonusEarned)
        val state = friendshipRepository.getFriendshipState("grandma_mei").first()
        assertNotNull(state)
    }

    @Test
    fun `failed attempts grant no friendship bonus`() = runTest {
        repository.startSession(ListeningSessionConfig())
        val result = repository.submitAnswer(
            failAttempt("listen_ex_greet_hello")
        ) as Status.ExerciseCompleted

        assertEquals(0, result.result.friendshipBonusEarned)
    }

    @Test
    fun `first personal best flagged and slower later attempts are not`() = runTest {
        repository.startSession(ListeningSessionConfig())
        val first = repository.submitAnswer(
            successAttempt("listen_ex_greet_hello")
        ) as Status.ExerciseCompleted
        assertTrue(first.result.isNewPersonalBest)

        val second = repository.submitAnswer(
            successAttempt("listen_ex_greet_hello", timeTakenMs = 5000)
        ) as Status.ExerciseCompleted
        assertFalse(second.result.isNewPersonalBest)
    }

    @Test
    fun `faster later attempt flags a new personal best`() = runTest {
        repository.startSession(ListeningSessionConfig())
        repository.submitAnswer(successAttempt("listen_ex_greet_hello", timeTakenMs = 5000))

        val second = repository.submitAnswer(
            successAttempt("listen_ex_greet_hello", timeTakenMs = 1000)
        ) as Status.ExerciseCompleted
        assertTrue(second.result.isNewPersonalBest)
    }

    // ------------------------------------------------------------------
    // Badges
    // ------------------------------------------------------------------

    @Test
    fun `first successful answer earns listen_first badge`() = runTest {
        repository.startSession(ListeningSessionConfig())
        val result = repository.submitAnswer(
            successAttempt("listen_ex_greet_hello")
        ) as Status.ExerciseCompleted

        val badges = repository.getListeningBadges().first()
        val first = badges.find { it.id == "listen_first" }
        assertNotNull(first)
        assertTrue(first?.isEarned == true)
        assertNotNull(first?.earnedAt)
        assertEquals(1f, result.result.badgeProgress["listen_first"] ?: 0f, 0.001f)
    }

    @Test
    fun `badges and first practice are recorded in passport`() = runTest {
        repository.startSession(ListeningSessionConfig())
        repository.submitAnswer(successAttempt("listen_ex_greet_hello"))

        val entries = passportRepository.getRecentEntries(10).first()
        assertTrue(entries.any { it.type == EntryType.ACHIEVEMENT_UNLOCKED })
        assertTrue(entries.any { it.type == EntryType.LISTENING_PRACTICE })
    }

    @Test
    fun `replay count is tracked in stats and progress`() = runTest {
        repository.startSession(ListeningSessionConfig())
        val replay = repository.recordReplay("listen_ex_greet_hello") as Status.ReplayRecorded
        assertEquals(1, replay.replayCount)

        val stats = repository.getListeningStatistics().first()
        assertEquals(1, stats.totalReplayCount)

        repository.submitAnswer(
            successAttempt("listen_ex_greet_hello", replayCount = 1)
        )
        val progress = repository.getListeningProgress("greet_001").first()
        assertEquals(1, progress?.replayCount)
    }

    @Test
    fun `recordReplay errors for unknown exercise`() = runTest {
        assertTrue(repository.recordReplay("missing") is Status.Error)
    }

    // ------------------------------------------------------------------
    // System integration
    // ------------------------------------------------------------------

    @Test
    fun `successful answer increments vocabulary heard count`() = runTest {
        repository.startSession(ListeningSessionConfig())
        val before = vocabularyRepository.getWordById("greet_001").first()
        repository.submitAnswer(successAttempt("listen_ex_greet_hello"))

        val after = vocabularyRepository.getWordById("greet_001").first()
        assertEquals((before?.timesHeard ?: 0) + 1, after?.timesHeard)
    }

    @Test
    fun `successful answer unlocks related speaking exercise`() = runTest {
        repository.startSession(ListeningSessionConfig())

        val exercise = repository.getExerciseById("listen_ex_greet_hello").first()!!
        val speakingId = exercise.relatedSpeakingExerciseId
        assertNotNull(speakingId)

        val unlockedBefore = pronunciationRepository.getUnlockedExercises().first()
            .any { it.id == speakingId }

        repository.submitAnswer(successAttempt("listen_ex_greet_hello"))

        val unlockedAfter = pronunciationRepository.getUnlockedExercises().first()
            .any { it.id == speakingId }
        assertTrue(unlockedAfter || unlockedBefore)
    }

    @Test
    fun `successful answer updates listening quests`() = runTest {
        completeQuestChain()

        val started = questRepository.startQuest("quest_order_tea")
        assertTrue(started is QuestResult.Success)

        repository.startSession(ListeningSessionConfig())
        repository.submitAnswer(successAttempt("listen_ex_greet_hello"))
        repository.submitAnswer(successAttempt("listen_ex_greet_thanks"))

        val quest = questRepository.getAllQuests().first().find { it.id == "quest_order_tea" }
        assertNotNull(quest)
        assertEquals(QuestStatus.ACTIVE, quest?.status)
        val objective = quest?.objectives?.find { it.id == "obj_3_4" }
        assertNotNull(objective)
        assertEquals(ObjectiveType.LISTEN_TO_AUDIO, objective?.type)
        assertTrue((objective?.currentCount ?: 0) >= 1)
    }

    @Test
    fun `successful answer updates game progress milestone`() = runTest {
        repository.startSession(ListeningSessionConfig())
        repository.submitAnswer(successAttempt("listen_ex_greet_hello"))

        val gameProgress = gameProgressRepository.getGameProgress().first()
        assertEquals(1, gameProgress.totalListeningPractices)
        assertTrue(GameMilestone.FIRST_LISTENING in gameProgress.milestonesCompleted)
    }

    @Test
    fun `failed attempts do not touch quests or game progress`() = runTest {
        repository.startSession(ListeningSessionConfig())
        repository.submitAnswer(failAttempt("listen_ex_greet_hello"))

        val gameProgress = gameProgressRepository.getGameProgress().first()
        assertEquals(0, gameProgress.totalListeningPractices)
        assertFalse(GameMilestone.FIRST_LISTENING in gameProgress.milestonesCompleted)
    }

    // ------------------------------------------------------------------
    // Recorded streak and badge API
    // ------------------------------------------------------------------

    @Test
    fun `recordStreak updates current and longest streak`() = runTest {
        val result = repository.recordStreak(4)

        assertTrue(result is Status.StreakUpdated)
        assertEquals(4, (result as Status.StreakUpdated).currentStreak)

        val stats = repository.getListeningStatistics().first()
        assertEquals(4, stats.currentStreak)
        assertEquals(4, stats.longestStreak)
    }

    @Test
    fun `awardBadge awards a badge once`() = runTest {
        val first = repository.awardBadge("listen_quick_ear")
        assertTrue(first is Status.BadgeEarned)
        assertTrue((first as Status.BadgeEarned).badge.isEarned)

        val second = repository.awardBadge("listen_quick_ear")
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
        timestamp: Long = System.currentTimeMillis(),
        timeTakenMs: Long = 2000,
        replayCount: Int = 0,
    ): ListeningAttempt {
        val exercise = repository.getExerciseById(exerciseId).first()
        return ListeningAttempt(
            exerciseId = exerciseId,
            wordId = exercise?.relatedWordId ?: exercise?.clip?.wordId,
            chosenChoiceId = exercise?.correctChoice?.id ?: "choice_0",
            wasCorrect = true,
            replayCount = replayCount,
            timeTakenMs = timeTakenMs,
            timestamp = timestamp,
        )
    }

    private suspend fun failAttempt(
        exerciseId: String,
        timestamp: Long = System.currentTimeMillis(),
    ): ListeningAttempt {
        val exercise = repository.getExerciseById(exerciseId).first()
        val wrongChoice = exercise?.choices?.firstOrNull { choice ->
            exercise.choices.indexOf(choice) != exercise.correctChoiceIndex
        }
        return ListeningAttempt(
            exerciseId = exerciseId,
            wordId = exercise?.relatedWordId ?: exercise?.clip?.wordId,
            chosenChoiceId = wrongChoice?.id ?: "choice_1",
            wasCorrect = false,
            replayCount = 0,
            timeTakenMs = 3000,
            timestamp = timestamp,
        )
    }

    private fun attemptFor(exerciseId: String): ListeningAttempt =
        ListeningAttempt(
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