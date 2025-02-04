import androidx.compose.material.MaterialTheme
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.test.waitUntilAtLeastOneExists
import com.github.br0b.katrix.ChatScreen
import com.github.br0b.katrix.ChatViewModel
import io.ktor.client.engine.okhttp.OkHttp
import org.junit.Test

class AppTest {
    @OptIn(ExperimentalTestApi::class)
    @Test
    fun loginTest() =
        runComposeUiTest {
            setContent {
                MaterialTheme {
                    ChatScreen(ChatViewModel(), OkHttp.create())
                }
            }

            onNodeWithTag("LOGIN_BUTTON").performClick()
            waitUntilAtLeastOneExists(hasText("Homeserver"), 5000)
            onNodeWithTag("HOMESERVER_INPUT").performTextInput("http://localhost:8008")
            onNodeWithTag("USERNAME_INPUT").performTextInput("aha")
            onNodeWithTag("PASSWORD_INPUT").performTextInput("@3%(BzJtj|LxA\\U-<N*N")
            onNodeWithTag("LOGIN_DIALOG_BUTTON").performClick()
            waitUntilAtLeastOneExists(hasTestTag("LOGOUT_BUTTON"), 5000)
            onNodeWithTag("LOGOUT_BUTTON").assertIsDisplayed()
        }
}
