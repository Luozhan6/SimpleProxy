package com.simpleproxy.vpn

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.net.VpnService
import android.os.Build
import android.os.ParcelFileDescriptor
import androidx.core.app.NotificationCompat
import com.simpleproxy.MainActivity
import com.simpleproxy.data.ConnectionState
import com.simpleproxy.data.ProxyConfig
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.io.FileInputStream
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.util.concurrent.atomic.AtomicBoolean

class ProxyVpnService : VpnService() {

    companion object {
        private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
        val connectionState: StateFlow<ConnectionState> = _connectionState

        private val _currentIp = MutableStateFlow<String?>(null)
        val currentIp: StateFlow<String?> = _currentIp

        private var serviceInstance: ProxyVpnService? = null

        fun getInstance() = serviceInstance
    }

    private var vpnInterface: ParcelFileDescriptor? = null
    private val isRunning = AtomicBoolean(false)
    private var proxyConfig: ProxyConfig? = null
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    override fun onCreate() {
        super.onCreate()
        serviceInstance = this
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        @Suppress("DEPRECATION")
        val config = intent?.getParcelableExtra<ProxyConfig>("config")
        if (config == null) {
            stopSelf()
            return START_NOT_STICKY
        }
        proxyConfig = config

        startForeground(NOTIFICATION_ID, createNotification())
        startVpn()

        return START_STICKY
    }

    private fun startVpn() {
        scope.launch {
            try {
                _connectionState.value = ConnectionState.Connecting

                val builder = Builder()
                    .setSession("SimpleProxy")
                    .addAddress("10.0.0.2", 32)
                    .addRoute("0.0.0.0", 0)
                    .addDnsServer("8.8.8.8")
                    .addDnsServer("8.8.4.4")
                    .setMtu(1500)
                    .setBlocking(true)

                vpnInterface = builder.establish()

                if (vpnInterface == null) {
                    _connectionState.value = ConnectionState.Error("Failed to create VPN interface")
                    return@launch
                }

                isRunning.set(true)

                val socks5Client = Socks5Client(
                    proxyConfig!!.serverIp,
                    proxyConfig!!.port.toInt(),
                    proxyConfig!!.username,
                    proxyConfig!!.password
                )

                if (!socks5Client.connect()) {
                    _connectionState.value = ConnectionState.Error("Failed to connect to SOCKS5 server")
                    stopVpn()
                    return@launch
                }

                _connectionState.value = ConnectionState.Connected
                fetchCurrentIp()
                processVpnTraffic(socks5Client)

            } catch (e: Exception) {
                _connectionState.value = ConnectionState.Error(e.message ?: "Unknown error")
                stopVpn()
            }
        }
    }

    private fun processVpnTraffic(socks5Client: Socks5Client) {
        val vpnFd = vpnInterface ?: return
        val input = FileInputStream(vpnFd.fileDescriptor)
        val output = FileOutputStream(vpnFd.fileDescriptor)
        val buffer = ByteBuffer.allocate(32767)

        scope.launch {
            while (isRunning.get()) {
                try {
                    buffer.clear()
                    val length = input.read(buffer.array())

                    if (length > 0) {
                        buffer.limit(length)
                        val response = socks5Client.handlePacket(buffer)
                        if (response != null && response.hasRemaining()) {
                            output.write(response.array(), 0, response.remaining())
                        }
                    } else if (length < 0) {
                        break
                    }
                    delay(10)
                } catch (e: Exception) {
                    if (isRunning.get()) {
                        _connectionState.value = ConnectionState.Error(e.message ?: "Connection error")
                        break
                    }
                }
            }
        }
    }

    private fun stopVpn() {
        isRunning.set(false)
        try {
            vpnInterface?.close()
            vpnInterface = null
        } catch (e: Exception) {
        }
        _connectionState.value = ConnectionState.Disconnected
        _currentIp.value = null
    }

    override fun onDestroy() {
        serviceInstance = null
        stopVpn()
        scope.cancel()
        super.onDestroy()
    }

    override fun onStop() {
        stopVpn()
        super.onStop()
    }

    private fun fetchCurrentIp() {
        scope.launch {
            try {
                val url = java.net.URL("https://api.ipify.org")
                val connection = url.openConnection() as java.net.HttpURLConnection
                connection.requestMethod = "GET"
                connection.connectTimeout = 5000
                connection.readTimeout = 5000

                val reader = java.io.BufferedReader(java.io.InputStreamReader(connection.inputStream))
                val ip = reader.readLine()
                reader.close()

                _currentIp.value = ip
            } catch (e: Exception) {
                _currentIp.value = null
            }
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Proxy Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "SimpleProxy VPN Service"
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("SimpleProxy")
            .setContentText("VPN Connected")
            .setSmallIcon(android.R.drawable.ic_lock_lock)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private val NOTIFICATION_ID = 1
    private val CHANNEL_ID = "simpleproxy_channel"
}
