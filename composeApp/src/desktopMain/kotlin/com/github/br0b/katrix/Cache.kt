import net.folivo.trixnity.client.store.repository.realm.createRealmRepositoriesModule
import net.folivo.trixnity.client.media.MediaStore
import net.folivo.trixnity.client.media.okio.OkioMediaStore
import okio.Path
import okio.Path.Companion.toPath
import org.koin.core.module.Module

/**
 * A cache for storing data.
 */
class Cache private constructor(
    private val repositoryModule: Module,
    private val mediaStore: MediaStore
) {
    companion object {
        fun create(path: Path? = null): Cache {
            val nonNullPath = path ?: System.getProperty("user.home").toPath().resolve(".cache/katrix")
            val repositoryModule = createRealmRepositoriesModule {
                directory(nonNullPath.resolve("realm").toString())
            }
            val mediaStore = OkioMediaStore(nonNullPath.resolve("media"))
            return Cache(
                repositoryModule = repositoryModule,
                mediaStore = mediaStore
            )
        }
    }

    fun getRepositoryModule(): Module = repositoryModule

    fun getMediaStore(): MediaStore = mediaStore
}