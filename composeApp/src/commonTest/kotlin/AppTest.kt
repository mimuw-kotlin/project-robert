import androidx.compose.material.MaterialTheme
import androidx.compose.ui.test.*
import com.github.br0b.katrix.ChatScreen
import com.github.br0b.katrix.ChatViewModel
import io.ktor.client.engine.okhttp.*
import org.junit.Test

class AppTest {
    @OptIn(ExperimentalTestApi::class)
    @Test
    fun loginTest() = runComposeUiTest {
        setContent {
            MaterialTheme {
                ChatScreen(ChatViewModel(), OkHttp.create())
            }
        }

        onNodeWithTag("LOGIN_BUTTON").performClick()
        waitUntil { onNodeWithText("Homeserver").isDisplayed() }
        onNodeWithTag("HOMESERVER_INPUT").performTextInput("http://localhost:8008")
        onNodeWithTag("USERNAME_INPUT").performTextInput("aha")
        onNodeWithTag("PASSWORD_INPUT").performTextInput("@3%(BzJtj|LxA\\U-<N*N")
        onNodeWithTag("LOGIN_DIALOG_BUTTON").performClick()
        waitUntilAtLeastOneExists(hasTestTag("LOGOUT_BUTTON"), 5000)
        onNodeWithTag("LOGOUT_BUTTON").assertIsDisplayed()
    }
}