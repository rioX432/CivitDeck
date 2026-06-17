import Foundation
import Shared

/// Swift-side mirror of the Kotlin onboarding sealed step, so SwiftUI can switch
/// without depending on SKIE sealed-class ergonomics.
enum OnboardingStage: Equatable {
    case chooseMethod
    case scanning(results: [DiscoveredServer])
    case testing(hostname: String)
    case success(name: String, gpu: String?, vramMB: Int64)
    case failure(cause: ConnectionFailureCause, httpStatus: Int32?)

    static func == (lhs: OnboardingStage, rhs: OnboardingStage) -> Bool {
        switch (lhs, rhs) {
        case (.chooseMethod, .chooseMethod): return true
        case let (.scanning(l), .scanning(r)): return l.count == r.count
        case let (.testing(l), .testing(r)): return l == r
        case let (.success(ln, _, _), .success(rn, _, _)): return ln == rn
        case let (.failure(lc, _), .failure(rc, _)): return lc == rc
        default: return false
        }
    }
}

@MainActor
final class ConnectionOnboardingViewModelOwner: ObservableObject {
    let vm: ConnectionOnboardingViewModel
    private let store = ViewModelStore()

    @Published var stage: OnboardingStage = .chooseMethod
    @Published var lanScanSupported = false

    init() {
        vm = KoinHelper.shared.createConnectionOnboardingViewModel()
        store.put(key: "ConnectionOnboardingViewModel", viewModel: vm)
    }

    deinit { store.clear() }

    func observeUiState() async {
        for await state in vm.uiState {
            lanScanSupported = state.lanScanSupported
            stage = Self.mapStep(state.step)
        }
    }

    private static func mapStep(_ step: OnboardingStep) -> OnboardingStage {
        if let scanning = step as? OnboardingStepScanning {
            return .scanning(results: scanning.results as? [DiscoveredServer] ?? [])
        }
        if let testing = step as? OnboardingStepTesting {
            return .testing(hostname: testing.connection.hostname)
        }
        if let success = step as? OnboardingStepSuccess {
            return .success(
                name: success.connection.name,
                gpu: success.stats?.gpuName,
                vramMB: success.stats?.vramTotalMB ?? 0
            )
        }
        if let failure = step as? OnboardingStepFailure {
            return .failure(cause: failure.cause, httpStatus: failure.httpStatus?.int32Value)
        }
        return .chooseMethod
    }

    func chooseMethod() { vm.onChooseMethod() }
    func startScan() { vm.onStartScan() }
    func selectServer(_ server: DiscoveredServer) { vm.onSelectDiscoveredServer(server: server) }
    func qrScanned(_ raw: String) { vm.onQrScanned(raw: raw) }
    func retry() { vm.onRetry() }

    func manualSubmit(
        name: String,
        hostname: String,
        port: Int32,
        useHttps: Bool,
        acceptSelfSigned: Bool
    ) {
        vm.onManualSubmit(
            name: name,
            hostname: hostname,
            port: port,
            useHttps: useHttps,
            acceptSelfSigned: acceptSelfSigned
        )
    }
}
