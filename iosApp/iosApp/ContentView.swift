import SwiftUI

struct ContentView: View {
    @Environment(\.horizontalSizeClass) private var sizeClass
    @State private var selectedTab: SidebarTab? = .search

    var body: some View {
        if sizeClass == .regular {
            sidebarLayout
        } else {
            tabLayout
        }
    }

    private var tabLayout: some View {
        TabView {
            ModelSearchScreen()
                .tabItem {
                    Label("Search", systemImage: "magnifyingglass")
                }

            FavoritesScreen()
                .tabItem {
                    Label("Favorites", systemImage: "heart")
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
                FavoritesScreen()
                    .tag(SidebarTab.favorites)
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
    case favorites
    case prompts
    case settings

    var id: String { rawValue }

    var title: String {
        switch self {
        case .search: return "Search"
        case .favorites: return "Favorites"
        case .prompts: return "Prompts"
        case .settings: return "Settings"
        }
    }

    var icon: String {
        switch self {
        case .search: return "magnifyingglass"
        case .favorites: return "heart"
        case .prompts: return "bookmark"
        case .settings: return "gearshape"
        }
    }
}
