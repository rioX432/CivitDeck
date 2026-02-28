import SwiftUI
import Shared

@MainActor
class SDWebUIGenerationViewModel: ObservableObject {
    @Published var models: [String] = []
    @Published var samplers: [String] = []
    @Published var selectedModel: String = ""
    @Published var selectedSampler: String = "Euler"
    @Published var prompt: String = ""
    @Published var negativePrompt: String = ""
    @Published var steps: Double = 20
    @Published var cfgScale: Double = 7.0
    @Published var width: Int = 512
    @Published var height: Int = 512
    @Published var seedText: String = "-1"
    @Published var isLoading: Bool = false
    @Published var isGenerating: Bool = false
    @Published var progress: Double = 0
    @Published var progressStep: Int32 = 0
    @Published var progressTotalSteps: Int32 = 0
    @Published var generatedImages: [UIImage] = []
    @Published var error: String?

    private let fetchModelsUC = KoinHelper.shared.getFetchSDWebUIModelsUseCase()
    private let fetchSamplersUC = KoinHelper.shared.getFetchSDWebUISamplersUseCase()
    private let generateImageUC = KoinHelper.shared.getGenerateSDWebUIImageUseCase()
    private let interruptUC = KoinHelper.shared.getInterruptSDWebUIGenerationUseCase()

    func loadResources() async {
        isLoading = true
        do {
            let fetchedModels = try await fetchModelsUC.invoke()
            let fetchedSamplers = try await fetchSamplersUC.invoke()
            self.models = fetchedModels
            self.samplers = fetchedSamplers
            self.selectedModel = fetchedModels.first ?? ""
            self.selectedSampler = fetchedSamplers.first ?? "Euler"
        } catch {
            self.error = error.localizedDescription
        }
        isLoading = false
    }

    func generate() {
        guard !isGenerating, !prompt.isEmpty else { return }
        let seed = Int64(seedText) ?? -1
        let params = SDWebUIGenerationParams(
            prompt: prompt,
            negativePrompt: negativePrompt,
            steps: Int32(steps),
            cfgScale: cfgScale,
            width: Int32(width),
            height: Int32(height),
            samplerName: selectedSampler,
            seed: seed,
            initImageBase64: nil,
            denoisingStrength: 0.75
        )
        isGenerating = true
        generatedImages = []
        progress = 0
        Task {
            for await prog in generateImageUC.invoke(params: params) {
                if let generating = prog as? SDWebUIGenerationProgressGenerating {
                    self.progress = generating.fraction
                    self.progressStep = generating.step
                    self.progressTotalSteps = generating.totalSteps
                } else if let completed = prog as? SDWebUIGenerationProgressCompleted {
                    self.isGenerating = false
                    self.progress = 1.0
                    self.generatedImages = decodeImages(completed.base64Images)
                } else if let err = prog as? SDWebUIGenerationProgressError {
                    self.isGenerating = false
                    self.error = err.message
                }
            }
        }
    }

    func interruptGeneration() {
        Task {
            try? await interruptUC.invoke()
            isGenerating = false
        }
    }

    private func decodeImages(_ base64List: [String]) -> [UIImage] {
        base64List.compactMap { b64 in
            guard let data = Data(base64Encoded: b64) else { return nil }
            return UIImage(data: data)
        }
    }
}
