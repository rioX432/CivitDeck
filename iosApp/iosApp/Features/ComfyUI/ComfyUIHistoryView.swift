import SwiftUI
import Shared

struct ComfyUIHistoryView: View {
    @StateObject private var viewModel = ComfyUIHistoryViewModelOwner()
    @Environment(\.horizontalSizeClass) private var sizeClass

    var body: some View {
        NavigationStack {
            Group {
                if viewModel.isLoading && viewModel.images.isEmpty {
                    LoadingStateView()
                } else if let error = viewModel.error, viewModel.images.isEmpty {
                    ErrorStateView(message: error) {
                        viewModel.refresh()
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
        }
        .task { await viewModel.observeUiState() }
        .task { await viewModel.observeDatasets() }
        .task { await viewModel.observeShareHashtags() }
        .sheet(isPresented: $viewModel.showDatasetPicker, onDismiss: viewModel.onDismissDatasetPicker) {
            AddToDatasetSheet(
                datasets: viewModel.datasets,
                onSelectDataset: { datasetId in
                    viewModel.onDatasetSelected(datasetId: datasetId)
                },
                onCreateAndSelect: { name in
                    viewModel.onCreateDatasetAndSelect(name: name)
                }
            )
        }
    }

    // MARK: - Filter Bar

    private var filterBar: some View {
        ScrollView(.horizontal, showsIndicators: false) {
            HStack(spacing: Spacing.sm) {
                ForEach(Feature_comfyuiHistorySortOrder.allCases, id: \.self) { sort in
                    ChipButton(
                        label: sort.name,
                        isSelected: viewModel.selectedSort == sort,
                        action: { viewModel.onSelectSort(sort) }
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
        let filtered = viewModel.filteredImages
        return ScrollView {
            LazyVGrid(columns: columns, spacing: Spacing.sm) {
                ForEach(Array(filtered.enumerated()), id: \.element.id) { idx, image in
                    imageCell(image: image, allImages: filtered, index: idx)
                }
            }
            .padding(Spacing.sm)
        }
        .refreshable {
            viewModel.refresh()
        }
    }

    private func imageCell(image: ComfyUIGeneratedImage, allImages: [ComfyUIGeneratedImage], index: Int) -> some View {
        NavigationLink(destination: ComfyUIOutputDetailView(images: allImages, initialIndex: index)) {
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
            .clipShape(RoundedRectangle(cornerRadius: CornerRadius.image))
        }
        .buttonStyle(.plain)
        .contextMenu {
            Button {
                viewModel.onAddToDatasetTap(image: image)
            } label: {
                Label("Add to Dataset", systemImage: "folder.badge.plus")
            }
        }
    }
}
