import SwiftUI

@MainActor
final class ComparisonState: ObservableObject {
    @Published var selectedModelId: Int64?
    @Published var selectedModelName: String?

    var isActive: Bool { selectedModelId != nil }

    func startCompare(modelId: Int64, name: String) {
        selectedModelId = modelId
        selectedModelName = name
    }

    func cancel() {
        selectedModelId = nil
        selectedModelName = nil
    }
}
