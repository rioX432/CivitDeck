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
        }
    }
}
