package com.trigger.feature.gameassistant.audio

import android.media.*
import android.os.Process
import java.util.concurrent.atomic.AtomicBoolean

/** Bounded PCM frame processor kept separate for deterministic validation and reusable DSP paths. */
object PcmPitchShifter {
    fun shift(input:ShortArray,count:Int,pitchFactor:Float,output:ShortArray=ShortArray(count.coerceAtLeast(0))):ShortArray {
        require(count in 0..input.size) { "count must be within the input frame" }
        require(output.size>=count) { "output buffer is too small" }
        require(pitchFactor.isFinite() && pitchFactor in .7f..2f) { "pitch factor must be between 0.7 and 2.0" }
        if(count==0) return output
        for(i in 0 until count) {
            val position=i*pitchFactor; val base=position.toInt(); val a=base%count; val b=(a+1)%count; val fraction=position-base
            output[i]=(input[a]*(1f-fraction)+input[b]*fraction).toInt().coerceIn(Short.MIN_VALUE.toInt(),Short.MAX_VALUE.toInt()).toShort()
        }
        return output
    }
}

/** Audio capture and playback run on a dedicated audio-priority thread, never the UI thread. */
class FemaleVoiceDSP(private val sampleRate:Int=48000,private val pitchFactor:Float=1.42f):AutoCloseable {
    private val running=AtomicBoolean(false); private var worker:Thread?=null
    fun start() { if(!running.compareAndSet(false,true)) return; worker=Thread({ Process.setThreadPriority(Process.THREAD_PRIORITY_AUDIO);runPipeline() },"trigger-voice-dsp").also { it.start() } }
    private fun runPipeline() {
        val minIn=AudioRecord.getMinBufferSize(sampleRate,AudioFormat.CHANNEL_IN_MONO,AudioFormat.ENCODING_PCM_16BIT).coerceAtLeast(sampleRate/50*2)
        val minOut=AudioTrack.getMinBufferSize(sampleRate,AudioFormat.CHANNEL_OUT_MONO,AudioFormat.ENCODING_PCM_16BIT).coerceAtLeast(minIn)
        var input:AudioRecord?=null;var output:AudioTrack?=null
        try {
            input=AudioRecord(MediaRecorder.AudioSource.VOICE_RECOGNITION,sampleRate,AudioFormat.CHANNEL_IN_MONO,AudioFormat.ENCODING_PCM_16BIT,minIn)
            output=AudioTrack.Builder().setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ASSISTANCE_ACCESSIBILITY).setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build()).setAudioFormat(AudioFormat.Builder().setSampleRate(sampleRate).setEncoding(AudioFormat.ENCODING_PCM_16BIT).setChannelMask(AudioFormat.CHANNEL_OUT_MONO).build()).setBufferSizeInBytes(minOut).setTransferMode(AudioTrack.MODE_STREAM).build()
            input.startRecording();output.play();val frame=ShortArray(sampleRate/50);val transformed=ShortArray(frame.size)
            while(running.get()) { val count=input.read(frame,0,frame.size,AudioRecord.READ_BLOCKING);if(count<=0)continue;PcmPitchShifter.shift(frame,count,pitchFactor.coerceIn(.7f,2f),transformed);output.write(transformed,0,count,AudioTrack.WRITE_BLOCKING) }
        } catch (_:SecurityException) { running.set(false) } catch (_:IllegalStateException) { running.set(false) }
        finally { try { input?.stop() } catch (_:Exception) {};input?.release();try { output?.stop() } catch (_:Exception) {};output?.release() }
    }
    override fun close() { running.set(false);worker?.interrupt();worker?.join(500);worker=null }
}
