package com.example.ankiclone.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ankiclone.ui.components.AppScreen
import com.example.ankiclone.ui.components.DualActionRow
import com.example.ankiclone.ui.components.InfoPill
import com.example.ankiclone.ui.components.PrimaryButton
import com.example.ankiclone.ui.components.ScreenHeader
import com.example.ankiclone.ui.components.SectionCard
import com.example.ankiclone.ui.components.SecondaryButton
import com.example.ankiclone.ui.components.StatChip

@Composable
fun ProfileScreen(
    role: String,
    onNavigateToAdmin: () -> Unit,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier
) {
    AppScreen(modifier = modifier) {
        ScreenHeader(
            eyebrow = "我的",
            title = "账户与操作",
            subtitle = "统一查看当前身份，并在这里进入后台管理或切换账户。"
        )
        Spacer(modifier = Modifier.height(18.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            StatChip(
                label = "当前身份",
                value = if (role == "admin") "管理员" else "普通用户",
                modifier = Modifier.weight(1f)
            )
            StatChip(
                label = "权限状态",
                value = if (role == "admin") "可管理用户" else "学习模式",
                modifier = Modifier.weight(1f)
            )
        }
        Spacer(modifier = Modifier.height(18.dp))
        SectionCard(
            title = "账户中心",
            subtitle = if (role == "admin") {
                "你当前拥有更高权限，可进入后台查看用户与牌组概览。"
            } else {
                "保持当前学习节奏，随时可以切换账户继续同步数据。"
            }
        ) {
            InfoPill(text = if (role == "admin") "管理权限已启用" else "标准学习模式")
            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = if (role == "admin") {
                    "管理员账号适合查看整体数据、确认用户状态和后续接入更多后台功能。"
                } else {
                    "普通用户可以专注于学习、导入牌组与查看统计，页面布局也会保持更简洁。"
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(18.dp))
            if (role == "admin") {
                DualActionRow(
                    primaryText = "进入后台",
                    onPrimaryClick = onNavigateToAdmin,
                    secondaryText = "退出登录",
                    onSecondaryClick = onLogout
                )
            } else {
                SecondaryButton(
                    text = "退出登录 / 切换账户",
                    onClick = onLogout,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}
