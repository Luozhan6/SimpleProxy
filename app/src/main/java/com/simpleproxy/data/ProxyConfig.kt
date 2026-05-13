package com.simpleproxy.data

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class ProxyConfig(
    val serverIp: String = "",
    val port: String = "1080",
    val username: String = "",
    val password: String = ""
) : Parcelable {
    fun isValid(): Boolean = serverIp.isNotBlank() && port.isNotBlank() &&
            username.isNotBlank() && password.isNotBlank()
}
