import SwiftUI

extension View {
    @ViewBuilder
    func heroSource(id: Int64, in namespace: Namespace.ID) -> some View {
        if #available(iOS 18.0, *) {
            self.matchedTransitionSource(id: id, in: namespace)
        } else {
            self
        }
    }

    @ViewBuilder
    func applyHeroSource(id: Int64, in namespace: Namespace.ID?) -> some View {
        if let namespace {
            heroSource(id: id, in: namespace)
        } else {
            self
        }
    }

    @ViewBuilder
    func heroDestination(id: Int64, in namespace: Namespace.ID) -> some View {
        if #available(iOS 18.0, *) {
            self.navigationTransition(.zoom(sourceID: id, in: namespace))
        } else {
            self
        }
    }
}
