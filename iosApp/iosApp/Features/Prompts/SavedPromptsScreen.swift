import SwiftUI
import Shared

struct SavedPromptsScreen: View {
    @StateObject private var viewModel = SavedPromptsViewModel()

    var body: some View {
        Group {
            if viewModel.prompts.isEmpty {
                emptyState
            } else {
                promptList
            }
        }
        .navigationTitle("Saved Prompts")
        .navigationBarTitleDisplayMode(.inline)
    }

    private var emptyState: some View {
        VStack(spacing: Spacing.sm) {
            SwiftUI.Image(systemName: "bookmark")
                .font(.system(size: 48))
                .foregroundColor(.civitOnSurfaceVariant)
            Text("No saved prompts yet")
                .font(.headline)
                .foregroundColor(.civitOnSurfaceVariant)
            Text("Save prompts from the image viewer's info panel\nto reference them later.")
                .font(.caption)
                .foregroundColor(.civitOnSurfaceVariant)
                .multilineTextAlignment(.center)
        }
        .frame(maxHeight: .infinity)
    }

    private var promptList: some View {
        List {
            ForEach(viewModel.prompts, id: \.id) { prompt in
                PromptCardView(prompt: prompt) {
                    viewModel.delete(id: prompt.id)
                }
            }
        }
        .listStyle(.plain)
    }
}

// MARK: - Prompt Card

private struct PromptCardView: View {
    let prompt: SavedPrompt
    let onDelete: () -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            if let modelName = prompt.modelName {
                Text(modelName)
                    .font(.caption)
                    .foregroundColor(.civitPrimary)
            }

            Text(prompt.prompt)
                .font(.callout)
                .lineLimit(4)

            if let neg = prompt.negativePrompt {
                Text("Negative: \(neg)")
                    .font(.caption)
                    .foregroundColor(.civitOnSurfaceVariant)
                    .lineLimit(2)
            }

            paramsText

            HStack {
                Button("Copy") {
                    UIPasteboard.general.string = prompt.prompt
                }
                .buttonStyle(.bordered)
                .controlSize(.small)

                Spacer()

                Button(role: .destructive) {
                    onDelete()
                } label: {
                    SwiftUI.Image(systemName: "trash")
                }
                .buttonStyle(.borderless)
            }
        }
        .padding(.vertical, 4)
    }

    @ViewBuilder
    private var paramsText: some View {
        let parts = [
            prompt.sampler.map { "Sampler: \($0)" },
            prompt.steps.map { "Steps: \($0)" },
            prompt.cfgScale.map { "CFG: \($0)" },
            prompt.seed.map { "Seed: \($0)" },
            prompt.size.map { "Size: \($0)" },
        ].compactMap { $0 }

        if !parts.isEmpty {
            Text(parts.joined(separator: " · "))
                .font(.caption2)
                .foregroundColor(.civitOnSurfaceVariant)
        }
    }
}
