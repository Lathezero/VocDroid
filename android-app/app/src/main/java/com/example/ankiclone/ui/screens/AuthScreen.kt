package com.example.ankiclone.ui.screens

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.ankiclone.data.api.AuthRequest
import com.example.ankiclone.data.api.RetrofitClient
import com.example.ankiclone.ui.components.AppScreen
import com.example.ankiclone.ui.components.GlassCard
import com.example.ankiclone.ui.components.InfoPill
import com.example.ankiclone.ui.components.PrimaryButton
import com.example.ankiclone.ui.components.ScreenHeader
import com.example.ankiclone.ui.components.SecondaryButton
import com.example.ankiclone.ui.components.StatChip
import kotlinx.coroutines.launch
import org.json.JSONObject
import retrofit2.HttpException

@Composable
fun AuthScreen(onNavigateToHome: (String) -> Unit) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isLogin by remember { mutableStateOf(true) }
    var isLoading by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    fun parseAuthError(error: Throwable): String {
        if (error is HttpException) {
            val serverMessage = runCatching {
                val rawBody = error.response()?.errorBody()?.string().orEmpty()
                JSONObject(rawBody).optString("error").takeIf { it.isNotBlank() }
            }.getOrNull()

            return when (error.code()) {
                401 -> "账号不存在或密码错误，请先注册或检查密码"
                409 -> "用户名已存在，请直接登录或更换用户名"
                400 -> serverMessage ?: "请输入用户名和密码"
                else -> serverMessage ?: "服务器返回错误: ${error.code()}"
            }
        }

        return error.message?.takeIf { it.isNotBlank() } ?: "网络请求失败"
    }

    AppScreen {
        Column(
            verticalArrangement = Arrangement.Center
        ) {
            ScreenHeader(
                eyebrow = if (isLogin) "欢迎回来" else "创建新账户",
                title = if (isLogin) "登录开始学习" else "注册并同步数据",
                subtitle = "保留本地服务器同步能力，管理员和普通用户都从这里进入。"
            )
            Spacer(modifier = Modifier.height(20.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                StatChip(
                    label = "账号模式",
                    value = if (isLogin) "登录" else "注册",
                    modifier = Modifier.weight(1f)
                )
                StatChip(
                    label = "同步能力",
                    value = "本地服务",
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(modifier = Modifier.height(18.dp))
            GlassCard {
                Column(modifier = Modifier.padding(20.dp)) {
                    InfoPill(text = if (isLogin) "输入账号继续学习" else "创建账号后自动登录")
                    Spacer(modifier = Modifier.height(14.dp))
                    OutlinedTextField(
                        value = username,
                        onValueChange = { username = it },
                        label = { Text("用户名") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("密码") },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = if (isLogin) "登录后可直接查看牌组、统计和个人页面。" else "注册成功后会自动完成登录。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    PrimaryButton(
                        text = if (isLoading) "处理中..." else if (isLogin) "登录" else "注册并登录",
                        onClick = {
                            if (username.isBlank() || password.isBlank()) {
                                Toast.makeText(context, "请输入用户名和密码", Toast.LENGTH_SHORT).show()
                                return@PrimaryButton
                            }
                            isLoading = true
                            coroutineScope.launch {
                                try {
                                    val request = AuthRequest(username, password)
                                    val response = if (isLogin) {
                                        RetrofitClient.apiService.login(request)
                                    } else {
                                        RetrofitClient.apiService.register(request)
                                        RetrofitClient.apiService.login(request)
                                    }
                                    RetrofitClient.authToken = response.token
                                    onNavigateToHome(response.user.role)
                                } catch (e: Exception) {
                                    Toast.makeText(context, parseAuthError(e), Toast.LENGTH_LONG).show()
                                } finally {
                                    isLoading = false
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isLoading
                    )
                    if (isLoading) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center
                        ) {
                            CircularProgressIndicator(
                                color = MaterialTheme.colorScheme.secondary,
                                strokeWidth = 2.dp
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    SecondaryButton(
                        text = if (isLogin) "切换到注册" else "切换到登录",
                        onClick = { isLogin = !isLogin },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isLoading
                    )
                }
            }
        }
    }
}
