import com.zhufucdev.practiso.service.CommunityService
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertNotNull

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
}