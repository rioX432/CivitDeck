import Foundation
import Shared

@MainActor
final class BackupViewModel: ObservableObject {
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

    private let createBackupUseCase = KoinHelper.shared.getCreateBackupUseCase()
    private let restoreBackupUseCase = KoinHelper.shared.getRestoreBackupUseCase()
    private let parseBackupUseCase = KoinHelper.shared.getParseBackupUseCase()

    static let allCategoryNames: [String] = BackupCategory.allCases.map { $0.name }

    func toggleCategory(_ name: String) {
        if selectedCategories.contains(name) {
            selectedCategories.remove(name)
        } else {
            selectedCategories.insert(name)
        }
    }

    func selectAll() {
        selectedCategories = Set(Self.allCategoryNames)
    }

    func deselectAll() {
        selectedCategories = []
    }

    func exportBackup() {
        guard !selectedCategories.isEmpty else { return }
        isExporting = true
        error = nil

        let categories = selectedCategories.compactMap { name in
            BackupCategory.allCases.first { $0.name == name }
        }
        let categorySet = Set(categories)

        Task {
            do {
                let json = try await createBackupUseCase.invoke(categories: categorySet)
                let url = writeBackupFile(json: json)
                exportFileURL = url
                showExportSheet = url != nil
                isExporting = false
            } catch {
                self.error = "Export failed: \(error.localizedDescription)"
                isExporting = false
            }
        }
    }

    func onImportFileSelected(url: URL) {
        guard url.startAccessingSecurityScopedResource() else {
            error = "Cannot access file"
            return
        }
        defer { url.stopAccessingSecurityScopedResource() }

        do {
            let json = try String(contentsOf: url, encoding: .utf8)
            importJson = json

            Task {
                do {
                    let categories = try await parseBackupUseCase.invoke(json: json)
                    let categoryNames = categories.compactMap { ($0 as? BackupCategory)?.name }
                    importCategories = Set(categoryNames)
                    selectedCategories = Set(categoryNames)
                    showImportConfirmation = true
                } catch {
                    self.error = "Invalid backup file: \(error.localizedDescription)"
                }
            }
        } catch {
            self.error = "Cannot read file: \(error.localizedDescription)"
        }
    }

    func confirmImport() {
        guard let json = importJson, !selectedCategories.isEmpty else { return }
        isImporting = true
        showImportConfirmation = false
        error = nil

        let categories = selectedCategories.compactMap { name in
            BackupCategory.allCases.first { $0.name == name }
        }
        let categorySet = Set(categories)

        Task {
            do {
                try await restoreBackupUseCase.invoke(
                    json: json,
                    strategy: restoreStrategy,
                    categories: categorySet
                )
                message = "Restore completed successfully"
                importJson = nil
                importCategories = []
                isImporting = false
            } catch {
                self.error = "Restore failed: \(error.localizedDescription)"
                isImporting = false
            }
        }
    }

    func dismissImportConfirmation() {
        showImportConfirmation = false
        importJson = nil
        importCategories = []
    }

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
