import SwiftUI
import Shared

struct MetadataSheet: View {
    let meta: ImageGenerationMeta

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(alignment: .leading, spacing: 12) {
                    promptSection
                    parametersSection
                }
                .padding(16)
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

                Button("Copy Prompt") {
                    UIPasteboard.general.string = prompt
                }
                .buttonStyle(.bordered)
                .controlSize(.small)
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
