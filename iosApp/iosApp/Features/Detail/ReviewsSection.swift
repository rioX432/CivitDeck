import SwiftUI
import Shared

private let avatarSize: CGFloat = 28
private let textEditorMinHeight: CGFloat = 80

struct ReviewsSection: View {
    let reviews: [ResourceReview]
    let ratingTotals: RatingTotals?
    let sortOrder: ReviewSortOrder
    let isLoading: Bool
    let onSortChanged: (ReviewSortOrder) -> Void
    let onWriteReview: () -> Void
    @Environment(\.civitTheme) private var theme

    var body: some View {
        VStack(alignment: .leading, spacing: Spacing.sm) {
            Divider()
            reviewsHeader
            if let totals = ratingTotals, totals.total > 0 {
                thumbsSummary(up: Int(totals.thumbsUp), down: Int(totals.thumbsDown), primaryColor: theme.primary)
            }
            reviewsList
        }
        .padding(.horizontal, Spacing.lg)
    }

    // MARK: - Header

    private var reviewsHeader: some View {
        HStack {
            Text("Reviews (\(ratingTotals?.total ?? 0))")
                .font(.civitTitleSmall)
            Spacer()
            sortMenu
            Button("Write") { onWriteReview() }
                .font(.civitLabelMedium)
        }
    }

    private var sortMenu: some View {
        Menu {
            Button("Newest") { onSortChanged(.newest) }
            Button("Highest") { onSortChanged(.highestRated) }
            Button("Lowest") { onSortChanged(.lowestRated) }
        } label: {
            Text(sortOrder.label)
                .font(.civitLabelMedium)
        }
    }

    // MARK: - Reviews List

    @ViewBuilder
    private var reviewsList: some View {
        if isLoading {
            ProgressView()
                .frame(maxWidth: .infinity)
                .padding(Spacing.lg)
        } else if reviews.isEmpty {
            Text("No reviews yet")
                .font(.civitBodyMedium)
                .foregroundColor(.civitOnSurfaceVariant)
        } else {
            ForEach(reviews.prefix(5), id: \.id) { review in
                ReviewCardView(review: review)
            }
        }
    }
}

// MARK: - Thumbs Summary

private func thumbsSummary(up: Int, down: Int, primaryColor: Color) -> some View {
    HStack(spacing: Spacing.lg) {
        HStack(spacing: Spacing.xs) {
            SwiftUI.Image(systemName: "hand.thumbsup")
                .font(.caption2)
                .foregroundColor(primaryColor)
                .accessibilityHidden(true)
            Text("\(up)")
                .font(.civitLabelSmall)
        }
        HStack(spacing: Spacing.xs) {
            SwiftUI.Image(systemName: "hand.thumbsdown")
                .font(.caption2)
                .foregroundColor(.civitError)
                .accessibilityHidden(true)
            Text("\(down)")
                .font(.civitLabelSmall)
        }
    }
}

// MARK: - Review Card

struct ReviewCardView: View {
    let review: ResourceReview
    @Environment(\.civitTheme) private var theme

    var body: some View {
        VStack(alignment: .leading, spacing: Spacing.sm) {
            HStack {
                Circle()
                    .fill(theme.primary.opacity(0.2))
                    .frame(width: avatarSize, height: avatarSize)
                    .overlay(
                        Text(String((review.username?.first ?? Character("?")).uppercased()))
                            .font(.civitLabelSmall)
                            .foregroundColor(theme.primary)
                    )
                Text(review.username ?? "Anonymous")
                    .font(.civitLabelMedium)
                Spacer()
                SwiftUI.Image(systemName: review.recommended ? "hand.thumbsup" : "hand.thumbsdown")
                    .font(.civitLabelSmall)
                    .foregroundColor(review.recommended ? theme.primary : .civitError)
                    .accessibilityLabel(review.recommended ? "Recommended" : "Not recommended")
            }

            HStack(spacing: Spacing.xs) {
                SwiftUI.Image(systemName: review.recommended ? "hand.thumbsup" : "hand.thumbsdown")
                    .font(.civitLabelXSmall)
                    .foregroundColor(review.recommended ? theme.primary : .civitError)
                    .accessibilityHidden(true)
                Text(review.recommended ? "Recommended" : "Not Recommended")
                    .font(.civitLabelSmall)
                    .foregroundColor(review.recommended ? theme.primary : .civitError)
            }

            if let details = review.details, !details.isEmpty {
                Text(details)
                    .font(.civitBodySmall)
                    .foregroundColor(.civitOnSurfaceVariant)
            }

            Text(formatDate(review.createdAt))
                .font(.civitLabelSmall)
                .foregroundColor(.civitOnSurfaceVariant.opacity(0.7))
        }
        .padding(Spacing.md)
        .background(Color.civitSurfaceVariant.opacity(0.5))
        .clipShape(RoundedRectangle(cornerRadius: CornerRadius.card))
    }

    private func formatDate(_ isoDate: String) -> String {
        String(isoDate.prefix(10))
    }
}

// MARK: - Submit Review Sheet

struct SubmitReviewSheet: View {
    let isSubmitting: Bool
    let onSubmit: (_ rating: Int32, _ recommended: Bool, _ details: String?) -> Void
    let onDismiss: () -> Void
    @Environment(\.civitTheme) private var theme

    @State private var rating: Int = 0
    @State private var recommended: Bool = true
    @State private var details: String = ""

    var body: some View {
        NavigationView {
            Form {
                Section("Rating") {
                    HStack(spacing: Spacing.xs) {
                        ForEach(1...5, id: \.self) { i in
                            Button {
                                rating = i
                            } label: {
                                SwiftUI.Image(systemName: i <= rating ? "star.fill" : "star")
                                    .font(.title2)
                                    .foregroundColor(i <= rating ? theme.primary : .civitOnSurfaceVariant.opacity(0.3))
                                    .accessibilityLabel("\(i) star\(i == 1 ? "" : "s")")
                            }
                            .buttonStyle(.plain)
                        }
                    }
                }

                Section("Recommendation") {
                    Picker("", selection: $recommended) {
                        Label("Recommend", systemImage: "hand.thumbsup").tag(true)
                        Label("Not recommended", systemImage: "hand.thumbsdown").tag(false)
                    }
                    .pickerStyle(.segmented)
                }

                Section("Details (optional)") {
                    TextEditor(text: $details)
                        .frame(minHeight: textEditorMinHeight)
                }

                Section {
                    Button {
                        onSubmit(Int32(rating), recommended, details.isEmpty ? nil : details)
                    } label: {
                        HStack {
                            if isSubmitting {
                                ProgressView()
                                    .padding(.trailing, Spacing.xs)
                            }
                            Text("Submit Review")
                        }
                        .frame(maxWidth: .infinity)
                    }
                    .disabled(rating == 0 || isSubmitting)
                }
            }
            .navigationTitle("Write a Review")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Cancel") { onDismiss() }
                }
            }
        }
    }
}

// MARK: - Helpers

private extension ReviewSortOrder {
    var label: String {
        switch self {
        case .newest: return "Newest"
        case .highestRated: return "Highest"
        case .lowestRated: return "Lowest"
        @unknown default: return "Unknown"
        }
    }
}
