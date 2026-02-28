import SwiftUI
import Shared

struct NotificationsSettingsView: View {
    @ObservedObject var viewModel: SettingsViewModelOwner

    var body: some View {
        List {
            Section("Alerts") {
                notificationsToggle
                if viewModel.notificationsEnabled {
                    pollingIntervalPicker
                }
            }
        }
        .navigationTitle("Notifications")
        .navigationBarTitleDisplayMode(.inline)
    }

    private var notificationsToggle: some View {
        Toggle(isOn: Binding(
            get: { viewModel.notificationsEnabled },
            set: { viewModel.onNotificationsEnabledChanged($0) }
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
            get: { viewModel.pollingInterval },
            set: { viewModel.onPollingIntervalChanged($0) }
        )) {
            ForEach(PollingInterval.allCases.filter { $0 != .off }, id: \.self) { interval in
                Text(interval.displayName).tag(interval)
            }
        }
    }
}
