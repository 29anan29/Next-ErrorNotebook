package com.errorbook.app.ui.reasondetail

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.errorbook.app.data.local.entity.ReviewResult
import com.errorbook.app.data.model.ReasonQuestionItem
import com.errorbook.app.data.model.ReasonWithCount
import com.errorbook.app.ui.theme.ErrorBookTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * 复习模式的验收断言（PRD §13「复习时默认不显示答案」「点击显示错因后才展示错因」）。
 *
 * 只测 ReviewPaneContent 这层纯 UI，不把 ViewModel 与 Room 拉进来。
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], qualifiers = "w360dp-h640dp")
class ReviewPaneUiTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val item = ReasonQuestionItem(
        questionReasonId = 1L,
        questionId = 1L,
        imagePath = "/nonexistent.jpg",
        ocrText = "1 + 1 = ?",
        note = null,
        createdAt = 1L,
        detail = "符号看错了",
    )

    private val reason = ReasonWithCount(
        id = 7L,
        name = "计算错误",
        category = "习惯性",
        color = "#2A9D99",
        wrongCount = 3,
        questionCount = 2,
        lastWrongAt = 1L,
    )

    private fun state(revealed: Boolean) = ReasonDetailUiState(
        isLoading = false,
        reason = reason,
        questions = listOf(item),
        currentIndex = 0,
        revealed = revealed,
    )

    private fun assertNotVisible(text: String) {
        assertTrue(
            "「$text」本应不可见",
            composeRule.onAllNodesWithText(text).fetchSemanticsNodes().isEmpty(),
        )
    }

    @Test
    fun `默认不显示错因 只显示引导与揭示按钮`() {
        composeRule.setContent {
            ErrorBookTheme {
                ReviewPaneContent(
                    uiState = state(revealed = false),
                    onReveal = {},
                    onReview = {},
                    onNext = {},
                    onQuestionClick = {},
                )
            }
        }

        composeRule.onNodeWithText("先想一想：我为什么错？").assertIsDisplayed()
        composeRule.onNodeWithText("显示错因").assertIsDisplayed()
        assertNotVisible("计算错误")
        assertNotVisible("符号看错了")
    }

    @Test
    fun `揭示后展示错因名与说明并出现反馈按钮`() {
        composeRule.setContent {
            ErrorBookTheme {
                ReviewPaneContent(
                    uiState = state(revealed = true),
                    onReveal = {},
                    onReview = {},
                    onNext = {},
                    onQuestionClick = {},
                )
            }
        }

        composeRule.onNodeWithText("计算错误").assertIsDisplayed()
        composeRule.onNodeWithText("符号看错了").assertIsDisplayed()
        composeRule.onNodeWithText("记住了").assertIsDisplayed()
        composeRule.onNodeWithText("又错").assertIsDisplayed()
        composeRule.onNodeWithText("模糊").assertIsDisplayed()
        assertNotVisible("显示错因")
    }

    @Test
    fun `点又错会把 wrong 反馈回传给上层`() {
        var captured: String? = null
        composeRule.setContent {
            ErrorBookTheme {
                ReviewPaneContent(
                    uiState = state(revealed = true),
                    onReveal = {},
                    onReview = { captured = it },
                    onNext = {},
                    onQuestionClick = {},
                )
            }
        }

        composeRule.onNodeWithText("又错").performClick()

        assertEquals(ReviewResult.WRONG, captured)
    }

    @Test
    fun `题目文本在揭示前后都可见`() {
        composeRule.setContent {
            ErrorBookTheme {
                ReviewPaneContent(
                    uiState = state(revealed = false),
                    onReveal = {},
                    onReview = {},
                    onNext = {},
                    onQuestionClick = {},
                )
            }
        }
        composeRule.onNodeWithText("1 + 1 = ?").assertIsDisplayed()
    }
}
