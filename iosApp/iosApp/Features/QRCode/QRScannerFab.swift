import SwiftUI

struct QRScannerDestination: Hashable {}

struct QRScannerFab: View {
    var visible: Bool = true

    var body: some View {
        NavigationLink(value: QRScannerDestination()) {
            Image(systemName: "qrcode.viewfinder")
                .accessibilityLabel("QR scanner")
                .font(.body)
                .foregroundColor(.civitPrimary)
                .frame(width: 44, height: 44)
                .background(Color.civitTertiaryContainer)
                .clipShape(RoundedRectangle(cornerRadius: CornerRadius.card))
                .shadow(color: .black.opacity(0.1), radius: 4, y: 2)
        }
        .padding(.trailing, Spacing.lg)
        .opacity(visible ? 1 : 0)
        .animation(MotionAnimation.fast, value: visible)
    }
}
