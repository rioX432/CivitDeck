import SwiftUI
import Shared

struct TemplateParameterView: View {
    let template: WorkflowTemplate
    let onApply: ([String: String]) -> Void
    @State private var values: [String: String]
    @Environment(\.dismiss) private var dismiss

    init(template: WorkflowTemplate, onApply: @escaping ([String: String]) -> Void) {
        self.template = template
        self.onApply = onApply
        var initial: [String: String] = [:]
        for variable in template.variables {
            initial[variable.name] = variable.defaultValue
        }
        _values = State(initialValue: initial)
    }

    var body: some View {
        List {
            if !template.description_.isEmpty {
                Section {
                    Text(template.description_)
                        .font(.civitBodyMedium)
                        .foregroundColor(.civitOnSurfaceVariant)
                }
            }
            Section("Parameters") {
                ForEach(template.variables, id: \.name) { variable in
                    parameterInput(variable: variable)
                }
            }
            Section {
                Button {
                    onApply(values)
                    dismiss()
                } label: {
                    Text("Generate")
                        .frame(maxWidth: .infinity)
                        .font(.civitBodyMedium)
                }
                .disabled(!allRequiredFilled)
            }
        }
        .navigationTitle(template.name)
        .navigationBarTitleDisplayMode(.inline)
        .toolbar {
            ToolbarItem(placement: .cancellationAction) {
                Button("Cancel") { dismiss() }
            }
        }
    }

    private var allRequiredFilled: Bool {
        template.variables
            .filter { $0.required }
            .allSatisfy { !(values[$0.name] ?? "").isEmpty }
    }

    @ViewBuilder
    private func parameterInput(variable: TemplateVariable) -> some View {
        VStack(alignment: .leading, spacing: Spacing.xs) {
            Text(variable.label.isEmpty ? variable.name : variable.label)
                .font(.civitBodySmall)
                .fontWeight(.semibold)
            if !variable.description_.isEmpty {
                Text(variable.description_)
                    .font(.civitLabelSmall)
                    .foregroundColor(.civitOnSurfaceVariant)
            }
            switch variable.type {
            case .slider:
                sliderInput(variable: variable)
            case .select:
                selectInput(variable: variable)
            case .number:
                numberInput(variable: variable)
            default:
                textInput(variable: variable)
            }
        }
        .padding(.vertical, Spacing.xs)
    }

    private func sliderInput(variable: TemplateVariable) -> some View {
        let minVal = variable.min?.doubleValue ?? 0
        let maxVal = variable.max?.doubleValue ?? 100
        let stepVal = variable.step?.doubleValue ?? 1
        let current = Binding<Double>(
            get: {
                let val = Double(values[variable.name] ?? variable.defaultValue) ?? minVal
                return Swift.min(Swift.max(val, minVal), maxVal)
            },
            set: { newValue in
                values[variable.name] = formatSliderValue(newValue, step: stepVal)
            }
        )
        return VStack {
            HStack {
                Text(formatSliderValue(minVal, step: stepVal))
                    .font(.civitLabelSmall)
                    .foregroundColor(.civitOnSurfaceVariant)
                Spacer()
                Text(formatSliderValue(current.wrappedValue, step: stepVal))
                    .font(.civitBodyMedium)
                    .foregroundColor(.civitPrimary)
                Spacer()
                Text(formatSliderValue(maxVal, step: stepVal))
                    .font(.civitLabelSmall)
                    .foregroundColor(.civitOnSurfaceVariant)
            }
            Slider(
                value: current,
                in: minVal...maxVal,
                step: stepVal
            )
        }
    }

    private func selectInput(variable: TemplateVariable) -> some View {
        let options = variable.options as? [String] ?? []
        return Picker(
            variable.label.isEmpty ? variable.name : variable.label,
            selection: Binding(
                get: { values[variable.name] ?? variable.defaultValue },
                set: { values[variable.name] = $0 }
            )
        ) {
            ForEach(options, id: \.self) { option in
                Text(option).tag(option)
            }
        }
        .pickerStyle(.menu)
    }

    private func numberInput(variable: TemplateVariable) -> some View {
        // If has min/max, render as slider
        if variable.min != nil && variable.max != nil {
            return AnyView(sliderInput(variable: variable))
        } else {
            return AnyView(
                TextField(
                    variable.required ? "Required" : "Optional",
                    text: Binding(
                        get: { values[variable.name] ?? "" },
                        set: { values[variable.name] = $0 }
                    )
                )
                .keyboardType(.decimalPad)
                .textFieldStyle(.roundedBorder)
            )
        }
    }

    private func textInput(variable: TemplateVariable) -> some View {
        let isPrompt = variable.name.contains("prompt")
        return TextField(
            variable.required ? "Required" : "Optional",
            text: Binding(
                get: { values[variable.name] ?? "" },
                set: { values[variable.name] = $0 }
            ),
            axis: isPrompt ? .vertical : .horizontal
        )
        .lineLimit(isPrompt ? 3...6 : 1...1)
        .textFieldStyle(.roundedBorder)
    }

    private func formatSliderValue(_ value: Double, step: Double) -> String {
        if step >= 1 {
            return String(Int(value.rounded()))
        } else if step >= 0.1 {
            return String(format: "%.1f", value)
        } else if step >= 0.01 {
            return String(format: "%.2f", value)
        } else {
            return String(format: "%.3f", value)
        }
    }
}
