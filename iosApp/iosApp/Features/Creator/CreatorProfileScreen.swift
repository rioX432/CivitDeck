import SwiftUI
import Shared

struct CreatorProfileScreen: View {
    @StateObject private var viewModel: CreatorProfileViewModel
    @Environment(\.horizontalSizeClass) private var sizeClass

    init(username: String) {
        _viewModel = StateObject(wrappedValue: CreatorProfileViewModel(username: username))
    }

    private var columns: [GridItem] {
        AdaptiveGrid.columns(sizeClass: sizeClass)
    }

    var body: some View {
        Group {
            if viewModel.isLoading && viewModel.models.isEmpty {
                LoadingStateView()
            } else if let error = viewModel.error, viewModel.models.isEmpty {
                ErrorStateView(message: error) {
                    Task { await viewModel.refresh() }
                }
            } else {
                modelGrid
            }
        }
        .navigationTitle(viewModel.username)
        .navigationBarTitleDisplayMode(.inline)
        .toolbar {
            ToolbarItem(placement: .navigationBarTrailing) {
                Button(action: viewModel.toggleFollow) {
                    Image(systemName: viewModel.isFollowing ? "person.badge.minus" : "person.badge.plus")
                }
            }
        }
    }

    private var modelGrid: some View {
        ScrollView {
            LazyVGrid(columns: columns, spacing: Spacing.sm) {
                ForEach(Array(viewModel.models.enumerated()), id: \.element.id) { index, model in
                    NavigationLink(value: model.id) {
                        ModelCardView(model: model)
                    }
                    .buttonStyle(.plain)
                    .onAppear {
                        if index == viewModel.models.count - 3 {
                            viewModel.loadMore()
                        }
                    }
                }
            }
            .padding(.horizontal, Spacing.md)

            if viewModel.isLoadingMore {
                ProgressView()
                    .padding()
            }
        }
        .refreshable {
            await viewModel.refresh()
        }
    }
}
