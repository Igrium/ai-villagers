package com.igrium.aivillagers.com.igrium.elevenlabs.requests

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class VoiceSettings(
    @SerialName("stability")
    val stability: Double?,

    @SerialName("similarity_boost")
    val similarityBoost: Double?,

    @SerialName("style")
    val style: Double?,

    @SerialName("use_speaker_boost")
    val useSpeakerBoost: Boolean?
)

@Serializable
data class PronunciationDictionaryLocator(
    @SerialName("pronunciation_dictionary_id")
    val pronunciationDictionaryId: String,

    @SerialName("version_id")
    val versionId: String
)

@Serializable
data class TTSRequest(
    /**
     * The text that will get converted into speech.
     */
    @SerialName("text")
    val text: String,

    /**
     * Identifier of the model that will be used, you can query them using GET /v1/models.
     * The model needs to have support for text to speech, you can check this using the can_do_text_to_speech property.
     */
    @SerialName("model_id")
    val modelId: String? = null,

    /**
     * Language code (ISO 639-1) used to enforce a language for the model.
     * Currently only Turbo v2.5 supports language enforcement.
     * For other models, an error will be returned if language code is provided.
     */
    @SerialName("language_code")
    val languageCode: String? = null,

    /**
     * Voice settings overriding stored settings for the given voice. They are applied only on the given request.
     */
    @SerialName("voice_settings")
    val voiceSettings: VoiceSettings? = null,

    /**
     * A list of pronunciation dictionary locators (id, version_id) to be applied to the text.
     * They will be applied in order. You may have up to 3 locators per request
     */
    @SerialName("pronunciation_dictionary_locators")
    val pronunciationDictionaryLocators: List<PronunciationDictionaryLocator>? = null,

    /**
     * If specified, our system will make a best effort to sample deterministically, such that repeated requests with
     * the same seed and parameters should return the same result. Determinism is not guaranteed.
     * Must be integer between 0 and 4294967295.
     */
    @SerialName("seed")
    val seed: Int? = null,

    /**
     * The text that came before the text of the current request.
     * Can be used to improve the flow of prosody when concatenating together multiple generations
     * or to influence the prosody in the current generation.
     */
    @SerialName("previous_text")
    val previousText: String? = null,

    /**
     * The text that comes after the text of the current request.
     * Can be used to improve the flow of prosody when concatenating together multiple generations
     * or to influence the prosody in the current generation.
     */
    @SerialName("next_text")
    val nextText: String? = null,

    /**
     * A list of request_id of the samples that were generated before this generation.
     * Can be used to improve the flow of prosody when splitting up a large task into multiple requests.
     * The results will be best when the same model is used across the generations.
     * In case both previous_text and previous_request_ids is send, previous_text will be ignored.
     * A maximum of 3 request_ids can be send.
     */
    @SerialName("previous_request_ids")
    val previousRequestIds: List<String>? = null,

    /**
     * A list of request_id of the samples that were generated before this generation.
     * Can be used to improve the flow of prosody when splitting up a large task into multiple requests.
     * The results will be best when the same model is used across the generations.
     * In case both next_text and next_request_ids is send, next_text will be ignored.
     * A maximum of 3 request_ids can be send.
     */
    @SerialName("next_request_ids")
    val nextRequestIds: List<String>? = null,

    @Deprecated("Deprecated by ElevenLabs")
    @SerialName("use_pvc_as_ivc")
    val usePvcAsIvc: Boolean? = null
)