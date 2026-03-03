import SwiftUI
import Shared

struct ImageDetailSheet: View {
    let image: DatasetImage
    let onTrainableToggle: (Bool) -> Void
    @Environment(\.dismiss) private var dismiss

    var body: some View {
        NavigationStack {
            List {
                sourceBadgeSection
                trainableSection
                if let note = image.licenseNote, !note.isEmpty {
                    licenseSection(note: note)
                }
            }
            .navigationTitle("Image Info")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarTrailing) {
                    Button("Done") { dismiss() }
                }
            }
        }
        .presentationDetents([.medium])
    }

    private var sourceBadgeSection: some View {
        Section("Source") {
            HStack {
                Text(sourceLabel)
                    .font(.caption.bold())
                    .padding(.horizontal, Spacing.md)
                    .padding(.vertical, Spacing.xs)
                    .background(sourceColor.opacity(0.15))
                    .foregroundColor(sourceColor)
                    .clipShape(Capsule())
                Spacer()
            }
        }
    }

    private var trainableSection: some View {
        Section {
            Toggle("Include in training", isOn: Binding(
                get: { image.trainable },
                set: { onTrainableToggle($0) }
            ))
        } header: { Text("Training") }
    }

    private func licenseSection(note: String) -> some View {
        Section("License") {
            Text(note)
                .font(.caption)
                .foregroundColor(.secondary)
        }
    }

    private var sourceLabel: String {
        switch image.sourceType {
        case .civitai: return "CivitAI"
        case .local: return "Local"
        case .generated: return "Generated"
        default: return "Unknown"
        }
    }

    private var sourceColor: Color {
        switch image.sourceType {
        case .civitai: return .civitPrimary
        case .local: return .civitSecondary
        case .generated: return .civitTertiary
        default: return .civitOnSurfaceVariant
        }
    }
}
