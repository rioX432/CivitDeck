import CoreSpotlight
import Shared

enum SpotlightItemBuilder {
    static let domainIdentifier = "com.riox432.civitdeck.favorites"

    static func build(from model: Core_domainFavoriteModelSummary) -> CSSearchableItem {
        let attributeSet = CSSearchableItemAttributeSet(contentType: .item)
        attributeSet.title = model.name
        attributeSet.contentDescription = model.type.name
        if let thumbUrl = model.thumbnailUrl, let url = URL(string: thumbUrl) {
            attributeSet.thumbnailURL = url
        }
        return CSSearchableItem(
            uniqueIdentifier: "\(model.id)",
            domainIdentifier: domainIdentifier,
            attributeSet: attributeSet
        )
    }
}
