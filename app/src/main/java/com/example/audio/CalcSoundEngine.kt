package com.example.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

enum class CalcSoundType {
    DIGIT,
    OPERATOR,
    UTILITY,
    EQUALS
}

class CalcSoundEngine(context: Context) {

    private val appContext = context.applicationContext
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    @Volatile
    private var isReleased = false

    @Volatile
    private var digitSoundId: Int = 0

    @Volatile
    private var operatorSoundId: Int = 0

    @Volatile
    private var utilitySoundId: Int = 0

    @Volatile
    private var equalsSoundId: Int = 0

    private val soundPool: SoundPool? = runCatching {
        val attrs = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        SoundPool.Builder()
            .setMaxStreams(6)
            .setAudioAttributes(attrs)
            .build()
    }.getOrNull()

    init {
        if (soundPool != null) {
            scope.launch {
                runCatching {
                    val dir = File(appContext.cacheDir, "calc_sfx").apply { mkdirs() }
                    digitSoundId = loadTone(
                        file = File(dir, "tap_digit.wav"),
                        pcm = synthesizeSoftTap(freqHz = 620.0, harmonicHz = 1240.0, durationMs = 22, decayMs = 5.0)
                    )
                    operatorSoundId = loadTone(
                        file = File(dir, "tap_op.wav"),
                        pcm = synthesizeSoftTap(freqHz = 470.0, harmonicHz = 940.0, durationMs = 24, decayMs = 5.5)
                    )
                    utilitySoundId = loadTone(
                        file = File(dir, "tap_util.wav"),
                        pcm = synthesizeSoftTap(freqHz = 360.0, harmonicHz = 540.0, durationMs = 22, decayMs = 4.8)
                    )
                    equalsSoundId = loadTone(
                        file = File(dir, "tap_equals.wav"),
                        pcm = synthesizeEqualsChime()
                    )
                }
            }
        }
    }

    fun play(type: CalcSoundType, enabled: Boolean) {
        if (!enabled || isReleased) return
        val pool = soundPool ?: return
        val soundId = when (type) {
            CalcSoundType.DIGIT -> digitSoundId
            CalcSoundType.OPERATOR -> operatorSoundId
            CalcSoundType.UTILITY -> utilitySoundId
            CalcSoundType.EQUALS -> equalsSoundId
        }
        if (soundId != 0) {
            val vol = when (type) {
                CalcSoundType.DIGIT -> 0.22f
                CalcSoundType.OPERATOR -> 0.22f
                CalcSoundType.UTILITY -> 0.19f
                CalcSoundType.EQUALS -> 0.25f
            }
            pool.play(soundId, vol, vol, 1, 0, 1.0f)
        }
    }

    fun release() {
        isReleased = true
        runCatching { soundPool?.release() }
    }

    private fun loadTone(file: File, pcm: ShortArray): Int {
        writeWav(file, pcm)
        return soundPool?.load(file.absolutePath, 1) ?: 0
    }

    private fun synthesizeSoftTap(
        freqHz: Double,
        harmonicHz: Double,
        durationMs: Int,
        decayMs: Double
    ): ShortArray {
        val sampleRate = 44100
        val numSamples = (sampleRate * durationMs) / 1000
        val out = ShortArray(numSamples)
        val attackSamples = (sampleRate * 2) / 1000 // 2ms soft ramp-in prevents click artifacts
        val tau = decayMs / 1000.0

        for (i in 0 until numSamples) {
            val t = i.toDouble() / sampleRate
            val attack = if (i < attackSamples) {
                sin((PI / 2.0) * (i.toDouble() / attackSamples))
            } else {
                1.0
            }
            val env = attack * exp(-t / tau)
            val fundamental = sin(2.0 * PI * freqHz * t)
            val overtone = 0.22 * sin(2.0 * PI * harmonicHz * t)
            val sample = (fundamental + overtone) * env * 0.35 * Short.MAX_VALUE
            out[i] = sample.toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
        return out
    }

    private fun synthesizeEqualsChime(): ShortArray {
        val sampleRate = 44100
        val numSamples = (sampleRate * 65) / 1000
        val out = ShortArray(numSamples)
        val attackSamples = (sampleRate * 3) / 1000
        val secondNoteStart = (sampleRate * 18) / 1000

        for (i in 0 until numSamples) {
            val t = i.toDouble() / sampleRate
            val attack1 = if (i < attackSamples) sin((PI / 2.0) * (i.toDouble() / attackSamples)) else 1.0
            val n1 = sin(2.0 * PI * 523.25 * t) * attack1 * exp(-t / 0.012)

            val n2 = if (i >= secondNoteStart) {
                val t2 = (i - secondNoteStart).toDouble() / sampleRate
                val i2 = i - secondNoteStart
                val attack2 = if (i2 < attackSamples) sin((PI / 2.0) * (i2.toDouble() / attackSamples)) else 1.0
                sin(2.0 * PI * 659.25 * t2) * attack2 * exp(-t2 / 0.016)
            } else {
                0.0
            }

            val sample = (n1 * 0.65 + n2 * 0.75) * 0.32 * Short.MAX_VALUE
            out[i] = sample.toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
        return out
    }

    private fun writeWav(file: File, pcm: ShortArray) {
        val sampleRate = 44100
        val dataSize = pcm.size * 2
        val buffer = ByteBuffer.allocate(44 + dataSize).order(ByteOrder.LITTLE_ENDIAN)
        buffer.put("RIFF".toByteArray(Charsets.US_ASCII))
        buffer.putInt(36 + dataSize)
        buffer.put("WAVE".toByteArray(Charsets.US_ASCII))
        buffer.put("fmt ".toByteArray(Charsets.US_ASCII))
        buffer.putInt(16) // PCM chunk size
        buffer.putShort(1) // AudioFormat = PCM
        buffer.putShort(1) // Mono
        buffer.putInt(sampleRate)
        buffer.putInt(sampleRate * 2) // ByteRate
        buffer.putShort(2) // BlockAlign
        buffer.putShort(16) // BitsPerSample
        buffer.put("data".toByteArray(Charsets.US_ASCII))
        buffer.putInt(dataSize)
        for (s in pcm) {
            buffer.putShort(s)
        }
        FileOutputStream(file).use { it.write(buffer.array()) }
    }
}
