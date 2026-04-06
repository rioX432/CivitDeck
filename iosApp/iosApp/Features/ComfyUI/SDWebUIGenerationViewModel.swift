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

    func onPromptChanged(_ value: String) { vm.onPromptChanged(value: value) }
    func onNegativePromptChanged(_ value: String) { vm.onNegativePromptChanged(value: value) }
    func onModelSelected(_ model: String) { vm.onModelSelected(model: model) }
    func onSamplerSelected(_ sampler: String) { vm.onSamplerSelected(sampler: sampler) }
    func onStepsChanged(_ steps: Int32) { vm.onStepsChanged(steps: steps) }
    func onCfgChanged(_ cfg: Double) { vm.onCfgChanged(cfg: cfg) }
    func onWidthChanged(_ w: Int32) { vm.onWidthChanged(w: w) }
    func onHeightChanged(_ h: Int32) { vm.onHeightChanged(h: h) }
    func onSeedChanged(_ seed: Int64) { vm.onSeedChanged(seed: seed) }
    func onDismissError() { vm.onDismissError() }
    func onGenerate() { vm.onGenerate() }
    func onInterrupt() { vm.onInterrupt() }
}
