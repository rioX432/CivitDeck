import SwiftUI
import Shared

struct AdvancedSettingsView: View {
    @ObservedObject var viewModel: SettingsViewModelOwner

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
                    NavigationLink(destination: ComfyUIHistoryView()) {
                        VStack(alignment: .leading, spacing: Spacing.xs) {
                            Text("Output Gallery")
                                .font(.civitBodyMedium)
                            Text("Browse generated images")
                                .font(.civitBodySmall)
                                .foregroundColor(.civitOnSurfaceVariant)
                        }
                    }
                }
                Section("SD WebUI") {
                    NavigationLink(destination: SDWebUISettingsView()) {
                        VStack(alignment: .leading, spacing: Spacing.xs) {
                            Text("SD WebUI Connections")
                                .font(.civitBodyMedium)
                            Text("Manage Automatic1111/Forge server connections")
                                .font(.civitBodySmall)
                                .foregroundColor(.civitOnSurfaceVariant)
                        }
                    }
                }
                Section("Civitai Link") {
                    NavigationLink(destination: CivitaiLinkSettingsView()) {
                        VStack(alignment: .leading, spacing: Spacing.xs) {
                            Text("Civitai Link Setup")
                                .font(.civitBodyMedium)
                            Text("Send models directly to your PC")
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
                Text("Enables ComfyUI, SD WebUI, Civitai Link, model files, and advanced metadata")
                    .font(.civitBodySmall)
                    .foregroundColor(.civitOnSurfaceVariant)
            }
        }
    }
}
