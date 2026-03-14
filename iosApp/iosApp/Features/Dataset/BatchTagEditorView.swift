import SwiftUI
import Shared

struct BatchTagEditorView: View {
    @StateObject private var viewModel: BatchTagEditorViewModel
    @Environment(\.dismiss) private var dismiss
    @Environment(\.horizontalSizeClass) private var sizeClass

    init(datasetId: Int64) {
        _viewModel = StateObject(wrappedValue: BatchTagEditorViewModel(datasetId: datasetId))
    }

    var body: some View {
        NavigationStack {
            VStack(spacing: 0) {
                tagInputSection
                Divider()
                imageGrid
            }
            .navigationTitle(viewModel.isAddMode ? "Add Tags" : "Remove Tags")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar { toolbarContent }
        }
    }

    @ToolbarContentBuilder
    private var toolbarContent: some ToolbarContent {
        ToolbarItem(placement: .navigationBarLeading) {
            Button("Done") { dismiss() }
        }
        ToolbarItem(placement: .navigationBarTrailing) {
            HStack(spacing: Spacing.sm) {
                Button(viewModel.selectedImageIds.isEmpty ? "Select All" : "Clear") {
                    if viewModel.selectedImageIds.isEmpty {
                        viewModel.selectAll()
                    } else {
                        viewModel.clearSelection()
                    }
                }
                Button(viewModel.isAddMode ? "Add" : "Remove") {
                    viewModel.toggleMode()
                }
                .foregroundColor(viewModel.isAddMode ? .civitPrimary : .civitError)
            }
        }
    }

    private var tagInputSection: some View {
        VStack(alignment: .leading, spacing: Spacing.sm) {
            HStack {
                TextField("Tag name", text: $viewModel.tagInput)
                    .textFieldStyle(.roundedBorder)
                    .onChange(of: viewModel.tagInput) { newValue in
                        viewModel.updateTagInput(newValue)
                    }
                if !viewModel.tagInput.isEmpty {
                    Button("Apply") {
                        viewModel.applyTag(viewModel.tagInput)
                    }
                    .disabled(viewModel.selectedImageIds.isEmpty)
                }
            }
            .padding(.horizontal, Spacing.lg)
            .padding(.top, Spacing.md)

            if !viewModel.suggestions.isEmpty {
                suggestionChips
            }
        }
        .padding(.bottom, Spacing.sm)
    }

    private var suggestionChips: some View {
        ScrollView(.horizontal, showsIndicators: false) {
            HStack(spacing: Spacing.xs) {
                ForEach(viewModel.suggestions, id: \.self) { tag in
                    Button(tag) {
                        viewModel.applyTag(tag)
                    }
                    .font(.caption)
                    .padding(.horizontal, Spacing.sm)
                    .padding(.vertical, Spacing.xs)
                    .background(Color.civitSurfaceVariant)
                    .clipShape(Capsule())
                    .foregroundColor(.civitOnSurfaceVariant)
                }
            }
            .padding(.horizontal, Spacing.lg)
        }
    }

    private var columns: [GridItem] {
        AdaptiveGrid.columns(sizeClass: sizeClass)
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
                img.resizable().scaledToFill().transition(.opacity)
            case .failure:
                Color.civitSurfaceVariant
                    .overlay {
                        Image(systemName: "photo")
                            .accessibilityHidden(true)
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
        .clipShape(RoundedRectangle(cornerRadius: 8))
        .overlay(alignment: .topLeading) { selectionIndicator(isSelected: isSelected) }
        .overlay(alignment: .bottomTrailing) { tagCountBadge(image: image) }
        .onTapGesture { viewModel.toggleSelection(image.id) }
    }

    private func selectionIndicator(isSelected: Bool) -> some View {
        Circle()
            .fill(isSelected ? Color.accentColor : Color.civitSurface.opacity(0.7))
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

    private func tagCountBadge(image: DatasetImage) -> some View {
        let count = image.tags.count
        return Group {
            if count > 0 {
                Text("\(count)")
                    .font(.civitIconXSmall)
                    .padding(.horizontal, Spacing.xs)
                    .padding(.vertical, Spacing.xxs)
                    .background(Color.civitSecondary.opacity(0.85))
                    .foregroundColor(.civitOnSecondary)
                    .clipShape(RoundedRectangle(cornerRadius: 4))
                    .padding(Spacing.xs)
            }
        }
    }
}
