import SwiftUI
import Shared

struct ComfyUISettingsView: View {
    @StateObject private var viewModel = ComfyUISettingsViewModelOwner()
    @Environment(\.civitTheme) private var theme

    var body: some View {
        List {
            Section {
                NavigationLink(destination: ConnectionOnboardingView()) {
                    Label("Guided setup", systemImage: "wand.and.stars")
                }
            }
            statusSection
            if let stats = viewModel.systemStats {
                serverHardwareSection(stats)
            }
            if !viewModel.visibleSuggestions.isEmpty {
                optimizationSuggestionsSection
            }
            ntfySection
            scanLanSection
            if viewModel.isConnected {
                NavigationLink("Open txt2img Generator") {
                    ComfyUIGenerationView()
                }
                NavigationLink("View Queue") {
                    ComfyUIQueueView()
                }
                NavigationLink("View History") {
                    ComfyUIHistoryView()
                }
            }
            connectionsSection
        }
        .navigationTitle("ComfyUI")
        .navigationBarTitleDisplayMode(.inline)
        .toolbar {
            ToolbarItem(placement: .primaryAction) {
                Button {
                    viewModel.editingConnection = nil
                    viewModel.showAddSheet = true
                } label: {
                    Image(systemName: "plus")
                        .accessibilityLabel("Add connection")
                }
            }
        }
        .sheet(isPresented: $viewModel.showAddSheet) {
            AddConnectionSheet(
                editing: viewModel.editingConnection,
                onSave: viewModel.onSave
            )
        }
        .task { await viewModel.observeUiState() }
    }

    private var statusSection: some View {
        Section {
            VStack(alignment: .leading, spacing: Spacing.xs) {
                HStack {
                    VStack(alignment: .leading) {
                        HStack(spacing: Spacing.xs) {
                            Text(statusLabel)
                                .font(.civitTitleMedium)
                            securityBadge
                        }
                        if let active = viewModel.activeConnection {
                            Text(active.baseUrl)
                                .font(.civitBodySmall)
                                .foregroundColor(.civitOnSurfaceVariant)
                        }
                    }
                    Spacer()
                    if viewModel.isTesting {
                        ProgressView()
                    } else if viewModel.activeConnection != nil {
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

    @ViewBuilder
    private var securityBadge: some View {
        if let active = viewModel.activeConnection {
            if active.useHttps {
                if active.acceptSelfSigned {
                    Label("Self-signed", systemImage: "lock.trianglebadge.exclamationmark")
                        .font(.civitLabelSmall)
                        .foregroundColor(.orange)
                } else {
                    Label("HTTPS", systemImage: "lock.fill")
                        .font(.civitLabelSmall)
                        .foregroundColor(theme.primary)
                }
            } else {
                let isLan = isLanAddress(active.hostname)
                if isLan {
                    Label("LAN", systemImage: "wifi")
                        .font(.civitLabelSmall)
                        .foregroundColor(.civitOnSurfaceVariant)
                } else {
                    Label("HTTP", systemImage: "exclamationmark.triangle")
                        .font(.civitLabelSmall)
                        .foregroundColor(.civitError)
                }
            }
        }
    }

    private func isLanAddress(_ hostname: String) -> Bool {
        hostname.hasPrefix("192.168.") ||
        hostname.hasPrefix("10.") ||
        hostname.hasPrefix("172.16.") || hostname.hasPrefix("172.17.") ||
        hostname.hasPrefix("172.18.") || hostname.hasPrefix("172.19.") ||
        hostname.hasPrefix("172.2") || hostname.hasPrefix("172.3") ||
        hostname.hasPrefix("127.") ||
        hostname.lowercased() == "localhost"
    }

    private var statusLabel: String {
        if viewModel.activeConnection == nil { return "No server configured" }
        if viewModel.isTesting { return "Testing..." }
        if viewModel.isConnected { return "Connected" }
        if viewModel.activeConnection?.lastTestSuccess?.boolValue == false {
            return "Connection Error"
        }
        return "Disconnected"
    }

    private func serverHardwareSection(_ stats: Core_domainSystemStats) -> some View {
        Section("Server Hardware") {
            LabeledContent("GPU") {
                Text(stats.gpuName)
                    .font(.civitBodySmall)
            }
            vramProgressRow(stats)
            LabeledContent("RAM") {
                Text("\(stats.ramTotalMB) MB total")
                    .font(.civitBodySmall)
            }
            if let version = stats.comfyuiVersion {
                LabeledContent("ComfyUI") {
                    Text(version)
                        .font(.civitBodySmall)
                }
            }
            if let pytorch = stats.pytorchVersion {
                LabeledContent("PyTorch") {
                    Text(pytorch)
                        .font(.civitBodySmall)
                }
            }
            LabeledContent("OS") {
                Text(stats.os)
                    .font(.civitBodySmall)
            }
        }
    }

    private func vramProgressRow(_ stats: Core_domainSystemStats) -> some View {
        let vramUsed = stats.vramTotalMB - stats.vramFreeMB
        let progress = stats.vramTotalMB > 0
            ? Double(vramUsed) / Double(stats.vramTotalMB)
            : 0.0
        return VStack(alignment: .leading, spacing: Spacing.xs) {
            Text("VRAM Usage")
                .font(.civitLabelMedium)
            ProgressView(value: progress) {
                Text("\(vramUsed) / \(stats.vramTotalMB) MB")
                    .font(.civitBodySmall)
                    .foregroundColor(.civitOnSurfaceVariant)
            }
        }
    }

    private var optimizationSuggestionsSection: some View {
        Section("Optimization Suggestions") {
            ForEach(viewModel.visibleSuggestions, id: \.id) { suggestion in
                HStack(alignment: .top) {
                    VStack(alignment: .leading, spacing: Spacing.xs) {
                        Text(suggestion.title)
                            .font(.civitTitleSmall)
                        Text(suggestion.description_)
                            .font(.civitBodySmall)
                            .foregroundColor(.civitOnSurfaceVariant)
                    }
                    Spacer()
                    Button {
                        viewModel.dismissSuggestion(id: suggestion.id)
                    } label: {
                        Image(systemName: "xmark.circle")
                            .foregroundColor(.civitOnSurfaceVariant)
                            .accessibilityLabel("Dismiss")
                    }
                    .buttonStyle(.plain)
                }
            }
        }
    }

    private var ntfySection: some View {
        Section("Push Notifications (ntfy)") {
            HStack {
                Text("Status")
                    .font(.civitBodyMedium)
                Spacer()
                Text(viewModel.isNtfySubscribed ? "Subscribed" : "Not configured")
                    .font(.civitLabelSmall)
                    .foregroundColor(viewModel.isNtfySubscribed ? theme.primary : .civitOnSurfaceVariant)
            }
            if let active = viewModel.activeConnection, active.isNtfyConfigured {
                Text("\(active.resolvedNtfyServerUrl)/\(active.ntfyTopic ?? "")")
                    .font(.civitBodySmall)
                    .foregroundColor(.civitOnSurfaceVariant)
                HStack(spacing: Spacing.sm) {
                    Button {
                        viewModel.onTestNtfy()
                    } label: {
                        if viewModel.isNtfyTestSending {
                            ProgressView()
                        } else {
                            Text("Test Notification")
                        }
                    }
                    .disabled(viewModel.isNtfyTestSending)
                    if let result = viewModel.ntfyTestResult {
                        Text(result ? "Sent successfully" : "Failed to send")
                            .font(.civitBodySmall)
                            .foregroundColor(result ? theme.primary : .civitError)
                    }
                }
            } else {
                Text(
                    "Install ComfyUI-ntfy custom node on your server and set the same topic there. " +
                        "Notifications will be received even when the app is closed."
                )
                    .font(.civitBodySmall)
                    .foregroundColor(.civitOnSurfaceVariant)
            }
        }
    }

    private var scanLanSection: some View {
        Section("LAN Discovery") {
            HStack {
                Text("Scan your local network for ComfyUI servers")
                    .font(.civitBodySmall)
                    .foregroundColor(.civitOnSurfaceVariant)
                Spacer()
                if viewModel.isScanning {
                    ProgressView()
                } else {
                    Button("Scan LAN", action: viewModel.onScanLan)
                }
            }
            if let scanError = viewModel.scanError, !viewModel.isScanning {
                Text("Scan failed: \(scanError)")
                    .font(.civitBodySmall)
                    .foregroundColor(.civitError)
            }
            ForEach(viewModel.discoveredServers, id: \.ip) { server in
                Button {
                    viewModel.onSelectDiscoveredServer(server: server)
                } label: {
                    HStack {
                        VStack(alignment: .leading) {
                            Text(server.displayName)
                                .font(.civitBodyMedium)
                            Text("\(server.ip):\(server.port)")
                                .font(.civitBodySmall)
                                .foregroundColor(.civitOnSurfaceVariant)
                        }
                        Spacer()
                        Text("Add")
                            .foregroundColor(theme.primary)
                    }
                }
                .buttonStyle(.plain)
            }
        }
    }

    private var connectionsSection: some View {
        Section("Connections") {
            ForEach(viewModel.connections, id: \.id) { conn in
                connectionRow(conn)
            }
        }
    }

    private func connectionRow(_ conn: ComfyUIConnection) -> some View {
        HStack {
            Image(systemName: conn.id == viewModel.activeConnection?.id
                ? "checkmark.circle.fill" : "circle")
                .foregroundColor(theme.primary)
                .onTapGesture { viewModel.onActivate(id: conn.id) }
                .accessibilityLabel(conn.id == viewModel.activeConnection?.id
                    ? "Active connection: \(conn.name)"
                    : "Select connection: \(conn.name)")
                .accessibilityAddTraits(.isButton)
            VStack(alignment: .leading) {
                HStack(spacing: Spacing.xs) {
                    Text(conn.name).font(.civitBodyMedium)
                    if conn.isSecure {
                        Image(systemName: "lock.fill")
                            .font(.civitLabelSmall)
                            .foregroundColor(theme.primary)
                    }
                }
                Text(conn.baseUrl)
                    .font(.civitBodySmall)
                    .foregroundColor(.civitOnSurfaceVariant)
            }
            Spacer()
            Button {
                viewModel.editingConnection = conn
                viewModel.showAddSheet = true
            } label: {
                Image(systemName: "pencil")
                    .accessibilityLabel("Edit")
            }
            .buttonStyle(.borderless)
        }
        .swipeActions(edge: .trailing) {
            Button(role: .destructive) {
                viewModel.onDelete(id: conn.id)
            } label: {
                Label("Delete", systemImage: "trash")
            }
        }
    }
}

struct AddConnectionSheet: View {
    let editing: ComfyUIConnection?
    let onSave: (String, String, Int32, Bool, Bool, String?, String?) -> Void
    @Environment(\.dismiss) private var dismiss
    @State private var name: String
    @State private var hostname: String
    @State private var portText: String
    @State private var useHttps: Bool
    @State private var acceptSelfSigned: Bool
    @State private var ntfyServerUrl: String
    @State private var ntfyTopic: String

    init(
        editing: ComfyUIConnection?,
        onSave: @escaping (String, String, Int32, Bool, Bool, String?, String?) -> Void
    ) {
        self.editing = editing
        self.onSave = onSave
        _name = State(initialValue: editing?.name ?? "")
        _hostname = State(initialValue: editing?.hostname ?? "")
        _portText = State(initialValue: editing.map { String($0.port) } ?? "8188")
        _useHttps = State(initialValue: editing?.useHttps ?? false)
        _acceptSelfSigned = State(initialValue: editing?.acceptSelfSigned ?? false)
        _ntfyServerUrl = State(initialValue: editing?.ntfyServerUrl ?? "")
        _ntfyTopic = State(initialValue: editing?.ntfyTopic ?? "")
    }

    var body: some View {
        NavigationStack {
            Form {
                connectionSection
                securitySection
                ntfySection
            }
            .navigationTitle(editing != nil ? "Edit Connection" : "Add Connection")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Cancel") { dismiss() }
                }
                ToolbarItem(placement: .confirmationAction) {
                    Button("Save") {
                        let port = Int32(portText) ?? 8188
                        onSave(
                            name.isEmpty ? hostname : name,
                            hostname, port, useHttps, acceptSelfSigned,
                            ntfyServerUrl.isEmpty ? nil : ntfyServerUrl,
                            ntfyTopic.isEmpty ? nil : ntfyTopic
                        )
                        dismiss()
                    }
                    .disabled(hostname.isEmpty)
                }
            }
        }
    }

    private var connectionSection: some View {
        Section {
            TextField("Name (e.g. Home PC)", text: $name)
            TextField("Hostname / IP", text: $hostname)
                .autocapitalization(.none)
                .disableAutocorrection(true)
            TextField("Port", text: $portText)
                .keyboardType(.numberPad)
        }
    }

    private var securitySection: some View {
        Section("Security") {
            Toggle("Use HTTPS", isOn: $useHttps)
            if useHttps {
                Toggle("Accept self-signed certificates", isOn: $acceptSelfSigned)
            }
        }
    }

    private var ntfySection: some View {
        Section("Push Notifications (ntfy)") {
            TextField("Server URL (default: https://ntfy.sh)", text: $ntfyServerUrl)
                .autocapitalization(.none)
                .disableAutocorrection(true)
                .keyboardType(.URL)
            TextField("Topic (e.g. my-comfyui-topic)", text: $ntfyTopic)
                .autocapitalization(.none)
                .disableAutocorrection(true)
            Button("Generate Random Topic") {
                ntfyTopic = "civitdeck-\(UUID().uuidString.prefix(8).lowercased())"
            }
            Text("Install ComfyUI-ntfy custom node on your server and set the same topic there.")
                .font(.civitBodySmall)
                .foregroundColor(.civitOnSurfaceVariant)
        }
    }
}
