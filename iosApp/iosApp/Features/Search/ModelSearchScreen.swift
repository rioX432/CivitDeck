import SwiftUI
import Shared

struct ModelSearchScreen: View {
    @StateObject private var viewModel = ModelSearchViewModel()
    @FocusState private var isSearchFocused: Bool
    @State private var showHistory: Bool = false
    @State private var headerVisible: Bool = true
    @State private var headerHeight: CGFloat = 0
    @State private var previousDragY: CGFloat = 0
    @State private var accumulatedDelta: CGFloat = 0
    @State private var isDraggingDown: Bool = false

    private let columns = [
        GridItem(.flexible(), spacing: Spacing.sm),
        GridItem(.flexible(), spacing: Spacing.sm),
    ]

    var body: some View {
        NavigationStack {
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
            .navigationTitle("CivitDeck")
            .navigationBarTitleDisplayMode(.inline)
            .toolbarBackground(.visible, for: .navigationBar)
            .toolbar {
                ToolbarItem(placement: .navigationBarTrailing) {
                    NavigationLink {
                        SavedPromptsScreen()
                    } label: {
                        Image(systemName: "bookmark")
                    }
                }
                ToolbarItem(placement: .navigationBarTrailing) {
                    NavigationLink {
                        SettingsScreen()
                    } label: {
                        Image(systemName: "gearshape")
                    }
                }
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
            .task { await viewModel.observeSearchHistory() }
        }
    }

    private var collapsibleHeader: some View {
        VStack(spacing: 0) {
            searchBar
            searchHistoryDropdown
            typeFilterChips
            baseModelFilterChips
            sortAndPeriodChips
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

    private var searchBar: some View {
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
                    ForEach(Array(viewModel.models.enumerated()), id: \.element.id) { index, model in
                        NavigationLink(value: model.id) {
                            ModelCardView(model: model)
                        }
                        .buttonStyle(.plain)
                        .transition(.opacity.combined(with: .offset(y: 20)))
                        .onAppear {
                            if index == viewModel.models.count - 3 {
                                viewModel.loadMore()
                            }
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
            viewModel.refresh()
        }
    }

    private var typeFilterChips: some View {
        ScrollView(.horizontal, showsIndicators: false) {
            HStack(spacing: Spacing.sm) {
                chipButton(label: "All", isSelected: viewModel.selectedType == nil) {
                    viewModel.onTypeSelected(nil)
                }
                ForEach(modelTypeOptions, id: \.self) { type in
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
                ForEach(baseModelOptions, id: \.self) { baseModel in
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
                ForEach(sortOptions, id: \.self) { sort in
                    chipButton(label: sortLabel(sort), isSelected: viewModel.selectedSort == sort) {
                        viewModel.onSortSelected(sort)
                    }
                }
                ForEach(periodOptions, id: \.self) { period in
                    chipButton(label: periodLabel(period), isSelected: viewModel.selectedPeriod == period) {
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
                viewModel.refresh()
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

    private var recommendationSections: some View {
        ForEach(viewModel.recommendations, id: \.title) { section in
            VStack(alignment: .leading, spacing: Spacing.xs) {
                Text(section.title)
                    .font(.civitTitleMedium)
                    .padding(.horizontal, Spacing.md)
                Text(section.reason)
                    .font(.civitBodySmall)
                    .foregroundColor(.civitOnSurfaceVariant)
                    .padding(.horizontal, Spacing.md)

                ScrollView(.horizontal, showsIndicators: false) {
                    HStack(spacing: Spacing.sm) {
                        ForEach(section.models, id: \.id) { model in
                            NavigationLink(value: model.id) {
                                ModelCardView(model: model)
                                    .frame(width: 160, height: 220)
                            }
                            .buttonStyle(.plain)
                        }
                    }
                    .padding(.horizontal, Spacing.md)
                }
            }
            .padding(.bottom, Spacing.sm)
        }
    }
}

private struct HeaderHeightPreferenceKey: PreferenceKey {
    static var defaultValue: CGFloat = 0
    static func reduce(value: inout CGFloat, nextValue: () -> CGFloat) {
        value = nextValue()
    }
}

private let baseModelOptions: [BaseModel] = [.sd15, .sdxl10, .pony, .flux1D, .flux1S, .sd21, .svd]
private let sortOptions: [CivitSortOrder] = [.mostDownloaded, .highestRated, .newest]
private let periodOptions: [TimePeriod] = [.allTime, .year, .month, .week, .day]

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

private let modelTypeOptions: [ModelType] = [
    .checkpoint, .lora, .loCon, .controlnet,
    .textualInversion, .hypernetwork, .upscaler, .vae,
    .poses, .wildcards, .workflows, .motionModule,
    .aestheticGradient, .other,
]
