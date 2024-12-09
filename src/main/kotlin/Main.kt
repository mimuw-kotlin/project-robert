import androidx.compose.ui.window.application
import io.ktor.client.engine.okhttp.*

fun getHttpClientEngine() = OkHttp.create()

fun main() = application {
    getHttpClientEngine().use { httpClientEngine ->
        Client(httpClientEngine).use { client ->
            UserInput(client).run()
        }
    }
}
