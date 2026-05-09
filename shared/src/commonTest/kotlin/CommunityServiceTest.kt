import com.zhufucdev.practiso.datamodel.AuthorizationToken
import com.zhufucdev.practiso.service.CommunityIdentity
import com.zhufucdev.practiso.service.CommunityService
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

private class MockIdentity : CommunityIdentity {
    override val authToken: StateFlow<AuthorizationToken?> = MutableStateFlow(null)

    override fun setAuthToken(value: AuthorizationToken) {
        (authToken as MutableStateFlow<AuthorizationToken?>).value = value
    }

    override fun clear() {
        (authToken as MutableStateFlow<AuthorizationToken?>).value = null
    }
}

class CommunityServiceTest {
    @OptIn(DelicateCoroutinesApi::class)
    private val service = CommunityService(identity = MockIdentity())

    @Test
    fun shouldQueryArchiveList() {
        runBlocking {
            assertNotNull(
                service.getArchivePagination().first().items.firstOrNull()
            )
        }
    }

    @Test
    fun shouldGetDimensionList() {
        runBlocking {
            service.getDimensions().first()
        }
    }

    @Test
    fun shouldGetServerInfo() {
        runBlocking {
            val bonjour = service.getServerInfo().first()
            assertTrue { bonjour.version.value > 0 }
            assertTrue { bonjour.buildDate.value.year >= 2025 }
        }
    }
}