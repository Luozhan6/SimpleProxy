package com.simpleproxy.ui

import android.net.VpnService
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.simpleproxy.MainActivity
import com.simpleproxy.data.ConnectionState

@Composable
fun MainScreen(
    viewModel: MainViewModel = viewModel()
) {
    val context = LocalContext.current
    val activity = context as? MainActivity

    val serverIp by viewModel.serverIp.collectAsState()
    val port by viewModel.port.collectAsState()
    val username by viewModel.username.collectAsState()
    val password by viewModel.password.collectAsState()
    val connectionState by viewModel.connectionState.collectAsState()
    val currentIp by viewModel.currentIp.collectAsState()

    val isConnected = connectionState is ConnectionState.Connected

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "SimpleProxy",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(32.dp))

        OutlinedTextField(
            value = serverIp,
            onValueChange = { viewModel.updateServerIp(it) },
            label = { Text("服务器IP") },
            enabled = !isConnected,
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = port,
            onValueChange = { viewModel.updatePort(it) },
            label = { Text("端口") },
            enabled = !isConnected,
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = username,
            onValueChange = { viewModel.updateUsername(it) },
            label = { Text("用户名") },
            enabled = !isConnected,
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { viewModel.updatePassword(it) },
            label = { Text("密码") },
            enabled = !isConnected,
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(32.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Button(
                onClick = {
                    val prepareIntent = VpnService.prepare(context)
                    if (prepareIntent != null) {
                        activity?.requestVpnPermission()
                    } else {
                        viewModel.toggleConnection()
                    }
                },
                enabled = connectionState !is ConnectionState.Connecting,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isConnected)
                        MaterialTheme.colorScheme.error
                    else
                        MaterialTheme.colorScheme.primary
                )
            ) {
                Text(
                    text = when (connectionState) {
                        is ConnectionState.Connecting -> "连接中..."
                        is ConnectionState.Connected -> "断开"
                        else -> "连接"
                    }
                )
            }

            OutlinedButton(
                onClick = { viewModel.checkIp() },
                enabled = isConnected
            ) {
                Text("检测IP")
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = when (connectionState) {
                is ConnectionState.Disconnected -> "未连接"
                is ConnectionState.Connecting -> "连接中..."
                is ConnectionState.Connected -> "已连接"
                is ConnectionState.Error -> "错误: ${(connectionState as ConnectionState.Error).message}"
            },
            color = when (connectionState) {
                is ConnectionState.Connected -> MaterialTheme.colorScheme.primary
                is ConnectionState.Error -> MaterialTheme.colorScheme.error
                else -> MaterialTheme.colorScheme.onBackground
            }
        )

        if (currentIp != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "当前IP: $currentIp",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
        }
    }
}
