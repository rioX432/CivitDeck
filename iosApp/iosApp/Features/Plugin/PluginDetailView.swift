import SwiftUI
import Shared

private let configEditorMinHeight: CGFloat = 80

struct PluginDetailView: View {
    @StateObject private var viewModel: PluginDetailViewModel
    @Environment(\.dismiss) private var dismiss
    @State private var editedConfig = ""
    @State private var showUninstallConfirm = false

    init(pluginId: String) {
        _viewModel = StateObject(wrappedValue: PluginDetailViewModel(pluginId: pluginId))
    }

    var body: some View {
        Group {
            if viewModel.isLoading {
                ProgressView("Loading...")
            } else if let plugin = viewModel.plugin {
                pluginContent(plugin)
            }
        }
        .navigationTitle(viewModel.plugin?.name ?? "Plugin")
        .navigationBarTitleDisplayMode(.inline)
        .task {
            await viewModel.load()
            editedConfig = viewModel.configJson
        }
        .alert("Uninstall Plugin", isPresented: $showUninstallConfirm) {
            Button("Cancel", role: .cancel) {}
            Button("Uninstall", role: .destructive) {
                Task {
                    if await viewModel.uninstall() {
                        dismiss()
                    }
                }
            }
        } message: {
            Text("Are you sure? This will remove the plugin and its data.")
        }
    }

    private func pluginContent(_ plugin: InstalledPlugin) -> some View {
        List {
            manifestSection(plugin)
            enableSection(plugin)
            capabilitiesSection(plugin)
            configSection(plugin)
            uninstallSection
        }
    }

    private func manifestSection(_ plugin: InstalledPlugin) -> some View {
        Section("Plugin Info") {
            manifestRow("Name", plugin.name)
            manifestRow("Version", plugin.version)
            manifestRow("Author", plugin.author)
            manifestRow("Type", viewModel.typeLabel(for: plugin.pluginType))
            manifestRow("Min App Version", plugin.minAppVersion)
            if !plugin.description_.isEmpty {
                Text(plugin.description_)
                    .font(.civitBodySmall)
                    .foregroundColor(.civitOnSurfaceVariant)
            }
        }
    }

    private func manifestRow(_ label: String, _ value: String) -> some View {
        HStack {
            Text(label)
                .font(.civitBodyMedium)
            Spacer()
            Text(value)
                .font(.civitBodyMedium)
                .foregroundColor(.civitOnSurfaceVariant)
        }
    }

    private func enableSection(_ plugin: InstalledPlugin) -> some View {
        Section {
            Toggle("Enabled", isOn: Binding(
                get: { viewModel.isActive },
                set: { viewModel.togglePlugin(isActive: $0) }
            ))
        }
    }

    private func capabilitiesSection(_ plugin: InstalledPlugin) -> some View {
        Section("Capabilities") {
            if plugin.capabilities.isEmpty {
                Text("No capabilities declared")
                    .font(.civitBodySmall)
                    .foregroundColor(.civitOnSurfaceVariant)
            } else {
                ForEach(plugin.capabilities, id: \.self) { capability in
                    Text(capability as String)
                        .font(.civitBodyMedium)
                        .foregroundColor(.civitOnSurfaceVariant)
                }
            }
        }
    }

    private func configSection(_ plugin: InstalledPlugin) -> some View {
        Section("Configuration") {
            TextEditor(text: $editedConfig)
                .font(.civitBodySmall)
                .frame(minHeight: configEditorMinHeight)
            if editedConfig != viewModel.configJson {
                Button("Save Config") {
                    viewModel.saveConfig(editedConfig)
                }
            }
        }
    }

    private var uninstallSection: some View {
        Section {
            Button("Uninstall Plugin", role: .destructive) {
                showUninstallConfirm = true
            }
        }
    }
}
