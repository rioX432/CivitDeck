import SwiftUI
import Shared

private let cardStackOffset: CGFloat = 12
private let undoButtonSize: CGFloat = 48
private let actionButtonSize: CGFloat = 56

struct SwipeDiscoveryView: View {
    @StateObject private var viewModel = SwipeDiscoveryViewModelOwner()
    @Environment(\.dismiss) private var dismiss
    @Environment(\.civitTheme) private var theme

    var onModelDetail: ((Int64) -> Void)?

    var body: some View {
        VStack(spacing: 0) {
            contentArea
            actionButtons
                .padding(.bottom, Spacing.lg)
        }
        .navigationTitle("Discover")
        .navigationBarTitleDisplayMode(.inline)
        .task {
            await viewModel.observeState()
        }
    }

    private var contentArea: some View {
        GeometryReader { geometry in
            ZStack {
                if viewModel.isLoading && viewModel.cards.isEmpty {
                    LoadingStateView()
                } else if let error = viewModel.error, viewModel.cards.isEmpty {
                    ErrorStateView(message: error)
                } else if viewModel.cards.isEmpty {
                    EmptyStateView(
                        icon: "books.vertical",
                        title: "No more models to discover"
                    )
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
                let offsetY = CGFloat(index) * cardStackOffset

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
                    .frame(width: undoButtonSize, height: undoButtonSize)
                    .foregroundColor(.civitOnSurface)
                    .background(Color.civitSurfaceContainerHigh)
                    .clipShape(Circle())
            }
            .accessibilityLabel("Undo last swipe")
            .disabled(!viewModel.hasLastDismissed)
            .opacity(!viewModel.hasLastDismissed ? 0.4 : 1.0)

            // Skip button
            Button(action: {
                if let top = viewModel.cards.first {
                    viewModel.onSwipeLeft(top)
                }
            }) {
                Image(systemName: "xmark")
                    .font(.title2)
                    .frame(width: actionButtonSize, height: actionButtonSize)
                    .foregroundColor(.civitError)
                    .background(Color.civitSurfaceContainerHigh)
                    .clipShape(Circle())
            }
            .accessibilityLabel("Skip")
            .disabled(viewModel.cards.isEmpty)

            // Favorite button
            Button(action: {
                if let top = viewModel.cards.first {
                    viewModel.onSwipeRight(top)
                }
            }) {
                Image(systemName: "heart.fill")
                    .font(.title2)
                    .frame(width: actionButtonSize, height: actionButtonSize)
                    .foregroundColor(theme.primary)
                    .background(Color.civitSurfaceContainerHigh)
                    .clipShape(Circle())
            }
            .accessibilityLabel("Add to favorites")
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
