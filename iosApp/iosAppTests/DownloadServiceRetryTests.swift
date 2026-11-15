@testable import CivitDeck
import Shared
import XCTest

final class DownloadServiceRetryTests: XCTestCase {
    private let downloadId: Int64 = 7
    private let persistedUrl = "https://civitai.com/api/download/models/42"

    @MainActor
    func testRetryStartsOneTaskForRecordOnlyInPersistedFailedState() async {
        let env = TestEnvironment()
        env.store.records[downloadId] = failedRecord(url: persistedUrl)
        env.apiKey = "current-key"

        let started = await env.service.retryDownload(downloadId: downloadId)

        XCTAssertTrue(started)
        XCTAssertEqual(env.factory.tasks.count, 1)
        let task = env.factory.tasks[0]
        XCTAssertEqual(task.request?.url?.absoluteString, persistedUrl)
        XCTAssertEqual(task.request?.value(forHTTPHeaderField: "Authorization"), "Bearer current-key")
        XCTAssertEqual(task.resumeCount, 1)
        XCTAssertEqual(env.factory.resumeDataTaskCount, 0)
    }

    @MainActor
    func testPendingWriteCompletesBeforeTaskResumes() async {
        let env = TestEnvironment()
        env.store.records[downloadId] = failedRecord(url: persistedUrl)

        await env.service.retryDownload(downloadId: downloadId)

        XCTAssertEqual(env.log.events, ["lookup", "status:pending", "resume"])
    }

    @MainActor
    func testRepeatedRapidRetryCreatesOneTask() async {
        let env = TestEnvironment()
        env.store.records[downloadId] = failedRecord(url: persistedUrl)

        async let first = env.service.retryDownload(downloadId: downloadId)
        async let second = env.service.retryDownload(downloadId: downloadId)
        async let third = env.service.retryDownload(downloadId: downloadId)
        let results = await [first, second, third]

        XCTAssertEqual(results.filter { $0 }.count, 1)
        XCTAssertEqual(env.factory.tasks.count, 1)
        XCTAssertEqual(env.factory.tasks.map(\.resumeCount), [1])
    }

    @MainActor
    func testEnqueueRacingRetryCreatesOneTask() async {
        let env = TestEnvironment()
        env.store.records[downloadId] = failedRecord(url: persistedUrl)

        async let enqueue: Void = env.service.handleEnqueuedDownload(downloadId: downloadId)
        async let retry = env.service.retryDownload(downloadId: downloadId)
        _ = await (enqueue, retry)

        XCTAssertEqual(env.factory.tasks.count, 1)
        XCTAssertEqual(env.factory.tasks.map(\.resumeCount), [1])
    }

    @MainActor
    func testRetryWhileTaskActiveDoesNotStartAnother() async {
        let env = TestEnvironment()
        env.store.records[downloadId] = failedRecord(url: persistedUrl)
        await env.service.handleEnqueuedDownload(downloadId: downloadId)

        let started = await env.service.retryDownload(downloadId: downloadId)

        XCTAssertFalse(started)
        XCTAssertEqual(env.factory.tasks.count, 1)
    }

    @MainActor
    func testRetryUsesKeyAtRetryTimeAndDiscardsObsoleteResumeData() async {
        let env = TestEnvironment()
        env.store.records[downloadId] = failedRecord(url: persistedUrl)
        env.apiKey = "old-key"
        await env.service.handleEnqueuedDownload(downloadId: downloadId)
        env.factory.tasks[0].resumeDataOnCancel = Data("stale-resume-data".utf8)
        env.service.cancelDownload(downloadId: downloadId)
        await drainMainActor()
        env.apiKey = "new-key"

        let started = await env.service.retryDownload(downloadId: downloadId)

        XCTAssertTrue(started)
        XCTAssertEqual(env.factory.tasks.count, 2)
        XCTAssertEqual(env.factory.resumeDataTaskCount, 0)
        XCTAssertEqual(env.factory.tasks[1].request?.value(forHTTPHeaderField: "Authorization"), "Bearer new-key")

        // The discarded resume data must not come back on a later resume of the same record.
        env.service.handleComplete(
            downloadId: downloadId, taskIdentifier: env.factory.tasks[1].taskIdentifier,
            location: nil, error: URLError(.networkConnectionLost)
        )
        env.service.resumeDownload(downloadId: downloadId, url: persistedUrl, apiKey: "new-key")
        XCTAssertEqual(env.factory.resumeDataTaskCount, 0)
        XCTAssertEqual(env.factory.tasks.count, 3)
    }

    @MainActor
    func testPausedResumeStillUsesResumeData() async {
        let env = TestEnvironment()
        env.store.records[downloadId] = failedRecord(url: persistedUrl)
        await env.service.handleEnqueuedDownload(downloadId: downloadId)
        env.factory.tasks[0].resumeDataOnCancel = Data("paused-resume-data".utf8)
        env.service.cancelDownload(downloadId: downloadId)
        await drainMainActor()

        env.service.resumeDownload(downloadId: downloadId, url: persistedUrl, apiKey: nil)

        XCTAssertEqual(env.factory.tasks[0].cancelCount, 1)
        XCTAssertEqual(env.factory.resumeDataTaskCount, 1)
        XCTAssertEqual(env.factory.tasks.count, 2)
    }

    @MainActor
    func testStaleCallbackDoesNotRemoveNewerTask() async {
        let env = TestEnvironment()
        env.store.records[downloadId] = failedRecord(url: persistedUrl)
        await env.service.retryDownload(downloadId: downloadId)
        let oldTask = env.factory.tasks[0]
        env.service.handleComplete(
            downloadId: downloadId, taskIdentifier: oldTask.taskIdentifier,
            location: nil, error: URLError(.timedOut)
        )
        await env.service.retryDownload(downloadId: downloadId)

        env.service.handleComplete(
            downloadId: downloadId, taskIdentifier: oldTask.taskIdentifier,
            location: nil, error: URLError(.timedOut)
        )
        let startedAgain = await env.service.retryDownload(downloadId: downloadId)

        XCTAssertFalse(startedAgain)
        XCTAssertEqual(env.factory.tasks.count, 2)
    }

    @MainActor
    func testFailedPreparationReleasesReservation() async {
        typealias Step = @MainActor (TestEnvironment) -> Void
        let cases: [(name: String, arrange: Step, repair: Step)] = [
            ("missing record", { _ in }, { env in
                env.store.records[self.downloadId] = self.failedRecord(url: self.persistedUrl)
            }),
            ("lookup error", { env in
                env.store.records[self.downloadId] = self.failedRecord(url: self.persistedUrl)
                env.store.lookupError = FakeError.lookup
            }, { env in env.store.lookupError = nil }),
            ("pending write error", { env in
                env.store.records[self.downloadId] = self.failedRecord(url: self.persistedUrl)
                env.store.statusError = FakeError.write
            }, { env in env.store.statusError = nil }),
        ]
        for testCase in cases {
            let env = TestEnvironment()
            testCase.arrange(env)

            let firstAttempt = await env.service.retryDownload(downloadId: downloadId)
            XCTAssertFalse(firstAttempt, testCase.name)
            XCTAssertTrue(env.factory.tasks.isEmpty, testCase.name)

            testCase.repair(env)
            let secondAttempt = await env.service.retryDownload(downloadId: downloadId)
            XCTAssertTrue(secondAttempt, testCase.name)
            XCTAssertEqual(env.factory.tasks.count, 1, testCase.name)
        }
    }

    @MainActor
    func testInvalidUrlDoesNotWritePendingOrStartTask() async {
        let env = TestEnvironment()
        env.store.records[downloadId] = failedRecord(url: "")

        let started = await env.service.retryDownload(downloadId: downloadId)

        XCTAssertFalse(started)
        XCTAssertTrue(env.factory.tasks.isEmpty)
        XCTAssertFalse(env.log.events.contains("status:pending"))
    }

    // MARK: - Helpers

    @MainActor
    private func drainMainActor() async {
        for _ in 0..<10 { await Task.yield() }
    }

    private func failedRecord(url: String) -> ModelDownload {
        ModelDownload(
            id: downloadId, modelId: 1, modelName: "Model", versionId: 2, versionName: "v1",
            fileId: 3, fileName: "model.safetensors", fileUrl: url, fileSizeBytes: 1024,
            downloadedBytes: 512, status: .failed, modelType: "LORA", destinationPath: nil,
            errorMessage: "HTTP 401", expectedSha256: nil, hashVerified: nil, createdAt: 0, updatedAt: 0
        )
    }
}

// MARK: - Fakes

private enum FakeError: Error {
    case lookup
    case write
}

private final class EventLog {
    var events: [String] = []
}

@MainActor
private final class TestEnvironment {
    let log = EventLog()
    let store: FakeDownloadRecordStore
    let factory: FakeDownloadTaskFactory
    var apiKey: String?
    private(set) var service: DownloadService!

    init() {
        store = FakeDownloadRecordStore(log: log)
        factory = FakeDownloadTaskFactory(log: log)
        service = DownloadService(
            repository: store,
            apiKeyProvider: { [unowned self] in self.apiKey },
            taskFactory: factory
        )
    }
}

/// Suspends on every call so concurrent callers interleave at the same points as the live repository.
@MainActor
private final class FakeDownloadRecordStore: DownloadRecordStore {
    var records: [Int64: ModelDownload] = [:]
    var lookupError: Error?
    var statusError: Error?
    private let log: EventLog

    init(log: EventLog) {
        self.log = log
    }

    func getDownloadById(id: Int64) async throws -> ModelDownload? {
        await Task.yield()
        log.events.append("lookup")
        if let lookupError { throw lookupError }
        return records[id]
    }

    func updateStatus(id: Int64, status: DownloadStatus, errorMessage: String?) async throws {
        await Task.yield()
        if let statusError { throw statusError }
        log.events.append(status == .pending ? "status:pending" : "status:other")
    }

    func updateProgress(id: Int64, downloadedBytes: Int64) async throws {}

    func updateDestinationPath(id: Int64, path: String) async throws {}
}

private final class FakeDownloadTask: DownloadTaskHandle {
    let taskIdentifier: Int
    let request: URLRequest?
    var resumeDataOnCancel: Data?
    private(set) var resumeCount = 0
    private(set) var cancelCount = 0
    private let log: EventLog

    init(taskIdentifier: Int, request: URLRequest?, log: EventLog) {
        self.taskIdentifier = taskIdentifier
        self.request = request
        self.log = log
    }

    func resume() {
        resumeCount += 1
        log.events.append("resume")
    }

    func cancelProducingResumeData(_ completionHandler: @escaping @Sendable (Data?) -> Void) {
        cancelCount += 1
        completionHandler(resumeDataOnCancel)
    }
}

private final class FakeDownloadTaskFactory: DownloadTaskFactory {
    private(set) var tasks: [FakeDownloadTask] = []
    private(set) var resumeDataTaskCount = 0
    private let log: EventLog

    init(log: EventLog) {
        self.log = log
    }

    func makeTask(request: URLRequest, downloadId: Int64) -> DownloadTaskHandle {
        let task = FakeDownloadTask(taskIdentifier: tasks.count + 1, request: request, log: log)
        tasks.append(task)
        return task
    }

    func makeTask(resumeData: Data, downloadId: Int64) -> DownloadTaskHandle {
        resumeDataTaskCount += 1
        let task = FakeDownloadTask(taskIdentifier: tasks.count + 1, request: nil, log: log)
        tasks.append(task)
        return task
    }
}
