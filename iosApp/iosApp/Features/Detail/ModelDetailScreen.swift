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
    @State private var showCollectionSheet = false
    @State private var showComfyUIGeneration = false

    var body: some View {
        Group {
            if viewModel.isLoading && viewModel.model == nil {
                LoadingStateView()
                    .transition(.opacity)
            } else if let error = viewModel.error, viewModel.model == nil {
                ErrorStateView(message: error) {
                    viewModel.retry()
                }
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
        .task {
            await viewModel.observePowerUserMode()
        }
        .toolbar {
            ToolbarItem(placement: .navigationBarTrailing) {
                Button {
                    showCollectionSheet = true
                } label: {
                    Image(systemName: "folder.badge.plus")
                }
            }
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
                    HapticFeedback.impact.trigger()
                    viewModel.onFavoriteToggle()
                } label: {
                    Image(systemName: viewModel.isFavorite ? "heart.fill" : "heart")
                        .foregroundColor(viewModel.isFavorite ? .civitError : .civitOnSurface)
                }
            }
        }
        .task {
            await viewModel.observeCollections()
        }
        .task {
            await viewModel.observeModelCollections()
        }
        .sheet(isPresented: $showCollectionSheet) {
            AddToCollectionSheet(
                collections: viewModel.collections,
                modelCollectionIds: viewModel.modelCollectionIds,
                onToggleCollection: { viewModel.toggleCollection($0) },
                onCreateCollection: { viewModel.createCollectionAndAdd(name: $0) }
            )
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
        .sheet(isPresented: $showComfyUIGeneration) {
            NavigationView {
                ComfyUIGenerationView()
            }
        }
    }

    // MARK: - Content

    private func modelContent(model: Model) -> some View {
        ScrollView {
            VStack(spacing: Spacing.lg) {
                imageCarousel(model: model)
                modelHeader(model: model)
                ModelStatsRow(
                    downloadCount: model.stats.downloadCount,
                    favoriteCount: model.stats.favoriteCount,
                    rating: model.stats.rating,
                    commentCount: model.stats.commentCount
                )
                .padding(.horizontal, Spacing.lg)
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
        return allImages.filter { $0.isAllowedByFilter(viewModel.nsfwFilterLevel) }
    }

    private func imageCarousel(model: Model) -> some View {
        let images = filteredImages
        return Group {
            if !images.isEmpty {
                TabView {
                    ForEach(Array(images.enumerated()), id: \.offset) { index, image in
                        CivitAsyncImageView(imageUrl: image.url, aspectRatio: 1)
                            .onTapGesture {
                                selectedCarouselIndex = index
                            }
                    }
                }
                .tabViewStyle(.page(indexDisplayMode: .automatic))
                .frame(height: min(UIScreen.main.bounds.width, 600))
            }
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

    // MARK: - Image Actions Row

    private func imageActionsRow(modelVersionId: Int64) -> some View {
        VStack(spacing: Spacing.sm) {
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
            if viewModel.powerUserMode {
                Button {
                    showComfyUIGeneration = true
                } label: {
                    Text("Try in ComfyUI")
                        .frame(maxWidth: .infinity)
                }
                .buttonStyle(.bordered)
                .tint(.green)
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
                                HapticFeedback.selection.trigger()
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
            VersionDetailSection(
                version: version,
                powerUserMode: viewModel.powerUserMode
            )
        }
    }
}
