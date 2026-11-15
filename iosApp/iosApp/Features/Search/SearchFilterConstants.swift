import SwiftUI
import Shared

enum SearchFilter {
    static let sortOptions: [CivitSortOrder] = [.mostDownloaded, .highestRated, .newest, .quality]
    static let periodOptions: [TimePeriod] = [.allTime, .year, .month, .week, .day]

    static let modelTypeOptions: [ModelType] = [
        .checkpoint, .lora, .loCon, .controlnet,
        .textualInversion, .hypernetwork, .upscaler, .vae,
        .poses, .wildcards, .workflows, .motionModule,
        .aestheticGradient, .other,
    ]

    static func sortLabel(_ sort: CivitSortOrder) -> String {
        switch sort {
        case .highestRated: return "Highest Rated"
        case .mostDownloaded: return "Most Downloaded"
        case .newest: return "Newest"
        case .quality: return "Quality Score"
        default: return sort.name
        }
    }

    static func periodLabel(_ period: TimePeriod) -> String {
        switch period {
        case .allTime: return "All"
        case .year: return "Year"
        case .month: return "Month"
        case .week: return "Week"
        case .day: return "Day"
        }
    }
}

extension Core_domainModelSource {
    var displayLabel: String {
        switch self {
        case .civitai: return "CivitAI"
        case .huggingFace: return "HuggingFace"
        case .tensorArt: return "TensorArt"
        default: return name
        }
    }
}

/// Non-blocking provider notices shown above the results: at most one line per kind for each
/// provider, so a provider with several unsupported filters still takes one line for them.
struct SearchSourceNoticesView: View {
    private typealias NoticeReason = Feature_searchSearchSourceNoticeReason
    let notices: [Feature_searchSearchSourceNotice]

    var body: some View {
        if !notices.isEmpty {
            HStack(alignment: .top, spacing: Spacing.sm) {
                Image(systemName: "info.circle")
                    .accessibilityHidden(true)
                VStack(alignment: .leading, spacing: Spacing.xs) {
                    ForEach(lines, id: \.self) { line in
                        Text(line).font(.civitBodySmall)
                    }
                }
                Spacer(minLength: 0)
            }
            .padding(Spacing.md)
            .foregroundColor(.civitOnSecondaryContainer)
            .background(Color.civitSecondaryContainer)
            .clipShape(RoundedRectangle(cornerRadius: CornerRadius.card))
            .padding(.horizontal, Spacing.md)
            .padding(.bottom, Spacing.sm)
        }
    }

    private var lines: [String] {
        var sources: [Core_domainModelSource] = []
        for notice in notices where !sources.contains(notice.source) {
            sources.append(notice.source)
        }
        return sources.flatMap { source in
            Self.lines(source: source, reasons: Set(notices.filter { $0.source == source }.map(\.reason)))
        }
    }

    private static func lines(source: Core_domainModelSource, reasons: Set<NoticeReason>) -> [String] {
        let label = source.displayLabel
        let unsupported = NoticeReason.allCases.filter { reasons.contains($0) }.compactMap(filterLabel)
        var result: [String] = []
        if reasons.contains(.sourceFailed) {
            result.append(String(localized: "search_notice_source_failed \(label)"))
        }
        if !unsupported.isEmpty {
            let filters = unsupported.joined(separator: ", ")
            result.append(String(localized: "search_notice_unsupported_filters \(label) \(filters)"))
        }
        if reasons.contains(.unratedPreviewsHidden) {
            result.append(String(localized: "search_notice_previews_hidden \(label)"))
        }
        return result
    }

    private static func filterLabel(_ reason: NoticeReason) -> String? {
        switch reason {
        case .unsupportedSort: return String(localized: "search_notice_filter_sort")
        case .unsupportedPeriod: return String(localized: "search_notice_filter_period")
        case .unsupportedBaseModel: return String(localized: "search_notice_filter_base_model")
        case .unsupportedFreshFind: return String(localized: "search_notice_filter_fresh_only")
        case .unsupportedQualityFilter: return String(localized: "search_notice_filter_quality")
        case .sourceFailed, .unratedPreviewsHidden: return nil
        }
    }
}

struct HeaderHeightPreferenceKey: PreferenceKey {
    static var defaultValue: CGFloat = 0
    static func reduce(value: inout CGFloat, nextValue: () -> CGFloat) {
        value = nextValue()
    }
}

// MARK: - Recommendation Sections

struct RecommendationSectionsView: View {
    let recommendations: [RecommendationSection]
    // Records the recommendation-click signal before navigation; defaults to a no-op so
    // previews and other call sites without a view model still compile.
    var onRecommendationClick: (Int64) -> Void = { _ in }
    @Environment(\.horizontalSizeClass) private var sizeClass

    private var cardSize: CGSize {
        sizeClass == .regular ? CGSize(width: 200, height: 270) : CGSize(width: 160, height: 220)
    }

    var body: some View {
        ForEach(recommendations, id: \.title) { section in
            VStack(alignment: .leading, spacing: Spacing.xs) {
                Text(section.title)
                    .font(.civitTitleMedium)
                    .padding(.horizontal, Spacing.md)
                Text(section.reason)
                    .font(.civitBodySmall)
                    .foregroundColor(.civitOnSurfaceVariant)
                    .padding(.horizontal, Spacing.md)

                ScrollView(.horizontal, showsIndicators: false) {
                    HStack(spacing: Spacing.sm) {
                        ForEach(section.models, id: \.id) { model in
                            NavigationLink(value: model.id) {
                                ModelCardView(model: model)
                                    .frame(width: cardSize.width, height: cardSize.height)
                            }
                            .buttonStyle(.plain)
                            .simultaneousGesture(TapGesture().onEnded {
                                onRecommendationClick(model.id)
                            })
                        }
                    }
                    .padding(.horizontal, Spacing.md)
                }
            }
            .padding(.bottom, Spacing.sm)
        }
    }
}
