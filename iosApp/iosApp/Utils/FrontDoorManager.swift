import Foundation
import Shared

/// Observes the persisted CivitAI front-door choice (civitai.com / civitai.red)
/// from the shared module and keeps `CivitAiUrls.webBaseUrl` in sync so that
/// web/share links (model page, QR, share) open on the chosen site.
///
/// Mirrors the `ThemeManager` observation pattern. Affects web links ONLY —
/// the API and download hosts are unaffected.
@MainActor
final class FrontDoorManager: ObservableObject {
    private let observeFrontDoorModeUseCase: ObserveFrontDoorModeUseCase

    init() {
        self.observeFrontDoorModeUseCase = KoinHelper.shared.getObserveFrontDoorModeUseCase()
    }

    func observeFrontDoor() async {
        for await mode in observeFrontDoorModeUseCase.invoke() {
            CivitAiUrls.webBaseUrl = mode.webHost
        }
    }
}
