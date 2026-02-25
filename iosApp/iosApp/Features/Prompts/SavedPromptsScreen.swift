import SwiftUI
import Shared

struct SavedPromptsScreen: View {
    @StateObject private var viewModel = SavedPromptsViewModel()

    var body: some View {
        VStack(spacing: 0) {
            searchBar
            tabPicker
            if viewModel.displayedPrompts.isEmpty {
                emptyState
            } else {
                promptList
            }
        }
    }

    private var searchBar: some View {
        HStack {
            TextField("Search prompts...", text: $viewModel.searchQuery)
                .textFieldStyle(.roundedBorder)
        }
        .padding(.horizontal, Spacing.lg)
        .padding(.vertical, Spacing.sm)
    }

    private var tabPicker: some View {
        Picker("Tab", selection: $viewModel.selectedTab) {
            ForEach(PromptTab.allCases, id: \.self) { tab in
                Text(tab.rawValue).tag(tab)
            }
        }
        .pickerStyle(.segmented)
        .padding(.horizontal, Spacing.lg)
        .padding(.bottom, Spacing.sm)
    }

    private var emptyState: some View {
        VStack(spacing: Spacing.sm) {
            SwiftUI.Image(systemName: "bookmark")
                .font(.system(size: 48))
                .foregroundColor(.civitOnSurfaceVariant)
            Text("No saved prompts yet")
                .font(.headline)
                .foregroundColor(.civitOnSurfaceVariant)
            Text("Prompts are auto-saved when you view images.\nYou can also save prompts manually.")
                .font(.caption)
                .foregroundColor(.civitOnSurfaceVariant)
                .multilineTextAlignment(.center)
        }
        .frame(maxHeight: .infinity)
    }

    private var promptList: some View {
        List {
            ForEach(viewModel.displayedPrompts, id: \.id) { prompt in
                PromptCardView(
                    prompt: prompt,
                    onToggleTemplate: {
                        viewModel.toggleTemplate(
                            id: prompt.id,
                            isTemplate: !prompt.isTemplate,
                            templateName: prompt.templateName
                        )
                    },
                    onDelete: {
                        viewModel.delete(id: prompt.id)
                    }
                )
            }
        }
        .listStyle(.plain)
    }
}

// MARK: - Prompt Card

private struct PromptCardView: View {
    let prompt: SavedPrompt
    let onToggleTemplate: () -> Void
    let onDelete: () -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            headerRow
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
            actionsRow
        }
        .padding(.vertical, 4)
    }

    private var headerRow: some View {
        HStack {
            VStack(alignment: .leading, spacing: 2) {
                if let templateName = prompt.templateName {
                    Text(templateName)
                        .font(.caption)
                        .foregroundColor(.civitTertiary)
                }
                if let modelName = prompt.modelName {
                    Text(modelName)
                        .font(.caption)
                        .foregroundColor(.civitPrimary)
                }
            }
            Spacer()
            Button(action: onToggleTemplate) {
                SwiftUI.Image(systemName: prompt.isTemplate ? "star.fill" : "star")
                    .foregroundColor(prompt.isTemplate ? .civitPrimary : .civitOnSurfaceVariant)
            }
            .buttonStyle(.borderless)
        }
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

    private var actionsRow: some View {
        HStack {
            Button("Copy") {
                UIPasteboard.general.string = prompt.prompt
            }
            .buttonStyle(.bordered)
            .controlSize(.small)

            Button("Export") {
                exportPrompt()
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

    private func exportPrompt() {
        let meta = ImageGenerationMeta(
            prompt: prompt.prompt,
            negativePrompt: prompt.negativePrompt,
            sampler: prompt.sampler,
            cfgScale: prompt.cfgScale.map { KotlinDouble(double: $0.doubleValue) },
            steps: prompt.steps.map { KotlinInt(int: $0.int32Value) },
            seed: prompt.seed.map { KotlinLong(longLong: $0.int64Value) },
            model: prompt.modelName,
            size: prompt.size,
            additionalParams: [:]
        )
        let text = WorkflowExportService.shared.generateA1111Params(meta: meta)
        guard let scene = UIApplication.shared.connectedScenes.first as? UIWindowScene,
              let root = scene.windows.first?.rootViewController else { return }
        var topVC = root
        while let presented = topVC.presentedViewController {
            topVC = presented
        }
        let activityVC = UIActivityViewController(activityItems: [text], applicationActivities: nil)
        topVC.present(activityVC, animated: true)
    }
}
