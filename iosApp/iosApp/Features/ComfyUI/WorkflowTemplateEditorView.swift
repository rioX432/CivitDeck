import SwiftUI
import Shared

struct WorkflowTemplateEditorView: View {
    let initialTemplate: WorkflowTemplate
    let viewModel: WorkflowTemplateViewModel

    @State private var name: String
    @State private var templateType: WorkflowTemplateType
    @State private var variables: [TemplateVariableSwift]
    @Environment(\.dismiss) private var dismiss

    init(initialTemplate: WorkflowTemplate, viewModel: WorkflowTemplateViewModel) {
        self.initialTemplate = initialTemplate
        self.viewModel = viewModel
        _name = State(initialValue: initialTemplate.name)
        _templateType = State(initialValue: initialTemplate.type)
        _variables = State(initialValue: initialTemplate.variables.map {
            TemplateVariableSwift(from: $0)
        })
    }

    var body: some View {
        List {
            Section("Name") {
                TextField("Template name", text: $name)
            }
            Section("Type") {
                Picker("Type", selection: $templateType) {
                    ForEach(WorkflowTemplateType.allCases, id: \.self) { t in
                        Text(typeLabel(t)).tag(t)
                    }
                }
                .pickerStyle(.menu)
                .onChange(of: templateType) { newType in
                    variables = WorkflowTemplateViewModel.defaultVariables(for: newType)
                        .map { TemplateVariableSwift(from: $0) }
                }
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
                        type: templateType,
                        variables: variables.map { $0.toKotlin() },
                        isBuiltIn: false,
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
            HStack {
                Picker("Type", selection: $variable.type) {
                    ForEach(TemplateVariableTypeSwift.allCases, id: \.self) { t in
                        Text(t.rawValue).tag(t)
                    }
                }
                .pickerStyle(.menu)
                .frame(maxWidth: 120)
                TextField("Default", text: $variable.defaultValue)
                    .font(.civitBodySmall)
                    .textFieldStyle(.roundedBorder)
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
    var type: TemplateVariableTypeSwift
    var defaultValue: String
    var required: Bool

    init(name: String, type: TemplateVariableTypeSwift, defaultValue: String, required: Bool) {
        self.name = name
        self.type = type
        self.defaultValue = defaultValue
        self.required = required
    }

    init(from kotlin: TemplateVariable) {
        self.name = kotlin.name
        self.defaultValue = kotlin.defaultValue
        self.required = kotlin.required
        switch kotlin.type {
        case .text: self.type = .text
        case .number: self.type = .number
        case .select: self.type = .select
        default: self.type = .text
        }
    }

    func toKotlin() -> TemplateVariable {
        TemplateVariable(
            name: name,
            type: type.toKotlin(),
            defaultValue: defaultValue,
            options: [],
            required: required
        )
    }
}

enum TemplateVariableTypeSwift: String, CaseIterable {
    case text = "TEXT"
    case number = "NUMBER"
    case select = "SELECT"

    func toKotlin() -> TemplateVariableType {
        switch self {
        case .text: return .text
        case .number: return .number
        case .select: return .select
        }
    }
}

private func typeLabel(_ type: WorkflowTemplateType) -> String {
    switch type {
    case .txt2img: return "txt2img"
    case .img2img: return "img2img"
    case .inpainting: return "Inpainting"
    case .upscale: return "Upscale"
    default: return type.name
    }
}
