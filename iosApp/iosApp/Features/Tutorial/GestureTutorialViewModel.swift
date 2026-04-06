import Foundation
import Shared

@MainActor
final class GestureTutorialViewModelOwner: ObservableObject {
    @Published var shouldShowTutorial: Bool = false

    private let vm: GestureTutorialViewModel
    private let store: ViewModelStore

    init() {
        store = ViewModelStore()
        vm = KoinHelper.shared.createGestureTutorialViewModel()
        store.put(key: "GestureTutorialViewModel", viewModel: vm)
    }

    deinit { store.clear() }

    func observeShouldShow() async {
        for await show in vm.shouldShowTutorial {
            guard let boolValue = show as? Bool else { continue }
            self.shouldShowTutorial = boolValue
        }
    }

    func dismissTutorial() {
        vm.dismissTutorial()
    }
}
