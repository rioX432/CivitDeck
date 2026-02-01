import SwiftUI
import Shared

struct ImageGalleryScreen: View {
    @StateObject private var viewModel: ImageGalleryViewModel

    private let columns = [
        GridItem(.flexible(), spacing: 8),
        GridItem(.flexible(), spacing: 8),
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
                selectedIndex: $viewModel.selectedImageIndex
            )
        }
    }

    // MARK: - Filter Bar

    private var filterBar: some View {
        VStack(spacing: 4) {
            sortChips
            periodAndNsfwRow
        }
        .padding(.horizontal, 12)
        .padding(.vertical, 8)
    }

    private var sortChips: some View {
        ScrollView(.horizontal, showsIndicators: false) {
            HStack(spacing: 8) {
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

    private var periodAndNsfwRow: some View {
        HStack {
            ScrollView(.horizontal, showsIndicators: false) {
                HStack(spacing: 8) {
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

            Spacer(minLength: 8)

            HStack(spacing: 4) {
                Text("NSFW")
                    .font(.caption2)
                    .foregroundColor(.secondary)
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
        if viewModel.isLoading && viewModel.images.isEmpty {
            Spacer()
            ProgressView()
            Spacer()
        } else if let error = viewModel.error, viewModel.images.isEmpty {
            Spacer()
            errorView(message: error)
            Spacer()
        } else if viewModel.images.isEmpty && !viewModel.isLoading {
            Spacer()
            emptyView
            Spacer()
        } else {
            imageGrid
        }
    }

    // MARK: - Image Grid

    private var imageGrid: some View {
        ScrollView {
            LazyVGrid(columns: columns, spacing: 8) {
                ForEach(Array(viewModel.images.enumerated()), id: \.element.id) { index, image in
                    imageCell(image: image, index: index)
                        .onAppear {
                            if index >= viewModel.images.count - 6 {
                                viewModel.loadMore()
                            }
                        }
                }
            }
            .padding(.horizontal, 8)

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
                case .failure:
                    Rectangle()
                        .fill(Color(.systemGray5))
                        .overlay {
                            SwiftUI.Image(systemName: "photo")
                                .foregroundColor(.secondary)
                        }
                case .empty:
                    Rectangle()
                        .fill(Color(.systemGray6))
                        .overlay { ProgressView() }
                @unknown default:
                    EmptyView()
                }
            }
            .aspectRatio(aspectRatio, contentMode: .fill)
            .clipped()
            .clipShape(RoundedRectangle(cornerRadius: 8))
        }
        .buttonStyle(.plain)
    }

    // MARK: - States

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

    private var emptyView: some View {
        VStack(spacing: 8) {
            SwiftUI.Image(systemName: "photo.on.rectangle")
                .font(.largeTitle)
                .foregroundColor(.secondary)
            Text("No images found")
                .foregroundColor(.secondary)
        }
    }

    // MARK: - Chip Button

    private func chipButton(label: String, isSelected: Bool, action: @escaping () -> Void) -> some View {
        Button(action: action) {
            Text(label)
                .font(.caption)
                .fontWeight(isSelected ? .semibold : .regular)
                .padding(.horizontal, 12)
                .padding(.vertical, 6)
                .background(
                    isSelected
                        ? Color.accentColor.opacity(0.2)
                        : Color(.systemGray5)
                )
                .foregroundColor(isSelected ? .accentColor : .primary)
                .clipShape(Capsule())
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
