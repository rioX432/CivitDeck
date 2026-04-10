import CoreGraphics
import CoreML
import Foundation
import Shared
import UIKit

/// Swift implementation of the Kotlin `SigLIP2Bridge` protocol.
///
/// Loads a SigLIP-2 vision encoder compiled from `SigLIP2.mlpackage` and runs
/// Core ML inference to produce 768-d L2-normalized image embeddings.
///
/// **Preprocessing** (per `docs/research/siglip2-feasibility.md`):
/// - Resize 224x224 BICUBIC
/// - mean / std = `[0.5, 0.5, 0.5]`
/// - NCHW float32 input
/// - Output is L2-normalized so cosine similarity reduces to a dot product
final class SigLIP2Embedder: NSObject, SigLIP2Bridge {

    // MARK: SigLIP2Bridge protocol

    var isAvailable: Bool { model != nil }

    var embeddingModelId: String { Self.modelId }

    var dimension: Int32 { Int32(Self.outputDimension) }

    func embed(
        imageBytes: KotlinByteArray,
        onResult: @escaping (KotlinFloatArray?) -> Void
    ) {
        Self.inferenceQueue.async { [weak self] in
            guard let self, let model = self.model else {
                onResult(nil)
                return
            }
            let result = self.runInference(
                imageBytes: imageBytes,
                model: model
            )
            onResult(result)
        }
    }

    // MARK: Private — constants

    private static let modelId = "siglip2-base-patch16-224-coreml"
    private static let outputDimension = 768
    private static let imageSize = 224
    private static let normMean: Float = 0.5
    private static let normStd: Float = 0.5
    private static let inferenceQueue = DispatchQueue(
        label: "com.riox432.civitdeck.siglip2",
        qos: .userInitiated
    )

    // MARK: Private — model

    /// Lazily resolved Core ML model. Returns `nil` when the compiled model
    /// is missing from the bundle (e.g. `.mlpackage` not yet shipped).
    private lazy var model: MLModel? = {
        // Xcode compiles .mlpackage to .mlmodelc at build time
        guard let url = Bundle.main.url(
            forResource: "SigLIP2",
            withExtension: "mlmodelc"
        ) else {
            return nil
        }
        do {
            return try MLModel(contentsOf: url)
        } catch {
            print("SigLIP2Embedder: model loading failed: \(error)")
            return nil
        }
    }()

    // MARK: Private — inference

    private func runInference(
        imageBytes: KotlinByteArray,
        model: MLModel
    ) -> KotlinFloatArray? {
        guard let inputArray = preprocessImage(imageBytes: imageBytes) else {
            return nil
        }

        let featureValue = MLFeatureValue(multiArray: inputArray)
        let provider: MLDictionaryFeatureProvider
        do {
            provider = try MLDictionaryFeatureProvider(
                dictionary: ["pixel_values": featureValue]
            )
        } catch {
            print("SigLIP2Embedder: failed to create feature provider: \(error)")
            return nil
        }

        let prediction: MLFeatureProvider
        do {
            prediction = try model.prediction(from: provider)
        } catch {
            print("SigLIP2Embedder: prediction failed: \(error)")
            return nil
        }

        return extractEmbedding(from: prediction)
    }

    // MARK: Private — preprocessing

    private func preprocessImage(
        imageBytes: KotlinByteArray
    ) -> MLMultiArray? {
        let data = KotlinByteArrayUtils.toData(imageBytes)
        guard let uiImage = UIImage(data: data) else { return nil }
        guard let cgImage = resizeBicubic(
            uiImage,
            to: CGSize(
                width: Self.imageSize,
                height: Self.imageSize
            )
        ) else { return nil }

        return imageToMLMultiArray(cgImage)
    }

    private func resizeBicubic(
        _ image: UIImage,
        to size: CGSize
    ) -> CGImage? {
        guard let cgImage = image.cgImage else { return nil }
        let context = CGContext(
            data: nil,
            width: Int(size.width),
            height: Int(size.height),
            bitsPerComponent: 8,
            bytesPerRow: Int(size.width) * 4,
            space: CGColorSpaceCreateDeviceRGB(),
            bitmapInfo: CGImageAlphaInfo.premultipliedLast.rawValue
        )
        context?.interpolationQuality = .high
        context?.draw(cgImage, in: CGRect(origin: .zero, size: size))
        return context?.makeImage()
    }

    /// Converts a 224x224 CGImage to an NCHW float32 MLMultiArray with
    /// mean=0.5, std=0.5 normalization.
    private func imageToMLMultiArray(
        _ cgImage: CGImage
    ) -> MLMultiArray? {
        let width = Self.imageSize
        let height = Self.imageSize

        let array: MLMultiArray
        do {
            array = try MLMultiArray(
                shape: [1, 3, NSNumber(value: height), NSNumber(value: width)],
                dataType: .float32
            )
        } catch {
            print("SigLIP2Embedder: failed to create MLMultiArray: \(error)")
            return nil
        }

        guard let context = CGContext(
            data: nil,
            width: width,
            height: height,
            bitsPerComponent: 8,
            bytesPerRow: width * 4,
            space: CGColorSpaceCreateDeviceRGB(),
            bitmapInfo: CGImageAlphaInfo.premultipliedLast.rawValue
        ) else { return nil }

        context.draw(
            cgImage,
            in: CGRect(x: 0, y: 0, width: width, height: height)
        )
        guard let pixelData = context.data else { return nil }

        let pixels = pixelData.bindMemory(
            to: UInt8.self,
            capacity: width * height * 4
        )
        fillMultiArray(
            array: array,
            pixels: pixels,
            width: width,
            height: height
        )
        return array
    }

    /// Fills NCHW MLMultiArray from RGBA pixel buffer with normalization.
    private func fillMultiArray(
        array: MLMultiArray,
        pixels: UnsafePointer<UInt8>,
        width: Int,
        height: Int
    ) {
        let channelStride = height * width
        let mean = Self.normMean
        let std = Self.normStd

        for row in 0..<height {
            for col in 0..<width {
                let pixelIndex = (row * width + col) * 4
                let red = Float(pixels[pixelIndex]) / 255.0
                let green = Float(pixels[pixelIndex + 1]) / 255.0
                let blue = Float(pixels[pixelIndex + 2]) / 255.0

                let spatialIndex = row * width + col
                // NCHW: [batch=0, channel, height, width]
                array[spatialIndex] = NSNumber(
                    value: (red - mean) / std
                )
                array[channelStride + spatialIndex] = NSNumber(
                    value: (green - mean) / std
                )
                array[2 * channelStride + spatialIndex] = NSNumber(
                    value: (blue - mean) / std
                )
            }
        }
    }

    // MARK: Private — output extraction

    private func extractEmbedding(
        from prediction: MLFeatureProvider
    ) -> KotlinFloatArray? {
        // Try known output names from conversion
        let outputNames = ["embedding", "pooler_output", "var_893"]
        var outputArray: MLMultiArray?
        for name in outputNames {
            if let value = prediction.featureValue(for: name)?.multiArrayValue {
                outputArray = value
                break
            }
        }

        // Fallback: use the first available output
        if outputArray == nil {
            for name in prediction.featureNames {
                if let value = prediction.featureValue(
                    for: name
                )?.multiArrayValue {
                    outputArray = value
                    break
                }
            }
        }

        guard let embedding = outputArray else { return nil }
        return l2Normalize(embedding)
    }

    /// L2-normalizes the embedding and converts to KotlinFloatArray.
    private func l2Normalize(
        _ multiArray: MLMultiArray
    ) -> KotlinFloatArray {
        let count = multiArray.count
        let result = KotlinFloatArray(size: Int32(count))

        // Compute L2 norm
        var sumSquared: Float = 0
        for idx in 0..<count {
            let val = multiArray[idx].floatValue
            sumSquared += val * val
        }
        let norm = max(sqrt(sumSquared), 1e-12)

        for idx in 0..<count {
            let normalized = multiArray[idx].floatValue / norm
            result.set(index: Int32(idx), value: normalized)
        }

        return result
    }
}

// MARK: - KotlinByteArray helpers

private enum KotlinByteArrayUtils {
    static func toData(_ byteArray: KotlinByteArray) -> Data {
        let size = byteArray.size
        var bytes = [UInt8](repeating: 0, count: Int(size))
        for idx in 0..<size {
            bytes[Int(idx)] = UInt8(bitPattern: byteArray.get(index: idx))
        }
        return Data(bytes)
    }
}
