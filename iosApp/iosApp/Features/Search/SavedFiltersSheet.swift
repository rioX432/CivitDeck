import SwiftUI
import Shared

struct SavedFiltersSheet: View {
    let savedFilters: [SavedSearchFilter]
    let onApply: (SavedSearchFilter) -> Void
    let onDelete: (Int64) -> Void
    let onDismiss: () -> Void

    var body: some View {
        NavigationStack {
            filterContent
                .navigationTitle("Saved Filters")
                .navigationBarTitleDisplayMode(.inline)
                .toolbar {
                    ToolbarItem(placement: .navigationBarTrailing) {
                        Button("Done") { onDismiss() }
                            .fontWeight(.semibold)
                    }
                }
        }
        .presentationDetents([.medium, .large])
    }

    @ViewBuilder
    private var filterContent: some View {
        if savedFilters.isEmpty {
            Text("No saved filters yet.")
                .font(.civitBodyMedium)
                .foregroundColor(.civitOnSurfaceVariant)
                .frame(maxWidth: .infinity, maxHeight: .infinity)
        } else {
            List {
                ForEach(savedFilters, id: \.id) { filter in
                    Button {
                        onApply(filter)
                        onDismiss()
                    } label: {
                        VStack(alignment: .leading, spacing: Spacing.xs) {
                            Text(filter.name)
                                .font(.civitBodyMedium)
                                .foregroundColor(.civitOnSurface)
                                .lineLimit(1)
                            Text(filterSummary(filter))
                                .font(.civitBodySmall)
                                .foregroundColor(.civitOnSurfaceVariant)
                                .lineLimit(1)
                        }
                        .contentShape(Rectangle())
                    }
                    .buttonStyle(.plain)
                    .swipeActions(edge: .trailing) {
                        Button(role: .destructive) {
                            onDelete(filter.id)
                        } label: {
                            Label("Delete", systemImage: "trash")
                        }
                    }
                }
            }
            .listStyle(.plain)
        }
    }

    private func filterSummary(_ filter: SavedSearchFilter) -> String {
        var parts: [String] = []
        if let type = filter.selectedType {
            parts.append(type.name)
        }
        parts.append(filter.selectedSort.name)
        let baseModels = filter.selectedBaseModels.compactMap { $0 as? BaseModel }
        if !baseModels.isEmpty {
            parts.append(baseModels.map { $0.displayName }.joined(separator: ", "))
        }
        if !filter.query.isEmpty {
            parts.append("\"\(filter.query)\"")
        }
        return parts.isEmpty ? "All models" : parts.joined(separator: " · ")
    }
}
