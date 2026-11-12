package com.garfiec.librechat

import android.text.InputType
import android.view.View
import android.view.inputmethod.EditorInfo
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextReplacement
import com.garfiec.librechat.core.common.brand.ServerLock
import com.garfiec.librechat.core.ui.theme.LibreChatTheme
import com.garfiec.librechat.feature.auth.screen.ServerUrlScreen
import com.garfiec.librechat.feature.auth.viewmodel.ServerUrlViewModel
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.koin.mp.KoinPlatform

class AuthFlowTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var view: View

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
            accountRoster = koin.get(),
            serverLock = ServerLock(null),
        )
        composeTestRule.setContent {
            view = LocalView.current
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
            composeTestRule.onAllNodesWithText("Connecting to LID.IA…").fetchSemanticsNodes().isNotEmpty() ||
                composeTestRule.onAllNodesWithText("Retry").fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onAllNodesWithText("Server URL").assertCountEquals(0)
        composeTestRule.onNodeWithTag("lideri_logo").assertIsDisplayed()
    }

    @Test
    fun serverUrlScreen_fieldAsksForUrlKeyboardWithoutAutocorrect() {
        setUpServerUrlScreen()
        composeTestRule.onNodeWithTag("server_url_field").performClick()
        composeTestRule.waitForIdle()
        val info = EditorInfo()
        composeTestRule.runOnUiThread { view.onCreateInputConnection(info) }

        assertEquals(0, info.inputType and InputType.TYPE_TEXT_FLAG_AUTO_CORRECT)
        assertEquals(InputType.TYPE_TEXT_VARIATION_URI, info.inputType and InputType.TYPE_MASK_VARIATION)
        assertEquals(EditorInfo.IME_ACTION_GO, info.imeOptions and EditorInfo.IME_MASK_ACTION)
    }

    @Test
    fun serverUrlScreen_keyboardGoConnects() {
        setUpServerUrlScreen()
        // A cleartext IP answers with the insecure-connection prompt before any network call.
        composeTestRule.onNodeWithText("Server URL").performTextReplacement("http://192.168.1.1")
        composeTestRule.onNodeWithTag("server_url_field").performImeAction()
        composeTestRule.onNodeWithText("Insecure connection").assertIsDisplayed()
    }
}
