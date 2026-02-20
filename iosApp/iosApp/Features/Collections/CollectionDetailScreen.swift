import SwiftUI
import Shared

struct CollectionDetailScreen: View {
    @StateObject private var viewModel: CollectionDetailViewModel
    @EnvironmentObject private var comparisonState: ComparisonState
    @Environment(\.horizontalSizeClass) private var sizeClass
    @State private var navigationPath = NavigationPath()
    @State private var showMoveSheet = false

    private var columns: [GridItem] {
        AdaptiveGrid.columns(userPreference: 2, sizeClass: sizeClass)
    }

    init(collectionId: Int64, collectionName: String) {
        _viewModel = StateObject(
            wrappedValue: CollectionDetailViewModel(collectionId: collectionId)
        )
    }

    var body: some View {
        NavigationStack(path: $navigationPath) {
            VStack(spacing: 0) {
                sortFilterBar
                if viewModel.displayModels.isEmpty {
                    emptyView
                } else {
                    modelsGrid
                }
            }
            .navigationTitle(viewModel.isSelectionMode
                ? "\(viewModel.selectedIds.count) selected"
                : "Collection")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar { toolbarContent }
            .navigationDestination(for: Int64.self) { modelId in
                ModelDetailScreen(modelId: modelId)
            }
            .navigationDestination(for: CompareDestination.self) { dest in
                ModelCompareScreen(
                    leftModelId: dest.leftModelId,
                    rightModelId: dest.rightModelId
                )
            }
        }
        .overlay(alignment: .bottom) {
            if viewModel.isSelectionMode && !viewModel.selectedIds.isEmpty {
                selectionBar
            }
        }
        .confirmationDialog(
            "Move to Collection",
            isPresented: $showMoveSheet,
            titleVisibility: .visible
        ) {
            ForEach(
                viewModel.collections.filter { $0.id != viewModel.collectionId },
                id: \.id
            ) { target in
                Button(target.name) {
                    viewModel.moveSelectedTo(target.id)
                }
            }
            Button("Cancel", role: .cancel) {}
        }
    }

    @ToolbarContentBuilder
    private var toolbarContent: some ToolbarContent {
        ToolbarItem(placement: .navigationBarTrailing) {
            if viewModel.isSelectionMode {
                Button("Select All") { viewModel.selectAll() }
            }
        }
        ToolbarItem(placement: .navigationBarLeading) {
            if viewModel.isSelectionMode {
                Button("Cancel") { viewModel.clearSelection() }
            }
        }
    }

    private var sortFilterBar: some View {
        ScrollView(.horizontal, showsIndicators: false) {
            HStack(spacing: Spacing.sm) {
                sortMenu
                typeMenu
            }
            .padding(.horizontal, Spacing.md)
            .padding(.vertical, Spacing.xs)
        }
    }

    private var sortMenu: some View {
        Menu {
            ForEach(CollectionSortOrder_.allCases, id: \.self) { order in
                Button {
                    viewModel.sortOrder = order
                } label: {
                    if viewModel.sortOrder == order {
                        Label(order.rawValue, systemImage: "checkmark")
                    } else {
                        Text(order.rawValue)
                    }
                }
            }
        } label: {
            Label(viewModel.sortOrder.rawValue, systemImage: "arrow.up.arrow.down")
                .font(.civitLabelSmall)
                .padding(.horizontal, Spacing.sm)
                .padding(.vertical, Spacing.xs)
                .background(Color.civitSurfaceVariant)
                .clipShape(Capsule())
        }
    }

    private var typeMenu: some View {
        Menu {
            Button {
                viewModel.typeFilter = nil
            } label: {
                if viewModel.typeFilter == nil {
                    Label("All Types", systemImage: "checkmark")
                } else {
                    Text("All Types")
                }
            }
            ForEach(CollectionDetailViewModel.allModelTypes, id: \.self) { type in
                Button {
                    viewModel.typeFilter = type
                } label: {
                    if viewModel.typeFilter == type {
                        Label(type.name, systemImage: "checkmark")
                    } else {
                        Text(type.name)
                    }
                }
            }
        } label: {
            Label(
                viewModel.typeFilter?.name ?? "All Types",
                systemImage: "line.3.horizontal.decrease"
            )
            .font(.civitLabelSmall)
            .padding(.horizontal, Spacing.sm)
            .padding(.vertical, Spacing.xs)
            .background(Color.civitSurfaceVariant)
            .clipShape(Capsule())
        }
    }

    private var modelsGrid: some View {
        ScrollView {
            LazyVGrid(columns: columns, spacing: Spacing.sm) {
                ForEach(viewModel.displayModels, id: \.id) { model in
                    Button {
                        if viewModel.isSelectionMode {
                            viewModel.toggleSelection(model.id)
                        } else if let cmpId = comparisonState.selectedModelId {
                            navigationPath.append(
                                CompareDestination(
                                    leftModelId: cmpId,
                                    rightModelId: model.id
                                )
                            )
                            comparisonState.cancel()
                        } else {
                            navigationPath.append(model.id)
                        }
                    } label: {
                        CollectionModelCard(
                            model: model,
                            isSelected: viewModel.selectedIds.contains(model.id),
                            isSelectionMode: viewModel.isSelectionMode
                        )
                    }
                    .buttonStyle(.plain)
                    .contextMenu {
                        Button {
                            viewModel.enterSelectionMode(model.id)
                        } label: {
                            Label("Select", systemImage: "checkmark.circle")
                        }
                        Button {
                            comparisonState.startCompare(
                                modelId: model.id, name: model.name
                            )
                        } label: {
                            Label("Compare", systemImage: "rectangle.split.2x1")
                        }
                    }
                }
            }
            .padding(.horizontal, Spacing.md)
        }
    }

    private var selectionBar: some View {
        HStack(spacing: Spacing.lg) {
            Button {
                showMoveSheet = true
            } label: {
                Label("Move", systemImage: "folder")
            }
            Button(role: .destructive) {
                viewModel.removeSelected()
            } label: {
                Label("Remove", systemImage: "trash")
            }
        }
        .padding()
        .frame(maxWidth: .infinity)
        .background(.ultraThinMaterial)
    }

    private var emptyView: some View {
        VStack(spacing: Spacing.sm) {
            Spacer()
            Image(systemName: "tray")
                .font(.largeTitle)
                .foregroundColor(.civitOnSurfaceVariant)
            Text("No models in this collection")
                .font(.civitTitleMedium)
                .foregroundColor(.civitOnSurfaceVariant)
            Text("Add models from the detail screen")
                .font(.civitBodyMedium)
                .foregroundColor(.civitOnSurfaceVariant)
            Spacer()
        }
    }
}

private struct CollectionModelCard: View {
    let model: FavoriteModelSummary
    let isSelected: Bool
    let isSelectionMode: Bool

    var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            thumbnailImage
            cardInfo
        }
        .background(Color.civitSurface)
        .clipShape(RoundedRectangle(cornerRadius: CornerRadius.card))
        .shadow(color: .black.opacity(0.1), radius: 4, x: 0, y: 2)
        .overlay(alignment: .topLeading) {
            if isSelectionMode {
                selectionIndicator
            }
        }
    }

    @ViewBuilder
    private var thumbnailImage: some View {
        if let urlString = model.thumbnailUrl, let imageUrl = URL(string: urlString) {
            Color.civitSurfaceVariant
                .aspectRatio(1, contentMode: .fit)
                .overlay {
                    CachedAsyncImage(url: imageUrl) { phase in
                        switch phase {
                        case .success(let image):
                            image.resizable().scaledToFill().transition(.opacity)
                        case .failure:
                            Image(systemName: "photo").foregroundColor(.civitOnSurfaceVariant)
                        case .empty:
                            Rectangle().fill(Color.civitSurfaceVariant).shimmer()
                        @unknown default:
                            Image(systemName: "photo").foregroundColor(.civitOnSurfaceVariant)
                        }
                    }
                }
                .clipped()
        } else {
            Rectangle()
                .fill(Color.civitSurfaceVariant)
                .aspectRatio(1, contentMode: .fit)
                .overlay {
                    Image(systemName: "photo").foregroundColor(.civitOnSurfaceVariant)
                }
        }
    }

    private var cardInfo: some View {
        VStack(alignment: .leading, spacing: Spacing.xs) {
            Text(model.name)
                .font(.civitTitleSmall)
                .lineLimit(1)
            Text(model.type.name)
                .font(.civitLabelSmall)
                .padding(.horizontal, 6)
                .padding(.vertical, 2)
                .background(Color.civitSurfaceVariant)
                .clipShape(Capsule())
            statsRow
        }
        .padding(Spacing.sm)
    }

    private var statsRow: some View {
        HStack(spacing: Spacing.sm) {
            statItem(icon: "arrow.down.circle",
                     value: FormatUtils.shared.formatCount(count: model.downloadCount))
            statItem(icon: "heart",
                     value: FormatUtils.shared.formatCount(count: model.favoriteCount))
            statItem(icon: "star",
                     value: FormatUtils.shared.formatRating(rating: model.rating))
        }
    }

    private func statItem(icon: String, value: String) -> some View {
        HStack(spacing: 2) {
            Image(systemName: icon).font(.system(size: IconSize.statIcon))
            Text(value).font(.civitLabelSmall)
        }
        .foregroundColor(.civitOnSurfaceVariant)
    }

    private var selectionIndicator: some View {
        Circle()
            .fill(isSelected ? Color.accentColor : Color.white.opacity(0.7))
            .frame(width: 24, height: 24)
            .overlay {
                if isSelected {
                    Image(systemName: "checkmark")
                        .font(.caption.bold())
                        .foregroundColor(.white)
                }
            }
            .padding(Spacing.sm)
    }
}
