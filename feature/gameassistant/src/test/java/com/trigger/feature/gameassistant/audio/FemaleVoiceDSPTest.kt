package com.trigger.feature.gameassistant.audio

import org.junit.Assert.*
import org.junit.Test

class FemaleVoiceDSPTest {
    @Test fun shiftsFramesWithoutReadingOutsidePartialBuffer() {
        val input=shortArrayOf(0,1000,2000,3000,Short.MIN_VALUE,Short.MAX_VALUE)
        val output=ShortArray(8) { 123 }
        PcmPitchShifter.shift(input,4,1.42f,output)
        assertTrue(output.take(4).all { it in Short.MIN_VALUE..Short.MAX_VALUE })
        assertEquals(123.toShort(),output[4]);assertEquals(123.toShort(),output[7])
    }
    @Test fun acceptsSupportedPitchBoundsAndEmptyFrames() {
        val input=shortArrayOf(0,Short.MAX_VALUE,Short.MIN_VALUE)
        assertEquals(3,PcmPitchShifter.shift(input,3,.7f).size)
        assertEquals(3,PcmPitchShifter.shift(input,3,2f).size)
        assertEquals(0,PcmPitchShifter.shift(input,0,1.42f).size)
    }
    @Test fun rejectsInvalidFactorAndBufferBoundaries() {
        assertThrows(IllegalArgumentException::class.java) { PcmPitchShifter.shift(shortArrayOf(1),2,1f) }
        assertThrows(IllegalArgumentException::class.java) { PcmPitchShifter.shift(shortArrayOf(1),1,Float.NaN) }
        assertThrows(IllegalArgumentException::class.java) { PcmPitchShifter.shift(shortArrayOf(1),1,1f,ShortArray(0)) }
    }
    @Test fun frameProcessingIsSynchronousAndBoundedToFrameSize() {
        val input=ShortArray(960) { (it%200).toShort() };val start=System.nanoTime()
        val result=PcmPitchShifter.shift(input,input.size,1.42f)
        assertEquals(input.size,result.size)
        assertTrue("DSP frame helper should process a 20 ms frame promptly",System.nanoTime()-start<100_000_000L)
    }
}
