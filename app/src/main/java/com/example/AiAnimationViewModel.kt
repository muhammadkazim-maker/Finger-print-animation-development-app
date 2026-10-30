package com.example

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Content
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import org.json.JSONObject
import java.util.Locale
import kotlin.math.abs

private const val TAG = "AiAnimationViewModel"

sealed interface AiGenUiState {
    data object Idle : AiGenUiState
    data object Loading : AiGenUiState
    data class Success(val animation: SavedAiAnimation) : AiGenUiState
    data class Error(val message: String) : AiGenUiState
}

class AiAnimationViewModel : ViewModel() {

    private val animationSchema = Schema.obj(
        properties = mapOf(
            "title" to Schema.string(description = "Short 2 to 4 word animation title"),
            "styleBadge" to Schema.string(description = "Short 1 to 2 word style badge"),
            "primaryArchetype" to Schema.string(description = "Primary archetype: CLASSIC_RING, SCAN_LASER, PARTICLE_BURST, WAVE_RIPPLE, FIRE_CORONA, ICE_CRYSTAL, FIRE_AND_ICE, GALAXY_ORBIT, HEXAGON_CUBE, TRIANGLE_PYRAMID, SQUARE_HUD, DIAMOND_PRISM, BUTTERFLY_WINGS, HEART_PULSE, ROYAL_CROWN, CYBER_SHIELD, MATRIX_CIRCUIT, NATURE_BLOOM, WATER_DROP, INFINITY_DNA, SOUND_EQUALIZER, or PROGRESS_COMPASS"),
            "secondaryArchetype" to Schema.string(description = "Secondary overlay style: STAR_BLOOM, SCAN_LASER, POLYGON_HEX, or PARTICLE_RING"),
            "primaryColorHex" to Schema.string(description = "Primary 6-digit hex color like #00F5FF matching the image and prompt"),
            "secondaryColorHex" to Schema.string(description = "Secondary 6-digit hex color like #FF2A85 matching the image and prompt"),
            "accentColorHex" to Schema.string(description = "Accent 6-digit highlight hex color like #FFD740"),
            "customVerticesCsv" to Schema.string(description = "8 to 12 comma-separated x:y points in -85..85 tracing the custom shape silhouette around center (0,0), e.g. 0:-78,48:-42,76:0,48:42,0:78,-48:42,-76:0,-48:-42")
        )
    )

    private val structuredConfig = generationConfig {
        responseMimeType = "application/json"
        responseSchema = animationSchema
        maxOutputTokens = 260
        temperature = 0.65f
    }

    private val fastModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.5-flash-lite",
            generationConfig = structuredConfig
        )
    }

    private val defaultModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            generationConfig = structuredConfig
        )
    }

    private val streamModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.5-flash-lite",
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                maxOutputTokens = 260
                temperature = 0.65f
            }
        )
    }

    private val _uiState = MutableStateFlow<AiGenUiState>(AiGenUiState.Idle)
    val uiState: StateFlow<AiGenUiState> = _uiState.asStateFlow()

    fun resetState() {
        _uiState.value = AiGenUiState.Idle
    }

    fun generateAnimation(
        context: Context,
        userPrompt: String,
        referenceAttachments: List<ReferenceImageAttachment> = emptyList(),
        onSavedToCollection: (List<SavedAiAnimation>) -> Unit
    ) {
        val trimmed = userPrompt.trim()
        if (trimmed.isEmpty() && referenceAttachments.isEmpty()) {
            _uiState.value = AiGenUiState.Error("Please enter a prompt or attach at least one reference image.")
            return
        }

        viewModelScope.launch {
            _uiState.value = AiGenUiState.Loading
            try {
                val leadSig = referenceAttachments.firstOrNull()?.signature
                val imageContextNotes = if (leadSig != null) {
                    val c1Hex = ImageReferenceAnalyzer.formatHex(leadSig.primaryHex)
                    val c2Hex = ImageReferenceAnalyzer.formatHex(leadSig.secondaryHex)
                    val c3Hex = ImageReferenceAnalyzer.formatHex(leadSig.accentHex)
                    "Attached ${referenceAttachments.size} reference image(s). Detected visual palette: primary=$c1Hex, secondary=$c2Hex, accent=$c3Hex, geometry=${leadSig.shapeSummary}. Match the attached reference image's shape, glow style, and colors closely."
                } else {
                    ""
                }

                val effectiveUserDescription = when {
                    trimmed.isNotEmpty() && referenceAttachments.isNotEmpty() ->
                        "$trimmed (guided by ${referenceAttachments.size} reference image${if (referenceAttachments.size > 1) "s" else ""})"
                    trimmed.isNotEmpty() -> trimmed
                    else -> "Custom animation inspired by ${referenceAttachments.size} attached reference image${if (referenceAttachments.size > 1) "s" else ""} (${leadSig?.shapeSummary ?: "custom design"})"
                }

                val systemPrompt = """
                    Design a custom AMOLED fingerprint unlock animation matching the user's request and any attached reference image(s).
                    User prompt: "${trimmed.ifEmpty { "Replicate the attached reference image design, silhouette, and glowing colors as a fingerprint unlock animation." }}"
                    $imageContextNotes
                    Return JSON with keys:
                    - title (2-4 words)
                    - styleBadge (1-2 words)
                    - primaryArchetype (one of CLASSIC_RING, SCAN_LASER, PARTICLE_BURST, WAVE_RIPPLE, FIRE_CORONA, ICE_CRYSTAL, FIRE_AND_ICE, GALAXY_ORBIT, HEXAGON_CUBE, TRIANGLE_PYRAMID, SQUARE_HUD, DIAMOND_PRISM, BUTTERFLY_WINGS, HEART_PULSE, ROYAL_CROWN, CYBER_SHIELD, MATRIX_CIRCUIT, NATURE_BLOOM, WATER_DROP, INFINITY_DNA, SOUND_EQUALIZER, PROGRESS_COMPASS)
                    - secondaryArchetype (STAR_BLOOM, SCAN_LASER, POLYGON_HEX, or PARTICLE_RING)
                    - primaryColorHex (#RRGGBB)
                    - secondaryColorHex (#RRGGBB)
                    - accentColorHex (#RRGGBB)
                    - customVerticesCsv (8 to 12 comma-separated x:y points in -85..85 tracing the custom shape contour around center 0:0)
                """.trimIndent()

                val multimodalContent = content {
                    referenceAttachments.take(3).forEach { attachment ->
                        image(attachment.bitmap)
                    }
                    text(systemPrompt)
                }

                val jsonString = withTimeout(60_000L) {
                    requestAnimationJsonWithFallback(multimodalContent)
                }

                val json = JSONObject(jsonString)
                val title = json.optString("title", "AI Custom Pulse").take(36).ifBlank { "AI Custom Pulse" }
                val styleBadge = json.optString(
                    "styleBadge",
                    if (referenceAttachments.isNotEmpty()) "Image + AI" else "AI Custom"
                ).take(24).ifBlank { "AI Custom" }
                val primaryArch = json.optString("primaryArchetype", "CLASSIC_RING").ifBlank { "CLASSIC_RING" }

                val fallbackPrimary = leadSig?.primaryHex ?: 0xFF00E5FFL
                val fallbackSecondary = leadSig?.secondaryHex ?: 0xFF7C4DFFL
                val fallbackAccent = leadSig?.accentHex ?: 0xFFFFD740L

                val primaryHex = AnimationCatalog.parseHexColor(
                    json.optString("primaryColorHex", ImageReferenceAnalyzer.formatHex(fallbackPrimary)),
                    fallbackPrimary
                )
                val secondaryHex = AnimationCatalog.parseHexColor(
                    json.optString("secondaryColorHex", ImageReferenceAnalyzer.formatHex(fallbackSecondary)),
                    fallbackSecondary
                )
                val accentHex = if (json.has("accentColorHex")) {
                    AnimationCatalog.parseHexColor(
                        json.optString("accentColorHex", ImageReferenceAnalyzer.formatHex(fallbackAccent)),
                        fallbackAccent
                    )
                } else {
                    leadSig?.accentHex ?: blendAccentHex(primaryHex, secondaryHex)
                }

                val promptLower = effectiveUserDescription.lowercase(Locale.US)
                val seed = abs(effectiveUserDescription.hashCode())
                val secondaryArch = json.optString("secondaryArchetype", "").ifBlank {
                    when {
                        promptLower.contains("scan") || promptLower.contains("laser") -> "SCAN_LASER"
                        promptLower.contains("hex") || promptLower.contains("polygon") || promptLower.contains("triangle") -> "POLYGON_HEX"
                        promptLower.contains("star") || promptLower.contains("fire") || promptLower.contains("bloom") || promptLower.contains("crystal") -> "STAR_BLOOM"
                        else -> "PARTICLE_RING"
                    }
                }

                val aiVertices = ImageReferenceAnalyzer.parseVerticesCsv(
                    json.optString("customVerticesCsv", "")
                )
                val finalContourPoints = when {
                    leadSig != null && aiVertices.size >= 6 -> {
                        // Blend image-traced silhouette with Gemini's semantic shape vertices
                        blendContours(leadSig.contourPoints, aiVertices)
                    }
                    leadSig != null && leadSig.contourPoints.isNotEmpty() -> leadSig.contourPoints
                    aiVertices.isNotEmpty() -> aiVertices
                    else -> emptyList()
                }

                val strokeWidth = json.optInt("strokeWidth", 5 + (seed % 3)).coerceIn(3, 8)
                val starPoints = json.optInt("starPoints", 8 + ((seed / 3) % 10)).coerceIn(4, 24)
                val dashLength = json.optInt("dashLength", 12 + ((seed / 7) % 18)).coerceIn(4, 36)
                val gapLength = json.optInt("gapLength", 8 + ((seed / 11) % 10)).coerceIn(4, 22)
                val clockwise = !promptLower.contains("counter")

                val lottieJson = AnimationCatalog.buildAiCustomLottieJson(
                    title = title,
                    primaryArchetypeName = primaryArch,
                    secondaryArchetypeName = secondaryArch,
                    primaryHex = primaryHex,
                    secondaryHex = secondaryHex,
                    accentHex = accentHex,
                    strokeWidth = strokeWidth,
                    starPoints = starPoints,
                    dashLen = dashLength,
                    gapLen = gapLength,
                    clockwise = clockwise,
                    customContourPoints = finalContourPoints
                )

                val now = System.currentTimeMillis()
                val savedItem = SavedAiAnimation(
                    id = "ai_custom_$now",
                    title = title,
                    prompt = effectiveUserDescription,
                    styleBadge = styleBadge,
                    primaryHex = primaryHex,
                    secondaryHex = secondaryHex,
                    createdAtMs = now,
                    lottieJson = lottieJson
                )

                val updatedList = MyCollectionStore.saveNew(context, savedItem)
                onSavedToCollection(updatedList)
                _uiState.value = AiGenUiState.Success(savedItem)
            } catch (e: TimeoutCancellationException) {
                Log.e(TAG, "AI Animation generation timed out", e)
                _uiState.value = AiGenUiState.Error("Generation timed out after 60 seconds. Please try again.")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Firebase AI animation generation failed", e)
                _uiState.value = AiGenUiState.Error(e.localizedMessage ?: "Failed to generate animation.")
            }
        }
    }

    private suspend fun requestAnimationJsonWithFallback(inputContent: Content): String {
        // Stage 1: Ultra-low-latency Gemini 3.5 Flash-Lite with multimodal input
        try {
            val fastResponse = withTimeout(18_000L) {
                fastModel.generateContent(inputContent)
            }
            val text = fastResponse.text?.trim()
            if (!text.isNullOrEmpty()) {
                return text
            }
        } catch (e: TimeoutCancellationException) {
            Log.w(TAG, "gemini-3.5-flash-lite timed out, falling back to gemini-3.8-flash", e)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "gemini-3.5-flash-lite failed, falling back to gemini-3.8-flash", e)
        }

        // Stage 2: Default Gemini 3.8 Flash with multimodal input
        try {
            val defaultResponse = withTimeout(22_000L) {
                defaultModel.generateContent(inputContent)
            }
            val text = defaultResponse.text?.trim()
            if (!text.isNullOrEmpty()) {
                return text
            }
        } catch (e: TimeoutCancellationException) {
            Log.w(TAG, "gemini-3.8-flash timed out, falling back to streaming inference", e)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "gemini-3.8-flash failed, falling back to streaming inference", e)
        }

        // Stage 3: Streaming inference to minimize Time-to-First-Token
        val accumulated = StringBuilder()
        streamModel.generateContentStream(inputContent).collect { chunk ->
            accumulated.append(chunk.text.orEmpty())
        }
        val raw = accumulated.toString().trim()
        if (raw.isEmpty()) {
            throw IllegalStateException("Model returned an empty response.")
        }
        val startIdx = raw.indexOf('{')
        val endIdx = raw.lastIndexOf('}')
        return if (startIdx >= 0 && endIdx > startIdx) {
            raw.substring(startIdx, endIdx + 1)
        } else {
            raw
        }
    }

    private fun blendContours(
        imagePoints: List<Pair<Float, Float>>,
        aiPoints: List<Pair<Float, Float>>
    ): List<Pair<Float, Float>> {
        if (imagePoints.isEmpty()) return aiPoints
        if (aiPoints.isEmpty()) return imagePoints
        val count = imagePoints.size
        return List(count) { i ->
            val imgPt = imagePoints[i]
            val aiPt = aiPoints[(i * aiPoints.size) / count]
            (imgPt.first * 0.55f + aiPt.first * 0.45f) to (imgPt.second * 0.55f + aiPt.second * 0.45f)
        }
    }

    private fun blendAccentHex(c1: Long, c2: Long): Long {
        val r1 = (c1 shr 16) and 0xFF
        val g1 = (c1 shr 8) and 0xFF
        val b1 = c1 and 0xFF
        val r2 = (c2 shr 16) and 0xFF
        val g2 = (c2 shr 8) and 0xFF
        val b2 = c2 and 0xFF
        val r = ((r1 + r2) / 2 + 45).coerceAtMost(255)
        val g = ((g1 + g2) / 2 + 45).coerceAtMost(255)
        val b = ((b1 + b2) / 2 + 45).coerceAtMost(255)
        return 0xFF000000L or (r shl 16) or (g shl 8) or b
    }
}
