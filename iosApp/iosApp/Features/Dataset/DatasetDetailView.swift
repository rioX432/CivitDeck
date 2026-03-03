import SwiftUI
import Shared

struct DatasetDetailView: View {
    @StateObject private var viewModel: DatasetDetailViewModel
    @Environment(\.horizontalSizeClass) private var sizeClass

    private let datasetName: String

    private var columns: [GridItem] {
        AdaptiveGrid.columns(sizeClass: sizeClass)
    }

    init(datasetId: Int64, datasetName: String) {
        _viewModel = StateObject(
            wrappedValue: DatasetDetailViewModel(datasetId: datasetId)
        )
        self.datasetName = datasetName
    }

    var body: some View {
        Group {
            if viewModel.images.isEmpty {
                emptyView
            } else {
                imageGrid
            }
        }
        .navigationTitle(viewModel.isSelectionMode
            ? "\(viewModel.selectedImageIds.count) selected"
            : datasetName)
        .navigationBarTitleDisplayMode(.inline)
        .toolbar { toolbarContent }
        .overlay(alignment: .bottom) {
            if viewModel.isSelectionMode && !viewModel.selectedImageIds.isEmpty {
                selectionBar
            }
        }
    }

    @ToolbarContentBuilder
    private var toolbarContent: some ToolbarContent {
        ToolbarItem(placement: .navigationBarTrailing) {
            if viewModel.isSelectionMode {
                Button("Select All") {
                    for image in viewModel.images {
                        viewModel.selectedImageIds.insert(image.id)
                    }
                }
            }
        }
        ToolbarItem(placement: .navigationBarLeading) {
            if viewModel.isSelectionMode {
                Button("Cancel") { viewModel.clearSelection() }
            }
        }
    }

    private var imageGrid: some View {
        ScrollView {
            LazyVGrid(columns: columns, spacing: Spacing.sm) {
                ForEach(viewModel.images, id: \.id) { image in
                    imageCell(image: image)
                }
            }
            .padding(Spacing.sm)
        }
    }

    private func imageCell(image: DatasetImage) -> some View {
        let isSelected = viewModel.selectedImageIds.contains(image.id)
        return CachedAsyncImage(url: URL(string: image.imageUrl)) { phase in
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
        .overlay(alignment: .topLeading) {
            if viewModel.isSelectionMode {
                selectionIndicator(isSelected: isSelected)
            }
        }
        .onTapGesture {
            if viewModel.isSelectionMode {
                viewModel.toggleSelection(id: image.id)
            }
        }
        .onLongPressGesture(minimumDuration: .infinity, pressing: { pressing in
            if pressing && !viewModel.isSelectionMode {
                viewModel.enterSelectionMode(id: image.id)
            }
        }, perform: {})
    }

    private func selectionIndicator(isSelected: Bool) -> some View {
        Circle()
            .fill(isSelected ? Color.accentColor : Color.white.opacity(0.7))
            .frame(width: 24, height: 24)
            .overlay {
                if isSelected {
                    Image(systemName: "checkmark")
                        .font(.caption.bold())
                        .foregroundColor(.civitOnPrimary)
                }
            }
            .padding(Spacing.sm)
    }

    private var selectionBar: some View {
        HStack(spacing: Spacing.lg) {
            Button(role: .destructive) {
                viewModel.removeSelected()
            } label: {
                Label(
                    "Remove \(viewModel.selectedImageIds.count) image\(viewModel.selectedImageIds.count == 1 ? "" : "s")",
                    systemImage: "trash"
                )
            }
        }
        .padding()
        .frame(maxWidth: .infinity)
        .background(.ultraThinMaterial)
    }

    private var emptyView: some View {
        EmptyStateView(
            icon: "photo.on.rectangle.angled",
            title: "No images in this dataset",
            subtitle: "Add images from ComfyUI history"
        )
    }
}
