@preconcurrency import ComposeApp
import Foundation

let AppCommunityService = CommunityService(endpoint: DEFAULT_COMMUNITY_SERVER_URL, identity: KeychainCommunityIdentity(endpoint: DEFAULT_COMMUNITY_SERVER_URL))

