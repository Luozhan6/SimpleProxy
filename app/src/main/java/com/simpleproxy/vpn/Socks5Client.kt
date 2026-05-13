package com.simpleproxy.vpn

import java.io.InputStream
import java.io.OutputStream
import java.net.InetAddress
import java.net.Socket
import java.nio.ByteBuffer

class Socks5Client(
    private val serverIp: String,
    private val port: Int,
    private val username: String,
    private val password: String
) {
    private var socket: Socket? = null
    private var inputStream: InputStream? = null
    private var outputStream: OutputStream? = null

    companion object {
        private const val SOCKS_VERSION = 5.toByte()
        private const val AUTH_METHOD_NONE = 0.toByte()
        private const val AUTH_METHOD_PASSWORD = 2.toByte()
        private const val AUTH_VERSION = 1.toByte()
        private const val CMD_CONNECT = 1.toByte()
        private const val CMD_UDP_ASSOCIATE = 3.toByte()
        private const val ATYP_IPV4 = 1.toByte()
        private const val ATYP_DOMAIN = 3.toByte()
        private const val ATYP_IPV6 = 4.toByte()
        private const val REP_SUCCESS = 0.toByte()
    }

    fun connect(): Boolean {
        return try {
            socket = Socket(serverIp, port)
            socket?.soTimeout = 30000
            inputStream = socket?.getInputStream()
            outputStream = socket?.getOutputStream()

            if (!authenticate()) {
                return false
            }

            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    private fun authenticate(): Boolean {
        val authRequest = ByteBuffer.allocate(4)
        authRequest.put(SOCKS_VERSION)
        authRequest.put(2) // 2 authentication methods
        authRequest.put(AUTH_METHOD_NONE)
        authRequest.put(AUTH_METHOD_PASSWORD)
        outputStream?.write(authRequest.array(), 0, 4)

        val authResponse = ByteArray(2)
        val read = inputStream?.read(authResponse) ?: 0
        if (read < 2 || authResponse[0] != SOCKS_VERSION) {
            return false
        }

        val selectedMethod = authResponse[1]
        if (selectedMethod == AUTH_METHOD_PASSWORD) {
            return authenticateWithPassword()
        } else if (selectedMethod == AUTH_METHOD_NONE) {
            return true
        }

        return false
    }

    private fun authenticateWithPassword(): Boolean {
        val usernameBytes = username.toByteArray()
        val passwordBytes = password.toByteArray()

        val authPacket = ByteBuffer.allocate(3 + usernameBytes.size + passwordBytes.size)
        authPacket.put(AUTH_VERSION)
        authPacket.put(usernameBytes.size.toByte())
        authPacket.put(usernameBytes)
        authPacket.put(passwordBytes.size.toByte())
        authPacket.put(passwordBytes)

        outputStream?.write(authPacket.array(), 0, authPacket.position())
        outputStream?.flush()

        val authResponse = ByteArray(2)
        val read = inputStream?.read(authResponse) ?: 0
        return read >= 2 && authResponse[1] == 0.toByte()
    }

    fun handlePacket(buffer: ByteBuffer): ByteBuffer? {
        return try {
            val data = ByteArray(buffer.remaining())
            buffer.get(data)

            outputStream?.write(data)
            outputStream?.flush()

            val response = ByteArray(32767)
            val read = inputStream?.read(response) ?: 0

            if (read > 0) {
                ByteBuffer.wrap(response.copyOf(read))
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    fun close() {
        try {
            inputStream?.close()
            outputStream?.close()
            socket?.close()
        } catch (e: Exception) {
        }
    }
}
