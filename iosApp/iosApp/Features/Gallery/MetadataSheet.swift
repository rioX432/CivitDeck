import SwiftUI
import Shared

struct MetadataSheet: View {
    let meta: ImageGenerationMeta
    var onSavePrompt: () -> Void = {}

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(alignment: .leading, spacing: Spacing.md) {
                    promptSection
                    parametersSection
                    exportSection
                }
                .padding(Spacing.lg)
            }
            .navigationTitle("Generation Info")
            .navigationBarTitleDisplayMode(.inline)
        }
    }

    // MARK: - Prompt Section

    @ViewBuilder
    private var promptSection: some View {
        if let prompt = meta.prompt {
            VStack(alignment: .leading, spacing: 4) {
                Text("Prompt")
                    .font(.caption)
                    .foregroundColor(.accentColor)
                Text(prompt)
                    .font(.callout)

                HStack(spacing: 8) {
                    Button("Copy Prompt") {
                        HapticFeedback.success.trigger()
                        UIPasteboard.general.string = prompt
                    }
                    .buttonStyle(.bordered)
                    .controlSize(.small)

                    Button("Save Prompt") {
                        onSavePrompt()
                    }
                    .buttonStyle(.bordered)
                    .controlSize(.small)
                }
                .padding(.top, 4)
            }
        }

        if let negativePrompt = meta.negativePrompt {
            VStack(alignment: .leading, spacing: 4) {
                Text("Negative Prompt")
                    .font(.caption)
                    .foregroundColor(.accentColor)
                Text(negativePrompt)
                    .font(.callout)
            }
        }

        if meta.prompt != nil || meta.negativePrompt != nil {
            Divider()
        }
    }

    // MARK: - Parameters Section

    @ViewBuilder
    private var parametersSection: some View {
        if let model = meta.model {
            paramRow(label: "Model", value: model)
        }
        if let sampler = meta.sampler {
            paramRow(label: "Sampler", value: sampler)
        }
        if let steps = meta.steps {
            paramRow(label: "Steps", value: "\(steps)")
        }
        if let cfgScale = meta.cfgScale {
            paramRow(label: "CFG Scale", value: "\(cfgScale)")
        }
        if let seed = meta.seed {
            paramRow(label: "Seed", value: "\(seed)")
        }
        if let size = meta.size {
            paramRow(label: "Size", value: size)
        }
        advancedParamsSection
    }

    // MARK: - Advanced Parameters

    @ViewBuilder
    private var advancedParamsSection: some View {
        let params = meta.additionalParams
        if !params.isEmpty {
            Divider()
            Text("Advanced Parameters")
                .font(.caption)
                .foregroundColor(.accentColor)
            ForEach(Array(params.keys.sorted()), id: \.self) { key in
                if let value = params[key] {
                    paramRow(label: key, value: value)
                }
            }
        }
    }

    // MARK: - Export Section

    @ViewBuilder
    private var exportSection: some View {
        Divider()
        Text("Export")
            .font(.caption)
            .foregroundColor(.accentColor)
        HStack(spacing: 8) {
            Button("ComfyUI Workflow") {
                let text = WorkflowExportService.shared.generateComfyUIWorkflow(meta: meta)
                presentShareSheet(text: text)
            }
            .buttonStyle(.bordered)
            .controlSize(.small)

            Button("A1111 Params") {
                let text = WorkflowExportService.shared.generateA1111Params(meta: meta)
                presentShareSheet(text: text)
            }
            .buttonStyle(.bordered)
            .controlSize(.small)
        }
    }

    private func presentShareSheet(text: String) {
        guard let scene = UIApplication.shared.connectedScenes.first as? UIWindowScene,
              let root = scene.windows.first?.rootViewController else { return }
        var topVC = root
        while let presented = topVC.presentedViewController {
            topVC = presented
        }
        let activityVC = UIActivityViewController(activityItems: [text], applicationActivities: nil)
        topVC.present(activityVC, animated: true)
    }

    private func paramRow(label: String, value: String) -> some View {
        HStack {
            Text(label)
                .font(.subheadline)
                .foregroundColor(.secondary)
            Spacer()
            Text(value)
                .font(.subheadline)
        }
    }
}
