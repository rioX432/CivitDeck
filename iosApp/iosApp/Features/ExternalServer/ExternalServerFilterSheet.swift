import SwiftUI
import Shared

struct ExternalServerFilterSheet: View {
    let filters: ExternalServerImageFilters
    let onApply: (ExternalServerImageFilters) -> Void
    let onReset: () -> Void

    @Environment(\.dismiss) private var dismiss
    @State private var search: String
    @State private var character: String
    @State private var scenario: String
    @State private var sort: String
    @State private var nsfw: String

    init(
        filters: ExternalServerImageFilters,
        onApply: @escaping (ExternalServerImageFilters) -> Void,
        onReset: @escaping () -> Void
    ) {
        self.filters = filters
        self.onApply = onApply
        self.onReset = onReset
        _search = State(initialValue: filters.search)
        _character = State(initialValue: filters.character)
        _scenario = State(initialValue: filters.scenario)
        _sort = State(initialValue: filters.sort)
        _nsfw = State(initialValue: filters.nsfw)
    }

    var body: some View {
        NavigationStack {
            Form {
                Section("Search") {
                    TextField("Search...", text: $search)
                }

                Section("Filters") {
                    TextField("Character", text: $character)
                    TextField("Scenario", text: $scenario)
                }

                Section("Sort") {
                    Picker("Sort by", selection: $sort) {
                        Text("Newest").tag("newest")
                        Text("Oldest").tag("oldest")
                        Text("Score").tag("score")
                    }
                    .pickerStyle(.segmented)
                }

                Section("Content") {
                    Picker("NSFW Filter", selection: $nsfw) {
                        Text("All").tag("")
                        Text("NSFW").tag("true")
                        Text("SFW").tag("false")
                    }
                    .pickerStyle(.segmented)
                }
            }
            .navigationTitle("Filters")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Reset") {
                        onReset()
                        dismiss()
                    }
                }
                ToolbarItem(placement: .confirmationAction) {
                    Button("Apply") {
                        let newFilters = ExternalServerImageFilters(
                            character: character.trimmingCharacters(in: .whitespaces),
                            scenario: scenario.trimmingCharacters(in: .whitespaces),
                            nsfw: nsfw,
                            status: "",
                            sort: sort,
                            search: search.trimmingCharacters(in: .whitespaces)
                        )
                        onApply(newFilters)
                        dismiss()
                    }
                }
            }
        }
        .presentationDetents([.medium, .large])
    }
}
