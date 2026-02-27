import Foundation
import Shared

@MainActor
final class SettingsViewModel: ObservableObject {
    @Published var nsfwFilterLevel: NsfwFilterLevel = .off
    @Published var nsfwBlurSettings: NsfwBlurSettings = NsfwBlurSettings(
        softIntensity: 75, matureIntensity: 25, explicitIntensity: 0
    )
    @Published var defaultSortOrder: CivitSortOrder = .mostDownloaded
    @Published var defaultTimePeriod: TimePeriod = .allTime
    @Published var gridColumns: Int32 = 2
    @Published var hiddenModels: [HiddenModelEntity] = []
    @Published var excludedTags: [String] = []
    @Published var apiKey: String?
    @Published var connectedUsername: String?
    @Published var isValidatingApiKey: Bool = false
    @Published var apiKeyError: String?
    @Published var powerUserMode: Bool = false
    @Published var accentColor: AccentColor = .blue
    @Published var amoledDarkMode: Bool = false
    @Published var isOnline: Bool = true
    @Published var offlineCacheEnabled: Bool = true
    @Published var cacheSizeLimitMb: Int32 = 200
    @Published var cacheEntryCount: Int32 = 0
    @Published var cacheFormattedSize: String = "0 B"

    private let observeNsfwFilterUseCase: ObserveNsfwFilterUseCase
    private let setNsfwFilterUseCase: SetNsfwFilterUseCase
    private let observeNsfwBlurSettingsUseCase: ObserveNsfwBlurSettingsUseCase
    private let setNsfwBlurSettingsUseCase: SetNsfwBlurSettingsUseCase
    private let observeDefaultSortOrderUseCase: ObserveDefaultSortOrderUseCase
    private let setDefaultSortOrderUseCase: SetDefaultSortOrderUseCase
    private let observeDefaultTimePeriodUseCase: ObserveDefaultTimePeriodUseCase
    private let setDefaultTimePeriodUseCase: SetDefaultTimePeriodUseCase
    private let observeGridColumnsUseCase: ObserveGridColumnsUseCase
    private let setGridColumnsUseCase: SetGridColumnsUseCase
    private let getHiddenModelsUseCase: GetHiddenModelsUseCase
    private let unhideModelUseCase: UnhideModelUseCase
    private let getExcludedTagsUseCase: GetExcludedTagsUseCase
    private let addExcludedTagUseCase: AddExcludedTagUseCase
    private let removeExcludedTagUseCase: RemoveExcludedTagUseCase
    private let clearSearchHistoryUseCase: ClearSearchHistoryUseCase
    private let clearBrowsingHistoryUseCase: ClearBrowsingHistoryUseCase
    private let clearCacheUseCase: ClearCacheUseCase
    private let observeApiKeyUseCase: ObserveApiKeyUseCase
    private let setApiKeyUseCase: SetApiKeyUseCase
    private let validateApiKeyUseCase: ValidateApiKeyUseCase
    private let observePowerUserModeUseCase: ObservePowerUserModeUseCase
    private let setPowerUserModeUseCase: SetPowerUserModeUseCase
    private let observeAccentColorUseCase: ObserveAccentColorUseCase
    private let setAccentColorUseCase: SetAccentColorUseCase
    private let observeAmoledDarkModeUseCase: ObserveAmoledDarkModeUseCase
    private let setAmoledDarkModeUseCase: SetAmoledDarkModeUseCase
    private let observeNetworkStatusUseCase: ObserveNetworkStatusUseCase
    private let observeOfflineCacheEnabledUseCase: ObserveOfflineCacheEnabledUseCase
    private let setOfflineCacheEnabledUseCase: SetOfflineCacheEnabledUseCase
    private let observeCacheSizeLimitUseCase: ObserveCacheSizeLimitUseCase
    private let setCacheSizeLimitUseCase: SetCacheSizeLimitUseCase
    private let getCacheInfoUseCase: GetCacheInfoUseCase
    private let evictCacheUseCase: EvictCacheUseCase

    init() {
        self.observeNsfwFilterUseCase = KoinHelper.shared.getObserveNsfwFilterUseCase()
        self.setNsfwFilterUseCase = KoinHelper.shared.getSetNsfwFilterUseCase()
        self.observeNsfwBlurSettingsUseCase = KoinHelper.shared.getObserveNsfwBlurSettingsUseCase()
        self.setNsfwBlurSettingsUseCase = KoinHelper.shared.getSetNsfwBlurSettingsUseCase()
        self.observeDefaultSortOrderUseCase = KoinHelper.shared.getObserveDefaultSortOrderUseCase()
        self.setDefaultSortOrderUseCase = KoinHelper.shared.getSetDefaultSortOrderUseCase()
        self.observeDefaultTimePeriodUseCase = KoinHelper.shared.getObserveDefaultTimePeriodUseCase()
        self.setDefaultTimePeriodUseCase = KoinHelper.shared.getSetDefaultTimePeriodUseCase()
        self.observeGridColumnsUseCase = KoinHelper.shared.getObserveGridColumnsUseCase()
        self.setGridColumnsUseCase = KoinHelper.shared.getSetGridColumnsUseCase()
        self.getHiddenModelsUseCase = KoinHelper.shared.getHiddenModelsUseCase()
        self.unhideModelUseCase = KoinHelper.shared.getUnhideModelUseCase()
        self.getExcludedTagsUseCase = KoinHelper.shared.getExcludedTagsUseCase()
        self.addExcludedTagUseCase = KoinHelper.shared.getAddExcludedTagUseCase()
        self.removeExcludedTagUseCase = KoinHelper.shared.getRemoveExcludedTagUseCase()
        self.clearSearchHistoryUseCase = KoinHelper.shared.getClearSearchHistoryUseCase()
        self.clearBrowsingHistoryUseCase = KoinHelper.shared.getClearBrowsingHistoryUseCase()
        self.clearCacheUseCase = KoinHelper.shared.getClearCacheUseCase()
        self.observeApiKeyUseCase = KoinHelper.shared.getObserveApiKeyUseCase()
        self.setApiKeyUseCase = KoinHelper.shared.getSetApiKeyUseCase()
        self.validateApiKeyUseCase = KoinHelper.shared.getValidateApiKeyUseCase()
        self.observePowerUserModeUseCase = KoinHelper.shared.getObservePowerUserModeUseCase()
        self.setPowerUserModeUseCase = KoinHelper.shared.getSetPowerUserModeUseCase()
        self.observeAccentColorUseCase = KoinHelper.shared.getObserveAccentColorUseCase()
        self.setAccentColorUseCase = KoinHelper.shared.getSetAccentColorUseCase()
        self.observeAmoledDarkModeUseCase = KoinHelper.shared.getObserveAmoledDarkModeUseCase()
        self.setAmoledDarkModeUseCase = KoinHelper.shared.getSetAmoledDarkModeUseCase()
        self.observeNetworkStatusUseCase = KoinHelper.shared.getObserveNetworkStatusUseCase()
        self.observeOfflineCacheEnabledUseCase = KoinHelper.shared.getObserveOfflineCacheEnabledUseCase()
        self.setOfflineCacheEnabledUseCase = KoinHelper.shared.getSetOfflineCacheEnabledUseCase()
        self.observeCacheSizeLimitUseCase = KoinHelper.shared.getObserveCacheSizeLimitUseCase()
        self.setCacheSizeLimitUseCase = KoinHelper.shared.getSetCacheSizeLimitUseCase()
        self.getCacheInfoUseCase = KoinHelper.shared.getCacheInfoUseCase()
        self.evictCacheUseCase = KoinHelper.shared.getEvictCacheUseCase()
        loadMutableData()
    }

    func observeNsfwFilter() async {
        let flow = SkieSwiftFlow<NsfwFilterLevel>(observeNsfwFilterUseCase.invoke())
        for await value in flow {
            nsfwFilterLevel = value
        }
    }

    func observeSortOrder() async {
        for await value in observeDefaultSortOrderUseCase.invoke() {
            defaultSortOrder = value
        }
    }

    func observeTimePeriod() async {
        for await value in observeDefaultTimePeriodUseCase.invoke() {
            defaultTimePeriod = value
        }
    }

    func observeGridColumns() async {
        for await value in observeGridColumnsUseCase.invoke() {
            gridColumns = value.int32Value
        }
    }

    func observeAccentColor() async {
        for await value in observeAccentColorUseCase.invoke() {
            accentColor = value
        }
    }

    func observeAmoledDarkMode() async {
        for await value in observeAmoledDarkModeUseCase.invoke() {
            amoledDarkMode = value.boolValue
        }
    }
    func observeNsfwBlurSettings() async {
        for await value in observeNsfwBlurSettingsUseCase.invoke() {
            nsfwBlurSettings = value
        }
    }

    func observeNetworkStatus() async {
        for await value in observeNetworkStatusUseCase.invoke() {
            isOnline = value.boolValue
        }
    }

    func observeOfflineCacheEnabled() async {
        for await value in observeOfflineCacheEnabledUseCase.invoke() {
            offlineCacheEnabled = value.boolValue
        }
    }

    func observeCacheSizeLimit() async {
        for await value in observeCacheSizeLimitUseCase.invoke() {
            cacheSizeLimitMb = value.int32Value
        }
    }

    func onNsfwFilterToggle() {
        let newLevel: NsfwFilterLevel = nsfwFilterLevel == .off ? .all : .off
        nsfwFilterLevel = newLevel
        Task { try? await setNsfwFilterUseCase.invoke(level: newLevel) }
    }

    func onNsfwBlurSettingsChanged(_ settings: NsfwBlurSettings) {
        nsfwBlurSettings = settings
        Task { try? await setNsfwBlurSettingsUseCase.invoke(settings: settings) }
    }

    func onSortOrderChanged(_ sort: CivitSortOrder) {
        Task { try? await setDefaultSortOrderUseCase.invoke(sort: sort) }
    }

    func onTimePeriodChanged(_ period: TimePeriod) {
        Task { try? await setDefaultTimePeriodUseCase.invoke(period: period) }
    }

    func onGridColumnsChanged(_ columns: Int32) {
        Task { try? await setGridColumnsUseCase.invoke(columns: columns) }
    }

    func onAccentColorChanged(_ color: AccentColor) {
        accentColor = color
        Task { try? await setAccentColorUseCase.invoke(color: color) }
    }

    func onAmoledDarkModeChanged(_ enabled: Bool) {
        amoledDarkMode = enabled
        Task { try? await setAmoledDarkModeUseCase.invoke(enabled: enabled) }
    }

    func onUnhideModel(_ modelId: Int64) {
        Task {
            try? await unhideModelUseCase.invoke(modelId: modelId)
            loadMutableData()
        }
    }

    func onAddExcludedTag(_ tag: String) {
        let trimmed = tag.trimmingCharacters(in: .whitespacesAndNewlines).lowercased()
        guard !trimmed.isEmpty else { return }
        Task {
            try? await addExcludedTagUseCase.invoke(tag: trimmed)
            loadMutableData()
        }
    }

    func onRemoveExcludedTag(_ tag: String) {
        Task {
            try? await removeExcludedTagUseCase.invoke(tag: tag)
            loadMutableData()
        }
    }

    func onClearSearchHistory() {
        Task { try? await clearSearchHistoryUseCase.invoke() }
    }

    func onClearBrowsingHistory() {
        Task { try? await clearBrowsingHistoryUseCase.invoke() }
    }

    func onClearCache() {
        Task {
            try? await clearCacheUseCase.invoke()
            await refreshCacheInfo()
        }
    }

    func observePowerUserMode() async {
        for await value in observePowerUserModeUseCase.invoke() {
            powerUserMode = value.boolValue
        }
    }

    func onPowerUserModeChanged(_ enabled: Bool) {
        powerUserMode = enabled
        Task { try? await setPowerUserModeUseCase.invoke(enabled: enabled) }
    }

    func observeApiKey() async {
        for await value in observeApiKeyUseCase.invoke() {
            apiKey = value as String?
        }
    }

    func onValidateAndSaveApiKey(_ key: String) {
        let trimmed = key.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !trimmed.isEmpty else { return }
        isValidatingApiKey = true
        apiKeyError = nil
        Task {
            do {
                let username = try await validateApiKeyUseCase.invoke(apiKey: trimmed)
                try await setApiKeyUseCase.invoke(apiKey: trimmed)
                connectedUsername = username
                isValidatingApiKey = false
            } catch {
                isValidatingApiKey = false
                apiKeyError = error.localizedDescription
            }
        }
    }

    func onClearApiKey() {
        Task {
            try? await setApiKeyUseCase.invoke(apiKey: nil)
            connectedUsername = nil
            apiKeyError = nil
        }
    }

    func onOfflineCacheEnabledChanged(_ enabled: Bool) {
        offlineCacheEnabled = enabled
        Task { try? await setOfflineCacheEnabledUseCase.invoke(enabled: enabled) }
    }

    func onCacheSizeLimitChanged(_ limitMb: Int32) {
        cacheSizeLimitMb = limitMb
        Task {
            try? await setCacheSizeLimitUseCase.invoke(limitMb: limitMb)
            try? await evictCacheUseCase.invoke(maxBytes: Int64(limitMb) * 1024 * 1024)
            await refreshCacheInfo()
        }
    }

    private func refreshCacheInfo() async {
        if let info = try? await getCacheInfoUseCase.invoke() {
            cacheEntryCount = Int32(info.entryCount)
            cacheFormattedSize = info.formattedSize
        }
    }

    private func loadMutableData() {
        Task {
            hiddenModels = (try? await getHiddenModelsUseCase.invoke()) ?? []
            excludedTags = (try? await getExcludedTagsUseCase.invoke()) ?? []
            await refreshCacheInfo()
        }
        Task {
            let observed = try? await observeApiKeyUseCase.invoke().first(where: { _ in true })
            guard let key = observed as? String else { return }
            connectedUsername = try? await validateApiKeyUseCase.invoke(apiKey: key)
        }
    }
}
