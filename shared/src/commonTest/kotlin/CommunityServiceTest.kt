import com.zhufucdev.practiso.service.CommunityService
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class CommunityServiceTest {
    @OptIn(DelicateCoroutinesApi::class)
    private val service = CommunityService()

    @Test
    fun shouldQueryArchiveList() {
        runBlocking {
            assertNotNull(
                service.getArchivePagination().items.firstOrNull()
            )
        }
    }

    @Test
    fun shouldGetDimensionList() {
        runBlocking {
            service.getDimensions()
        }
    }

    @Test
    fun shouldGetServerInfo() {
        runBlocking {
            val bonjour = service.getServerInfo()
            assertTrue { bonjour.version.value > 0 }
            assertTrue { bonjour.buildDate.value.year >= 2025 }
        }
    }
}