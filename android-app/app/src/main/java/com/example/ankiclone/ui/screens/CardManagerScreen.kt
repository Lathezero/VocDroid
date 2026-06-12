package com.example.ankiclone.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ankiclone.data.api.AddCardsRequest
import com.example.ankiclone.data.api.Card
import com.example.ankiclone.data.api.CardRequest
import com.example.ankiclone.data.api.RetrofitClient
import com.example.ankiclone.data.api.UpdateCardRequest
import com.example.ankiclone.ui.components.AppScreen
import com.example.ankiclone.ui.components.BackTextButton
import com.example.ankiclone.ui.components.EmptyStateCard
import com.example.ankiclone.ui.components.GlassCard
import com.example.ankiclone.ui.components.PrimaryButton
import com.example.ankiclone.ui.components.ScreenHeader
import com.example.ankiclone.ui.components.SecondaryButton
import com.example.ankiclone.ui.components.StatChip
import kotlinx.coroutines.launch

private data class StatusFilter(val key: String?, val label: String)

private val statusFilters = listOf(
    StatusFilter(null, "全部"),
    StatusFilter("new", "新词"),
    StatusFilter("learning", "学习中"),
    StatusFilter("review", "待复习")
)

private fun statusLabel(status: String?): String = when (status) {
    "new" -> "新词"
    "learning" -> "学习中"
    "review" -> "待复习"
    else -> status ?: "未知"
}

private fun parseErrorMessage(rawMessage: String?, fallback: String): String {
    return rawMessage
        ?.takeIf { it.isNotBlank() }
        ?.let { message ->
            Regex(""""error"\s*:\s*"([^"]+)"""").find(message)?.groupValues?.getOrNull(1) ?: message
        }
        ?: fallback
}

@Composable
fun CardManagerScreen(
    deckId: Int,
    deckName: String,
    onNavigateBack: () -> Unit
) {
    var cards by remember { mutableStateOf<List<Card>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var query by remember { mutableStateOf("") }
    var activeFilter by remember { mutableStateOf<String?>(null) }

    // null = 不显示弹窗；编辑时存目标卡，新增时用哨兵
    var editingCard by remember { mutableStateOf<Card?>(null) }
    var showAddDialog by remember { mutableStateOf(false) }
    var cardPendingDelete by remember { mutableStateOf<Card?>(null) }

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    suspend fun reload() {
        try {
            cards = RetrofitClient.apiService.getAllCardsForDeck(deckId)
        } catch (e: Exception) {
            Toast.makeText(context, "加载卡片失败：${e.message ?: e.javaClass.simpleName}", Toast.LENGTH_LONG).show()
        } finally {
            isLoading = false
        }
    }

    LaunchedEffect(deckId) {
        reload()
    }

    val filteredCards = remember(cards, query, activeFilter) {
        val q = query.trim().lowercase()
        cards.filter { card ->
            (activeFilter == null || card.status == activeFilter) &&
                (q.isEmpty() ||
                    card.front_content.lowercase().contains(q) ||
                    card.back_content.lowercase().contains(q))
        }
    }

    AppScreen {
        ScreenHeader(
            eyebrow = "卡片管理",
            title = deckName,
            subtitle = "搜索、筛选、新增、编辑或删除该牌组中的单词卡片。"
        )
        Spacer(modifier = Modifier.height(16.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            StatChip(label = "卡片总数", value = cards.size.toString(), modifier = Modifier.weight(1f))
            StatChip(label = "当前筛选", value = filteredCards.size.toString(), modifier = Modifier.weight(1f))
        }
        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            label = { Text("搜索单词或释义") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(12.dp))

        // 状态筛选 chip 行
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            statusFilters.forEach { filter ->
                val selected = activeFilter == filter.key
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(14.dp))
                        .background(
                            if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.22f)
                            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        )
                        .border(
                            width = 1.dp,
                            color = if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                            else MaterialTheme.colorScheme.outline.copy(alpha = 0.24f),
                            shape = RoundedCornerShape(14.dp)
                        )
                        .clickable { activeFilter = filter.key }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = filter.label,
                        style = MaterialTheme.typography.labelMedium,
                        color = if (selected) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(14.dp))

        PrimaryButton(
            text = "新增卡片",
            onClick = { showAddDialog = true },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(14.dp))

        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.secondary)
            }
        } else if (filteredCards.isEmpty()) {
            EmptyStateCard(
                title = if (cards.isEmpty()) "暂无卡片" else "没有匹配的卡片",
                description = if (cards.isEmpty()) "点击上方“新增卡片”来添加第一张卡片。" else "换个关键词或筛选条件再试试。",
                modifier = Modifier.weight(1f)
            )
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(filteredCards, key = { it.id }) { card ->
                    GlassCard {
                        Column(modifier = Modifier.padding(18.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = card.front_content,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    text = statusLabel(card.status),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = card.back_content,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (!card.part_of_speech.isNullOrBlank()) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "词性：${card.part_of_speech}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                SecondaryButton(
                                    text = "编辑",
                                    onClick = { editingCard = card },
                                    modifier = Modifier.weight(1f)
                                )
                                SecondaryButton(
                                    text = "删除",
                                    onClick = { cardPendingDelete = card },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        BackTextButton(text = "返回牌组列表", onClick = onNavigateBack)
    }

    // 新增卡片弹窗
    if (showAddDialog) {
        CardEditDialog(
            title = "新增卡片",
            initialFront = "",
            initialBack = "",
            initialPos = "",
            onDismiss = { showAddDialog = false },
            onConfirm = { front, back, pos ->
                coroutineScope.launch {
                    try {
                        val response = RetrofitClient.apiService.addCardsToDeck(
                            deckId,
                            AddCardsRequest(listOf(CardRequest(front = front, back = back, partOfSpeech = pos)))
                        )
                        if (response.isSuccessful) {
                            Toast.makeText(context, "已添加", Toast.LENGTH_SHORT).show()
                            showAddDialog = false
                            reload()
                        } else {
                            Toast.makeText(
                                context,
                                parseErrorMessage(response.errorBody()?.string(), "添加失败"),
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    } catch (e: Exception) {
                        Toast.makeText(context, "添加失败：${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        )
    }

    // 编辑卡片弹窗
    val cardBeingEdited = editingCard
    if (cardBeingEdited != null) {
        CardEditDialog(
            title = "编辑卡片",
            initialFront = cardBeingEdited.front_content,
            initialBack = cardBeingEdited.back_content,
            initialPos = cardBeingEdited.part_of_speech ?: "",
            onDismiss = { editingCard = null },
            onConfirm = { front, back, pos ->
                coroutineScope.launch {
                    try {
                        val response = RetrofitClient.apiService.updateCard(
                            cardBeingEdited.id,
                            UpdateCardRequest(front = front, back = back, partOfSpeech = pos)
                        )
                        if (response.isSuccessful) {
                            Toast.makeText(context, "已保存", Toast.LENGTH_SHORT).show()
                            editingCard = null
                            reload()
                        } else {
                            Toast.makeText(
                                context,
                                parseErrorMessage(response.errorBody()?.string(), "保存失败"),
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    } catch (e: Exception) {
                        Toast.makeText(context, "保存失败：${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        )
    }

    // 删除确认弹窗
    val deleteTarget = cardPendingDelete
    if (deleteTarget != null) {
        AlertDialog(
            onDismissRequest = { cardPendingDelete = null },
            title = { Text("删除卡片") },
            text = { Text("确定要删除卡片「${deleteTarget.front_content}」吗？此操作无法恢复。") },
            confirmButton = {
                PrimaryButton(
                    text = "确认删除",
                    onClick = {
                        coroutineScope.launch {
                            try {
                                val response = RetrofitClient.apiService.deleteCard(deleteTarget.id)
                                if (response.isSuccessful) {
                                    Toast.makeText(context, "已删除", Toast.LENGTH_SHORT).show()
                                    cardPendingDelete = null
                                    reload()
                                } else {
                                    Toast.makeText(
                                        context,
                                        parseErrorMessage(response.errorBody()?.string(), "删除失败"),
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            } catch (e: Exception) {
                                Toast.makeText(context, "删除失败：${e.message}", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                )
            },
            dismissButton = {
                BackTextButton(text = "取消", onClick = { cardPendingDelete = null })
            }
        )
    }
}

@Composable
private fun CardEditDialog(
    title: String,
    initialFront: String,
    initialBack: String,
    initialPos: String,
    onDismiss: () -> Unit,
    onConfirm: (front: String, back: String, pos: String?) -> Unit
) {
    val context = LocalContext.current
    var front by remember { mutableStateOf(initialFront) }
    var back by remember { mutableStateOf(initialBack) }
    var pos by remember { mutableStateOf(initialPos) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                OutlinedTextField(
                    value = front,
                    onValueChange = { front = it },
                    label = { Text("正面（单词）") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = back,
                    onValueChange = { back = it },
                    label = { Text("背面（释义）") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = pos,
                    onValueChange = { pos = it },
                    label = { Text("词性（可选，如 n. v. adj.）") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            PrimaryButton(
                text = "保存",
                onClick = {
                    if (front.isBlank() || back.isBlank()) {
                        Toast.makeText(context, "正面和背面都不能为空", Toast.LENGTH_SHORT).show()
                        return@PrimaryButton
                    }
                    onConfirm(front.trim(), back.trim(), pos.trim().ifBlank { null })
                }
            )
        },
        dismissButton = {
            BackTextButton(text = "取消", onClick = onDismiss)
        }
    )
}
