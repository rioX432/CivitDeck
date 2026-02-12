import SwiftUI
import Shared

struct ImageGalleryScreen: View {
    @StateObject private var viewModel: ImageGalleryViewModel

    private let columns = [
        GridItem(.flexible(), spacing: Spacing.sm),
        GridItem(.flexible(), spacing: Spacing.sm),
    ]

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
            periodAndNsfwRow
        }
        .padding(.horizontal, Spacing.md)
        .padding(.vertical, Spacing.sm)
    }

    private var sortChips: some View {
        ScrollView(.horizontal, showsIndicators: false) {
            HStack(spacing: Spacing.sm) {
                ForEach(sortOptions, id: \.self) { sort in
                    chipButton(
                        label: sortLabel(sort),
                        isSelected: viewModel.selectedSort == sort
                    ) {
                        viewModel.onSortSelected(sort)
                    }
                }
            }
        }
    }

    private var aspectRatioChips: some View {
        ScrollView(.horizontal, showsIndicators: false) {
            HStack(spacing: Spacing.sm) {
                chipButton(
                    label: "All",
                    isSelected: viewModel.selectedAspectRatio == nil
                ) {
                    viewModel.onAspectRatioSelected(nil)
                }
                ForEach(AspectRatioFilter.allCases, id: \.self) { filter in
                    chipButton(
                        label: filter.rawValue,
                        isSelected: viewModel.selectedAspectRatio == filter
                    ) {
                        viewModel.onAspectRatioSelected(filter)
                    }
                }
            }
        }
    }

    private var periodAndNsfwRow: some View {
        HStack {
            ScrollView(.horizontal, showsIndicators: false) {
                HStack(spacing: Spacing.sm) {
                    ForEach(periodOptions, id: \.self) { period in
                        chipButton(
                            label: periodLabel(period),
                            isSelected: viewModel.selectedPeriod == period
                        ) {
                            viewModel.onPeriodSelected(period)
                        }
                    }
                }
            }

            Spacer(minLength: Spacing.sm)

            HStack(spacing: Spacing.xs) {
                Text("NSFW")
                    .font(.civitLabelSmall)
                    .foregroundColor(.civitOnSurfaceVariant)
                Toggle("", isOn: Binding(
                    get: { viewModel.showNsfw },
                    set: { _ in viewModel.onNsfwToggle() }
                ))
                .labelsHidden()
                .scaleEffect(0.8)
            }
            .fixedSize()
        }
    }

    // MARK: - Content

    @ViewBuilder
    private var contentArea: some View {
        Group {
            if viewModel.isLoading && viewModel.images.isEmpty {
                Spacer()
                ProgressView()
                    .transition(.opacity)
                Spacer()
            } else if let error = viewModel.error, viewModel.images.isEmpty {
                Spacer()
                errorView(message: error)
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
        ScrollView {
            LazyVGrid(columns: columns, spacing: Spacing.sm) {
                ForEach(Array(viewModel.images.enumerated()), id: \.element.id) { index, image in
                    imageCell(image: image, index: index)
                        .onAppear {
                            if index >= viewModel.images.count - 6 {
                                viewModel.loadMore()
                            }
                        }
                }
            }
            .padding(.horizontal, Spacing.sm)

            if viewModel.isLoadingMore {
                ProgressView()
                    .padding()
            }
        }
    }

    private func imageCell(image: CivitImage, index: Int) -> some View {
        let aspectRatio: CGFloat = (image.width > 0 && image.height > 0)
            ? CGFloat(image.width) / CGFloat(image.height)
            : 1.0

        return Button {
            viewModel.onImageSelected(index)
        } label: {
            AsyncImage(url: URL(string: image.url)) { phase in
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
            .clipShape(RoundedRectangle(cornerRadius: CornerRadius.image))
        }
        .buttonStyle(.plain)
    }

    // MARK: - States

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

    private var emptyView: some View {
        VStack(spacing: Spacing.sm) {
            SwiftUI.Image(systemName: "photo.on.rectangle")
                .font(.largeTitle)
                .foregroundColor(.civitOnSurfaceVariant)
            Text("No images found")
                .foregroundColor(.civitOnSurfaceVariant)
        }
    }

    // MARK: - Chip Button

    private func chipButton(label: String, isSelected: Bool, action: @escaping () -> Void) -> some View {
        Button(action: action) {
            Text(label)
                .font(.civitLabelMedium)
                .fontWeight(isSelected ? .semibold : .regular)
                .padding(.horizontal, Spacing.md)
                .padding(.vertical, 6)
                .background(
                    isSelected
                        ? Color.civitPrimary.opacity(0.2)
                        : Color.civitSurfaceVariant
                )
                .foregroundColor(isSelected ? .civitPrimary : .civitOnSurface)
                .clipShape(Capsule())
                .animation(MotionAnimation.spring, value: isSelected)
        }
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
