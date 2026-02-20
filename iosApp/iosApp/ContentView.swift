import SwiftUI

struct ContentView: View {
    @Environment(\.horizontalSizeClass) private var sizeClass
    @State private var selectedTab: SidebarTab? = .search
    @StateObject private var comparisonState = ComparisonState()

    var body: some View {
        Group {
            if sizeClass == .regular {
                sidebarLayout
            } else {
                tabLayout
            }
        }
        .environmentObject(comparisonState)
    }

    private var tabLayout: some View {
        TabView {
            ModelSearchScreen()
                .tabItem {
                    Label("Search", systemImage: "magnifyingglass")
                }

            CollectionsScreen()
                .tabItem {
                    Label("Collections", systemImage: "folder")
                }

            SavedPromptsScreen()
                .tabItem {
                    Label("Prompts", systemImage: "bookmark")
                }

            SettingsScreen()
                .tabItem {
                    Label("Settings", systemImage: "gearshape")
                }
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
                ModelSearchScreen()
                    .tag(SidebarTab.search)
                CollectionsScreen()
                    .tag(SidebarTab.collections)
                SavedPromptsScreen()
                    .tag(SidebarTab.prompts)
                SettingsScreen()
                    .tag(SidebarTab.settings)
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
