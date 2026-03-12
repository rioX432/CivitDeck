import SwiftUI
import Shared

struct ExternalServerGalleryView: View {
    let serverName: String
    @StateObject private var viewModel = ExternalServerGalleryViewModel()
    @State private var selectedImage: ServerImage?

    private let columns = [
        GridItem(.flexible(), spacing: Spacing.xxs),
        GridItem(.flexible(), spacing: Spacing.xxs),
        GridItem(.flexible(), spacing: Spacing.xxs),
    ]

    var body: some View {
        Group {
            if viewModel.isLoading {
                ProgressView()
                    .frame(maxWidth: .infinity, maxHeight: .infinity)
            } else if let error = viewModel.error, viewModel.images.isEmpty {
                VStack(spacing: Spacing.md) {
                    Text("Failed to load images")
                        .font(.civitTitleMedium)
                    Text(error)
                        .font(.civitBodySmall)
                        .foregroundColor(.civitOnSurfaceVariant)
                        .multilineTextAlignment(.center)
                    Button("Retry") {
                        Task { await viewModel.loadFirstPage() }
                    }
                }
                .padding(Spacing.lg)
            } else {
                ScrollView {
                    LazyVGrid(columns: columns, spacing: Spacing.xxs) {
                        ForEach(viewModel.images, id: \.id) { image in
                            ServerImageCell(image: image)
                                .onTapGesture { selectedImage = image }
                                .onAppear {
                                    if image.id == viewModel.images.last?.id {
                                        Task { await viewModel.loadMore() }
                                    }
                                }
                        }
                    }
                    if viewModel.isLoadingMore {
                        ProgressView()
                            .padding(Spacing.md)
                    }
                }
                .refreshable { await viewModel.refresh() }
            }
        }
        .navigationTitle(serverName)
        .navigationBarTitleDisplayMode(.inline)
        .toolbar {
            ToolbarItemGroup(placement: .navigationBarTrailing) {
                if viewModel.supportsFilters {
                    Button {
                        viewModel.showFilterSheet = true
                    } label: {
                        Image(systemName: "line.3.horizontal.decrease.circle")
                    }
                }
                if viewModel.supportsGeneration {
                    Button {
                        viewModel.showGenerationSheet = true
                        if viewModel.generationOptions.isEmpty {
                            Task { await viewModel.loadGenerationOptions() }
                        }
                    } label: {
                        Image(systemName: "bolt.fill")
                    }
                }
            }
        }
        .sheet(isPresented: $viewModel.showFilterSheet) {
            ExternalServerFilterSheet(
                filters: viewModel.filters,
                onApply: { viewModel.applyFilters($0) },
                onReset: { viewModel.resetFilters() }
            )
        }
        .sheet(isPresented: $viewModel.showGenerationSheet) {
            ExternalServerGenerationSheet(viewModel: viewModel)
        }
        .sheet(item: $selectedImage) { image in
            NavigationStack {
                ExternalServerImageDetailView(image: image)
            }
        }
        .alert("Generation Status", isPresented: .constant(viewModel.activeJob != nil)) {
            Button("Dismiss") { viewModel.dismissJobStatus() }
        } message: {
            if let job = viewModel.activeJob {
                Text(jobStatusMessage(job))
            }
        }
        .task {
            await viewModel.loadCapabilities()
            await viewModel.loadFirstPage()
        }
    }

    private func jobStatusMessage(_ job: GenerationJob) -> String {
        var msg = ""
        switch job.status {
        case .queued: msg = "Queued"
        case .running: msg = "Generating... \(Int(job.progress * 100))%"
        case .completed: msg = "Complete"
        case .error: msg = "Error"
        default: msg = "Unknown"
        }
        if !job.message.isEmpty {
            msg += "\n\(job.message)"
        }
        if job.total > 0 {
            msg += "\n\(job.completed)/\(job.total) images"
        }
        return msg
    }
}

// MARK: - ServerImage Identifiable
extension ServerImage: @retroactive Identifiable {}

private struct ServerImageCell: View {
    let image: ServerImage

    var body: some View {
        ZStack(alignment: .topTrailing) {
            CivitAsyncImageView(
                imageUrl: image.thumbUrl ?? image.file,
                contentMode: .fill,
                aspectRatio: 1.0
            )
            if let score = image.aestheticScore {
                Text(String(format: "%.1f", Double(score)))
                    .font(.civitLabelSmall)
                    .foregroundColor(.white)
                    .padding(Spacing.xs)
                    .background(Color.black.opacity(0.5))
                    .cornerRadius(Spacing.xs)
                    .padding(Spacing.xs)
            }
        }
    }
}
