import SwiftUI
import UniformTypeIdentifiers
import Shared

struct BackupView: View {
    @StateObject private var viewModel = BackupViewModel()
    @State private var showAlert = false
    @State private var alertMessage = ""

    var body: some View {
        List {
            dataSelectionSection
            actionSection
        }
        .navigationTitle("Backup & Restore")
        .navigationBarTitleDisplayMode(.inline)
        .fileImporter(
            isPresented: $viewModel.showImportPicker,
            allowedContentTypes: [.json, .data],
            allowsMultipleSelection: false
        ) { result in
            if case .success(let urls) = result, let url = urls.first {
                viewModel.onImportFileSelected(url: url)
            }
        }
        .sheet(isPresented: $viewModel.showExportSheet) {
            if let url = viewModel.exportFileURL {
                ShareSheet(items: [url])
            }
        }
        .alert("Restore Backup", isPresented: $viewModel.showImportConfirmation) {
            importConfirmationActions
        } message: {
            Text("Found \(viewModel.importCategories.count) categories. Tap Restore to import with \(viewModel.restoreStrategy == .merge ? "merge" : "overwrite") strategy.")
        }
        .onChange(of: viewModel.message) { newValue in
            if let msg = newValue {
                alertMessage = msg
                showAlert = true
                viewModel.message = nil
            }
        }
        .onChange(of: viewModel.error) { newValue in
            if let msg = newValue {
                alertMessage = msg
                showAlert = true
                viewModel.error = nil
            }
        }
        .alert("Notice", isPresented: $showAlert) {
            Button("OK") {}
        } message: {
            Text(alertMessage)
        }
    }

    private var dataSelectionSection: some View {
        Section {
            HStack {
                Button("Select All") { viewModel.selectAll() }
                Spacer()
                Button("Deselect All") { viewModel.deselectAll() }
            }
            .buttonStyle(.borderless)

            ForEach(BackupCategory.allCases, id: \.name) { category in
                let name = category.name
                let displayName = category.displayName
                Toggle(displayName, isOn: Binding(
                    get: { viewModel.selectedCategories.contains(name) },
                    set: { _ in viewModel.toggleCategory(name) }
                ))
            }
        } header: {
            Text("Select Data")
        }
    }

    private var actionSection: some View {
        Section {
            Button {
                viewModel.exportBackup()
            } label: {
                HStack {
                    if viewModel.isExporting {
                        ProgressView()
                            .padding(.trailing, Spacing.xs)
                    }
                    Text("Export Backup")
                }
                .frame(maxWidth: .infinity, alignment: .center)
            }
            .disabled(viewModel.selectedCategories.isEmpty || viewModel.isExporting)

            Button {
                viewModel.showImportPicker = true
            } label: {
                HStack {
                    if viewModel.isImporting {
                        ProgressView()
                            .padding(.trailing, Spacing.xs)
                    }
                    Text("Import from File")
                }
                .frame(maxWidth: .infinity, alignment: .center)
            }
            .disabled(viewModel.isImporting)

            Picker("Restore Strategy", selection: $viewModel.restoreStrategy) {
                Text("Merge").tag(RestoreStrategy.merge)
                Text("Overwrite").tag(RestoreStrategy.overwrite)
            }
        } header: {
            Text("Actions")
        }
    }

    @ViewBuilder
    private var importConfirmationActions: some View {
        Button("Restore") { viewModel.confirmImport() }
        Button("Cancel", role: .cancel) { viewModel.dismissImportConfirmation() }
    }
}
