import SwiftUI
import Shared

struct SDWebUISettingsView: View {
    @StateObject private var viewModel = SDWebUISettingsViewModel()

    var body: some View {
        List {
            statusSection
            if viewModel.isConnected {
                NavigationLink("Open Generator") {
                    SDWebUIGenerationView()
                }
            }
            connectionsSection
        }
        .navigationTitle("SD WebUI")
        .navigationBarTitleDisplayMode(.inline)
        .toolbar {
            ToolbarItem(placement: .primaryAction) {
                Button {
                    viewModel.editingConnection = nil
                    viewModel.showAddSheet = true
                } label: {
                    Image(systemName: "plus")
                        .accessibilityLabel("Add server")
                }
            }
        }
        .sheet(isPresented: $viewModel.showAddSheet) {
            SDWebUIAddConnectionSheet(
                editing: viewModel.editingConnection,
                onSave: viewModel.onSave
            )
        }
        .task { await viewModel.observeConnectionsList() }
        .task { await viewModel.observeActiveConn() }
    }

    private var statusSection: some View {
        Section {
            VStack(alignment: .leading, spacing: Spacing.xs) {
                HStack {
                    VStack(alignment: .leading) {
                        Text(statusLabel)
                            .font(.civitTitleMedium)
                        if let active = viewModel.activeConnection {
                            Text("\(active.hostname):\(active.port)")
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

    private var statusLabel: String {
        if viewModel.activeConnection == nil { return "No server configured" }
        if viewModel.isTesting { return "Testing..." }
        if viewModel.isConnected { return "Connected" }
        if viewModel.activeConnection?.lastTestSuccess?.boolValue == false {
            return "Connection Error"
        }
        return "Disconnected"
    }

    private var connectionsSection: some View {
        Section("Connections") {
            ForEach(viewModel.connections, id: \.id) { conn in
                connectionRow(conn)
            }
        }
    }

    private func connectionRow(_ conn: SDWebUIConnection) -> some View {
        HStack {
            Image(systemName: conn.id == viewModel.activeConnection?.id
                ? "checkmark.circle.fill" : "circle")
                .foregroundColor(.civitPrimary)
                .accessibilityLabel(conn.id == viewModel.activeConnection?.id
                    ? "Active connection" : "Inactive connection")
                .onTapGesture { viewModel.onActivate(id: conn.id) }
            VStack(alignment: .leading) {
                Text(conn.name).font(.civitBodyMedium)
                Text("\(conn.hostname):\(conn.port)")
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

struct SDWebUIAddConnectionSheet: View {
    let editing: SDWebUIConnection?
    let onSave: (String, String, Int32) -> Void
    @Environment(\.dismiss) private var dismiss
    @State private var name: String
    @State private var hostname: String
    @State private var portText: String

    init(editing: SDWebUIConnection?, onSave: @escaping (String, String, Int32) -> Void) {
        self.editing = editing
        self.onSave = onSave
        _name = State(initialValue: editing?.name ?? "")
        _hostname = State(initialValue: editing?.hostname ?? "")
        _portText = State(initialValue: editing.map { String($0.port) } ?? "7860")
    }

    var body: some View {
        NavigationStack {
            Form {
                TextField("Name (e.g. Home PC)", text: $name)
                TextField("Hostname / IP", text: $hostname)
                    .autocapitalization(.none)
                    .disableAutocorrection(true)
                TextField("Port", text: $portText)
                    .keyboardType(.numberPad)
            }
            .navigationTitle(editing != nil ? "Edit Connection" : "Add Connection")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Cancel") { dismiss() }
                }
                ToolbarItem(placement: .confirmationAction) {
                    Button("Save") {
                        let port = Int32(portText) ?? 7860
                        onSave(name.isEmpty ? hostname : name, hostname, port)
                        dismiss()
                    }
                    .disabled(hostname.isEmpty)
                }
            }
        }
    }
}
