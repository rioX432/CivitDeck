import SwiftUI
import Shared

struct SimilarModelsView: View {
    @StateObject private var viewModel: SimilarModelsViewModel
    @Environment(\.horizontalSizeClass) private var sizeClass

    init(modelId: Int64) {
        _viewModel = StateObject(wrappedValue: SimilarModelsViewModel(modelId: modelId))
    }

    var body: some View {
        Group {
            if viewModel.isLoading {
                LoadingStateView()
            } else if let error = viewModel.error {
                ErrorStateView(message: error) {
                    viewModel.retry()
                }
            } else if viewModel.similarModels.isEmpty {
                EmptyStateView(
                    icon: "magnifyingglass",
                    title: "No similar models found",
                    subtitle: "Try a different model to find related content."
                )
            } else {
                similarModelsGrid
            }
        }
        .navigationTitle(navigationTitle)
        .navigationBarTitleDisplayMode(.inline)
    }

    private var navigationTitle: String {
        if let name = viewModel.sourceModel?.name {
            return "Similar to \(name)"
        }
        return "Similar Models"
    }

    private var similarModelsGrid: some View {
        ScrollView {
            LazyVGrid(
                columns: AdaptiveGrid.columns(sizeClass: sizeClass),
                spacing: Spacing.sm
            ) {
                ForEach(viewModel.similarModels, id: \.id) { model in
                    NavigationLink(value: model.id) {
                        ModelCardView(model: model)
                    }
                    .buttonStyle(.plain)
                }
            }
            .padding(.horizontal, Spacing.sm)
            .padding(.top, Spacing.sm)
        }
    }
}
