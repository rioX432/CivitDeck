import SwiftUI
import Shared

extension DatasetImage: @retroactive Identifiable {}

struct DatasetDetailView: View {
    @StateObject private var viewModel: DatasetDetailViewModel
    @Environment(\.horizontalSizeClass) private var sizeClass

    private let datasetName: String
    @State private var editCaptionImage: DatasetImage?
    @State private var showBatchTagEditor = false

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
        .sheet(isPresented: Binding(
            get: { viewModel.detailImage != nil },
            set: { if !$0 { viewModel.dismissDetail() } }
        )) {
            if let image = viewModel.detailImage {
                ImageDetailSheet(image: image) { newValue in
                    viewModel.updateTrainable(id: image.id, trainable: newValue)
                }
            }
        }
        .sheet(item: $editCaptionImage) { img in
            CaptionEditorSheet(image: img) { newText in
                viewModel.saveCaption(imageId: img.id, text: newText)
            }
        }
        .sheet(isPresented: $showBatchTagEditor) {
            BatchTagEditorView(datasetId: viewModel.datasetId)
        }
        .sheet(isPresented: $viewModel.showDuplicateReview) {
            DuplicateReviewSheet(datasetId: viewModel.datasetId)
        }
        .sheet(isPresented: $viewModel.showResolutionFilter) {
            ResolutionFilterSheet(
                initialMinWidth: viewModel.minWidth,
                initialMinHeight: viewModel.minHeight
            ) { width, height in
                viewModel.setResolutionFilter(minWidth: width, minHeight: height)
            }
        }
        .sheet(isPresented: $viewModel.showExportSheet) {
            ExportDatasetSheet(
                imageCount: viewModel.trainableImageCount,
                nonTrainableCount: viewModel.nonTrainableImageCount,
                onExport: { viewModel.startExport() }
            )
        }
        .overlay {
            if let progress = viewModel.exportProgress {
                ExportProgressOverlay(
                    progress: progress,
                    onDismiss: { viewModel.dismissExportResult() }
                )
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
            } else {
                Button { viewModel.showExportSheet = true } label: {
                    Image(systemName: "square.and.arrow.up")
                }
                Menu {
                    Button("Review Duplicates") { viewModel.showDuplicateReview = true }
                    Button("Resolution Filter") { viewModel.showResolutionFilter = true }
                } label: {
                    Image(systemName: "slider.horizontal.3")
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
            if !viewModel.isSelectionMode {
                sourceFilterPicker
                if viewModel.duplicateImageCount > 0 || viewModel.lowResImageCount > 0 {
                    Text(
                        "\(viewModel.images.count) images" +
                        " · \(viewModel.duplicateImageCount) duplicates" +
                        " · \(viewModel.lowResImageCount) low-res"
                    )
                    .font(.caption)
                    .foregroundColor(.secondary)
                    .padding(.horizontal, Spacing.sm)
                    .frame(maxWidth: .infinity, alignment: .leading)
                }
            }
            LazyVGrid(columns: columns, spacing: Spacing.sm) {
                ForEach(viewModel.filteredImages, id: \.id) { image in
                    imageCell(image: image)
                }
            }
            .padding(Spacing.sm)
        }
    }

    private var sourceFilterPicker: some View {
        Picker("Source", selection: $viewModel.selectedSource) {
            Text("All").tag(nil as ImageSource?)
            Text("CivitAI").tag(ImageSource.civitai as ImageSource?)
            Text("Local").tag(ImageSource.local as ImageSource?)
            Text("Generated").tag(ImageSource.generated as ImageSource?)
        }
        .pickerStyle(.segmented)
        .padding(Spacing.sm)
    }

    private func imageCell(image: DatasetImage) -> some View {
        let isSelected = viewModel.selectedImageIds.contains(image.id)
        return imageThumbnail(image: image)
            .clipShape(RoundedRectangle(cornerRadius: 8))
            .overlay(alignment: .topLeading) {
                if viewModel.isSelectionMode {
                    selectionIndicator(isSelected: isSelected)
                }
            }
            .overlay(alignment: .topTrailing) {
                excludedBadge(image: image)
            }
            .overlay(alignment: .bottomTrailing) {
                if !viewModel.isSelectionMode {
                    SourceBadgeMiniView(source: image.sourceType)
                        .padding(Spacing.xs)
                }
            }
            .onTapGesture {
                if viewModel.isSelectionMode {
                    viewModel.toggleSelection(id: image.id)
                } else {
                    viewModel.showDetail(image)
                }
            }
            .onLongPressGesture(minimumDuration: .infinity, pressing: { pressing in
                if pressing && !viewModel.isSelectionMode {
                    viewModel.enterSelectionMode(id: image.id)
                }
            }, perform: {})
            .contextMenu {
                Button("Edit Caption") { editCaptionImage = image }
                Button("Batch Edit Tags") { showBatchTagEditor = true }
                Button("Select") { viewModel.enterSelectionMode(id: image.id) }
            }
    }

    private func imageThumbnail(image: DatasetImage) -> some View {
        CachedAsyncImage(url: URL(string: image.imageUrl)) { phase in
            switch phase {
            case .success(let img):
                img.resizable().scaledToFill().transition(.opacity)
            case .failure:
                Color.civitSurfaceVariant
                    .overlay {
                        Image(systemName: "photo")
                            .foregroundColor(.civitOnSurfaceVariant)
                    }
            case .empty:
                Color.civitSurfaceVariant.shimmer()
            @unknown default:
                Color.clear
            }
        }
        .frame(maxWidth: .infinity)
        .aspectRatio(1, contentMode: .fit)
        .clipped()
    }

    @ViewBuilder
    private func excludedBadge(image: DatasetImage) -> some View {
        if image.excluded {
            Text("Excluded")
                .font(.caption2.bold())
                .padding(.horizontal, Spacing.xs)
                .background(Color.civitError)
                .foregroundColor(.white)
                .clipShape(Capsule())
                .padding(Spacing.xs)
        }
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
            Button {
                showBatchTagEditor = true
            } label: {
                Label("Edit Tags", systemImage: "tag")
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

private struct SourceBadgeMiniView: View {
    let source: ImageSource

    var body: some View {
        Text(label)
            .font(.civitIconXSmall)
            .padding(.horizontal, Spacing.xs)
            .padding(.vertical, Spacing.xxs)
            .background(badgeColor.opacity(0.85))
            .foregroundColor(.white)
            .clipShape(RoundedRectangle(cornerRadius: 4))
    }

    private var label: String {
        switch source {
        case .civitai: return "CI"
        case .local: return "LO"
        case .generated: return "GN"
        default: return "?"
        }
    }

    private var badgeColor: Color {
        switch source {
        case .civitai: return .civitPrimary
        case .local: return .civitSecondary
        case .generated: return .civitTertiary
        default: return .civitOnSurfaceVariant
        }
    }
}
