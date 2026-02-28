import Foundation
import Shared

@MainActor
class ComfyUIGenerationViewModel: ObservableObject {
    @Published var checkpoints: [String] = []
    @Published var selectedCheckpoint = ""
    @Published var prompt = ""
    @Published var negativePrompt = ""
    @Published var steps: Double = 20
    @Published var cfgScale: Double = 7.0
    @Published var width = "512"
    @Published var height = "512"
    @Published var seed = ""
    @Published var isLoadingCheckpoints = false
    // LoRA
    @Published var availableLoras: [String] = []
    @Published var loraSelections: [LoraSelectionSwift] = []
    // ControlNet
    @Published var availableControlNets: [String] = []
    @Published var controlNetEnabled = false
    @Published var selectedControlNet = ""
    @Published var controlNetStrength: Double = 1.0
    // Custom workflow
    @Published var customWorkflowJson: String?
    @Published var workflowImportError: String?
    // Generation
    @Published var generationStatus: GenerationStatusSwift = .idle
    @Published var currentStep: Int = 0
    @Published var totalSteps: Int = 0
    @Published var resultImageUrls: [String] = []
    @Published var error: String?
    @Published var imageSaveSuccess: Bool?

    var progressFraction: Float {
        guard totalSteps > 0 else { return 0 }
        return Float(currentStep) / Float(totalSteps)
    }

    private let fetchCheckpointsUseCase = KoinHelper.shared.getFetchComfyUICheckpointsUseCase()
    private let fetchLorasUseCase = KoinHelper.shared.getFetchComfyUILorasUseCase()
    private let fetchControlNetsUseCase = KoinHelper.shared.getFetchComfyUIControlNetsUseCase()
    private let importWorkflowUseCase = KoinHelper.shared.getImportWorkflowUseCase()
    private let submitGeneration = KoinHelper.shared.getSubmitComfyUIGenerationUseCase()
    private let pollResult = KoinHelper.shared.getPollComfyUIResultUseCase()
    private let saveImageUseCase = KoinHelper.shared.getSaveGeneratedImageUseCase()
    private var progressTask: Task<Void, Never>?

    func loadAll() async {
        await withTaskGroup(of: Void.self) { group in
            group.addTask { await self.loadCheckpoints() }
            group.addTask { await self.loadLoras() }
            group.addTask { await self.loadControlNets() }
        }
    }

    private func loadCheckpoints() async {
        isLoadingCheckpoints = true
        do {
            let list = try await fetchCheckpointsUseCase.invoke()
            checkpoints = list as? [String] ?? []
            selectedCheckpoint = checkpoints.first ?? ""
        } catch {
            self.error = error.localizedDescription
        }
        isLoadingCheckpoints = false
    }

    private func loadLoras() async {
        do {
            let list = try await fetchLorasUseCase.invoke()
            availableLoras = list as? [String] ?? []
        } catch {
            // Non-fatal: LoRA list simply stays empty
        }
    }

    private func loadControlNets() async {
        do {
            let list = try await fetchControlNetsUseCase.invoke()
            availableControlNets = list as? [String] ?? []
        } catch {
            // Non-fatal
        }
    }

    // -- LoRA --

    func onLoraAdded(_ name: String) {
        guard !loraSelections.contains(where: { $0.name == name }) else { return }
        loraSelections.append(LoraSelectionSwift(name: name, strengthModel: 1.0, strengthClip: 1.0))
    }

    func onLoraRemoved(_ name: String) {
        loraSelections.removeAll { $0.name == name }
    }

    func onLoraStrengthChanged(name: String, strength: Double) {
        guard let idx = loraSelections.firstIndex(where: { $0.name == name }) else { return }
        loraSelections[idx].strengthModel = Float(strength)
        loraSelections[idx].strengthClip = Float(strength)
    }

    // -- ControlNet --

    func onControlNetToggled(_ enabled: Bool) {
        controlNetEnabled = enabled
    }

    // -- Custom workflow --

    func onImportWorkflow(_ jsonInput: String) {
        do {
            let result = try importWorkflowUseCase.invoke(jsonString: jsonInput)
            customWorkflowJson = result
            workflowImportError = nil
        } catch {
            workflowImportError = error.localizedDescription
        }
    }

    func onClearCustomWorkflow() {
        customWorkflowJson = nil
        workflowImportError = nil
    }

    // -- Generation --

    func onGenerate() {
        let hasCustomWorkflow = customWorkflowJson != nil
        guard hasCustomWorkflow || (!selectedCheckpoint.isEmpty && !prompt.isEmpty) else { return }
        progressTask?.cancel()
        generationStatus = .submitting
        self.error = nil
        resultImageUrls = []
        currentStep = 0
        totalSteps = 0

        Task {
            do {
                let w = Int32(width) ?? 512
                let h = Int32(height) ?? 512
                let s = Int64(seed) ?? -1
                let kLoras = loraSelections.map {
                    LoraSelection(name: $0.name, strengthModel: $0.strengthModel, strengthClip: $0.strengthClip)
                }
                let params = ComfyUIGenerationParams(
                    checkpoint: selectedCheckpoint,
                    prompt: prompt,
                    negativePrompt: negativePrompt,
                    steps: Int32(steps),
                    cfgScale: cfgScale,
                    seed: s,
                    width: w,
                    height: h,
                    samplerName: "euler",
                    scheduler: "normal",
                    loraSelections: kLoras,
                    controlNetEnabled: controlNetEnabled,
                    controlNetModel: selectedControlNet,
                    controlNetStrength: Float(controlNetStrength),
                    customWorkflowJson: customWorkflowJson
                )
                let promptId = try await submitGeneration.invoke(params: params)
                generationStatus = .running
                await pollForResult(promptId: promptId)
            } catch {
                generationStatus = .error
                self.error = error.localizedDescription
            }
        }
    }

    func onSaveImage(url: String) {
        Task {
            do {
                let success = try await saveImageUseCase.invoke(url: url, filename: "civitdeck_gen")
                imageSaveSuccess = success
            } catch {
                imageSaveSuccess = false
            }
        }
    }

    private func pollForResult(promptId: String) async {
        for _ in 0..<120 {
            try? await Task.sleep(nanoseconds: 3_000_000_000)
            do {
                let result = try await pollResult.invoke(promptId: promptId)
                let status = result.status
                if status == .completed {
                    resultImageUrls = result.imageUrls
                    generationStatus = .completed
                    return
                } else if status == .error {
                    self.error = result.error ?? "Generation failed"
                    generationStatus = .error
                    return
                }
            } catch {
                self.error = error.localizedDescription
                generationStatus = .error
                return
            }
        }
        self.error = "Generation timed out"
        generationStatus = .error
    }
}

struct LoraSelectionSwift: Identifiable {
    let id = UUID()
    let name: String
    var strengthModel: Float
    var strengthClip: Float
}

enum GenerationStatusSwift {
    case idle, submitting, running, completed, error
}
