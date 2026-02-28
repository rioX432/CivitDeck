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
    @Published var generationStatus: GenerationStatusSwift = .idle
    @Published var currentStep: Int = 0
    @Published var totalSteps: Int = 0
    @Published var resultImageUrls: [String] = []
    @Published var error: String?

    var progressFraction: Float {
        guard totalSteps > 0 else { return 0 }
        return Float(currentStep) / Float(totalSteps)
    }

    private let fetchCheckpoints = KoinHelper.shared.getFetchComfyUICheckpointsUseCase()
    private let submitGeneration = KoinHelper.shared.getSubmitComfyUIGenerationUseCase()
    private let pollResult = KoinHelper.shared.getPollComfyUIResultUseCase()
    private let observeProgress = KoinHelper.shared.getObserveGenerationProgressUseCase()
    private var progressTask: Task<Void, Never>?

    func loadCheckpoints() async {
        isLoadingCheckpoints = true
        do {
            let list = try await fetchCheckpoints.invoke()
            checkpoints = list as? [String] ?? []
            selectedCheckpoint = checkpoints.first ?? ""
            isLoadingCheckpoints = false
        } catch {
            self.error = error.localizedDescription
            isLoadingCheckpoints = false
        }
    }

    func onGenerate() {
        guard !selectedCheckpoint.isEmpty, !prompt.isEmpty else { return }
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
                    scheduler: "normal"
                )
                let promptId = try await submitGeneration.invoke(params: params)
                generationStatus = .running
                await startWebSocketProgress(promptId: promptId)
            } catch {
                generationStatus = .error
                self.error = error.localizedDescription
            }
        }
    }

    private func startWebSocketProgress(promptId: String) async {
        // ObserveGenerationProgressUseCase requires host+port — fall back to polling if unavailable
        // For now bridge via polling; WebSocket is invoked from shared ViewModel on Android.
        // On iOS we drive progress via pollForResult until SKIE-based Flow collection is wired.
        await pollForResult(promptId: promptId)
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

enum GenerationStatusSwift {
    case idle, submitting, running, completed, error
}
