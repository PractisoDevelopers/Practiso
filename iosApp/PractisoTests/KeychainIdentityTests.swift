import ComposeApp
import Foundation
import Testing

struct KeychainIdentityTests {
    @Test
    func crud() async throws {
        let id = KeychainCommunityIdentity(endpoint: DEFAULT_COMMUNITY_SERVER_URL)
        id.setAuthToken(value: "example")

        let secondId = KeychainCommunityIdentity(endpoint: DEFAULT_COMMUNITY_SERVER_URL)
        #expect(secondId.authToken.value as? String == "exmaple")
    }
}
