import SwiftUI
import Shared

// Tags carry the source folder, so a file name present in both folders stays two distinct options.
private let checkpointTagPrefix = "ckpt:"
private let diffusionModelTagPrefix = "unet:"

/// The generation form's model picker, plus the family, text encoder and VAE pickers a
/// diffusion model needs. A server without diffusion models shows only the checkpoint picker.
struct GenerationModelPickers: View {
    @ObservedObject var viewModel: ComfyUIGenerationViewModelOwner

    var body: some View {
        VStack(alignment: .leading, spacing: Spacing.sm) {
            modelPicker
            if viewModel.isDiffusionModelSelected {
                familyPicker
                filePicker(
                    title: "comfyui_text_encoder",
                    files: viewModel.textEncoders,
                    selection: Binding(
                        get: { viewModel.selectedTextEncoder }, set: { viewModel.onTextEncoderSelected($0) }
                    )
                )
                filePicker(
                    title: "comfyui_vae",
                    files: viewModel.vaes,
                    selection: Binding(get: { viewModel.selectedVae }, set: { viewModel.onVaeSelected($0) })
                )
            }
        }
    }

    private var hasDiffusionModels: Bool { !viewModel.diffusionModels.isEmpty }

    private var modelPicker: some View {
        let title: LocalizedStringKey = hasDiffusionModels ? "comfyui_model" : "comfyui_checkpoint"
        return VStack(alignment: .leading, spacing: Spacing.xs) {
            Text(title).font(.civitLabelMedium)
            if viewModel.isLoadingCheckpoints {
                ProgressView()
            } else {
                Picker(title, selection: Binding(get: { selectedModelTag }, set: selectModel(tag:))) {
                    if selectedModelTag.isEmpty {
                        Text("comfyui_select_placeholder").tag("")
                    }
                    if hasDiffusionModels {
                        Section("comfyui_model_section_checkpoints") { checkpointOptions }
                        Section("comfyui_model_section_diffusion_models") { diffusionModelOptions }
                    } else {
                        checkpointOptions
                    }
                }
                .pickerStyle(.menu)
            }
        }
    }

    private var checkpointOptions: some View {
        ForEach(viewModel.checkpoints, id: \.self) { ckpt in
            Text(ckpt).tag(checkpointTagPrefix + ckpt).lineLimit(1)
        }
    }

    private var diffusionModelOptions: some View {
        ForEach(viewModel.diffusionModels, id: \.self) { model in
            Text(model).tag(diffusionModelTagPrefix + model).lineLimit(1)
        }
    }

    private var selectedModelTag: String {
        if viewModel.isDiffusionModelSelected {
            let model = viewModel.selectedDiffusionModel
            return model.isEmpty ? "" : diffusionModelTagPrefix + model
        }
        let checkpoint = viewModel.selectedCheckpoint
        return checkpoint.isEmpty ? "" : checkpointTagPrefix + checkpoint
    }

    private func selectModel(tag: String) {
        if tag.hasPrefix(diffusionModelTagPrefix) {
            viewModel.onDiffusionModelSelected(String(tag.dropFirst(diffusionModelTagPrefix.count)))
        } else if tag.hasPrefix(checkpointTagPrefix) {
            viewModel.onCheckpointSelected(String(tag.dropFirst(checkpointTagPrefix.count)))
        }
    }

    // A Menu of buttons rather than a Picker, so a family the server cannot load stays listed as
    // a disabled entry that says why.
    private var familyPicker: some View {
        VStack(alignment: .leading, spacing: Spacing.xs) {
            Text("comfyui_model_family").font(.civitLabelMedium)
            Menu {
                ForEach(Core_domainDiffusionModelFamily.allCases, id: \.self) { family in
                    familyOption(family)
                }
            } label: {
                HStack(spacing: Spacing.xs) {
                    selectedFamilyText
                    Image(systemName: "chevron.up.chevron.down")
                        .imageScale(.small)
                        .accessibilityHidden(true)
                }
                .frame(minHeight: 44)
                .contentShape(Rectangle())
            }
            .accessibilityLabel(Text("comfyui_model_family"))
            .accessibilityValue(selectedFamilyText)
        }
    }

    private var selectedFamilyText: Text {
        if let family = viewModel.selectedFamily {
            return Text(verbatim: family.baseModel)
        }
        return Text("comfyui_select_placeholder")
    }

    @ViewBuilder
    private func familyOption(_ family: Core_domainDiffusionModelFamily) -> some View {
        if viewModel.isFamilySupported(family) {
            Button {
                viewModel.onModelFamilySelected(family)
            } label: {
                if family == viewModel.selectedFamily {
                    Label(family.baseModel, systemImage: "checkmark")
                } else {
                    Text(verbatim: family.baseModel)
                }
            }
        } else {
            Button {} label: {
                Text("comfyui_model_family_unsupported \(family.baseModel)")
            }
            .disabled(true)
        }
    }

    private func filePicker(
        title: LocalizedStringKey,
        files: [String],
        selection: Binding<String>
    ) -> some View {
        VStack(alignment: .leading, spacing: Spacing.xs) {
            Text(title).font(.civitLabelMedium)
            if files.isEmpty {
                Text("comfyui_no_files_on_server")
                    .font(.civitBodySmall)
                    .foregroundColor(.civitOnSurfaceVariant)
            } else {
                Picker(title, selection: selection) {
                    Text("comfyui_select_placeholder").tag("")
                    ForEach(files, id: \.self) { file in
                        Text(file).tag(file).lineLimit(1)
                    }
                }
                .pickerStyle(.menu)
            }
        }
    }
}
