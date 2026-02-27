import Foundation
import Shared

@MainActor
final class GestureTutorialViewModel: ObservableObject {
    @Published var shouldShowTutorial: Bool = false

    private let observeSeenTutorialVersion: ObserveSeenTutorialVersionUseCase
    private let setSeenTutorialVersion: SetSeenTutorialVersionUseCase

    static let currentTutorialVersion: Int32 = 1

    init() {
        self.observeSeenTutorialVersion = KoinHelper.shared.getObserveSeenTutorialVersionUseCase()
        self.setSeenTutorialVersion = KoinHelper.shared.getSetSeenTutorialVersionUseCase()
        observeVersion()
    }

    private func observeVersion() {
        Task {
            for await version in observeSeenTutorialVersion.invoke() {
                guard let intVersion = version as? Int32 else { continue }
                self.shouldShowTutorial = intVersion < Self.currentTutorialVersion
            }
        }
    }

    func dismissTutorial() {
        Task {
            try? await setSeenTutorialVersion.invoke(version: Self.currentTutorialVersion)
        }
    }
}
