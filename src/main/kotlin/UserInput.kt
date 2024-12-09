import io.ktor.http.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import net.folivo.trixnity.client.flatten
import net.folivo.trixnity.clientserverapi.model.authentication.IdentifierType
import net.folivo.trixnity.core.model.RoomId
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * Handles user input and interacts with the [Client].
 */
class UserInput(
    private val client: Client
) {
    private val logger: Logger = LoggerFactory.getLogger(UserInput::class.java)

    fun run() = runBlocking {
        var command: String?

        while (true) {
            print("> ")
            command = readlnOrNull()
            when (command) {
                "q" -> break
                "login" -> {
                    logger.info("Logging in...")
                    login().fold(
                        onSuccess = { logger.info("Login successful") },
                        onFailure = { error -> logger.error("Login failed: $error") }
                    )
                }

                "logout" -> {
                    logger.info("Logging out...")
                    client.logout().fold(
                        onSuccess = { logger.info("Logout successful") },
                        onFailure = { error -> logger.error("Logout failed: $error") }
                    )
                }

                "rooms" -> {
                    logger.info("Getting rooms...")
                    println(client.getRooms()?.flatten()?.first()?.keys)
                    logger.info("Rooms retrieved")
                }

                "send" -> {
                    print("Room ID: ")
                    val roomId = readln()
                    print("Text: ")
                    val text = readln()
                    val message = Client.Message(RoomId(roomId), text)
                    println("Sending message...")
                    client.send(message)
                    println("Message sent")
                }

                else -> println("Unknown command")
            }
        }
    }

    private suspend fun login(): Result<Unit> {
        print("Base URL: ")
        val baseUrl = readln()
        print("Username: ")
        val username = readln()
        print("Password: ")
        val password = readln()

        val loginData = Client.LoginData(Url(baseUrl), IdentifierType.User(username), password)

        return client.login(loginData)
    }
}
