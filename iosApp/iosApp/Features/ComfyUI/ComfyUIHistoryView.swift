import SwiftUI
import Shared

struct ComfyUIHistoryView: View {
    @StateObject private var viewModel = ComfyUIHistoryViewModel()
    @Environment(\.horizontalSizeClass) private var sizeClass

    var body: some View {
        Group {
            if viewModel.isLoading && viewModel.images.isEmpty {
                LoadingStateView()
            } else if let error = viewModel.errorMessage, viewModel.images.isEmpty {
                ErrorStateView(message: error) {
                    viewModel.retry()
                }
            } else if viewModel.images.isEmpty {
                EmptyStateView(
                    icon: "photo.on.rectangle.angled",
                    title: "No generated images",
                    subtitle: "Images from ComfyUI generation history will appear here."
                )
            } else {
                VStack(spacing: 0) {
                    filterBar
                    imageGrid
                }
            }
        }
        .navigationTitle("History")
        .navigationBarTitleDisplayMode(.inline)
        .task { viewModel.startObserving() }
        .onDisappear { viewModel.stopObserving() }
    }

    // MARK: - Filter Bar

    private var filterBar: some View {
        VStack(spacing: 0) {
            sortChips
            if viewModel.workflows.count > 1 {
                workflowFilter
            }
        }
        .padding(.vertical, Spacing.xs)
    }

    private var sortChips: some View {
        ScrollView(.horizontal, showsIndicators: false) {
            HStack(spacing: Spacing.sm) {
                ForEach(HistorySortOrder.allCases, id: \.self) { sort in
                    ChipButton(
                        label: sort.rawValue,
                        isSelected: viewModel.selectedSort == sort,
                        action: { viewModel.selectedSort = sort }
                    )
                }
            }
            .padding(.horizontal, Spacing.md)
            .padding(.vertical, Spacing.xs)
        }
    }

    private var workflowFilter: some View {
        ScrollView(.horizontal, showsIndicators: false) {
            HStack(spacing: Spacing.sm) {
                ChipButton(
                    label: "All",
                    isSelected: viewModel.selectedWorkflow == nil,
                    action: { viewModel.selectedWorkflow = nil }
                )
                ForEach(viewModel.workflows, id: \.self) { promptId in
                    ChipButton(
                        label: String(promptId.prefix(8)),
                        isSelected: viewModel.selectedWorkflow == promptId,
                        action: { viewModel.selectedWorkflow = promptId }
                    )
                }
            }
            .padding(.horizontal, Spacing.md)
            .padding(.vertical, Spacing.xs)
        }
    }

    // MARK: - Image Grid

    private var imageGrid: some View {
        let columns = AdaptiveGrid.columns(sizeClass: sizeClass)
        return ScrollView {
            LazyVGrid(columns: columns, spacing: Spacing.sm) {
                ForEach(viewModel.filteredImages, id: \.id) { image in
                    imageCell(image: image)
                }
            }
            .padding(Spacing.sm)
        }
        .refreshable {
            viewModel.retry()
        }
    }

    private func imageCell(image: ComfyUIGeneratedImage) -> some View {
        NavigationLink(destination: ComfyUIOutputDetailView(image: image)) {
            CachedAsyncImage(url: URL(string: image.imageUrl)) { phase in
                switch phase {
                case .success(let img):
                    img
                        .resizable()
                        .scaledToFill()
                        .transition(.opacity)
                case .failure:
                    Color.civitSurfaceVariant
                        .overlay {
                            Image(systemName: "photo")
                                .foregroundColor(.civitOnSurfaceVariant)
                        }
                case .empty:
                    Color.civitSurfaceVariant
                        .shimmer()
                @unknown default:
                    Color.clear
                }
            }
            .frame(maxWidth: .infinity)
            .aspectRatio(1, contentMode: .fit)
            .clipped()
            .clipShape(RoundedRectangle(cornerRadius: 8))
        }
        .buttonStyle(.plain)
    }
}
