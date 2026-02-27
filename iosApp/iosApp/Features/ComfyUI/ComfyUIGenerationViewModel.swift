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
    @Published var resultImageUrls: [String] = []
    @Published var error: String?

    private let fetchCheckpoints = KoinHelper.shared.getFetchComfyUICheckpointsUseCase()
    private let submitGeneration = KoinHelper.shared.getSubmitComfyUIGenerationUseCase()
    private let pollResult = KoinHelper.shared.getPollComfyUIResultUseCase()

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
        generationStatus = .submitting
        self.error = nil
        resultImageUrls = []

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
                await pollForResult(promptId: promptId)
            } catch {
                generationStatus = .error
                self.error = error.localizedDescription
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

enum GenerationStatusSwift {
    case idle, submitting, running, completed, error
}
