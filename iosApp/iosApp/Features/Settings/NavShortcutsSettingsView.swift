import SwiftUI
import Shared

struct NavShortcutsSettingsView: View {
    @ObservedObject var viewModel: DisplaySettingsViewModelOwner

    var body: some View {
        List {
            Section {
                Text("Choose up to 2 shortcuts to pin to the bottom navigation bar.")
                    .font(.civitBodySmall)
                    .foregroundColor(.civitOnSurfaceVariant)
            }
            Section {
                ForEach(NavShortcut.allCases, id: \.name) { shortcut in
                    let isSelected = viewModel.customNavShortcuts.contains { $0 == shortcut }
                    let isDisabled = !isSelected && viewModel.customNavShortcuts.count >= 2
                    Toggle(shortcut.label, isOn: Binding(
                        get: { isSelected },
                        set: { enabled in
                            guard !isDisabled || !enabled else { return }
                            var updated = viewModel.customNavShortcuts
                            if enabled {
                                updated.append(shortcut)
                            } else {
                                updated.removeAll { $0 == shortcut }
                            }
                            viewModel.onCustomNavShortcutsChanged(updated)
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
