import Foundation
import Shared

@MainActor
final class ModelFileBrowserViewModel: ObservableObject {
    @Published var directories: [ModelDirectory] = []
    @Published var files: [LocalModelFile] = []
    @Published var scanStatus: ScanStatus = .idle
    @Published var scanProgress: String = ""
    @Published var errorMessage: String?

    private let observeModelDirectoriesUseCase: ObserveModelDirectoriesUseCase
    private let observeLocalModelFilesUseCase: ObserveLocalModelFilesUseCase
    private let addModelDirectoryUseCase: AddModelDirectoryUseCase
    private let removeModelDirectoryUseCase: RemoveModelDirectoryUseCase
    private let scanModelDirectoriesUseCase: ScanModelDirectoriesUseCase
    private let verifyModelHashUseCase: VerifyModelHashUseCase

    init() {
        self.observeModelDirectoriesUseCase = KoinHelper.shared.getObserveModelDirectoriesUseCase()
        self.observeLocalModelFilesUseCase = KoinHelper.shared.getObserveLocalModelFilesUseCase()
        self.addModelDirectoryUseCase = KoinHelper.shared.getAddModelDirectoryUseCase()
        self.removeModelDirectoryUseCase = KoinHelper.shared.getRemoveModelDirectoryUseCase()
        self.scanModelDirectoriesUseCase = KoinHelper.shared.getScanModelDirectoriesUseCase()
        self.verifyModelHashUseCase = KoinHelper.shared.getVerifyModelHashUseCase()
    }

    func observeDirectories() async {
        for await value in observeModelDirectoriesUseCase.invoke() {
            directories = (value as? [ModelDirectory]) ?? []
        }
    }

    func observeFiles() async {
        for await value in observeLocalModelFilesUseCase.invoke() {
            files = (value as? [LocalModelFile]) ?? []
        }
    }

    func onAddDirectory(_ path: String) {
        let trimmed = path.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !trimmed.isEmpty else { return }
        Task {
            try? await addModelDirectoryUseCase.invoke(path: trimmed, label: nil)
        }
    }

    func onRemoveDirectory(_ id: Int64) {
        Task {
            try? await removeModelDirectoryUseCase.invoke(id: id)
        }
    }

    func onScanAll() {
        guard scanStatus == .idle || scanStatus == .completed || scanStatus == .error else { return }
        scanStatus = .scanning
        Task {
            do {
                try await scanModelDirectoriesUseCase.invoke(
                    directoryId: -1,
                    onProgress: { current, total in
                        Task { @MainActor in
                            self.scanProgress = "Hashing file \(current) of \(total)..."
                        }
                    }
                )
                scanStatus = .verifying
                scanProgress = "Verifying hashes..."
                await verifyAllHashes()
                scanStatus = .completed
            } catch {
                scanStatus = .error
                errorMessage = error.localizedDescription
            }
        }
    }

    func onDismissError() {
        errorMessage = nil
        scanStatus = .idle
    }

    private func verifyAllHashes() async {
        let unmatched = files.filter { $0.matchedModel == nil }
        for (index, file) in unmatched.enumerated() {
            scanProgress = "Verifying \(index + 1) of \(unmatched.count)..."
            try? await verifyModelHashUseCase.invoke(fileId: file.id, sha256Hash: file.sha256Hash)
        }
    }
}
