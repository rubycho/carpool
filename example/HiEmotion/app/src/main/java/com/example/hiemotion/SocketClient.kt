package com.example.hiemotion

import java.io.InputStream
import java.net.Socket
import java.nio.ByteBuffer

class SocketClient {
    companion object {
        private fun int2byte(i: Int): ByteArray {
            return byteArrayOf(
                (i and 0x000000FF).toByte(),
                (i shr 8).toByte(),
                (i shr 16).toByte(),
                (i shr 24).toByte()
            )
        }

        fun readFromStream(iStream: InputStream): String {
            val data = ArrayList<Byte>()

            var c = 0
            while (c != -1) {
                c = iStream.read()
                data.add(c.toByte())
            }

            data.removeLast()
            return String(data.toByteArray())
        }

        fun creatSocket(): Socket {
            return Socket(BuildConfig.SOCKET_HOST, BuildConfig.SOCKET_PORT)
        }

        fun sendWav(data: ByteArray): String {
            val socket = creatSocket()
            val oStream = socket.getOutputStream()
            val iStream = socket.getInputStream()

            oStream.write(byteArrayOf('W'.toByte()))

            oStream.write(int2byte(data.size))
            oStream.write(data)

            val emotion = readFromStream(iStream)

            iStream.close()
            oStream.close()
            socket.close()

            return emotion
        }

        fun sendConfidence(data: Float) {
            val socket = creatSocket()
            val oStream = socket.getOutputStream()

            oStream.write(byteArrayOf('C'.toByte()))
            oStream.write(ByteBuffer.allocate(4).putFloat(data).array())

            oStream.close()
            socket.close()
        }
    }
}