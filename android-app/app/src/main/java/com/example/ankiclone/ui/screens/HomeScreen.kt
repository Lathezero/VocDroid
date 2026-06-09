package com.example.ankiclone.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ankiclone.data.api.Deck
import com.example.ankiclone.data.api.RetrofitClient
import com.example.ankiclone.ui.components.AppScreen
import com.example.ankiclone.ui.components.DualActionRow
import com.example.ankiclone.ui.components.EmptyStateCard
import com.example.ankiclone.ui.components.GlassCard
import com.example.ankiclone.ui.components.InfoPill
import com.example.ankiclone.ui.components.ScreenHeader
import com.example.ankiclone.ui.components.SectionCard
import com.example.ankiclone.ui.components.StatChip
import kotlinx.coroutines.launch

@Composable
fun HomeScreen(
    onNavigateToStudy: (String) -> Unit,
    onNavigateToImport: () -> Unit,
    onNavigateToOcrImport: () -> Unit,
    modifier: Modifier = Modifier
) {
    var decks by remember { mutableStateOf<List<Deck>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        coroutineScope.launch {
            try {
                decks = RetrofitClient.apiService.getDecks()
            } catch (e: Exception) {
                errorMessage = e.message
            } finally {
                isLoading = false
            }
        }
    }

    AppScreen(modifier = modifier) {
        ScreenHeader(
            eyebrow = "主页",
            title = "我的牌组",
            subtitle = "查看当前账号下的牌组，支持普通导入和 OCR 识别导入。"
        )
        Spacer(modifier = Modifier.height(18.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            StatChip(label = "牌组数量", value = decks.size.toString(), modifier = Modifier.weight(1f))
            StatChip(
                label = "学习状态",
                value = if (isLoading) "同步中" else "已就绪",
                modifier = Modifier.weight(1f)
            )
        }
        Spacer(modifier = Modifier.height(18.dp))
        SectionCard(
            title = "快速开始",
            subtitle = "延续上次学习节奏，或者快速导入新的词表内容。"
        ) {
            DualActionRow(
                primaryText = "识别导入",
                onPrimaryClick = onNavigateToOcrImport,
                secondaryText = "导入牌组",
                onSecondaryClick = onNavigateToImport
            )
        }
        Spacer(modifier = Modifier.height(18.dp))

        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.secondary)
            }
        } else if (errorMessage != null) {
            EmptyStateCard(
                title = "加载失败",
                description = "无法获取牌组数据：$errorMessage",
                modifier = Modifier.weight(1f)
            )
        } else if (decks.isEmpty()) {
            EmptyStateCard(
                title = "暂无牌组",
                description = "点击下方按钮导入 JSON、Anki 牌组，或使用 OCR 识别导入。",
                modifier = Modifier.weight(1f)
            )
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(decks) { deck ->
                    GlassCard(
                        modifier = Modifier.clickable { onNavigateToStudy("${deck.id}") }
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            InfoPill(text = "进入学习")
                            Spacer(modifier = Modifier.height(14.dp))
                            Text(
                                text = deck.name,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = deck.description ?: "点击进入开始学习和复习",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}
