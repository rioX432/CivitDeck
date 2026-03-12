import SwiftUI
import Shared

struct ReviewsSection: View {
    let reviews: [ResourceReview]
    let ratingTotals: RatingTotals?
    let sortOrder: ReviewSortOrder
    let isLoading: Bool
    let onSortChanged: (ReviewSortOrder) -> Void
    let onWriteReview: () -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: Spacing.sm) {
            Divider()
            reviewsHeader
            if let totals = ratingTotals, totals.total > 0 {
                RatingDistributionChart(totals: totals)
                thumbsSummary(up: Int(totals.thumbsUp), down: Int(totals.thumbsDown))
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

    // MARK: - Thumbs Summary

    private func thumbsSummary(up: Int, down: Int) -> some View {
        HStack(spacing: Spacing.lg) {
            HStack(spacing: Spacing.xs) {
                SwiftUI.Image(systemName: "hand.thumbsup")
                    .font(.caption2)
                    .foregroundColor(.civitPrimary)
                Text("\(up)")
                    .font(.civitLabelSmall)
            }
            HStack(spacing: Spacing.xs) {
                SwiftUI.Image(systemName: "hand.thumbsdown")
                    .font(.caption2)
                    .foregroundColor(.civitError)
                Text("\(down)")
                    .font(.civitLabelSmall)
            }
        }
    }
}

// MARK: - Rating Distribution Chart

struct RatingDistributionChart: View {
    let totals: RatingTotals

    var body: some View {
        let maxCount = max(
            totals.star1, totals.star2, totals.star3, totals.star4, totals.star5, 1
        )
        VStack(spacing: Spacing.xxs) {
            ForEach((1...5).reversed(), id: \.self) { star in
                ratingBar(star: star, count: Int(totals.countForStar(star: Int32(star))), maxCount: Int(maxCount))
            }
        }
    }

    private func ratingBar(star: Int, count: Int, maxCount: Int) -> some View {
        HStack(spacing: Spacing.xs) {
            Text("\(star)")
                .font(.civitLabelSmall)
                .frame(width: 12)
            SwiftUI.Image(systemName: "star.fill")
                .font(.civitLabelXSmall)
                .foregroundColor(.civitPrimary)
            GeometryReader { geo in
                ZStack(alignment: .leading) {
                    RoundedRectangle(cornerRadius: 2)
                        .fill(Color.civitSurfaceVariant)
                    RoundedRectangle(cornerRadius: 2)
                        .fill(Color.civitPrimary)
                        .frame(width: maxCount > 0
                               ? geo.size.width * CGFloat(count) / CGFloat(maxCount)
                               : 0)
                }
            }
            .frame(height: 6)
            Text("\(count)")
                .font(.civitLabelSmall)
                .foregroundColor(.civitOnSurfaceVariant)
                .frame(width: 28, alignment: .trailing)
        }
    }
}

// MARK: - Review Card

struct ReviewCardView: View {
    let review: ResourceReview

    var body: some View {
        VStack(alignment: .leading, spacing: Spacing.sm) {
            HStack {
                Circle()
                    .fill(Color.civitPrimary.opacity(0.2))
                    .frame(width: 28, height: 28)
                    .overlay(
                        Text(String((review.username?.first ?? Character("?")).uppercased()))
                            .font(.civitLabelSmall)
                            .foregroundColor(.civitPrimary)
                    )
                Text(review.username ?? "Anonymous")
                    .font(.civitLabelMedium)
                Spacer()
                starRating(rating: Int(review.rating))
            }

            if review.recommended {
                HStack(spacing: Spacing.xs) {
                    SwiftUI.Image(systemName: "hand.thumbsup")
                        .font(.civitLabelXSmall)
                        .foregroundColor(.civitPrimary)
                    Text("Recommended")
                        .font(.civitLabelSmall)
                        .foregroundColor(.civitPrimary)
                }
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

    private func starRating(rating: Int) -> some View {
        HStack(spacing: 1) {
            ForEach(1...5, id: \.self) { i in
                SwiftUI.Image(systemName: i <= rating ? "star.fill" : "star")
                    .font(.civitLabelXSmall)
                    .foregroundColor(i <= rating ? .civitPrimary : .civitOnSurfaceVariant.opacity(0.3))
            }
        }
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
                                    .foregroundColor(i <= rating ? .civitPrimary : .civitOnSurfaceVariant.opacity(0.3))
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
                        .frame(minHeight: 80)
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
        }
    }
}
