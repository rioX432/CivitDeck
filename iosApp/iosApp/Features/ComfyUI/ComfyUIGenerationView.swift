import SwiftUI
import Shared

struct ComfyUIGenerationView: View {
    @StateObject private var viewModel = ComfyUIGenerationViewModel()

    var body: some View {
        ScrollView {
            VStack(spacing: Spacing.md) {
                checkpointPicker
                promptInputs
                parameterControls
                generateButton
                statusSection
                resultGrid
            }
            .padding(Spacing.md)
        }
        .navigationTitle("txt2img")
        .navigationBarTitleDisplayMode(.inline)
        .task { await viewModel.loadCheckpoints() }
    }

    private var checkpointPicker: some View {
        VStack(alignment: .leading, spacing: Spacing.xs) {
            Text("Checkpoint").font(.civitLabelMedium)
            if viewModel.isLoadingCheckpoints {
                ProgressView()
            } else {
                Picker("Checkpoint", selection: $viewModel.selectedCheckpoint) {
                    ForEach(viewModel.checkpoints, id: \.self) { ckpt in
                        Text(ckpt).tag(ckpt).lineLimit(1)
                    }
                }
                .pickerStyle(.menu)
            }
        }
    }

    private var promptInputs: some View {
        VStack(spacing: Spacing.sm) {
            TextField("Prompt", text: $viewModel.prompt, axis: .vertical)
                .lineLimit(3...6)
                .textFieldStyle(.roundedBorder)
            TextField("Negative Prompt", text: $viewModel.negativePrompt, axis: .vertical)
                .lineLimit(2...4)
                .textFieldStyle(.roundedBorder)
        }
    }

    private var parameterControls: some View {
        VStack(spacing: Spacing.sm) {
            paramSlider(label: "Steps", value: $viewModel.steps, range: 1...150, format: "%.0f")
            paramSlider(label: "CFG Scale", value: $viewModel.cfgScale, range: 1...30, format: "%.1f")
            HStack(spacing: Spacing.sm) {
                TextField("Width", text: $viewModel.width)
                    .keyboardType(.numberPad)
                    .textFieldStyle(.roundedBorder)
                TextField("Height", text: $viewModel.height)
                    .keyboardType(.numberPad)
                    .textFieldStyle(.roundedBorder)
            }
            TextField("Seed (-1 = random)", text: $viewModel.seed)
                .keyboardType(.numberPad)
                .textFieldStyle(.roundedBorder)
        }
    }

    private func paramSlider(
        label: String,
        value: Binding<Double>,
        range: ClosedRange<Double>,
        format: String
    ) -> some View {
        HStack {
            Text("\(label): \(String(format: format, value.wrappedValue))")
                .font(.civitBodySmall)
                .frame(width: 120, alignment: .leading)
            Slider(value: value, in: range)
        }
    }

    private var generateButton: some View {
        let isGenerating = viewModel.generationStatus == .submitting
            || viewModel.generationStatus == .running
        return Button(action: viewModel.onGenerate) {
            HStack {
                if isGenerating {
                    ProgressView().tint(.white)
                }
                Text(isGenerating ? "Generating..." : "Generate")
            }
            .frame(maxWidth: .infinity)
        }
        .buttonStyle(.borderedProminent)
        .disabled(isGenerating || viewModel.selectedCheckpoint.isEmpty || viewModel.prompt.isEmpty)
    }

    @ViewBuilder
    private var statusSection: some View {
        switch viewModel.generationStatus {
        case .running:
            generationProgressSection
        case .error:
            Text(viewModel.error ?? "Generation failed")
                .font(.civitBodySmall)
                .foregroundColor(.civitError)
        case .completed:
            Text("Generation complete!")
                .font(.civitBodyMedium)
                .foregroundColor(.civitPrimary)
        default:
            EmptyView()
        }
    }

    @ViewBuilder
    private var generationProgressSection: some View {
        VStack(spacing: Spacing.xs) {
            if viewModel.totalSteps > 0 {
                ProgressView(value: viewModel.progressFraction)
                    .progressViewStyle(.linear)
                Text("Step \(viewModel.currentStep) / \(viewModel.totalSteps)")
                    .font(.civitBodySmall)
            } else {
                ProgressView()
                Text("Generating...").font(.civitBodySmall)
            }
        }
    }

    @ViewBuilder
    private var resultGrid: some View {
        if !viewModel.resultImageUrls.isEmpty {
            let columns = [GridItem(.flexible()), GridItem(.flexible())]
            LazyVGrid(columns: columns, spacing: Spacing.sm) {
                ForEach(viewModel.resultImageUrls, id: \.self) { url in
                    CachedAsyncImage(url: URL(string: url)) { phase in
                        switch phase {
                        case .success(let image):
                            image.resizable().aspectRatio(1, contentMode: .fill)
                        case .failure:
                            Color.civitSurfaceVariant
                        default:
                            Color.civitSurfaceVariant.overlay(ProgressView())
                        }
                    }
                    .frame(minHeight: 150)
                    .clipShape(RoundedRectangle(cornerRadius: 8))
                }
            }
        }
    }
}
