import SwiftUI

struct TagFilterSection: View {
    let title: String?
    @Binding var input: String
    let placeholder: String
    let tags: [String]
    let chipColor: Color
    let onAdd: (String) -> Void
    let onRemove: (String) -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: Spacing.xs) {
            if let title {
                Text(title)
                    .font(.civitLabelMedium)
                    .foregroundColor(.civitOnSurfaceVariant)
                    .padding(.horizontal, Spacing.lg)
            }

            tagInputRow

            if !tags.isEmpty {
                tagChips
            }
        }
        .padding(.bottom, Spacing.sm)
    }

    private var tagInputRow: some View {
        HStack(spacing: Spacing.sm) {
            TextField(placeholder, text: $input)
                .font(.civitBodySmall)
                .submitLabel(.done)
                .onSubmit(submitTag)
                .padding(Spacing.sm)
                .overlay(
                    RoundedRectangle(cornerRadius: CornerRadius.searchBar)
                        .stroke(Color.civitOutlineVariant, lineWidth: 1)
                )
            Button(action: submitTag) {
                Image(systemName: "plus")
                    .accessibilityLabel("Add tag")
                    .font(.civitBodyMedium)
            }
        }
        .padding(.horizontal, Spacing.lg)
    }

    private var tagChips: some View {
        ScrollView(.horizontal, showsIndicators: false) {
            HStack(spacing: Spacing.xs) {
                ForEach(tags, id: \.self) { tag in
                    HStack(spacing: Spacing.xs) {
                        Text(tag)
                            .font(.civitLabelSmall)
                        Button {
                            onRemove(tag)
                        } label: {
                            Image(systemName: "xmark")
                                .accessibilityLabel("Remove tag")
                                .font(.civitLabelXSmall)
                        }
                    }
                    .padding(.horizontal, Spacing.sm)
                    .padding(.vertical, Spacing.xs)
                    .background(chipColor.opacity(0.15))
                    .foregroundColor(chipColor)
                    .clipShape(Capsule())
                }
            }
            .padding(.horizontal, Spacing.lg)
        }
    }

    private func submitTag() {
        guard !input.isEmpty else { return }
        onAdd(input)
        input = ""
    }
}
