import SwiftUI

private let promptEditorMinHeight: CGFloat = 80
private let negativePromptEditorMinHeight: CGFloat = 60
private let seedFieldWidth: CGFloat = 100

struct SDWebUIGenerationView: View {
    @StateObject private var viewModel = SDWebUIGenerationViewModelOwner()
    @State private var showError = false

    var body: some View {
        Group {
            if viewModel.isLoading {
                ProgressView("Loading resources...")
            } else {
                generationForm
            }
        }
        .navigationTitle("SD WebUI Generation")
        .navigationBarTitleDisplayMode(.inline)
        .task { await viewModel.observeUiState() }
        .alert("Error", isPresented: $showError, presenting: viewModel.error) { _ in
            Button("OK") { viewModel.error = nil }
        } message: { error in
            Text(error)
        }
        .onChange(of: viewModel.error) { err in
            showError = err != nil
        }
    }

    private var generationForm: some View {
        Form {
            modelSection
            promptSection
            parametersSection
            generateSection
            if !viewModel.generatedImages.isEmpty {
                resultsSection
            }
        }
    }

    private var modelSection: some View {
        Section("Model & Sampler") {
            if !viewModel.models.isEmpty {
                Picker("Model", selection: $viewModel.selectedModel) {
                    ForEach(viewModel.models, id: \.self) { Text($0).tag($0) }
                }
            }
            Picker("Sampler", selection: $viewModel.selectedSampler) {
                ForEach(viewModel.samplers, id: \.self) { Text($0).tag($0) }
            }
        }
    }

    private var promptSection: some View {
        Section("Prompts") {
            VStack(alignment: .leading, spacing: Spacing.xs) {
                Text("Prompt").font(.civitBodySmall).foregroundColor(.civitOnSurfaceVariant)
                TextEditor(text: $viewModel.prompt)
                    .frame(minHeight: promptEditorMinHeight)
            }
            VStack(alignment: .leading, spacing: Spacing.xs) {
                Text("Negative Prompt").font(.civitBodySmall).foregroundColor(.civitOnSurfaceVariant)
                TextEditor(text: $viewModel.negativePrompt)
                    .frame(minHeight: negativePromptEditorMinHeight)
            }
        }
    }

    private var parametersSection: some View {
        Section("Parameters") {
            stepsRow
            cfgRow
            dimensionRow
            seedRow
        }
    }

    private var stepsRow: some View {
        VStack(alignment: .leading, spacing: Spacing.xs) {
            HStack {
                Text("Steps").font(.civitBodyMedium)
                Spacer()
                Text("\(Int(viewModel.steps))").font(.civitBodySmall).foregroundColor(.civitOnSurfaceVariant)
            }
            Slider(value: $viewModel.steps, in: 1...50, step: 1)
        }
    }

    private var cfgRow: some View {
        VStack(alignment: .leading, spacing: Spacing.xs) {
            HStack {
                Text("CFG Scale").font(.civitBodyMedium)
                Spacer()
                Text(String(format: "%.1f", viewModel.cfgScale))
                    .font(.civitBodySmall)
                    .foregroundColor(.civitOnSurfaceVariant)
            }
            Slider(value: $viewModel.cfgScale, in: 1...20, step: 0.5)
        }
    }

    private var dimensionRow: some View {
        let sizes = [256, 512, 768, 1024]
        return Group {
            Picker("Width", selection: $viewModel.width) {
                ForEach(sizes, id: \.self) { Text("\($0)").tag($0) }
            }
            Picker("Height", selection: $viewModel.height) {
                ForEach(sizes, id: \.self) { Text("\($0)").tag($0) }
            }
        }
    }

    private var seedRow: some View {
        HStack {
            Text("Seed (-1 = random)").font(.civitBodyMedium)
            Spacer()
            TextField("-1", text: $viewModel.seed)
                .keyboardType(.numbersAndPunctuation)
                .multilineTextAlignment(.trailing)
                .frame(width: seedFieldWidth)
        }
    }

    private var generateSection: some View {
        Section {
            if viewModel.isGenerating {
                VStack(spacing: Spacing.sm) {
                    let label = viewModel.progressTotalSteps > 0
                        ? "Step \(viewModel.progressStep) / \(viewModel.progressTotalSteps)"
                        : "Generating..."
                    Text(label).font(.civitBodySmall)
                    ProgressView(value: viewModel.progress)
                    Button("Interrupt") { viewModel.onInterrupt() }
                        .foregroundColor(.civitError)
                }
            } else {
                Button("Generate") { viewModel.onGenerate() }
                    .disabled(viewModel.prompt.isEmpty)
                    .frame(maxWidth: .infinity)
            }
        }
    }

    private var resultsSection: some View {
        Section("Generated Images") {
            let columns = [GridItem(.flexible()), GridItem(.flexible())]
            LazyVGrid(columns: columns, spacing: Spacing.sm) {
                ForEach(Array(viewModel.generatedImages.enumerated()), id: \.offset) { _, img in
                    Image(uiImage: img)
                        .resizable()
                        .aspectRatio(1, contentMode: .fill)
                        .clipped()
                }
            }
        }
    }
}
