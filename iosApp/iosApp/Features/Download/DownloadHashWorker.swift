import CryptoKit
import Foundation

/// Sequential reads from a downloaded file; injectable so bounded-read, error and cancellation
/// behavior can be tested without real I/O.
protocol DownloadChunkReader: AnyObject {
    func read(upToCount count: Int) throws -> Data?
    func close() throws
}

final class FileHandleChunkReader: DownloadChunkReader {
    private let handle: FileHandle

    init(url: URL) throws {
        handle = try FileHandle(forReadingFrom: url)
    }

    func read(upToCount count: Int) throws -> Data? {
        try handle.read(upToCount: count)
    }

    func close() throws {
        try handle.close()
    }
}

/// Hashes downloaded files on its own executor so multi-gigabyte model files never block
/// MainActor, and reads them in fixed chunks so memory stays bounded regardless of file size.
actor DownloadHashWorker {
    static let chunkSize = 1_048_576

    typealias ReaderFactory = @Sendable (URL) throws -> DownloadChunkReader

    private let openReader: ReaderFactory

    init(openReader: @escaping ReaderFactory = { try FileHandleChunkReader(url: $0) }) {
        self.openReader = openReader
    }

    /// Returns the lowercase hex SHA-256, or nil when the file cannot be opened or read; the shared
    /// verifier treats nil as unverified. Throws `CancellationError` instead of returning nil so a
    /// cancelled attempt is never mistaken for an unreadable file and completed.
    func sha256(of url: URL) throws -> String? {
        let reader: DownloadChunkReader
        do {
            reader = try openReader(url)
        } catch {
            if error is CancellationError { throw error }
            try Task.checkCancellation()
            print("DownloadHashWorker: failed to open file for hash verification: \(error)")
            return nil
        }
        defer { try? reader.close() }
        var hasher = SHA256()
        while true {
            try Task.checkCancellation()
            // FileHandle can hand back autoreleased buffers; draining them per chunk keeps memory
            // at one chunk even though this loop never returns to a run loop.
            let outcome: ChunkOutcome = try autoreleasepool {
                let chunk: Data?
                do {
                    chunk = try reader.read(upToCount: Self.chunkSize)
                } catch {
                    if error is CancellationError { throw error }
                    try Task.checkCancellation()
                    print("DownloadHashWorker: failed to read file for hash verification: \(error)")
                    return .unreadable
                }
                // FileHandle reports end of file with empty data; nil is treated the same.
                guard let chunk, !chunk.isEmpty else { return .endOfFile }
                hasher.update(data: chunk)
                return .hashed
            }
            switch outcome {
            case .hashed:
                continue
            case .endOfFile:
                try Task.checkCancellation()
                return hasher.finalize().map { String(format: "%02x", $0) }.joined()
            case .unreadable:
                return nil
            }
        }
    }

    private enum ChunkOutcome {
        case hashed
        case endOfFile
        case unreadable
    }
}
