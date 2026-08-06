package com.sworddao.phoenix.feature.pronunciation.data

import com.sworddao.phoenix.feature.pronunciation.data.PronunciationAttempt
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

interface PronunciationEngine {
    val name: String
    val isAvailable: Boolean
    val supportedLanguages: List<String>

    suspend fun initialize(): EngineResult
    suspend fun startListening(config: RecognitionConfig): Flow<RecognitionPartialResult>
    suspend fun stopListening(): EngineResult
    suspend fun recognize(audioPath: String, expectedText: String, expectedPinyin: String): RecognitionResult
    suspend fun recognizeFromText(spokenText: String, expectedText: String, expectedPinyin: String): RecognitionResult
    fun getEngineInfo(): EngineInfo
    suspend fun shutdown()
}

data class RecognitionConfig(
    val language: String = "zh-CN",
    val expectedPhrase: String = "",
    val expectedPinyin: String = "",
    val enableToneDetection: Boolean = true,
    val enablePartialResults: Boolean = true,
    val maxDurationMs: Long = 10000,
    val silenceTimeoutMs: Long = 3000,
)

data class RecognitionPartialResult(
    val text: String,
    val confidence: Float,
    val isFinal: Boolean,
    val timestamp: Long = System.currentTimeMillis(),
)

data class RecognitionResult(
    val recognizedText: String,
    val confidence: Float,
    val toneAccuracy: Float,
    val fluencyScore: Float,
    val alternativeTexts: List<String> = emptyList(),
    val error: String? = null,
) {
    val isSuccessful: Boolean
        get() = error == null && confidence > 0.3f
}

data class EngineInfo(
    val name: String,
    val version: String,
    val modelSizeMb: Long,
    val requiresNetwork: Boolean,
    val supportedFeatures: List<EngineFeature> = emptyList(),
)

enum class EngineFeature {
    OFFLINE_RECOGNITION,
    TONE_DETECTION,
    PARTIAL_RESULTS,
    SPEAKER_ADAPTATION,
    NOISE_SUPPRESSION,
    STREAMING,
}

sealed class EngineResult {
    data class Success(val message: String = "") : EngineResult()
    data class Error(val message: String) : EngineResult()
    data class NotAvailable(val reason: String) : EngineResult()
    data class RequiresDownload(val downloadUrl: String, val sizeMb: Long) : EngineResult()
}

class MockPronunciationEngine @Inject constructor() : PronunciationEngine {
    override val name = "MockEngine"
    override val isAvailable = true
    override val supportedLanguages = listOf("zh-CN", "en-US")

    private var _isInitialized = false

    override suspend fun initialize(): EngineResult {
        _isInitialized = true
        return EngineResult.Success("Mock engine initialized")
    }

    override suspend fun startListening(config: RecognitionConfig): Flow<RecognitionPartialResult> {
        val phrase = config.expectedPhrase.ifBlank { config.expectedPinyin }.ifBlank { "nǐ hǎo" }
        return kotlinx.coroutines.flow.flow {
            emit(RecognitionPartialResult(phrase.take(phrase.length / 2).ifEmpty { phrase }, 0.4f, false))
            kotlinx.coroutines.delay(300)
            emit(RecognitionPartialResult(phrase, 0.6f, false))
            kotlinx.coroutines.delay(300)
            emit(RecognitionPartialResult(phrase, 0.9f, true))
        }
    }

    override suspend fun stopListening(): EngineResult {
        return EngineResult.Success("Stopped listening")
    }

    override suspend fun recognize(audioPath: String, expectedText: String, expectedPinyin: String): RecognitionResult {
        val similarity = calculateSimilarity(expectedPinyin, "nǐ hǎo")
        return RecognitionResult(
            recognizedText = "你 好",
            confidence = similarity,
            toneAccuracy = similarity * 0.9f,
            fluencyScore = similarity * 0.85f,
        )
    }

    override suspend fun recognizeFromText(spokenText: String, expectedText: String, expectedPinyin: String): RecognitionResult {
        val similarity = calculateSimilarity(expectedPinyin, spokenText)
        return RecognitionResult(
            recognizedText = spokenText,
            confidence = similarity,
            toneAccuracy = similarity * 0.9f,
            fluencyScore = similarity * 0.85f,
        )
    }

    override fun getEngineInfo(): EngineInfo {
        return EngineInfo(
            name = "Mock Pronunciation Engine",
            version = "1.0.0",
            modelSizeMb = 0,
            requiresNetwork = false,
            supportedFeatures = listOf(
                EngineFeature.OFFLINE_RECOGNITION,
                EngineFeature.TONE_DETECTION,
                EngineFeature.PARTIAL_RESULTS,
            )
        )
    }

    override suspend fun shutdown() {
        _isInitialized = false
    }

    private fun calculateSimilarity(expected: String, actual: String): Float {
        val expectedNormalized = expected.lowercase().replace(" ", "").replace("，", "").replace("。", "")
        val actualNormalized = actual.lowercase().replace(" ", "").replace("，", "").replace("。", "")

        if (expectedNormalized == actualNormalized) return 1.0f

        val maxLen = maxOf(expectedNormalized.length, actualNormalized.length)
        if (maxLen == 0) return 1.0f

        var matches = 0
        for (i in 0 until minOf(expectedNormalized.length, actualNormalized.length)) {
            if (expectedNormalized[i] == actualNormalized[i]) matches++
        }

        return matches.toFloat() / maxLen
    }
}

class AndroidSpeechRecognizerEngine(
    private val context: android.content.Context
) : PronunciationEngine {
    override val name = "AndroidSpeechRecognizer"
    override val isAvailable = android.speech.SpeechRecognizer.isRecognitionAvailable(context)
    override val supportedLanguages = listOf("zh-CN", "en-US", "zh-TW", "zh-HK")

    private var recognizer: android.speech.SpeechRecognizer? = null
    private var listener: RecognitionListener? = null

    override suspend fun initialize(): EngineResult {
        return kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            try {
                recognizer = android.speech.SpeechRecognizer.createSpeechRecognizer(context)
                listener = RecognitionListener()
                recognizer?.setRecognitionListener(listener!!)
                EngineResult.Success("Android SpeechRecognizer initialized")
            } catch (e: Exception) {
                EngineResult.Error("Failed to initialize: ${e.message}")
            }
        }
    }

    override suspend fun startListening(config: RecognitionConfig): Flow<RecognitionPartialResult> {
        return kotlinx.coroutines.flow.callbackFlow {
            val intent = android.content.Intent(android.speech.RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
            intent.putExtra(android.speech.RecognizerIntent.EXTRA_LANGUAGE_MODEL, android.speech.RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            intent.putExtra(android.speech.RecognizerIntent.EXTRA_LANGUAGE, config.language)
            intent.putExtra(android.speech.RecognizerIntent.EXTRA_MAX_RESULTS, 3)
            intent.putExtra(android.speech.RecognizerIntent.EXTRA_PARTIAL_RESULTS, config.enablePartialResults)

            listener?.onPartialResultsCallback = { results ->
                val text = results?.firstOrNull() ?: ""
                val confidence = if (results != null && results.isNotEmpty()) 0.8f else 0.3f
                trySend(RecognitionPartialResult(text, confidence, false))
            }

            recognizer?.startListening(intent)

            awaitClose { recognizer?.stopListening() }
        }
    }

    override suspend fun stopListening(): EngineResult {
        recognizer?.stopListening()
        return EngineResult.Success("Stopped listening")
    }

    override suspend fun recognize(audioPath: String, expectedText: String, expectedPinyin: String): RecognitionResult {
        return kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            RecognitionResult("", 0f, 0f, 0f, error = "File recognition not supported")
        }
    }

    override suspend fun recognizeFromText(spokenText: String, expectedText: String, expectedPinyin: String): RecognitionResult {
        val similarity = calculateSimilarity(expectedPinyin, spokenText)
        return RecognitionResult(
            recognizedText = spokenText,
            confidence = similarity,
            toneAccuracy = similarity * 0.9f,
            fluencyScore = similarity * 0.85f,
        )
    }

    override fun getEngineInfo(): EngineInfo {
        return EngineInfo(
            name = "Android SpeechRecognizer",
            version = "1.0",
            modelSizeMb = 0,
            requiresNetwork = true,
            supportedFeatures = listOf(
                EngineFeature.OFFLINE_RECOGNITION,
                EngineFeature.PARTIAL_RESULTS,
            )
        )
    }

    override suspend fun shutdown() {
        recognizer?.destroy()
        recognizer = null
    }

    private fun calculateSimilarity(expected: String, actual: String): Float {
        val expectedNormalized = expected.lowercase().replace(" ", "")
        val actualNormalized = actual.lowercase().replace(" ", "")

        if (expectedNormalized == actualNormalized) return 1.0f

        val maxLen = maxOf(expectedNormalized.length, actualNormalized.length)
        if (maxLen == 0) return 1.0f

        var matches = 0
        for (i in 0 until minOf(expectedNormalized.length, actualNormalized.length)) {
            if (expectedNormalized[i] == actualNormalized[i]) matches++
        }

        return matches.toFloat() / maxLen
    }

    private class RecognitionListener : android.speech.RecognitionListener {
        var onPartialResultsCallback: ((List<String>?) -> Unit)? = null

        override fun onReadyForSpeech(params: android.os.Bundle?) {}
        override fun onBeginningOfSpeech() {}
        override fun onRmsChanged(rmsdB: Float) {}
        override fun onBufferReceived(buffer: ByteArray?) {}
        override fun onEndOfSpeech() {}
        override fun onError(error: Int) {}
        override fun onResults(results: android.os.Bundle?) {
            val matches = results?.getStringArrayList(android.speech.SpeechRecognizer.RESULTS_RECOGNITION)
            onPartialResultsCallback?.invoke(matches)
        }
        override fun onPartialResults(partialResults: android.os.Bundle?) {
            val matches = partialResults?.getStringArrayList(android.speech.SpeechRecognizer.RESULTS_RECOGNITION)
            onPartialResultsCallback?.invoke(matches)
        }
        override fun onEvent(eventType: Int, params: android.os.Bundle?) {}
    }
}

class VoskEngine(
    private val context: android.content.Context,
    private val modelPath: String
) : PronunciationEngine {
    override val name = "Vosk"
    override val isAvailable = java.io.File(modelPath).exists()
    override val supportedLanguages = listOf("zh-CN")

    override suspend fun initialize(): EngineResult {
        return kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            if (!isAvailable) {
                EngineResult.NotAvailable("Vosk model not found at $modelPath")
            } else {
                EngineResult.Success("Vosk initialized")
            }
        }
    }

    override suspend fun startListening(config: RecognitionConfig): Flow<RecognitionPartialResult> {
        return kotlinx.coroutines.flow.flow {
            emit(RecognitionPartialResult("", 0f, false))
        }
    }

    override suspend fun stopListening(): EngineResult = EngineResult.Success("Stopped")

    override suspend fun recognize(audioPath: String, expectedText: String, expectedPinyin: String): RecognitionResult {
        return RecognitionResult("", 0f, 0f, 0f, error = "Vosk integration pending")
    }

    override suspend fun recognizeFromText(spokenText: String, expectedText: String, expectedPinyin: String): RecognitionResult {
        val similarity = calculateSimilarity(expectedPinyin, spokenText)
        return RecognitionResult(spokenText, similarity, similarity * 0.9f, similarity * 0.85f)
    }

    override fun getEngineInfo(): EngineInfo {
        return EngineInfo("Vosk", "1.0", 50, false, listOf(EngineFeature.OFFLINE_RECOGNITION, EngineFeature.TONE_DETECTION))
    }

    override suspend fun shutdown() {}

    private fun calculateSimilarity(expected: String, actual: String): Float {
        val expectedNormalized = expected.lowercase().replace(" ", "")
        val actualNormalized = actual.lowercase().replace(" ", "")
        if (expectedNormalized == actualNormalized) return 1.0f
        val maxLen = maxOf(expectedNormalized.length, actualNormalized.length)
        if (maxLen == 0) return 1.0f
        var matches = 0
        for (i in 0 until minOf(expectedNormalized.length, actualNormalized.length)) {
            if (expectedNormalized[i] == actualNormalized[i]) matches++
        }
        return matches.toFloat() / maxLen
    }
}

class WhisperCppEngine(
    private val context: android.content.Context,
    private val modelPath: String
) : PronunciationEngine {
    override val name = "Whisper.cpp"
    override val isAvailable = java.io.File(modelPath).exists()
    override val supportedLanguages = listOf("zh", "en", "zh-CN", "zh-TW")

    override suspend fun initialize(): EngineResult {
        return kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            if (!isAvailable) {
                EngineResult.NotAvailable("Whisper model not found at $modelPath")
            } else {
                EngineResult.Success("Whisper.cpp initialized")
            }
        }
    }

    override suspend fun startListening(config: RecognitionConfig): Flow<RecognitionPartialResult> {
        return kotlinx.coroutines.flow.flow {
            emit(RecognitionPartialResult("", 0f, false))
        }
    }

    override suspend fun stopListening(): EngineResult = EngineResult.Success("Stopped")

    override suspend fun recognize(audioPath: String, expectedText: String, expectedPinyin: String): RecognitionResult {
        return RecognitionResult("", 0f, 0f, 0f, error = "Whisper.cpp integration pending")
    }

    override suspend fun recognizeFromText(spokenText: String, expectedText: String, expectedPinyin: String): RecognitionResult {
        val similarity = calculateSimilarity(expectedPinyin, spokenText)
        return RecognitionResult(spokenText, similarity, similarity * 0.9f, similarity * 0.85f)
    }

    override fun getEngineInfo(): EngineInfo {
        return EngineInfo("Whisper.cpp", "1.0", 100, false, listOf(EngineFeature.OFFLINE_RECOGNITION, EngineFeature.TONE_DETECTION, EngineFeature.STREAMING))
    }

    override suspend fun shutdown() {}

    private fun calculateSimilarity(expected: String, actual: String): Float {
        val expectedNormalized = expected.lowercase().replace(" ", "")
        val actualNormalized = actual.lowercase().replace(" ", "")
        if (expectedNormalized == actualNormalized) return 1.0f
        val maxLen = maxOf(expectedNormalized.length, actualNormalized.length)
        if (maxLen == 0) return 1.0f
        var matches = 0
        for (i in 0 until minOf(expectedNormalized.length, actualNormalized.length)) {
            if (expectedNormalized[i] == actualNormalized[i]) matches++
        }
        return matches.toFloat() / maxLen
    }
}