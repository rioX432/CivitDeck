import SwiftUI
import Shared

struct ModelDetailScreen: View {
    @StateObject private var viewModel: ModelDetailViewModel

    init(modelId: Int64) {
        _viewModel = StateObject(wrappedValue: ModelDetailViewModel(modelId: modelId))
    }

    var body: some View {
        Group {
            if viewModel.isLoading && viewModel.model == nil {
                ProgressView()
            } else if let error = viewModel.error, viewModel.model == nil {
                errorView(message: error)
            } else if let model = viewModel.model {
                modelContent(model: model)
            }
        }
        .navigationTitle(viewModel.model?.name ?? "")
        .navigationBarTitleDisplayMode(.inline)
        .task {
            await viewModel.observeFavorite()
        }
        .toolbar {
            ToolbarItem(placement: .navigationBarTrailing) {
                if let model = viewModel.model,
                   let url = URL(string: "https://civitai.com/models/\(model.id)") {
                    ShareLink(item: url) {
                        Image(systemName: "square.and.arrow.up")
                    }
                }
            }
            ToolbarItem(placement: .navigationBarTrailing) {
                Button {
                    viewModel.onFavoriteToggle()
                } label: {
                    Image(systemName: viewModel.isFavorite ? "heart.fill" : "heart")
                        .foregroundColor(viewModel.isFavorite ? .red : .primary)
                }
            }
        }
    }

    // MARK: - Content

    private func modelContent(model: Model) -> some View {
        ScrollView {
            VStack(spacing: 16) {
                imageCarousel(model: model)
                modelHeader(model: model)
                statsRow(model: model)
                viewImagesButton
                tagsSection(tags: model.tags)
                descriptionSection(description: model.description_)
                versionSelector(model: model)
                versionDetail
            }
        }
    }

    // MARK: - Image Carousel

    private func imageCarousel(model: Model) -> some View {
        let images = viewModel.selectedVersion?.images ?? []
        return Group {
            if !images.isEmpty {
                TabView {
                    ForEach(Array(images.enumerated()), id: \.offset) { _, image in
                        if let url = URL(string: image.url) {
                            AsyncImage(url: url) { phase in
                                switch phase {
                                case .success(let img):
                                    img
                                        .resizable()
                                        .scaledToFill()
                                case .failure:
                                    imagePlaceholder
                                case .empty:
                                    ProgressView()
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
                .frame(height: UIScreen.main.bounds.width)
            }
        }
    }

    private var imagePlaceholder: some View {
        Rectangle()
            .fill(Color(.systemGray5))
            .overlay {
                Image(systemName: "photo")
                    .foregroundColor(.secondary)
            }
    }

    // MARK: - Model Header

    private func modelHeader(model: Model) -> some View {
        VStack(alignment: .leading, spacing: 4) {
            Text(model.name)
                .font(.title2)
                .fontWeight(.bold)

            HStack(spacing: 8) {
                Text(model.type.name)
                    .font(.caption)
                    .padding(.horizontal, 8)
                    .padding(.vertical, 4)
                    .background(Color(.systemGray5))
                    .clipShape(Capsule())

                if let creator = model.creator {
                    Text("by \(creator.username)")
                        .font(.subheadline)
                        .foregroundColor(.secondary)
                }
            }
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(.horizontal, 16)
    }

    // MARK: - Stats Row

    private func statsRow(model: Model) -> some View {
        HStack {
            statColumn(
                value: FormatUtils.shared.formatCount(count: model.stats.downloadCount),
                label: "Downloads"
            )
            Spacer()
            statColumn(
                value: FormatUtils.shared.formatCount(count: model.stats.favoriteCount),
                label: "Favorites"
            )
            Spacer()
            statColumn(
                value: FormatUtils.shared.formatRating(rating: model.stats.rating),
                label: "Rating"
            )
            Spacer()
            statColumn(
                value: FormatUtils.shared.formatCount(count: model.stats.commentCount),
                label: "Comments"
            )
        }
        .padding(.horizontal, 16)
    }

    private func statColumn(value: String, label: String) -> some View {
        VStack(spacing: 2) {
            Text(value)
                .font(.headline)
            Text(label)
                .font(.caption)
                .foregroundColor(.secondary)
        }
    }

    // MARK: - View Images Button

    private var viewImagesButton: some View {
        Button {
            // Will navigate to image gallery in future issue
        } label: {
            Text("View Community Images")
                .frame(maxWidth: .infinity)
        }
        .buttonStyle(.bordered)
        .padding(.horizontal, 16)
    }

    // MARK: - Tags Section

    @ViewBuilder
    private func tagsSection(tags: [String]) -> some View {
        if !tags.isEmpty {
            VStack(alignment: .leading, spacing: 8) {
                Text("Tags")
                    .font(.subheadline)
                    .fontWeight(.semibold)

                WrappingHStack(tags: tags)
            }
            .frame(maxWidth: .infinity, alignment: .leading)
            .padding(.horizontal, 16)
        }
    }

    // MARK: - Description Section

    @ViewBuilder
    private func descriptionSection(description: String?) -> some View {
        if let description, !description.isEmpty {
            VStack(alignment: .leading, spacing: 8) {
                Divider()
                Text("Description")
                    .font(.subheadline)
                    .fontWeight(.semibold)
                Text(htmlToPlainText(description))
                    .font(.body)
                    .foregroundColor(.secondary)
            }
            .frame(maxWidth: .infinity, alignment: .leading)
            .padding(.horizontal, 16)
        }
    }

    // MARK: - Version Selector

    @ViewBuilder
    private func versionSelector(model: Model) -> some View {
        let versions = model.modelVersions
        if versions.count > 1 {
            VStack(alignment: .leading, spacing: 8) {
                Divider()
                    .padding(.horizontal, 16)

                Text("Versions")
                    .font(.subheadline)
                    .fontWeight(.semibold)
                    .padding(.horizontal, 16)

                ScrollView(.horizontal, showsIndicators: false) {
                    HStack(spacing: 8) {
                        ForEach(Array(versions.enumerated()), id: \.offset) { index, version in
                            Button {
                                viewModel.onVersionSelected(index)
                            } label: {
                                Text(version.name)
                                    .font(.caption)
                                    .fontWeight(index == viewModel.selectedVersionIndex ? .semibold : .regular)
                                    .padding(.horizontal, 12)
                                    .padding(.vertical, 6)
                                    .background(
                                        index == viewModel.selectedVersionIndex
                                            ? Color.accentColor.opacity(0.2)
                                            : Color(.systemGray5)
                                    )
                                    .foregroundColor(
                                        index == viewModel.selectedVersionIndex
                                            ? .accentColor : .primary
                                    )
                                    .clipShape(Capsule())
                            }
                        }
                    }
                    .padding(.horizontal, 16)
                }
            }
        }
    }

    // MARK: - Version Detail

    @ViewBuilder
    private var versionDetail: some View {
        if let version = viewModel.selectedVersion {
            VStack(alignment: .leading, spacing: 8) {
                if let baseModel = version.baseModel {
                    HStack {
                        Text("Base Model")
                            .foregroundColor(.secondary)
                        Spacer()
                        Text(baseModel)
                    }
                    .font(.subheadline)
                }

                let trainedWords = version.trainedWords
                if !trainedWords.isEmpty {
                    Text("Trained Words")
                        .font(.subheadline)
                        .fontWeight(.semibold)
                    Text(trainedWords.joined(separator: ", "))
                        .font(.caption)
                        .foregroundColor(.secondary)
                }

                let files = version.files
                if !files.isEmpty {
                    Text("Files")
                        .font(.subheadline)
                        .fontWeight(.semibold)
                        .padding(.top, 4)

                    ForEach(Array(files.enumerated()), id: \.offset) { _, file in
                        fileInfoRow(file: file)
                    }
                }
            }
            .frame(maxWidth: .infinity, alignment: .leading)
            .padding(.horizontal, 16)
            .padding(.bottom, 16)
        }
    }

    private func fileInfoRow(file: ModelFile) -> some View {
        VStack(alignment: .leading, spacing: 2) {
            Text(file.name)
                .font(.caption)
                .lineLimit(1)

            HStack(spacing: 8) {
                Text(FormatUtils.shared.formatFileSize(sizeKB: file.sizeKB))
                    .font(.caption2)
                    .foregroundColor(.secondary)
                if let format = file.format {
                    Text(format)
                        .font(.caption2)
                        .foregroundColor(.secondary)
                }
                if let fp = file.fp {
                    Text(fp)
                        .font(.caption2)
                        .foregroundColor(.secondary)
                }
            }
        }
        .padding(.vertical, 2)
    }

    // MARK: - Error View

    private func errorView(message: String) -> some View {
        VStack(spacing: 16) {
            Text(message)
                .foregroundColor(.red)
                .multilineTextAlignment(.center)
            Button("Retry") {
                viewModel.retry()
            }
            .buttonStyle(.bordered)
        }
        .padding()
    }

    // MARK: - Helpers

    private func htmlToPlainText(_ html: String) -> String {
        guard let data = html.data(using: .utf8),
              let attributedString = try? NSAttributedString(
                data: data,
                options: [
                    .documentType: NSAttributedString.DocumentType.html,
                    .characterEncoding: String.Encoding.utf8.rawValue,
                ],
                documentAttributes: nil
              ) else {
            return html
        }
        return attributedString.string
    }
}

// MARK: - Wrapping HStack for Tags

private struct WrappingHStack: View {
    let tags: [String]

    var body: some View {
        LazyVGrid(
            columns: [GridItem(.adaptive(minimum: 80), spacing: 8)],
            alignment: .leading,
            spacing: 8
        ) {
            ForEach(tags, id: \.self) { tag in
                Text(tag)
                    .font(.caption)
                    .padding(.horizontal, 10)
                    .padding(.vertical, 4)
                    .background(Color(.systemGray5))
                    .clipShape(Capsule())
            }
        }
    }
}
