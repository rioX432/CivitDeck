import SwiftUI
import Shared

struct SettingsScreen: View {
    @StateObject private var viewModel = SettingsViewModel()

    var body: some View {
        NavigationStack {
            List {
                contentFilterSection
                displaySection
                dataManagementSection
                aboutSection
            }
            .navigationTitle("Settings")
            .navigationBarTitleDisplayMode(.inline)
            .task { await viewModel.observeNsfwFilter() }
            .task { await viewModel.observeSortOrder() }
            .task { await viewModel.observeTimePeriod() }
            .task { await viewModel.observeGridColumns() }
        }
    }

    private var contentFilterSection: some View {
        Section("Content Filter") {
            Toggle(isOn: Binding(
                get: { viewModel.nsfwFilterLevel != .off },
                set: { _ in viewModel.onNsfwFilterToggle() }
            )) {
                VStack(alignment: .leading, spacing: 4) {
                    Text("NSFW Content")
                        .font(.civitBodyMedium)
                    Text("Show NSFW content in search results")
                        .font(.civitBodySmall)
                        .foregroundColor(.civitOnSurfaceVariant)
                }
            }
        }
    }

    private var displaySection: some View {
        Section("Display") {
            sortOrderPicker
            timePeriodPicker
            gridColumnsPicker
        }
    }

    private var sortOrderPicker: some View {
        Picker("Default Sort", selection: Binding(
            get: { viewModel.defaultSortOrder },
            set: { viewModel.onSortOrderChanged($0) }
        )) {
            ForEach(SearchFilter.sortOptions, id: \.self) { sort in
                Text(SearchFilter.sortLabel(sort)).tag(sort)
            }
        }
    }

    private var timePeriodPicker: some View {
        Picker("Default Period", selection: Binding(
            get: { viewModel.defaultTimePeriod },
            set: { viewModel.onTimePeriodChanged($0) }
        )) {
            ForEach(SearchFilter.periodOptions, id: \.self) { period in
                Text(SearchFilter.periodLabel(period)).tag(period)
            }
        }
    }

    private var gridColumnsPicker: some View {
        Picker("Grid Columns", selection: Binding(
            get: { viewModel.gridColumns },
            set: { viewModel.onGridColumnsChanged($0) }
        )) {
            Text("2").tag(Int32(2))
            Text("3").tag(Int32(3))
        }
        .pickerStyle(.segmented)
    }

    private var dataManagementSection: some View {
        Section("Data Management") {
            NavigationLink {
                HiddenModelsView(
                    models: viewModel.hiddenModels,
                    onUnhide: viewModel.onUnhideModel
                )
            } label: {
                HStack {
                    Text("Hidden Models")
                    Spacer()
                    Text("\(viewModel.hiddenModels.count) models")
                        .font(.civitBodySmall)
                        .foregroundColor(.civitOnSurfaceVariant)
                }
            }
            NavigationLink {
                ExcludedTagsView(
                    tags: viewModel.excludedTags,
                    onAdd: viewModel.onAddExcludedTag,
                    onRemove: viewModel.onRemoveExcludedTag
                )
            } label: {
                HStack {
                    Text("Excluded Tags")
                    Spacer()
                    Text("\(viewModel.excludedTags.count) tags")
                        .font(.civitBodySmall)
                        .foregroundColor(.civitOnSurfaceVariant)
                }
            }
            ClearActionButton(label: "Clear Search History", action: viewModel.onClearSearchHistory)
            ClearActionButton(label: "Clear Browsing History", action: viewModel.onClearBrowsingHistory)
            ClearActionButton(label: "Clear Cache", action: viewModel.onClearCache)
        }
    }

    private var aboutSection: some View {
        Section("About") {
            HStack {
                Text("App Version")
                Spacer()
                Text(Bundle.main.infoDictionary?["CFBundleShortVersionString"] as? String ?? "1.0.0")
                    .foregroundColor(.civitOnSurfaceVariant)
            }
            NavigationLink("Open Source Licenses") {
                LicensesView()
            }
        }
    }
}

private struct ClearActionButton: View {
    let label: String
    let action: () -> Void
    @State private var showConfirmation = false

    var body: some View {
        Button(label) {
            showConfirmation = true
        }
        .foregroundColor(.civitError)
        .alert(label, isPresented: $showConfirmation) {
            Button("Cancel", role: .cancel) {}
            Button("Clear", role: .destructive) { action() }
        } message: {
            Text("Are you sure? This cannot be undone.")
        }
    }
}
