import Foundation

/// Compile-time feature flags. Mirrors Android's `BuildConfig` fields so the two
/// platforms can gate experiments in lockstep.
///
/// Default off until the on-device SigLIP-2 embedder is producing real vectors
/// (parent #602, phases C/D). Flip individual flags manually for development.
enum FeatureFlags {
    /// Gates the embedding-based "Find Similar Models" UI surface.
    static let similaritySearch = false
}
