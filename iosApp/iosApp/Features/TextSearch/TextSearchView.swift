import SwiftUI
import Shared

struct TextSearchView: View {
    @StateObject private var viewModel = TextSearchViewModelOwner()
    @Environment(\.horizontalSizeClass) private var sizeClass

    var body: some View {
        Group {
            if !viewModel.isModelAvailable {
                unavailableState
            } else if viewModel.isLoading {
                LoadingStateView()
            } else if let error = viewModel.error {
                ErrorStateView(message: error) {
                    viewModel.retry()
                }
            } else if viewModel.hasSearched && viewModel.results.isEmpty {
                emptyResultsState
            } else if !viewModel.results.isEmpty {
                searchResultsGrid
            } else {
                idleState
            }
        }
        .task { await viewModel.observeUiState() }
        .navigationTitle("AI Search")
        .navigationBarTitleDisplayMode(.inline)
        .safeAreaInset(edge: .top) {
            if viewModel.isModelAvailable {
                searchBar
            }
        }
    }

    private var searchBar: some View {
        HStack(spacing: Spacing.sm) {
            TextField("Describe what you're looking for...", text: Binding(
                get: { viewModel.query },
                set: { viewModel.onQueryChanged($0) }
            ))
            .textFieldStyle(.roundedBorder)
            .submitLabel(.search)
            .onSubmit { viewModel.search() }

            Button {
                viewModel.search()
            } label: {
                Image(systemName: "magnifyingglass")
                    .font(.body)
                    .accessibilityLabel("Search")
            }
            .disabled(viewModel.query.trimmingCharacters(in: .whitespaces).isEmpty)
        }
        .padding(.horizontal, Spacing.md)
        .padding(.vertical, Spacing.sm)
        .background(.bar)
    }

    private var unavailableState: some View {
        EmptyStateView(
            icon: "sparkles",
            title: "AI Search coming soon",
            subtitle: "Text-to-image search using SigLIP-2 is under development. " +
                "The text encoder and tokenizer are being integrated."
        )
    }

    private var emptyResultsState: some View {
        EmptyStateView(
            icon: "magnifyingglass",
            title: "No matching models found",
            subtitle: "Try a different description or wait for more models to be indexed."
        )
    }

    private var idleState: some View {
        EmptyStateView(
            icon: "sparkles",
            title: "Search by description",
            subtitle: "Describe the kind of model you're looking for and AI will find matches."
        )
    }

    private var searchResultsGrid: some View {
        ScrollView {
            LazyVGrid(
                columns: AdaptiveGrid.columns(sizeClass: sizeClass),
                spacing: Spacing.sm
            ) {
                ForEach(viewModel.results, id: \.id) { model in
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
