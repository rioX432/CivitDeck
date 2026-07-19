import Foundation

/// Compile-time feature flags. Mirrors Android's `BuildConfig` fields so the two
/// platforms can gate experiments in lockstep.
///
/// Default off until the on-device SigLIP-2 embedder is producing real vectors
/// (parent #602, phases C/D). Flip individual flags manually for development.
enum FeatureFlags {
    /// Gates the embedding-based "Find Similar Models" / "AI Search" UI surface.
    ///
    /// Semantic (SigLIP-2) search is an **Android-only experiment**. iOS ships no
    /// text or vision encoder (see `core-ml/src/iosMain/.../ml/*`), so this flag
    /// must stay `false` on iOS — flipping it would surface a permanently empty,
    /// non-functional screen. Kept only to mirror Android's `BuildConfig` field.
    static let similaritySearch = false
}
