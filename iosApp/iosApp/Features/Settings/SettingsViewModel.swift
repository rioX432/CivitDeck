import Foundation
import Shared

// MARK: - ContentFilterSettingsViewModelOwner

@MainActor
final class ContentFilterSettingsViewModelOwner: ObservableObject {
    @Published var nsfwFilterLevel: NsfwFilterLevel = .off
    @Published var nsfwBlurSettings: NsfwBlurSettings = NsfwBlurSettings(
        softIntensity: 75, matureIntensity: 25, explicitIntensity: 0
    )
    @Published var hiddenModels: [Core_domainHiddenModel] = []
    @Published var excludedTags: [String] = []

    private let vm: ContentFilterSettingsViewModel
    private let store: ViewModelStore

    init() {
        store = ViewModelStore()
        vm = KoinHelper.shared.createContentFilterSettingsViewModel()
        store.put(key: "ContentFilterSettingsViewModel", viewModel: vm)
    }

    deinit { store.clear() }

    func observeUiState() async {
        for await state in vm.uiState {
            nsfwFilterLevel = state.nsfwFilterLevel
            nsfwBlurSettings = state.nsfwBlurSettings
            hiddenModels = state.hiddenModels as? [Core_domainHiddenModel] ?? []
            excludedTags = state.excludedTags as? [String] ?? []
        }
    }

    func onNsfwFilterToggle() { vm.onNsfwFilterToggle() }

    func onNsfwBlurSettingsChanged(_ settings: NsfwBlurSettings) {
        vm.onNsfwBlurSettingsChanged(settings: settings)
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
}

// MARK: - DisplaySettingsViewModelOwner

@MainActor
final class DisplaySettingsViewModelOwner: ObservableObject {
    @Published var defaultSortOrder: CivitSortOrder = .mostDownloaded
    @Published var defaultTimePeriod: TimePeriod = .allTime
    @Published var gridColumns: Int32 = 2
    @Published var accentColor: AccentColor = .blue
    @Published var amoledDarkMode: Bool = false
    @Published var themeMode: ThemeMode = .system
    @Published var customNavShortcuts: [NavShortcut] = []

    private let vm: DisplaySettingsViewModel
    private let store: ViewModelStore

    init() {
        store = ViewModelStore()
        vm = KoinHelper.shared.createDisplaySettingsViewModel()
        store.put(key: "DisplaySettingsViewModel", viewModel: vm)
    }

    deinit { store.clear() }

    func observeUiState() async {
        for await state in vm.uiState {
            defaultSortOrder = state.defaultSortOrder
            defaultTimePeriod = state.defaultTimePeriod
            gridColumns = state.gridColumns
            accentColor = state.accentColor
            amoledDarkMode = state.amoledDarkMode
            themeMode = state.themeMode
            customNavShortcuts = state.customNavShortcuts as? [NavShortcut] ?? []
        }
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

    func onCustomNavShortcutsChanged(_ shortcuts: [NavShortcut]) {
        vm.onCustomNavShortcutsChanged(shortcuts: shortcuts)
    }
}

// MARK: - AppBehaviorSettingsViewModelOwner

@MainActor
final class AppBehaviorSettingsViewModelOwner: ObservableObject {
    @Published var powerUserMode: Bool = false
    @Published var notificationsEnabled: Bool = false
    @Published var pollingInterval: PollingInterval = .off

    private let vm: AppBehaviorSettingsViewModel
    private let store: ViewModelStore

    init() {
        store = ViewModelStore()
        vm = KoinHelper.shared.createAppBehaviorSettingsViewModel()
        store.put(key: "AppBehaviorSettingsViewModel", viewModel: vm)
    }

    deinit { store.clear() }

    func observeUiState() async {
        for await state in vm.uiState {
            powerUserMode = state.powerUserMode
            notificationsEnabled = state.notificationsEnabled
            pollingInterval = state.pollingInterval
        }
    }

    func onPowerUserModeChanged(_ enabled: Bool) {
        vm.onPowerUserModeChanged(enabled: enabled)
    }

    func onNotificationsEnabledChanged(_ enabled: Bool) {
        vm.onNotificationsEnabledChanged(enabled: enabled)
    }

    func onPollingIntervalChanged(_ interval: PollingInterval) {
        vm.onPollingIntervalChanged(interval: interval)
    }
}

// MARK: - AuthSettingsViewModelOwner

@MainActor
final class AuthSettingsViewModelOwner: ObservableObject {
    @Published var apiKey: String?
    @Published var connectedUsername: String?
    @Published var isValidatingApiKey: Bool = false
    @Published var apiKeyError: String?

    private let vm: AuthSettingsViewModel
    private let store: ViewModelStore

    init() {
        store = ViewModelStore()
        vm = KoinHelper.shared.createAuthSettingsViewModel()
        store.put(key: "AuthSettingsViewModel", viewModel: vm)
    }

    deinit { store.clear() }

    func observeUiState() async {
        for await state in vm.uiState {
            apiKey = state.apiKey
            connectedUsername = state.connectedUsername
            isValidatingApiKey = state.isValidatingApiKey
            apiKeyError = state.apiKeyError
        }
    }

    func onValidateAndSaveApiKey(_ key: String) {
        vm.onValidateAndSaveApiKey(apiKey: key)
    }

    func onClearApiKey() { vm.onClearApiKey() }
}

// MARK: - StorageSettingsViewModelOwner

@MainActor
final class StorageSettingsViewModelOwner: ObservableObject {
    @Published var isOnline: Bool = true
    @Published var offlineCacheEnabled: Bool = true
    @Published var cacheSizeLimitMb: Int32 = 200
    @Published var cacheEntryCount: Int32 = 0
    @Published var cacheFormattedSize: String = "0 B"
    @Published var hiddenModels: [Core_domainHiddenModel] = []

    private let vm: StorageSettingsViewModel
    private let store: ViewModelStore

    init() {
        store = ViewModelStore()
        vm = KoinHelper.shared.createStorageSettingsViewModel()
        store.put(key: "StorageSettingsViewModel", viewModel: vm)
    }

    deinit { store.clear() }

    func observeUiState() async {
        for await state in vm.uiState {
            isOnline = state.isOnline
            offlineCacheEnabled = state.offlineCacheEnabled
            cacheSizeLimitMb = state.cacheSizeLimitMb
            cacheEntryCount = Int32(state.cacheInfo.entryCount)
            cacheFormattedSize = state.cacheInfo.formattedSize
            hiddenModels = state.hiddenModels as? [Core_domainHiddenModel] ?? []
        }
    }

    func onOfflineCacheEnabledChanged(_ enabled: Bool) {
        vm.onOfflineCacheEnabledChanged(enabled: enabled)
    }

    func onCacheSizeLimitChanged(_ limitMb: Int32) {
        vm.onCacheSizeLimitChanged(limitMb: limitMb)
    }

    func onClearSearchHistory() { vm.onClearSearchHistory() }

    func onClearBrowsingHistory() { vm.onClearBrowsingHistory() }

    func onClearCache() { vm.onClearCache() }

    func onUnhideModel(_ modelId: Int64) {
        vm.onUnhideModel(modelId: modelId)
    }
}
