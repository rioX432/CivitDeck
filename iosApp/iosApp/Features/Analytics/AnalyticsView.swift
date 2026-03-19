import SwiftUI
import Charts

private let chartHeight: CGFloat = 180
private let barRowHeight: CGFloat = 36

struct AnalyticsView: View {
    @StateObject private var viewModel = AnalyticsViewModel()
    @Environment(\.civitTheme) private var theme

    var body: some View {
        Group {
            if viewModel.isLoading {
                ProgressView("Loading stats...")
            } else {
                analyticsContent
            }
        }
        .navigationTitle("Usage Stats")
        .navigationBarTitleDisplayMode(.inline)
        .task { await viewModel.loadStats() }
    }

    private var analyticsContent: some View {
        List {
            summarySection
            if let avgMs = viewModel.averageViewDurationMs {
                avgDurationSection(avgMs)
            }
            if !viewModel.dailyViewCounts.isEmpty {
                viewTrendSection
            }
            if !viewModel.topModelTypes.isEmpty {
                modelTypesSection
            }
            if !viewModel.topCreators.isEmpty {
                creatorsSection
            }
            if !viewModel.topSearchQueries.isEmpty {
                searchesSection
            }
        }
    }

    private var summarySection: some View {
        Section("Overview") {
            HStack(spacing: Spacing.lg) {
                StatBadge(label: "Views", value: viewModel.totalViews)
                StatBadge(label: "Favorites", value: viewModel.totalFavorites)
                StatBadge(label: "Searches", value: viewModel.totalSearches)
            }
            .frame(maxWidth: .infinity)
            .padding(.vertical, Spacing.sm)
        }
    }

    private func avgDurationSection(_ avgMs: Int64) -> some View {
        let seconds = avgMs / 1000
        let label = seconds >= 60
            ? "\(seconds / 60)m \(seconds % 60)s"
            : "\(seconds)s"
        return Section("Avg. View Duration") {
            Text(label)
                .font(.civitHeadlineSmall)
                .fontWeight(.bold)
                .foregroundColor(.civitOnSurface)
                .frame(maxWidth: .infinity, alignment: .center)
                .padding(.vertical, Spacing.sm)
        }
    }

    private var viewTrendSection: some View {
        Section("Views (Last 30 Days)") {
            Chart(viewModel.dailyViewCounts, id: \.day) { item in
                LineMark(
                    x: .value("Day", item.day),
                    y: .value("Views", item.count)
                )
                .foregroundStyle(theme.primary)
                PointMark(
                    x: .value("Day", item.day),
                    y: .value("Views", item.count)
                )
                .foregroundStyle(theme.primary)
            }
            .frame(height: chartHeight)
            .chartXAxis {
                AxisMarks(values: .stride(by: .day, count: 7)) { _ in
                    AxisGridLine()
                    AxisValueLabel(format: .dateTime.month(.abbreviated).day())
                }
            }
        }
    }

    private var modelTypesSection: some View {
        Section("Top Model Types") {
            Chart(viewModel.topModelTypes.prefix(5), id: \.name) { item in
                BarMark(
                    x: .value("Count", item.count),
                    y: .value("Type", item.name)
                )
                .foregroundStyle(theme.primary)
            }
            .frame(height: CGFloat(min(viewModel.topModelTypes.count, 5)) * barRowHeight)
        }
    }

    private var creatorsSection: some View {
        Section("Most Viewed Creators") {
            ForEach(viewModel.topCreators.prefix(5), id: \.name) { stat in
                RankedStatRow(name: stat.name, count: stat.count)
            }
        }
    }

    private var searchesSection: some View {
        Section("Top Searches") {
            ForEach(viewModel.topSearchQueries.prefix(5), id: \.name) { stat in
                RankedStatRow(name: stat.name, count: stat.count)
            }
        }
    }
}

private struct StatBadge: View {
    let label: String
    let value: Int

    var body: some View {
        VStack(spacing: Spacing.xs) {
            Text("\(value)")
                .font(.civitHeadlineSmall)
                .fontWeight(.bold)
                .foregroundColor(.civitOnSurface)
            Text(label)
                .font(.civitBodySmall)
                .foregroundColor(.civitOnSurfaceVariant)
        }
        .frame(maxWidth: .infinity)
    }
}

private struct RankedStatRow: View {
    let name: String
    let count: Int

    var body: some View {
        HStack {
            Text(name)
                .font(.civitBodyMedium)
                .lineLimit(1)
            Spacer()
            Text("\(count)")
                .font(.civitBodyMedium)
                .foregroundColor(.civitOnSurfaceVariant)
        }
    }
}
