import SwiftUI
import Shared

struct ModelDetailScreen: View {
    @StateObject private var viewModel: ModelDetailViewModel

    init(modelId: Int64) {
        _viewModel = StateObject(wrappedValue: ModelDetailViewModel(modelId: modelId))
    }

    @State private var isDescriptionExpanded = false
    @State private var selectedCarouselIndex: Int?
    @State private var showImageGrid = false
    @State private var gridSelectedIndex: Int?

    var body: some View {
        Group {
            if viewModel.isLoading && viewModel.model == nil {
                ProgressView()
                    .transition(.opacity)
            } else if let error = viewModel.error, viewModel.model == nil {
                errorView(message: error)
                    .transition(.opacity)
            } else if let model = viewModel.model {
                modelContent(model: model)
                    .transition(.opacity)
            }
        }
        .animation(MotionAnimation.standard, value: viewModel.isLoading)
        .animation(MotionAnimation.standard, value: viewModel.model != nil)
        .navigationTitle(viewModel.model?.name ?? "")
        .navigationBarTitleDisplayMode(.inline)
        .task {
            await viewModel.observeFavorite()
        }
        .task {
            await viewModel.observeNsfwFilter()
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
                        .foregroundColor(viewModel.isFavorite ? .civitError : .civitOnSurface)
                }
            }
        }
        .fullScreenCover(isPresented: Binding(
            get: { selectedCarouselIndex != nil },
            set: { if !$0 { selectedCarouselIndex = nil } }
        )) {
            CarouselViewer(
                images: filteredImages,
                selectedIndex: $selectedCarouselIndex
            )
        }
        .sheet(isPresented: $showImageGrid) {
            ImageGridSheet(
                images: filteredImages,
                onDismiss: { showImageGrid = false },
                onImageSelected: { gridSelectedIndex = $0 }
            )
        }
        .fullScreenCover(isPresented: Binding(
            get: { gridSelectedIndex != nil },
            set: { if !$0 { gridSelectedIndex = nil } }
        )) {
            GridImageViewer(
                images: filteredImages,
                selectedIndex: $gridSelectedIndex
            )
        }
    }

    // MARK: - Content

    private func modelContent(model: Model) -> some View {
        ScrollView {
            VStack(spacing: Spacing.lg) {
                imageCarousel(model: model)
                modelHeader(model: model)
                statsRow(model: model)
                if let version = viewModel.selectedVersion {
                    imageActionsRow(modelVersionId: version.id)
                }
                tagsSection(tags: model.tags)
                descriptionSection(description: model.description_)
                versionSelector(model: model)
                versionDetail
            }
        }
    }

    // MARK: - Image Carousel

    private var filteredImages: [ModelImage] {
        let allImages = viewModel.selectedVersion?.images ?? []
        if viewModel.nsfwFilterLevel == .off {
            return allImages.filter { !$0.nsfw }
        }
        return Array(allImages)
    }

    private func imageCarousel(model: Model) -> some View {
        let images = filteredImages
        return Group {
            if !images.isEmpty {
                TabView {
                    ForEach(Array(images.enumerated()), id: \.offset) { index, image in
                        if let url = URL(string: image.url) {
                            CachedAsyncImage(url: url) { phase in
                                switch phase {
                                case .success(let img):
                                    img
                                        .resizable()
                                        .scaledToFill()
                                        .transition(.opacity)
                                case .failure:
                                    imagePlaceholder
                                case .empty:
                                    Rectangle()
                                        .fill(Color.civitSurfaceVariant)
                                        .shimmer()
                                @unknown default:
                                    imagePlaceholder
                                }
                            }
                            .frame(maxWidth: .infinity)
                            .aspectRatio(1, contentMode: .fit)
                            .clipped()
                            .onTapGesture {
                                selectedCarouselIndex = index
                            }
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
            .fill(Color.civitSurfaceVariant)
            .overlay {
                Image(systemName: "photo")
                    .foregroundColor(.civitOnSurfaceVariant)
            }
    }

    // MARK: - Model Header

    private func modelHeader(model: Model) -> some View {
        VStack(alignment: .leading, spacing: Spacing.xs) {
            Text(model.name)
                .font(.civitHeadlineSmall)
                .fontWeight(.bold)

            HStack(spacing: Spacing.sm) {
                Text(model.type.name)
                    .font(.civitLabelMedium)
                    .padding(.horizontal, Spacing.sm)
                    .padding(.vertical, Spacing.xs)
                    .background(Color.civitSurfaceVariant)
                    .clipShape(Capsule())

                if let creator = model.creator {
                    NavigationLink(value: creator.username) {
                        Text("by \(creator.username)")
                            .font(.civitBodyMedium)
                            .foregroundColor(.civitPrimary)
                    }
                }
            }
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(.horizontal, Spacing.lg)
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
        .padding(.horizontal, Spacing.lg)
    }

    private func statColumn(value: String, label: String) -> some View {
        VStack(spacing: 2) {
            Text(value)
                .font(.civitTitleMedium)
            Text(label)
                .font(.civitLabelSmall)
                .foregroundColor(.civitOnSurfaceVariant)
        }
    }

    // MARK: - Image Actions Row

    private func imageActionsRow(modelVersionId: Int64) -> some View {
        HStack(spacing: Spacing.sm) {
            NavigationLink {
                ImageGalleryScreen(modelVersionId: modelVersionId)
            } label: {
                Text("View Community Images")
                    .frame(maxWidth: .infinity)
            }
            .buttonStyle(.bordered)

            if !filteredImages.isEmpty {
                Button {
                    showImageGrid = true
                } label: {
                    SwiftUI.Image(systemName: "square.grid.2x2")
                        .font(.title3)
                }
                .buttonStyle(.bordered)
            }
        }
        .padding(.horizontal, Spacing.lg)
    }

    // MARK: - Tags Section

    @ViewBuilder
    private func tagsSection(tags: [String]) -> some View {
        if !tags.isEmpty {
            VStack(alignment: .leading, spacing: Spacing.sm) {
                Text("Tags")
                    .font(.civitTitleSmall)

                WrappingHStack(tags: tags)
            }
            .frame(maxWidth: .infinity, alignment: .leading)
            .padding(.horizontal, Spacing.lg)
        }
    }

    // MARK: - Description Section

    @ViewBuilder
    private func descriptionSection(description: String?) -> some View {
        if let description, !description.isEmpty {
            VStack(alignment: .leading, spacing: Spacing.sm) {
                Divider()
                Text("Description")
                    .font(.civitTitleSmall)
                Text(htmlToPlainText(description))
                    .font(.civitBodyMedium)
                    .foregroundColor(.civitOnSurfaceVariant)
                    .lineLimit(isDescriptionExpanded ? nil : 4)
                    .animation(MotionAnimation.standard, value: isDescriptionExpanded)
                Button {
                    isDescriptionExpanded.toggle()
                } label: {
                    Text(isDescriptionExpanded ? "Show less" : "Show more")
                        .font(.civitLabelMedium)
                        .foregroundColor(.civitPrimary)
                }
            }
            .frame(maxWidth: .infinity, alignment: .leading)
            .padding(.horizontal, Spacing.lg)
        }
    }

    // MARK: - Version Selector

    @ViewBuilder
    private func versionSelector(model: Model) -> some View {
        let versions = model.modelVersions
        if versions.count > 1 {
            VStack(alignment: .leading, spacing: Spacing.sm) {
                Divider()
                    .padding(.horizontal, Spacing.lg)

                Text("Versions")
                    .font(.civitTitleSmall)
                    .padding(.horizontal, Spacing.lg)

                ScrollView(.horizontal, showsIndicators: false) {
                    HStack(spacing: Spacing.sm) {
                        ForEach(Array(versions.enumerated()), id: \.offset) { index, version in
                            let selected = index == viewModel.selectedVersionIndex
                            Button {
                                viewModel.onVersionSelected(index)
                            } label: {
                                Text(version.name)
                                    .font(.civitLabelMedium)
                                    .fontWeight(selected ? .semibold : .regular)
                                    .padding(.horizontal, Spacing.md)
                                    .padding(.vertical, 6)
                                    .background(
                                        selected
                                            ? Color.civitPrimary.opacity(0.2)
                                            : Color.civitSurfaceVariant
                                    )
                                    .foregroundColor(
                                        selected ? .civitPrimary : .civitOnSurface
                                    )
                                    .clipShape(Capsule())
                                    .animation(MotionAnimation.spring, value: selected)
                            }
                        }
                    }
                    .padding(.horizontal, Spacing.lg)
                }
            }
        }
    }

    // MARK: - Version Detail

    @ViewBuilder
    private var versionDetail: some View {
        if let version = viewModel.selectedVersion {
            VStack(alignment: .leading, spacing: Spacing.sm) {
                if let baseModel = version.baseModel {
                    HStack {
                        Text("Base Model")
                            .foregroundColor(.civitOnSurfaceVariant)
                        Spacer()
                        Text(baseModel)
                    }
                    .font(.civitBodyMedium)
                }

                let trainedWords = version.trainedWords
                if !trainedWords.isEmpty {
                    Text("Trained Words")
                        .font(.civitTitleSmall)
                    Text(trainedWords.joined(separator: ", "))
                        .font(.civitBodySmall)
                        .foregroundColor(.civitOnSurfaceVariant)
                }

                let files = version.files
                if !files.isEmpty {
                    Text("Files")
                        .font(.civitTitleSmall)
                        .padding(.top, Spacing.xs)

                    ForEach(Array(files.enumerated()), id: \.offset) { _, file in
                        fileInfoRow(file: file)
                    }
                }
            }
            .frame(maxWidth: .infinity, alignment: .leading)
            .padding(.horizontal, Spacing.lg)
            .padding(.bottom, Spacing.lg)
        }
    }

    private func fileInfoRow(file: ModelFile) -> some View {
        VStack(alignment: .leading, spacing: 2) {
            Text(file.name)
                .font(.civitBodySmall)
                .lineLimit(1)

            HStack(spacing: Spacing.sm) {
                Text(FormatUtils.shared.formatFileSize(sizeKB: file.sizeKB))
                    .font(.civitLabelSmall)
                    .foregroundColor(.civitOnSurfaceVariant)
                if let format = file.format {
                    Text(format)
                        .font(.civitLabelSmall)
                        .foregroundColor(.civitOnSurfaceVariant)
                }
                if let fp = file.fp {
                    Text(fp)
                        .font(.civitLabelSmall)
                        .foregroundColor(.civitOnSurfaceVariant)
                }
            }
        }
        .padding(.vertical, 2)
    }

    // MARK: - Error View

    private func errorView(message: String) -> some View {
        VStack(spacing: Spacing.lg) {
            Text(message)
                .foregroundColor(.civitError)
                .multilineTextAlignment(.center)
            Button("Retry") {
                viewModel.retry()
            }
            .buttonStyle(.bordered)
        }
        .padding()
    }
}
