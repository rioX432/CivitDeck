@file:OptIn(ExperimentalForeignApi::class)

package com.riox432.civitdeck.data.api.comfyui

import kotlinx.cinterop.CPointer
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.UByteVar
import kotlinx.cinterop.allocArray
import kotlinx.cinterop.convert
import kotlinx.cinterop.get
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.reinterpret
import platform.CoreCrypto.CC_SHA256
import platform.CoreCrypto.CC_SHA256_DIGEST_LENGTH
import platform.CoreFoundation.CFArrayGetCount
import platform.CoreFoundation.CFArrayGetValueAtIndex
import platform.CoreFoundation.CFDataGetBytePtr
import platform.CoreFoundation.CFDataGetLength
import platform.CoreFoundation.CFIndex
import platform.CoreFoundation.CFRelease
import platform.Foundation.NSURLAuthenticationChallenge
import platform.Foundation.NSURLAuthenticationMethodServerTrust
import platform.Foundation.NSURLCredential
import platform.Foundation.NSURLSessionAuthChallengeCancelAuthenticationChallenge
import platform.Foundation.NSURLSessionAuthChallengeDisposition
import platform.Foundation.NSURLSessionAuthChallengePerformDefaultHandling
import platform.Foundation.NSURLSessionAuthChallengeUseCredential
import platform.Foundation.credentialForTrust
import platform.Foundation.serverTrust
import platform.Security.SecCertificateCopyData
import platform.Security.SecCertificateRef
import platform.Security.SecTrustCopyCertificateChain
import platform.Security.SecTrustRef

/** How to answer an `NSURLAuthenticationChallenge`: the arguments for its completion handler. */
class ComfyUIServerTrustDecision(
    val disposition: NSURLSessionAuthChallengeDisposition,
    val credential: NSURLCredential?,
)

/**
 * Accepts only a server whose leaf certificate matches [trust]'s pin, and records the presented
 * fingerprint on [trust] before deciding. Chain, hostname and validity dates are deliberately not
 * checked: a pinned self-signed certificate is trusted for exactly its own bytes.
 *
 * A rejection cancels the challenge instead of falling back to default handling, because default
 * handling would accept any other certificate a public CA signed and so bypass the pin. Accepting
 * with a credential built from the server trust only works while App Transport Security does not
 * cover the ComfyUI host (`NSAllowsArbitraryLoads` in `Info.plist`).
 *
 * Never throws, so Swift `URLSessionDelegate`s can call [evaluate] directly.
 */
class ComfyUIServerTrustEvaluator(private val trust: ComfyUIServerTrust.PinnedLeaf) {

    /** Decides a challenge; anything other than a server-trust challenge gets default handling. */
    fun evaluate(challenge: NSURLAuthenticationChallenge): ComfyUIServerTrustDecision {
        val protectionSpace = challenge.protectionSpace
        if (protectionSpace.authenticationMethod != NSURLAuthenticationMethodServerTrust) {
            return ComfyUIServerTrustDecision(NSURLSessionAuthChallengePerformDefaultHandling, null)
        }
        return evaluate(protectionSpace.serverTrust)
    }

    internal fun evaluate(serverTrust: SecTrustRef?): ComfyUIServerTrustDecision {
        // Cleared first so a handshake without a readable certificate cannot leave the value of
        // an earlier handshake behind.
        trust.presentedSha256 = null
        if (serverTrust == null) return CANCEL
        val presented = leafCertificateSha256(serverTrust) ?: return CANCEL
        trust.presentedSha256 = presented
        if (presented != trust.expectedSha256) return CANCEL
        return ComfyUIServerTrustDecision(
            NSURLSessionAuthChallengeUseCredential,
            NSURLCredential.credentialForTrust(serverTrust),
        )
    }

    private companion object {
        val CANCEL = ComfyUIServerTrustDecision(NSURLSessionAuthChallengeCancelAuthenticationChallenge, null)
    }
}

/** SHA-256 of the leaf certificate's DER bytes as 64 lowercase hex characters. */
private fun leafCertificateSha256(serverTrust: SecTrustRef): String? {
    val chain = SecTrustCopyCertificateChain(serverTrust) ?: return null
    try {
        if (CFArrayGetCount(chain) < 1) return null
        val leaf: SecCertificateRef = CFArrayGetValueAtIndex(chain, 0)?.reinterpret() ?: return null
        val der = SecCertificateCopyData(leaf) ?: return null
        try {
            return sha256Hex(CFDataGetBytePtr(der), CFDataGetLength(der))
        } finally {
            CFRelease(der)
        }
    } finally {
        CFRelease(chain)
    }
}

private fun sha256Hex(bytes: CPointer<UByteVar>?, length: CFIndex): String? {
    if (bytes == null || length <= 0) return null
    return memScoped {
        val digest = allocArray<UByteVar>(CC_SHA256_DIGEST_LENGTH)
        CC_SHA256(bytes, length.convert(), digest)
        (0 until CC_SHA256_DIGEST_LENGTH).joinToString("") { digest[it].toString(16).padStart(2, '0') }
    }
}
