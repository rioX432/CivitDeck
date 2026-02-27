import SwiftUI
import Shared

struct SwipeDiscoveryView: View {
    @StateObject private var viewModel = SwipeDiscoveryViewModel()
    @Environment(\.dismiss) private var dismiss

    var onModelDetail: ((Int64) -> Void)?

    var body: some View {
        VStack(spacing: 0) {
            contentArea
            actionButtons
                .padding(.bottom, Spacing.lg)
        }
        .navigationTitle("Discover")
        .navigationBarTitleDisplayMode(.inline)
    }

    private var contentArea: some View {
        GeometryReader { geometry in
            ZStack {
                if viewModel.isLoading && viewModel.cards.isEmpty {
                    ProgressView()
                } else if let error = viewModel.error, viewModel.cards.isEmpty {
                    Text(error)
                        .foregroundColor(.civitError)
                } else if viewModel.cards.isEmpty {
                    Text("No more models to discover")
                        .font(.civitBodyMedium)
                        .foregroundColor(.civitOnSurfaceVariant)
                } else {
                    cardStack(in: geometry)
                }
            }
            .frame(maxWidth: .infinity, maxHeight: .infinity)
        }
        .padding(.horizontal, Spacing.lg)
    }

    @ViewBuilder
    private func cardStack(in geometry: GeometryProxy) -> some View {
        let visibleCards = Array(viewModel.cards.prefix(3))

        ZStack {
            ForEach(Array(visibleCards.enumerated().reversed()), id: \.element.id) { index, model in
                let scale = 1.0 - Double(index) * 0.05
                let offsetY = CGFloat(index) * 12

                if index == 0 {
                    SwipeCardView(
                        model: model,
                        screenWidth: geometry.size.width,
                        onSwiped: { direction in
                            handleSwipe(model: model, direction: direction)
                        }
                    )
                    .scaleEffect(scale)
                    .offset(y: offsetY)
                } else {
                    DiscoveryCardContent(model: model)
                        .scaleEffect(scale)
                        .offset(y: offsetY)
                }
            }
        }
    }

    private var actionButtons: some View {
        HStack(spacing: Spacing.lg * 2) {
            // Undo button
            Button(action: viewModel.undoLastSwipe) {
                Image(systemName: "arrow.uturn.backward")
                    .font(.title3)
                    .frame(width: 48, height: 48)
                    .foregroundColor(.civitOnSurface)
                    .background(Color.civitSurfaceContainerHigh)
                    .clipShape(Circle())
            }
            .disabled(viewModel.lastDismissed == nil)
            .opacity(viewModel.lastDismissed == nil ? 0.4 : 1.0)

            // Skip button
            Button(action: {
                if let top = viewModel.cards.first {
                    viewModel.onSwipeLeft(top)
                }
            }) {
                Image(systemName: "xmark")
                    .font(.title2)
                    .frame(width: 56, height: 56)
                    .foregroundColor(.civitError)
                    .background(Color.civitSurfaceContainerHigh)
                    .clipShape(Circle())
            }
            .disabled(viewModel.cards.isEmpty)

            // Favorite button
            Button(action: {
                if let top = viewModel.cards.first {
                    viewModel.onSwipeRight(top)
                }
            }) {
                Image(systemName: "heart.fill")
                    .font(.title2)
                    .frame(width: 56, height: 56)
                    .foregroundColor(.civitPrimary)
                    .background(Color.civitSurfaceContainerHigh)
                    .clipShape(Circle())
            }
            .disabled(viewModel.cards.isEmpty)
        }
        .padding(.vertical, Spacing.md)
    }

    private func handleSwipe(model: Model, direction: SwipeDirection) {
        switch direction {
        case .right:
            viewModel.onSwipeRight(model)
        case .left:
            viewModel.onSwipeLeft(model)
        case .up:
            let modelId = viewModel.onSwipeUp(model)
            onModelDetail?(modelId)
        }
    }
}
