// NOTE: This file belongs to the CivitDeckWidget extension target.
// Create the target in Xcode: File > New > Target > Widget Extension,
// then add this file and CivitDeckWidget.swift to that target.

import WidgetKit
import SwiftUI

@main
struct CivitDeckWidgetBundle: WidgetBundle {
    var body: some Widget {
        CivitDeckTrendingWidget()
    }
}
