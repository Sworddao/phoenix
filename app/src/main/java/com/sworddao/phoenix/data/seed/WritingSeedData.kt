package com.sworddao.phoenix.data.seed

import com.sworddao.phoenix.feature.writing.data.HanziCharacter
import com.sworddao.phoenix.feature.writing.data.HanziStroke
import com.sworddao.phoenix.feature.writing.data.StrokeDirection
import com.sworddao.phoenix.feature.writing.data.StrokeType
import com.sworddao.phoenix.feature.writing.data.WritingDifficulty
import com.sworddao.phoenix.feature.writing.data.WritingExercise
import com.sworddao.phoenix.feature.writing.data.WritingExerciseType

object WritingSeedData {

    fun createInitialCharacters(): List<HanziCharacter> = listOf(
        character("write_char_ni", "你", "nǐ", "you", listOf(3), "greet_001", BEGINNER, 10, 1,
            listOf(
                s(1, LEFT_FALLING, DIAGONAL_DOWN_LEFT, "Left-falling", "撇"),
                s(2, VERTICAL, TOP_TO_BOTTOM, "Vertical", "竖"),
                s(3, LEFT_FALLING, DIAGONAL_DOWN_LEFT, "Left-falling", "撇"),
                s(4, HOOK, LEFT_TO_RIGHT, "Horizontal hook", "横钩"),
                s(5, HOOK, TOP_TO_BOTTOM, "Vertical hook", "竖钩"),
                s(6, LEFT_FALLING, DIAGONAL_DOWN_LEFT, "Left-falling", "撇"),
                s(7, DOT, TOP_TO_BOTTOM, "Dot", "点"),
            )
        ),
        character("write_char_hao", "好", "hǎo", "good", listOf(3), "greet_001", BEGINNER, 10, 2,
            listOf(
                s(1, LEFT_FALLING, DIAGONAL_DOWN_LEFT, "Left-falling", "撇"),
                s(2, LEFT_FALLING, DIAGONAL_DOWN_LEFT, "Left-falling", "撇"),
                s(3, HORIZONTAL, LEFT_TO_RIGHT, "Horizontal", "横"),
                s(4, TURNING, DIAGONAL_DOWN_LEFT, "Turning", "横折"),
                s(5, HOOK, TOP_TO_BOTTOM, "Vertical hook", "竖钩"),
                s(6, HORIZONTAL, LEFT_TO_RIGHT, "Horizontal", "横"),
            )
        ),
        character("write_char_zai", "再", "zài", "again", listOf(4), "greet_002", BEGINNER, 10, 3,
            listOf(
                s(1, HORIZONTAL, LEFT_TO_RIGHT, "Horizontal", "横"),
                s(2, VERTICAL, TOP_TO_BOTTOM, "Vertical", "竖"),
                s(3, TURNING, TOP_TO_BOTTOM, "Turning", "横折"),
                s(4, VERTICAL, TOP_TO_BOTTOM, "Vertical", "竖"),
                s(5, HORIZONTAL, LEFT_TO_RIGHT, "Horizontal", "横"),
                s(6, HORIZONTAL, LEFT_TO_RIGHT, "Horizontal", "横"),
            )
        ),
        character("write_char_jian", "见", "jiàn", "see", listOf(4), "greet_002", BEGINNER, 10, 4,
            listOf(
                s(1, VERTICAL, TOP_TO_BOTTOM, "Vertical", "竖"),
                s(2, TURNING, DIAGONAL_DOWN_RIGHT, "Turning", "横折"),
                s(3, LEFT_FALLING, DIAGONAL_DOWN_LEFT, "Left-falling", "撇"),
                s(4, HOOK, DIAGONAL_DOWN_RIGHT, "Vertical-horizontal hook", "竖弯钩"),
            )
        ),
        character("write_char_xie", "谢", "xiè", "thank", listOf(4), "greet_003", INTERMEDIATE, 12, 5,
            listOf(
                s(1, DOT, TOP_TO_BOTTOM, "Dot", "点"),
                s(2, TURNING, DIAGONAL_DOWN_RIGHT, "Turning", "横折提"),
                s(3, LEFT_FALLING, DIAGONAL_DOWN_LEFT, "Left-falling", "撇"),
                s(4, VERTICAL, TOP_TO_BOTTOM, "Vertical", "竖"),
                s(5, TURNING, TOP_TO_BOTTOM, "Turning", "横折钩"),
                s(6, HORIZONTAL, LEFT_TO_RIGHT, "Horizontal", "横"),
                s(7, HORIZONTAL, LEFT_TO_RIGHT, "Horizontal", "横"),
                s(8, HORIZONTAL, LEFT_TO_RIGHT, "Horizontal", "横"),
                s(9, LEFT_FALLING, DIAGONAL_DOWN_LEFT, "Left-falling", "撇"),
                s(10, HORIZONTAL, LEFT_TO_RIGHT, "Horizontal", "横"),
                s(11, HOOK, TOP_TO_BOTTOM, "Vertical hook", "竖钩"),
                s(12, DOT, TOP_TO_BOTTOM, "Dot", "点"),
            )
        ),
        character("write_char_ma", "吗", "ma", "question particle", listOf(0), "greet_007", BEGINNER, 10, 6,
            listOf(
                s(1, VERTICAL, TOP_TO_BOTTOM, "Vertical", "竖"),
                s(2, TURNING, TOP_TO_BOTTOM, "Turning", "横折"),
                s(3, HORIZONTAL, LEFT_TO_RIGHT, "Horizontal", "横"),
                s(4, TURNING, DIAGONAL_DOWN_RIGHT, "Turning", "横折"),
                s(5, HOOK, TOP_TO_BOTTOM, "Turning hook", "竖折折钩"),
                s(6, HORIZONTAL, LEFT_TO_RIGHT, "Horizontal", "横"),
            )
        ),
        character("write_char_chi", "吃", "chī", "eat", listOf(1), "food_001", BEGINNER, 10, 7,
            listOf(
                s(1, VERTICAL, TOP_TO_BOTTOM, "Vertical", "竖"),
                s(2, TURNING, TOP_TO_BOTTOM, "Turning", "横折"),
                s(3, HORIZONTAL, LEFT_TO_RIGHT, "Horizontal", "横"),
                s(4, LEFT_FALLING, DIAGONAL_DOWN_LEFT, "Left-falling", "撇"),
                s(5, TURNING, DIAGONAL_DOWN_RIGHT, "Turning hook", "横折弯钩"),
                s(6, VERTICAL, TOP_TO_BOTTOM, "Vertical", "竖"),
            )
        ),
        character("write_char_fan", "饭", "fàn", "rice/meal", listOf(4), "food_001", ELEMENTARY, 12, 8,
            listOf(
                s(1, LEFT_FALLING, DIAGONAL_DOWN_LEFT, "Left-falling", "撇"),
                s(2, HOOK, LEFT_TO_RIGHT, "Horizontal hook", "横钩"),
                s(3, RAISING, DIAGONAL_DOWN_RIGHT, "Rising", "竖提"),
                s(4, LEFT_FALLING, DIAGONAL_DOWN_LEFT, "Left-falling", "撇"),
                s(5, LEFT_FALLING, DIAGONAL_DOWN_LEFT, "Left-falling", "撇"),
                s(6, TURNING, DIAGONAL_DOWN_LEFT, "Turning", "横撇"),
                s(7, RIGHT_FALLING, DIAGONAL_DOWN_RIGHT, "Right-falling", "捺"),
            )
        ),
        character("write_char_he", "喝", "hē", "drink", listOf(1), "food_002", INTERMEDIATE, 12, 9,
            listOf(
                s(1, VERTICAL, TOP_TO_BOTTOM, "Vertical", "竖"),
                s(2, TURNING, TOP_TO_BOTTOM, "Turning", "横折"),
                s(3, HORIZONTAL, LEFT_TO_RIGHT, "Horizontal", "横"),
                s(4, VERTICAL, TOP_TO_BOTTOM, "Vertical", "竖"),
                s(5, TURNING, TOP_TO_BOTTOM, "Turning", "横折"),
                s(6, HORIZONTAL, LEFT_TO_RIGHT, "Horizontal", "横"),
                s(7, HORIZONTAL, LEFT_TO_RIGHT, "Horizontal", "横"),
                s(8, LEFT_FALLING, DIAGONAL_DOWN_LEFT, "Left-falling", "撇"),
                s(9, TURNING, TOP_TO_BOTTOM, "Turning hook", "横折钩"),
                s(10, VERTICAL, TOP_TO_BOTTOM, "Vertical", "竖"),
                s(11, HOOK, DIAGONAL_DOWN_RIGHT, "Vertical hook", "竖钩"),
                s(12, LEFT_FALLING, DIAGONAL_DOWN_LEFT, "Left-falling", "撇"),
            )
        ),
        character("write_char_yi", "一", "yī", "one", listOf(1), "num_001", BEGINNER, 10, 10,
            listOf(
                s(1, HORIZONTAL, LEFT_TO_RIGHT, "Horizontal", "横"),
            )
        ),
        character("write_char_er", "二", "èr", "two", listOf(4), "num_002", BEGINNER, 10, 11,
            listOf(
                s(1, HORIZONTAL, LEFT_TO_RIGHT, "Horizontal", "横"),
                s(2, HORIZONTAL, LEFT_TO_RIGHT, "Horizontal", "横"),
            )
        ),
        character("write_char_san", "三", "sān", "three", listOf(1), "num_003", BEGINNER, 10, 12,
            listOf(
                s(1, HORIZONTAL, LEFT_TO_RIGHT, "Horizontal", "横"),
                s(2, HORIZONTAL, LEFT_TO_RIGHT, "Horizontal", "横"),
                s(3, HORIZONTAL, LEFT_TO_RIGHT, "Horizontal", "横"),
            )
        ),
        character("write_char_shi", "十", "shí", "ten", listOf(2), "num_004", BEGINNER, 10, 13,
            listOf(
                s(1, HORIZONTAL, LEFT_TO_RIGHT, "Horizontal", "横"),
                s(2, VERTICAL, TOP_TO_BOTTOM, "Vertical", "竖"),
            )
        ),
        character("write_char_ren", "人", "rén", "person", listOf(2), null, BEGINNER, 10, 14,
            listOf(
                s(1, LEFT_FALLING, DIAGONAL_DOWN_LEFT, "Left-falling", "撇"),
                s(2, RIGHT_FALLING, DIAGONAL_DOWN_RIGHT, "Right-falling", "捺"),
            )
        ),
        character("write_char_da", "大", "dà", "big", listOf(4), null, BEGINNER, 10, 15,
            listOf(
                s(1, HORIZONTAL, LEFT_TO_RIGHT, "Horizontal", "横"),
                s(2, LEFT_FALLING, DIAGONAL_DOWN_LEFT, "Left-falling", "撇"),
                s(3, RIGHT_FALLING, DIAGONAL_DOWN_RIGHT, "Right-falling", "捺"),
            )
        ),
        character("write_char_tian", "天", "tiān", "sky/day", listOf(1), null, BEGINNER, 10, 16,
            listOf(
                s(1, HORIZONTAL, LEFT_TO_RIGHT, "Horizontal", "横"),
                s(2, HORIZONTAL, LEFT_TO_RIGHT, "Horizontal", "横"),
                s(3, LEFT_FALLING, DIAGONAL_DOWN_LEFT, "Left-falling", "撇"),
                s(4, RIGHT_FALLING, DIAGONAL_DOWN_RIGHT, "Right-falling", "捺"),
            )
        ),
        character("write_char_kou", "口", "kǒu", "mouth", listOf(3), null, BEGINNER, 10, 17,
            listOf(
                s(1, VERTICAL, TOP_TO_BOTTOM, "Vertical", "竖"),
                s(2, TURNING, TOP_TO_BOTTOM, "Turning", "横折"),
                s(3, HORIZONTAL, LEFT_TO_RIGHT, "Horizontal", "横"),
            )
        ),
        character("write_char_shan", "山", "shān", "mountain", listOf(1), null, BEGINNER, 10, 18,
            listOf(
                s(1, VERTICAL, TOP_TO_BOTTOM, "Vertical", "竖"),
                s(2, TURNING, TOP_TO_BOTTOM, "Turning", "竖折"),
                s(3, VERTICAL, TOP_TO_BOTTOM, "Vertical", "竖"),
            )
        ),
        character("write_char_zhong", "中", "zhōng", "middle", listOf(1), null, BEGINNER, 10, 19,
            listOf(
                s(1, VERTICAL, TOP_TO_BOTTOM, "Vertical", "竖"),
                s(2, TURNING, TOP_TO_BOTTOM, "Turning", "横折"),
                s(3, HORIZONTAL, LEFT_TO_RIGHT, "Horizontal", "横"),
                s(4, VERTICAL, TOP_TO_BOTTOM, "Vertical", "竖"),
            )
        ),
    )

    fun createInitialExercises(): List<WritingExercise> {
        val characters = createInitialCharacters()
        val exercises = mutableListOf<WritingExercise>()
        var order = 1

        characters.forEach { character ->
            exercises += WritingExercise(
                id = "write_ex_trace_${character.id.removePrefix("write_char_")}",
                type = WritingExerciseType.TRACE_STROKES,
                difficulty = character.difficulty,
                character = character,
                prompt = "按照笔顺描摹“${character.hanzi}”的每一笔",
                xpReward = character.xpReward,
                friendshipBonus = if (character.wordId?.startsWith("greet_") == true) 2 else 0,
                isUnlocked = true,
                order = order++,
            )
        }

        listOf("一", "二", "三", "十", "人", "大", "天", "口", "山", "中").forEach { hanzi ->
            val character = characters.find { it.hanzi == hanzi } ?: return@forEach
            exercises += WritingExercise(
                id = "write_ex_order_${character.id.removePrefix("write_char_")}",
                type = WritingExerciseType.STROKE_ORDER,
                difficulty = character.difficulty,
                character = character,
                prompt = "把“${character.hanzi}”的笔画按正确顺序排列",
                xpReward = character.xpReward,
                isUnlocked = true,
                order = order++,
            )
        }

        listOf("一", "三", "人", "大").forEach { hanzi ->
            val character = characters.find { it.hanzi == hanzi } ?: return@forEach
            exercises += WritingExercise(
                id = "write_ex_dir_${character.id.removePrefix("write_char_")}",
                type = WritingExerciseType.DIRECTION_CHECK,
                difficulty = character.difficulty,
                character = character,
                prompt = "判断“${character.hanzi}”每一笔的书写方向",
                xpReward = character.xpReward,
                isUnlocked = true,
                order = order++,
            )
        }

        return exercises
    }

    private fun character(
        id: String,
        hanzi: String,
        pinyin: String,
        english: String,
        syllableTones: List<Int>,
        wordId: String?,
        difficulty: WritingDifficulty,
        xpReward: Int,
        order: Int,
        strokes: List<HanziStroke>,
    ): HanziCharacter = HanziCharacter(
        id = id,
        hanzi = hanzi,
        pinyin = pinyin,
        english = english,
        syllableTones = syllableTones,
        wordId = wordId,
        strokes = strokes.map { it.copy(character = hanzi) },
        difficulty = difficulty,
        xpReward = xpReward,
        order = order,
    )

    private fun s(
        order: Int,
        type: StrokeType,
        direction: StrokeDirection,
        name: String,
        nameCn: String,
    ): HanziStroke = HanziStroke(
        id = "stroke_$order",
        character = "",
        order = order,
        type = type,
        direction = direction,
        name = name,
        nameCn = nameCn,
    )

    private val BEGINNER = WritingDifficulty.BEGINNER
    private val ELEMENTARY = WritingDifficulty.ELEMENTARY
    private val INTERMEDIATE = WritingDifficulty.INTERMEDIATE
    private val LEFT_FALLING = StrokeType.LEFT_FALLING
    private val RIGHT_FALLING = StrokeType.RIGHT_FALLING
    private val VERTICAL = StrokeType.VERTICAL
    private val HORIZONTAL = StrokeType.HORIZONTAL
    private val DOT = StrokeType.DOT
    private val HOOK = StrokeType.HOOK
    private val RAISING = StrokeType.RAISING
    private val TURNING = StrokeType.TURNING
    private val LEFT_TO_RIGHT = StrokeDirection.LEFT_TO_RIGHT
    private val TOP_TO_BOTTOM = StrokeDirection.TOP_TO_BOTTOM
    private val DIAGONAL_DOWN_LEFT = StrokeDirection.DIAGONAL_DOWN_LEFT
    private val DIAGONAL_DOWN_RIGHT = StrokeDirection.DIAGONAL_DOWN_RIGHT
}
