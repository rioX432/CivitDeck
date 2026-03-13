import SwiftUI
import Shared

struct DuplicateReviewSheet: View {
    @StateObject private var viewModel: DuplicateReviewViewModel
    @Environment(\.dismiss) private var dismiss

    init(datasetId: Int64) {
        _viewModel = StateObject(wrappedValue: DuplicateReviewViewModel(datasetId: datasetId))
    }

    var body: some View {
        NavigationStack {
            Group {
                if viewModel.groups.isEmpty {
                    EmptyStateView(
                        icon: "checkmark.seal",
                        title: "No duplicates found",
                        subtitle: "All images in this dataset appear unique"
                    )
                } else {
                    groupList
                }
            }
            .navigationTitle("Review Duplicates")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarTrailing) {
                    Button("Done") { dismiss() }
                }
            }
        }
        .presentationDetents([.large])
    }

    private var groupList: some View {
        List(Array(viewModel.groups.enumerated()), id: \.offset) { _, group in
            DuplicateGroupRow(
                group: group,
                onKeep: viewModel.keepImage,
                onRemove: viewModel.removeImage
            )
        }
    }
}

private struct DuplicateGroupRow: View {
    let group: [DatasetImage]
    let onKeep: (Int64) -> Void
    let onRemove: (Int64) -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: Spacing.sm) {
            Text("Duplicate group (\(group.count) images)")
                .font(.caption)
                .foregroundColor(.secondary)
            ScrollView(.horizontal, showsIndicators: false) {
                HStack(spacing: Spacing.sm) {
                    ForEach(group, id: \.id) { image in
                        DuplicateImageCell(
                            image: image,
                            onKeep: { onKeep(image.id) },
                            onRemove: { onRemove(image.id) }
                        )
                    }
                }
            }
        }
        .listRowSeparator(.hidden)
    }
}

private let duplicateThumbnailSize: CGFloat = 120

private struct DuplicateImageCell: View {
    let image: DatasetImage
    let onKeep: () -> Void
    let onRemove: () -> Void

    var body: some View {
        VStack(spacing: Spacing.xs) {
            CachedAsyncImage(url: URL(string: image.imageUrl)) { phase in
                switch phase {
                case .success(let img):
                    img.resizable().scaledToFill()
                case .empty:
                    Color.civitSurfaceVariant.shimmer()
                default:
                    Color.civitSurfaceVariant
                }
            }
            .frame(width: duplicateThumbnailSize, height: duplicateThumbnailSize)
            .clipShape(RoundedRectangle(cornerRadius: CornerRadius.image))
            .overlay(alignment: .top) {
                if image.excluded {
                    Text("Excluded")
                        .font(.caption2.bold())
                        .padding(.horizontal, Spacing.xs)
                        .background(Color.civitError)
                        .foregroundColor(.white)
                        .clipShape(Capsule())
                        .padding(.top, Spacing.xs)
                }
            }
            HStack(spacing: Spacing.xs) {
                Button("Keep") { onKeep() }
                    .buttonStyle(.borderedProminent)
                    .tint(image.excluded ? .civitOutline : .civitPrimary)
                    .font(.caption)
                Button("Remove") { onRemove() }
                    .buttonStyle(.bordered)
                    .tint(.civitError)
                    .font(.caption)
            }
        }
        .frame(width: duplicateThumbnailSize)
    }
}
