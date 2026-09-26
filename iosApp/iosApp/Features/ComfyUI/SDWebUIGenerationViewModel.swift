import SwiftUI
import Shared
import UIKit

@MainActor
final class SDWebUIGenerationViewModelOwner: ObservableObject {
    let vm: Feature_comfyuiSDWebUIGenerationViewModel
    private let store = ViewModelStore()

    @Published var models: [String] = []
    @Published var samplers: [String] = []
    @Published var selectedModel: String = ""
    @Published var selectedSampler: String = "Euler"
    @Published var prompt: String = ""
    @Published var negativePrompt: String = ""
    @Published var steps: Double = 20
    @Published var cfgScale: Double = 7.0
    @Published var width: Int32 = 512
    @Published var height: Int32 = 512
    @Published var seed: String = "-1"
    @Published var isLoading: Bool = false
    @Published var isGenerating: Bool = false
    @Published var progress: Double = 0
    @Published var progressStep: Int32 = 0
    @Published var progressTotalSteps: Int32 = 0
    @Published var generatedImages: [UIImage] = []
    @Published var error: String?

    init() {
        vm = KoinHelper.shared.createSDWebUIGenerationViewModel()
        store.put(key: "SDWebUIGenerationViewModel", viewModel: vm)
    }

    deinit { store.clear() }

    func observeUiState() async {
        for await state in vm.uiState {
            models = state.models as? [String] ?? []
            samplers = state.samplers as? [String] ?? []
            selectedModel = state.selectedModel
            selectedSampler = state.selectedSampler
            isLoading = state.isLoading
            isGenerating = state.isGenerating
            progress = state.progress
            progressStep = state.progressStep
            progressTotalSteps = state.progressTotalSteps
            let base64List = state.generatedImages as? [String] ?? []
            generatedImages = base64List.compactMap { b64 in
                guard let data = Data(base64Encoded: b64) else { return nil }
                return UIImage(data: data)
            }
            error = state.error
        }
    }

    var isSeedValid: Bool { parsedSeed(seed) != nil }

    /// KMP `onGenerate()` ignores a blank prompt, and an unparseable seed never reaches KMP.
    var canGenerate: Bool {
        !prompt.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty && isSeedValid
    }

    func onPromptChanged(_ value: String) {
        prompt = value
        vm.onPromptChanged(value: value)
    }
    func onNegativePromptChanged(_ value: String) {
        negativePrompt = value
        vm.onNegativePromptChanged(value: value)
    }
    func onModelSelected(_ model: String) {
        selectedModel = model
        vm.onModelSelected(model: model)
    }
    func onSamplerSelected(_ sampler: String) {
        selectedSampler = sampler
        vm.onSamplerSelected(sampler: sampler)
    }
    /// Rounds so the slider, its label and the submitted step count stay the same integer.
    func onStepsChanged(_ steps: Double) {
        let rounded = steps.rounded()
        self.steps = rounded
        vm.onStepsChanged(steps: Int32(rounded))
    }
    func onCfgChanged(_ cfg: Double) {
        cfgScale = cfg
        vm.onCfgChanged(cfg: cfg)
    }
    func onWidthChanged(_ width: Int32) {
        self.width = width
        vm.onWidthChanged(w: width)
    }
    func onHeightChanged(_ height: Int32) {
        self.height = height
        vm.onHeightChanged(h: height)
    }
    /// Blank text means a random seed. Unparseable text stays in the field without reaching KMP.
    func onSeedChanged(_ text: String) {
        seed = text
        if let value = parsedSeed(text) { vm.onSeedChanged(seed: value) }
    }
    func onDismissError() {
        error = nil
        vm.onDismissError()
    }
    func onGenerate() { vm.onGenerate() }
    func onInterrupt() { vm.onInterrupt() }
}

/// KMP seed value meaning "random"; an empty seed field shows it as the placeholder.
private let randomSeed: Int64 = -1

private func parsedSeed(_ text: String) -> Int64? {
    let trimmed = text.trimmingCharacters(in: .whitespacesAndNewlines)
    return trimmed.isEmpty ? randomSeed : Int64(trimmed)
}
