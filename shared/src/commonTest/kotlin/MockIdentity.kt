import com.zhufucdev.practiso.datamodel.AuthorizationToken
import com.zhufucdev.practiso.service.CommunityIdentity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class MockIdentity : CommunityIdentity {
    override val authToken: StateFlow<AuthorizationToken?> = MutableStateFlow(null)

    override fun setAuthToken(value: AuthorizationToken) {
        (authToken as MutableStateFlow<AuthorizationToken?>).value = value
    }

    override fun clear() {
        (authToken as MutableStateFlow<AuthorizationToken?>).value = null
    }
}

