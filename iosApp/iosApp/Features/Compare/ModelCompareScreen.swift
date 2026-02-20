import SwiftUI
import Shared

struct ModelCompareScreen: View {
    @StateObject private var viewModel: ModelCompareViewModel

    init(leftModelId: Int64, rightModelId: Int64) {
        _viewModel = StateObject(wrappedValue: ModelCompareViewModel(
            leftModelId: leftModelId,
            rightModelId: rightModelId
        ))
    }

    var body: some View {
        Group {
            if viewModel.isLoading && viewModel.leftModel == nil {
                ProgressView()
            } else if let error = viewModel.error,
                      viewModel.leftModel == nil || viewModel.rightModel == nil {
                errorView(message: error)
            } else if let leftModel = viewModel.leftModel,
                      let rightModel = viewModel.rightModel {
                compareContent(left: leftModel, right: rightModel)
            }
        }
        .navigationTitle("Compare")
        .navigationBarTitleDisplayMode(.inline)
        .task { await viewModel.observeNsfwFilter() }
    }

    // MARK: - Content

    private func compareContent(left: Model, right: Model) -> some View {
        ScrollView {
            VStack(spacing: 0) {
                comparePanels(left: left, right: right)
                compareVersionSelectors(left: left, right: right)
                Divider().padding(.vertical, Spacing.sm)
                specsTable(left: left, right: right)
            }
        }
    }

    // MARK: - Panels

    private func comparePanels(left: Model, right: Model) -> some View {
        HStack(alignment: .top, spacing: 0) {
            comparePanel(
                model: left,
                versionIndex: viewModel.leftSelectedVersionIndex
            )
            Divider()
            comparePanel(
                model: right,
                versionIndex: viewModel.rightSelectedVersionIndex
            )
        }
    }

    private func comparePanel(model: Model, versionIndex: Int) -> some View {
        let versions = model.modelVersions
        let version = versionIndex < versions.count ? versions[versionIndex] : nil
        let images = filteredImages(version: version)

        return VStack(alignment: .leading, spacing: Spacing.xs) {
            imagePager(images: images)
            Text(model.name)
                .font(.civitTitleSmall)
                .lineLimit(2)
                .padding(.horizontal, Spacing.xs)
            Text(model.type.name)
                .font(.civitLabelSmall)
                .foregroundColor(.civitOnSurfaceVariant)
                .padding(.horizontal, Spacing.xs)
        }
        .frame(maxWidth: .infinity)
    }

    // MARK: - Image Pager

    private func imagePager(images: [ModelImage]) -> some View {
        Group {
            if images.isEmpty {
                Rectangle()
                    .fill(Color.civitSurfaceVariant)
                    .aspectRatio(1, contentMode: .fit)
                    .overlay {
                        Image(systemName: "photo")
                            .foregroundColor(.civitOnSurfaceVariant)
                    }
            } else {
                TabView {
                    ForEach(Array(images.enumerated()), id: \.offset) { _, image in
                        if let url = URL(string: image.url) {
                            CachedAsyncImage(url: url) { phase in
                                switch phase {
                                case .success(let img):
                                    img.resizable().scaledToFill().transition(.opacity)
                                case .failure:
                                    imagePlaceholder
                                case .empty:
                                    Rectangle().fill(Color.civitSurfaceVariant).shimmer()
                                @unknown default:
                                    imagePlaceholder
                                }
                            }
                            .frame(maxWidth: .infinity)
                            .aspectRatio(1, contentMode: .fit)
                            .clipped()
                        }
                    }
                }
                .tabViewStyle(.page(indexDisplayMode: .automatic))
                .aspectRatio(1, contentMode: .fit)
            }
        }
    }

    private var imagePlaceholder: some View {
        Rectangle()
            .fill(Color.civitSurfaceVariant)
            .overlay {
                Image(systemName: "photo")
                    .foregroundColor(.civitOnSurfaceVariant)
            }
    }

    // MARK: - Version Selectors

    private func compareVersionSelectors(left: Model, right: Model) -> some View {
        HStack(alignment: .top, spacing: 0) {
            versionChips(
                versions: left.modelVersions,
                selectedIndex: viewModel.leftSelectedVersionIndex,
                onSelect: viewModel.onLeftVersionSelected
            )
            versionChips(
                versions: right.modelVersions,
                selectedIndex: viewModel.rightSelectedVersionIndex,
                onSelect: viewModel.onRightVersionSelected
            )
        }
    }

    private func versionChips(
        versions: [ModelVersion],
        selectedIndex: Int,
        onSelect: @escaping (Int) -> Void
    ) -> some View {
        Group {
            if versions.count > 1 {
                ScrollView(.horizontal, showsIndicators: false) {
                    HStack(spacing: Spacing.xs) {
                        ForEach(Array(versions.enumerated()), id: \.offset) { index, version in
                            Button {
                                onSelect(index)
                            } label: {
                                Text(version.name)
                                    .font(.civitLabelSmall)
                                    .fontWeight(index == selectedIndex ? .semibold : .regular)
                                    .padding(.horizontal, Spacing.sm)
                                    .padding(.vertical, 4)
                                    .background(
                                        index == selectedIndex
                                            ? Color.civitPrimary.opacity(0.2)
                                            : Color.civitSurfaceVariant
                                    )
                                    .foregroundColor(
                                        index == selectedIndex ? .civitPrimary : .civitOnSurface
                                    )
                                    .clipShape(Capsule())
                            }
                        }
                    }
                    .padding(.horizontal, Spacing.xs)
                    .padding(.vertical, Spacing.xs)
                }
                .frame(maxWidth: .infinity)
            }
        }
    }

    // MARK: - Specs Table

    private func specsTable(left: Model, right: Model) -> some View {
        VStack(alignment: .leading, spacing: Spacing.xs) {
            Text("Comparison")
                .font(.civitTitleSmall)
                .padding(.horizontal, Spacing.md)
                .padding(.bottom, Spacing.xs)

            specRow(label: "Type", leftValue: left.type.name, rightValue: right.type.name)
            specRow(
                label: "Base Model",
                leftValue: left.modelVersions.first?.baseModel ?? "-",
                rightValue: right.modelVersions.first?.baseModel ?? "-"
            )
            specRow(
                label: "Downloads",
                leftValue: FormatUtils.shared.formatCount(count: left.stats.downloadCount),
                rightValue: FormatUtils.shared.formatCount(count: right.stats.downloadCount)
            )
            specRow(
                label: "Favorites",
                leftValue: FormatUtils.shared.formatCount(count: left.stats.favoriteCount),
                rightValue: FormatUtils.shared.formatCount(count: right.stats.favoriteCount)
            )
            specRow(
                label: "Rating",
                leftValue: FormatUtils.shared.formatRating(rating: left.stats.rating),
                rightValue: FormatUtils.shared.formatRating(rating: right.stats.rating)
            )
            specRow(
                label: "File Size",
                leftValue: primaryFileSize(model: left),
                rightValue: primaryFileSize(model: right)
            )
        }
        .padding(.bottom, Spacing.lg)
    }

    private func specRow(label: String, leftValue: String, rightValue: String) -> some View {
        HStack {
            Text(label)
                .font(.civitBodySmall)
                .foregroundColor(.civitOnSurfaceVariant)
                .frame(maxWidth: .infinity, alignment: .leading)
            Text(leftValue)
                .font(.civitBodySmall)
                .frame(maxWidth: .infinity, alignment: .leading)
                .lineLimit(1)
            Text(rightValue)
                .font(.civitBodySmall)
                .frame(maxWidth: .infinity, alignment: .leading)
                .lineLimit(1)
        }
        .padding(.horizontal, Spacing.md)
    }

    // MARK: - Helpers

    private func filteredImages(version: ModelVersion?) -> [ModelImage] {
        guard let version else { return [] }
        return version.images.filter { $0.isAllowedByFilter(viewModel.nsfwFilterLevel) }
    }

    private func primaryFileSize(model: Model) -> String {
        guard let version = model.modelVersions.first else { return "-" }
        let file = version.files.first { $0.primary } ?? version.files.first
        guard let file else { return "-" }
        return FormatUtils.shared.formatFileSize(sizeKB: file.sizeKB)
    }

    private func errorView(message: String) -> some View {
        VStack(spacing: Spacing.lg) {
            Text(message)
                .foregroundColor(.civitError)
                .multilineTextAlignment(.center)
        }
        .padding()
    }
}

// MARK: - Comparison Bottom Bar

struct ComparisonBottomBar: View {
    let modelName: String
    let onCancel: () -> Void

    var body: some View {
        HStack(spacing: Spacing.sm) {
            Image(systemName: "rectangle.split.2x1")
                .foregroundColor(.civitPrimary)

            VStack(alignment: .leading, spacing: 2) {
                Text("Comparing: \(modelName)")
                    .font(.civitLabelMedium)
                    .fontWeight(.medium)
                    .lineLimit(1)
                Text("Tap another model to compare")
                    .font(.civitLabelSmall)
                    .foregroundColor(.civitOnSurfaceVariant)
            }

            Spacer()

            Button("Cancel") { onCancel() }
                .font(.civitLabelMedium)
                .foregroundColor(.civitPrimary)
        }
        .padding(.horizontal, Spacing.lg)
        .padding(.vertical, Spacing.sm)
        .background(Color.civitSurfaceContainerHigh)
        .clipShape(RoundedRectangle(cornerRadius: CornerRadius.card))
        .shadow(color: .black.opacity(0.15), radius: 6, y: -2)
        .padding(.horizontal, Spacing.md)
        .padding(.bottom, Spacing.sm)
    }
}
