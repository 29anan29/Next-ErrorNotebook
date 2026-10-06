package com.errorbook.app.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.navigation.NavDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.errorbook.app.R
import com.errorbook.app.ui.capture.CaptureScreen
import com.errorbook.app.ui.export.ExportScreen
import com.errorbook.app.ui.home.HomeScreen
import com.errorbook.app.ui.navigation.Screen
import com.errorbook.app.ui.questiondetail.QuestionDetailScreen
import com.errorbook.app.ui.reasondetail.ReasonDetailScreen
import com.errorbook.app.ui.reasonmanage.ReasonManageScreen
import com.errorbook.app.ui.settings.SettingsScreen

@Composable
fun ErrorBookRoot() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination
    val showBottomBar = currentDestination?.route in bottomDestinations

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            if (showBottomBar) {
                BottomBar(navController = navController, currentDestination = currentDestination)
            }
        },
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            composable(Screen.Home.route) {
                HomeScreen(
                    onReasonClick = { id -> navController.navigate(Screen.ReasonDetail.createRoute(id)) },
                    onExportClick = { navController.navigate(Screen.Export.route) },
                )
            }
            composable(Screen.Capture.route) {
                CaptureScreen(onSaved = { navController.popBackStack() })
            }
            composable(Screen.ReasonManage.route) {
                ReasonManageScreen()
            }
            composable(Screen.Settings.route) {
                SettingsScreen()
            }
            composable(
                route = Screen.ReasonDetail.route,
                arguments = listOf(navArgument("reasonId") { type = NavType.LongType }),
            ) {
                ReasonDetailScreen(
                    onBack = { navController.popBackStack() },
                    onQuestionClick = { id -> navController.navigate(Screen.QuestionDetail.createRoute(id)) },
                )
            }
            composable(
                route = Screen.QuestionDetail.route,
                arguments = listOf(navArgument("questionId") { type = NavType.LongType }),
            ) {
                QuestionDetailScreen(onBack = { navController.popBackStack() })
            }
            composable(Screen.Export.route) {
                ExportScreen(onBack = { navController.popBackStack() })
            }
        }
    }
}

private val bottomDestinations = setOf(
    Screen.Home.route,
    Screen.Capture.route,
    Screen.ReasonManage.route,
    Screen.Settings.route,
)

private data class BottomItem(
    val route: String,
    val label: String,
    val iconRes: Int,
)

private val bottomItems = listOf(
    BottomItem(Screen.Home.route, "首页", R.drawable.ic_home),
    BottomItem(Screen.Capture.route, "录题", R.drawable.ic_notebook_pen),
    BottomItem(Screen.ReasonManage.route, "错因", R.drawable.ic_tags),
    BottomItem(Screen.Settings.route, "设置", R.drawable.ic_settings),
)

@Composable
private fun BottomBar(navController: NavHostController, currentDestination: NavDestination?) {
    NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
        bottomItems.forEach { item ->
            val selected = currentDestination?.route == item.route
            NavigationBarItem(
                selected = selected,
                onClick = {
                    navController.navigate(item.route) {
                        popUpTo(Screen.Home.route) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                icon = {
                    Icon(
                        painter = painterResource(item.iconRes),
                        contentDescription = item.label,
                        tint = if (selected) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                    )
                },
                label = {
                    Text(
                        text = item.label,
                        style = MaterialTheme.typography.labelSmall,
                    )
                },
            )
        }
    }
}
