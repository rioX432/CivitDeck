import SwiftUI
import Shared

struct ExternalServerGalleryView: View {
    let serverName: String
    @StateObject private var viewModel = ExternalServerGalleryViewModel()
    @Environment(\.horizontalSizeClass) private var sizeClass
    @State private var selectedIndex: Int?
    @State private var showJobAlert = false

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
                    LazyVGrid(columns: AdaptiveGrid.columns(sizeClass: sizeClass), spacing: Spacing.sm) {
                        ForEach(Array(viewModel.images.enumerated()), id: \.element.id) { idx, image in
                            ServerImageCell(image: image)
                                .accessibilityLabel("Select image")
                                .onTapGesture { selectedIndex = idx }
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
                            .accessibilityLabel("Filters")
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
                            .accessibilityLabel("Generate")
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
        .fullScreenCover(isPresented: Binding(
            get: { selectedIndex != nil },
            set: { if !$0 { selectedIndex = nil } }
        )) {
            if let idx = selectedIndex {
                NavigationStack {
                    ExternalServerImageDetailView(
                        images: viewModel.images,
                        initialIndex: idx
                    )
                }
            }
        }
        .onChange(of: viewModel.activeJob != nil) { showJobAlert = $0 }
        .alert("Generation Status", isPresented: $showJobAlert) {
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
        CachedAsyncImage(url: URL(string: image.thumbUrl ?? image.file)) { phase in
            switch phase {
            case .success(let img):
                img
                    .resizable()
                    .scaledToFill()
                    .transition(.opacity)
            case .failure:
                Color.civitSurfaceVariant
                    .overlay {
                        Image(systemName: "photo")
                            .foregroundColor(.civitOnSurfaceVariant)
                    }
            case .empty:
                Color.civitSurfaceVariant
                    .shimmer()
            @unknown default:
                Color.clear
            }
        }
        .frame(maxWidth: .infinity)
        .aspectRatio(1, contentMode: .fit)
        .clipped()
        .cornerRadius(Spacing.sm)
    }
}
