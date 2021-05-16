package com.example.hiemotion

/**
 * Customized WavRecorder, original code from:
 * https://stackoverflow.com/questions/5245497/how-to-record-wav-format-file-in-android
 */

import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Log
import java.io.IOException
import java.net.Socket
import kotlin.concurrent.thread
import kotlin.experimental.and

/**
 * WavRecorder
 * Detect human voice and send 4s recorded speech segment to server
 */
class WavRecorder() {
    private var recorder: AudioRecord? = null
    private var isRecording = false

    private var recordingThread: Thread? = null

    fun startRecording() {
        recorder = AudioRecord(MediaRecorder.AudioSource.VOICE_RECOGNITION,
                RECORDER_SAMPLE_RATE, RECORDER_CHANNELS,
                RECORDER_AUDIO_ENCODING, 512)
        recorder?.startRecording()
        isRecording = true

        recordingThread = thread(true) {
            sendWavDataToServer()
        }
    }

    fun stopRecording() {
        recorder?.run {
            isRecording = false;
            stop()
            release()
            recordingThread = null
            recorder = null
        }
    }

    private fun short2byte(sData: ShortArray): ByteArray {
        val arrSize = sData.size
        val bytes = ByteArray(arrSize * 2)
        for (i in 0 until arrSize) {
            bytes[i * 2] = (sData[i] and 0x00FF).toByte()
            bytes[i * 2 + 1] = (sData[i].toInt() shr 8).toByte()
            sData[i] = 0
        }
        return bytes
    }

    private fun sendWavDataToServer() {
        val sData = ShortArray(BufferElements2Rec)
        val sDataAcc = arrayListOf<Short>()

        val data = arrayListOf<Byte>()
        for (byte in wavFileHeader()) {
            data.add(byte)
        }

        while (isRecording) {
            // gets the voice output from microphone to byte format
            recorder?.read(sData, 0, BufferElements2Rec)
            sDataAcc.addAll(sData.toTypedArray())

            try {
                val bData = short2byte(sData)
                for (byte in bData)
                    data.add(byte)
            } catch (e: IOException) {
                e.printStackTrace()
            }

            val elapsedTime = (data.size.toFloat() - 44) / (2 * RECORDER_SAMPLE_RATE.toFloat()) // val
            if (elapsedTime > 1.0) { // val
                updateHeaderInformation(data)

                /* calculate ZCR and Energy */
                var ZCR = 0.0F
                var EGY = 0.0F
                for (i: Int in 0 until sDataAcc.size-1) {
                    val curr = sDataAcc[i]
                    val next = sDataAcc[i+1]

                    if ((curr >= 0) && (next < 0)) ZCR++
                    if ((curr < 0) && (next >= 0)) ZCR++
                }

                for (i: Int in 0 until sDataAcc.size) {
                    EGY += sDataAcc[i] * sDataAcc[i]
                }

                ZCR /= sDataAcc.size
                ZCR *= 100

                EGY /= sDataAcc.size

                Log.d("ZCR: ", ZCR.toString())
                Log.d("EGY: ", EGY.toString())

                data.clear()
                for (byte in wavFileHeader()) {
                    data.add(byte)
                }
                sDataAcc.clear()
            }
        }
    }

    fun sendToServer(data: ArrayList<Byte>) {
        val copy = data.toByteArray()
        thread (true) {
            val socket = Socket(BuildConfig.SOCKET_HOST, BuildConfig.SOCKET_PORT)
            val oStream = socket.getOutputStream()
            oStream.write(copy)

            oStream.close()
            socket.close()
        }
    }

    /**
     * Constructs header for wav file format
     */
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

        header[22] = NUMBER_CHANNELS.toByte()
        header[23] = 0

        header[24] = (RECORDER_SAMPLE_RATE and 0xff).toByte() // Sampling rate
        header[25] = (RECORDER_SAMPLE_RATE shr 8 and 0xff).toByte()
        header[26] = (RECORDER_SAMPLE_RATE shr 16 and 0xff).toByte()
        header[27] = (RECORDER_SAMPLE_RATE shr 24 and 0xff).toByte()

        header[28] = (BYTE_RATE and 0xff).toByte() // Byte rate = (Sample Rate * BitsPerSample * Channels) / 8
        header[29] = (BYTE_RATE shr 8 and 0xff).toByte()
        header[30] = (BYTE_RATE shr 16 and 0xff).toByte()
        header[31] = (BYTE_RATE shr 24 and 0xff).toByte()

        header[32] = (NUMBER_CHANNELS * BITS_PER_SAMPLE / 8).toByte() //  16 Bits stereo
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

    companion object {
        const val RECORDER_SAMPLE_RATE = 44100
        const val RECORDER_CHANNELS: Int = android.media.AudioFormat.CHANNEL_IN_MONO
        const val RECORDER_AUDIO_ENCODING: Int = android.media.AudioFormat.ENCODING_PCM_16BIT
        const val BITS_PER_SAMPLE: Short = 16
        const val NUMBER_CHANNELS: Short = 1
        const val BYTE_RATE = RECORDER_SAMPLE_RATE * NUMBER_CHANNELS * 16 / 8

        var BufferElements2Rec = 1024
    }
}
