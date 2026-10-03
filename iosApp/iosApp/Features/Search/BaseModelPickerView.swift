import SwiftUI
import Shared

/// The filter sheet's "Base model" row: the applied selection as removable chips and a button
/// that opens ``BaseModelPickerView``.
struct BaseModelFilterRow: View {
    let selection: Set<BaseModel>
    let catalog: Core_domainBaseModelCatalog?
    let onApply: (Set<BaseModel>) -> Void
    @State private var showPicker = false
    @Environment(\.civitTheme) private var theme

    var body: some View {
        VStack(alignment: .leading, spacing: Spacing.xs) {
            HStack(spacing: Spacing.sm) {
                Text("search_base_model_title")
                    .font(.civitLabelMedium)
                    .foregroundColor(.civitOnSurfaceVariant)
                if selection.isEmpty {
                    Text("search_base_model_any")
                        .font(.civitBodySmall)
                        .foregroundColor(.civitOnSurfaceVariant)
                }
                Spacer()
                chooseButton
            }
            .padding(.horizontal, Spacing.lg)
            if !selection.isEmpty {
                selectedChips
            }
        }
        .sheet(isPresented: $showPicker) {
            BaseModelPickerView(catalog: catalog, initialSelection: selection, onApply: onApply)
        }
    }

    private var chooseButton: some View {
        Button {
            showPicker = true
        } label: {
            Group {
                if selection.isEmpty {
                    Text("search_base_model_choose")
                } else {
                    Text("search_base_model_choose_count \(selection.count)")
                }
            }
            .font(.civitLabelMedium)
            .foregroundColor(theme.primary)
            .frame(minHeight: 44)
            .contentShape(Rectangle())
        }
    }

    private var selectedChips: some View {
        ScrollView(.horizontal, showsIndicators: false) {
            HStack(spacing: Spacing.xs) {
                ForEach(BaseModelOrdering.inCatalogOrder(selection, catalog: catalog), id: \.self) { baseModel in
                    selectedChip(baseModel)
                }
            }
            .padding(.horizontal, Spacing.lg)
        }
    }

    private func selectedChip(_ baseModel: BaseModel) -> some View {
        Button {
            onApply(selection.subtracting([baseModel]))
        } label: {
            HStack(spacing: Spacing.xs) {
                Text(verbatim: baseModel.displayName)
                    .font(.civitLabelMedium)
                BaseModelStatusMarker(status: catalog?.statusOf(baseModel: baseModel))
                Image(systemName: "xmark")
                    .font(.civitLabelXSmall)
                    .accessibilityHidden(true)
            }
            .padding(.horizontal, Spacing.md)
            .padding(.vertical, Spacing.sm)
            .background(theme.primary.opacity(0.2))
            .foregroundColor(theme.primary)
            .clipShape(Capsule())
            // The capsule stays compact; the frame extends the tap area to the 44 pt minimum.
            .frame(minHeight: 44)
            .contentShape(Rectangle())
        }
        .buttonStyle(.plain)
        .accessibilityLabel(Text("search_base_model_remove_a11y \(baseModel.displayName)"))
    }
}

/// Searchable multi-select over CivitAI's base model catalog. The selection stays local until
/// "Done", so the search runs one request per picker session instead of one per tap; "Cancel" and
/// swiping the sheet down discard it, as on Desktop.
struct BaseModelPickerView: View {
    let catalog: Core_domainBaseModelCatalog?
    let onApply: (Set<BaseModel>) -> Void
    @State private var selection: Set<BaseModel>
    @State private var query = ""
    @State private var isOlderExpanded = false
    @Environment(\.dismiss) private var dismiss
    @Environment(\.civitTheme) private var theme

    init(
        catalog: Core_domainBaseModelCatalog?,
        initialSelection: Set<BaseModel>,
        onApply: @escaping (Set<BaseModel>) -> Void
    ) {
        self.catalog = catalog
        self.onApply = onApply
        _selection = State(initialValue: initialSelection)
    }

    private var trimmedQuery: String {
        query.trimmingCharacters(in: .whitespacesAndNewlines)
    }

    private var isSearching: Bool { !trimmedQuery.isEmpty }

    private func matching(_ baseModels: [BaseModel]) -> [BaseModel] {
        guard isSearching else { return baseModels }
        return baseModels.filter { $0.displayName.range(of: trimmedQuery, options: .caseInsensitive) != nil }
    }

    var body: some View {
        NavigationStack {
            pickerList
                .navigationTitle(Text("base_model_picker_title"))
                .navigationBarTitleDisplayMode(.inline)
                .searchable(
                    text: $query,
                    placement: .navigationBarDrawer(displayMode: .always),
                    prompt: Text("base_model_picker_search_prompt")
                )
                .autocorrectionDisabled()
                .textInputAutocapitalization(.never)
                .toolbar {
                    ToolbarItem(placement: .navigationBarLeading) {
                        Button("action_cancel") { dismiss() }
                    }
                    ToolbarItem(placement: .navigationBarTrailing) {
                        HStack(spacing: Spacing.xs) {
                            Button("action_clear") { selection = [] }
                                .disabled(selection.isEmpty)
                            Button("action_done") {
                                onApply(selection)
                                dismiss()
                            }
                            .fontWeight(.semibold)
                        }
                    }
                }
        }
    }

    private var pickerList: some View {
        let selected = matching(BaseModelOrdering.inCatalogOrder(selection, catalog: catalog))
        let current = matching(catalog?.active ?? [])
        let older = matching(catalog?.retired ?? [])
        return List {
            if !selected.isEmpty {
                Section {
                    ForEach(selected, id: \.self) { row($0) }
                } header: {
                    Text("base_model_picker_section_selected")
                }
            }
            if catalog == nil {
                Section {
                    HStack(spacing: Spacing.sm) {
                        ProgressView()
                        Text("base_model_picker_loading").foregroundColor(.civitOnSurfaceVariant)
                    }
                }
            } else if isSearching && selected.isEmpty && current.isEmpty && older.isEmpty {
                Section {
                    Text("base_model_picker_no_results").foregroundColor(.civitOnSurfaceVariant)
                }
            }
            if !current.isEmpty {
                Section {
                    ForEach(current, id: \.self) { row($0) }
                } header: {
                    Text("base_model_picker_section_current")
                }
            }
            if !older.isEmpty {
                olderSection(older)
            }
        }
        .listStyle(.insetGrouped)
    }

    private func olderSection(_ older: [BaseModel]) -> some View {
        Section {
            // Expanded while searching so a match in the older list is never hidden.
            DisclosureGroup(
                isExpanded: Binding(
                    get: { isOlderExpanded || isSearching },
                    set: { isOlderExpanded = $0 }
                ),
                content: {
                    ForEach(older, id: \.self) { row($0) }
                },
                label: {
                    HStack {
                        Text("base_model_picker_section_older")
                        Spacer()
                        Text(older.count, format: .number).foregroundColor(.civitOnSurfaceVariant)
                    }
                }
            )
        }
    }

    private func row(_ baseModel: BaseModel) -> some View {
        let isSelected = selection.contains(baseModel)
        return Button {
            if isSelected {
                selection.remove(baseModel)
            } else {
                selection.insert(baseModel)
            }
        } label: {
            HStack(spacing: Spacing.sm) {
                Text(verbatim: baseModel.displayName)
                    .foregroundColor(.civitOnSurface)
                BaseModelStatusMarker(status: catalog?.statusOf(baseModel: baseModel))
                Spacer()
                if isSelected {
                    Image(systemName: "checkmark")
                        .foregroundColor(theme.primary)
                        .accessibilityHidden(true)
                }
            }
            .contentShape(Rectangle())
        }
        .accessibilityAddTraits(isSelected ? .isSelected : [])
    }
}

/// "retired" / "unknown" next to a base model label. Nothing is shown for an active value, or
/// before the catalog has loaded, when the status is not known yet.
private struct BaseModelStatusMarker: View {
    let status: Core_domainBaseModelStatus?

    var body: some View {
        switch status {
        case .retired:
            marker(Text("search_base_model_marker_retired"))
        case .unknown:
            marker(Text("search_base_model_marker_unknown"))
        default:
            EmptyView()
        }
    }

    private func marker(_ text: Text) -> some View {
        text
            .font(.civitLabelSmall)
            .foregroundColor(.civitOnSurfaceVariant)
    }
}

private enum BaseModelOrdering {
    /// The catalog's API order (active, then retired), then values outside the catalog by label:
    /// a Swift `Set` has no stable order of its own.
    static func inCatalogOrder(_ baseModels: Set<BaseModel>, catalog: Core_domainBaseModelCatalog?) -> [BaseModel] {
        let known = (catalog?.active ?? []) + (catalog?.retired ?? [])
        let ordered = known.filter { baseModels.contains($0) }
        let rest = baseModels.subtracting(ordered).sorted { $0.apiValue < $1.apiValue }
        return ordered + rest
    }
}
