import SwiftUI

struct ContentView: View {
    var body: some View {
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
}
