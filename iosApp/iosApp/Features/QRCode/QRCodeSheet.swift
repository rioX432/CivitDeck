import SwiftUI

struct QRCodeSheet: View {
    let modelId: Int64
    let modelName: String
    @Environment(\.dismiss) private var dismiss

    private var civitaiUrl: String {
        CivitAiUrls.modelUrl(modelId: modelId)
    }

    private var qrImage: UIImage? {
        QRCodeGenerator.generate(from: civitaiUrl)
    }

    var body: some View {
        NavigationView {
            ScrollView {
                VStack(spacing: Spacing.lg) {
                    qrCardContent
                    shareButton
                }
                .padding(Spacing.lg)
            }
            .navigationTitle("QR Code")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Done") { dismiss() }
                }
            }
        }
    }

    // MARK: - QR Card

    private var qrCardContent: some View {
        VStack(spacing: Spacing.md) {
            Text(modelName)
                .font(.civitTitleMedium)
                .fontWeight(.semibold)
                .multilineTextAlignment(.center)
                .lineLimit(2)

            if let qrImage {
                Image(uiImage: qrImage)
                    .interpolation(.none)
                    .resizable()
                    .scaledToFit()
                    .frame(width: 240, height: 240)
                    .clipShape(RoundedRectangle(cornerRadius: Spacing.sm))
            }

            Text(civitaiUrl)
                .font(.civitBodySmall)
                .foregroundColor(.civitOnSurfaceVariant)
                .lineLimit(1)
        }
        .padding(Spacing.lg)
        .frame(maxWidth: .infinity)
        .background(Color.civitSurfaceVariant.opacity(0.5))
        .clipShape(RoundedRectangle(cornerRadius: CornerRadius.card))
    }

    // MARK: - Share

    private var shareButton: some View {
        Button {
            shareQRCode()
        } label: {
            Label("Share QR Code", systemImage: "square.and.arrow.up")
                .frame(maxWidth: .infinity)
        }
        .buttonStyle(.borderedProminent)
    }

    private func shareQRCode() {
        guard let qrImage else { return }
        let activityVC = UIActivityViewController(
            activityItems: [
                qrImage,
                "Check out this model on CivitAI: \(modelName)\n\(civitaiUrl)"
            ],
            applicationActivities: nil
        )
        guard let windowScene = UIApplication.shared.connectedScenes.first as? UIWindowScene,
              let rootVC = windowScene.windows.first?.rootViewController else { return }
        let presenter = rootVC.presentedViewController ?? rootVC
        activityVC.popoverPresentationController?.sourceView = presenter.view
        presenter.present(activityVC, animated: true)
    }
}
