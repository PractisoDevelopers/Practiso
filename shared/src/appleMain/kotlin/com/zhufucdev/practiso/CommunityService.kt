package com.zhufucdev.practiso

import com.zhufucdev.practiso.convert.NSData
import com.zhufucdev.practiso.datamodel.AuthorizationToken
import com.zhufucdev.practiso.service.CommunityIdentity
import com.zhufucdev.practiso.service.CommunityService
import kotlinx.cinterop.ByteVar
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.MemScope
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.toKString
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import platform.CoreFoundation.CFDictionaryAddValue
import platform.CoreFoundation.CFDictionaryCreateMutable
import platform.CoreFoundation.CFDictionaryGetValue
import platform.CoreFoundation.CFDictionaryRef
import platform.CoreFoundation.CFMutableDictionaryRef
import platform.CoreFoundation.CFTypeRefVar
import platform.Foundation.CFBridgingRelease
import platform.Foundation.CFBridgingRetain
import platform.Security.SecItemAdd
import platform.Security.SecItemCopyMatching
import platform.Security.SecItemDelete
import platform.Security.errSecItemNotFound
import platform.Security.errSecSuccess
import platform.Security.kSecAttrAccount
import platform.Security.kSecAttrServer
import platform.Security.kSecAttrService
import platform.Security.kSecClass
import platform.Security.kSecClassInternetPassword
import platform.Security.kSecMatchLimit
import platform.Security.kSecMatchLimitOne
import platform.Security.kSecValueData
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

const val COMMUNITY_ACCOUNT = "community"

class KeychainError(val status: Int) : IllegalStateException("unexpected Keychain status: $status")

@Suppress("UNCHECKED_CAST")
@OptIn(ExperimentalForeignApi::class)
class KeychainCommunityIdentity(private val endpoint: String) : CommunityIdentity {
    override val authToken: StateFlow<AuthorizationToken?> = MutableStateFlow(run {
        memScoped {
            withSecQuery { parameters ->
                CFDictionaryAddValue(parameters, kSecMatchLimit, kSecMatchLimitOne)
                val item = alloc<CFTypeRefVar>()
                val status = SecItemCopyMatching(parameters, item.ptr)
                if (status == errSecItemNotFound) {
                    null
                } else if (status != errSecSuccess) {
                    throw KeychainError(status)
                } else {
                    val result = item.ptr as CFDictionaryRef
                    val tokenPtr = CFDictionaryGetValue(result, kSecValueData) as CPointer<ByteVar>
                    AuthorizationToken(tokenPtr.toKString())
                }
            }
        }
    })

    @OptIn(ExperimentalContracts::class)
    private fun <T> MemScope.withSecQuery(block: (CFMutableDictionaryRef) -> T): T {
        contract {
            callsInPlace(block, InvocationKind.EXACTLY_ONCE)
        }
        val parameters = CFDictionaryCreateMutable(null, 3, null, null)
        CFDictionaryAddValue(parameters, kSecClass, kSecClassInternetPassword)
        CFDictionaryAddValue(
            parameters,
            kSecAttrAccount,
            CFBridgingRetain(NSData(COMMUNITY_ACCOUNT))
        )
        CFDictionaryAddValue(
            parameters,
            kSecAttrServer,
            CFBridgingRetain(NSData(endpoint))
        )
        val result = block(parameters!!)
        CFBridgingRelease(parameters)
        return result
    }

    override fun setAuthToken(value: AuthorizationToken) {
        val status = memScoped {
            withSecQuery { parameters ->
                CFDictionaryAddValue(
                    parameters,
                    kSecValueData,
                    CFBridgingRetain(NSData(value.toString()))
                )
                SecItemAdd(parameters, null)
            }
        }
        if (status != errSecSuccess) {
            throw KeychainError(status)
        }
    }

    @OptIn(ExperimentalForeignApi::class)
    override fun clear() {
        memScoped {
            withSecQuery(::SecItemDelete)
        }
    }
}
