import SwiftUI

/// A masonry / waterfall layout that distributes items across columns,
/// placing each item in the shortest column to create a staggered effect.
struct StaggeredGrid<Data: RandomAccessCollection, ID: Hashable, Content: View>: View {
    private let data: Data
    private let columnCount: Int
    private let spacing: CGFloat
    private let idKeyPath: KeyPath<Data.Element, ID>
    private let content: (Data.Element) -> Content
    private let aspectRatio: (Data.Element) -> CGFloat

    init(
        data: Data,
        columnCount: Int,
        spacing: CGFloat = Spacing.sm,
        id: KeyPath<Data.Element, ID>,
        aspectRatio: @escaping (Data.Element) -> CGFloat,
        @ViewBuilder content: @escaping (Data.Element) -> Content
    ) {
        self.data = data
        self.columnCount = max(columnCount, 1)
        self.spacing = spacing
        self.idKeyPath = id
        self.aspectRatio = aspectRatio
        self.content = content
    }

    var body: some View {
        let columns = distributeItems()
        HStack(alignment: .top, spacing: spacing) {
            ForEach(0..<columnCount, id: \.self) { columnIndex in
                LazyVStack(spacing: spacing) {
                    ForEach(columns[columnIndex], id: idKeyPath) { item in
                        content(item)
                    }
                }
            }
        }
    }

    private func distributeItems() -> [[Data.Element]] {
        var columns: [[Data.Element]] = Array(repeating: [], count: columnCount)
        var heights: [CGFloat] = Array(repeating: 0, count: columnCount)

        for item in data {
            let shortestIndex = heights.enumerated()
                .min(by: { $0.element < $1.element })?.offset ?? 0

            columns[shortestIndex].append(item)

            let ratio = aspectRatio(item)
            let itemHeight: CGFloat = ratio > 0 ? 1.0 / ratio : 1.0
            heights[shortestIndex] += itemHeight + spacing
        }

        return columns
    }
}
