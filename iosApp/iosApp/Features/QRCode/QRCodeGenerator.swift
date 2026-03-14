import CoreImage
import CoreImage.CIFilterBuiltins
import UIKit

/// Generates a QR code UIImage from a URL string using CoreImage.
enum QRCodeGenerator {

    static func generate(from string: String, size: CGFloat = 512) -> UIImage? {
        let context = CIContext()
        let filter = CIFilter.qrCodeGenerator()
        guard let data = string.data(using: .utf8) else { return nil }
        // KVC-based API is the standard CIFilter interface for QR code generation;
        // there is no typed alternative for inputMessage/inputCorrectionLevel.
        filter.setValue(data, forKey: "inputMessage")
        filter.setValue("M", forKey: "inputCorrectionLevel")

        guard let ciImage = filter.outputImage else { return nil }

        let scaleX = size / ciImage.extent.size.width
        let scaleY = size / ciImage.extent.size.height
        let scaledImage = ciImage.transformed(by: CGAffineTransform(scaleX: scaleX, y: scaleY))

        guard let cgImage = context.createCGImage(scaledImage, from: scaledImage.extent) else {
            return nil
        }
        return UIImage(cgImage: cgImage)
    }
}
