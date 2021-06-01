package com.example.miccentral

import android.content.Context
import android.content.Intent
import android.util.Log
import be.tarsos.dsp.AudioDispatcher
import be.tarsos.dsp.AudioEvent
import be.tarsos.dsp.AudioProcessor
import be.tarsos.dsp.io.android.AudioDispatcherFactory
import be.tarsos.dsp.pitch.PitchDetectionHandler
import be.tarsos.dsp.pitch.PitchProcessor
import kotlin.concurrent.thread
import kotlin.math.abs


class DSPRecorder(val context: Context) {
    private lateinit var dispatcher: AudioDispatcher

    private val voiceData = ArrayList<Byte>()
    private var voiceTimestamp: Long = 0
    private var pitchTimestamp: Long = 0

    private var isRecording = false
    private var isRecMode = false
    private var recRequest = false
    private var recRequestTimeStamp = 0L

    companion object {
        const val TAG = "DSPRecorder"
        const val INTENT_ACTION = "MIC_CENTRAL_REC_RESPONSE"

        const val PITCH_FREQ_MIN            = 100
        const val SAMPLE_RATE               = 44100
        const val BUFF_SIZE                 = 1024 * 7
        const val BITS_PER_SAMPLE: Short    = 16
        const val NUM_CHANNELS: Short       = 1
        const val BYTE_RATE                 = SAMPLE_RATE * NUM_CHANNELS * 16 / 8
    }

    fun setRecRequest(timestamp: Long) {
        recRequest = true
        recRequestTimeStamp = timestamp
    }

    fun handleRecRequest() {
        recRequest = false
        isRecMode = true

        pitchTimestamp = System.currentTimeMillis()
    }

    fun resetRecord() {
        isRecording = true
        voiceTimestamp = System.currentTimeMillis()
        voiceData.clear()

        for (byte in wavFileHeader())
            voiceData.add(byte)
    }

    fun startRecording() {
        dispatcher = AudioDispatcherFactory.fromDefaultMicrophone(SAMPLE_RATE, BUFF_SIZE, 0)

        val pitchDetectionHandler = PitchDetectionHandler { p0, p1 ->
            val pitch = p0?.pitch
            if (pitch!! >= PITCH_FREQ_MIN)
                pitchTimestamp = System.currentTimeMillis()

            if (!isRecording) {
                if (pitch >= PITCH_FREQ_MIN) {
                    resetRecord()
                    return@PitchDetectionHandler
                }
                if (recRequest) {
                    resetRecord()
                    handleRecRequest()
                    return@PitchDetectionHandler
                }
            }
        }

        val pitchProcessor = PitchProcessor(
                PitchProcessor.PitchEstimationAlgorithm.FFT_YIN,
                SAMPLE_RATE.toFloat(),
                BUFF_SIZE,
                pitchDetectionHandler
        )
        dispatcher.addAudioProcessor(pitchProcessor)

        val baseProcessor = object: AudioProcessor {
            override fun process(p0: AudioEvent?): Boolean {
                if (isRecording) {
                    Log.d(TAG, "recording enabled")

                    if (recRequest) {
                        resetRecord()
                        handleRecRequest()
                    }

                    for (byte in p0!!.byteBuffer)
                        voiceData.add(byte)

                    if (abs(System.currentTimeMillis() - pitchTimestamp) > 1500) {
                        isRecording = false
                        updateHeaderInformation(voiceData)
                        Log.d(TAG, voiceData.size.toString())

                        var filename = System.currentTimeMillis().toString() + ".wav"
                        if (isRecMode)
                            filename = "$recRequestTimeStamp.wav"

                        thread(true) {
                            val voiceData2 = voiceData.toList().toByteArray()
                            val fos = context.openFileOutput(filename, Context.MODE_PRIVATE)
                            fos.write(voiceData2)
                            fos.close()

                            val intent = Intent(INTENT_ACTION)
                            intent.putExtra("path", filename)
                            context.sendBroadcast(intent)
                        }

                        isRecMode = false
                    }
                }
                return true
            }

            override fun processingFinished() { }
        }
        dispatcher.addAudioProcessor(baseProcessor)

        Thread(dispatcher, "Audio Thread").start()
    }

    fun stopRecording() {
        if (!dispatcher.isStopped)
            dispatcher.stop()
    }

    private fun wavFileHeader(): ByteArray {
        val headerSize = 44
        val header = ByteArray(headerSize)

        header[0] = 'R'.toByte() // RIFF/WAVE header
        header[1] = 'I'.toByte()
        header[2] = 'F'.toByte()
        header[3] = 'F'.toByte()

        header[4] = (0 and 0xff).toByte() // Size of the overall file, 0 because unknown
        header[5] = (0 shr 8 and 0xff).toByte()
        header[6] = (0 shr 16 and 0xff).toByte()
        header[7] = (0 shr 24 and 0xff).toByte()

        header[8] = 'W'.toByte()
        header[9] = 'A'.toByte()
        header[10] = 'V'.toByte()
        header[11] = 'E'.toByte()

        header[12] = 'f'.toByte() // 'fmt ' chunk
        header[13] = 'm'.toByte()
        header[14] = 't'.toByte()
        header[15] = ' '.toByte()

        header[16] = 16 // Length of format data
        header[17] = 0
        header[18] = 0
        header[19] = 0

        header[20] = 1 // Type of format (1 is PCM)
        header[21] = 0

        header[22] = NUM_CHANNELS.toByte()
        header[23] = 0

        header[24] = (SAMPLE_RATE and 0xff).toByte() // Sampling rate
        header[25] = (SAMPLE_RATE shr 8 and 0xff).toByte()
        header[26] = (SAMPLE_RATE shr 16 and 0xff).toByte()
        header[27] = (SAMPLE_RATE shr 24 and 0xff).toByte()

        header[28] = (BYTE_RATE and 0xff).toByte() // Byte rate = (Sample Rate * BitsPerSample * Channels) / 8
        header[29] = (BYTE_RATE shr 8 and 0xff).toByte()
        header[30] = (BYTE_RATE shr 16 and 0xff).toByte()
        header[31] = (BYTE_RATE shr 24 and 0xff).toByte()

        header[32] = (NUM_CHANNELS * BITS_PER_SAMPLE / 8).toByte() //  16 Bits stereo
        header[33] = 0

        header[34] = BITS_PER_SAMPLE.toByte() // Bits per sample
        header[35] = 0

        header[36] = 'd'.toByte()
        header[37] = 'a'.toByte()
        header[38] = 't'.toByte()
        header[39] = 'a'.toByte()

        header[40] = (0 and 0xff).toByte() // Size of the data section.
        header[41] = (0 shr 8 and 0xff).toByte()
        header[42] = (0 shr 16 and 0xff).toByte()
        header[43] = (0 shr 24 and 0xff).toByte()

        return header
    }

    private fun updateHeaderInformation(data: ArrayList<Byte>) {
        val fileSize = data.size
        val contentSize = fileSize - 44

        data[4] = (fileSize and 0xff).toByte() // Size of the overall file
        data[5] = (fileSize shr 8 and 0xff).toByte()
        data[6] = (fileSize shr 16 and 0xff).toByte()
        data[7] = (fileSize shr 24 and 0xff).toByte()

        data[40] = (contentSize and 0xff).toByte() // Size of the data section.
        data[41] = (contentSize shr 8 and 0xff).toByte()
        data[42] = (contentSize shr 16 and 0xff).toByte()
        data[43] = (contentSize shr 24 and 0xff).toByte()
    }
}
