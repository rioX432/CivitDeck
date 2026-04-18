import Foundation
import Shared

// MARK: - SettingsViewModelStore

/// Holds all Settings Kotlin ViewModels with proper lifecycle management.
/// Views consume state via SKIE `Observing` instead of @Published wrappers.
@MainActor
final class SettingsViewModelStore: ObservableObject {
    let auth: AuthSettingsViewModel
    let display: DisplaySettingsViewModel
    let contentFilter: ContentFilterSettingsViewModel
    let appBehavior: AppBehaviorSettingsViewModel
    let storage: StorageSettingsViewModel

    private let store = ViewModelStore()

    init() {
        auth = KoinHelper.shared.createAuthSettingsViewModel()
        display = KoinHelper.shared.createDisplaySettingsViewModel()
        contentFilter = KoinHelper.shared.createContentFilterSettingsViewModel()
        appBehavior = KoinHelper.shared.createAppBehaviorSettingsViewModel()
        storage = KoinHelper.shared.createStorageSettingsViewModel()

        store.put(key: "auth", viewModel: auth)
        store.put(key: "display", viewModel: display)
        store.put(key: "contentFilter", viewModel: contentFilter)
        store.put(key: "appBehavior", viewModel: appBehavior)
        store.put(key: "storage", viewModel: storage)
    }

    deinit { store.clear() }

    func replayGestureTutorial() {
        let tutorialVm = KoinHelper.shared.createGestureTutorialViewModel()
        tutorialVm.resetTutorial()
    }
}
