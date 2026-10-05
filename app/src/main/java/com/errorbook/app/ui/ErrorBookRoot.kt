package com.errorbook.app.ui

import androidx.compose.foundation.layout.Box
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
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.navigation.NavDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.errorbook.app.ui.navigation.Screen
import com.errorbook.app.ui.capture.CaptureScreen
import com.errorbook.app.ui.export.ExportScreen
import com.errorbook.app.ui.home.HomeScreen
import com.errorbook.app.ui.questiondetail.QuestionDetailScreen
import com.errorbook.app.ui.reasondetail.ReasonDetailScreen
import com.errorbook.app.ui.reasonmanage.ReasonManageScreen
import com.errorbook.app.ui.settings.SettingsScreen

@Composable
fun ErrorBookRoot() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            BottomBar(navController = navController, currentDestination = currentDestination)
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
                    onReasonClick = { id -> navController.navigate(Screen.ReasonDetail.route(id)) },
                    onExportClick = { navController.navigate(Screen.Export.route) },
                )
            }
            composable(Screen.Capture.route) { CaptureScreen(onSaved = { navController.popBackStack() }) }
            composable(Screen.ReasonManage.route) { ReasonManageScreen() }
            composable(Screen.Settings.route) { SettingsScreen() }
            composable(Screen.ReasonDetail.route) {
                val id = it.arguments?.getString("reasonId")?.toLongOrNull() ?: 0L
                ReasonDetailScreen(
                    reasonId = id,
                    onBack = { navController.popBackStack() },
                    onQuestionClick = { qid -> navController.navigate(Screen.QuestionDetail.route(qid)) },
                )
            }
            composable(Screen.QuestionDetail.route) {
                val id = it.arguments?.getString("questionId")?.toLongOrNull() ?: 0L
                QuestionDetailScreen(questionId = id, onBack = { navController.popBackStack() })
            }
            composable(Screen.Export.route) { ExportScreen(onBack = { navController.popBackStack() }) }
        }
    }
}

@Composable
private fun BottomBar(navController: NavHostController, currentDestination: NavDestination?) {
    val items = listOf(
        Triple(Screen.Home.route, "首页", com.errorbook.app.R.drawable.ic_home),
        Triple(Screen.Capture.route, "录题", com.errorbook.app.R.drawable.ic_notebook_pen),
        Triple(Screen.ReasonManage.route, "错因", com.errorbook.app.R.drawable.ic_tags),
        Triple(Screen.Settings.route, "设置", com.errorbook.app.R.drawable.ic_settings),
    )
    NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
        items.forEach { (route, label, iconRes) ->
            val selected = currentDestination?.route == route
            NavigationBarItem(
                selected = selected,
                onClick = {
                    navController.navigate(route) {
                        popUpTo(Screen.Home.route) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                icon = {
                    Icon(
                        painter = painterResource(iconRes),
                        contentDescription = label,
                        colorFilter = ColorFilter.tint(
                            if (selected) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                        ),
                    )
                },
                label = { Text(label, style = MaterialTheme.typography.labelSmall) },
            )
        }
    }
}