package com.example.ankiclone.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.ankiclone.data.api.RetrofitClient
import com.example.ankiclone.data.api.StatsResponse
import com.example.ankiclone.ui.components.AnimatedBar
import com.example.ankiclone.ui.components.AppScreen
import com.example.ankiclone.ui.components.EmptyStateCard
import com.example.ankiclone.ui.components.ScreenHeader
import com.example.ankiclone.ui.components.SectionCard
import com.example.ankiclone.ui.components.StatChip
import kotlin.math.max

@Composable
fun StatisticsScreen(modifier: Modifier = Modifier) {
    var stats by remember { mutableStateOf<StatsResponse?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        try {
            stats = RetrofitClient.apiService.getStats()
        } catch (_: Exception) {
            stats = null
        } finally {
            isLoading = false
        }
    }

    val proficiencyData = stats?.proficiency ?: emptyList()
    val frequencyData = stats?.frequency ?: emptyList()
    val totalReviewed = proficiencyData.sumOf { it.count }
    val totalFrequency = frequencyData.sumOf { it.count }

    AppScreen(modifier = modifier, scrollable = true) {
        ScreenHeader(
            eyebrow = "统计",
            title = "学习数据总览",
            subtitle = "实时展示当前账号的单词熟练度和最近 7 天复习频率。"
        )
        Spacer(modifier = Modifier.height(18.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            StatChip(
                label = "熟练度统计",
                value = totalReviewed.toString(),
                modifier = Modifier.weight(1f)
            )
            StatChip(
                label = "近 7 天复习",
                value = totalFrequency.toString(),
                modifier = Modifier.weight(1f)
            )
        }
        Spacer(modifier = Modifier.height(18.dp))
        SectionCard(
            title = "学习洞察",
            subtitle = "把当前统计转换成更直观的复习提示"
        ) {
            Text(
                text = if (isLoading) {
                    "正在同步最新统计数据，稍后会生成你的复习概览。"
                } else if (totalReviewed == 0 && totalFrequency == 0) {
                    "你还没有形成统计样本，完成几次学习后，这里会展示熟练度分布和近期趋势。"
                } else {
                    "当前累计记录 $totalReviewed 条熟练度状态，最近 7 天共有 $totalFrequency 次复习，建议优先关注“学习中”和“待复习”词条。"
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
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
        } else {
            SectionCard(
                title = "单词熟练度",
                subtitle = "按新词、学习中、待复习等状态统计"
            ) {
                if (proficiencyData.isEmpty()) {
                    EmptyStateCard(
                        title = "暂无熟练度数据",
                        description = "等你开始学习后，这里会展示各状态的柱状统计图。"
                    )
                } else {
                    val maxProficiency = max(1, proficiencyData.maxOfOrNull { it.count } ?: 1)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        proficiencyData.forEach { stat ->
                            val color = when (stat.status) {
                                "new" -> Color(0xFF94A3B8)
                                "learning" -> Color(0xFFF7B84B)
                                "review" -> Color(0xFF6A8DFF)
                                else -> Color(0xFF35D3C9)
                            }
                            val label = when (stat.status) {
                                "new" -> "新词"
                                "learning" -> "学习中"
                                "review" -> "待复习"
                                else -> stat.status ?: "未知"
                            }
                            AnimatedBar(
                                label = label,
                                value = stat.count.toString(),
                                fraction = stat.count.toFloat() / maxProficiency.toFloat(),
                                color = color,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            SectionCard(
                title = "最近 7 天复习频率",
                subtitle = "按日期统计近期复习次数"
            ) {
                if (frequencyData.isEmpty()) {
                    EmptyStateCard(
                        title = "暂无复习频率数据",
                        description = "等你开始复习后，这里会自动绘制 7 天趋势。"
                    )
                } else {
                    val maxFreq = max(1, frequencyData.maxOfOrNull { it.count } ?: 1)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        frequencyData.forEach { stat ->
                            AnimatedBar(
                                label = stat.date?.substringAfter("-") ?: "未知",
                                value = stat.count.toString(),
                                fraction = stat.count.toFloat() / maxFreq.toFloat(),
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }
    }
}
