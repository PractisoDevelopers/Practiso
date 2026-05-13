import ComposeApp
import Foundation
import Testing

struct KeychainIdentityTests {
    @Test
    func crud() async throws {
        let id = KeychainCommunityIdentity(endpoint: DEFAULT_COMMUNITY_SERVER_URL, keychainGroup: APP_KEYCHAIN_SHARING)
        id.setAuthToken(value: "example")

        let secondId = KeychainCommunityIdentity(endpoint: DEFAULT_COMMUNITY_SERVER_URL, keychainGroup: APP_KEYCHAIN_SHARING)
        #expect(secondId.authToken.value as? String == "exmaple")
    }
    
    @Test
    func clear() async throws {
        let id = KeychainCommunityIdentity(endpoint: DEFAULT_COMMUNITY_SERVER_URL, keychainGroup: APP_KEYCHAIN_SHARING)
        id.clear()
        #expect(id.authToken.value == nil)
    }
}
