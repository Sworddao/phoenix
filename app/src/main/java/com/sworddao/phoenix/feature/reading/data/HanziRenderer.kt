package com.sworddao.phoenix.feature.reading.data

import javax.inject.Inject
import javax.inject.Singleton

interface HanziRenderer {
    val name: String
    val isAvailable: Boolean
    fun render(hanzi: String?, pinyin: String, mode: CharacterRevealState): RenderedHanzi
    fun tonesOf(pinyin: String): List<Int>
    fun getRendererInfo(): HanziRendererInfo
}

@Singleton
class MockHanziRenderer @Inject constructor() : HanziRenderer {

    override val name: String = "MockHanziRenderer"
    override val isAvailable: Boolean = true

    override fun render(hanzi: String?, pinyin: String, mode: CharacterRevealState): RenderedHanzi {
        val tones = tonesOf(pinyin)
        val spans = pinyin.split(Regex("\\s+")).filter { it.isNotEmpty() }
            .mapIndexed { index, syllable ->
                RenderedHanziSpan(text = syllable, toneColor = toneFor(tones.getOrNull(index) ?: 0))
            }

        return when (mode) {
            CharacterRevealState.HIDDEN -> RenderedHanzi(
                hanzi = null,
                maskedHanzi = maskHanzi(hanzi),
                pinyin = pinyin,
                toneColoredPinyin = spans,
                isHanziVisible = false,
            )
            CharacterRevealState.PINYIN_ONLY -> RenderedHanzi(
                hanzi = null,
                maskedHanzi = maskHanzi(hanzi),
                pinyin = pinyin,
                toneColoredPinyin = spans,
                isHanziVisible = false,
            )
            CharacterRevealState.HANZI_ONLY -> RenderedHanzi(
                hanzi = hanzi,
                maskedHanzi = hanzi ?: "",
                pinyin = "",
                toneColoredPinyin = emptyList(),
                isHanziVisible = true,
            )
            CharacterRevealState.HANZI_AND_PINYIN -> RenderedHanzi(
                hanzi = hanzi,
                maskedHanzi = hanzi ?: "",
                pinyin = pinyin,
                toneColoredPinyin = spans,
                isHanziVisible = true,
            )
            CharacterRevealState.TONE_COLORED_PINYIN -> RenderedHanzi(
                hanzi = hanzi,
                maskedHanzi = hanzi ?: "",
                pinyin = pinyin,
                toneColoredPinyin = spans,
                isHanziVisible = true,
            )
            CharacterRevealState.TAP_TO_REVEAL -> RenderedHanzi(
                hanzi = null,
                maskedHanzi = maskHanzi(hanzi),
                pinyin = pinyin,
                toneColoredPinyin = spans,
                isHanziVisible = false,
            )
            CharacterRevealState.AUTO_REVEAL -> RenderedHanzi(
                hanzi = null,
                maskedHanzi = maskHanzi(hanzi),
                pinyin = pinyin,
                toneColoredPinyin = spans,
                isHanziVisible = false,
            )
        }
    }

    override fun tonesOf(pinyin: String): List<Int> {
        return pinyin
            .split(Regex("\\s+"))
            .filter { it.isNotEmpty() }
            .map { syllable ->
                var tone = 0
                for (char in syllable) {
                    tone = when (char) {
                        in "āēīōūǖ" -> 1
                        in "áéíóúǘ" -> 2
                        in "ǎěǐǒǔǚ" -> 3
                        in "àèìòùǜ" -> 4
                        else -> continue
                    }
                    break
                }
                tone
            }
    }

    override fun getRendererInfo(): HanziRendererInfo {
        return HanziRendererInfo(
            name = name,
            version = "1.0",
            supportedModes = CharacterRevealState.entries,
            supportsToneColoring = true,
        )
    }

    private fun maskHanzi(hanzi: String?): String {
        if (hanzi.isNullOrEmpty()) return ""
        return hanzi.map { '▢' }.joinToString("")
    }

    private fun toneFor(tone: Int): ToneColor? {
        return when (tone) {
            1 -> ToneColor.TONE1
            2 -> ToneColor.TONE2
            3 -> ToneColor.TONE3
            4 -> ToneColor.TONE4
            else -> ToneColor.NEUTRAL
        }
    }
}