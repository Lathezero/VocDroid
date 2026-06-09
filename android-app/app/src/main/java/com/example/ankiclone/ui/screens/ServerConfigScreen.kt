package com.example.ankiclone.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.ankiclone.data.api.RetrofitClient
import com.example.ankiclone.ui.components.AppScreen
import com.example.ankiclone.ui.components.GlassCard
import com.example.ankiclone.ui.components.InfoPill
import com.example.ankiclone.ui.components.PrimaryButton
import com.example.ankiclone.ui.components.ScreenHeader
import com.example.ankiclone.ui.components.StatChip

@Composable
fun ServerConfigScreen(onNavigateToAuth: () -> Unit) {
    var serverUrl by remember { mutableStateOf("http://10.0.2.2:3000") }

    AppScreen {
        Column(
            verticalArrangement = Arrangement.Center
        ) {
            ScreenHeader(
                eyebrow = "本地服务器连接",
                title = "先配置服务地址",
                subtitle = "安卓模拟器默认使用 http://10.0.2.2:3000 访问你电脑上的本地 Node.js 服务。"
            )
            Spacer(modifier = Modifier.height(18.dp))
            InfoPill(text = "推荐使用模拟器回环地址")
            Spacer(modifier = Modifier.height(12.dp))
            StatChip(label = "推荐地址", value = "10.0.2.2:3000")
            Spacer(modifier = Modifier.height(18.dp))
            GlassCard {
                Column(modifier = Modifier.padding(20.dp)) {
                    OutlinedTextField(
                        value = serverUrl,
                        onValueChange = { serverUrl = it },
                        label = { Text("服务器地址") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "支持直接输入 IP:端口，应用会自动补全 http://",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    PrimaryButton(
                        text = "保存并继续",
                        onClick = {
                            RetrofitClient.updateBaseUrl(serverUrl)
                            onNavigateToAuth()
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}
