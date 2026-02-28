import SwiftUI
import Shared

struct AppearanceSettingsView: View {
    @ObservedObject var viewModel: SettingsViewModelOwner

    var body: some View {
        List {
            Section("Theme") {
                accentColorPicker
                amoledDarkModeToggle
            }
            Section("Display") {
                gridColumnsPicker
            }
        }
        .navigationTitle("Appearance")
        .navigationBarTitleDisplayMode(.inline)
    }

    private var accentColorPicker: some View {
        Picker("Accent Color", selection: Binding(
            get: { viewModel.accentColor },
            set: { viewModel.onAccentColorChanged($0) }
        )) {
            ForEach(AccentColor.allCases, id: \.self) { color in
                Text(color.displayName).tag(color)
            }
        }
    }

    private var amoledDarkModeToggle: some View {
        Toggle(isOn: Binding(
            get: { viewModel.amoledDarkMode },
            set: { viewModel.onAmoledDarkModeChanged($0) }
        )) {
            VStack(alignment: .leading, spacing: Spacing.xs) {
                Text("AMOLED Dark Mode")
                    .font(.civitBodyMedium)
                Text("Pure black background for OLED screens")
                    .font(.civitBodySmall)
                    .foregroundColor(.civitOnSurfaceVariant)
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
}
