import SwiftUI
import Shared

struct ContentView: View {
    @Environment(\.horizontalSizeClass) private var sizeClass
    @State private var selectedTabId: String = "discover"
    @StateObject private var comparisonState = ComparisonState()
    @StateObject private var tutorialVm = GestureTutorialViewModel()
    @StateObject private var searchViewModel = ModelSearchViewModel()
    @StateObject private var displaySettings = DisplaySettingsViewModelOwner()
    @EnvironmentObject private var router: NavigationRouter

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
        .task { await displaySettings.observeUiState() }
        .environmentObject(comparisonState)
        .onChange(of: router.pendingDeepLink) { link in
            guard let link else { return }
            switch link {
            case .favorites:
                selectedTabId = "library"
                _ = router.consume()
            case .trending, .search:
                selectedTabId = "discover"
                _ = router.consume()
            case .modelDetail:
                selectedTabId = "discover"
                // DeepLink is consumed by ModelSearchScreen
            }
        }
    }

    private var tabLayout: some View {
        TabView(selection: $selectedTabId) {
            ModelSearchScreen(viewModel: searchViewModel)
                .tabItem { Label("Discover", systemImage: "magnifyingglass") }
                .tag("discover")

            CreateHubView()
                .tabItem { Label("Create", systemImage: "wand.and.sparkles") }
                .tag("create")

            LibraryView()
                .tabItem { Label("Library", systemImage: "folder") }
                .tag("library")

            ForEach(displaySettings.customNavShortcuts, id: \.name) { shortcut in
                shortcutView(for: shortcut)
                    .tabItem { Label(shortcut.navTabLabel, systemImage: shortcut.navTabIcon) }
                    .tag(shortcut.name)
            }

            SettingsScreen()
                .tabItem { Label("Settings", systemImage: "gearshape") }
                .tag("settings")
        }
    }

    @ViewBuilder
    private func shortcutView(for shortcut: NavShortcut) -> some View {
        switch shortcut {
        case .outputGallery:
            NavigationStack { ComfyUIHistoryView() }
        case .generate:
            NavigationStack { ComfyUIGenerationView() }
        case .externalServerGallery:
            NavigationStack { ExternalServerGalleryView(serverName: "Server") }
        default:
            EmptyView()
        }
    }

    private var sidebarLayout: some View {
        let optionalSelection = Binding<String?>(
            get: { selectedTabId },
            set: { if let v = $0 { selectedTabId = v } }
        )
        return NavigationSplitView {
            List(selection: optionalSelection) {
                Label("Discover", systemImage: "magnifyingglass").tag("discover" as String?)
                Label("Create", systemImage: "wand.and.sparkles").tag("create" as String?)
                Label("Library", systemImage: "folder").tag("library" as String?)
                Label("Settings", systemImage: "gearshape").tag("settings" as String?)
            }
            .navigationTitle("CivitDeck")
        } detail: {
            TabView(selection: $selectedTabId) {
                ModelSearchScreen(viewModel: searchViewModel).tag("discover")
                CreateHubView().tag("create")
                LibraryView().tag("library")
                SettingsScreen().tag("settings")
            }
            .toolbar(.hidden, for: .tabBar)
        }
    }
}

// MARK: - NavShortcut Tab Helpers

extension NavShortcut {
    var navTabLabel: String {
        switch self {
        case .outputGallery: return "Output"
        case .generate: return "Generate"
        case .imageGallery: return "Images"
        case .externalServerGallery: return "Server"
        default: return label
        }
    }

    var navTabIcon: String {
        switch self {
        case .outputGallery: return "photo.on.rectangle.angled"
        case .generate: return "sparkles"
        case .imageGallery: return "photo"
        case .externalServerGallery: return "server.rack"
        default: return "square.grid.2x2"
        }
    }
}
