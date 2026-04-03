import Foundation
import Shared
import UIKit

@MainActor
final class ComfyUIGenerationViewModelOwner: ObservableObject {
    let vm: Feature_comfyuiComfyUIGenerationViewModel
    private let store = ViewModelStore()

    // Published properties mapped from KMP UiState
    @Published var checkpoints: [String] = []
    @Published var selectedCheckpoint = ""
    @Published var prompt = ""
    @Published var negativePrompt = ""
    @Published var steps: Double = 20
    @Published var cfgScale: Double = 7.0
    @Published var width: String = "512"
    @Published var height: String = "512"
    @Published var seed: String = ""
    @Published var isLoadingCheckpoints = false
    // LoRA
    @Published var availableLoras: [String] = []
    @Published var loraSelections: [LoraSelection] = []
    // ControlNet
    @Published var availableControlNets: [String] = []
    @Published var controlNetEnabled = false
    @Published var selectedControlNet = ""
    @Published var controlNetStrength: Double = 1.0
    // Custom workflow
    @Published var customWorkflowJson: String?
    @Published var workflowImportError: String?
    // Generation
    @Published var generationStatus: GenerationStatus = .idle
    @Published var currentStep: Int32 = 0
    @Published var totalSteps: Int32 = 0
    @Published var resultImageUrls: [String] = []
    @Published var error: String?
    @Published var imageSaveSuccess: KotlinBoolean?
    @Published var previewImage: UIImage?
    @Published var currentNodeName: String = ""

    var progressFraction: Float {
        guard totalSteps > 0 else { return 0 }
        return Float(currentStep) / Float(totalSteps)
    }

    init() {
        vm = KoinHelper.shared.createComfyUIGenerationViewModel()
        store.put(key: "ComfyUIGenerationViewModel", viewModel: vm)
    }

    deinit { store.clear() }

    func observeUiState() async {
        for await state in vm.uiState {
            checkpoints = state.checkpoints as? [String] ?? []
            selectedCheckpoint = state.selectedCheckpoint
            isLoadingCheckpoints = state.isLoadingCheckpoints
            availableLoras = state.availableLoras as? [String] ?? []
            loraSelections = state.loraSelections as? [LoraSelection] ?? []
            availableControlNets = state.availableControlNets as? [String] ?? []
            controlNetEnabled = state.controlNetEnabled
            selectedControlNet = state.selectedControlNet
            controlNetStrength = Double(state.controlNetStrength)
            customWorkflowJson = state.customWorkflowJson
            workflowImportError = state.workflowImportError
            generationStatus = state.generationStatus
            currentStep = state.currentStep
            totalSteps = state.totalSteps
            error = state.error
            imageSaveSuccess = state.imageSaveSuccess
            currentNodeName = state.currentNodeName
            // Preview image from bytes
            if let bytes = state.previewImageBytes {
                let data = kotlinByteArrayToData(bytes)
                if let image = UIImage(data: data) {
                    previewImage = image
                }
            }
            // Result image URLs
            if let result = state.result {
                resultImageUrls = result.imageUrls
            }
        }
    }

    // MARK: - Actions (delegate to KMP VM)

    func onCheckpointSelected(_ checkpoint: String) { vm.onCheckpointSelected(checkpoint: checkpoint) }
    func onPromptChanged(_ value: String) { vm.onPromptChanged(prompt: value) }
    func onNegativePromptChanged(_ value: String) { vm.onNegativePromptChanged(prompt: value) }
    func onStepsChanged(_ steps: Double) {
        self.steps = steps
        vm.onStepsChanged(steps: Int32(steps))
    }
    func onCfgScaleChanged(_ cfg: Double) {
        cfgScale = cfg
        vm.onCfgScaleChanged(cfg: cfg)
    }
    func onWidthChanged(_ width: String) {
        self.width = width
        if let w = Int32(width) { vm.onWidthChanged(width: w) }
    }
    func onHeightChanged(_ height: String) {
        self.height = height
        if let h = Int32(height) { vm.onHeightChanged(height: h) }
    }
    func onSeedChanged(_ seed: String) {
        self.seed = seed
        if let s = Int64(seed) { vm.onSeedChanged(seed: s) }
    }
    func onLoraAdded(_ name: String) { vm.onLoraAdded(loraName: name) }
    func onLoraRemoved(_ name: String) { vm.onLoraRemoved(loraName: name) }
    func onLoraStrengthChanged(name: String, strengthModel: Float, strengthClip: Float) {
        vm.onLoraStrengthChanged(loraName: name, strengthModel: strengthModel, strengthClip: strengthClip)
    }
    func onControlNetToggled(_ enabled: Bool) { vm.onControlNetToggled(enabled: enabled) }
    func onControlNetSelected(_ model: String) { vm.onControlNetSelected(model: model) }
    func onControlNetStrengthChanged(_ strength: Float) { vm.onControlNetStrengthChanged(strength: strength) }
    func onImportWorkflow(_ json: String) { vm.onImportWorkflow(jsonInput: json) }
    func onClearCustomWorkflow() { vm.onClearCustomWorkflow() }
    func onGenerate() { vm.onGenerate() }
    func onSaveImage(url: String) { vm.onSaveImage(imageUrl: url) }
    func onDismissSaveResult() { vm.onDismissSaveResult() }
    func onInterrupt() { vm.onInterrupt() }
}

/// Convert Kotlin ByteArray to Swift Data
private func kotlinByteArrayToData(_ byteArray: KotlinByteArray) -> Data {
    let size = byteArray.size
    var bytes = [UInt8](repeating: 0, count: Int(size))
    for i in 0..<size {
        bytes[Int(i)] = UInt8(bitPattern: byteArray.get(index: i))
    }
    return Data(bytes)
}
