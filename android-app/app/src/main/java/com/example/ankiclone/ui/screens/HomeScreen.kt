package com.example.ankiclone.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import android.widget.Toast
import androidx.compose.ui.platform.LocalContext
import com.example.ankiclone.data.api.Deck
import com.example.ankiclone.data.api.RetrofitClient
import com.example.ankiclone.ui.components.AppScreen
import com.example.ankiclone.ui.components.BackTextButton
import com.example.ankiclone.ui.components.DualActionRow
import com.example.ankiclone.ui.components.EmptyStateCard
import com.example.ankiclone.ui.components.GlassCard
import com.example.ankiclone.ui.components.InfoPill
import com.example.ankiclone.ui.components.PrimaryButton
import com.example.ankiclone.ui.components.ScreenHeader
import com.example.ankiclone.ui.components.SecondaryButton
import com.example.ankiclone.ui.components.SectionCard
import com.example.ankiclone.ui.components.StatChip
import kotlinx.coroutines.launch

@Composable
fun HomeScreen(
    onNavigateToStudy: (String) -> Unit,
    onNavigateToSpell: (String) -> Unit,
    onNavigateToImport: () -> Unit,
    onNavigateToOcrImport: () -> Unit,
    onNavigateToCardManager: (Int, String) -> Unit,
    onNavigateToReviewDue: () -> Unit,
    modifier: Modifier = Modifier
) {
    var decks by remember { mutableStateOf<List<Deck>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var deckPendingDelete by remember { mutableStateOf<Deck?>(null) }
    var isDeleting by remember { mutableStateOf(false) }
    var dueCount by remember { mutableStateOf(0) }
    val context = LocalContext.current
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
        coroutineScope.launch {
            try {
                dueCount = RetrofitClient.apiService.getDueCards().size
            } catch (_: Exception) {
                dueCount = 0
            }
        }
    }

    AppScreen(modifier = modifier, scrollable = true) {
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

        SectionCard(
            title = "今日待复习",
            subtitle = "根据记忆曲线，到期和新词都会汇总在这里。"
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "$dueCount",
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (dueCount > 0) "张卡片等待复习" else "今天已全部复习完成",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (dueCount > 0) {
                    PrimaryButton(
                        text = "开始复习",
                        onClick = onNavigateToReviewDue
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(18.dp))

        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(240.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.secondary)
            }
        } else if (errorMessage != null) {
            EmptyStateCard(
                title = "加载失败",
                description = "无法获取牌组数据：$errorMessage"
            )
        } else if (decks.isEmpty()) {
            EmptyStateCard(
                title = "暂无牌组",
                description = "点击下方按钮导入 JSON、Anki 牌组，或使用 OCR 识别导入。"
            )
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                decks.forEach { deck ->
                    Box(modifier = Modifier.fillMaxWidth()) {
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
                                Spacer(modifier = Modifier.height(12.dp))
                                SecondaryButton(
                                    text = "管理卡片",
                                    onClick = { onNavigateToCardManager(deck.id, deck.name) },
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                SecondaryButton(
                                    text = "拼写测验",
                                    onClick = { onNavigateToSpell("${deck.id}") },
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                        // 右上角交叉删除按钮
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(12.dp)
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f))
                                .border(
                                    width = 1.dp,
                                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.28f),
                                    shape = CircleShape
                                )
                                .clickable { deckPendingDelete = deck },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "✕",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }

    val pendingDeck = deckPendingDelete
    if (pendingDeck != null) {
        AlertDialog(
            onDismissRequest = { if (!isDeleting) deckPendingDelete = null },
            title = { Text("删除牌组") },
            text = {
                Text("确定要删除牌组「${pendingDeck.name}」吗？该牌组下的所有卡片和复习记录都会被一并删除，且无法恢复。")
            },
            confirmButton = {
                PrimaryButton(
                    text = if (isDeleting) "删除中..." else "确认删除",
                    onClick = {
                        isDeleting = true
                        coroutineScope.launch {
                            try {
                                val response = RetrofitClient.apiService.deleteDeck(pendingDeck.id)
                                if (response.isSuccessful) {
                                    decks = decks.filterNot { it.id == pendingDeck.id }
                                    Toast.makeText(context, "已删除牌组", Toast.LENGTH_SHORT).show()
                                    deckPendingDelete = null
                                } else {
                                    Toast.makeText(context, "删除失败：${response.code()}", Toast.LENGTH_SHORT).show()
                                }
                            } catch (e: Exception) {
                                Toast.makeText(context, "删除失败：${e.message}", Toast.LENGTH_SHORT).show()
                            } finally {
                                isDeleting = false
                            }
                        }
                    },
                    enabled = !isDeleting
                )
            },
            dismissButton = {
                BackTextButton(text = "取消", onClick = { if (!isDeleting) deckPendingDelete = null })
            }
        )
    }
}
