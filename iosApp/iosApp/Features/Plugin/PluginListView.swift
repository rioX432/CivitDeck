import SwiftUI
import Shared

struct PluginListView: View {
    @StateObject private var viewModel = PluginListViewModel()

    var body: some View {
        Group {
            if viewModel.isLoading {
                ProgressView("Loading plugins...")
            } else if viewModel.plugins.isEmpty {
                emptyState
            } else {
                pluginList
            }
        }
        .navigationTitle("Plugins")
        .navigationBarTitleDisplayMode(.inline)
        .task { await viewModel.observePlugins() }
    }

    private var emptyState: some View {
        VStack(spacing: Spacing.md) {
            Image(systemName: "puzzlepiece.extension")
                .font(.system(size: 48))
                .foregroundColor(.civitOnSurfaceVariant)
            Text("No plugins installed")
                .font(.civitBodyMedium)
                .foregroundColor(.civitOnSurface)
            Text("Plugins extend the app with new features")
                .font(.civitBodySmall)
                .foregroundColor(.civitOnSurfaceVariant)
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
    }

    private var pluginList: some View {
        List {
            ForEach(viewModel.plugins, id: \.id) { plugin in
                NavigationLink(destination: PluginDetailView(pluginId: plugin.id)) {
                    PluginRow(plugin: plugin, viewModel: viewModel)
                }
            }
        }
    }
}

private struct PluginRow: View {
    let plugin: InstalledPlugin
    let viewModel: PluginListViewModel

    var body: some View {
        HStack(spacing: Spacing.md) {
            pluginIcon
            pluginInfo
            Spacer()
            statusDot
            Toggle("", isOn: Binding(
                get: { viewModel.isActive(plugin) },
                set: { viewModel.togglePlugin(plugin, isActive: $0) }
            ))
            .labelsHidden()
        }
        .padding(.vertical, Spacing.xs)
    }

    private var pluginIcon: some View {
        ZStack {
            RoundedRectangle(cornerRadius: 8)
                .fill(Color.civitPrimaryContainer)
                .frame(width: 40, height: 40)
            Image(systemName: "puzzlepiece.extension")
                .foregroundColor(.civitOnPrimaryContainer)
        }
    }

    private var pluginInfo: some View {
        VStack(alignment: .leading, spacing: 2) {
            Text(plugin.name)
                .font(.civitBodyMedium)
                .foregroundColor(.civitOnSurface)
            HStack(spacing: Spacing.sm) {
                Text("v\(plugin.version)")
                    .font(.civitBodySmall)
                    .foregroundColor(.civitOnSurfaceVariant)
                PluginTypeBadge(label: viewModel.typeLabel(for: plugin.pluginType))
            }
        }
    }

    private var statusDot: some View {
        Circle()
            .fill(statusColor)
            .frame(width: 8, height: 8)
    }

    private var statusColor: Color {
        switch plugin.state {
        case .active:
            return .civitPrimary
        case .error:
            return .civitError
        default:
            return .civitOutline
        }
    }
}

private struct PluginTypeBadge: View {
    let label: String

    var body: some View {
        Text(label)
            .font(.civitLabelSmall)
            .foregroundColor(.civitOnSecondaryContainer)
            .padding(.horizontal, Spacing.sm)
            .padding(.vertical, 2)
            .background(Color.civitSecondaryContainer)
            .cornerRadius(4)
    }
}
