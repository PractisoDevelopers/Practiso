import com.zhufucdev.practiso.DEFAULT_COMMUNITY_SERVER_URL
import com.zhufucdev.practiso.KeychainCommunityIdentity
import com.zhufucdev.practiso.datamodel.AuthorizationToken
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

private val EXAMPLE_TOKEN = AuthorizationToken("example")

class KeychainIdentityTest {
    @Test
    fun shouldCRUD() {
        val id = KeychainCommunityIdentity(DEFAULT_COMMUNITY_SERVER_URL)
        id.clear()
        assertNull(id.authToken.value, "clear not working")

        id.setAuthToken(EXAMPLE_TOKEN)
        assertEquals(EXAMPLE_TOKEN, id.authToken.value, "setAuthToken not working")

        val secondId = KeychainCommunityIdentity(DEFAULT_COMMUNITY_SERVER_URL)
        assertNotNull(secondId.authToken, "setToken not syncing")

        id.clear()
        assertNull(id.authToken.value, "clear not working")
        assertNull(secondId.authToken.value, "clear not syncing")
    }
}