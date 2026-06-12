package com.example.ankiclone.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import com.example.ankiclone.ui.components.AppScreen
import com.example.ankiclone.ui.components.ChartPoint
import com.example.ankiclone.ui.components.DonutChart
import com.example.ankiclone.ui.components.EmptyStateCard
import com.example.ankiclone.ui.components.ForgettingCurveChart
import com.example.ankiclone.ui.components.LineChart
import com.example.ankiclone.ui.components.PieSlice
import com.example.ankiclone.ui.components.ScreenHeader
import com.example.ankiclone.ui.components.SectionCard
import com.example.ankiclone.ui.components.StatChip

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
    val streak = stats?.streak
    val totalReviewed = proficiencyData.sumOf { it.count }
    val totalFrequency = frequencyData.sumOf { it.count }

    fun statusColor(status: String?): Color = when (status) {
        "new" -> Color(0xFF94A3B8)
        "learning" -> Color(0xFFF7B84B)
        "review" -> Color(0xFF6A8DFF)
        else -> Color(0xFF35D3C9)
    }

    fun statusLabel(status: String?): String = when (status) {
        "new" -> "新词"
        "learning" -> "学习中"
        "review" -> "待复习"
        else -> status ?: "未知"
    }

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
        Spacer(modifier = Modifier.height(10.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            StatChip(
                label = "连续打卡",
                value = "${streak?.current ?: 0} 天",
                modifier = Modifier.weight(1f)
            )
            StatChip(
                label = "累计复习",
                value = "${streak?.totalReviews ?: 0} 次",
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
                    val base = "当前累计记录 $totalReviewed 条熟练度状态，最近 7 天共有 $totalFrequency 次复习，建议优先关注“学习中”和“待复习”词条。"
                    val current = streak?.current ?: 0
                    if (current > 0) "已连续学习 $current 天，继续保持！$base" else base
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
                title = "单词熟练度分布",
                subtitle = "各状态词条占比一目了然"
            ) {
                if (proficiencyData.isEmpty()) {
                    EmptyStateCard(
                        title = "暂无熟练度数据",
                        description = "等你开始学习后，这里会展示各状态的占比饼图。"
                    )
                } else {
                    DonutChart(
                        slices = proficiencyData.map { stat ->
                            PieSlice(
                                label = statusLabel(stat.status),
                                value = stat.count,
                                color = statusColor(stat.status)
                            )
                        }
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            SectionCard(
                title = "最近 7 天复习频率",
                subtitle = "按日期展示近期复习次数趋势"
            ) {
                if (frequencyData.isEmpty()) {
                    EmptyStateCard(
                        title = "暂无复习频率数据",
                        description = "等你开始复习后，这里会自动绘制 7 天折线趋势。"
                    )
                } else {
                    // 后端按日期倒序返回，折线图需要按时间正序展示
                    LineChart(
                        points = frequencyData
                            .sortedBy { it.date }
                            .map { stat ->
                                ChartPoint(
                                    label = stat.date?.substringAfter("-") ?: "未知",
                                    value = stat.count
                                )
                            },
                        lineColor = MaterialTheme.colorScheme.primary
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            SectionCard(
                title = "艾宾浩斯遗忘曲线",
                subtitle = "记忆保持率随时间衰减，圆点为推荐复习时机"
            ) {
                ForgettingCurveChart()
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "记忆会随时间快速衰减，按曲线上的关键节点（1、2、4、7、15、30 天）及时复习，可以显著减缓遗忘、巩固长期记忆。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}
