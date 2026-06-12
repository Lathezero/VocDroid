package com.example.ankiclone

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.ankiclone.ui.screens.*
import com.example.ankiclone.ui.theme.AnkiCloneTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AnkiCloneTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    NavHost(navController = navController, startDestination = "server_config") {
                        composable("server_config") {
                            ServerConfigScreen(
                                onNavigateToAuth = {
                                    navController.navigate("auth") {
                                        popUpTo("server_config") { inclusive = true }
                                    }
                                }
                            )
                        }
                        composable("auth") {
                            AuthScreen(
                                onNavigateToHome = { role ->
                                    navController.navigate("main_tab/$role") {
                                        popUpTo("auth") { inclusive = true }
                                    }
                                }
                            )
                        }
                        composable(
                            "main_tab/{role}",
                            arguments = listOf(navArgument("role") { type = NavType.StringType })
                        ) { backStackEntry ->
                            val role = backStackEntry.arguments?.getString("role") ?: "user"
                            MainTabScreen(
                                role = role,
                                onNavigateToStudy = { deckName ->
                                    navController.navigate("study/$deckName")
                                },
                                onNavigateToSpell = { deckName ->
                                    navController.navigate("spell/$deckName")
                                },
                                onNavigateToReviewDue = {
                                    navController.navigate("study_due")
                                },
                                onNavigateToCardManager = { deckId, deckName ->
                                    navController.navigate("card_manager/$deckId/$deckName")
                                },
                                onNavigateToImport = {
                                    navController.navigate("import")
                                },
                                onNavigateToOcrImport = {
                                    navController.navigate("ocr_import")
                                },
                                onNavigateToAdmin = {
                                    navController.navigate("admin")
                                },
                                onLogout = {
                                    navController.navigate("auth") {
                                        popUpTo(0) { inclusive = true }
                                    }
                                }
                            )
                        }
                        composable(
                            "study/{deckName}",
                            arguments = listOf(navArgument("deckName") { type = NavType.StringType })
                        ) { backStackEntry ->
                            val deckName = backStackEntry.arguments?.getString("deckName") ?: "Unknown"
                            StudyScreen(
                                deckName = deckName,
                                onNavigateBack = { navController.popBackStack() }
                            )
                        }
                        composable("study_due") {
                            StudyScreen(
                                deckName = "due",
                                dueMode = true,
                                onNavigateBack = { navController.popBackStack() }
                            )
                        }
                        composable(
                            "spell/{deckName}",
                            arguments = listOf(navArgument("deckName") { type = NavType.StringType })
                        ) { backStackEntry ->
                            val deckName = backStackEntry.arguments?.getString("deckName") ?: "Unknown"
                            SpellScreen(
                                deckName = deckName,
                                onNavigateBack = { navController.popBackStack() }
                            )
                        }
                        composable(
                            "card_manager/{deckId}/{deckName}",
                            arguments = listOf(
                                navArgument("deckId") { type = NavType.IntType },
                                navArgument("deckName") { type = NavType.StringType }
                            )
                        ) { backStackEntry ->
                            val deckId = backStackEntry.arguments?.getInt("deckId") ?: 0
                            val deckName = backStackEntry.arguments?.getString("deckName") ?: "牌组"
                            CardManagerScreen(
                                deckId = deckId,
                                deckName = deckName,
                                onNavigateBack = { navController.popBackStack() }
                            )
                        }
                        composable("import") {
                            ImportScreen(
                                onNavigateBack = { navController.popBackStack() }
                            )
                        }
                        composable("ocr_import") {
                            OcrImportScreen(
                                onNavigateBack = { navController.popBackStack() }
                            )
                        }
                        composable("admin") {
                            AdminScreen(
                                onNavigateBack = { navController.popBackStack() }
                            )
                        }
                    }
                }
            }
        }
    }
}
