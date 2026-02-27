import Foundation
import WidgetKit
import Shared

// App Group suite shared with CivitDeckWidget extension.
// Must match WidgetConstants.appGroupSuite in the widget target.
private let widgetAppGroup = "group.com.riox432.civitdeck"

enum WidgetDataWriter {
    static func writeTrendingModel(id: Int64, name: String, thumbnailURL: String?) {
        let defaults = UserDefaults(suiteName: widgetAppGroup)
        defaults?.set(id, forKey: "widget_model_id")
        defaults?.set(name, forKey: "widget_model_name")
        if let url = thumbnailURL {
            defaults?.set(url, forKey: "widget_model_thumb")
        } else {
            defaults?.removeObject(forKey: "widget_model_thumb")
        }
        WidgetCenter.shared.reloadTimelines(ofKind: "CivitDeckTrendingWidget")
    }

    static func writeTrendingModel(from sections: [RecommendationSection]) {
        guard let firstModel = sections.compactMap({ $0.models.first }).first else { return }
        let thumbUrl = firstModel.modelVersions.first?.images.first?.url
        writeTrendingModel(id: firstModel.id, name: firstModel.name, thumbnailURL: thumbUrl)
    }
}
