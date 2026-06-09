package com.example.ankiclone.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.ankiclone.ui.components.AppScreen
import com.example.ankiclone.ui.components.BackTextButton
import com.example.ankiclone.ui.components.GlassCard
import com.example.ankiclone.ui.components.ScreenHeader
import com.example.ankiclone.ui.components.SectionCard

@Composable
fun AdminScreen(onNavigateBack: () -> Unit) {
    val users = listOf("张三 (普通用户)", "李四 (普通用户)", "admin (管理员)")
    val decks = listOf("张三: 英语四级", "张三: 考研政治", "李四: 物理公式")

    AppScreen(scrollable = true) {
        ScreenHeader(
            eyebrow = "管理员入口",
            title = "管理员面板",
            subtitle = "这里仍是占位内容，后续可继续接入真实后台管理数据。"
        )
        Spacer(modifier = Modifier.height(18.dp))
        SectionCard(title = "所有用户") {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(users) { user ->
                    GlassCard {
                        Text(
                            text = user,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        SectionCard(title = "用户牌组概览") {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(decks) { deck ->
                    GlassCard {
                        Text(
                            text = deck,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        BackTextButton(onClick = onNavigateBack)
    }
}
