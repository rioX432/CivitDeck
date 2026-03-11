import SwiftUI
import Shared

struct ImageGalleryScreen: View {
    @StateObject private var viewModel: ImageGalleryViewModel
    @Environment(\.horizontalSizeClass) private var sizeClass

    init(modelVersionId: Int64) {
        _viewModel = StateObject(wrappedValue: ImageGalleryViewModel(modelVersionId: modelVersionId))
    }

    var body: some View {
        VStack(spacing: 0) {
            filterBar
            contentArea
        }
        .navigationTitle("Images")
        .navigationBarTitleDisplayMode(.inline)
        .task {
            await viewModel.observeNsfwFilter()
        }
        .task {
            await viewModel.observeNsfwBlurSettings()
        }
        .fullScreenCover(isPresented: Binding(
            get: { viewModel.selectedImageIndex != nil },
            set: { if !$0 { viewModel.onDismissViewer() } }
        )) {
            ImageViewerScreen(
                images: viewModel.images,
                selectedIndex: $viewModel.selectedImageIndex,
                onSavePrompt: viewModel.savePrompt
            )
        }
    }

    // MARK: - Filter Bar

    private var filterBar: some View {
        VStack(spacing: Spacing.xs) {
            sortChips
            aspectRatioChips
            periodRow
        }
        .padding(.horizontal, Spacing.md)
        .padding(.vertical, Spacing.sm)
    }

    private var sortChips: some View {
        ScrollView(.horizontal, showsIndicators: false) {
            HStack(spacing: Spacing.sm) {
                ForEach(sortOptions, id: \.self) { sort in
                    ChipButton(
                        label: sortLabel(sort),
                        isSelected: viewModel.selectedSort == sort,
                        action: { viewModel.onSortSelected(sort) }
                    )
                }
            }
        }
    }

    private var aspectRatioChips: some View {
        ScrollView(.horizontal, showsIndicators: false) {
            HStack(spacing: Spacing.sm) {
                ChipButton(
                    label: "All",
                    isSelected: viewModel.selectedAspectRatio == nil,
                    action: { viewModel.onAspectRatioSelected(nil) }
                )
                ForEach(AspectRatioFilter.allCases, id: \.self) { filter in
                    ChipButton(
                        label: filter.rawValue,
                        isSelected: viewModel.selectedAspectRatio == filter,
                        action: { viewModel.onAspectRatioSelected(filter) }
                    )
                }
            }
        }
    }

    private var periodRow: some View {
        ScrollView(.horizontal, showsIndicators: false) {
            HStack(spacing: Spacing.sm) {
                ForEach(periodOptions, id: \.self) { period in
                    ChipButton(
                        label: periodLabel(period),
                        isSelected: viewModel.selectedPeriod == period,
                        action: { viewModel.onPeriodSelected(period) }
                    )
                }
            }
        }
    }

    // MARK: - Content

    @ViewBuilder
    private var contentArea: some View {
        Group {
            if viewModel.isLoading && viewModel.images.isEmpty {
                LoadingStateView()
                    .transition(.opacity)
            } else if let error = viewModel.error, viewModel.images.isEmpty {
                Spacer()
                ErrorStateView(message: error) {
                    viewModel.retry()
                }
                .transition(.opacity)
                Spacer()
            } else if viewModel.images.isEmpty && !viewModel.isLoading {
                Spacer()
                emptyView
                    .transition(.opacity)
                Spacer()
            } else {
                imageGrid
                    .transition(.opacity)
            }
        }
        .animation(MotionAnimation.standard, value: viewModel.isLoading)
        .animation(MotionAnimation.standard, value: viewModel.error == nil)
    }

    // MARK: - Image Grid

    private var imageGrid: some View {
        let colCount = AdaptiveGrid.columnCount(sizeClass: sizeClass)
        let indexLookup = Dictionary(
            viewModel.images.enumerated().map { ($1.id, $0) },
            uniquingKeysWith: { first, _ in first }
        )
        return ScrollView {
            StaggeredGrid(
                data: viewModel.images,
                columnCount: colCount,
                spacing: Spacing.sm,
                id: \.id,
                aspectRatio: imageAspectRatio
            ) { image in
                staggeredImageCell(image: image, index: indexLookup[image.id] ?? 0)
            }
            .padding(.horizontal, Spacing.sm)

            if viewModel.isLoadingMore {
                ProgressView()
                    .padding()
            }
        }
    }

    private func imageAspectRatio(_ image: CivitImage) -> CGFloat {
        (image.width > 0 && image.height > 0)
            ? CGFloat(image.width) / CGFloat(image.height)
            : 1.0
    }

    private func staggeredImageCell(image: CivitImage, index: Int) -> some View {
        imageCell(image: image, index: index)
            .task {
                if index >= viewModel.images.count - 6 {
                    viewModel.loadMore()
                }
            }
    }

    private func imageCell(image: CivitImage, index: Int) -> some View {
        let aspectRatio: CGFloat = (image.width > 0 && image.height > 0)
            ? CGFloat(image.width) / CGFloat(image.height)
            : 1.0
        let blurRadius = CGFloat(viewModel.nsfwBlurSettings.blurRadiusFor(level: image.nsfwLevel))

        return Button {
            viewModel.onImageSelected(index)
        } label: {
            NsfwBlurView(blurRadius: blurRadius) {
                CachedAsyncImage(url: URL(string: image.url)) { phase in
                    switch phase {
                    case .success(let img):
                        img
                            .resizable()
                            .scaledToFill()
                            .transition(.opacity)
                    case .failure:
                        Rectangle()
                            .fill(Color.civitSurfaceVariant)
                            .overlay {
                                SwiftUI.Image(systemName: "photo")
                                    .foregroundColor(.civitOnSurfaceVariant)
                            }
                    case .empty:
                        Rectangle()
                            .fill(Color.civitSurfaceVariant)
                            .shimmer()
                    @unknown default:
                        EmptyView()
                    }
                }
                .aspectRatio(aspectRatio, contentMode: .fill)
                .clipped()
            }
            .clipShape(RoundedRectangle(cornerRadius: CornerRadius.image))
        }
        .buttonStyle(.plain)
    }

    // MARK: - States

    private var emptyView: some View {
        EmptyStateView(icon: "photo.on.rectangle", title: "No images found")
    }

    // MARK: - Labels

    private func sortLabel(_ sort: CivitSortOrder) -> String {
        switch sort {
        case .highestRated: return "Highest Rated"
        case .mostDownloaded: return "Most Downloaded"
        case .newest: return "Newest"
        }
    }

    private func periodLabel(_ period: TimePeriod) -> String {
        switch period {
        case .allTime: return "All"
        case .year: return "Year"
        case .month: return "Month"
        case .week: return "Week"
        case .day: return "Day"
        }
    }
}

// MARK: - Filter Options

private let sortOptions: [CivitSortOrder] = [.highestRated, .mostDownloaded, .newest]
private let periodOptions: [TimePeriod] = [.allTime, .year, .month, .week, .day]
