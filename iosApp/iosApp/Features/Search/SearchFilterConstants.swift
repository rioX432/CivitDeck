import SwiftUI
import Shared

enum SearchFilter {
    static let baseModelOptions: [BaseModel] = [.sd15, .sdxl10, .pony, .flux1D, .flux1S, .sd21, .svd]
    static let sortOptions: [CivitSortOrder] = [.mostDownloaded, .highestRated, .newest]
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

struct HeaderHeightPreferenceKey: PreferenceKey {
    static var defaultValue: CGFloat = 0
    static func reduce(value: inout CGFloat, nextValue: () -> CGFloat) {
        value = nextValue()
    }
}

// MARK: - Recommendation Sections

struct RecommendationSectionsView: View {
    let recommendations: [RecommendationSection]
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
                        }
                    }
                    .padding(.horizontal, Spacing.md)
                }
            }
            .padding(.bottom, Spacing.sm)
        }
    }
}
