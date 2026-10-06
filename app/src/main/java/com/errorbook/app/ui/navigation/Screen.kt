package com.errorbook.app.ui.navigation

sealed class Screen(val route: String) {
    data object Home : Screen("home")
    data object Capture : Screen("capture")
    data object ReasonManage : Screen("reason_manage")
    data object Settings : Screen("settings")
    data object Export : Screen("export")

    data object ReasonDetail : Screen("reason_detail/{reasonId}") {
        fun createRoute(reasonId: Long) = "reason_detail/$reasonId"
    }

    data object QuestionDetail : Screen("question_detail/{questionId}") {
        fun createRoute(questionId: Long) = "question_detail/$questionId"
    }
}
