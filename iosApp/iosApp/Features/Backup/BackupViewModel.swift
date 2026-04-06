import Foundation
import Shared

@MainActor
final class BackupViewModelOwner: ObservableObject {
    @Published var selectedCategories: Set<String> = Set(allCategoryNames)
    @Published var isExporting = false
    @Published var isImporting = false
    @Published var showExportSheet = false
    @Published var showImportPicker = false
    @Published var showImportConfirmation = false
    @Published var exportFileURL: URL?
    @Published var importJson: String?
    @Published var importCategories: Set<String> = []
    @Published var restoreStrategy: RestoreStrategy = .merge
    @Published var message: String?
    @Published var error: String?

    private let vm: BackupViewModel
    private let store: ViewModelStore

    static let allCategoryNames: [String] = BackupCategory.allCases.map { $0.name }

    init() {
        store = ViewModelStore()
        vm = KoinHelper.shared.createBackupViewModel()
        store.put(key: "BackupViewModel", viewModel: vm)
    }

    deinit { store.clear() }

    func observeUiState() async {
        for await state in vm.uiState {
            let categories = state.selectedCategories as? Set<BackupCategory> ?? []
            selectedCategories = Set(categories.map { $0.name })
            isExporting = state.isExporting
            isImporting = state.isImporting
            showImportConfirmation = state.showImportConfirmation
            restoreStrategy = state.restoreStrategy
            message = state.message
            error = state.error
            if let json = state.exportedJson {
                let url = writeBackupFile(json: json)
                exportFileURL = url
                showExportSheet = url != nil
                vm.onExportHandled()
            }
        }
    }

    func toggleCategory(_ name: String) {
        if let category = BackupCategory.allCases.first(where: { $0.name == name }) {
            vm.onToggleCategory(category: category)
        }
    }

    func selectAll() { vm.onSelectAll() }
    func deselectAll() { vm.onDeselectAll() }

    func exportBackup() { vm.onExport() }

    func onImportFileSelected(url: URL) {
        guard url.startAccessingSecurityScopedResource() else {
            error = "Cannot access file"
            return
        }
        defer { url.stopAccessingSecurityScopedResource() }

        do {
            let json = try String(contentsOf: url, encoding: .utf8)
            vm.onImportFileLoaded(json: json)
        } catch {
            self.error = "Cannot read file: \(error.localizedDescription)"
        }
    }

    func onRestoreStrategyChanged(_ strategy: RestoreStrategy) {
        vm.onRestoreStrategyChanged(strategy: strategy)
    }

    func confirmImport() { vm.onConfirmImport() }

    func dismissImportConfirmation() { vm.onDismissImportConfirmation() }

    func dismissMessage() { vm.onMessageDismissed() }

    func dismissError() { vm.onErrorDismissed() }

    private func writeBackupFile(json: String) -> URL? {
        let formatter = DateFormatter()
        formatter.dateFormat = "yyyyMMdd_HHmmss"
        let timestamp = formatter.string(from: Date())
        let fileName = "civitdeck_backup_\(timestamp).json"

        let tempDir = FileManager.default.temporaryDirectory
        let fileURL = tempDir.appendingPathComponent(fileName)

        do {
            try json.write(to: fileURL, atomically: true, encoding: .utf8)
            return fileURL
        } catch {
            self.error = "Failed to write backup file"
            return nil
        }
    }
}
