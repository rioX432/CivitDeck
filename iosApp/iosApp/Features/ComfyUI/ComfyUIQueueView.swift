import SwiftUI
import Shared

struct ComfyUIQueueView: View {
    @StateObject private var viewModel = ComfyUIQueueViewModelOwner()

    var body: some View {
        Group {
            if viewModel.isLoading && viewModel.jobs.isEmpty {
                LoadingStateView()
            } else if let error = viewModel.error, viewModel.jobs.isEmpty {
                ErrorStateView(message: error) {
                    viewModel.dismissError()
                }
            } else if viewModel.jobs.isEmpty {
                EmptyStateView(
                    icon: "square.stack",
                    title: "Queue is empty",
                    subtitle: "No jobs are running or pending."
                )
            } else {
                jobList
            }
        }
        .navigationTitle("Queue")
        .navigationBarTitleDisplayMode(.inline)
        .task {
            await viewModel.observeUiState()
        }
        .onDisappear {
        }
    }

    private var jobList: some View {
        List(viewModel.jobs, id: \.promptId) { job in
            QueueJobRow(
                job: job,
                isCancelling: viewModel.cancellingIds.contains(job.promptId)
            ) {
                viewModel.onCancelJob(promptId: job.promptId)
            }
        }
        .listStyle(.plain)
    }
}

private struct QueueJobRow: View {
    let job: QueueJob
    let isCancelling: Bool
    let onCancel: () -> Void
    @Environment(\.civitTheme) private var theme

    var body: some View {
        HStack(spacing: Spacing.sm) {
            VStack(alignment: .leading, spacing: Spacing.xs) {
                Text(String(job.promptId.prefix(8)) + "...")
                    .font(.civitBodySmall)
                    .foregroundColor(.civitOnSurfaceVariant)
                Text(statusLabel)
                    .font(.civitLabelMedium)
                    .foregroundColor(statusColor)
            }
            Spacer()
            if isCancelling {
                ProgressView()
            } else if job.status != .completed {
                Button(role: .destructive, action: onCancel) {
                    Image(systemName: "xmark.circle.fill")
                        .accessibilityLabel("Cancel job")
                        .foregroundColor(.civitError)
                }
                .buttonStyle(.plain)
            }
        }
        .padding(.vertical, Spacing.xs)
    }

    private var statusLabel: String {
        switch job.status {
        case .queued: return "Queued"
        case .running: return "Running"
        case .completed: return "Completed"
        case .error: return "Error"
        default: return "Unknown"
        }
    }

    private var statusColor: Color {
        switch job.status {
        case .running: return theme.primary
        case .error: return .civitError
        case .completed: return .green
        default: return .civitOnSurfaceVariant
        }
    }
}
