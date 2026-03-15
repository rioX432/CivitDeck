import SwiftUI
import Shared

struct ModelDetailScreen: View {
    @StateObject private var viewModel: ModelDetailViewModel

    init(modelId: Int64) {
        _viewModel = StateObject(wrappedValue: ModelDetailViewModel(modelId: modelId))
    }

    @State private var isDescriptionExpanded = false
    @State private var selectedCarouselIndex: Int?
    @State private var currentCarouselPage: Int = 0
    @State private var showImageGrid = false
    @State private var gridSelectedIndex: Int?
    @State private var showCollectionSheet = false
    @State private var showComfyUIGeneration = false
    @State private var showLinkSheet = false
    @State private var showQRCodeSheet = false
    @State private var showSubmitReviewSheet = false

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
            await withTaskGroup(of: Void.self) { group in
                group.addTask { await viewModel.observeFavorite() }
                group.addTask { await viewModel.observeNsfwFilter() }
                group.addTask { await viewModel.observePowerUserMode() }
                group.addTask { await viewModel.observeNote() }
                group.addTask { await viewModel.observePersonalTags() }
                group.addTask { await viewModel.observeDownloads() }
            }
        }
        .task { viewModel.loadReviews() }
        .toolbar {
            ToolbarItem(placement: .navigationBarTrailing) {
                Button {
                    showCollectionSheet = true
                } label: {
                    Image(systemName: "folder.badge.plus")
                        .accessibilityLabel("Add to collection")
                }
            }
            ToolbarItem(placement: .navigationBarTrailing) {
                Button {
                    showQRCodeSheet = true
                } label: {
                    Image(systemName: "qrcode")
                        .accessibilityLabel("QR code")
                }
            }
            ToolbarItem(placement: .navigationBarTrailing) {
                if let model = viewModel.model,
                   let url = URL(string: "https://civitai.com/models/\(model.id)") {
                    ShareLink(item: url) {
                        Image(systemName: "square.and.arrow.up")
                            .accessibilityLabel("Share")
                    }
                }
            }
            ToolbarItem(placement: .navigationBarTrailing) {
                Button {
                    HapticFeedback.impact.trigger()
                    viewModel.onFavoriteToggle()
                } label: {
                    Image(systemName: viewModel.isFavorite ? "heart.fill" : "heart")
                        .accessibilityLabel(viewModel.isFavorite ? "Remove from favorites" : "Add to favorites")
                        .foregroundColor(viewModel.isFavorite ? .civitError : .civitOnSurface)
                }
            }
        }
        .task {
            await withTaskGroup(of: Void.self) { group in
                group.addTask { await viewModel.observeCollections() }
                group.addTask { await viewModel.observeModelCollections() }
            }
        }
        .sheet(isPresented: $showCollectionSheet) {
            AddToCollectionSheet(
                collections: viewModel.collections,
                modelCollectionIds: viewModel.modelCollectionIds,
                onToggleCollection: { viewModel.toggleCollection($0) },
                onCreateCollection: { viewModel.createCollectionAndAdd(name: $0) }
            )
        }
        .overlay {
            CarouselViewer(
                images: filteredImages,
                selectedIndex: $selectedCarouselIndex
            )
            .ignoresSafeArea()
        }
        .sheet(isPresented: $showImageGrid) {
            ImageGridSheet(
                images: filteredImages,
                onDismiss: { showImageGrid = false },
                onImageSelected: { gridSelectedIndex = $0 }
            )
        }
        .overlay {
            GridImageViewer(
                images: filteredImages,
                selectedIndex: $gridSelectedIndex
            )
            .ignoresSafeArea()
        }
        .sheet(isPresented: $showComfyUIGeneration) {
            NavigationView {
                ComfyUIGenerationView()
            }
        }
        .sheet(isPresented: $showLinkSheet) {
            if let model = viewModel.model {
                CivitaiLinkSendSheet(model: model)
            }
        }
        .sheet(isPresented: $showQRCodeSheet) {
            if let model = viewModel.model {
                QRCodeSheet(
                    modelId: model.id,
                    modelName: model.name
                )
            }
        }
        .sheet(isPresented: $showSubmitReviewSheet) { submitReviewSheet }
        .onChange(of: viewModel.reviewSubmitSuccess) { success in
            if success { showSubmitReviewSheet = false; viewModel.dismissReviewSuccess() }
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
                ModelNotesSection(
                    note: viewModel.note,
                    onSave: { viewModel.saveNote($0) }
                )
                PersonalTagsSection(
                    tags: viewModel.personalTags,
                    onAdd: { viewModel.addTag($0) },
                    onRemove: { viewModel.removeTag($0) }
                )
                ReviewsSection(
                    reviews: viewModel.reviews,
                    ratingTotals: viewModel.ratingTotals,
                    sortOrder: viewModel.reviewSortOrder,
                    isLoading: viewModel.isReviewsLoading,
                    onSortChanged: { viewModel.onReviewSortChanged($0) },
                    onWriteReview: { showSubmitReviewSheet = true }
                )
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
                TabView(selection: $currentCarouselPage) {
                    ForEach(Array(images.enumerated()), id: \.offset) { index, image in
                        CivitAsyncImageView(imageUrl: image.url, aspectRatio: 1)
                            .contentShape(Rectangle())
                            .onTapGesture {
                                selectedCarouselIndex = index
                            }
                            .accessibilityLabel("Image \(index + 1) of \(images.count)")
                            .accessibilityAddTraits(.isButton)
                            .tag(index)
                    }
                }
                .tabViewStyle(.page(indexDisplayMode: .never))
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
                        VStack(spacing: Spacing.xxs) {
                            SwiftUI.Image(systemName: "square.grid.2x2")
                                .font(.title3)
                            Text("\(currentCarouselPage + 1)/\(filteredImages.count)")
                                .font(.civitLabelSmall)
                        }
                    }
                    .buttonStyle(.bordered)
                }
            }
            Button {
                showLinkSheet = true
            } label: {
                Text("Send to PC")
                    .frame(maxWidth: .infinity)
            }
            .buttonStyle(.bordered)
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
                                    .padding(.vertical, Spacing.xsPlus)
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
                powerUserMode: viewModel.powerUserMode,
                downloads: Dictionary(
                    uniqueKeysWithValues: viewModel.downloads.map { ($0.fileId, $0) }
                ),
                onDownload: { viewModel.downloadFile($0) },
                onCancelDownload: { viewModel.cancelDownload($0) }
            )
        }
    }

    private var submitReviewSheet: some View {
        SubmitReviewSheet(
            isSubmitting: viewModel.isSubmittingReview,
            onSubmit: { rating, recommended, details in
                guard let version = viewModel.selectedVersion else { return }
                viewModel.submitReview(
                    modelVersionId: version.id, rating: rating,
                    recommended: recommended, details: details
                )
            },
            onDismiss: { showSubmitReviewSheet = false }
        )
    }
}

// MARK: - Civitai Link Send

@MainActor
private class CivitaiLinkSendViewModel: ObservableObject {
    @Published var status: CivitaiLinkStatus = .disconnected
    private let observeStatus = KoinHelper.shared.getObserveCivitaiLinkStatusUseCase()
    private let sendResource = KoinHelper.shared.getSendResourceToPCUseCase()

    var isConnected: Bool { status == .connected }

    func observeLinkStatus() async {
        for await s in observeStatus.invoke() { self.status = s }
    }

    func sendToPC(versionId: Int64, modelId: Int64, versionName: String, downloadUrl: String) {
        Task {
            let resource = CivitaiLinkResource(
                versionId: versionId,
                modelId: modelId,
                versionName: versionName,
                downloadUrl: downloadUrl
            )
            try? await sendResource.invoke(resource: resource)
        }
    }
}

private struct CivitaiLinkSendSheet: View {
    let model: Model
    @StateObject private var vm = CivitaiLinkSendViewModel()
    @Environment(\.dismiss) private var dismiss

    var body: some View {
        NavigationView {
            Group {
                if vm.isConnected {
                    connectedView
                } else {
                    notConnectedView
                }
            }
            .navigationTitle("Send to PC")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Cancel") { dismiss() }
                }
            }
        }
        .task { await vm.observeLinkStatus() }
    }

    private var notConnectedView: some View {
        VStack(spacing: Spacing.md) {
            Image(systemName: "link.circle").font(.civitIconExtraLarge)
            Text("Civitai Link not configured").font(.civitTitleMedium)
            Text("Set up Civitai Link in Settings \u{2192} Advanced to send models to your PC")
                .font(.civitBodySmall)
                .multilineTextAlignment(.center)
                .foregroundColor(.civitOnSurfaceVariant)
        }
        .padding(Spacing.lg)
    }

    private var connectedView: some View {
        List {
            if let version = model.modelVersions.first {
                Button("Send \(version.name) to PC") {
                    vm.sendToPC(
                        versionId: version.id,
                        modelId: model.id,
                        versionName: version.name,
                        downloadUrl: "https://civitai.com/api/download/models/\(version.id)"
                    )
                    dismiss()
                }
            }
        }
    }
}
