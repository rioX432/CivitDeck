import SwiftUI

/// Navigation destination for the two-model comparison screen. Referenced by the search,
/// collections, and collection-detail screens' navigation stacks.
struct CompareDestination: Hashable {
    let leftModelId: Int64
    let rightModelId: Int64
}

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
