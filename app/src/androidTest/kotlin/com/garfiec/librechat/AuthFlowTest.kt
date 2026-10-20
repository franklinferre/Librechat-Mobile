package com.garfiec.librechat

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextReplacement
import com.garfiec.librechat.core.common.brand.ServerLock
import com.garfiec.librechat.core.ui.theme.LibreChatTheme
import com.garfiec.librechat.feature.auth.screen.ServerUrlScreen
import com.garfiec.librechat.feature.auth.viewmodel.ServerUrlViewModel
import org.junit.Rule
import org.junit.Test
import org.koin.mp.KoinPlatform

class AuthFlowTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    /**
     * These tests run inside the app's own package against its real Koin graph, so
     * [ServerUrlScreen]'s ViewModel pre-fills the field with whatever server URL the installed app
     * last saved on this device. Each test therefore sets the field's contents itself. Editing the
     * field also stops a prefill that has not landed yet from overwriting it later.
     *
     * The real graph provides the locked [ServerLock], which swaps the form for a connecting
     * screen. The form tests therefore build the ViewModel with `ServerLock(null)` (the unlocked
     * upstream behaviour) from the graph's other dependencies; the locked screen has its own test.
     */
    private fun setUpServerUrlScreen() {
        // Built before setContent: a ViewModel constructed inside a composable is flagged by lint
        // (ViewModelConstructorInComposable), and this one is deliberately not the Koin-provided one.
        val koin = KoinPlatform.getKoin()
        val viewModel = ServerUrlViewModel(
            serverDataStore = koin.get(),
            serverRepository = koin.get(),
            configRepository = koin.get(),
            accountSwitcher = koin.get(),
            serverLock = ServerLock(null),
        )
        composeTestRule.setContent {
            LibreChatTheme {
                ServerUrlScreen(
                    onServerValidate = {},
                    viewModel = viewModel,
                )
            }
        }
    }

    @Test
    fun serverUrlScreen_displaysCorrectly() {
        setUpServerUrlScreen()
        composeTestRule.onNodeWithText("Connect to LibreChat").assertIsDisplayed()
        composeTestRule.onNodeWithText("Server URL").assertIsDisplayed()
        composeTestRule.onNodeWithText("Connect").assertIsDisplayed()
    }

    @Test
    fun serverUrlScreen_connectButtonDisabledWhenEmpty() {
        setUpServerUrlScreen()
        composeTestRule.onNodeWithText("Server URL").performTextClearance()
        composeTestRule.onNodeWithText("Connect").assertIsNotEnabled()
    }

    @Test
    fun serverUrlScreen_enablesConnectOnInput() {
        setUpServerUrlScreen()
        composeTestRule.onNodeWithText("Server URL").performTextReplacement("https://example.com")
        composeTestRule.onNodeWithText("Connect").assertIsEnabled()
    }

    @Test
    fun lockedServerUrlScreen_showsConnectingWithoutUrlField() {
        composeTestRule.setContent {
            LibreChatTheme {
                ServerUrlScreen(onServerValidate = {})
            }
        }
        // Either the connect is still running or it failed fast and offers a retry.
        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            composeTestRule.onAllNodesWithText("Connecting to Lideri Chat…").fetchSemanticsNodes().isNotEmpty() ||
                composeTestRule.onAllNodesWithText("Retry").fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onAllNodesWithText("Server URL").assertCountEquals(0)
    }
}
