import SwiftUI
import Shared

struct WorkflowTemplateEditorView: View {
    let initialTemplate: WorkflowTemplate
    let viewModel: WorkflowTemplateViewModel

    @State private var name: String
    @State private var templateDescription: String
    @State private var templateType: WorkflowTemplateType
    @State private var templateCategory: WorkflowTemplateCategory
    @State private var variables: [TemplateVariableSwift]
    @Environment(\.dismiss) private var dismiss

    init(initialTemplate: WorkflowTemplate, viewModel: WorkflowTemplateViewModel) {
        self.initialTemplate = initialTemplate
        self.viewModel = viewModel
        _name = State(initialValue: initialTemplate.name)
        _templateDescription = State(initialValue: initialTemplate.description_)
        _templateType = State(initialValue: initialTemplate.type)
        _templateCategory = State(initialValue: initialTemplate.category)
        _variables = State(initialValue: initialTemplate.variables.map {
            TemplateVariableSwift(from: $0)
        })
    }

    var body: some View {
        List {
            Section("Name") {
                TextField("Template name", text: $name)
            }
            Section("Description") {
                TextField("Template description", text: $templateDescription, axis: .vertical)
                    .lineLimit(2...4)
            }
            Section("Type & Category") {
                Picker("Type", selection: $templateType) {
                    ForEach(WorkflowTemplateType.allCases, id: \.self) { type in
                        Text(typeLabel(type)).tag(type)
                    }
                }
                .pickerStyle(.menu)
                .onChange(of: templateType) { newType in
                    variables = WorkflowTemplateViewModel.defaultVariables(for: newType)
                        .map { TemplateVariableSwift(from: $0) }
                }
                Picker("Category", selection: $templateCategory) {
                    ForEach(WorkflowTemplateCategory.allCases, id: \.self) { category in
                        Text(categoryLabel(category)).tag(category)
                    }
                }
                .pickerStyle(.menu)
            }
            Section {
                ForEach($variables) { $variable in
                    VariableEditorRow(variable: $variable)
                }
                .onDelete { indices in
                    variables.remove(atOffsets: indices)
                }
                Button {
                    variables.append(TemplateVariableSwift(
                        name: "var_\(variables.count + 1)",
                        type: .text,
                        defaultValue: "",
                        required: true
                    ))
                } label: {
                    Label("Add Variable", systemImage: "plus")
                }
            } header: {
                Text("Variables")
            }
        }
        .navigationTitle(initialTemplate.id == 0 ? "Create Template" : "Edit Template")
        .navigationBarTitleDisplayMode(.inline)
        .toolbar {
            ToolbarItem(placement: .cancellationAction) {
                Button("Cancel") { dismiss() }
            }
            ToolbarItem(placement: .confirmationAction) {
                Button("Save") {
                    let updated = WorkflowTemplate(
                        id: initialTemplate.id,
                        name: name.trimmingCharacters(in: .whitespaces),
                        description: templateDescription.trimmingCharacters(in: .whitespaces),
                        type: templateType,
                        category: templateCategory,
                        variables: variables.map { $0.toKotlin() },
                        isBuiltIn: false,
                        version: initialTemplate.version,
                        author: initialTemplate.author,
                        createdAt: initialTemplate.createdAt
                    )
                    viewModel.onSaveTemplate(updated)
                    dismiss()
                }
                .disabled(name.trimmingCharacters(in: .whitespaces).isEmpty)
            }
        }
    }
}

private struct VariableEditorRow: View {
    @Binding var variable: TemplateVariableSwift

    var body: some View {
        VStack(alignment: .leading, spacing: Spacing.xs) {
            TextField("Variable name", text: $variable.name)
                .font(.civitBodySmall)
            TextField("Display label", text: $variable.label)
                .font(.civitBodySmall)
            HStack {
                Picker("Type", selection: $variable.type) {
                    ForEach(TemplateVariableTypeSwift.allCases, id: \.self) { type in
                        Text(type.rawValue).tag(type)
                    }
                }
                .pickerStyle(.menu)
                .frame(maxWidth: 120)
                TextField("Default", text: $variable.defaultValue)
                    .font(.civitBodySmall)
                    .textFieldStyle(.roundedBorder)
            }
            if variable.type == .slider {
                HStack(spacing: Spacing.sm) {
                    TextField("Min", text: Binding(
                        get: { variable.min.map { String($0) } ?? "" },
                        set: { variable.min = Double($0) }
                    ))
                    .font(.civitBodySmall)
                    .textFieldStyle(.roundedBorder)
                    .frame(maxWidth: 80)
                    TextField("Max", text: Binding(
                        get: { variable.max.map { String($0) } ?? "" },
                        set: { variable.max = Double($0) }
                    ))
                    .font(.civitBodySmall)
                    .textFieldStyle(.roundedBorder)
                    .frame(maxWidth: 80)
                    TextField("Step", text: Binding(
                        get: { variable.step.map { String($0) } ?? "" },
                        set: { variable.step = Double($0) }
                    ))
                    .font(.civitBodySmall)
                    .textFieldStyle(.roundedBorder)
                    .frame(maxWidth: 80)
                }
            }
            Toggle("Required", isOn: $variable.required)
                .font(.civitBodySmall)
        }
        .padding(.vertical, Spacing.xs)
    }
}

// MARK: - Swift-side model

struct TemplateVariableSwift: Identifiable {
    let id = UUID()
    var name: String
    var label: String
    var description: String
    var type: TemplateVariableTypeSwift
    var defaultValue: String
    var min: Double?
    var max: Double?
    var step: Double?
    var options: [String]
    var required: Bool

    init(
        name: String,
        type: TemplateVariableTypeSwift,
        defaultValue: String,
        required: Bool,
        label: String = "",
        description: String = "",
        min: Double? = nil,
        max: Double? = nil,
        step: Double? = nil,
        options: [String] = []
    ) {
        self.name = name
        self.label = label
        self.description = description
        self.type = type
        self.defaultValue = defaultValue
        self.min = min
        self.max = max
        self.step = step
        self.options = options
        self.required = required
    }

    init(from kotlin: TemplateVariable) {
        self.name = kotlin.name
        self.label = kotlin.label
        self.description = kotlin.description_
        self.defaultValue = kotlin.defaultValue
        self.min = kotlin.min?.doubleValue
        self.max = kotlin.max?.doubleValue
        self.step = kotlin.step?.doubleValue
        self.options = kotlin.options as? [String] ?? []
        self.required = kotlin.required
        switch kotlin.type {
        case .text: self.type = .text
        case .number: self.type = .number
        case .select: self.type = .select
        case .slider: self.type = .slider
        default: self.type = .text
        }
    }

    func toKotlin() -> TemplateVariable {
        TemplateVariable(
            name: name,
            label: label,
            description: description,
            type: type.toKotlin(),
            defaultValue: defaultValue,
            min: min.map { KotlinDouble(double: $0) },
            max: max.map { KotlinDouble(double: $0) },
            step: step.map { KotlinDouble(double: $0) },
            options: options,
            required: required
        )
    }
}

enum TemplateVariableTypeSwift: String, CaseIterable {
    case text = "TEXT"
    case number = "NUMBER"
    case select = "SELECT"
    case slider = "SLIDER"

    func toKotlin() -> TemplateVariableType {
        switch self {
        case .text: return .text
        case .number: return .number
        case .select: return .select
        case .slider: return .slider
        }
    }
}

private func typeLabel(_ type: WorkflowTemplateType) -> String {
    switch type {
    case .txt2Img: return "txt2img"
    case .img2Img: return "img2img"
    case .inpainting: return "Inpainting"
    case .upscale: return "Upscale"
    case .lora: return "LoRA"
    default: return type.name
    }
}

private func categoryLabel(_ category: WorkflowTemplateCategory) -> String {
    switch category {
    case .general: return "General"
    case .anime: return "Anime"
    case .photorealistic: return "Photo"
    case .artistic: return "Artistic"
    case .utility: return "Utility"
    default: return category.name
    }
}
