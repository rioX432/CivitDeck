@testable import CivitDeck
import CryptoKit
import Shared
import XCTest

final class DownloadHashWorkerTests: XCTestCase {
    private var tempDir: URL!

    override func setUpWithError() throws {
        try super.setUpWithError()
        tempDir = FileManager.default.temporaryDirectory
            .appendingPathComponent("DownloadHashWorkerTests-\(UUID().uuidString)")
        try FileManager.default.createDirectory(at: tempDir, withIntermediateDirectories: true)
    }

    override func tearDownWithError() throws {
        try? FileManager.default.removeItem(at: tempDir)
        try super.tearDownWithError()
    }

    // MARK: - Worker

    func testKnownDigestsForEmptyAndSmallFiles() async throws {
        let worker = DownloadHashWorker()

        let empty = try await worker.sha256(of: writeFile(Data()))
        let abc = try await worker.sha256(of: writeFile(Data("abc".utf8)))

        XCTAssertEqual(empty, "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855")
        XCTAssertEqual(abc, "ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad")
    }

    func testMultichunkFileMatchesOneShotDigestWithBoundedReads() async throws {
        let bytes = Data((0..<(DownloadHashWorker.chunkSize * 5 / 2 + 7)).map { UInt8(truncatingIfNeeded: $0 &* 31) })
        let file = try writeFile(bytes)
        let recorder = ReadRecorder()
        let worker = DownloadHashWorker { url in
            RecordingReader(inner: try FileHandleChunkReader(url: url), recorder: recorder)
        }

        let digest = try await worker.sha256(of: file)

        XCTAssertEqual(digest, hex(SHA256.hash(data: bytes)))
        // Three data chunks plus the empty end-of-file read.
        XCTAssertEqual(recorder.requestedCounts, Array(repeating: DownloadHashWorker.chunkSize, count: 4))
        XCTAssertEqual(DownloadHashWorker.chunkSize, 1_048_576)
        XCTAssertEqual(recorder.closeCount, 1)
    }

    @MainActor
    func testHashingDoesNotRunOnMainThread() async throws {
        let file = try writeFile(Data(repeating: 0xAB, count: DownloadHashWorker.chunkSize + 1))
        let recorder = ReadRecorder()
        let worker = DownloadHashWorker { url in
            RecordingReader(inner: try FileHandleChunkReader(url: url), recorder: recorder)
        }

        _ = try await worker.sha256(of: file)

        XCTAssertFalse(recorder.mainThreadFlags.isEmpty)
        XCTAssertFalse(recorder.mainThreadFlags.contains(true))
    }

    func testReadErrorReturnsNilAndClosesReader() async throws {
        let reader = ScriptedReader(steps: [.chunk(Data("partial".utf8)), .fail])
        let worker = DownloadHashWorker { _ in reader }

        let digest = try await worker.sha256(of: tempDir.appendingPathComponent("unused"))

        XCTAssertNil(digest)
        XCTAssertEqual(reader.readCount, 2)
        XCTAssertEqual(reader.closeCount, 1)
    }

    func testMissingFileReturnsNil() async throws {
        let digest = try await DownloadHashWorker().sha256(of: tempDir.appendingPathComponent("missing"))

        XCTAssertNil(digest)
    }

    func testCancellationThrowsInsteadOfReturningNilAndStopsReading() async throws {
        let reader = ScriptedReader(steps: [
            .cancelCurrentTaskThenChunk(Data("first".utf8)),
            .chunk(Data("second".utf8)),
        ])
        let worker = DownloadHashWorker { _ in reader }

        do {
            _ = try await worker.sha256(of: tempDir.appendingPathComponent("unused"))
            XCTFail("Expected CancellationError")
        } catch {
            XCTAssertTrue(error is CancellationError)
        }
        XCTAssertEqual(reader.readCount, 1)
        XCTAssertEqual(reader.closeCount, 1)
    }

    // MARK: - Service completion

    @MainActor
    func testMismatchIsRejectedAndPreservesExistingDestination() async throws {
        let env = ServiceEnvironment(expectedSha256: sha("good bytes"))
        defer { env.cleanUp() }
        try env.writeDestination(Data("good bytes".utf8))
        let location = try writeFile(Data("bad bytes".utf8))

        env.service.handleComplete(downloadId: env.downloadId, taskIdentifier: 1, location: location, error: nil)
        try await waitUntilRemoved(location)

        XCTAssertEqual(env.verifier.calls.map(\.actualSha256), [sha("bad bytes")])
        XCTAssertEqual(try Data(contentsOf: env.destination), Data("good bytes".utf8))
    }

    @MainActor
    func testMatchingDigestReplacesDestination() async throws {
        let env = ServiceEnvironment(expectedSha256: sha("new bytes"))
        defer { env.cleanUp() }
        try env.writeDestination(Data("old bytes".utf8))
        let location = try writeFile(Data("new bytes".utf8))

        env.service.handleComplete(downloadId: env.downloadId, taskIdentifier: 1, location: location, error: nil)
        try await waitUntilRemoved(location)

        XCTAssertEqual(env.verifier.calls.map(\.actualSha256), [sha("new bytes")])
        XCTAssertEqual(try Data(contentsOf: env.destination), Data("new bytes".utf8))
    }

    @MainActor
    func testMissingExpectedHashOrUnreadableFileCompletesUnverified() async throws {
        let cases: [(name: String, expected: String?, worker: DownloadHashWorker)] = [
            ("nil expected", nil, DownloadHashWorker()),
            ("blank expected", "  ", DownloadHashWorker()),
            ("read failure", sha("bytes"), DownloadHashWorker { _ in ScriptedReader(steps: [.fail]) }),
        ]
        for testCase in cases {
            let env = ServiceEnvironment(expectedSha256: testCase.expected, worker: testCase.worker)
            defer { env.cleanUp() }
            let location = try writeFile(Data("bytes".utf8))

            env.service.handleComplete(downloadId: env.downloadId, taskIdentifier: 1, location: location, error: nil)
            try await waitUntilRemoved(location)

            XCTAssertEqual(env.verifier.calls.count, 1, testCase.name)
            XCTAssertNil(env.verifier.calls.first?.actualSha256 ?? nil, testCase.name)
            XCTAssertEqual(try? Data(contentsOf: env.destination), Data("bytes".utf8), testCase.name)
        }
    }

    @MainActor
    func testCancelDuringHashingSkipsVerifierAndReplacementAndDedupesStarts() async throws {
        let gate = ReadGate()
        let worker = DownloadHashWorker { _ in GatedReader(gate: gate) }
        let env = ServiceEnvironment(expectedSha256: sha("good bytes"), worker: worker)
        defer { env.cleanUp() }
        try env.writeDestination(Data("good bytes".utf8))
        let location = try writeFile(Data("stale bytes".utf8))

        env.service.handleComplete(downloadId: env.downloadId, taskIdentifier: 1, location: location, error: nil)
        try await waitFor { gate.hasStarted }

        let duplicateStart = await env.service.retryDownload(downloadId: env.downloadId)
        XCTAssertFalse(duplicateStart)
        XCTAssertEqual(env.factory.taskCount, 0)

        env.service.cancelDownload(downloadId: env.downloadId)
        gate.release()
        try await waitUntilRemoved(location)

        XCTAssertTrue(env.verifier.calls.isEmpty)
        XCTAssertEqual(try Data(contentsOf: env.destination), Data("good bytes".utf8))
        let restarted = await env.service.retryDownload(downloadId: env.downloadId)
        XCTAssertTrue(restarted)
        XCTAssertEqual(env.factory.taskCount, 1)
    }

    // MARK: - Helpers

    private func writeFile(_ data: Data) throws -> URL {
        let url = tempDir.appendingPathComponent(UUID().uuidString)
        try data.write(to: url)
        return url
    }

    private func sha(_ text: String) -> String {
        hex(SHA256.hash(data: Data(text.utf8)))
    }

    private func hex(_ digest: SHA256.Digest) -> String {
        digest.map { String(format: "%02x", $0) }.joined()
    }

    private func waitUntilRemoved(_ location: URL) async throws {
        try await waitFor { !FileManager.default.fileExists(atPath: location.path) }
    }

    /// Polls for an end state rather than asserting on latency; the deadline only bounds a hang.
    private func waitFor(_ condition: () -> Bool) async throws {
        let deadline = Date().addingTimeInterval(10)
        while !condition() {
            guard Date() < deadline else {
                XCTFail("Timed out waiting for condition")
                return
            }
            try await Task.sleep(nanoseconds: 10_000_000)
        }
    }
}

// MARK: - Reader fakes

private final class ReadRecorder {
    private let lock = NSLock()
    private var counts: [Int] = []
    private var flags: [Bool] = []
    private var closes = 0

    var requestedCounts: [Int] { lock.withLock { counts } }
    var mainThreadFlags: [Bool] { lock.withLock { flags } }
    var closeCount: Int { lock.withLock { closes } }

    func recordRead(count: Int) {
        let isMain = Thread.isMainThread
        lock.withLock {
            counts.append(count)
            flags.append(isMain)
        }
    }

    func recordClose() {
        lock.withLock { closes += 1 }
    }
}

private final class RecordingReader: DownloadChunkReader {
    private let inner: DownloadChunkReader
    private let recorder: ReadRecorder

    init(inner: DownloadChunkReader, recorder: ReadRecorder) {
        self.inner = inner
        self.recorder = recorder
    }

    func read(upToCount count: Int) throws -> Data? {
        recorder.recordRead(count: count)
        return try inner.read(upToCount: count)
    }

    func close() throws {
        recorder.recordClose()
        try inner.close()
    }
}

private enum FakeReadError: Error {
    case failed
}

private final class ScriptedReader: DownloadChunkReader {
    enum Step {
        case chunk(Data)
        case cancelCurrentTaskThenChunk(Data)
        case fail
    }

    private var steps: [Step]
    private(set) var readCount = 0
    private(set) var closeCount = 0

    init(steps: [Step]) {
        self.steps = steps
    }

    func read(upToCount count: Int) throws -> Data? {
        readCount += 1
        guard !steps.isEmpty else { return Data() }
        switch steps.removeFirst() {
        case .chunk(let data):
            return data
        case .cancelCurrentTaskThenChunk(let data):
            withUnsafeCurrentTask { $0?.cancel() }
            return data
        case .fail:
            throw FakeReadError.failed
        }
    }

    func close() throws {
        closeCount += 1
    }
}

/// Holds the first read until released, so the test can act while hashing is in progress.
private final class ReadGate {
    private let lock = NSLock()
    private let semaphore = DispatchSemaphore(value: 0)
    private var started = false

    var hasStarted: Bool { lock.withLock { started } }

    func waitForRelease() {
        lock.withLock { started = true }
        semaphore.wait()
    }

    func release() {
        semaphore.signal()
    }
}

private final class GatedReader: DownloadChunkReader {
    private let gate: ReadGate
    private var delivered = false

    init(gate: ReadGate) {
        self.gate = gate
    }

    func read(upToCount count: Int) throws -> Data? {
        guard !delivered else { return Data() }
        gate.waitForRelease()
        delivered = true
        return Data("stale bytes".utf8)
    }

    func close() throws {}
}

// MARK: - Service fakes

@MainActor
private final class ServiceEnvironment {
    let downloadId: Int64 = 11
    let modelType = "HashTest-\(UUID().uuidString)"
    let verifier: FakeHashVerifier
    let factory = CountingTaskFactory()
    private(set) var service: DownloadService!

    var destination: URL {
        typeDir.appendingPathComponent("model.safetensors")
    }

    private var typeDir: URL {
        FileManager.default.urls(for: .documentDirectory, in: .userDomainMask)[0]
            .appendingPathComponent("Downloads")
            .appendingPathComponent(modelType)
    }

    init(expectedSha256: String?, worker: DownloadHashWorker = DownloadHashWorker()) {
        let record = ModelDownload(
            id: downloadId, modelId: 1, modelName: "Model", versionId: 2, versionName: "v1",
            fileId: 3, fileName: "model.safetensors", fileUrl: "https://civitai.com/api/download/models/42",
            fileSizeBytes: 1024, downloadedBytes: 1024, status: .downloading, modelType: modelType,
            destinationPath: nil, errorMessage: nil, expectedSha256: expectedSha256, hashVerified: nil,
            createdAt: 0, updatedAt: 0
        )
        verifier = FakeHashVerifier(expectedSha256: expectedSha256)
        service = DownloadService(
            repository: SingleRecordStore(record: record),
            apiKeyProvider: { nil },
            taskFactory: factory,
            hashVerifier: verifier,
            hashWorker: worker
        )
    }

    func writeDestination(_ data: Data) throws {
        try FileManager.default.createDirectory(at: typeDir, withIntermediateDirectories: true)
        try data.write(to: destination)
    }

    func cleanUp() {
        try? FileManager.default.removeItem(at: typeDir)
    }
}

/// Mirrors the shared use case's acceptance rule: only a computed digest that differs from a
/// published hash is rejected.
@MainActor
private final class FakeHashVerifier: DownloadHashVerifier {
    private(set) var calls: [(downloadId: Int64, actualSha256: String?)] = []
    private let expectedSha256: String?

    init(expectedSha256: String?) {
        self.expectedSha256 = expectedSha256
    }

    func verify(downloadId: Int64, actualSha256: String?) async throws -> Bool {
        calls.append((downloadId, actualSha256))
        guard let expectedSha256, !expectedSha256.trimmingCharacters(in: .whitespaces).isEmpty,
              let actualSha256 else { return true }
        return actualSha256.caseInsensitiveCompare(expectedSha256) == .orderedSame
    }
}

@MainActor
private final class SingleRecordStore: DownloadRecordStore {
    private let record: ModelDownload

    init(record: ModelDownload) {
        self.record = record
    }

    func getDownloadById(id: Int64) async throws -> ModelDownload? {
        id == record.id ? record : nil
    }

    func updateStatus(id: Int64, status: DownloadStatus, errorMessage: String?) async throws {}

    func updateProgress(id: Int64, downloadedBytes: Int64) async throws {}

    func updateDestinationPath(id: Int64, path: String) async throws {}
}

private final class NoopTask: DownloadTaskHandle {
    let taskIdentifier: Int

    init(taskIdentifier: Int) {
        self.taskIdentifier = taskIdentifier
    }

    func resume() {}

    func cancelProducingResumeData(_ completionHandler: @escaping @Sendable (Data?) -> Void) {
        completionHandler(nil)
    }
}

private final class CountingTaskFactory: DownloadTaskFactory {
    private(set) var taskCount = 0

    func makeTask(request: URLRequest, downloadId: Int64) -> DownloadTaskHandle {
        taskCount += 1
        return NoopTask(taskIdentifier: taskCount)
    }

    func makeTask(resumeData: Data, downloadId: Int64) -> DownloadTaskHandle {
        taskCount += 1
        return NoopTask(taskIdentifier: taskCount)
    }
}
