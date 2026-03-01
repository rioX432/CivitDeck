import SwiftUI

struct ContentView: View {
    @Environment(\.horizontalSizeClass) private var sizeClass
    @State private var selectedTab: SidebarTab? = .search
    @StateObject private var comparisonState = ComparisonState()
    @StateObject private var tutorialVm = GestureTutorialViewModel()
    @StateObject private var searchViewModel = ModelSearchViewModel()
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
        .environmentObject(comparisonState)
        .onChange(of: router.pendingDeepLink) { link in
            guard let link else { return }
            switch link {
            case .favorites:
                selectedTab = .collections
                _ = router.consume()
            case .trending, .search:
                selectedTab = .search
                _ = router.consume()
            case .modelDetail:
                selectedTab = .search
                // DeepLink is consumed by ModelSearchScreen
            }
        }
    }

    private var tabLayout: some View {
        TabView(selection: $selectedTab) {
            ModelSearchScreen(viewModel: searchViewModel)
                .tabItem {
                    Label("Search", systemImage: "magnifyingglass")
                }
                .tag(SidebarTab.search as SidebarTab?)

            CollectionsScreen()
                .tabItem {
                    Label("Collections", systemImage: "folder")
                }
                .tag(SidebarTab.collections as SidebarTab?)

            SavedPromptsScreen()
                .tabItem {
                    Label("Prompts", systemImage: "bookmark")
                }
                .tag(SidebarTab.prompts as SidebarTab?)

            SettingsScreen()
                .tabItem {
                    Label("Settings", systemImage: "gearshape")
                }
                .tag(SidebarTab.settings as SidebarTab?)
        }
    }

    private var sidebarLayout: some View {
        NavigationSplitView {
            List(selection: $selectedTab) {
                ForEach(SidebarTab.allCases) { tab in
                    Label(tab.title, systemImage: tab.icon)
                        .tag(tab)
                }
            }
            .navigationTitle("CivitDeck")
        } detail: {
            TabView(selection: $selectedTab) {
                ModelSearchScreen(viewModel: searchViewModel)
                    .tag(SidebarTab.search as SidebarTab?)
                CollectionsScreen()
                    .tag(SidebarTab.collections as SidebarTab?)
                SavedPromptsScreen()
                    .tag(SidebarTab.prompts as SidebarTab?)
                SettingsScreen()
                    .tag(SidebarTab.settings as SidebarTab?)
            }
            .toolbar(.hidden, for: .tabBar)
        }
    }
}

private enum SidebarTab: String, CaseIterable, Identifiable {
    case search
    case collections
    case prompts
    case settings

    var id: String { rawValue }

    var title: String {
        switch self {
        case .search: return "Search"
        case .collections: return "Collections"
        case .prompts: return "Prompts"
        case .settings: return "Settings"
        }
    }

    var icon: String {
        switch self {
        case .search: return "magnifyingglass"
        case .collections: return "folder"
        case .prompts: return "bookmark"
        case .settings: return "gearshape"
        }
    }
}
