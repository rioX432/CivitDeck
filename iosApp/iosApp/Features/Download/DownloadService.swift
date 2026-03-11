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

    func cancelDownload(downloadId: Int64) {
        activeTasks[downloadId]?.cancel()
        activeTasks.removeValue(forKey: downloadId)
    }

    fileprivate func handleProgress(downloadId: Int64, written: Int64, total: Int64) {
        Task {
            try? await repository.updateProgress(id: downloadId, downloadedBytes: written)
            try? await repository.updateStatus(
                id: downloadId, status: .downloading, errorMessage: nil
            )
        }
    }

    fileprivate func handleComplete(downloadId: Int64, location: URL?, error: Error?) {
        activeTasks.removeValue(forKey: downloadId)
        if let error {
            Task {
                try? await repository.updateStatus(
                    id: downloadId, status: .failed, errorMessage: error.localizedDescription
                )
            }
            return
        }
        guard let location else { return }
        Task {
            guard let download = try? await repository.getDownloadById(id: downloadId) else {
                try? FileManager.default.removeItem(at: location)
                return
            }
            let documentsDir = FileManager.default.urls(
                for: .documentDirectory, in: .userDomainMask
            ).first!
            let typeDir = documentsDir
                .appendingPathComponent("Downloads")
                .appendingPathComponent(download.modelType)
            try? FileManager.default.createDirectory(
                at: typeDir, withIntermediateDirectories: true
            )
            let destFile = typeDir.appendingPathComponent(download.fileName)
            try? FileManager.default.removeItem(at: destFile)
            do {
                try FileManager.default.moveItem(at: location, to: destFile)
                try? await repository.updateDestinationPath(id: downloadId, path: destFile.path)
                try? await repository.updateStatus(
                    id: downloadId, status: .completed, errorMessage: nil
                )
            } catch {
                try? await repository.updateStatus(
                    id: downloadId, status: .failed,
                    errorMessage: "Failed to move file: \(error.localizedDescription)"
                )
            }
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
        try? FileManager.default.copyItem(at: location, to: tempFile)

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
