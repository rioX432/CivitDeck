import SwiftUI
import Shared

struct ContentFilterSettingsView: View {
    @ObservedObject var viewModel: ContentFilterSettingsViewModelOwner
    @ObservedObject var displayViewModel: DisplaySettingsViewModelOwner

    var body: some View {
        List {
            Section("NSFW") {
                nsfwToggle
                if viewModel.nsfwFilterLevel != .off {
                    NsfwBlurSettingsSection(
                        settings: viewModel.nsfwBlurSettings,
                        onChanged: viewModel.onNsfwBlurSettingsChanged
                    )
                }
            }
            Section("Defaults") {
                sortOrderPicker
                timePeriodPicker
            }
            Section("Tags") {
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
            }
        }
        .navigationTitle("Content & Filters")
        .navigationBarTitleDisplayMode(.inline)
    }

    private var nsfwToggle: some View {
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

    private var sortOrderPicker: some View {
        Picker("Default Sort", selection: Binding(
            get: { displayViewModel.defaultSortOrder },
            set: { displayViewModel.onSortOrderChanged($0) }
        )) {
            ForEach(SearchFilter.sortOptions, id: \.self) { sort in
                Text(SearchFilter.sortLabel(sort)).tag(sort)
            }
        }
    }

    private var timePeriodPicker: some View {
        Picker("Default Period", selection: Binding(
            get: { displayViewModel.defaultTimePeriod },
            set: { displayViewModel.onTimePeriodChanged($0) }
        )) {
            ForEach(SearchFilter.periodOptions, id: \.self) { period in
                Text(SearchFilter.periodLabel(period)).tag(period)
            }
        }
    }
}

private struct NsfwBlurSettingsSection: View {
    let settings: NsfwBlurSettings
    let onChanged: (NsfwBlurSettings) -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: Spacing.sm) {
            Text("Blur Intensity")
                .font(.civitBodySmall)
                .foregroundColor(.civitOnSurfaceVariant)
            // swiftlint:disable:next line_length
            Text("Controls blur strength for NSFW images in the Image Gallery. Tap any blurred image to reveal it temporarily.")
                .font(.civitBodySmall)
                .foregroundColor(.civitOnSurfaceVariant)
            BlurSliderRow(label: "Soft", intensity: Int(settings.softIntensity)) { value in
                onChanged(NsfwBlurSettings(
                    softIntensity: Int32(value),
                    matureIntensity: settings.matureIntensity,
                    explicitIntensity: settings.explicitIntensity
                ))
            }
            BlurSliderRow(label: "Mature", intensity: Int(settings.matureIntensity)) { value in
                onChanged(NsfwBlurSettings(
                    softIntensity: settings.softIntensity,
                    matureIntensity: Int32(value),
                    explicitIntensity: settings.explicitIntensity
                ))
            }
            BlurSliderRow(label: "Explicit", intensity: Int(settings.explicitIntensity)) { value in
                onChanged(NsfwBlurSettings(
                    softIntensity: settings.softIntensity,
                    matureIntensity: settings.matureIntensity,
                    explicitIntensity: Int32(value)
                ))
            }
        }
    }
}

private struct BlurSliderRow: View {
    let label: String
    let intensity: Int
    let onChanged: (Int) -> Void

    var body: some View {
        VStack(spacing: 2) {
            HStack {
                Text(label)
                    .font(.civitBodyMedium)
                Spacer()
                Text(intensity == 0 ? "Hidden" : "\(intensity)%")
                    .font(.civitBodySmall)
                    .foregroundColor(.civitOnSurfaceVariant)
            }
            Slider(
                value: Binding(
                    get: { Double(intensity) },
                    set: { onChanged(Int($0)) }
                ),
                in: 0...100,
                step: 25
            )
        }
    }
}
