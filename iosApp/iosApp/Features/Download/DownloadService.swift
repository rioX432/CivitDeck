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
        Task {
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
            try? FileManager.default.removeItem(at: destFile)
            do {
                try FileManager.default.moveItem(at: location, to: destFile)
                do {
                    try await repository.updateDestinationPath(id: downloadId, path: destFile.path)
                } catch {
                    print("DownloadService: failed to update destination path: \(error)")
                }
                await verifyHash(downloadId: downloadId, filePath: destFile, expectedSha256: download.expectedSha256)
                do {
                    try await repository.updateStatus(
                        id: downloadId, status: .completed, errorMessage: nil
                    )
                } catch {
                    print("DownloadService: failed to update completed status: \(error)")
                }
            } catch {
                do {
                    try await repository.updateStatus(
                        id: downloadId, status: .failed,
                        errorMessage: "Failed to move file: \(error.localizedDescription)"
                    )
                } catch {
                    print("DownloadService: failed to update failed status: \(error)")
                }
            }
        }
    }
    private func verifyHash(downloadId: Int64, filePath: URL, expectedSha256: String?) async {
        guard let expectedHash = expectedSha256, !expectedHash.isEmpty else { return }
        let fileData: Data
        do {
            fileData = try Data(contentsOf: filePath)
        } catch {
            print("DownloadService: failed to read file for hash verification: \(error)")
            return
        }
        let digest = SHA256.hash(data: fileData)
        let actualHash = digest.map { String(format: "%02x", $0) }.joined()
        let matched = actualHash.lowercased() == expectedHash.lowercased()
        do {
            try await repository.updateHashVerified(id: downloadId, verified: matched)
        } catch {
            print("DownloadService: failed to update hash verification: \(error)")
        }
    }
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
