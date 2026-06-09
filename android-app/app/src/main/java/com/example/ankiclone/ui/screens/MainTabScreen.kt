package com.example.ankiclone.ui.screens

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.Icon
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp

@Composable
fun MainTabScreen(
    role: String,
    onNavigateToStudy: (String) -> Unit,
    onNavigateToImport: () -> Unit,
    onNavigateToOcrImport: () -> Unit,
    onNavigateToAdmin: () -> Unit,
    onLogout: () -> Unit
) {
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val tabs = listOf("主页", "统计", "我的")
    val icons = listOf(Icons.Default.Home, Icons.Default.Star, Icons.Default.Person)

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
                Surface(
                    shape = RoundedCornerShape(28.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
                    tonalElevation = 12.dp,
                    shadowElevation = 18.dp,
                    modifier = Modifier
                        .clip(RoundedCornerShape(28.dp))
                        .border(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.18f),
                            shape = RoundedCornerShape(28.dp)
                        )
                ) {
                    NavigationBar(
                        containerColor = Color.Transparent,
                        tonalElevation = 0.dp
                    ) {
                        tabs.forEachIndexed { index, title ->
                            NavigationBarItem(
                                selected = selectedTabIndex == index,
                                onClick = { selectedTabIndex = index },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = MaterialTheme.colorScheme.primary,
                                    selectedTextColor = MaterialTheme.colorScheme.primary,
                                    indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.18f),
                                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                                ),
                                icon = { Icon(icons[index], contentDescription = title) },
                                label = { Text(title, style = MaterialTheme.typography.labelMedium) }
                            )
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        Modifier.padding(innerPadding).let { paddingModifier ->
            when (selectedTabIndex) {
                0 -> HomeScreen(
                    onNavigateToStudy = onNavigateToStudy,
                    onNavigateToImport = onNavigateToImport,
                    onNavigateToOcrImport = onNavigateToOcrImport,
                    modifier = paddingModifier
                )
                1 -> StatisticsScreen(modifier = paddingModifier)
                2 -> ProfileScreen(
                    role = role,
                    onNavigateToAdmin = onNavigateToAdmin,
                    onLogout = onLogout,
                    modifier = paddingModifier
                )
            }
        }
    }
}
