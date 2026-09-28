package com.trigger.feature.gameassistant.audio

import android.media.*
import android.os.Process
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.max

/** Low-latency PCM monitor DSP. Requires RECORD_AUDIO; routes only to the app's audio output. */
class FemaleVoiceDSP(private val sampleRate: Int = 48000, private val pitchFactor: Float = 1.42f) : AutoCloseable {
    private val running = AtomicBoolean(false); private var worker: Thread? = null
    fun start() { if (!running.compareAndSet(false,true)) return
        worker = Thread({ Process.setThreadPriority(Process.THREAD_PRIORITY_AUDIO); runPipeline() }, "trigger-voice-dsp").also { it.start() }
    }
    private fun runPipeline() {
        val minIn = AudioRecord.getMinBufferSize(sampleRate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT).coerceAtLeast(sampleRate/50*2)
        val minOut = AudioTrack.getMinBufferSize(sampleRate, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT).coerceAtLeast(minIn)
        var input: AudioRecord? = null; var output: AudioTrack? = null
        try {
            input = AudioRecord(MediaRecorder.AudioSource.VOICE_RECOGNITION, sampleRate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, minIn)
            output = AudioTrack.Builder().setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ASSISTANCE_ACCESSIBILITY).setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build()).setAudioFormat(AudioFormat.Builder().setSampleRate(sampleRate).setEncoding(AudioFormat.ENCODING_PCM_16BIT).setChannelMask(AudioFormat.CHANNEL_OUT_MONO).build()).setBufferSizeInBytes(minOut).setTransferMode(AudioTrack.MODE_STREAM).build()
            input.startRecording(); output.play(); val src = ShortArray(sampleRate/50); val dst = ShortArray(src.size)
            while (running.get()) { val count = input.read(src,0,src.size,AudioRecord.READ_BLOCKING); if (count <= 0) continue
                // Linear resampling shifts pitch while retaining 20ms chunks. This is a light pitch shift,
                // not a formant-preserving vocoder; callers should not route monitoring back to mic input.
                val factor = pitchFactor.coerceIn(.7f,2f); for (i in 0 until count) { val pos = i*factor; val a = (pos.toInt() % count); val b = (a+1)%count; val frac=pos-pos.toInt(); dst[i] = (src[a]*(1-frac)+src[b]*frac).toInt().coerceIn(Short.MIN_VALUE.toInt(),Short.MAX_VALUE.toInt()).toShort() }
                output.write(dst,0,count,AudioTrack.WRITE_BLOCKING)
            }
        } catch (_: SecurityException) { running.set(false) } catch (_: IllegalStateException) { running.set(false) } finally { try { input?.stop() } catch (_: Exception) {}; input?.release(); try { output?.stop() } catch (_: Exception) {}; output?.release() }
    }
    override fun close() { running.set(false); worker?.interrupt(); worker?.join(500); worker = null }
}
