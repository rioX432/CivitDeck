import SwiftUI
import Shared

struct ComfyHubBrowserView: View {
    @StateObject private var viewModel = ComfyHubBrowserViewModelOwner()
    @Environment(\.horizontalSizeClass) private var sizeClass

    var body: some View {
        VStack(spacing: 0) {
            searchBar
            categoryChips
            contentView
        }
        .navigationTitle("ComfyHub Workflows")
        .navigationBarTitleDisplayMode(.inline)
    }

    private var searchBar: some View {
        TextField("Search workflows...", text: Binding(
            get: { viewModel.query },
            set: { viewModel.onQueryChanged($0) }
        ))
        .textFieldStyle(.roundedBorder)
        .padding(.horizontal, Spacing.md)
        .padding(.vertical, Spacing.sm)
    }

    private var categoryChips: some View {
        ScrollView(.horizontal, showsIndicators: false) {
            HStack(spacing: Spacing.xs) {
                ForEach(ComfyHubCategory.allCases, id: \.self) { category in
                    Button {
                        viewModel.onCategorySelected(category)
                    } label: {
                        Text(category.displayName)
                            .font(.civitLabelMedium)
                            .padding(.horizontal, Spacing.sm)
                            .padding(.vertical, Spacing.xs)
                            .background(
                                category == viewModel.selectedCategory
                                    ? Color.civitPrimary
                                    : Color.civitSurfaceVariant
                            )
                            .foregroundColor(
                                category == viewModel.selectedCategory
                                    ? Color.civitOnPrimary
                                    : Color.civitOnSurfaceVariant
                            )
                            .clipShape(Capsule())
                    }
                    .buttonStyle(.plain)
                }
            }
            .padding(.horizontal, Spacing.md)
            .padding(.bottom, Spacing.sm)
        }
    }

    @ViewBuilder
    private var contentView: some View {
        if viewModel.isLoading {
            LoadingStateView()
        } else if let error = viewModel.error {
            ErrorStateView(message: error) {
                viewModel.retry()
            }
        } else if viewModel.workflows.isEmpty {
            EmptyStateView(
                icon: "magnifyingglass",
                title: "No workflows found",
                subtitle: "Try different search terms or category."
            )
        } else {
            workflowList
        }
    }

    private var workflowList: some View {
        ScrollView {
            LazyVGrid(
                columns: AdaptiveGrid.columns(sizeClass: sizeClass),
                spacing: Spacing.sm
            ) {
                ForEach(viewModel.workflows, id: \.id) { workflow in
                    NavigationLink(destination: ComfyHubDetailView(workflowId: workflow.id)) {
                        WorkflowCardView(workflow: workflow)
                    }
                    .buttonStyle(.plain)
                }
            }
            .padding(.horizontal, Spacing.sm)
        }
    }
}

private struct WorkflowCardView: View {
    let workflow: ComfyHubWorkflow

    var body: some View {
        VStack(alignment: .leading, spacing: Spacing.xs) {
            Text(workflow.name)
                .font(.civitTitleMedium)
                .lineLimit(1)
            Text(workflow.description_)
                .font(.civitBodySmall)
                .foregroundColor(.civitOnSurfaceVariant)
                .lineLimit(2)
            HStack(spacing: Spacing.md) {
                Label(formatCount(Int(workflow.downloads)), systemImage: "arrow.down.circle")
                Label(String(format: "%.1f", workflow.rating), systemImage: "star")
                Text("\(workflow.nodeCount) nodes")
            }
            .font(.civitLabelSmall)
            .foregroundColor(.civitOnSurfaceVariant)
            Text(workflow.category)
                .font(.civitLabelSmall)
                .foregroundColor(.civitPrimary)
        }
        .padding(Spacing.md)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(Color.civitSurfaceVariant.opacity(0.3))
        .clipShape(RoundedRectangle(cornerRadius: 12))
    }

    private func formatCount(_ count: Int) -> String {
        count >= 1000 ? "\(count / 1000)k" : "\(count)"
    }
}
