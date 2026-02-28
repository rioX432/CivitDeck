import SwiftUI

struct DetailRow: View {
    let label: String
    let value: String
    var valueTruncation: Text.TruncationMode = .middle

    var body: some View {
        HStack {
            Text(label)
                .font(.civitBodyMedium)
                .foregroundColor(.civitOnSurfaceVariant)
            Spacer()
            Text(value)
                .font(.civitBodyMedium)
                .truncationMode(valueTruncation)
                .lineLimit(1)
        }
    }
}
