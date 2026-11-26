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
import java.util.Collections
import java.util.concurrent.ConcurrentHashMap
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

    private val soundMap = ConcurrentHashMap<CalcSoundType, Int>()
    private val loadedSoundIds: MutableSet<Int> = Collections.newSetFromMap(ConcurrentHashMap())

    private val soundPool: SoundPool? = runCatching {
        val attrs = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        SoundPool.Builder()
            .setMaxStreams(10)
            .setAudioAttributes(attrs)
            .build()
            .apply {
                setOnLoadCompleteListener { _, sampleId, status ->
                    if (status == 0 && sampleId > 0) {
                        loadedSoundIds.add(sampleId)
                    }
                }
            }
    }.getOrNull()

    init {
        if (soundPool != null) {
            scope.launch {
                runCatching {
                    val dir = File(appContext.cacheDir, "calc_sfx_soft_v1").apply { mkdirs() }
                    register(
                        CalcSoundType.DIGIT,
                        loadTone(File(dir, "soft_tap_digit.wav"), synthesizeSoftTap(640.0, 1280.0, 34, 8.5))
                    )
                    register(
                        CalcSoundType.OPERATOR,
                        loadTone(File(dir, "soft_tap_op.wav"), synthesizeSoftTap(490.0, 980.0, 36, 9.0))
                    )
                    register(
                        CalcSoundType.UTILITY,
                        loadTone(File(dir, "soft_tap_util.wav"), synthesizeSoftTap(380.0, 570.0, 34, 8.5))
                    )
                    register(
                        CalcSoundType.EQUALS,
                        loadTone(File(dir, "soft_tap_equals.wav"), synthesizeEqualsChime())
                    )
                }
            }
        }
    }

    private fun register(type: CalcSoundType, soundId: Int) {
        if (soundId > 0) {
            soundMap[type] = soundId
        }
    }

    fun play(
        type: CalcSoundType,
        enabled: Boolean,
        volume: Float = 0.75f
    ) {
        if (!enabled || isReleased) return
        val pool = soundPool ?: return
        val soundId = soundMap[type] ?: return
        if (soundId > 0 && loadedSoundIds.contains(soundId)) {
            val baseVol = when (type) {
                CalcSoundType.DIGIT -> 0.72f
                CalcSoundType.OPERATOR -> 0.76f
                CalcSoundType.UTILITY -> 0.68f
                CalcSoundType.EQUALS -> 0.88f
            }
            val vol = (baseVol * volume.coerceIn(0f, 1f)).coerceIn(0f, 1f)
            if (vol <= 0.005f) return
            pool.play(soundId, vol, vol, 1, 0, 1.0f)
        }
    }

    fun release() {
        isReleased = true
        loadedSoundIds.clear()
        soundMap.clear()
        runCatching { soundPool?.release() }
    }

    private fun loadTone(file: File, pcm: ShortArray): Int {
        val expectedBytes = 44L + pcm.size * 2L
        if (!file.exists() || file.length() != expectedBytes) {
            writeWav(file, pcm)
        }
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
        val attackSamples = (sampleRate * 2) / 1000
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
            val overtone = 0.25 * sin(2.0 * PI * harmonicHz * t)
            val sample = (fundamental + overtone) * env * 0.72 * Short.MAX_VALUE
            out[i] = sample.toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
        return out
    }

    private fun synthesizeEqualsChime(): ShortArray {
        val sampleRate = 44100
        val numSamples = (sampleRate * 85) / 1000
        val out = ShortArray(numSamples)
        val attackSamples = (sampleRate * 3) / 1000
        val secondNoteStart = (sampleRate * 22) / 1000

        for (i in 0 until numSamples) {
            val t = i.toDouble() / sampleRate
            val attack1 = if (i < attackSamples) sin((PI / 2.0) * (i.toDouble() / attackSamples)) else 1.0
            val n1 = sin(2.0 * PI * 523.25 * t) * attack1 * exp(-t / 0.018)

            val n2 = if (i >= secondNoteStart) {
                val i2 = i - secondNoteStart
                val t2 = i2.toDouble() / sampleRate
                val attack2 = if (i2 < attackSamples) sin((PI / 2.0) * (i2.toDouble() / attackSamples)) else 1.0
                sin(2.0 * PI * 659.25 * t2) * attack2 * exp(-t2 / 0.024)
            } else {
                0.0
            }

            val sample = (n1 * 0.65 + n2 * 0.78) * 0.68 * Short.MAX_VALUE
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
        buffer.putInt(16)
        buffer.putShort(1)
        buffer.putShort(1)
        buffer.putInt(sampleRate)
        buffer.putInt(sampleRate * 2)
        buffer.putShort(2)
        buffer.putShort(16)
        buffer.put("data".toByteArray(Charsets.US_ASCII))
        buffer.putInt(dataSize)
        for (s in pcm) {
            buffer.putShort(s)
        }
        FileOutputStream(file).use { it.write(buffer.array()) }
    }
}
