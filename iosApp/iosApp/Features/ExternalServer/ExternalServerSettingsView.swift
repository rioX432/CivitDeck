import SwiftUI
import Shared

struct ExternalServerSettingsView: View {
    @StateObject private var viewModel = ExternalServerSettingsViewModel()
    @Environment(\.civitTheme) private var theme

    var body: some View {
        List {
            statusSection
            if viewModel.isConnected {
                Section {
                    NavigationLink("Open Gallery") {
                        ExternalServerGalleryView(serverName: viewModel.activeConfig?.name ?? "Gallery")
                    }
                }
            }
            configsSection
        }
        .navigationTitle("Custom Server")
        .navigationBarTitleDisplayMode(.inline)
        .toolbar {
            ToolbarItem(placement: .primaryAction) {
                Button {
                    viewModel.editingConfig = nil
                    viewModel.showAddSheet = true
                } label: {
                    Image(systemName: "plus")
                        .accessibilityLabel("Add server")
                }
            }
        }
        .sheet(isPresented: $viewModel.showAddSheet) {
            AddServerSheet(
                editing: viewModel.editingConfig,
                onSave: viewModel.onSave
            )
        }
        .task { await viewModel.observeConfigsList() }
        .task { await viewModel.observeActiveConfig() }
    }

    private var statusSection: some View {
        Section {
            VStack(alignment: .leading, spacing: Spacing.xs) {
                HStack {
                    VStack(alignment: .leading) {
                        Text(statusLabel)
                            .font(.civitTitleMedium)
                        if let active = viewModel.activeConfig {
                            Text(active.baseUrl)
                                .font(.civitBodySmall)
                                .foregroundColor(.civitOnSurfaceVariant)
                        }
                    }
                    Spacer()
                    if viewModel.isTesting {
                        ProgressView()
                    } else if viewModel.activeConfig != nil {
                        Button("Test", action: viewModel.onTest)
                    }
                }
                if let error = viewModel.testError {
                    Text(error)
                        .font(.civitBodySmall)
                        .foregroundColor(.civitError)
                }
            }
        }
    }

    private var configsSection: some View {
        Section("Servers") {
            ForEach(viewModel.configs, id: \.id) { config in
                HStack {
                    Image(systemName: config.id == viewModel.activeConfig?.id ? "checkmark.circle.fill" : "circle")
                        .foregroundColor(config.id == viewModel.activeConfig?.id ? theme.primary : .civitOnSurfaceVariant)
                        .accessibilityLabel("Activate configuration")
                        .onTapGesture { viewModel.onActivate(id: config.id) }
                    VStack(alignment: .leading, spacing: Spacing.xs) {
                        Text(config.name).font(.civitBodyMedium)
                        Text(config.baseUrl)
                            .font(.civitBodySmall)
                            .foregroundColor(.civitOnSurfaceVariant)
                    }
                    Spacer()
                    Button {
                        viewModel.editingConfig = config
                        viewModel.showAddSheet = true
                    } label: {
                        Image(systemName: "pencil")
                            .accessibilityLabel("Edit server")
                    }
                    .buttonStyle(.plain)
                    .foregroundColor(.civitOnSurfaceVariant)
                }
                .swipeActions(edge: .trailing) {
                    Button(role: .destructive) {
                        viewModel.onDelete(id: config.id)
                    } label: {
                        Label("Delete", systemImage: "trash")
                    }
                }
            }
        }
    }

    private var statusLabel: String {
        if viewModel.activeConfig == nil { return "No server configured" }
        if viewModel.isTesting { return "Testing..." }
        if viewModel.isConnected { return "Connected" }
        if viewModel.activeConfig?.lastTestSuccess?.boolValue == false { return "Connection Error" }
        return "Not tested"
    }
}

private struct AddServerSheet: View {
    let editing: ExternalServerConfig?
    let onSave: (String, String, String) -> Void

    @State private var name: String
    @State private var baseUrl: String
    @State private var apiKey: String
    @Environment(\.dismiss) private var dismiss

    init(editing: ExternalServerConfig?, onSave: @escaping (String, String, String) -> Void) {
        self.editing = editing
        self.onSave = onSave
        _name = State(initialValue: editing?.name ?? "")
        _baseUrl = State(initialValue: editing?.baseUrl ?? "")
        _apiKey = State(initialValue: editing?.apiKey ?? "")
    }

    var body: some View {
        NavigationStack {
            Form {
                Section {
                    TextField("Display Name", text: $name)
                    TextField("Server URL", text: $baseUrl)
                        .autocapitalization(.none)
                        .keyboardType(.URL)
                    TextField("API Key (optional)", text: $apiKey)
                        .autocapitalization(.none)
                }
                Section {
                    Text("Enter the base URL of your server (e.g. http://192.168.1.100:7860)")
                        .font(.civitBodySmall)
                        .foregroundColor(.civitOnSurfaceVariant)
                }
            }
            .navigationTitle(editing == nil ? "Add Server" : "Edit Server")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Cancel") { dismiss() }
                }
                ToolbarItem(placement: .confirmationAction) {
                    Button("Save") {
                        onSave(name.trimmingCharacters(in: .whitespaces),
                               baseUrl.trimmingCharacters(in: .whitespaces),
                               apiKey.trimmingCharacters(in: .whitespaces))
                        dismiss()
                    }
                    .disabled(name.isEmpty || baseUrl.isEmpty)
                }
            }
        }
    }
}
