import SwiftUI
import Shared

struct ExternalServerGenerationSheet: View {
    @ObservedObject var viewModel: ExternalServerGalleryViewModelOwner
    @Environment(\.dismiss) private var dismiss

    var body: some View {
        NavigationStack {
            generationContent
            .navigationTitle("Generate")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Cancel") { dismiss() }
                }
            }
        }
        .presentationDetents([.large])
    }

    @ViewBuilder
    private var generationContent: some View {
        if viewModel.isLoadingOptions {
            ProgressView()
                .frame(maxWidth: .infinity, maxHeight: .infinity)
        } else {
            generationForm
        }
    }

    private var generationForm: some View {
        Form {
            ForEach(viewModel.generationOptions, id: \.key) { option in
                DynamicFormField(
                    option: option,
                    value: Binding(
                        get: { viewModel.generationParams[option.key] ?? "" },
                        set: { viewModel.onGenerationParamChanged(key: option.key, value: $0) }
                    ),
                    dependentChoices: viewModel.dependentChoices[option.key]
                )
            }

            if let error = viewModel.generationError {
                Section {
                    Text(error)
                        .foregroundColor(.civitError)
                        .font(.civitBodySmall)
                }
            }

            Section {
                Button {
                    Task { viewModel.onSubmitGeneration() }
                } label: {
                    HStack {
                        Spacer()
                        if viewModel.isSubmittingGeneration {
                            ProgressView()
                        } else {
                            Text("Start Generation")
                        }
                        Spacer()
                    }
                }
                .disabled(viewModel.isSubmittingGeneration)
            }
        }
    }
}

private struct DynamicFormField: View {
    let option: GenerationOption
    @Binding var value: String
    let dependentChoices: [GenerationChoice]?

    var body: some View {
        Section(option.label) {
            switch option.type {
            case .select:
                SelectField(
                    choices: dependentChoices ?? option.choices.compactMap { $0 as? GenerationChoice },
                    selectedValue: $value
                )
            case .text:
                TextField(option.placeholder ?? "", text: $value)
            case .number:
                NumberField(
                    value: $value,
                    min: option.min?.intValue ?? 1,
                    max: option.max?.intValue ?? 100
                )
            default:
                TextField(option.label, text: $value)
            }
        }
    }
}

private struct SelectField: View {
    let choices: [GenerationChoice]
    @Binding var selectedValue: String

    var body: some View {
        Picker("", selection: $selectedValue) {
            ForEach(choices, id: \.value) { choice in
                VStack(alignment: .leading) {
                    Text(choice.label)
                    if let desc = choice.description_ {
                        Text(desc)
                            .font(.civitBodySmall)
                            .foregroundColor(.civitOnSurfaceVariant)
                    }
                }
                .tag(choice.value)
            }
        }
        .pickerStyle(.menu)
    }
}

private struct NumberField: View {
    @Binding var value: String
    let min: Int
    let max: Int

    private var numValue: Double {
        Double(value) ?? Double(min)
    }

    var body: some View {
        VStack(alignment: .leading, spacing: Spacing.xs) {
            HStack {
                Text("\(Int(numValue))")
                    .font(.civitBodyMedium)
                Spacer()
            }
            Slider(
                value: Binding(
                    get: { numValue },
                    set: { value = "\(Int($0))" }
                ),
                in: Double(min)...Double(max),
                step: 1
            )
        }
    }
}
