import SwiftUI
import Shared

struct SettingsScreen: View {
    @StateObject private var viewModel = SettingsViewModel()

    var body: some View {
        List {
            Section {
                Toggle(isOn: Binding(
                    get: { viewModel.nsfwFilterLevel != .off },
                    set: { _ in viewModel.onNsfwFilterToggle() }
                )) {
                    VStack(alignment: .leading, spacing: 4) {
                        Text("NSFW Content")
                            .font(.civitBodyMedium)
                        Text("Show NSFW content in search results")
                            .font(.civitBodySmall)
                            .foregroundColor(.civitOnSurfaceVariant)
                    }
                }
            }
        }
        .navigationTitle("Settings")
        .navigationBarTitleDisplayMode(.inline)
        .task {
            await viewModel.observeNsfwFilter()
        }
    }
}
