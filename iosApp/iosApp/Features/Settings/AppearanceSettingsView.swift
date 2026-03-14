import SwiftUI
import Shared
import UniformTypeIdentifiers

struct AppearanceSettingsView: View {
    @ObservedObject var viewModel: DisplaySettingsViewModelOwner
    @StateObject private var themePickerVM = ThemePickerViewModel()
    @State private var showingFilePicker = false
    @State private var importError: String?
    @State private var showImportError = false

    var body: some View {
        List {
            Section("Theme") {
                themeModePicker
                accentColorPicker
                amoledDarkModeToggle
            }
            if !themePickerVM.themePlugins.isEmpty {
                Section("Custom Themes") {
                    builtInThemeRow
                    ForEach(themePickerVM.themePlugins, id: \.manifest.id) { plugin in
                        customThemeRow(plugin)
                    }
                }
            }
            Section {
                importThemeButton
            }
            Section("Display") {
                gridColumnsPicker
            }
        }
        .navigationTitle("Appearance")
        .navigationBarTitleDisplayMode(.inline)
        .task { await themePickerVM.observe() }
        .fileImporter(
            isPresented: $showingFilePicker,
            allowedContentTypes: [UTType.json],
            allowsMultipleSelection: false
        ) { result in
            handleFileImport(result)
        }
        .onChange(of: importError != nil) { showImportError = $0 }
        .alert("Import Error", isPresented: $showImportError) {
            Button("OK") { importError = nil }
        } message: {
            if let error = importError {
                Text(error)
            }
        }
    }

    private var themeModePicker: some View {
        Picker("Color Scheme", selection: Binding(
            get: { viewModel.themeMode },
            set: { viewModel.onThemeModeChanged($0) }
        )) {
            Text("Light").tag(ThemeMode.light)
            Text("Dark").tag(ThemeMode.dark)
            Text("System").tag(ThemeMode.system)
        }
        .pickerStyle(.segmented)
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

    private var builtInThemeRow: some View {
        let noneActive = themePickerVM.themePlugins.allSatisfy {
            $0.state != PluginState.active
        }
        return Button {
            Task { await themePickerVM.activateTheme(nil) }
        } label: {
            HStack {
                Text("Built-in (Accent Color)")
                    .foregroundColor(.primary)
                Spacer()
                if noneActive {
                    Image(systemName: "checkmark")
                        .accessibilityHidden(true)
                        .foregroundColor(.accentColor)
                }
            }
        }
    }

    private func customThemeRow(_ plugin: ThemePlugin) -> some View {
        Button {
            Task { await themePickerVM.activateTheme(plugin.manifest.id) }
        } label: {
            HStack {
                VStack(alignment: .leading, spacing: 2) {
                    Text(plugin.manifest.name)
                        .foregroundColor(.primary)
                    if !plugin.manifest.author.isEmpty {
                        Text("by \(plugin.manifest.author)")
                            .font(.civitBodySmall)
                            .foregroundColor(.civitOnSurfaceVariant)
                    }
                }
                Spacer()
                if plugin.state == PluginState.active {
                    Image(systemName: "checkmark")
                        .accessibilityHidden(true)
                        .foregroundColor(.accentColor)
                }
            }
        }
    }

    private var importThemeButton: some View {
        Button {
            showingFilePicker = true
        } label: {
            Label("Import Theme from JSON", systemImage: "doc.badge.plus")
        }
    }

    private func handleFileImport(_ result: Result<[URL], Error>) {
        switch result {
        case .success(let urls):
            guard let url = urls.first else { return }
            guard url.startAccessingSecurityScopedResource() else {
                importError = "Cannot access file"
                return
            }
            defer { url.stopAccessingSecurityScopedResource() }
            do {
                let data = try Data(contentsOf: url)
                guard let jsonString = String(data: data, encoding: .utf8) else {
                    importError = "File is not valid UTF-8"
                    return
                }
                Task { await themePickerVM.importTheme(jsonString) }
            } catch {
                importError = error.localizedDescription
            }
        case .failure(let error):
            importError = error.localizedDescription
        }
    }
}

// MARK: - Theme Picker ViewModel

@MainActor
final class ThemePickerViewModel: ObservableObject {
    @Published var themePlugins: [ThemePlugin] = []

    private let observeThemePluginsUseCase: ObserveThemePluginsUseCase
    private let activateThemePluginUseCase: ActivateThemePluginUseCase
    private let importThemeUseCase: ImportThemeUseCase

    init() {
        self.observeThemePluginsUseCase = KoinHelper.shared.getObserveThemePluginsUseCase()
        self.activateThemePluginUseCase = KoinHelper.shared.getActivateThemePluginUseCase()
        self.importThemeUseCase = KoinHelper.shared.getImportThemeUseCase()
    }

    func observe() async {
        for await plugins in observeThemePluginsUseCase.invoke() {
            themePlugins = plugins.compactMap { $0 as? ThemePlugin }
        }
    }

    func activateTheme(_ pluginId: String?) async {
        try? await activateThemePluginUseCase.invoke(pluginId: pluginId)
    }

    func importTheme(_ jsonString: String) async {
        _ = try? await importThemeUseCase.invoke(jsonString: jsonString)
    }
}
