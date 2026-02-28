import SwiftUI
import Shared

struct ComfyUIHistoryView: View {
    @StateObject private var viewModel = ComfyUIHistoryViewModel()
    @Environment(\.horizontalSizeClass) private var sizeClass
    @State private var selectedImageId: String?
    @State private var showSaveAlert = false

    private var selectedImage: ComfyUIGeneratedImage? {
        viewModel.images.first { $0.id == selectedImageId }
    }

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
                    workflowFilter
                    imageGrid
                }
            }
        }
        .navigationTitle("History")
        .navigationBarTitleDisplayMode(.inline)
        .task { viewModel.startObserving() }
        .onDisappear { viewModel.stopObserving() }
        .sheet(isPresented: Binding(
            get: { selectedImageId != nil },
            set: { if !$0 { selectedImageId = nil } }
        )) {
            if let image = selectedImage {
                ComfyUIOutputDetailView(
                    image: image,
                    onSave: { url in
                        viewModel.onSaveImage(url: url)
                        selectedImageId = nil
                    }
                )
            }
        }
        .alert(
            viewModel.imageSaveSuccess == true ? "Saved to Photos" : "Save failed",
            isPresented: $showSaveAlert
        ) {
            Button("OK") { viewModel.imageSaveSuccess = nil }
        }
        .onChange(of: viewModel.imageSaveSuccess) { newValue in
            if newValue != nil { showSaveAlert = true }
        }
    }

    // MARK: - Workflow Filter

    @ViewBuilder
    private var workflowFilter: some View {
        if viewModel.workflows.count > 1 {
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
                .padding(.vertical, Spacing.sm)
            }
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
        Button {
            selectedImageId = image.id
        } label: {
            CachedAsyncImage(url: URL(string: image.imageUrl)) { phase in
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
                            Image(systemName: "photo")
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
            .aspectRatio(1, contentMode: .fill)
            .clipped()
            .clipShape(RoundedRectangle(cornerRadius: 8))
        }
        .buttonStyle(.plain)
    }
}
