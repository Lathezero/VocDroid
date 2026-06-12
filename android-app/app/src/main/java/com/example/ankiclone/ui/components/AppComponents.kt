package com.example.ankiclone.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val BackgroundBrush = Brush.verticalGradient(
    colors = listOf(
        Color(0xFF0B1325),
        Color(0xFF08111F),
        Color(0xFF060B16)
    )
)

private val HeroBrush = Brush.linearGradient(
    colors = listOf(
        Color(0x336A8DFF),
        Color(0x2235D3C9),
        Color(0x22F4B95F)
    )
)

private val HighlightBrush = Brush.linearGradient(
    colors = listOf(
        Color(0x446A8DFF),
        Color(0x2235D3C9)
    )
)

@Composable
fun AppScreen(
    modifier: Modifier = Modifier,
    scrollable: Boolean = false,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color.Transparent
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(BackgroundBrush)
        ) {
            AmbientOrb(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .offset(x = (-76).dp, y = (-54).dp),
                size = 220.dp,
                colors = listOf(Color(0x446A8DFF), Color.Transparent)
            )
            AmbientOrb(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .offset(x = 96.dp, y = 120.dp),
                size = 280.dp,
                colors = listOf(Color(0x2AF4B95F), Color.Transparent)
            )
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(HeroBrush)
            )
            if (scrollable) {
                Column(
                    modifier = modifier
                        .fillMaxSize()
                        .statusBarsPadding()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp, vertical = 24.dp),
                    content = content
                )
            } else {
                Column(
                    modifier = modifier
                        .fillMaxSize()
                        .statusBarsPadding()
                        .padding(horizontal = 20.dp, vertical = 24.dp),
                    content = content
                )
            }
        }
    }
}

@Composable
private fun AmbientOrb(
    modifier: Modifier = Modifier,
    size: androidx.compose.ui.unit.Dp,
    colors: List<Color>
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(Brush.radialGradient(colors = colors))
    )
}

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.18f),
                shape = RoundedCornerShape(28.dp)
            ),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.88f)
        ),
        shape = RoundedCornerShape(28.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 10.dp)
    ) {
        content()
    }
}

@Composable
fun ScreenHeader(
    eyebrow: String,
    title: String,
    subtitle: String
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        InfoPill(text = eyebrow)
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun InfoPill(text: String) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(18.dp))
            .background(
                Brush.horizontalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.16f),
                        MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f)
                    )
                )
            )
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.24f),
                shape = RoundedCornerShape(18.dp)
            )
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.6.sp
            ),
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.9f)
        )
    }
}

@Composable
fun StatChip(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    GlassCard(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(HighlightBrush)
                .padding(16.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    Button(
        onClick = onClick,
        modifier = modifier.height(56.dp),
        enabled = enabled,
        shape = RoundedCornerShape(20.dp),
        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 14.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
        )
    ) {
        Text(text = text, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun SecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    Button(
        onClick = onClick,
        modifier = modifier.height(56.dp),
        enabled = enabled,
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.28f)),
        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 14.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
            contentColor = MaterialTheme.colorScheme.onSurface
        )
    ) {
        Text(text = text, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun EmptyStateCard(
    title: String,
    description: String,
    modifier: Modifier = Modifier
) {
    GlassCard(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            InfoPill(text = "暂无内容")
            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = description,
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun AnimatedBar(
    label: String,
    value: String,
    fraction: Float,
    color: Color,
    modifier: Modifier = Modifier
) {
    val animatedFraction by animateFloatAsState(
        targetValue = fraction.coerceIn(0.05f, 1f),
        animationSpec = tween(durationMillis = 700, easing = FastOutSlowInEasing),
        label = "barAnimation"
    )

    Column(
        modifier = modifier.fillMaxHeight(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Bottom
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(6.dp))
        Box(
            modifier = Modifier
                .width(28.dp)
                .fillMaxHeight(animatedFraction)
                .clip(RoundedCornerShape(topStart = 10.dp, topEnd = 10.dp, bottomStart = 6.dp, bottomEnd = 6.dp))
                .background(color)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun SectionCard(
    title: String,
    subtitle: String? = null,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    GlassCard(modifier = modifier) {
        Column(modifier = Modifier.padding(horizontal = 18.dp, vertical = 20.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            if (subtitle != null) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            content()
        }
    }
}

@Composable
fun BackTextButton(
    text: String = "返回",
    onClick: () -> Unit
) {
    TextButton(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp)
    ) {
        Text(text = text, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun AnimatedSection(visible: Boolean = true, content: @Composable () -> Unit) {
    AnimatedVisibility(visible = visible) {
        content()
    }
}

@Composable
fun DualActionRow(
    primaryText: String,
    onPrimaryClick: () -> Unit,
    secondaryText: String,
    onSecondaryClick: () -> Unit
) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        SecondaryButton(
            text = secondaryText,
            onClick = onSecondaryClick,
            modifier = Modifier.weight(1f)
        )
        PrimaryButton(
            text = primaryText,
            onClick = onPrimaryClick,
            modifier = Modifier.weight(1f)
        )
    }
}

// =======================
// 图表组件
// =======================

data class PieSlice(val label: String, val value: Int, val color: Color)

data class ChartPoint(val label: String, val value: Int)

/** 环形饼图：展示各状态占比，右侧（小屏下方）配图例。 */
@Composable
fun DonutChart(
    slices: List<PieSlice>,
    modifier: Modifier = Modifier
) {
    val total = slices.sumOf { it.value }.coerceAtLeast(1)
    val animatedProgress by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing),
        label = "donutAnimation"
    )

    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Box(
            modifier = Modifier
                .size(140.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val strokeWidth = size.minDimension * 0.16f
                val diameter = size.minDimension - strokeWidth
                val topLeft = Offset(
                    (size.width - diameter) / 2f,
                    (size.height - diameter) / 2f
                )
                val arcSize = Size(diameter, diameter)
                var startAngle = -90f
                slices.forEach { slice ->
                    val sweep = slice.value.toFloat() / total.toFloat() * 360f * animatedProgress
                    drawArc(
                        color = slice.color,
                        startAngle = startAngle,
                        sweepAngle = sweep,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Butt)
                    )
                    startAngle += slice.value.toFloat() / total.toFloat() * 360f
                }
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = total.toString(),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "总计",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            slices.forEach { slice ->
                val percent = slice.value.toFloat() / total.toFloat() * 100f
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(CircleShape)
                            .background(slice.color)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = slice.label,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = "${slice.value} · ${"%.0f".format(percent)}%",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

/** 折线图：展示按顺序排列的数据点趋势，底部带 X 轴标签。 */
@Composable
fun LineChart(
    points: List<ChartPoint>,
    lineColor: Color,
    modifier: Modifier = Modifier
) {
    if (points.isEmpty()) return
    val maxValue = (points.maxOfOrNull { it.value } ?: 1).coerceAtLeast(1)
    val animatedProgress by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing),
        label = "lineAnimation"
    )
    val gridColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.18f)
    val fillColor = lineColor.copy(alpha = 0.16f)

    Column(modifier = modifier.fillMaxWidth()) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
        ) {
            val leftPad = 8f
            val rightPad = 8f
            val topPad = 12f
            val bottomPad = 12f
            val chartWidth = size.width - leftPad - rightPad
            val chartHeight = size.height - topPad - bottomPad

            // 横向网格线
            val gridLines = 4
            for (i in 0..gridLines) {
                val y = topPad + chartHeight * i / gridLines
                drawLine(
                    color = gridColor,
                    start = Offset(leftPad, y),
                    end = Offset(size.width - rightPad, y),
                    strokeWidth = 1.5f
                )
            }

            val stepX = if (points.size > 1) chartWidth / (points.size - 1) else 0f
            val offsets = points.mapIndexed { index, point ->
                val x = leftPad + stepX * index
                val ratio = point.value.toFloat() / maxValue.toFloat()
                val y = topPad + chartHeight * (1f - ratio * animatedProgress)
                Offset(x, y)
            }

            // 折线下方渐变填充
            val fillPath = Path().apply {
                moveTo(offsets.first().x, topPad + chartHeight)
                offsets.forEach { lineTo(it.x, it.y) }
                lineTo(offsets.last().x, topPad + chartHeight)
                close()
            }
            drawPath(path = fillPath, color = fillColor)

            // 折线
            val linePath = Path().apply {
                offsets.forEachIndexed { index, offset ->
                    if (index == 0) moveTo(offset.x, offset.y) else lineTo(offset.x, offset.y)
                }
            }
            drawPath(
                path = linePath,
                color = lineColor,
                style = Stroke(width = 4f, cap = StrokeCap.Round)
            )

            // 数据点
            offsets.forEach { offset ->
                drawCircle(color = lineColor, radius = 6f, center = offset)
                drawCircle(color = Color.White, radius = 2.5f, center = offset)
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            points.forEach { point ->
                Text(
                    text = point.label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * 艾宾浩斯遗忘曲线：记忆保持率 R = e^(-t/S) 的理论曲线，
 * 并标注推荐的复习时间点（5分钟、30分钟、12小时、1天、2天、4天、7天、15天）。
 */
@Composable
fun ForgettingCurveChart(
    modifier: Modifier = Modifier
) {
    val curveColor = MaterialTheme.colorScheme.secondary
    val pointColor = MaterialTheme.colorScheme.primary
    val gridColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.18f)
    val fillColor = curveColor.copy(alpha = 0.14f)
    val animatedProgress by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(durationMillis = 1000, easing = FastOutSlowInEasing),
        label = "forgettingAnimation"
    )

    // 横轴用对数刻度（天），覆盖 0~30 天；S 为记忆稳定度参数。
    val dayMarks = listOf(0f, 1f, 2f, 4f, 7f, 15f, 30f)
    val stability = 1.8f

    Column(modifier = modifier.fillMaxWidth()) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(190.dp)
        ) {
            val leftPad = 8f
            val rightPad = 8f
            val topPad = 12f
            val bottomPad = 12f
            val chartWidth = size.width - leftPad - rightPad
            val chartHeight = size.height - topPad - bottomPad

            for (i in 0..4) {
                val y = topPad + chartHeight * i / 4
                drawLine(
                    color = gridColor,
                    start = Offset(leftPad, y),
                    end = Offset(size.width - rightPad, y),
                    strokeWidth = 1.5f
                )
            }

            // 用 log(1+day) 归一化横轴，让短期衰减看得更清楚
            val maxDay = 30f
            val maxLog = kotlin.math.ln(1f + maxDay)
            fun dayToX(day: Float): Float =
                leftPad + chartWidth * (kotlin.math.ln(1f + day) / maxLog)
            fun retentionToY(r: Float): Float =
                topPad + chartHeight * (1f - r)

            val samples = 80
            val curveOffsets = (0..samples).map { i ->
                val day = maxDay * i / samples
                val retention = kotlin.math.exp(-day / stability)
                Offset(dayToX(day), retentionToY(retention))
            }

            // 仅绘制到 animatedProgress 对应的比例，形成从左到右展开的动画
            val visibleCount = (curveOffsets.size * animatedProgress).toInt().coerceAtLeast(1)
            val visible = curveOffsets.take(visibleCount)

            val fillPath = Path().apply {
                moveTo(visible.first().x, topPad + chartHeight)
                visible.forEach { lineTo(it.x, it.y) }
                lineTo(visible.last().x, topPad + chartHeight)
                close()
            }
            drawPath(path = fillPath, color = fillColor)

            val curvePath = Path().apply {
                visible.forEachIndexed { index, offset ->
                    if (index == 0) moveTo(offset.x, offset.y) else lineTo(offset.x, offset.y)
                }
            }
            drawPath(
                path = curvePath,
                color = curveColor,
                style = Stroke(width = 4f, cap = StrokeCap.Round)
            )

            // 复习时间点标注
            dayMarks.drop(1).forEach { day ->
                val retention = kotlin.math.exp(-day / stability)
                val center = Offset(dayToX(day), retentionToY(retention))
                if (center.x <= leftPad + chartWidth * animatedProgress + 4f) {
                    drawCircle(color = pointColor, radius = 6f, center = center)
                    drawCircle(color = Color.White, radius = 2.5f, center = center)
                }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            listOf("第0天", "1天", "2天", "4天", "7天", "15天", "30天").forEach { mark ->
                Text(
                    text = mark,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
