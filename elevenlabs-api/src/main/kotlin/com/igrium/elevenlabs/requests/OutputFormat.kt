package com.igrium.elevenlabs.requests

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
public enum class OutputFormat {
    /**
     * Output format, mp3 with 22.05kHz sample rate at 32kbps
     */
    @SerialName("mp3_22050_32") MP3_22040_32,

    /**
     * Output format, mp3 with 44.1kHz sample rate at 32kbps
     */
    @SerialName("mp3_44100_32") MP3_44100_32,

    /**
     * Output format, mp3 with 44.1kHz sample rate at 64kbps
     */
    @SerialName("mp3_44100_64") MP3_44100_64,

    /**
     * Output format, mp3 with 44.1kHz sample rate at 96kbps
     */
    @SerialName("mp3_44100_96") MP3_44100_96,

    /**
     * Default output format, mp3 with 44.1kHz sample rate at 128kbps
     */
    @SerialName("mp3_44100_128") MP3_44100_128,

    /**
     * Output format, mp3 with 44.1kHz sample rate at 192kbps.
     */
    @SerialName("mp3_44100_192") MP3_44100_192,

    /**
     * PCM format (S16LE) with 16kHz sample rate.
     */
    @SerialName("pcm_16000") PCM_16000,

    /**
     * PCM format (S16LE) with 22.05kHz sample rate.
     */
    @SerialName("pcm_22050") PCM_22050,

    /**
     * PCM format (S16LE) with 24kHz sample rate.
     */
    @SerialName("pcm_24000") PCM_24000,

    /**
     * PCM format (S16LE) with 44.1kHz sample rate.
     * Requires you to be subscribed to Independent Publisher tier or above.
     */
    @SerialName("pcm_44100") PCM_44100,

    /**
     * μ-law format (sometimes written mu-law, often approximated as u-law) with 8kHz sample rate.
     * Note that this format is commonly used for Twilio audio inputs.
     */
    @SerialName("ulaw_8000") ULAW_8000;

    @OptIn(ExperimentalSerializationApi::class)
    public fun getSerialName(): String {
        return serializer().descriptor.getElementName(ordinal)
    }
}