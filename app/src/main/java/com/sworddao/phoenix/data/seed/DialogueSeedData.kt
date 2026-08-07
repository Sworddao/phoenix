package com.sworddao.phoenix.data.seed


import com.sworddao.phoenix.feature.dialogue.data.Dialogue
import com.sworddao.phoenix.feature.dialogue.data.ActionType
import com.sworddao.phoenix.feature.dialogue.data.DialogueAction
import com.sworddao.phoenix.feature.dialogue.data.DialogueChoice
import com.sworddao.phoenix.feature.dialogue.data.DialogueCondition
import com.sworddao.phoenix.feature.dialogue.data.DialogueNode
import com.sworddao.phoenix.feature.dialogue.data.DialogueNodeType
import com.sworddao.phoenix.feature.dialogue.data.Speaker

object DialogueSeedData {

fun loadDialogues(): List<Dialogue> {
        return listOf(
            createGrandmaMeiDialogue()
        )
    }

fun createGrandmaMeiDialogue(): Dialogue {
        return Dialogue(
            id = "grandma_mei_greeting",
            npcId = "grandma_mei",
            title = "Meeting Grandma Mei",
            description = "Your first conversation with Grandma Mei at her bakery.",
            startNodeId = "start",
            nodes = listOf(
                DialogueNode(
                    id = "start",
                    type = DialogueNodeType.NPC_SPEAKS,
                    speaker = Speaker.NPC,
                    speakerName = "Grandma Mei",
                    text = "Oh! A new face in the village! Welcome, welcome! I am Grandma Mei. I run this little bakery.",
                    pinyin = "O! Xin lian zai cunli! Huanying, huanying! Wo shi Nainai Mei. Wo kai zhege xiao mianbao dian.",
                    nextNodeId = "player_respond_1"
                ),
                DialogueNode(
                    id = "player_respond_1",
                    type = DialogueNodeType.PLAYER_CHOOSES,
                    speaker = Speaker.PLAYER,
                    speakerName = "Player",
                    text = "",
                    choices = listOf(
                        DialogueChoice(
                            id = "choice_greeting",
                            text = "Hello! Nice to meet you!",
                            pinyin = "Ni hao! Hen gaoxing renshi ni!",
                            nextNodeId = "mei_happy"
                        ),
                        DialogueChoice(
                            id = "choice_bakery",
                            text = "This bakery smells amazing!",
                            pinyin = "Zhege mianbao dian wen qilai hao xiang!",
                            nextNodeId = "mei_proud"
                        ),
                        DialogueChoice(
                            id = "choice_name",
                            text = "I am a friend. What is your name?",
                            pinyin = "Wo shi yige pengyou. Ni jiao shenme mingzi?",
                            nextNodeId = "mei_introduce"
                        )
                    )
                ),
                DialogueNode(
                    id = "mei_happy",
                    type = DialogueNodeType.NPC_SPEAKS,
                    speaker = Speaker.NPC,
                    speakerName = "Grandma Mei",
                    text = "How polite! I like you already. Here, have a fresh steamed bun. It is on the house!",
                    pinyin = "Duome you limao! Wo yijing xihuan ni le. Lai, chi yige re teng teng de baozi! Wo qing ke!",
                    nextNodeId = "player_thank"
                ),
                DialogueNode(
                    id = "mei_proud",
                    type = DialogueNodeType.NPC_SPEAKS,
                    speaker = Speaker.NPC,
                    speakerName = "Grandma Mei",
                    text = "You have a good nose! I have been baking for forty years. The secret is fresh ingredients and love.",
                    pinyin = "Ni bizi hen ling! Wo kao le sishi nian le. Mijue shi xinxian de cailiao he ai.",
                    nextNodeId = "player_thank"
                ),
                DialogueNode(
                    id = "mei_introduce",
                    type = DialogueNodeType.NPC_SPEAKS,
                    speaker = Speaker.NPC,
                    speakerName = "Grandma Mei",
                    text = "Mei! Like the Chinese word for beautiful. But do not let the name fool you - I am just an old baker who loves feeding people.",
                    pinyin = "Wo jiao Mei! Xiang Zhongwen de mei. Danbie bei mingzi pianle - wo zhishi yige xihuan wei ren chifan de lao mianbao shifu.",
                    nextNodeId = "player_thank"
                ),
                DialogueNode(
                    id = "player_thank",
                    type = DialogueNodeType.PLAYER_CHOOSES,
                    speaker = Speaker.PLAYER,
                    speakerName = "Player",
                    text = "",
                    choices = listOf(
                        DialogueChoice(
                            id = "choice_learn",
                            text = "Can you teach me some food words?",
                            pinyin = "Ni neng jiao wo yixie shiwu cihui ma?",
                            nextNodeId = "mei_teach",
                            actions = listOf(
                                DialogueAction(
                                    type = ActionType.ADD_FRIENDSHIP_XP,
                                    targetId = "grandma_mei",
                                    value = "25"
                                )
                            )
                        ),
                        DialogueChoice(
                            id = "choice_visit",
                            text = "I will come back to visit!",
                            pinyin = "Wo hui zai lai kan ni de!",
                            nextNodeId = "mei_farewell",
                            actions = listOf(
                                DialogueAction(
                                    type = ActionType.ADD_FRIENDSHIP_XP,
                                    targetId = "grandma_mei",
                                    value = "15"
                                )
                            )
                        )
                    )
                ),
                DialogueNode(
                    id = "mei_teach",
                    type = DialogueNodeType.NPC_SPEAKS,
                    speaker = Speaker.NPC,
                    speakerName = "Grandma Mei",
                    text = "Of course! In Chinese, we say chifan for eating a meal. And hao chi means delicious! Try saying it!",
                    pinyin = "Dangran! Zai Zhongwen, women shuo chifan jiushi chi fan. Hao chi jiushi hen hao chi! Ni shishi shuo!",
                    nextNodeId = "end",
                    actions = listOf(
                        DialogueAction(
                            type = ActionType.UNLOCK_VOCABULARY,
                            targetId = "food_basics",
                            value = "chifan,hao chi"
                        )
                    )
                ),
                DialogueNode(
                    id = "mei_farewell",
                    type = DialogueNodeType.NPC_SPEAKS,
                    speaker = Speaker.NPC,
                    speakerName = "Grandma Mei",
                    text = "Come back anytime! I always have fresh bread in the morning. And remember - an empty stomach makes for a sad day!",
                    pinyin = "Suishi huilai! Wo zaoshang zong you xinmian bao. Jizhu - duzi e le yitian dou bu kaixin!",
                    nextNodeId = "end"
                ),
                DialogueNode(
                    id = "end",
                    type = DialogueNodeType.CONVERSATION_END,
                    speaker = Speaker.NARRATOR,
                    speakerName = "",
                    text = "You have had a lovely conversation with Grandma Mei. She seems like a wonderful friend to have in the village.",
                    pinyin = "Ni he Nainai Mei liao de hen kaixin. Ta kanqilai shi cunli hen hao de pengyou.",
                    actions = listOf(
                        DialogueAction(
                            type = ActionType.PRACTICE_SPEAKING,
                            targetId = "pronunciation",
                            value = "pron_ex_dlg_hao_chi,pron_ex_dlg_meet"
                        ),
                        DialogueAction(
                            type = ActionType.PRACTICE_LISTENING,
                            targetId = "listening",
                            value = "listen_ex_greet_hello,listen_ex_greet_thanks"
                        ),
                        DialogueAction(
                            type = ActionType.PRACTICE_READING,
                            targetId = "reading",
                            value = "read_ex_greet_hello,read_ex_greet_thanks"
                        ),
                        DialogueAction(
                            type = ActionType.ADD_FRIENDSHIP_XP,
                            targetId = "grandma_mei",
                            value = "10"
                        )
                    )
                )
            )
        )
    }

}
