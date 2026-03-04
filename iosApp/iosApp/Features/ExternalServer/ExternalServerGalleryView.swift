import SwiftUI
import Shared

struct ExternalServerGalleryView: View {
    let serverName: String
    @StateObject private var viewModel = ExternalServerGalleryViewModel()

    private let columns = [
        GridItem(.flexible(), spacing: 2),
        GridItem(.flexible(), spacing: 2),
        GridItem(.flexible(), spacing: 2),
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
                    LazyVGrid(columns: columns, spacing: 2) {
                        ForEach(viewModel.images, id: \.id) { image in
                            ServerImageCell(image: image)
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
            }
        }
        .navigationTitle(serverName)
        .navigationBarTitleDisplayMode(.inline)
        .task { await viewModel.loadFirstPage() }
    }
}

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
                    .cornerRadius(4)
                    .padding(Spacing.xs)
            }
        }
    }
}
