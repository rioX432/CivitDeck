import Foundation
import Shared

@MainActor
final class ShareHashtagViewModel: ObservableObject {
    @Published var hashtags: [ShareHashtag] = []

    private let observeUseCase = KoinHelper.shared.getObserveShareHashtagsUseCase()
    private let addUseCase = KoinHelper.shared.getAddShareHashtagUseCase()
    private let removeUseCase = KoinHelper.shared.getRemoveShareHashtagUseCase()
    private let toggleUseCase = KoinHelper.shared.getToggleShareHashtagUseCase()

    func startObserving() async {
        for await list in observeUseCase.invoke() {
            hashtags = list.compactMap { $0 as? ShareHashtag }
        }
    }

    func toggle(tag: String, isEnabled: Bool) {
        Task {
            try? await toggleUseCase.invoke(tag: tag, isEnabled: isEnabled)
        }
    }

    func add(tag: String) {
        Task {
            try? await addUseCase.invoke(tag: tag)
        }
    }

    func remove(tag: String) {
        Task {
            try? await removeUseCase.invoke(tag: tag)
        }
    }
}
