package com.example.ankiclone.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.ankiclone.data.api.Card
import com.example.ankiclone.data.api.RetrofitClient
import com.example.ankiclone.data.api.ReviewResult
import com.example.ankiclone.data.local.rememberTts
import com.example.ankiclone.ui.components.AppScreen
import com.example.ankiclone.ui.components.BackTextButton
import com.example.ankiclone.ui.components.EmptyStateCard
import com.example.ankiclone.ui.components.GlassCard
import com.example.ankiclone.ui.components.InfoPill
import com.example.ankiclone.ui.components.PrimaryButton
import com.example.ankiclone.ui.components.ScreenHeader
import com.example.ankiclone.ui.components.StatChip
import kotlinx.coroutines.launch

@Composable
fun SpellScreen(
    deckName: String,
    onNavigateBack: () -> Unit
) {
    var cards by remember { mutableStateOf<List<Card>>(emptyList()) }
    var currentIndex by remember { mutableIntStateOf(0) }
    var input by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(true) }
    var isReviewing by remember { mutableStateOf(false) }
    // null = 尚未提交，true/false = 本张卡的判定结果（已提交）
    var lastCorrect by remember { mutableStateOf<Boolean?>(null) }
    val coroutineScope = rememberCoroutineScope()
    val speak = rememberTts()

    LaunchedEffect(deckName) {
        coroutineScope.launch {
            try {
                val id = deckName.toIntOrNull()
                cards = if (id != null) {
                    RetrofitClient.apiService.getCardsForDeck(id)
                } else {
                    emptyList()
                }
            } catch (_: Exception) {
                cards = emptyList()
            } finally {
                isLoading = false
            }
        }
    }

    fun submit() {
        if (currentIndex >= cards.size || isReviewing || lastCorrect != null) return
        val currentCard = cards[currentIndex]
        val correct = input.trim().equals(currentCard.front_content.trim(), ignoreCase = true)
        lastCorrect = correct
        isReviewing = true

        coroutineScope.launch {
            try {
                // 拼对按「良好」(2)，拼错按「重来」(0)，让拼写也推进记忆曲线与统计/打卡
                RetrofitClient.apiService.reviewCard(
                    cardId = currentCard.id,
                    result = ReviewResult(performanceRating = if (correct) 2 else 0)
                )
            } catch (_: Exception) {
            } finally {
                isReviewing = false
            }
        }
    }

    fun next() {
        val correct = lastCorrect
        val currentCard = cards.getOrNull(currentIndex)
        lastCorrect = null
        input = ""
        if (correct == false && currentCard != null) {
            // 拼错的卡移到队列末尾，本轮稍后再考一次
            val reordered = cards.toMutableList()
            reordered.removeAt(currentIndex)
            reordered.add(currentCard)
            cards = reordered
        } else {
            currentIndex++
        }
    }

    AppScreen {
        ScreenHeader(
            eyebrow = "拼写测验",
            title = "看中文拼英文",
            subtitle = "根据中文释义输入对应的英文单词，大小写和首尾空格不影响判定。"
        )
        Spacer(modifier = Modifier.height(18.dp))

        if (isLoading) {
            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.secondary)
            }
        } else if (cards.isEmpty() || currentIndex >= cards.size) {
            EmptyStateCard(
                title = "当前牌组已测验完毕",
                description = "恭喜你，这一轮所有单词都已经拼写完成。",
                modifier = Modifier.weight(1f)
            )
        } else {
            val currentCard = cards[currentIndex]
            val answered = lastCorrect != null
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                StatChip(
                    label = "当前进度",
                    value = "${currentIndex + 1} / ${cards.size}",
                    modifier = Modifier.weight(1f)
                )
                StatChip(
                    label = "状态",
                    value = if (answered) (if (lastCorrect == true) "正确" else "错误") else "待作答",
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(modifier = Modifier.height(18.dp))
            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        InfoPill(text = "请拼写下列释义对应的英文单词")
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = currentCard.back_content,
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                        if (!currentCard.part_of_speech.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "词性：${currentCard.part_of_speech}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(modifier = Modifier.height(20.dp))
                        OutlinedTextField(
                            value = input,
                            onValueChange = { if (!answered) input = it },
                            singleLine = true,
                            enabled = !answered,
                            label = { Text("输入英文单词") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        if (answered) {
                            Spacer(modifier = Modifier.height(16.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = if (lastCorrect == true) "✓ 回答正确" else "✗ 正确答案：${currentCard.front_content}",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center,
                                    color = if (lastCorrect == true) Color(0xFF35D3C9) else Color(0xFFF87171)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                IconButton(onClick = { speak(currentCard.front_content) }) {
                                    Text(
                                        text = "🔊",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.secondary
                                    )
                                }
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            if (answered) {
                PrimaryButton(
                    text = "下一题",
                    onClick = { next() },
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                PrimaryButton(
                    text = "提交",
                    onClick = { submit() },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isReviewing && input.isNotBlank()
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        BackTextButton(text = "返回牌组列表", onClick = onNavigateBack)
    }
}
