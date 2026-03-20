import SwiftUI
import Shared

struct ComfyHubDetailView: View {
    @StateObject private var viewModel: ComfyHubDetailViewModel
    @State private var showImportAlert = false
    @State private var alertMessage = ""

    init(workflowId: String) {
        _viewModel = StateObject(wrappedValue: ComfyHubDetailViewModel(workflowId: workflowId))
    }

    var body: some View {
        Group {
            if viewModel.isLoading {
                LoadingStateView()
            } else if let error = viewModel.error {
                ErrorStateView(message: error) {
                    viewModel.retry()
                }
            } else if let workflow = viewModel.workflow {
                detailContent(workflow)
            }
        }
        .navigationTitle(viewModel.workflow?.name ?? "Workflow Detail")
        .navigationBarTitleDisplayMode(.inline)
        .alert("Import", isPresented: $showImportAlert) {
            Button("OK") { viewModel.dismissImportResult() }
        } message: {
            Text(alertMessage)
        }
        .onChange(of: viewModel.importResult != nil) { hasResult in
            guard hasResult, let result = viewModel.importResult else { return }
            switch result {
            case .success:
                alertMessage = "Workflow imported successfully!"
            case .failure(let msg):
                alertMessage = "Import failed: \(msg)"
            }
            showImportAlert = true
        }
    }

    private func detailContent(_ workflow: ComfyHubWorkflow) -> some View {
        ScrollView {
            VStack(alignment: .leading, spacing: Spacing.md) {
                headerSection(workflow)
                Divider()
                descriptionSection(workflow)
                tagsSection(workflow)
                Divider()
                nodeGraphSection
                Divider()
                importButton
            }
            .padding(Spacing.md)
        }
    }

    private func headerSection(_ workflow: ComfyHubWorkflow) -> some View {
        VStack(alignment: .leading, spacing: Spacing.xs) {
            Text(workflow.name)
                .font(.civitHeadlineSmall)
            Text("by \(workflow.author)")
                .font(.civitBodyMedium)
                .foregroundColor(.civitOnSurfaceVariant)
            HStack(spacing: Spacing.md) {
                Label(formatCount(Int(workflow.downloads)), systemImage: "arrow.down.circle")
                Label(String(format: "%.1f", workflow.rating), systemImage: "star")
                Text("\(workflow.nodeCount) nodes")
                Text(workflow.category)
                    .foregroundColor(.civitPrimary)
            }
            .font(.civitBodySmall)
            .foregroundColor(.civitOnSurfaceVariant)
        }
    }

    private func descriptionSection(_ workflow: ComfyHubWorkflow) -> some View {
        VStack(alignment: .leading, spacing: Spacing.xs) {
            Text("Description")
                .font(.civitTitleMedium)
            Text(workflow.description_)
                .font(.civitBodyMedium)
        }
    }

    private func tagsSection(_ workflow: ComfyHubWorkflow) -> some View {
        Group {
            if !workflow.tags.isEmpty {
                FlowLayout(spacing: Spacing.xs) {
                    ForEach(workflow.tags, id: \.self) { tag in
                        Text(tag)
                            .font(.civitLabelSmall)
                            .padding(.horizontal, Spacing.sm)
                            .padding(.vertical, Spacing.xs)
                            .background(Color.civitSurfaceVariant)
                            .clipShape(Capsule())
                    }
                }
            }
        }
    }

    private var nodeGraphSection: some View {
        VStack(alignment: .leading, spacing: Spacing.xs) {
            Text("Node Graph (\(viewModel.nodeNames.count) node types)")
                .font(.civitTitleMedium)
            ForEach(viewModel.nodeNames, id: \.self) { name in
                Text(name)
                    .font(.civitBodySmall)
                    .foregroundColor(.civitOnSurfaceVariant)
                    .padding(.leading, Spacing.sm)
            }
        }
    }

    private var importButton: some View {
        Button {
            viewModel.onImport()
        } label: {
            HStack {
                if viewModel.isImporting {
                    ProgressView()
                        .tint(.white)
                    Text("Importing...")
                } else {
                    Image(systemName: "arrow.down.circle")
                    Text("Import to ComfyUI")
                }
            }
            .frame(maxWidth: .infinity)
            .padding(.vertical, Spacing.sm)
        }
        .buttonStyle(.borderedProminent)
        .disabled(viewModel.isImporting)
    }

    private func formatCount(_ count: Int) -> String {
        count >= 1000 ? "\(count / 1000)k" : "\(count)"
    }
}
