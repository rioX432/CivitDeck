import SwiftUI
import Shared

struct SavedPromptsScreen: View {
    @StateObject private var viewModel = SavedPromptsViewModel()
    @State private var templateDialogPromptId: Int64?
    @State private var templateNameInput = ""
    @State private var applyTemplatePrompt: SavedPrompt?

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
        .alert("Save as Template", isPresented: showTemplateAlert) {
            TextField("Template name (optional)", text: $templateNameInput)
            Button("Save") {
                let name = templateNameInput.trimmingCharacters(in: .whitespaces)
                if let promptId = templateDialogPromptId {
                    viewModel.toggleTemplate(
                        id: promptId,
                        isTemplate: true,
                        templateName: name.isEmpty ? nil : name
                    )
                }
                templateDialogPromptId = nil
            }
            Button("Cancel", role: .cancel) {
                templateDialogPromptId = nil
            }
        }
        .sheet(item: $applyTemplatePrompt) { prompt in
            ApplyTemplateSheet(prompt: prompt)
        }
    }

    private var showTemplateAlert: Binding<Bool> {
        Binding(
            get: { templateDialogPromptId != nil },
            set: { if !$0 { templateDialogPromptId = nil } }
        )
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
        EmptyStateView(
            icon: "bookmark",
            title: "No saved prompts yet",
            subtitle: "Prompts are auto-saved when you view images.\nYou can also save prompts manually."
        )
        .frame(maxHeight: .infinity)
    }

    private var promptList: some View {
        List {
            ForEach(viewModel.displayedPrompts, id: \.id) { prompt in
                PromptCardView(
                    prompt: prompt,
                    onToggleTemplate: {
                        if prompt.isTemplate {
                            viewModel.toggleTemplate(
                                id: prompt.id, isTemplate: false, templateName: nil
                            )
                        } else {
                            templateNameInput = ""
                            templateDialogPromptId = prompt.id
                        }
                    },
                    onDelete: {
                        viewModel.delete(id: prompt.id)
                    },
                    onApply: prompt.isTemplate ? {
                        let variables = PromptTemplateEngine.shared.extractVariables(
                            template: prompt.prompt
                        )
                        if variables.isEmpty {
                            HapticFeedback.success.trigger()
                            UIPasteboard.general.string = prompt.prompt
                        } else {
                            applyTemplatePrompt = prompt
                        }
                    } : nil
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
    var onApply: (() -> Void)?

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
            if let onApply {
                Button("Apply") { onApply() }
                    .buttonStyle(.bordered)
                    .controlSize(.small)
            }

            Button("Copy") {
                HapticFeedback.success.trigger()
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
            cfgScale: prompt.cfgScale,
            steps: prompt.steps,
            seed: prompt.seed,
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

// MARK: - Apply Template Sheet

private struct ApplyTemplateSheet: View {
    let prompt: SavedPrompt
    @Environment(\.dismiss) private var dismiss
    @State private var values: [String: String] = [:]

    private var variables: [String] {
        let list = PromptTemplateEngine.shared.extractVariables(template: prompt.prompt)
        return list.compactMap { $0 as? String }
    }

    var body: some View {
        NavigationStack {
            Form {
                ForEach(variables, id: \.self) { variable in
                    TextField(variable, text: binding(for: variable))
                }
            }
            .navigationTitle(prompt.templateName ?? "Apply Template")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Cancel") { dismiss() }
                }
                ToolbarItem(placement: .confirmationAction) {
                    Button("Generate") {
                        let result = PromptTemplateEngine.shared.substitute(
                            template: prompt.prompt,
                            values: values
                        )
                        HapticFeedback.success.trigger()
                        UIPasteboard.general.string = result
                        dismiss()
                    }
                }
            }
        }
    }

    private func binding(for key: String) -> Binding<String> {
        Binding(
            get: { values[key] ?? "" },
            set: { values[key] = $0 }
        )
    }
}

// MARK: - SavedPrompt + Identifiable

extension SavedPrompt: @retroactive Identifiable {}
