import Foundation
import CoreML
import Shared

/// Swift implementation of the Kotlin `SigLIP2Bridge` protocol.
///
/// Loads a SigLIP-2 vision encoder packaged as `SigLIP2.mlpackage` from the app bundle
/// and runs Core ML inference for the iosMain `ImageEmbeddingModel` actual.
///
/// **Current status (#700 — wiring only)**: the `.mlpackage` is not bundled yet, so
/// `model` resolves to `nil`, `isAvailable` returns `false`, and `embed(...)` invokes
/// the callback with `nil`. The Kotlin actual short-circuits on `isAvailable` so the
/// rest of the embedding pipeline behaves like a no-op until a follow-up issue ships
/// the model file plus the inference body.
///
/// **Preprocessing for the future implementation** (per `docs/research/siglip2-feasibility.md`):
/// - Resize 224×224 BICUBIC
/// - mean / std = `[0.5, 0.5, 0.5]` (NOT the 0.485/0.456/0.406 OpenAI/CLIP stats)
/// - NCHW float32 input
/// - Output L2-normalized 768-d vector so cosine similarity reduces to a dot product
final class SigLIP2Embedder: NSObject, SigLIP2Bridge {

    // MARK: SigLIP2Bridge protocol

    var isAvailable: Bool { model != nil }

    var embeddingModelId: String { Self.modelId }

    var dimension: Int32 { Int32(Self.outputDimension) }

    func embed(
        imageBytes: KotlinByteArray,
        onResult: @escaping (KotlinFloatArray?) -> Void
    ) {
        // No model file in this PR — see class header. Real inference is intentionally
        // omitted so the Kotlin side can wire up the bridge end-to-end without dragging
        // a ~85 MB binary into the repo.
        _ = imageBytes
        Self.inferenceQueue.async {
            onResult(nil)
        }
    }

    // MARK: Private

    private static let modelId = "siglip2-base-p16-224"
    private static let outputDimension = 768
    private static let inferenceQueue = DispatchQueue(
        label: "com.riox432.civitdeck.siglip2",
        qos: .userInitiated
    )

    /// Lazily resolved Core ML model. Returns `nil` (and stays `nil`) when the
    /// `.mlpackage` is missing from the bundle — that's the expected state in this PR.
    private lazy var model: MLModel? = {
        guard let url = Bundle.main.url(forResource: "SigLIP2", withExtension: "mlpackage") else {
            return nil
        }
        return try? MLModel(contentsOf: url)
    }()
}
