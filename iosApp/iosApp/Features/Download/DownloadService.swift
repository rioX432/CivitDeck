import CryptoKit
import Foundation
import Shared

@MainActor
final class DownloadService: NSObject, ObservableObject {
    static let shared = DownloadService()

    private var activeTasks: [Int64: URLSessionDownloadTask] = [:]
    private let downloadDelegate = DownloadSessionDelegate()
    private lazy var session: URLSession = {
        let config = URLSessionConfiguration.background(
            withIdentifier: "com.riox432.civitdeck.download"
        )
        config.isDiscretionary = false
        config.sessionSendsLaunchEvents = true
        return URLSession(configuration: config, delegate: downloadDelegate, delegateQueue: nil)
    }()

    private lazy var repository: ModelDownloadRepository = {
        KoinHelper.shared.getModelDownloadRepository()
    }()

    private lazy var verifyDownloadHash: VerifyDownloadHashUseCase = {
        KoinHelper.shared.getVerifyDownloadHashUseCase()
    }()

    override private init() {
        super.init()
        downloadDelegate.service = self
    }

    func startDownload(downloadId: Int64, url: String, apiKey: String?) {
        guard let downloadUrl = URL(string: url) else { return }
        var request = URLRequest(url: downloadUrl)
        if let apiKey, !apiKey.isEmpty {
            request.setValue("Bearer \(apiKey)", forHTTPHeaderField: "Authorization")
        }
        let task = session.downloadTask(with: request)
        task.taskDescription = String(downloadId)
        activeTasks[downloadId] = task
        task.resume()
    }

    /// Starts the transfer for a download that was just enqueued (inserted into the DB
    /// as `Pending`) but has no in-flight `URLSessionDownloadTask` yet.
    func handleEnqueuedDownload(downloadId: Int64) async {
        do {
            guard let download = try await repository.getDownloadById(id: downloadId) else {
                print("DownloadService: enqueued download record not found for id \(downloadId)")
                return
            }
            let apiKey = KoinHelper.shared.getApiKeyProvider().apiKey
            startDownload(downloadId: downloadId, url: download.fileUrl, apiKey: apiKey)
        } catch {
            print("DownloadService: failed to fetch enqueued download record: \(error)")
        }
    }

    /// Stores resume data keyed by download ID for pause/resume
    private var resumeDataStore: [Int64: Data] = [:]

    func cancelDownload(downloadId: Int64) {
        if let task = activeTasks[downloadId] {
            task.cancel(byProducingResumeData: { [weak self] data in
                Task { @MainActor [weak self] in
                    if let data {
                        self?.resumeDataStore[downloadId] = data
                    }
                }
            })
        }
        activeTasks.removeValue(forKey: downloadId)
    }

    func resumeDownload(downloadId: Int64, url: String, apiKey: String?) {
        if let resumeData = resumeDataStore.removeValue(forKey: downloadId) {
            let task = session.downloadTask(withResumeData: resumeData)
            task.taskDescription = String(downloadId)
            activeTasks[downloadId] = task
            task.resume()
        } else {
            startDownload(downloadId: downloadId, url: url, apiKey: apiKey)
        }
    }

    fileprivate func handleProgress(downloadId: Int64, written: Int64, total: Int64) {
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

    fileprivate func handleComplete(downloadId: Int64, location: URL?, error: Error?) {
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
        Task { await moveCompletedDownload(downloadId: downloadId, location: location) }
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
        let destFile = typeDir.appendingPathComponent(download.fileName)
        do {
            try await repository.updateDestinationPath(id: downloadId, path: destFile.path)
        } catch {
            print("DownloadService: failed to update destination path: \(error)")
        }
        // The shared use case writes the final status; the downloaded file replaces an existing
        // destination only after it passes, so a rejected download never displaces a good file.
        let accepted: Bool
        do {
            accepted = try await verifyDownloadHash.invoke(
                downloadId: downloadId,
                actualSha256: computeSha256(of: location, expectedSha256: download.expectedSha256)
            ).boolValue
        } catch {
            try? FileManager.default.removeItem(at: location)
            await markFailed(downloadId: downloadId, message: "Failed to verify file: \(error.localizedDescription)")
            return
        }
        guard accepted else {
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
    /// use case treats nil as unverified rather than as a mismatch.
    private func computeSha256(of filePath: URL, expectedSha256: String?) -> String? {
        guard let expectedSha256, !expectedSha256.isEmpty else { return nil }
        let fileData: Data
        do {
            fileData = try Data(contentsOf: filePath)
        } catch {
            print("DownloadService: failed to read file for hash verification: \(error)")
            return nil
        }
        return SHA256.hash(data: fileData).map { String(format: "%02x", $0) }.joined()
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

        if let httpResponse = downloadTask.response as? HTTPURLResponse,
           !(200...299).contains(httpResponse.statusCode) {
            Task { @MainActor [weak service] in
                service?.handleComplete(
                    downloadId: downloadId,
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
                service?.handleComplete(downloadId: downloadId, location: nil, error: error)
            }
            return
        }

        Task { @MainActor [weak service] in
            service?.handleComplete(downloadId: downloadId, location: tempFile, error: nil)
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
        Task { @MainActor [weak service] in
            service?.handleProgress(
                downloadId: downloadId,
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
        Task { @MainActor [weak service] in
            service?.handleComplete(downloadId: downloadId, location: nil, error: error)
        }
    }
}
