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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.ankiclone.data.api.Card
import com.example.ankiclone.data.api.RetrofitClient
import com.example.ankiclone.data.api.ReviewResult
import com.example.ankiclone.ui.components.AppScreen
import com.example.ankiclone.ui.components.BackTextButton
import com.example.ankiclone.ui.components.EmptyStateCard
import com.example.ankiclone.ui.components.GlassCard
import com.example.ankiclone.ui.components.InfoPill
import com.example.ankiclone.ui.components.PrimaryButton
import com.example.ankiclone.ui.components.ScreenHeader
import com.example.ankiclone.ui.components.SecondaryButton
import com.example.ankiclone.ui.components.StatChip
import kotlinx.coroutines.launch

@Composable
fun StudyScreen(
    deckName: String,
    onNavigateBack: () -> Unit
) {
    var cards by remember { mutableStateOf<List<Card>>(emptyList()) }
    var currentIndex by remember { mutableIntStateOf(0) }
    var isFlipped by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(true) }
    var isReviewing by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(deckName) {
        coroutineScope.launch {
            try {
                val id = deckName.toIntOrNull()
                if (id != null) {
                    cards = RetrofitClient.apiService.getCardsForDeck(id)
                }
            } catch (_: Exception) {
                cards = emptyList()
            } finally {
                isLoading = false
            }
        }
    }

    fun handleReview(rating: Int) {
        if (currentIndex >= cards.size || isReviewing) return
        val currentCard = cards[currentIndex]
        isReviewing = true

        coroutineScope.launch {
            try {
                RetrofitClient.apiService.reviewCard(
                    cardId = currentCard.id,
                    result = ReviewResult(performanceRating = rating)
                )
            } catch (_: Exception) {
            } finally {
                isReviewing = false
                isFlipped = false
                currentIndex++
            }
        }
    }

    AppScreen {
        ScreenHeader(
            eyebrow = "学习模式",
            title = "正在学习牌组",
            subtitle = "点击卡片查看答案，并根据掌握程度选择熟练度。"
        )
        Spacer(modifier = Modifier.height(18.dp))

        if (isLoading) {
            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.secondary)
            }
        } else if (cards.isEmpty() || currentIndex >= cards.size) {
            EmptyStateCard(
                title = "当前牌组已复习完毕",
                description = "恭喜你，这一轮所有卡片都已经学习完成。",
                modifier = Modifier.weight(1f)
            )
        } else {
            val currentCard = cards[currentIndex]
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
                    label = "当前面",
                    value = if (isFlipped) "答案" else "问题",
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(modifier = Modifier.height(18.dp))
            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clickable(enabled = !isFlipped) { isFlipped = true }
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        InfoPill(text = if (isFlipped) "已显示答案" else "点击卡片查看答案")
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = if (isFlipped) "答案" else "题目",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.secondary
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = if (isFlipped) currentCard.back_content else currentCard.front_content,
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                        if (isFlipped && !currentCard.part_of_speech.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "词性：${currentCard.part_of_speech}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (!isFlipped) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "先在脑海中回忆答案，再点击卡片进行校验。",
                                style = MaterialTheme.typography.bodySmall,
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            if (isFlipped) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        SecondaryButton("重来", { handleReview(0) }, Modifier.weight(1f), !isReviewing)
                        SecondaryButton("困难", { handleReview(1) }, Modifier.weight(1f), !isReviewing)
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        PrimaryButton("良好", { handleReview(2) }, Modifier.weight(1f), !isReviewing)
                        PrimaryButton("简单", { handleReview(3) }, Modifier.weight(1f), !isReviewing)
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "根据掌握程度选择评分，系统会据此安排后续复习频率。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                PrimaryButton(
                    text = "显示答案",
                    onClick = { isFlipped = true },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        BackTextButton(text = "返回牌组列表", onClick = onNavigateBack)
    }
}
