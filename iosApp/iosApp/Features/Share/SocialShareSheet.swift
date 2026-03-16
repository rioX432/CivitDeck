import SwiftUI
import Shared

struct SocialShareSheet: View {
    let hashtags: [ShareHashtag]
    let onToggle: (String, Bool) -> Void
    let onAdd: (String) -> Void
    let onRemove: (String) -> Void

    @Environment(\.dismiss) private var dismiss
    @Environment(\.civitTheme) private var theme
    @State private var caption = ""
    @State private var newTagInput = ""

    private let charLimit = 280

    private var enabledTags: [String] {
        hashtags.filter { $0.isEnabled }.map { $0.tag }
    }

    private var hashtagText: String {
        enabledTags.joined(separator: " ")
    }

    private var fullText: String {
        [caption.trimmingCharacters(in: .whitespacesAndNewlines), hashtagText]
            .filter { !$0.isEmpty }
            .joined(separator: "\n\n")
    }

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(alignment: .leading, spacing: Spacing.md) {
                    captionSection
                    hashtagSection
                    addTagSection
                    Spacer(minLength: Spacing.lg)
                    actionButtons
                }
                .padding(Spacing.lg)
            }
            .navigationTitle("Share")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Cancel") { dismiss() }
                }
            }
        }
    }

    // MARK: - Caption

    private var captionSection: some View {
        VStack(alignment: .leading, spacing: Spacing.xs) {
            Text("Caption")
                .font(.civitLabelMedium)
                .foregroundColor(.civitOnSurfaceVariant)
            TextEditor(text: $caption)
                .frame(minHeight: 80)
                .padding(Spacing.xs)
                .overlay(
                    RoundedRectangle(cornerRadius: CornerRadius.chip)
                        .stroke(Color.civitOutline, lineWidth: 1)
                )
            charCounter
        }
    }

    private var charCounter: some View {
        let count = fullText.count
        let color: Color = {
            if count > charLimit { return .red }
            if count > Int(Double(charLimit) * 0.9) { return .orange }
            return .civitOnSurfaceVariant
        }()
        return Text("\(count) / \(charLimit)")
            .font(.civitBodySmall)
            .foregroundColor(color)
    }

    // MARK: - Hashtags

    private var hashtagSection: some View {
        VStack(alignment: .leading, spacing: Spacing.xs) {
            Text("Hashtags")
                .font(.civitLabelMedium)
                .foregroundColor(.civitOnSurfaceVariant)
            FlowLayout(spacing: Spacing.sm) {
                ForEach(hashtags, id: \.tag) { hashtag in
                    hashtagChip(hashtag)
                }
            }
        }
    }

    private func hashtagChip(_ hashtag: ShareHashtag) -> some View {
        Button {
            HapticFeedback.selection.trigger()
            onToggle(hashtag.tag, !hashtag.isEnabled)
        } label: {
            HStack(spacing: Spacing.xs) {
                Text(hashtag.tag)
                    .font(.civitLabelMedium)
                if hashtag.isCustom {
                    Button {
                        onRemove(hashtag.tag)
                    } label: {
                        Image(systemName: "xmark")
                            .font(.system(size: 10, weight: .bold))
                    }
                }
            }
            .padding(.horizontal, Spacing.md)
            .padding(.vertical, Spacing.xsPlus)
            .background(hashtag.isEnabled ? theme.primary.opacity(0.2) : Color.civitSurfaceVariant)
            .foregroundColor(hashtag.isEnabled ? theme.primary : .civitOnSurface)
            .clipShape(Capsule())
            .animation(MotionAnimation.spring, value: hashtag.isEnabled)
        }
    }

    // MARK: - Add Tag

    private var addTagSection: some View {
        HStack(spacing: Spacing.sm) {
            TextField("Add tag", text: $newTagInput)
                .textFieldStyle(.roundedBorder)
                .onSubmit { addTag() }
            Button { addTag() } label: {
                Image(systemName: "plus")
                    .fontWeight(.semibold)
            }
            .disabled(newTagInput.trimmingCharacters(in: .whitespaces).isEmpty)
        }
    }

    private func addTag() {
        let trimmed = newTagInput.trimmingCharacters(in: .whitespaces)
        guard !trimmed.isEmpty else { return }
        onAdd(trimmed)
        newTagInput = ""
    }

    // MARK: - Actions

    private var actionButtons: some View {
        HStack(spacing: Spacing.sm) {
            Button {
                UIPasteboard.general.string = fullText
                HapticFeedback.success.trigger()
                dismiss()
            } label: {
                Label("Copy", systemImage: "doc.on.doc")
                    .frame(maxWidth: .infinity)
            }
            .buttonStyle(.bordered)

            Button {
                presentShareSheet()
            } label: {
                Label("Share", systemImage: "square.and.arrow.up")
                    .frame(maxWidth: .infinity)
            }
            .buttonStyle(.borderedProminent)
        }
    }

    private func presentShareSheet() {
        let items: [Any] = [fullText]
        let activityVC = UIActivityViewController(
            activityItems: items,
            applicationActivities: nil
        )
        guard let windowScene = UIApplication.shared.connectedScenes.first as? UIWindowScene,
              let rootVC = windowScene.windows.first?.rootViewController else { return }
        var presenter = rootVC
        while let presented = presenter.presentedViewController {
            presenter = presented
        }
        activityVC.popoverPresentationController?.sourceView = presenter.view
        presenter.present(activityVC, animated: true)
    }
}

// MARK: - Flow Layout

private struct FlowLayout: Layout {
    var spacing: CGFloat

    func sizeThatFits(
        proposal: ProposedViewSize,
        subviews: Subviews,
        cache: inout ()
    ) -> CGSize {
        let result = computeLayout(proposal: proposal, subviews: subviews)
        return result.size
    }

    func placeSubviews(
        in bounds: CGRect,
        proposal: ProposedViewSize,
        subviews: Subviews,
        cache: inout ()
    ) {
        let result = computeLayout(proposal: proposal, subviews: subviews)
        for (index, position) in result.positions.enumerated() {
            subviews[index].place(
                at: CGPoint(x: bounds.minX + position.x, y: bounds.minY + position.y),
                proposal: .unspecified
            )
        }
    }

    private func computeLayout(
        proposal: ProposedViewSize,
        subviews: Subviews
    ) -> (size: CGSize, positions: [CGPoint]) {
        let maxWidth = proposal.width ?? .infinity
        var positions: [CGPoint] = []
        var x: CGFloat = 0
        var y: CGFloat = 0
        var rowHeight: CGFloat = 0
        var totalHeight: CGFloat = 0

        for subview in subviews {
            let size = subview.sizeThatFits(.unspecified)
            if x + size.width > maxWidth, x > 0 {
                x = 0
                y += rowHeight + spacing
                rowHeight = 0
            }
            positions.append(CGPoint(x: x, y: y))
            rowHeight = max(rowHeight, size.height)
            x += size.width + spacing
        }
        totalHeight = y + rowHeight

        return (CGSize(width: maxWidth, height: totalHeight), positions)
    }
}
