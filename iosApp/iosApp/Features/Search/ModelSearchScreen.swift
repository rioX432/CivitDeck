import SwiftUI
import Shared

struct CompareDestination: Hashable {
    let leftModelId: Int64
    let rightModelId: Int64
}

struct ModelSearchScreen: View {
    @StateObject private var viewModel = ModelSearchViewModel()
    @EnvironmentObject private var comparisonState: ComparisonState
    @Environment(\.horizontalSizeClass) private var sizeClass
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
    @State private var navigationPath = NavigationPath()

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
                            ProgressView()
                                .transition(.opacity)
                        } else if let error = viewModel.error, viewModel.models.isEmpty {
                            errorView(message: error)
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
                    filterFab
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
            }
            .navigationDestination(for: String.self) { username in
                CreatorProfileScreen(username: username)
            }
            .navigationDestination(for: CompareDestination.self) { dest in
                ModelCompareScreen(
                    leftModelId: dest.leftModelId,
                    rightModelId: dest.rightModelId
                )
            }
            .task { await viewModel.observeSearchHistory() }
            .task { await viewModel.observeGridColumns() }
        }
    }

    private var activeFilterCount: Int {
        var count = 0
        if viewModel.selectedType != nil { count += 1 }
        if !viewModel.selectedBaseModels.isEmpty { count += 1 }
        if viewModel.selectedSort != .mostDownloaded { count += 1 }
        if viewModel.selectedPeriod != .allTime { count += 1 }
        if viewModel.isFreshFindEnabled { count += 1 }
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
        HStack {
            Image(systemName: "magnifyingglass")
                .foregroundColor(.civitOnSurfaceVariant)
            TextField("Search models...", text: $viewModel.query)
                .focused($isSearchFocused)
                .submitLabel(.search)
                .onSubmit {
                    viewModel.onSearch()
                    showHistory = false
                }
                .onChange(of: viewModel.query) { newValue in
                    showHistory = newValue.isEmpty
                        && isSearchFocused
                        && !viewModel.searchHistory.isEmpty
                }
                .onChange(of: isSearchFocused) { focused in
                    showHistory = focused
                        && viewModel.query.isEmpty
                        && !viewModel.searchHistory.isEmpty
                }
            if !viewModel.query.isEmpty {
                Button {
                    viewModel.query = ""
                    showHistory = isSearchFocused && !viewModel.searchHistory.isEmpty
                } label: {
                    Image(systemName: "xmark.circle.fill")
                        .foregroundColor(.civitOnSurfaceVariant)
                }
            }
        }
        .padding(10)
        .overlay(
            RoundedRectangle(cornerRadius: CornerRadius.searchBar)
                .stroke(Color.civitOutlineVariant, lineWidth: 1)
        )
        .padding(.horizontal, Spacing.lg)
        .padding(.vertical, Spacing.sm)
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
                    Button("Done") {
                        showFilterSheet = false
                    }
                    .fontWeight(.semibold)
                }
            }
        }
        .presentationDetents([.medium, .large])
    }

    @ViewBuilder
    private var searchHistoryDropdown: some View {
        if showHistory && !viewModel.searchHistory.isEmpty {
            VStack(alignment: .leading, spacing: 0) {
                ForEach(viewModel.searchHistory, id: \.self) { item in
                    Button {
                        viewModel.onHistoryItemClick(item)
                        showHistory = false
                        isSearchFocused = false
                    } label: {
                        HStack(spacing: Spacing.sm) {
                            Image(systemName: "clock.arrow.circlepath")
                                .foregroundColor(.civitOnSurfaceVariant)
                                .font(.civitBodySmall)
                            Text(item)
                                .font(.civitBodyMedium)
                                .foregroundColor(.civitOnSurface)
                            Spacer()
                        }
                        .padding(.horizontal, Spacing.lg)
                        .padding(.vertical, Spacing.sm)
                    }
                }
                Button {
                    viewModel.clearSearchHistory()
                    showHistory = false
                } label: {
                    Text("Clear history")
                        .font(.civitLabelMedium)
                        .foregroundColor(.civitPrimary)
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
                    ForEach(viewModel.models, id: \.id) { model in
                        Button {
                            if let cmpId = comparisonState.selectedModelId {
                                navigationPath.append(
                                    CompareDestination(leftModelId: cmpId, rightModelId: model.id)
                                )
                                comparisonState.cancel()
                            } else {
                                navigationPath.append(model.id)
                            }
                        } label: {
                            ModelCardView(model: model)
                        }
                        .buttonStyle(.plain)
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
                        .transition(.opacity.combined(with: .offset(y: 20)))
                        .onAppear {
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

    private var typeFilterChips: some View {
        ScrollView(.horizontal, showsIndicators: false) {
            HStack(spacing: Spacing.sm) {
                chipButton(label: "All", isSelected: viewModel.selectedType == nil) {
                    viewModel.onTypeSelected(nil)
                }
                ForEach(SearchFilter.modelTypeOptions, id: \.self) { type in
                    chipButton(label: type.name, isSelected: viewModel.selectedType == type) {
                        viewModel.onTypeSelected(type)
                    }
                }
            }
            .padding(.horizontal, Spacing.lg)
            .padding(.vertical, Spacing.sm)
        }
    }

    private var baseModelFilterChips: some View {
        ScrollView(.horizontal, showsIndicators: false) {
            HStack(spacing: Spacing.sm) {
                ForEach(SearchFilter.baseModelOptions, id: \.self) { baseModel in
                    chipButton(
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

    private var sortAndPeriodChips: some View {
        ScrollView(.horizontal, showsIndicators: false) {
            HStack(spacing: Spacing.sm) {
                chipButton(label: "Fresh Only", isSelected: viewModel.isFreshFindEnabled) {
                    viewModel.onFreshFindToggled()
                }
                ForEach(SearchFilter.sortOptions, id: \.self) { sort in
                    chipButton(label: SearchFilter.sortLabel(sort), isSelected: viewModel.selectedSort == sort) {
                        viewModel.onSortSelected(sort)
                    }
                }
                ForEach(SearchFilter.periodOptions, id: \.self) { period in
                    let selected = viewModel.selectedPeriod == period
                    chipButton(label: SearchFilter.periodLabel(period), isSelected: selected) {
                        viewModel.onPeriodSelected(period)
                    }
                }
            }
            .padding(.horizontal, Spacing.lg)
            .padding(.bottom, Spacing.sm)
        }
    }

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

    private func errorView(message: String) -> some View {
        VStack(spacing: Spacing.lg) {
            Text(message)
                .foregroundColor(.civitError)
                .multilineTextAlignment(.center)
            Button("Retry") {
                Task { await viewModel.refresh() }
            }
            .buttonStyle(.bordered)
        }
        .padding()
    }

    private var emptyView: some View {
        VStack(spacing: Spacing.sm) {
            Image(systemName: "magnifyingglass")
                .font(.largeTitle)
                .foregroundColor(.civitOnSurfaceVariant)
            Text("No models found")
                .foregroundColor(.civitOnSurfaceVariant)
        }
    }
}

// MARK: - Filter FAB

extension ModelSearchScreen {
    var filterFab: some View {
        Button {
            showFilterSheet = true
        } label: {
            ZStack(alignment: .topTrailing) {
                Image(systemName: "line.3.horizontal.decrease")
                    .font(.title2)
                    .foregroundColor(.civitPrimary)
                    .frame(width: 56, height: 56)
                    .background(Color.civitSurfaceContainerHigh)
                    .clipShape(RoundedRectangle(cornerRadius: 16))
                    .shadow(color: .black.opacity(0.15), radius: 6, y: 3)

                if activeFilterCount > 0 {
                    Text("\(activeFilterCount)")
                        .font(.system(size: 11, weight: .bold))
                        .foregroundColor(.white)
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

// MARK: - Extracted Subviews

extension ModelSearchScreen {
    var includedTagsSection: some View {
        TagFilterSection(
            title: "Tags (include)",
            input: $includeTagInput,
            placeholder: "Include tag...",
            tags: viewModel.includedTags,
            chipColor: .civitPrimary,
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
