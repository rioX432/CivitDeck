import SwiftUI
import Shared

struct CompareDestination: Hashable {
    let leftModelId: Int64
    let rightModelId: Int64
}

struct ModelSearchScreen: View {
    @ObservedObject var viewModel: ModelSearchViewModel
    @EnvironmentObject private var comparisonState: ComparisonState
    @EnvironmentObject private var router: NavigationRouter
    @Environment(\.horizontalSizeClass) private var sizeClass
    @Environment(\.civitTheme) private var theme
    @FocusState private var isSearchFocused: Bool
    @State private var showHistory: Bool = false
    @State private var headerVisible: Bool = true
    @State private var headerHeight: CGFloat = 0
    @State private var previousDragY: CGFloat = 0
    @State private var accumulatedDelta: CGFloat = 0
    @State private var isDraggingDown: Bool = false
    @State private var includeTagInput: String = ""
    @State private var excludeTagInput: String = ""
    @State private var showFilterSheet: Bool = false
    @State private var showSavedFiltersSheet: Bool = false
    @State private var showSaveFilterAlert: Bool = false
    @State private var saveFilterName: String = ""
    @State private var navigationPath = NavigationPath()
    @Namespace private var heroNamespace

    private var columns: [GridItem] {
        AdaptiveGrid.columns(userPreference: Int(viewModel.gridColumns), sizeClass: sizeClass)
    }

    var body: some View {
        NavigationStack(path: $navigationPath) {
            ZStack(alignment: .bottom) {
                VStack(spacing: 0) {
                    collapsibleHeader
                    ZStack {
                        if viewModel.isLoading && viewModel.models.isEmpty {
                            LoadingStateView()
                                .transition(.opacity)
                        } else if let error = viewModel.error, viewModel.models.isEmpty {
                            ErrorStateView(message: error) {
                                Task { await viewModel.refresh() }
                            }
                            .transition(.opacity)
                        } else if viewModel.models.isEmpty && !viewModel.isLoading {
                            emptyView
                                .transition(.opacity)
                        } else {
                            modelGrid
                                .transition(.opacity)
                        }
                    }
                    .animation(MotionAnimation.standard, value: viewModel.isLoading)
                    .animation(MotionAnimation.standard, value: viewModel.error == nil)
                    .frame(maxHeight: .infinity)
                }
                .clipped()
                HStack {
                    Spacer()
                    VStack(spacing: Spacing.sm) {
                        QRScannerFab(visible: headerVisible)
                        DiscoverFab(visible: headerVisible)
                        filterFab
                    }
                }
                if comparisonState.isActive {
                    ComparisonBottomBar(
                        modelName: comparisonState.selectedModelName ?? "",
                        onCancel: { comparisonState.cancel() }
                    )
                    .transition(.move(edge: .bottom).combined(with: .opacity))
                    .animation(MotionAnimation.fast, value: comparisonState.isActive)
                }
            }
            .navigationBarHidden(true)
            .sheet(isPresented: $showFilterSheet) {
                filterSheet
            }
            .task {
                await viewModel.observeNsfwFilter()
            }
            .navigationDestination(for: Int64.self) { modelId in
                ModelDetailScreen(modelId: modelId)
                    .heroDestination(id: modelId, in: heroNamespace)
            }
            .navigationDestination(for: String.self) { username in
                CreatorProfileScreen(username: username)
            }
            .navigationDestination(for: CompareDestination.self) { dest in
                ModelCompareScreen(leftModelId: dest.leftModelId, rightModelId: dest.rightModelId)
            }
            .navigationDestination(for: DiscoveryDestination.self) { _ in
                SwipeDiscoveryView(onModelDetail: { modelId in navigationPath.append(modelId) })
            }
            .navigationDestination(for: QRScannerDestination.self) { _ in
                QRScannerView { navigationPath.removeLast(); navigationPath.append($0) }
            }
            .task { await viewModel.observeSearchHistory() }
            .task { await viewModel.observeGridColumns() }
            .task { await viewModel.observeOwnedHashes() }
            .task { await viewModel.observeFavorites() }
            .task { await viewModel.observeSavedFilters() }
            .onChange(of: router.pendingDeepLink) { link in
                guard case .modelDetail(let id) = link else { return }
                navigationPath.append(id)
                _ = router.consume()
            }
        }
    }

    private var activeFilterCount: Int {
        var count = 0
        if viewModel.selectedType != nil { count += 1 }
        if !viewModel.selectedBaseModels.isEmpty { count += 1 }
        if viewModel.selectedSort != .mostDownloaded { count += 1 }
        if viewModel.selectedPeriod != .allTime { count += 1 }
        if viewModel.isFreshFindEnabled { count += 1 }
        if viewModel.isQualityFilterEnabled { count += 1 }
        if !viewModel.includedTags.isEmpty { count += 1 }
        if !viewModel.excludedTags.isEmpty { count += 1 }
        return count
    }

    private var collapsibleHeader: some View {
        VStack(spacing: 0) {
            searchBarWithFilterButton
            searchHistoryDropdown
        }
        .background(
            GeometryReader { geo in
                Color.clear.preference(
                    key: HeaderHeightPreferenceKey.self,
                    value: geo.size.height
                )
            }
        )
        .onPreferenceChange(HeaderHeightPreferenceKey.self) { height in
            headerHeight = height
        }
        .offset(y: headerVisible ? 0 : -headerHeight)
        .frame(height: headerVisible ? nil : 0, alignment: .top)
        .clipped()
    }

    private var searchBarWithFilterButton: some View {
        SearchBarView(
            text: $viewModel.query,
            placeholder: "Search models...",
            onSubmit: {
                viewModel.onSearch()
                showHistory = false
            },
            isFocused: $isSearchFocused
        )
        .onChange(of: viewModel.query) { newValue in
            showHistory = newValue.isEmpty
                && isSearchFocused
                && !viewModel.searchHistory.isEmpty
        }
        .onChange(of: isSearchFocused) { focused in
            if !focused { viewModel.onSearch() }
            showHistory = focused
                && viewModel.query.isEmpty
                && !viewModel.searchHistory.isEmpty
        }
    }

    private var filterSheet: some View {
        NavigationStack {
            ScrollView {
                VStack(spacing: Spacing.sm) {
                    typeFilterChips
                    baseModelFilterChips
                    sortAndPeriodChips
                    includedTagsSection
                    excludedTagsSection
                }
                .padding(.vertical, Spacing.sm)
            }
            .navigationTitle("Filters")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarLeading) {
                    Button("Reset") {
                        viewModel.resetFilters()
                        showFilterSheet = false
                    }
                }
                ToolbarItem(placement: .navigationBarTrailing) {
                    HStack(spacing: Spacing.xs) {
                        Button {
                            showSavedFiltersSheet = true
                        } label: {
                            Image(systemName: "bookmark.fill")
                        }
                        Button {
                            saveFilterName = ""
                            showSaveFilterAlert = true
                        } label: {
                            Image(systemName: "bookmark.badge.plus")
                        }
                        Button("Done") {
                            showFilterSheet = false
                        }
                        .fontWeight(.semibold)
                    }
                }
            }
        }
        .presentationDetents([.medium, .large])
        .sheet(isPresented: $showSavedFiltersSheet) {
            SavedFiltersSheet(
                savedFilters: viewModel.savedFilters,
                onApply: { filter in
                    viewModel.applyFilter(filter)
                    showFilterSheet = false
                },
                onDelete: { id in viewModel.deleteSavedFilter(id: id) },
                onDismiss: { showSavedFiltersSheet = false }
            )
        }
        .alert("Save Filter", isPresented: $showSaveFilterAlert) {
            TextField("Filter name", text: $saveFilterName)
            Button("Save") {
                let name = saveFilterName.trimmingCharacters(in: .whitespaces)
                if !name.isEmpty {
                    viewModel.saveCurrentFilter(name: name)
                }
            }
            Button("Cancel", role: .cancel) {}
        }
    }

    @ViewBuilder
    private var searchHistoryDropdown: some View {
        if showHistory && !viewModel.searchHistory.isEmpty {
            VStack(alignment: .leading, spacing: 0) {
                ForEach(viewModel.searchHistory, id: \.self) { item in
                    HStack(spacing: 0) {
                        Button {
                            viewModel.removeSearchHistoryItem(item)
                        } label: {
                            Image(systemName: "xmark")
                                .foregroundColor(.civitOnSurfaceVariant)
                                .font(.civitBodySmall)
                                .padding(.leading, Spacing.lg)
                                .padding(.vertical, Spacing.sm)
                                .padding(.trailing, Spacing.sm)
                        }
                        Button {
                            viewModel.onHistoryItemClick(item)
                            showHistory = false
                            isSearchFocused = false
                        } label: {
                            HStack {
                                Text(item)
                                    .font(.civitBodyMedium)
                                    .foregroundColor(.civitOnSurface)
                                Spacer()
                            }
                            .padding(.vertical, Spacing.sm)
                            .padding(.trailing, Spacing.lg)
                        }
                    }
                }
                Button {
                    viewModel.clearSearchHistory()
                    showHistory = false
                } label: {
                    Text("Clear history")
                        .font(.civitLabelMedium)
                        .foregroundColor(theme.primary)
                        .padding(.horizontal, Spacing.lg)
                        .padding(.vertical, Spacing.sm)
                }
            }
            .background(Color.civitSurfaceContainerHigh)
            .cornerRadius(CornerRadius.card)
            .padding(.horizontal, Spacing.lg)
            .transition(.opacity.combined(with: .move(edge: .top)))
            .animation(MotionAnimation.standard, value: showHistory)
        }
    }

    private var modelGrid: some View {
        ScrollView {
            VStack(spacing: 0) {
                if !viewModel.recommendations.isEmpty {
                    recommendationSections
                }

                LazyVGrid(columns: columns, spacing: Spacing.sm) {
                    ForEach(Array(viewModel.models.enumerated()), id: \.element.id) { index, model in
                        SwipeableModelCardView(
                            model: model,
                            isFavorite: viewModel.favoriteIds.contains(model.id),
                            onFavoriteToggle: { viewModel.toggleFavorite(model) },
                            onHide: { viewModel.hideModel(model.id, name: model.name) },
                            isOwned: viewModel.isModelOwned(model),
                            heroNamespace: heroNamespace
                        )
                        .onTapGesture {
                            if let cmpId = comparisonState.selectedModelId {
                                navigationPath.append(
                                    CompareDestination(leftModelId: cmpId, rightModelId: model.id)
                                )
                                comparisonState.cancel()
                            } else {
                                navigationPath.append(model.id)
                            }
                        }
                        .contextMenu {
                            Button {
                                comparisonState.startCompare(
                                    modelId: model.id, name: model.name
                                )
                            } label: {
                                Label("Compare", systemImage: "rectangle.split.2x1")
                            }
                            Button(role: .destructive) {
                                viewModel.hideModel(model.id, name: model.name)
                            } label: {
                                Label("Hide model", systemImage: "eye.slash")
                            }
                        }
                        .staggeredEntrance(index: index)
                        .task {
                            viewModel.onModelAppear(model.id)
                        }
                    }
                }
                .padding(.horizontal, Spacing.md)

                if viewModel.isLoadingMore {
                    ProgressView()
                        .transition(.opacity)
                        .padding()
                }
            }
        }
        .simultaneousGesture(
            DragGesture(minimumDistance: 5)
                .onChanged { value in
                    let currentY = value.translation.height
                    let delta = currentY - previousDragY
                    previousDragY = currentY

                    guard abs(delta) > 0.5 else { return }

                    // delta > 0 → finger moves down → scroll up → show header
                    // delta < 0 → finger moves up → scroll down → hide header
                    let draggingDown = delta < 0

                    if draggingDown != isDraggingDown {
                        accumulatedDelta = 0
                        isDraggingDown = draggingDown
                    }

                    accumulatedDelta += abs(delta)

                    if draggingDown && headerVisible && accumulatedDelta > 20 {
                        withAnimation(MotionAnimation.fast) { headerVisible = false }
                        accumulatedDelta = 0
                    } else if !draggingDown && !headerVisible && accumulatedDelta > 20 {
                        withAnimation(MotionAnimation.fast) { headerVisible = true }
                        accumulatedDelta = 0
                    }
                }
                .onEnded { _ in
                    previousDragY = 0
                    accumulatedDelta = 0
                }
        )
        .animation(MotionAnimation.standard, value: viewModel.isLoadingMore)
        .refreshable {
            await viewModel.refresh()
        }
    }
    private var emptyView: some View {
        EmptyStateView(icon: "magnifyingglass", title: "No models found")
    }
}
extension ModelSearchScreen { // MARK: - Filter Chips
    var typeFilterChips: some View {
        ScrollView(.horizontal, showsIndicators: false) {
            HStack(spacing: Spacing.sm) {
                ChipButton(label: "All", isSelected: viewModel.selectedType == nil) {
                    viewModel.onTypeSelected(nil)
                }
                ForEach(SearchFilter.modelTypeOptions, id: \.self) { type in
                    ChipButton(label: type.name, isSelected: viewModel.selectedType == type) {
                        viewModel.onTypeSelected(type)
                    }
                }
            }
            .padding(.horizontal, Spacing.lg)
            .padding(.vertical, Spacing.sm)
        }
    }
    var baseModelFilterChips: some View {
        ScrollView(.horizontal, showsIndicators: false) {
            HStack(spacing: Spacing.sm) {
                ForEach(SearchFilter.baseModelOptions, id: \.self) { baseModel in
                    ChipButton(
                        label: baseModel.displayName,
                        isSelected: viewModel.selectedBaseModels.contains(baseModel)
                    ) {
                        viewModel.onBaseModelToggled(baseModel)
                    }
                }
            }
            .padding(.horizontal, Spacing.lg)
            .padding(.bottom, Spacing.sm)
        }
    }
    var sortAndPeriodChips: some View {
        ScrollView(.horizontal, showsIndicators: false) {
            HStack(spacing: Spacing.sm) {
                ChipButton(label: "Fresh Only", isSelected: viewModel.isFreshFindEnabled) {
                    viewModel.onFreshFindToggled()
                }
                ChipButton(label: "Quality", isSelected: viewModel.isQualityFilterEnabled) {
                    viewModel.onQualityFilterToggled()
                }
                ForEach(SearchFilter.sortOptions, id: \.self) { sort in
                    ChipButton(label: SearchFilter.sortLabel(sort), isSelected: viewModel.selectedSort == sort) {
                        viewModel.onSortSelected(sort)
                    }
                }
                ForEach(SearchFilter.periodOptions, id: \.self) { period in
                    let selected = viewModel.selectedPeriod == period
                    ChipButton(label: SearchFilter.periodLabel(period), isSelected: selected) {
                        viewModel.onPeriodSelected(period)
                    }
                }
            }
            .padding(.horizontal, Spacing.lg)
            .padding(.bottom, Spacing.sm)
        }
    }
}
extension ModelSearchScreen { // MARK: - Filter FAB
    var filterFab: some View {
        Button {
            showFilterSheet = true
        } label: {
            ZStack(alignment: .topTrailing) {
                Image(systemName: "line.3.horizontal.decrease")
                    .font(.title2)
                    .foregroundColor(theme.primary)
                    .frame(width: 56, height: 56)
                    .background(Color.civitSurfaceContainerHigh)
                    .clipShape(RoundedRectangle(cornerRadius: CornerRadius.large))
                    .shadow(color: .black.opacity(0.15), radius: 6, y: 3)
                if activeFilterCount > 0 {
                    Text("\(activeFilterCount)")
                        .font(.civitBadgeLabel)
                        .foregroundColor(.civitOnError)
                        .frame(width: 18, height: 18)
                        .background(Color.civitError)
                        .clipShape(Circle())
                        .offset(x: 4, y: -4)
                }
            }
        }
        .padding(Spacing.lg)
        .opacity(headerVisible ? 1 : 0)
        .animation(MotionAnimation.fast, value: headerVisible)
    }
}
extension ModelSearchScreen { // MARK: - Extracted Subviews
    var includedTagsSection: some View {
        TagFilterSection(
            title: "Tags (include)",
            input: $includeTagInput,
            placeholder: "Include tag...",
            tags: viewModel.includedTags,
            chipColor: theme.primary,
            onAdd: { viewModel.addIncludedTag($0) },
            onRemove: { viewModel.removeIncludedTag($0) }
        )
    }
    var excludedTagsSection: some View {
        TagFilterSection(
            title: nil,
            input: $excludeTagInput,
            placeholder: "Exclude tag...",
            tags: viewModel.excludedTags,
            chipColor: .civitError,
            onAdd: { viewModel.addExcludedTag($0) },
            onRemove: { viewModel.removeExcludedTag($0) }
        )
    }
    var recommendationSections: some View {
        RecommendationSectionsView(recommendations: viewModel.recommendations)
    }
}
