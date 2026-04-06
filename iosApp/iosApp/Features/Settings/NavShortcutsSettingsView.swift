import SwiftUI
import Shared

struct NavShortcutsSettingsView: View {
    let viewModel: DisplaySettingsViewModel

    var body: some View {
        Observing(viewModel.uiState) {
            ProgressView()
        } content: { state in
            List {
                Section {
                    Text("Choose up to 2 shortcuts to pin to the bottom navigation bar.")
                        .font(.civitBodySmall)
                        .foregroundColor(.civitOnSurfaceVariant)
                }
                Section {
                    let shortcuts = state.customNavShortcuts as? [NavShortcut] ?? []
                    ForEach(NavShortcut.allCases, id: \.name) { shortcut in
                        let isSelected = shortcuts.contains { $0 == shortcut }
                        let isDisabled = !isSelected && shortcuts.count >= 2
                        Toggle(shortcut.label, isOn: Binding(
                            get: { isSelected },
                            set: { enabled in
                                guard !isDisabled || !enabled else { return }
                                var updated = shortcuts
                                if enabled {
                                    updated.append(shortcut)
                                } else {
                                    updated.removeAll { $0 == shortcut }
                                }
                                viewModel.onCustomNavShortcutsChanged(shortcuts: updated)
                            }
                        ))
                        .disabled(isDisabled)
                    }
                }
            }
            .navigationTitle("Navigation Shortcuts")
            .navigationBarTitleDisplayMode(.inline)
        }
    }
}
