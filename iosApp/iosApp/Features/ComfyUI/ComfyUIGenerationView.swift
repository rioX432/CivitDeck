import SwiftUI
import Shared

private let workflowTextEditorMinHeight: CGFloat = 200
private let resultImageMinHeight: CGFloat = 150
private let sliderLabelWidth: CGFloat = 120
private let loraLabelWidth: CGFloat = 110

struct ComfyUIGenerationView: View {
    @StateObject private var viewModel = ComfyUIGenerationViewModelOwner()
    @Environment(\.civitTheme) private var theme
    @State private var showWorkflowImport = false
    @State private var workflowInputText = ""
    @State private var showSaveAlert = false
    @State private var showTemplatePicker = false

    var body: some View {
        ScrollView {
            VStack(spacing: Spacing.md) {
                checkpointPicker
                promptInputs
                parameterControls
                loraSection
                controlNetSection
                customWorkflowSection
                generateButton
                statusSection
                resultGrid
            }
            .padding(Spacing.md)
        }
        .navigationTitle("txt2img")
        .navigationBarTitleDisplayMode(.inline)
        .toolbar {
            ToolbarItem(placement: .navigationBarTrailing) {
                Button {
                    showTemplatePicker = true
                } label: {
                    Image(systemName: "folder")
                        .accessibilityLabel("Templates")
                }
            }
        }
        .task { await viewModel.observeUiState() }
        .sheet(isPresented: $showTemplatePicker) {
            NavigationStack {
                WorkflowTemplateView(isPicker: true, onSelect: { _ in
                    showTemplatePicker = false
                })
            }
        }
        .sheet(isPresented: $showWorkflowImport) {
            workflowImportSheet
        }
        .alert(
            viewModel.imageSaveSuccess == true ? "Saved to gallery" : "Save failed",
            isPresented: $showSaveAlert
        ) {
            Button("OK") { viewModel.imageSaveSuccess = nil }
        }
        .onChange(of: viewModel.imageSaveSuccess) { newValue in
            if newValue != nil { showSaveAlert = true }
        }
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
                .frame(width: sliderLabelWidth, alignment: .leading)
            Slider(value: value, in: range)
        }
    }

    private var loraSection: some View {
        GroupBox(label: Label("LoRA", systemImage: "cpu")) {
            VStack(alignment: .leading, spacing: Spacing.xs) {
                if viewModel.availableLoras.isEmpty {
                    Text("No LoRAs found on server").font(.civitBodySmall)
                        .foregroundColor(.civitOnSurfaceVariant)
                } else {
                    Menu("Add LoRA") {
                        ForEach(viewModel.availableLoras, id: \.self) { lora in
                            Button(lora.components(separatedBy: "/").last ?? lora) {
                                viewModel.onLoraAdded(lora)
                            }
                        }
                    }
                    .buttonStyle(.bordered)
                }
                ForEach(viewModel.loraSelections, id: \.name) { lora in
                    LoraRow(lora: lora, viewModel: viewModel)
                }
            }
        }
    }

    private var controlNetSection: some View {
        GroupBox(label: Label("ControlNet", systemImage: "slider.horizontal.3")) {
            VStack(alignment: .leading, spacing: Spacing.xs) {
                Toggle("Enable ControlNet", isOn: $viewModel.controlNetEnabled)
                if viewModel.controlNetEnabled {
                    Picker("Model", selection: $viewModel.selectedControlNet) {
                        Text("Select...").tag("")
                        ForEach(viewModel.availableControlNets, id: \.self) { cn in
                            Text(cn.components(separatedBy: "/").last ?? cn).tag(cn)
                        }
                    }
                    .pickerStyle(.menu)
                    paramSlider(label: "Strength", value: $viewModel.controlNetStrength, range: 0...2, format: "%.2f")
                }
            }
        }
    }

    private var customWorkflowSection: some View {
        GroupBox(label: Label("Custom Workflow", systemImage: "doc.badge.gearshape")) {
            VStack(alignment: .leading, spacing: Spacing.xs) {
                if let json = viewModel.customWorkflowJson {
                    HStack {
                        Text("Workflow loaded (\(json.count) chars)")
                            .font(.civitBodySmall).foregroundColor(theme.primary)
                        Spacer()
                        Button(action: viewModel.onClearCustomWorkflow) {
                            Image(systemName: "xmark.circle.fill")
                                .accessibilityLabel("Clear")
                        }
                        .buttonStyle(.plain)
                    }
                } else {
                    Button("Import Workflow JSON") { showWorkflowImport = true }
                        .buttonStyle(.bordered)
                }
                if let err = viewModel.workflowImportError {
                    Text(err).font(.civitBodySmall).foregroundColor(.civitError)
                }
            }
        }
    }

    @ViewBuilder
    private var workflowImportSheet: some View {
        NavigationStack {
            VStack(spacing: Spacing.md) {
                Text("Paste ComfyUI Workflow JSON").font(.civitBodyMedium)
                TextEditor(text: $workflowInputText)
                    .font(.civitMonoCaption)
                    .frame(maxWidth: .infinity, minHeight: workflowTextEditorMinHeight)
                    .overlay(
                        RoundedRectangle(cornerRadius: CornerRadius.image)
                            .stroke(Color.civitOnSurfaceVariant.opacity(0.4))
                    )
                if let err = viewModel.workflowImportError {
                    Text(err).font(.civitBodySmall).foregroundColor(.civitError)
                }
            }
            .padding(Spacing.md)
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Cancel") { showWorkflowImport = false }
                }
                ToolbarItem(placement: .confirmationAction) {
                    Button("Import") {
                        viewModel.onImportWorkflow(workflowInputText)
                        if viewModel.workflowImportError == nil {
                            showWorkflowImport = false
                        }
                    }
                }
            }
        }
    }

    private var generateButton: some View {
        let isGenerating = viewModel.generationStatus == .submitting
            || viewModel.generationStatus == .running
        let canGenerate = viewModel.customWorkflowJson != nil
            || (!viewModel.selectedCheckpoint.isEmpty && !viewModel.prompt.isEmpty)
        return HStack(spacing: Spacing.sm) {
            Button(action: viewModel.onGenerate) {
                HStack {
                    if isGenerating {
                        ProgressView().tint(theme.onPrimary)
                    }
                    Text(isGenerating ? "Generating..." : "Generate")
                }
                .frame(maxWidth: .infinity)
            }
            .buttonStyle(.borderedProminent)
            .disabled(isGenerating || !canGenerate)

            if isGenerating {
                Button(action: viewModel.onInterrupt) {
                    Image(systemName: "stop.fill")
                        .accessibilityLabel("Stop generation")
                }
                .buttonStyle(.borderedProminent)
                .tint(.civitError)
            }
        }
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
                .foregroundColor(theme.primary)
        default:
            EmptyView()
        }
    }

    @ViewBuilder
    private var generationProgressSection: some View {
        VStack(spacing: Spacing.sm) {
            if viewModel.totalSteps > 0 {
                ProgressView(value: viewModel.progressFraction)
                    .progressViewStyle(.linear)
                Text("Step \(viewModel.currentStep) / \(viewModel.totalSteps)")
                    .font(.civitBodySmall)
            } else {
                ProgressView()
                Text("Generating...").font(.civitBodySmall)
            }
            if !viewModel.currentNodeName.isEmpty {
                Text("Node: \(viewModel.currentNodeName)")
                    .font(.civitBodySmall)
                    .foregroundColor(.civitOnSurfaceVariant)
            }
            if let preview = viewModel.previewImage {
                Image(uiImage: preview)
                    .resizable()
                    .aspectRatio(contentMode: .fit)
                    .frame(maxWidth: .infinity)
                    .clipShape(RoundedRectangle(cornerRadius: CornerRadius.image))
                    .accessibilityLabel("Generation preview")
            }
        }
    }

    @ViewBuilder
    private var resultGrid: some View {
        if !viewModel.resultImageUrls.isEmpty {
            let columns = [GridItem(.flexible()), GridItem(.flexible())]
            LazyVGrid(columns: columns, spacing: Spacing.sm) {
                ForEach(viewModel.resultImageUrls, id: \.self) { url in
                    VStack(spacing: 0) {
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
                        .frame(minHeight: resultImageMinHeight)
                        .clipShape(RoundedRectangle(cornerRadius: CornerRadius.image))
                        .accessibilityLabel("Generated image")
                        Button {
                            viewModel.onSaveImage(url: url)
                        } label: {
                            Label("Save", systemImage: "square.and.arrow.down")
                                .font(.civitBodySmall)
                                .frame(maxWidth: .infinity)
                        }
                        .buttonStyle(.borderless)
                        .padding(.top, Spacing.xs)
                    }
                }
            }
        }
    }
}

private struct LoraRow: View {
    let lora: LoraSelection
    let viewModel: ComfyUIGenerationViewModelOwner

    var body: some View {
        VStack(alignment: .leading, spacing: Spacing.xs) {
            HStack {
                Text(lora.name.components(separatedBy: "/").last ?? lora.name)
                    .font(.civitBodySmall).lineLimit(1)
                Spacer()
                Button { viewModel.onLoraRemoved(lora.name) } label: {
                    Image(systemName: "xmark.circle.fill")
                        .accessibilityLabel("Remove")
                        .foregroundColor(.civitError)
                }
                .buttonStyle(.plain)
            }
            HStack {
                Text("Strength: \(String(format: "%.2f", lora.strengthModel))")
                    .font(.civitBodySmall)
                    .frame(width: loraLabelWidth, alignment: .leading)
                Slider(
                    value: Binding(
                        get: { Double(lora.strengthModel) },
                        set: { newValue in
                            viewModel.onLoraStrengthChanged(
                                name: lora.name,
                                strengthModel: Float(newValue),
                                strengthClip: Float(newValue),
                            )
                        },
                    ),
                    in: 0...2
                )
            }
        }
        .padding(.vertical, Spacing.xs)
    }
}
