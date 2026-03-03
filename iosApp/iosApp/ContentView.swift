import SwiftUI

struct ContentView: View {
    @Environment(\.horizontalSizeClass) private var sizeClass
    @State private var selectedTabId: String = "search"
    @StateObject private var comparisonState = ComparisonState()
    @StateObject private var tutorialVm = GestureTutorialViewModel()
    @StateObject private var searchViewModel = ModelSearchViewModel()
    @StateObject private var navBarVm = SettingsViewModelOwner()
    @EnvironmentObject private var router: NavigationRouter

    private var activeShortcuts: [NavShortcut] {
        navBarVm.powerUserMode ? navBarVm.customNavShortcuts : []
    }

    var body: some View {
        Group {
            if tutorialVm.shouldShowTutorial {
                GestureTutorialView(onDismiss: tutorialVm.dismissTutorial)
            } else if sizeClass == .regular {
                sidebarLayout
            } else {
                tabLayout
            }
        }
        .environmentObject(comparisonState)
        .task { await navBarVm.observeUiState() }
        .onChange(of: router.pendingDeepLink) { link in
            guard let link else { return }
            switch link {
            case .favorites:
                selectedTabId = "collections"
                _ = router.consume()
            case .trending, .search:
                selectedTabId = "search"
                _ = router.consume()
            case .modelDetail:
                selectedTabId = "search"
                // DeepLink is consumed by ModelSearchScreen
            }
        }
    }

    private var tabLayout: some View {
        TabView(selection: $selectedTabId) {
            ModelSearchScreen(viewModel: searchViewModel)
                .tabItem { Label("Search", systemImage: "magnifyingglass") }
                .tag("search")

            CollectionsScreen()
                .tabItem { Label("Saved", systemImage: "folder") }
                .tag("collections")

            ForEach(activeShortcuts, id: \.name) { shortcut in
                shortcutView(for: shortcut)
                    .tabItem { Label(shortcut.tabLabel, systemImage: shortcut.iconName) }
                    .tag(shortcut.name)
            }

            SettingsScreen()
                .tabItem { Label("Settings", systemImage: "gearshape") }
                .tag("settings")
        }
    }

    private var sidebarLayout: some View {
        let optionalSelection = Binding<String?>(
            get: { selectedTabId },
            set: { if let v = $0 { selectedTabId = v } }
        )
        return NavigationSplitView {
            List(selection: optionalSelection) {
                Label("Search", systemImage: "magnifyingglass").tag("search" as String?)
                Label("Collections", systemImage: "folder").tag("collections" as String?)
                ForEach(activeShortcuts, id: \.name) { shortcut in
                    Label(shortcut.label, systemImage: shortcut.iconName).tag(shortcut.name as String?)
                }
                Label("Settings", systemImage: "gearshape").tag("settings" as String?)
            }
            .navigationTitle("CivitDeck")
        } detail: {
            TabView(selection: $selectedTabId) {
                ModelSearchScreen(viewModel: searchViewModel).tag("search")
                CollectionsScreen().tag("collections")
                ForEach(activeShortcuts, id: \.name) { shortcut in
                    shortcutView(for: shortcut).tag(shortcut.name)
                }
                SettingsScreen().tag("settings")
            }
            .toolbar(.hidden, for: .tabBar)
        }
    }

    @ViewBuilder
    private func shortcutView(for shortcut: NavShortcut) -> some View {
        switch shortcut {
        case .outputGallery:
            ComfyUIHistoryView()
        case .generate:
            ComfyUIGenerationView()
        case .imageGallery:
            ImageGalleryScreen(modelVersionId: 0)
        default:
            EmptyView()
        }
    }
}

extension NavShortcut {
    var iconName: String {
        switch self {
        case .outputGallery: return "photo.stack"
        case .generate: return "wand.and.sparkles"
        case .imageGallery: return "photo"
        default: return "star"
        }
    }

    /// Shortened label for use in tab bar (avoids truncation when 5 tabs are shown)
    var tabLabel: String {
        switch self {
        case .outputGallery: return "Output"
        case .generate: return "Generate"
        case .imageGallery: return "Images"
        default: return label
        }
    }
}
