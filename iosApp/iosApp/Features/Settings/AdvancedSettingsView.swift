import SwiftUI
import Shared

struct AdvancedSettingsView: View {
    @ObservedObject var viewModel: SettingsViewModel

    var body: some View {
        List {
            Section("Power User") {
                powerUserModeToggle
            }
            if viewModel.powerUserMode {
                Section("ComfyUI") {
                    NavigationLink(destination: ComfyUISettingsView()) {
                        VStack(alignment: .leading, spacing: Spacing.xs) {
                            Text("Server Connections")
                                .font(.civitBodyMedium)
                            Text("Manage ComfyUI server connections")
                                .font(.civitBodySmall)
                                .foregroundColor(.civitOnSurfaceVariant)
                        }
                    }
                }
                Section("Model Files") {
                    NavigationLink {
                        ModelFileBrowserScreen()
                    } label: {
                        Label("Model File Browser", systemImage: "folder.badge.gearshape")
                    }
                }
            }
        }
        .navigationTitle("Advanced")
        .navigationBarTitleDisplayMode(.inline)
    }

    private var powerUserModeToggle: some View {
        Toggle(isOn: Binding(
            get: { viewModel.powerUserMode },
            set: { viewModel.onPowerUserModeChanged($0) }
        )) {
            VStack(alignment: .leading, spacing: Spacing.xs) {
                Text("Power User Mode")
                    .font(.civitBodyMedium)
                Text("Show advanced metadata on detail screens")
                    .font(.civitBodySmall)
                    .foregroundColor(.civitOnSurfaceVariant)
            }
        }
    }
}
