package com.sworddao.phoenix.feature.dialogue

import androidx.lifecycle.SavedStateHandle
import com.sworddao.phoenix.feature.dialogue.data.ActionType
import com.sworddao.phoenix.feature.dialogue.data.ConversationPhase
import com.sworddao.phoenix.feature.dialogue.data.Dialogue
import com.sworddao.phoenix.feature.dialogue.data.DialogueAction
import com.sworddao.phoenix.feature.dialogue.data.DialogueHistoryEntry
import com.sworddao.phoenix.feature.dialogue.data.DialogueNode
import com.sworddao.phoenix.feature.dialogue.data.DialogueNodeType
import com.sworddao.phoenix.feature.dialogue.data.DialogueResult
import com.sworddao.phoenix.feature.dialogue.data.Speaker
import com.sworddao.phoenix.feature.dialogue.domain.DialogueRepository
import com.sworddao.phoenix.feature.dialogue.viewmodel.DialogueViewModel
import com.sworddao.phoenix.feature.dialogue.viewmodel.ProcessedAction
import com.sworddao.phoenix.feature.friendship.domain.FriendshipRepository
import com.sworddao.phoenix.feature.friendship.data.FriendshipState
import com.sworddao.phoenix.feature.gameplay.data.DialogueResultHolder
import com.sworddao.phoenix.feature.quest.domain.QuestRepository
import com.sworddao.phoenix.feature.quest.data.QuestResult
import com.sworddao.phoenix.feature.vocabulary.data.VocabularyResult
import com.sworddao.phoenix.feature.vocabulary.domain.VocabularyRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DialogueViewModelActionTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var viewModel: DialogueViewModel
    private lateinit var mockDialogueRepository: FakeDialogueRepository
    private lateinit var mockFriendshipRepository: FakeFriendshipRepository
    private lateinit var mockQuestRepository: FakeQuestRepository
    private lateinit var mockVocabularyRepository: FakeVocabularyRepository
    private lateinit var dialogueResultHolder: DialogueResultHolder

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        mockDialogueRepository = FakeDialogueRepository()
        mockFriendshipRepository = FakeFriendshipRepository()
        mockQuestRepository = FakeQuestRepository()
        mockVocabularyRepository = FakeVocabularyRepository()
        dialogueResultHolder = DialogueResultHolder()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private suspend fun waitForDialogueLoaded(viewModel: DialogueViewModel) {
        viewModel.uiState.first { it.dialogue != null }
    }

    private suspend fun waitForConversationComplete(viewModel: DialogueViewModel) {
        viewModel.uiState.first { it.isConversationComplete }
    }

    private fun createViewModel(dialogueId: String = "npc_1"): DialogueViewModel {
        val savedStateHandle = SavedStateHandle(mapOf("dialogueId" to dialogueId))
        return DialogueViewModel(
            savedStateHandle = savedStateHandle,
            dialogueRepository = mockDialogueRepository,
            friendshipRepository = mockFriendshipRepository,
            questRepository = mockQuestRepository,
            vocabularyRepository = mockVocabularyRepository,
            dialogueResultHolder = dialogueResultHolder
        )
    }

    private fun setupAndCreateViewModel(
        actions: List<DialogueAction> = emptyList(),
        dialogueId: String = "npc_1"
): DialogueViewModel {
        mockDialogueRepository.completeWithActions(actions)
        return createViewModel(dialogueId)
    }

    @Test
    fun `processActions handles ADD_FRIENDSHIP_XP successfully`() = runTest {
        val actions = listOf(
            DialogueAction(
                type = ActionType.ADD_FRIENDSHIP_XP,
                targetId = "npc_1",
                value = "25"
            )
        )

        val viewModel = setupAndCreateViewModel(actions)
        waitForDialogueLoaded(viewModel)
        viewModel.advanceDialogue()
        waitForConversationComplete(viewModel)

        val state = viewModel.uiState.value
        assertTrue(state.isConversationComplete)
        assertEquals(1, state.processedActions.size)
        assertTrue(state.processedActions[0].success)
        assertEquals(ActionType.ADD_FRIENDSHIP_XP, state.processedActions[0].type)
        assertEquals(25, mockFriendshipRepository.lastXpAdded)
    }

    @Test
    fun `processActions handles UNLOCK_VOCABULARY successfully`() = runTest {
        val actions = listOf(
            DialogueAction(
                type = ActionType.UNLOCK_VOCABULARY,
                targetId = "vocab_category",
                value = "word1,word2"
            )
        )

        val viewModel = setupAndCreateViewModel(actions)
        waitForDialogueLoaded(viewModel)
        viewModel.advanceDialogue()
        waitForConversationComplete(viewModel)

        val state = viewModel.uiState.value
        assertTrue(state.isConversationComplete)
        assertEquals(1, state.processedActions.size)
        assertTrue(state.processedActions[0].success)
        assertEquals(ActionType.UNLOCK_VOCABULARY, state.processedActions[0].type)
        assertTrue(mockVocabularyRepository.discoveredWords.contains("word1"))
        assertTrue(mockVocabularyRepository.discoveredWords.contains("word2"))
    }

    @Test
    fun `processActions handles COMPLETE_QUEST successfully`() = runTest {
        val actions = listOf(
            DialogueAction(
                type = ActionType.COMPLETE_QUEST,
                targetId = "quest_1",
                value = ""
            )
        )

        val viewModel = setupAndCreateViewModel(actions)
        waitForDialogueLoaded(viewModel)
        viewModel.advanceDialogue()
        waitForConversationComplete(viewModel)

        val state = viewModel.uiState.value
        assertTrue(state.isConversationComplete)
        assertEquals(1, state.processedActions.size)
        assertTrue(state.processedActions[0].success)
        assertEquals(ActionType.COMPLETE_QUEST, state.processedActions[0].type)
        assertEquals("quest_1", mockQuestRepository.completedQuestId)
    }

    @Test
    fun `processActions handles GIVE_ITEM successfully`() = runTest {
        val actions = listOf(
            DialogueAction(
                type = ActionType.GIVE_ITEM,
                targetId = "item_1",
                value = "1"
            )
        )

        val viewModel = setupAndCreateViewModel(actions)
        waitForDialogueLoaded(viewModel)
        viewModel.advanceDialogue()
        waitForConversationComplete(viewModel)

        val state = viewModel.uiState.value
        assertTrue(state.isConversationComplete)
        assertEquals(1, state.processedActions.size)
        assertTrue(state.processedActions[0].success)
        assertEquals(ActionType.GIVE_ITEM, state.processedActions[0].type)
    }

    @Test
    fun `processActions handles multiple actions`() = runTest {
        val actions = listOf(
            DialogueAction(
                type = ActionType.ADD_FRIENDSHIP_XP,
                targetId = "npc_1",
                value = "10"
            ),
            DialogueAction(
                type = ActionType.UNLOCK_VOCABULARY,
                targetId = "vocab_category",
                value = "word1"
            ),
            DialogueAction(
                type = ActionType.COMPLETE_QUEST,
                targetId = "quest_1",
                value = ""
            )
        )

        val viewModel = setupAndCreateViewModel(actions)
        waitForDialogueLoaded(viewModel)
        viewModel.advanceDialogue()
        waitForConversationComplete(viewModel)

        val state = viewModel.uiState.value
        assertTrue(state.isConversationComplete)
        assertEquals(3, state.processedActions.size)
        assertTrue(state.processedActions.all { it.success })
    }

    @Test
    fun `processActions handles empty actions`() = runTest {
        val actions = emptyList<DialogueAction>()

        val viewModel = setupAndCreateViewModel(actions)
        waitForDialogueLoaded(viewModel)
        viewModel.advanceDialogue()
        waitForConversationComplete(viewModel)

        val state = viewModel.uiState.value
        assertTrue(state.isConversationComplete)
        assertTrue(state.processedActions.isEmpty())
        assertFalse(state.isProcessingActions)
    }

    @Test
    fun `processActions handles invalid XP value`() = runTest {
        val actions = listOf(
            DialogueAction(
                type = ActionType.ADD_FRIENDSHIP_XP,
                targetId = "npc_1",
                value = "invalid"
            )
        )

        val viewModel = setupAndCreateViewModel(actions)
        waitForDialogueLoaded(viewModel)
        viewModel.advanceDialogue()
        waitForConversationComplete(viewModel)

        val state = viewModel.uiState.value
        assertTrue(state.isConversationComplete)
        assertEquals(1, state.processedActions.size)
        assertFalse(state.processedActions[0].success)
    }

    @Test
    fun `dialogueResultHolder stores results correctly`() = runTest {
        val actions = listOf(
            DialogueAction(
                type = ActionType.ADD_FRIENDSHIP_XP,
                targetId = "npc_1",
                value = "30"
            )
        )

        val viewModel = setupAndCreateViewModel(actions)
        waitForDialogueLoaded(viewModel)
        viewModel.advanceDialogue()
        waitForConversationComplete(viewModel)

        assertEquals("npc_1", dialogueResultHolder.lastDialogueId)
        assertEquals("npc_1", dialogueResultHolder.lastNpcId)
        assertEquals(1, dialogueResultHolder.lastProcessedActions.size)
        assertEquals(30, dialogueResultHolder.lastXpEarned)
    }

    @Test
    fun `dialogueResultHolder clears correctly`() {
        dialogueResultHolder.storeResults(
            dialogueId = "test",
            npcId = "npc_1",
            processedActions = listOf(
                ProcessedAction(
                    type = ActionType.ADD_FRIENDSHIP_XP,
                    targetId = "npc_1",
                    value = "10",
                    success = true
                )
            )
        )

        dialogueResultHolder.clear()

        assertEquals("", dialogueResultHolder.lastDialogueId)
        assertEquals("", dialogueResultHolder.lastNpcId)
        assertTrue(dialogueResultHolder.lastProcessedActions.isEmpty())
        assertEquals(0, dialogueResultHolder.lastXpEarned)
    }

    @Test
    fun `selectChoice triggers action processing`() = runTest {
        val actions = listOf(
            DialogueAction(
                type = ActionType.ADD_FRIENDSHIP_XP,
                targetId = "npc_1",
                value = "15"
            )
        )

        mockDialogueRepository.choiceCompletesWithActions("choice_1", actions)
        val viewModel = createViewModel("npc_1")
        waitForDialogueLoaded(viewModel)

        viewModel.selectChoice("choice_1")
        waitForConversationComplete(viewModel)

        val state = viewModel.uiState.value
        assertTrue(state.isConversationComplete)
        assertEquals(1, state.processedActions.size)
        assertTrue(state.processedActions[0].success)
    }
}

private class FakeDialogueRepository : DialogueRepository {
    private val dialogues = MutableStateFlow<List<Dialogue>>(emptyList())
    private var pendingActions: List<DialogueAction> = emptyList()
    private var choiceActions: Map<String, List<DialogueAction>> = emptyMap()

    fun setupDialogue() {
        dialogues.value = listOf(
            Dialogue(
                id = "test_dialogue",
                npcId = "npc_1",
                title = "Test Dialogue",
                description = "A test dialogue",
                startNodeId = "start",
                nodes = listOf(
                    DialogueNode(
                        id = "start",
                        type = DialogueNodeType.NPC_SPEAKS,
                        speaker = Speaker.NPC,
                        speakerName = "Test NPC",
                        text = "Hello!",
                        nextNodeId = "end"
                    ),
                    DialogueNode(
                        id = "end",
                        type = DialogueNodeType.CONVERSATION_END,
                        speaker = Speaker.NARRATOR,
                        text = "Conversation ended"
                    )
                )
            )
        )
    }

    fun completeWithActions(actions: List<DialogueAction>) {
        pendingActions = actions
        setupDialogue()
    }

    fun choiceCompletesWithActions(choiceId: String, actions: List<DialogueAction>) {
        choiceActions = mapOf(choiceId to actions)
        dialogues.value = listOf(
            Dialogue(
                id = "test_dialogue",
                npcId = "npc_1",
                title = "Test Dialogue",
                description = "A test dialogue",
                startNodeId = "start",
                nodes = listOf(
                    DialogueNode(
                        id = "start",
                        type = DialogueNodeType.PLAYER_CHOOSES,
                        speaker = Speaker.PLAYER,
                        text = "",
                        choices = listOf(
                            com.sworddao.phoenix.feature.dialogue.data.DialogueChoice(
                                id = choiceId,
                                text = "Choice",
                                nextNodeId = "end",
                                actions = actions
                            )
                        )
                    ),
                    DialogueNode(
                        id = "end",
                        type = DialogueNodeType.CONVERSATION_END,
                        speaker = Speaker.NARRATOR,
                        text = "Conversation ended"
                    )
                )
            )
        )
    }

    override fun getDialogueByNpcId(npcId: String): Flow<Dialogue?> {
        return dialogues.map { list -> list.firstOrNull { it.npcId == npcId } }
    }

    override fun getAllDialogues(): Flow<List<Dialogue>> = dialogues

    override suspend fun startConversation(dialogueId: String): DialogueResult {
        val dialogue = dialogues.value.firstOrNull { it.id == dialogueId }
            ?: return DialogueResult.Error("Dialogue not found")
        return DialogueResult.NodeLoaded(
            node = dialogue.nodes.first(),
            history = listOf(
                DialogueHistoryEntry(
                    speaker = Speaker.NPC,
                    speakerName = "Test NPC",
                    text = "Hello!"
                )
            ),
            choices = emptyList()
        )
    }

    override suspend fun selectChoice(dialogueId: String, choiceId: String): DialogueResult {
        val dialogue = dialogues.value.firstOrNull { it.id == dialogueId }
            ?: return DialogueResult.Error("Dialogue not found")

        val endNode = dialogue.nodes.lastOrNull { it.type == DialogueNodeType.CONVERSATION_END }
            ?: return DialogueResult.Error("No end node")

        return DialogueResult.ConversationEnded(
            actions = choiceActions[choiceId] ?: emptyList(),
            history = listOf(
                DialogueHistoryEntry(
                    speaker = Speaker.NARRATOR,
                    speakerName = "",
                    text = "Conversation ended"
                )
            )
        )
    }

    override suspend fun advanceDialogue(dialogueId: String): DialogueResult {
        val dialogue = dialogues.value.firstOrNull { it.id == dialogueId }
            ?: return DialogueResult.Error("Dialogue not found")

        val endNode = dialogue.nodes.lastOrNull { it.type == DialogueNodeType.CONVERSATION_END }
            ?: return DialogueResult.Error("No end node")

        return DialogueResult.ConversationEnded(
            actions = pendingActions,
            history = listOf(
                DialogueHistoryEntry(
                    speaker = Speaker.NARRATOR,
                    speakerName = "",
                    text = "Conversation ended"
                )
            )
        )
    }
}

private class FakeFriendshipRepository : FriendshipRepository {
    var lastXpAdded: Int = 0
        private set

    override fun getFriendshipState(npcId: String): Flow<FriendshipState?> {
        return flowOf(FriendshipState(npcId = npcId))
    }

    override fun getAllFriendshipStates(): Flow<List<FriendshipState>> = flowOf(emptyList())

    override suspend fun addFriendshipXp(npcId: String, xpAmount: Int): FriendshipState {
        lastXpAdded = xpAmount
        return FriendshipState(npcId = npcId)
    }

    override suspend fun recordConversation(
        npcId: String,
        dialogueId: String,
        dialogueTitle: String,
        xpGained: Int,
        topicsDiscussed: List<String>,
        choicesSummary: List<String>
    ) {}

    override fun getConversationHistory(npcId: String) = flowOf(emptyList<com.sworddao.phoenix.feature.friendship.data.ConversationMemory>())

    override fun getFriendshipEvents(npcId: String) = flowOf(emptyList<com.sworddao.phoenix.feature.friendship.data.FriendshipEvent>())

    override suspend fun unlockTopic(npcId: String, topic: String) {}

    override suspend fun initializeFriendship(npcId: String) {}
}

private class FakeQuestRepository : QuestRepository {
    var completedQuestId: String = ""
        private set

    private val emptyQuestList: List<com.sworddao.phoenix.feature.quest.data.Quest> = emptyList()

    override fun getAllQuests(): Flow<List<com.sworddao.phoenix.feature.quest.data.Quest>> = flowOf(emptyQuestList)
    override fun getQuestById(questId: String): Flow<com.sworddao.phoenix.feature.quest.data.Quest?> = flowOf(null)
    override fun getQuestsByType(type: com.sworddao.phoenix.feature.quest.data.QuestType): Flow<List<com.sworddao.phoenix.feature.quest.data.Quest>> = flowOf(emptyQuestList)
    override fun getQuestsByDifficulty(difficulty: com.sworddao.phoenix.feature.quest.data.QuestDifficulty): Flow<List<com.sworddao.phoenix.feature.quest.data.Quest>> = flowOf(emptyQuestList)
    override fun getQuestsByCategory(category: com.sworddao.phoenix.feature.quest.data.QuestCategory): Flow<List<com.sworddao.phoenix.feature.quest.data.Quest>> = flowOf(emptyQuestList)
    override fun getQuestsByFilter(filter: com.sworddao.phoenix.feature.quest.data.QuestFilter): Flow<List<com.sworddao.phoenix.feature.quest.data.Quest>> = flowOf(emptyQuestList)
    override fun getActiveQuests(): Flow<List<com.sworddao.phoenix.feature.quest.data.Quest>> = flowOf(emptyQuestList)
    override fun getCompletedQuests(): Flow<List<com.sworddao.phoenix.feature.quest.data.Quest>> = flowOf(emptyQuestList)
    override fun getAvailableQuests(): Flow<List<com.sworddao.phoenix.feature.quest.data.Quest>> = flowOf(emptyQuestList)
    override fun getQuestProgress(questId: String): Flow<com.sworddao.phoenix.feature.quest.data.QuestProgress?> = flowOf(null)
    override fun getQuestStats(): Flow<com.sworddao.phoenix.feature.quest.data.QuestStats> = flowOf(
        com.sworddao.phoenix.feature.quest.data.QuestStats(
            totalQuests = 0,
            completedQuests = 0,
            activeQuests = 0,
            lockedQuests = 0,
            availableQuests = 0,
            completionRate = 0f,
            totalExperienceEarned = 0,
            favoriteQuestType = null
        )
    )
    override suspend fun startQuest(questId: String) = QuestResult.Success("Started")
    override suspend fun completeQuest(questId: String): QuestResult {
        completedQuestId = questId
        return QuestResult.Success("Completed")
    }
    override suspend fun abandonQuest(questId: String) = QuestResult.Success("Abandoned")
    override suspend fun updateObjectiveProgress(questId: String, objectiveId: String, progress: Int) = QuestResult.Success("Updated")
    override suspend fun checkPrerequisites(questId: String) = true
    override suspend fun getQuestsByNpc(npcId: String): Flow<List<com.sworddao.phoenix.feature.quest.data.Quest>> = flowOf(emptyQuestList)
    override suspend fun getQuestsByLocation(locationId: String): Flow<List<com.sworddao.phoenix.feature.quest.data.Quest>> = flowOf(emptyQuestList)
    override suspend fun refreshQuestAvailability() {}
}

private class FakeVocabularyRepository : VocabularyRepository {
    val discoveredWords = mutableListOf<String>()

    private val emptyWordList: List<com.sworddao.phoenix.feature.vocabulary.data.VocabularyWord> = emptyList()
    private val emptyCategoryList: List<com.sworddao.phoenix.feature.vocabulary.data.VocabularyCategory> = emptyList()

    override fun getAllWords(): Flow<List<com.sworddao.phoenix.feature.vocabulary.data.VocabularyWord>> = flowOf(emptyWordList)
    override fun getWordById(wordId: String): Flow<com.sworddao.phoenix.feature.vocabulary.data.VocabularyWord?> = flowOf(null)
    override fun getWordsByCategory(category: com.sworddao.phoenix.feature.vocabulary.data.VocabularyCategory): Flow<List<com.sworddao.phoenix.feature.vocabulary.data.VocabularyWord>> = flowOf(emptyWordList)
    override fun getWordsByMastery(mastery: com.sworddao.phoenix.feature.vocabulary.data.VocabularyMastery): Flow<List<com.sworddao.phoenix.feature.vocabulary.data.VocabularyWord>> = flowOf(emptyWordList)
    override fun getWordsByDifficulty(difficulty: com.sworddao.phoenix.feature.vocabulary.data.VocabularyDifficulty): Flow<List<com.sworddao.phoenix.feature.vocabulary.data.VocabularyWord>> = flowOf(emptyWordList)
    override fun getWordsByRegion(regionId: String): Flow<List<com.sworddao.phoenix.feature.vocabulary.data.VocabularyWord>> = flowOf(emptyWordList)
    override fun getWordsByNpc(npcId: String): Flow<List<com.sworddao.phoenix.feature.vocabulary.data.VocabularyWord>> = flowOf(emptyWordList)
    override fun getWordsByQuest(questId: String): Flow<List<com.sworddao.phoenix.feature.vocabulary.data.VocabularyWord>> = flowOf(emptyWordList)
    override fun getDiscoveredWords(): Flow<List<com.sworddao.phoenix.feature.vocabulary.data.VocabularyWord>> = flowOf(emptyWordList)
    override fun getUndiscoveredWords(): Flow<List<com.sworddao.phoenix.feature.vocabulary.data.VocabularyWord>> = flowOf(emptyWordList)
    override fun getFavorites(): Flow<List<com.sworddao.phoenix.feature.vocabulary.data.VocabularyWord>> = flowOf(emptyWordList)
    override fun getRecentlyLearned(limit: Int): Flow<List<com.sworddao.phoenix.feature.vocabulary.data.VocabularyWord>> = flowOf(emptyWordList)
    override fun searchWords(query: String): Flow<List<com.sworddao.phoenix.feature.vocabulary.data.VocabularyWord>> = flowOf(emptyWordList)
    override fun getStatistics(): Flow<com.sworddao.phoenix.feature.vocabulary.data.VocabularyStatistics> = flowOf(com.sworddao.phoenix.feature.vocabulary.data.VocabularyStatistics())
    override fun getCategories(): Flow<List<com.sworddao.phoenix.feature.vocabulary.data.VocabularyCategory>> = flowOf(emptyCategoryList)
    override fun getProgress(wordId: String): Flow<com.sworddao.phoenix.feature.vocabulary.data.VocabularyProgress?> = flowOf(null)
    override suspend fun discoverWord(wordId: String): VocabularyResult {
        discoveredWords.add(wordId)
        return VocabularyResult.Success("Discovered")
    }
    override suspend fun updateMastery(wordId: String, mastery: com.sworddao.phoenix.feature.vocabulary.data.VocabularyMastery) = VocabularyResult.Success("Updated")
    override suspend fun toggleFavorite(wordId: String) = VocabularyResult.Success("Toggled")
    override suspend fun incrementReview(wordId: String) = VocabularyResult.Success("Incremented")
    override suspend fun incrementSpoken(wordId: String) = VocabularyResult.Success("Incremented")
    override suspend fun incrementHeard(wordId: String) = VocabularyResult.Success("Incremented")
    override suspend fun recordDiscovery(wordId: String, source: com.sworddao.phoenix.feature.vocabulary.data.VocabularySource) = VocabularyResult.Success("Recorded")
    override suspend fun addWords(words: List<com.sworddao.phoenix.feature.vocabulary.data.VocabularyWord>) = VocabularyResult.Success("Added")
}
