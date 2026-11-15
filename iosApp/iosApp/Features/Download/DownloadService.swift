import Foundation
import Shared

/// The persisted download state `DownloadService` reads and writes; injectable so transfer
/// creation can be exercised without Koin or a database.
protocol DownloadRecordStore: AnyObject {
    func getDownloadById(id: Int64) async throws -> ModelDownload?
    func updateStatus(id: Int64, status: DownloadStatus, errorMessage: String?) async throws
    func updateProgress(id: Int64, downloadedBytes: Int64) async throws
    func updateDestinationPath(id: Int64, path: String) async throws
}

protocol DownloadTaskHandle: AnyObject {
    var taskIdentifier: Int { get }
    func resume()
    func cancelProducingResumeData(_ completionHandler: @escaping @Sendable (Data?) -> Void)
}

protocol DownloadTaskFactory: AnyObject {
    func makeTask(request: URLRequest, downloadId: Int64) -> DownloadTaskHandle
    func makeTask(resumeData: Data, downloadId: Int64) -> DownloadTaskHandle
}

/// Records the final status for a finished file and decides whether it may replace the destination.
protocol DownloadHashVerifier: AnyObject {
    func verify(downloadId: Int64, actualSha256: String?) async throws -> Bool
}

extension URLSessionDownloadTask: DownloadTaskHandle {
    func cancelProducingResumeData(_ completionHandler: @escaping @Sendable (Data?) -> Void) {
        cancel(byProducingResumeData: completionHandler)
    }
}

/// Resolves the repository on first use because the shared singleton can be created before Koin starts.
private final class KoinDownloadRecordStore: DownloadRecordStore {
    private lazy var repository: ModelDownloadRepository = {
        KoinHelper.shared.getModelDownloadRepository()
    }()

    func getDownloadById(id: Int64) async throws -> ModelDownload? {
        try await repository.getDownloadById(id: id)
    }

    func updateStatus(id: Int64, status: DownloadStatus, errorMessage: String?) async throws {
        try await repository.updateStatus(id: id, status: status, errorMessage: errorMessage)
    }

    func updateProgress(id: Int64, downloadedBytes: Int64) async throws {
        try await repository.updateProgress(id: id, downloadedBytes: downloadedBytes)
    }

    func updateDestinationPath(id: Int64, path: String) async throws {
        try await repository.updateDestinationPath(id: id, path: path)
    }
}

private final class KoinDownloadHashVerifier: DownloadHashVerifier {
    private lazy var useCase: VerifyDownloadHashUseCase = {
        KoinHelper.shared.getVerifyDownloadHashUseCase()
    }()

    func verify(downloadId: Int64, actualSha256: String?) async throws -> Bool {
        try await useCase.invoke(downloadId: downloadId, actualSha256: actualSha256).boolValue
    }
}

private final class BackgroundSessionTaskFactory: DownloadTaskFactory {
    private let delegate: URLSessionDownloadDelegate
    private lazy var session: URLSession = {
        let config = URLSessionConfiguration.background(
            withIdentifier: "com.riox432.civitdeck.download"
        )
        config.isDiscretionary = false
        config.sessionSendsLaunchEvents = true
        return URLSession(configuration: config, delegate: delegate, delegateQueue: nil)
    }()

    init(delegate: URLSessionDownloadDelegate) {
        self.delegate = delegate
    }

    func makeTask(request: URLRequest, downloadId: Int64) -> DownloadTaskHandle {
        let task = session.downloadTask(with: request)
        task.taskDescription = String(downloadId)
        return task
    }

    func makeTask(resumeData: Data, downloadId: Int64) -> DownloadTaskHandle {
        let task = session.downloadTask(withResumeData: resumeData)
        task.taskDescription = String(downloadId)
        return task
    }
}

@MainActor
final class DownloadService: NSObject, ObservableObject {
    static let shared = DownloadService()

    /// An ID stays here until its task reaches a terminal callback, so enqueue, retry and resume
    /// for one record never create a second task while one is in flight.
    private var activeTasks: [Int64: DownloadTaskHandle] = [:]
    /// IDs whose transfer is being prepared across an `await`, before a task exists in `activeTasks`.
    private var startingIds: Set<Int64> = []
    /// Finished files still being hashed, verified and moved, so no new transfer races that step and
    /// Cancel can stop it. The token keeps a cancelled completion from removing a newer one's entry.
    private var completions: [Int64: (token: UUID, task: Task<Void, Never>)] = [:]
    private let downloadDelegate: DownloadSessionDelegate
    private let repository: DownloadRecordStore
    private let apiKeyProvider: () -> String?
    private let taskFactory: DownloadTaskFactory
    private let hashVerifier: DownloadHashVerifier
    private let hashWorker: DownloadHashWorker

    init(
        repository: DownloadRecordStore? = nil,
        apiKeyProvider: (() -> String?)? = nil,
        taskFactory: DownloadTaskFactory? = nil,
        hashVerifier: DownloadHashVerifier? = nil,
        hashWorker: DownloadHashWorker = DownloadHashWorker()
    ) {
        let delegate = DownloadSessionDelegate()
        downloadDelegate = delegate
        self.repository = repository ?? KoinDownloadRecordStore()
        self.apiKeyProvider = apiKeyProvider ?? { KoinHelper.shared.getApiKeyProvider().apiKey }
        self.taskFactory = taskFactory ?? BackgroundSessionTaskFactory(delegate: delegate)
        self.hashVerifier = hashVerifier ?? KoinDownloadHashVerifier()
        self.hashWorker = hashWorker
        super.init()
        delegate.service = self
    }

    private func isTransferInFlight(_ downloadId: Int64) -> Bool {
        startingIds.contains(downloadId) || completions[downloadId] != nil || activeTasks[downloadId] != nil
    }

    func startDownload(downloadId: Int64, url: String, apiKey: String?) {
        guard !isTransferInFlight(downloadId) else { return }
        createFreshTask(downloadId: downloadId, url: url, apiKey: apiKey)
    }

    private func makeRequest(url: String, apiKey: String?) -> URLRequest? {
        guard let downloadUrl = URL(string: url) else { return nil }
        var request = URLRequest(url: downloadUrl)
        if let apiKey, !apiKey.isEmpty {
            request.setValue("Bearer \(apiKey)", forHTTPHeaderField: "Authorization")
        }
        return request
    }

    private func createFreshTask(downloadId: Int64, url: String, apiKey: String?) {
        guard let request = makeRequest(url: url, apiKey: apiKey) else { return }
        launchTask(taskFactory.makeTask(request: request, downloadId: downloadId), downloadId: downloadId)
    }

    private func launchTask(_ task: DownloadTaskHandle, downloadId: Int64) {
        activeTasks[downloadId] = task
        task.resume()
    }

    /// Starts the transfer for a download that was just enqueued (inserted into the DB
    /// as `Pending`) but has no in-flight `URLSessionDownloadTask` yet.
    func handleEnqueuedDownload(downloadId: Int64) async {
        guard !isTransferInFlight(downloadId) else { return }
        startingIds.insert(downloadId)
        defer { startingIds.remove(downloadId) }
        do {
            guard let download = try await repository.getDownloadById(id: downloadId) else {
                print("DownloadService: enqueued download record not found for id \(downloadId)")
                return
            }
            createFreshTask(downloadId: downloadId, url: download.fileUrl, apiKey: apiKeyProvider())
        } catch {
            print("DownloadService: failed to fetch enqueued download record: \(error)")
        }
    }

    /// Restarts a failed download from byte 0 with the persisted URL and the API key current at
    /// retry time. `Pending` is written before the task resumes so that write can never land on
    /// top of a status reported by the transfer. Returns whether a new task was started.
    @discardableResult
    func retryDownload(downloadId: Int64) async -> Bool {
        guard !isTransferInFlight(downloadId) else { return false }
        startingIds.insert(downloadId)
        defer { startingIds.remove(downloadId) }
        do {
            guard let download = try await repository.getDownloadById(id: downloadId) else {
                print("DownloadService: retry download record not found for id \(downloadId)")
                return false
            }
            // Checked before the Pending write so an unusable URL leaves the record Failed, not stuck Pending.
            guard let request = makeRequest(url: download.fileUrl, apiKey: apiKeyProvider()) else {
                print("DownloadService: retry has invalid URL for id \(downloadId)")
                return false
            }
            try await repository.updateStatus(id: downloadId, status: .pending, errorMessage: nil)
            // Resume data belongs to the old request and would replay its Authorization header.
            resumeDataStore.removeValue(forKey: downloadId)
            launchTask(taskFactory.makeTask(request: request, downloadId: downloadId), downloadId: downloadId)
            return true
        } catch {
            print("DownloadService: failed to prepare retry for id \(downloadId): \(error)")
            return false
        }
    }

    /// Stores resume data keyed by download ID for pause/resume
    private var resumeDataStore: [Int64: Data] = [:]

    func cancelDownload(downloadId: Int64) {
        if let task = activeTasks[downloadId] {
            task.cancelProducingResumeData { [weak self] data in
                Task { @MainActor [weak self] in
                    // Arriving after a retry already started, this data would outlive that fresh request.
                    guard let self, let data, !self.isTransferInFlight(downloadId) else { return }
                    self.resumeDataStore[downloadId] = data
                }
            }
        }
        activeTasks.removeValue(forKey: downloadId)
        completions.removeValue(forKey: downloadId)?.task.cancel()
    }

    func resumeDownload(downloadId: Int64, url: String, apiKey: String?) {
        guard !isTransferInFlight(downloadId) else { return }
        if let resumeData = resumeDataStore.removeValue(forKey: downloadId) {
            launchTask(taskFactory.makeTask(resumeData: resumeData, downloadId: downloadId), downloadId: downloadId)
        } else {
            createFreshTask(downloadId: downloadId, url: url, apiKey: apiKey)
        }
    }

    /// A callback from a task that is no longer the one tracked for its ID (cancelled, or
    /// replaced by a retry) must not touch the newer transfer's state.
    private func isStaleCallback(downloadId: Int64, taskIdentifier: Int) -> Bool {
        if startingIds.contains(downloadId) || completions[downloadId] != nil { return true }
        guard let current = activeTasks[downloadId] else { return false }
        return current.taskIdentifier != taskIdentifier
    }

    func handleProgress(downloadId: Int64, taskIdentifier: Int, written: Int64, total: Int64) {
        guard !isStaleCallback(downloadId: downloadId, taskIdentifier: taskIdentifier) else { return }
        Task {
            do {
                try await repository.updateProgress(id: downloadId, downloadedBytes: written)
            } catch {
                print("DownloadService: progress update failed: \(error)")
            }
            do {
                try await repository.updateStatus(
                    id: downloadId, status: .downloading, errorMessage: nil
                )
            } catch {
                print("DownloadService: status update failed: \(error)")
            }
        }
    }

    func handleComplete(downloadId: Int64, taskIdentifier: Int, location: URL?, error: Error?) {
        if isStaleCallback(downloadId: downloadId, taskIdentifier: taskIdentifier) {
            if let location { try? FileManager.default.removeItem(at: location) }
            return
        }
        activeTasks.removeValue(forKey: downloadId)
        if let error {
            Task {
                do {
                    try await repository.updateStatus(
                        id: downloadId, status: .failed, errorMessage: error.localizedDescription
                    )
                } catch {
                    print("DownloadService: status update failed: \(error)")
                }
            }
            return
        }
        guard let location else { return }
        let token = UUID()
        let task = Task {
            await moveCompletedDownload(downloadId: downloadId, location: location)
            if completions[downloadId]?.token == token {
                completions.removeValue(forKey: downloadId)
            }
        }
        completions[downloadId] = (token, task)
    }

    private func moveCompletedDownload(downloadId: Int64, location: URL) async {
        let download: ModelDownload
        do {
            guard let fetched = try await repository.getDownloadById(id: downloadId) else {
                print("DownloadService: download record not found for id \(downloadId)")
                try? FileManager.default.removeItem(at: location)
                return
            }
            download = fetched
        } catch {
            print("DownloadService: failed to fetch download record: \(error)")
            try? FileManager.default.removeItem(at: location)
            return
        }
        guard !Task.isCancelled else {
            try? FileManager.default.removeItem(at: location)
            return
        }
        let destFile = prepareDestination(for: download)
        do {
            try await repository.updateDestinationPath(id: downloadId, path: destFile.path)
        } catch {
            print("DownloadService: failed to update destination path: \(error)")
        }
        let actualSha256: String?
        do {
            actualSha256 = try await computeSha256(of: location, expectedSha256: download.expectedSha256)
            try Task.checkCancellation()
        } catch {
            // Only cancellation throws here: a cancelled attempt must not reach the verifier, which
            // would record a status for a transfer the user stopped or has since restarted.
            try? FileManager.default.removeItem(at: location)
            return
        }
        // The shared use case writes the final status; the downloaded file replaces an existing
        // destination only after it passes, so a rejected download never displaces a good file.
        let accepted: Bool
        do {
            accepted = try await hashVerifier.verify(downloadId: downloadId, actualSha256: actualSha256)
        } catch {
            try? FileManager.default.removeItem(at: location)
            guard !Task.isCancelled else { return }
            await markFailed(downloadId: downloadId, message: "Failed to verify file: \(error.localizedDescription)")
            return
        }
        guard accepted else {
            try? FileManager.default.removeItem(at: location)
            return
        }
        // Cancelled while verifying: a newer attempt may already own the destination.
        guard !Task.isCancelled else {
            try? FileManager.default.removeItem(at: location)
            return
        }
        do {
            try replaceItem(at: destFile, with: location)
        } catch {
            try? FileManager.default.removeItem(at: location)
            await markFailed(downloadId: downloadId, message: "Failed to move file: \(error.localizedDescription)")
        }
    }

    private func prepareDestination(for download: ModelDownload) -> URL {
        let documentsDir = FileManager.default.urls(
            for: .documentDirectory, in: .userDomainMask
        ).first!
        let typeDir = documentsDir
            .appendingPathComponent("Downloads")
            .appendingPathComponent(download.modelType)
        do {
            try FileManager.default.createDirectory(
                at: typeDir, withIntermediateDirectories: true
            )
        } catch {
            print("DownloadService: failed to create directory: \(error)")
        }
        return typeDir.appendingPathComponent(download.fileName)
    }

    private func replaceItem(at destination: URL, with source: URL) throws {
        if FileManager.default.fileExists(atPath: destination.path) {
            _ = try FileManager.default.replaceItemAt(destination, withItemAt: source)
        } else {
            try FileManager.default.moveItem(at: source, to: destination)
        }
    }

    private func markFailed(downloadId: Int64, message: String) async {
        do {
            try await repository.updateStatus(id: downloadId, status: .failed, errorMessage: message)
        } catch {
            print("DownloadService: failed to update failed status: \(error)")
        }
    }

    /// Returns nil when CivitAI published no hash or the file could not be read; the shared
    /// use case treats nil as unverified rather than as a mismatch. Throws only on cancellation.
    private func computeSha256(of filePath: URL, expectedSha256: String?) async throws -> String? {
        guard let expectedSha256,
              !expectedSha256.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty else { return nil }
        return try await hashWorker.sha256(of: filePath)
    }
}

/// Mirrors Android's `"HTTP <code>"` failure message so a 401/403/etc. response is
/// surfaced as a failed download instead of silently completing with a hash mismatch.
private struct DownloadHTTPError: LocalizedError {
    let statusCode: Int
    var errorDescription: String? { "HTTP \(statusCode)" }
}

// MARK: - URLSession Delegate

private class DownloadSessionDelegate: NSObject, URLSessionDownloadDelegate {
    weak var service: DownloadService?

    func urlSession(
        _ session: URLSession,
        downloadTask: URLSessionDownloadTask,
        didFinishDownloadingTo location: URL
    ) {
        guard let downloadId = downloadTask.taskDescription.flatMap(Int64.init) else { return }
        let taskIdentifier = downloadTask.taskIdentifier

        if let httpResponse = downloadTask.response as? HTTPURLResponse,
           !(200...299).contains(httpResponse.statusCode) {
            Task { @MainActor [weak service] in
                service?.handleComplete(
                    downloadId: downloadId,
                    taskIdentifier: taskIdentifier,
                    location: nil,
                    error: DownloadHTTPError(statusCode: httpResponse.statusCode)
                )
            }
            return
        }

        // Copy to temp before dispatching to main actor (location is only valid in this callback)
        let tempDir = FileManager.default.temporaryDirectory
        let tempFile = tempDir.appendingPathComponent(UUID().uuidString)
        do {
            try FileManager.default.copyItem(at: location, to: tempFile)
        } catch {
            print("DownloadService: failed to copy downloaded file to temp: \(error)")
            Task { @MainActor [weak service] in
                service?.handleComplete(
                    downloadId: downloadId, taskIdentifier: taskIdentifier, location: nil, error: error
                )
            }
            return
        }

        Task { @MainActor [weak service] in
            service?.handleComplete(
                downloadId: downloadId, taskIdentifier: taskIdentifier, location: tempFile, error: nil
            )
        }
    }

    func urlSession(
        _ session: URLSession,
        downloadTask: URLSessionDownloadTask,
        didWriteData bytesWritten: Int64,
        totalBytesWritten: Int64,
        totalBytesExpectedToWrite: Int64
    ) {
        guard let downloadId = downloadTask.taskDescription.flatMap(Int64.init) else { return }
        let taskIdentifier = downloadTask.taskIdentifier
        Task { @MainActor [weak service] in
            service?.handleProgress(
                downloadId: downloadId,
                taskIdentifier: taskIdentifier,
                written: totalBytesWritten,
                total: totalBytesExpectedToWrite
            )
        }
    }

    func urlSession(
        _ session: URLSession,
        task: URLSessionTask,
        didCompleteWithError error: Error?
    ) {
        guard let error else { return }
        guard let downloadId = task.taskDescription.flatMap(Int64.init) else { return }
        let taskIdentifier = task.taskIdentifier
        Task { @MainActor [weak service] in
            service?.handleComplete(
                downloadId: downloadId, taskIdentifier: taskIdentifier, location: nil, error: error
            )
        }
    }
}
