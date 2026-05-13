package com.simpleproxy.ui

import android.app.Application
import android.content.Intent
import android.net.VpnService
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.simpleproxy.data.ConnectionState
import com.simpleproxy.data.ProxyConfig
import com.simpleproxy.vpn.ProxyVpnService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val _serverIp = MutableStateFlow("")
    val serverIp: StateFlow<String> = _serverIp

    private val _port = MutableStateFlow("1080")
    val port: StateFlow<String> = _port

    private val _username = MutableStateFlow("")
    val username: StateFlow<String> = _username

    private val _password = MutableStateFlow("")
    val password: StateFlow<String> = _password

    val connectionState: StateFlow<ConnectionState> = ProxyVpnService.connectionState
    val currentIp: StateFlow<String?> = ProxyVpnService.currentIp

    private var vpnPermissionRequested = false

    fun updateServerIp(value: String) {
        _serverIp.value = value
    }

    fun updatePort(value: String) {
        _port.value = value
    }

    fun updateUsername(value: String) {
        _username.value = value
    }

    fun updatePassword(value: String) {
        _password.value = value
    }

    fun toggleConnection() {
        viewModelScope.launch {
            when (connectionState.value) {
                is ConnectionState.Connected,
                is ConnectionState.Connecting -> disconnect()
                else -> connect()
            }
        }
    }

    private fun connect() {
        val prepareIntent = VpnService.prepare(getApplication())
        if (prepareIntent != null && !vpnPermissionRequested) {
            vpnPermissionRequested = true
            return
        }
        vpnPermissionRequested = false

        val config = ProxyConfig(
            _serverIp.value,
            _port.value,
            _username.value,
            _password.value
        )

        val intent = Intent(getApplication(), ProxyVpnService::class.java).apply {
            putExtra("config", config)
        }
        getApplication<Application>().startService(intent)
    }

    private fun disconnect() {
        val intent = Intent(getApplication(), ProxyVpnService::class.java)
        getApplication<Application>().stopService(intent)
    }

    fun checkIp() {
        ProxyVpnService.getInstance()
    }
}
