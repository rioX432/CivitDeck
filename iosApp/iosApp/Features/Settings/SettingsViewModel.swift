import Foundation
import Shared

/// Thin iOS wrapper around the shared Kotlin SettingsViewModel.
/// Subscribes to uiState and maps fields to @Published properties.
/// All business logic lives in Shared.SettingsViewModel.
@MainActor
final class SettingsViewModelOwner: ObservableObject {
    @Published var nsfwFilterLevel: NsfwFilterLevel = .off
    @Published var nsfwBlurSettings: NsfwBlurSettings = NsfwBlurSettings(
        softIntensity: 75, matureIntensity: 25, explicitIntensity: 0
    )
    @Published var defaultSortOrder: CivitSortOrder = .mostDownloaded
    @Published var defaultTimePeriod: TimePeriod = .allTime
    @Published var gridColumns: Int32 = 2
    @Published var hiddenModels: [Core_domainHiddenModel] = []
    @Published var excludedTags: [String] = []
    @Published var apiKey: String?
    @Published var connectedUsername: String?
    @Published var isValidatingApiKey: Bool = false
    @Published var apiKeyError: String?
    @Published var powerUserMode: Bool = false
    @Published var accentColor: AccentColor = .blue
    @Published var amoledDarkMode: Bool = false
    @Published var themeMode: ThemeMode = .system
    @Published var isOnline: Bool = true
    @Published var offlineCacheEnabled: Bool = true
    @Published var cacheSizeLimitMb: Int32 = 200
    @Published var cacheEntryCount: Int32 = 0
    @Published var cacheFormattedSize: String = "0 B"
    @Published var notificationsEnabled: Bool = false
    @Published var pollingInterval: PollingInterval = .off
    @Published var customNavShortcuts: [NavShortcut] = []

    private let vm: SettingsViewModel
    private let store: ViewModelStore

    init() {
        store = ViewModelStore()
        vm = KoinHelper.shared.createSettingsViewModel()
        store.put(key: "SettingsViewModel", viewModel: vm)
    }

    deinit {
        store.clear()
    }

    func observeUiState() async {
        for await state in vm.uiState {
            nsfwFilterLevel = state.nsfwFilterLevel
            nsfwBlurSettings = state.nsfwBlurSettings
            defaultSortOrder = state.defaultSortOrder
            defaultTimePeriod = state.defaultTimePeriod
            gridColumns = state.gridColumns
            hiddenModels = state.hiddenModels as? [Core_domainHiddenModel] ?? []
            excludedTags = state.excludedTags as? [String] ?? []
            apiKey = state.apiKey
            connectedUsername = state.connectedUsername
            isValidatingApiKey = state.isValidatingApiKey
            apiKeyError = state.apiKeyError
            powerUserMode = state.powerUserMode
            accentColor = state.accentColor
            amoledDarkMode = state.amoledDarkMode
            themeMode = state.themeMode
            isOnline = state.isOnline
            offlineCacheEnabled = state.offlineCacheEnabled
            cacheSizeLimitMb = state.cacheSizeLimitMb
            cacheEntryCount = Int32(state.cacheInfo.entryCount)
            cacheFormattedSize = state.cacheInfo.formattedSize
            notificationsEnabled = state.notificationsEnabled
            pollingInterval = state.pollingInterval
            customNavShortcuts = state.customNavShortcuts as? [NavShortcut] ?? []
        }
    }

    func onNsfwFilterToggle() { vm.onNsfwFilterToggle() }

    func onNsfwBlurSettingsChanged(_ settings: NsfwBlurSettings) {
        vm.onNsfwBlurSettingsChanged(settings: settings)
    }

    func onSortOrderChanged(_ sort: CivitSortOrder) {
        vm.onSortOrderChanged(sort: sort)
    }

    func onTimePeriodChanged(_ period: TimePeriod) {
        vm.onTimePeriodChanged(period: period)
    }

    func onGridColumnsChanged(_ columns: Int32) {
        vm.onGridColumnsChanged(columns: columns)
    }

    func onAccentColorChanged(_ color: AccentColor) {
        vm.onAccentColorChanged(color: color)
    }

    func onAmoledDarkModeChanged(_ enabled: Bool) {
        vm.onAmoledDarkModeChanged(enabled: enabled)
    }

    func onThemeModeChanged(_ mode: ThemeMode) {
        vm.onThemeModeChanged(mode: mode)
    }

    func onPowerUserModeChanged(_ enabled: Bool) {
        vm.onPowerUserModeChanged(enabled: enabled)
    }

    func onUnhideModel(_ modelId: Int64) {
        vm.onUnhideModel(modelId: modelId)
    }

    func onAddExcludedTag(_ tag: String) {
        vm.onAddExcludedTag(tag: tag)
    }

    func onRemoveExcludedTag(_ tag: String) {
        vm.onRemoveExcludedTag(tag: tag)
    }

    func onClearSearchHistory() { vm.onClearSearchHistory() }

    func onClearBrowsingHistory() { vm.onClearBrowsingHistory() }

    func onClearCache() { vm.onClearCache() }

    func onValidateAndSaveApiKey(_ key: String) {
        vm.onValidateAndSaveApiKey(apiKey: key)
    }

    func onClearApiKey() { vm.onClearApiKey() }

    func onNotificationsEnabledChanged(_ enabled: Bool) {
        vm.onNotificationsEnabledChanged(enabled: enabled)
    }

    func onPollingIntervalChanged(_ interval: PollingInterval) {
        vm.onPollingIntervalChanged(interval: interval)
    }

    func onOfflineCacheEnabledChanged(_ enabled: Bool) {
        vm.onOfflineCacheEnabledChanged(enabled: enabled)
    }

    func onCacheSizeLimitChanged(_ limitMb: Int32) {
        vm.onCacheSizeLimitChanged(limitMb: limitMb)
    }

    func onCustomNavShortcutsChanged(_ shortcuts: [NavShortcut]) {
        vm.onCustomNavShortcutsChanged(shortcuts: shortcuts)
    }
}
