import AVFoundation
import Shared
import SwiftUI

struct ConnectionOnboardingView: View {
    @StateObject private var viewModel = ConnectionOnboardingViewModelOwner()
    @Environment(\.dismiss) private var dismiss

    var body: some View {
        Group {
            switch viewModel.stage {
            case .chooseMethod:
                MethodPickerView(viewModel: viewModel)
            case let .scanning(results):
                ScanningView(viewModel: viewModel, results: results)
            case let .testing(hostname):
                TestingView(hostname: hostname)
            case let .success(name, gpu, vramMB):
                SuccessView(name: name, gpu: gpu, vramMB: vramMB) { dismiss() }
            case let .failure(cause, httpStatus, presentedSha256):
                FailureView(
                    viewModel: viewModel,
                    cause: cause,
                    httpStatus: httpStatus,
                    presentedSha256: presentedSha256
                )
            }
        }
        .navigationTitle("Connect to ComfyUI")
        .navigationBarTitleDisplayMode(.inline)
        .task { await viewModel.observeUiState() }
    }
}

// MARK: - Method picker

private struct MethodPickerView: View {
    @ObservedObject var viewModel: ConnectionOnboardingViewModelOwner
    @State private var showScanner = false

    var body: some View {
        Form {
            Section {
                Text("Choose how to connect to your ComfyUI server.")
                    .font(.civitBodyMedium)
            }
            if viewModel.lanScanSupported {
                Section {
                    Button {
                        viewModel.startScan()
                    } label: {
                        MethodRow(
                            title: "Auto-detect on this network",
                            subtitle: "Scan your LAN for a running ComfyUI server"
                        )
                    }
                }
            }
            Section {
                Button {
                    showScanner = true
                } label: {
                    MethodRow(
                        title: "Scan QR code",
                        subtitle: "Point the camera at a server QR code"
                    )
                }
            }
            ManualEntrySection(viewModel: viewModel)
        }
        .sheet(isPresented: $showScanner) {
            NavigationStack {
                ConnectionQRScannerView { raw in
                    showScanner = false
                    viewModel.qrScanned(raw)
                }
            }
        }
    }
}

private struct MethodRow: View {
    let title: String
    let subtitle: String

    var body: some View {
        VStack(alignment: .leading, spacing: Spacing.xs) {
            Text(title).font(.civitTitleMedium)
            Text(subtitle).font(.civitBodySmall).foregroundColor(.civitOnSurfaceVariant)
        }
    }
}

// MARK: - Manual entry

private struct ManualEntrySection: View {
    @ObservedObject var viewModel: ConnectionOnboardingViewModelOwner

    @State private var name = ""
    @State private var host = ""
    @State private var port = "8188"
    @State private var useHttps = false
    @State private var acceptSelfSigned = false

    var body: some View {
        Section("Enter manually") {
            TextField("Name (optional)", text: $name)
            TextField("Hostname / IP", text: $host)
                .textInputAutocapitalization(.never)
                .autocorrectionDisabled()
            TextField("Port", text: $port)
                .keyboardType(.numberPad)
            Toggle("Use HTTPS", isOn: $useHttps)
            Toggle("Accept self-signed certificates", isOn: $acceptSelfSigned)
            Button("Connect") {
                viewModel.manualSubmit(
                    name: name,
                    hostname: host.trimmingCharacters(in: .whitespaces),
                    port: Int32(port) ?? 8188,
                    useHttps: useHttps,
                    acceptSelfSigned: acceptSelfSigned
                )
            }
            .disabled(host.trimmingCharacters(in: .whitespaces).isEmpty)
        }
    }
}

// MARK: - Scanning

private struct ScanningView: View {
    @ObservedObject var viewModel: ConnectionOnboardingViewModelOwner
    let results: [DiscoveredServer]

    var body: some View {
        List {
            Section {
                HStack(spacing: Spacing.md) {
                    ProgressView()
                    Text("Scanning your network…").font(.civitBodyMedium)
                }
            }
            if results.isEmpty {
                Text("No servers found yet. Make sure ComfyUI is running with --listen.")
                    .font(.civitBodySmall)
                    .foregroundColor(.civitOnSurfaceVariant)
            } else {
                Section("Found") {
                    ForEach(results, id: \.ip) { server in
                        Button {
                            viewModel.selectServer(server)
                        } label: {
                            MethodRow(title: server.displayName, subtitle: "\(server.ip):\(server.port)")
                        }
                    }
                }
            }
            Button("Back") { viewModel.chooseMethod() }
        }
    }
}

// MARK: - Testing

private struct TestingView: View {
    let hostname: String

    var body: some View {
        VStack(spacing: Spacing.md) {
            ProgressView()
            Text("Testing connection to \(hostname)…").font(.civitBodyMedium)
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
    }
}

// MARK: - Success

private struct SuccessView: View {
    let name: String
    let gpu: String?
    let vramMB: Int64
    let onDone: () -> Void

    var body: some View {
        VStack(spacing: Spacing.md) {
            Image(systemName: "checkmark.circle.fill")
                .font(.civitIconExtraLarge)
                .foregroundColor(.civitPrimary)
                .accessibilityHidden(true)
            Text("Connected").font(.civitHeadlineSmall)
            Text("\(name) is connected and ready.")
                .font(.civitBodyMedium)
                .multilineTextAlignment(.center)
            if let gpu {
                Text("\(gpu) • \(vramMB) MB VRAM")
                    .font(.civitBodySmall)
                    .foregroundColor(.civitOnSurfaceVariant)
            }
            Button("Done", action: onDone)
                .buttonStyle(.borderedProminent)
        }
        .padding(Spacing.lg)
        .frame(maxWidth: .infinity, maxHeight: .infinity)
    }
}

// MARK: - Failure

private struct FailureView: View {
    @ObservedObject var viewModel: ConnectionOnboardingViewModelOwner
    let cause: ConnectionFailureCause
    let httpStatus: Int32?
    let presentedSha256: String?

    var body: some View {
        // Scrolls so the fingerprint and its buttons stay reachable at large Dynamic Type sizes,
        // while the minimum height keeps short messages vertically centered.
        GeometryReader { proxy in
            ScrollView {
                content
                    .padding(Spacing.lg)
                    .frame(maxWidth: .infinity, minHeight: proxy.size.height)
            }
        }
    }

    private var content: some View {
        VStack(spacing: Spacing.md) {
            Image(systemName: "exclamationmark.triangle.fill")
                .font(.civitIconExtraLarge)
                .foregroundColor(.civitError)
                .accessibilityHidden(true)
            Text(message)
                .font(.civitBodyMedium)
                .multilineTextAlignment(.center)
            if let fingerprint = confirmableFingerprint {
                CertificateFingerprintView(sha256Hex: fingerprint)
                Button("comfyui_onboarding_trust_certificate") { viewModel.trustCertificate() }
                    .buttonStyle(.borderedProminent)
                Button("Retry") { viewModel.retry() }
                    .buttonStyle(.bordered)
            } else {
                Button("Retry") { viewModel.retry() }
                    .buttonStyle(.borderedProminent)
            }
            Button("Back") { viewModel.chooseMethod() }
        }
        // The regular size renders bordered buttons shorter than the 44pt minimum touch target.
        .controlSize(.large)
    }

    private var confirmableFingerprint: String? {
        switch cause {
        case .certificateUnconfirmed, .certificateChanged:
            return presentedSha256
        default:
            return nil
        }
    }

    private var message: String {
        switch cause {
        case .unreachable:
            return "Server unreachable. Check the host, port and that ComfyUI is running."
        case .timeout:
            return "Connection timed out. The server may be offline or on a different network."
        case .tls:
            return "TLS error. On iOS, install the certificate or use a trusted tunnel."
        case .certificateUnconfirmed:
            return String(localized: "comfyui_onboarding_fail_cert_unconfirmed")
        case .certificateChanged:
            return String(localized: "comfyui_onboarding_fail_cert_changed")
        case .http:
            return "Server responded with an error (HTTP \(httpStatus ?? 0))."
        case .unknown:
            return "Could not connect. Check the address and try again."
        default:
            return "Could not connect. Check the address and try again."
        }
    }
}

private struct CertificateFingerprintView: View {
    let sha256Hex: String

    var body: some View {
        VStack(alignment: .leading, spacing: Spacing.sm) {
            Text("comfyui_onboarding_cert_fingerprint_label")
                .font(.civitTitleSmall)
            // Wraps instead of truncating so every byte stays comparable with the server's output.
            Text(Self.format(sha256Hex))
                .font(.civitMonoCaption)
                .fixedSize(horizontal: false, vertical: true)
                .textSelection(.enabled)
            Text("comfyui_onboarding_cert_fingerprint_hint")
                .font(.civitBodySmall)
                .foregroundColor(.civitOnSurfaceVariant)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(Spacing.md)
        .background(Color.civitSurfaceContainer)
        .clipShape(RoundedRectangle(cornerRadius: CornerRadius.card))
    }

    /// Formats a lowercase hex SHA-256 digest the way `openssl x509 -noout -fingerprint -sha256`
    /// prints it (colon-separated uppercase byte pairs), so the user can compare the two directly.
    static func format(_ sha256Hex: String) -> String {
        let hex = Array(sha256Hex.uppercased())
        return stride(from: 0, to: hex.count, by: 2)
            .map { String(hex[$0..<min($0 + 2, hex.count)]) }
            .joined(separator: ":")
    }
}

// MARK: - QR scanner (raw payload)

private struct ConnectionQRScannerView: View {
    @Environment(\.dismiss) private var dismiss
    let onScanned: (String) -> Void

    @State private var didScan = false
    @State private var permissionGranted = false

    var body: some View {
        ZStack {
            if permissionGranted {
                ConnectionCameraPreview(onDetected: handle)
                    .ignoresSafeArea()
                RoundedRectangle(cornerRadius: Spacing.md)
                    .stroke(Color.civitOnSurface.opacity(0.8), lineWidth: 2)
                    .frame(width: 250, height: 250)
            } else {
                Color.civitScrim.ignoresSafeArea()
                Text("Camera permission is required to scan QR codes")
                    .font(.civitBodyMedium)
                    .foregroundColor(.civitOnSurface)
                    .padding(Spacing.lg)
            }
        }
        .navigationTitle("Scan QR Code")
        .navigationBarTitleDisplayMode(.inline)
        .toolbar {
            ToolbarItem(placement: .cancellationAction) {
                Button("Cancel") { dismiss() }
            }
        }
        .task { await requestPermission() }
    }

    private func requestPermission() async {
        switch AVCaptureDevice.authorizationStatus(for: .video) {
        case .authorized:
            permissionGranted = true
        case .notDetermined:
            permissionGranted = await AVCaptureDevice.requestAccess(for: .video)
        default:
            permissionGranted = false
        }
    }

    private func handle(_ value: String) {
        guard !didScan else { return }
        didScan = true
        HapticFeedback.success.trigger()
        onScanned(value)
    }
}

private struct ConnectionCameraPreview: UIViewRepresentable {
    let onDetected: (String) -> Void

    func makeCoordinator() -> Coordinator { Coordinator(onDetected: onDetected) }

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
        previewLayer.frame = view.bounds
        view.layer.addSublayer(previewLayer)
        context.coordinator.previewLayer = previewLayer

        DispatchQueue.global(qos: .userInitiated).async { session.startRunning() }
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
        let onDetected: (String) -> Void

        init(onDetected: @escaping (String) -> Void) { self.onDetected = onDetected }

        func metadataOutput(
            _ output: AVCaptureMetadataOutput,
            didOutput metadataObjects: [AVMetadataObject],
            from connection: AVCaptureConnection
        ) {
            guard let object = metadataObjects.first as? AVMetadataMachineReadableCodeObject,
                  object.type == .qr,
                  let value = object.stringValue else { return }
            onDetected(value)
        }
    }
}
