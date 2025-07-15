import com.zhufucdev.practiso.service.CommunityService
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertNotNull

class CommunityServiceTest {
    private val service = CommunityService()

    @Test
    fun shouldQueryArchiveList() {
        runBlocking {
            assertNotNull(
                service.getArchives().items.firstOrNull()
            )
        }
    }
}