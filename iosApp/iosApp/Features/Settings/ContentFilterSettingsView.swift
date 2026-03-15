import SwiftUI
import Shared

struct ContentFilterSettingsView: View {
    @ObservedObject var viewModel: ContentFilterSettingsViewModelOwner
    @ObservedObject var displayViewModel: DisplaySettingsViewModelOwner
    @ObservedObject var appBehaviorViewModel: AppBehaviorSettingsViewModelOwner

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
            feedQualitySection
            notificationsSection
        }
        .navigationTitle("Content & Behavior")
        .navigationBarTitleDisplayMode(.inline)
    }

    private var nsfwToggle: some View {
        Toggle(isOn: Binding(
            get: { viewModel.nsfwFilterLevel != .off },
            set: { _ in viewModel.onNsfwFilterToggle() }
        )) {
            VStack(alignment: .leading, spacing: Spacing.xs) {
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
            set: { displayViewModel.defaultSortOrder = $0; displayViewModel.onSortOrderChanged($0) }
        )) {
            ForEach(SearchFilter.sortOptions, id: \.self) { sort in
                Text(SearchFilter.sortLabel(sort)).tag(sort)
            }
        }
    }

    private var timePeriodPicker: some View {
        Picker("Default Period", selection: Binding(
            get: { displayViewModel.defaultTimePeriod },
            set: { displayViewModel.defaultTimePeriod = $0; displayViewModel.onTimePeriodChanged($0) }
        )) {
            ForEach(SearchFilter.periodOptions, id: \.self) { period in
                Text(SearchFilter.periodLabel(period)).tag(period)
            }
        }
    }

    private var feedQualitySection: some View {
        Section("Search Quality Filter") {
            VStack(alignment: .leading, spacing: Spacing.xs) {
                HStack {
                    Text("Quality Threshold")
                        .font(.civitBodyMedium)
                    Spacer()
                    Text(appBehaviorViewModel.feedQualityThreshold == 0 ? "Off" : "\(appBehaviorViewModel.feedQualityThreshold)")
                        .font(.civitBodySmall)
                        .foregroundColor(.civitOnSurfaceVariant)
                }
                Text("Threshold for the quality filter in search results")
                    .font(.civitBodySmall)
                    .foregroundColor(.civitOnSurfaceVariant)
                Slider(
                    value: Binding(
                        get: { Double(appBehaviorViewModel.feedQualityThreshold) },
                        set: {
                            appBehaviorViewModel.feedQualityThreshold = Int32($0)
                            appBehaviorViewModel.onFeedQualityThresholdChanged(Int32($0))
                        }
                    ),
                    in: 0...100,
                    step: 1
                )
            }
        }
    }

    private var notificationsSection: some View {
        Section("Notifications") {
            notificationsToggle
            if appBehaviorViewModel.notificationsEnabled {
                pollingIntervalPicker
            }
        }
    }

    private var notificationsToggle: some View {
        Toggle(isOn: Binding(
            get: { appBehaviorViewModel.notificationsEnabled },
            set: {
                appBehaviorViewModel.notificationsEnabled = $0
                appBehaviorViewModel.onNotificationsEnabledChanged($0)
            }
        )) {
            VStack(alignment: .leading, spacing: Spacing.xs) {
                Text("Model Update Alerts")
                    .font(.civitBodyMedium)
                Text("Notify when favorited models get new versions")
                    .font(.civitBodySmall)
                    .foregroundColor(.civitOnSurfaceVariant)
            }
        }
    }

    private var pollingIntervalPicker: some View {
        Picker("Check Frequency", selection: Binding(
            get: { appBehaviorViewModel.pollingInterval },
            set: {
                appBehaviorViewModel.pollingInterval = $0
                appBehaviorViewModel.onPollingIntervalChanged($0)
            }
        )) {
            ForEach(PollingInterval.allCases.filter { $0 != .off }, id: \.self) { interval in
                Text(interval.displayName).tag(interval)
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
        VStack(spacing: Spacing.xxs) {
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
