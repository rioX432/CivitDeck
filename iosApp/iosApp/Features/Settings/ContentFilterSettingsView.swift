import SwiftUI
import Shared

struct ContentFilterSettingsView: View {
    let viewModel: ContentFilterSettingsViewModel
    let displayViewModel: DisplaySettingsViewModel
    let appBehaviorViewModel: AppBehaviorSettingsViewModel

    var body: some View {
        Observing(
            viewModel.uiState,
            displayViewModel.uiState,
            appBehaviorViewModel.uiState
        ) {
            ProgressView()
        } content: { filterState, displayState, behaviorState in
            List {
                Section("NSFW") {
                    nsfwToggle(state: filterState)
                    if filterState.nsfwFilterLevel != .off {
                        NsfwBlurSettingsSection(
                            settings: filterState.nsfwBlurSettings,
                            onChanged: { viewModel.onNsfwBlurSettingsChanged(settings: $0) }
                        )
                    }
                }
                Section("Defaults") {
                    sortOrderPicker(state: displayState)
                    timePeriodPicker(state: displayState)
                }
                Section("Tags") {
                    let excludedTags = filterState.excludedTags as? [String] ?? []
                    NavigationLink {
                        ExcludedTagsView(
                            tags: excludedTags,
                            onAdd: { viewModel.onAddExcludedTag(tag: $0) },
                            onRemove: { viewModel.onRemoveExcludedTag(tag: $0) }
                        )
                    } label: {
                        HStack {
                            Text("Excluded Tags")
                            Spacer()
                            Text("\(excludedTags.count) tags")
                                .font(.civitBodySmall)
                                .foregroundColor(.civitOnSurfaceVariant)
                        }
                    }
                }
                feedQualitySection(state: behaviorState)
                notificationsSection(state: behaviorState)
            }
            .navigationTitle("Content & Behavior")
            .navigationBarTitleDisplayMode(.inline)
        }
    }

    private func nsfwToggle(state: ContentFilterSettingsUiState) -> some View {
        Toggle(isOn: Binding(
            get: { state.nsfwFilterLevel != .off },
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

    private func sortOrderPicker(state: DisplaySettingsUiState) -> some View {
        Picker("Default Sort", selection: Binding(
            get: { state.defaultSortOrder },
            set: { displayViewModel.onSortOrderChanged(sort: $0) }
        )) {
            ForEach(SearchFilter.sortOptions, id: \.self) { sort in
                Text(SearchFilter.sortLabel(sort)).tag(sort)
            }
        }
    }

    private func timePeriodPicker(state: DisplaySettingsUiState) -> some View {
        Picker("Default Period", selection: Binding(
            get: { state.defaultTimePeriod },
            set: { displayViewModel.onTimePeriodChanged(period: $0) }
        )) {
            ForEach(SearchFilter.periodOptions, id: \.self) { period in
                Text(SearchFilter.periodLabel(period)).tag(period)
            }
        }
    }

    private func feedQualitySection(state: AppBehaviorSettingsUiState) -> some View {
        Section("Search Quality Filter") {
            VStack(alignment: .leading, spacing: Spacing.xs) {
                HStack {
                    Text("Quality Threshold")
                        .font(.civitBodyMedium)
                    Spacer()
                    Text(state.feedQualityThreshold == 0 ? "Off" : "\(state.feedQualityThreshold)")
                        .font(.civitBodySmall)
                        .foregroundColor(.civitOnSurfaceVariant)
                }
                Text("Threshold for the quality filter in search results")
                    .font(.civitBodySmall)
                    .foregroundColor(.civitOnSurfaceVariant)
                Slider(
                    value: Binding(
                        get: { Double(state.feedQualityThreshold) },
                        set: { appBehaviorViewModel.onFeedQualityThresholdChanged(threshold: Int32($0)) }
                    ),
                    in: 0...100,
                    step: 1
                )
            }
        }
    }

    private func notificationsSection(state: AppBehaviorSettingsUiState) -> some View {
        Section("Notifications") {
            notificationsToggle(state: state)
            if state.notificationsEnabled {
                pollingIntervalPicker(state: state)
            }
        }
    }

    private func notificationsToggle(state: AppBehaviorSettingsUiState) -> some View {
        Toggle(isOn: Binding(
            get: { state.notificationsEnabled },
            set: { appBehaviorViewModel.onNotificationsEnabledChanged(enabled: $0) }
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

    private func pollingIntervalPicker(state: AppBehaviorSettingsUiState) -> some View {
        Picker("Check Frequency", selection: Binding(
            get: { state.pollingInterval },
            set: { appBehaviorViewModel.onPollingIntervalChanged(interval: $0) }
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
