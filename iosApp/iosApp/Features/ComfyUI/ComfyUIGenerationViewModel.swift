import Foundation
import Shared
import UIKit

/// KMP seed value meaning "random"; shown as an empty seed field.
private let randomSeed: Int64 = -1

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
    // Dynamic workflow parameters
    @Published var extractedParameters: [Feature_comfyuiExtractedParameter] = []
    @Published var isLoadingParameters = false
    // Inpainting mask
    @Published var initImageFilename: String?
    @Published var maskImageFilename: String?
    @Published var denoiseStrength: Double = 0.75
    // Generation
    @Published var generationStatus: GenerationStatus = .idle
    @Published var currentStep: Int32 = 0
    @Published var totalSteps: Int32 = 0
    @Published var resultImageUrls: [String] = []
    @Published var error: String?
    @Published var imageSaveSuccess: KotlinBoolean?
    @Published var previewImage: UIImage?
    @Published var currentNodeName: String = ""

    // KMP values last mirrored into the numeric text fields. A field is rewritten only when KMP
    // changes its value, so an unrelated emission never snaps a cleared or half-typed field back.
    private var mirroredWidth: Int32?
    private var mirroredHeight: Int32?
    private var mirroredSeed: Int64?

    var progressFraction: Float {
        guard totalSteps > 0 else { return 0 }
        return Float(currentStep) / Float(totalSteps)
    }

    /// Width/height text that does not parse is never forwarded, so KMP would still submit the last valid size.
    var hasValidDimensions: Bool {
        Int32(width) != nil && Int32(height) != nil
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
            prompt = state.prompt
            negativePrompt = state.negativePrompt
            steps = Double(state.steps)
            cfgScale = state.cfgScale
            mirrorNumericFields(width: state.width, height: state.height, seed: state.seed)
            isLoadingCheckpoints = state.isLoadingCheckpoints
            availableLoras = state.availableLoras as? [String] ?? []
            loraSelections = state.loraSelections as? [LoraSelection] ?? []
            availableControlNets = state.availableControlNets as? [String] ?? []
            controlNetEnabled = state.controlNetEnabled
            selectedControlNet = state.selectedControlNet
            controlNetStrength = Double(state.controlNetStrength)
            customWorkflowJson = state.customWorkflowJson
            workflowImportError = state.workflowImportError
            extractedParameters = state.extractedParameters as? [Feature_comfyuiExtractedParameter] ?? []
            isLoadingParameters = state.isLoadingParameters
            initImageFilename = state.initImageFilename
            maskImageFilename = state.maskImageFilename
            denoiseStrength = state.denoiseStrength
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

    func onCheckpointSelected(_ checkpoint: String) {
        selectedCheckpoint = checkpoint
        vm.onCheckpointSelected(checkpoint: checkpoint)
    }
    func onPromptChanged(_ value: String) {
        prompt = value
        vm.onPromptChanged(prompt: value)
    }
    func onNegativePromptChanged(_ value: String) {
        negativePrompt = value
        vm.onNegativePromptChanged(prompt: value)
    }
    /// Rounds so the slider, its label and the submitted step count stay the same integer.
    func onStepsChanged(_ steps: Double) {
        let rounded = steps.rounded()
        self.steps = rounded
        vm.onStepsChanged(steps: Int32(rounded))
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
    /// Empty or unparseable text means a random seed.
    func onSeedChanged(_ seed: String) {
        self.seed = seed
        vm.onSeedChanged(seed: Int64(seed) ?? randomSeed)
    }
    func onLoraAdded(_ name: String) { vm.onLoraAdded(loraName: name) }
    func onLoraRemoved(_ name: String) { vm.onLoraRemoved(loraName: name) }
    func onLoraStrengthChanged(name: String, strengthModel: Float, strengthClip: Float) {
        vm.onLoraStrengthChanged(loraName: name, strengthModel: strengthModel, strengthClip: strengthClip)
    }
    func onControlNetToggled(_ enabled: Bool) {
        controlNetEnabled = enabled
        vm.onControlNetToggled(enabled: enabled)
    }
    func onControlNetSelected(_ model: String) {
        selectedControlNet = model
        vm.onControlNetSelected(model: model)
    }
    func onControlNetStrengthChanged(_ strength: Double) {
        controlNetStrength = strength
        vm.onControlNetStrengthChanged(strength: Float(strength))
    }
    func onImportWorkflow(_ json: String) { vm.onImportWorkflow(jsonInput: json) }
    func onClearCustomWorkflow() { vm.onClearCustomWorkflow() }
    func onParameterValueChanged(nodeId: String, paramName: String, newValue: String) {
        vm.onParameterValueChanged(nodeId: nodeId, paramName: paramName, newValue: newValue)
    }
    func onRefreshParameters() { vm.onRefreshParameters() }
    func onMaskUploaded(_ filename: String) { vm.onMaskUploaded(filename: filename) }
    func onClearMask() { vm.onClearMask() }
    func onDenoiseStrengthChanged(_ strength: Double) {
        denoiseStrength = strength
        vm.onDenoiseStrengthChanged(strength: strength)
    }
    func onGenerate() { vm.onGenerate() }
    func onSaveImage(url: String) { vm.onSaveImage(imageUrl: url) }
    func onDismissSaveResult() { vm.onDismissSaveResult() }
    func onInterrupt() { vm.onInterrupt() }

    private func mirrorNumericFields(width newWidth: Int32, height newHeight: Int32, seed newSeed: Int64) {
        if newWidth != mirroredWidth, Int32(width) != newWidth { width = String(newWidth) }
        if newHeight != mirroredHeight, Int32(height) != newHeight { height = String(newHeight) }
        if newSeed != mirroredSeed, seedValue(of: seed) != newSeed {
            seed = newSeed == randomSeed ? "" : String(newSeed)
        }
        mirroredWidth = newWidth
        mirroredHeight = newHeight
        mirroredSeed = newSeed
    }
}

private func seedValue(of text: String) -> Int64? {
    text.isEmpty ? randomSeed : Int64(text)
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
