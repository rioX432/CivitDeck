import AVFoundation
import SwiftUI

struct QRScannerView: View {
    @Environment(\.dismiss) private var dismiss
    let onModelScanned: (Int64) -> Void

    @State private var scannedModelId: Int64?
    @State private var cameraPermission: CameraPermission = .unknown

    var body: some View {
        ZStack {
            if cameraPermission == .granted {
                CameraPreview(onQRCodeDetected: handleScan)
                    .ignoresSafeArea()
                scannerOverlay
            } else if cameraPermission == .denied {
                permissionDeniedView
            } else {
                Color.civitScrim.ignoresSafeArea()
            }
        }
        .navigationTitle("Scan QR Code")
        .navigationBarTitleDisplayMode(.inline)
        .task {
            await checkCameraPermission()
        }
    }

    // MARK: - Scanner Overlay

    private var scannerOverlay: some View {
        VStack {
            Spacer()
            RoundedRectangle(cornerRadius: Spacing.md)
                .stroke(Color.civitOnSurface.opacity(0.8), lineWidth: 2)
                .frame(width: 250, height: 250)
            Spacer().frame(height: Spacing.lg)
            Text("Point at a CivitAI model QR code")
                .font(.civitBodyMedium)
                .foregroundColor(.civitOnSurface)
                .padding(.horizontal, Spacing.md)
                .padding(.vertical, Spacing.sm)
                .background(Color.civitScrim.opacity(0.6))
                .clipShape(RoundedRectangle(cornerRadius: Spacing.sm))
            Spacer()
        }
    }

    // MARK: - Permission

    private var permissionDeniedView: some View {
        VStack(spacing: Spacing.md) {
            Image(systemName: "camera.fill")
                .accessibilityHidden(true)
                .font(.civitIconExtraLarge)
                .foregroundColor(.civitOnSurfaceVariant)
            Text("Camera permission is required to scan QR codes")
                .font(.civitBodyMedium)
                .multilineTextAlignment(.center)
                .foregroundColor(.civitOnSurfaceVariant)
            Button("Open Settings") {
                if let url = URL(string: UIApplication.openSettingsURLString) {
                    UIApplication.shared.open(url)
                }
            }
            .buttonStyle(.bordered)
        }
        .padding(Spacing.lg)
    }

    // MARK: - Helpers

    private func checkCameraPermission() async {
        switch AVCaptureDevice.authorizationStatus(for: .video) {
        case .authorized:
            cameraPermission = .granted
        case .notDetermined:
            let granted = await AVCaptureDevice.requestAccess(for: .video)
            cameraPermission = granted ? .granted : .denied
        default:
            cameraPermission = .denied
        }
    }

    private func handleScan(_ code: String) {
        guard scannedModelId == nil else { return }
        guard let modelId = Self.extractModelId(from: code) else { return }
        scannedModelId = modelId
        HapticFeedback.success.trigger()
        onModelScanned(modelId)
        dismiss()
    }

    static func extractModelId(from url: String) -> Int64? {
        guard let range = url.range(of: #"civitai\.com/models/(\d+)"#, options: .regularExpression) else {
            return nil
        }
        let match = url[range]
        let components = match.split(separator: "/")
        guard let idString = components.last else { return nil }
        return Int64(idString)
    }
}

// MARK: - Camera Permission State

private enum CameraPermission {
    case unknown, granted, denied
}

// MARK: - Camera Preview (AVFoundation)

private struct CameraPreview: UIViewRepresentable {
    let onQRCodeDetected: (String) -> Void

    func makeCoordinator() -> Coordinator {
        Coordinator(onQRCodeDetected: onQRCodeDetected)
    }

    func makeUIView(context: Context) -> UIView {
        let view = UIView(frame: .zero)
        let session = AVCaptureSession()
        context.coordinator.session = session

        guard let device = AVCaptureDevice.default(for: .video),
              let input = try? AVCaptureDeviceInput(device: device),
              session.canAddInput(input) else { return view }

        session.addInput(input)

        let output = AVCaptureMetadataOutput()
        if session.canAddOutput(output) {
            session.addOutput(output)
            output.setMetadataObjectsDelegate(context.coordinator, queue: .main)
            output.metadataObjectTypes = [.qr]
        }

        let previewLayer = AVCaptureVideoPreviewLayer(session: session)
        previewLayer.videoGravity = .resizeAspectFill
        previewLayer.frame = UIScreen.main.bounds
        view.layer.addSublayer(previewLayer)
        context.coordinator.previewLayer = previewLayer

        DispatchQueue.global(qos: .userInitiated).async {
            session.startRunning()
        }

        return view
    }

    func updateUIView(_ uiView: UIView, context: Context) {
        context.coordinator.previewLayer?.frame = uiView.bounds
    }

    static func dismantleUIView(_ uiView: UIView, coordinator: Coordinator) {
        coordinator.session?.stopRunning()
    }

    final class Coordinator: NSObject, AVCaptureMetadataOutputObjectsDelegate {
        var session: AVCaptureSession?
        var previewLayer: AVCaptureVideoPreviewLayer?
        let onQRCodeDetected: (String) -> Void

        init(onQRCodeDetected: @escaping (String) -> Void) {
            self.onQRCodeDetected = onQRCodeDetected
        }

        func metadataOutput(
            _ output: AVCaptureMetadataOutput,
            didOutput metadataObjects: [AVMetadataObject],
            from connection: AVCaptureConnection
        ) {
            guard let object = metadataObjects.first as? AVMetadataMachineReadableCodeObject,
                  object.type == .qr,
                  let value = object.stringValue else { return }
            onQRCodeDetected(value)
        }
    }
}
