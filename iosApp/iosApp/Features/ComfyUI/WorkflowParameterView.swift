import SwiftUI
import Shared

private let sliderLabelWidth: CGFloat = 130

/// Renders extracted workflow parameters as a SwiftUI Form grouped by node.
struct WorkflowParameterView: View {
    let parameters: [Feature_comfyuiExtractedParameter]
    let onParameterChanged: (String, String, String) -> Void
    let onRefresh: () -> Void
    @Environment(\.dismiss) private var dismiss

    var body: some View {
        NavigationStack {
            List {
                ForEach(groupedParameters, id: \.nodeId) { group in
                    nodeSection(group)
                }
            }
            .navigationTitle("Workflow Parameters")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Done") { dismiss() }
                }
                ToolbarItem(placement: .navigationBarTrailing) {
                    Button {
                        onRefresh()
                    } label: {
                        Image(systemName: "arrow.clockwise")
                            .accessibilityLabel("Refresh parameters")
                    }
                }
            }
        }
    }

    private var groupedParameters: [NodeGroup] {
        var groups: [String: NodeGroup] = [:]
        var order: [String] = []
        for param in parameters {
            if groups[param.nodeId] == nil {
                groups[param.nodeId] = NodeGroup(
                    nodeId: param.nodeId,
                    nodeTitle: param.nodeTitle,
                    classType: param.nodeClassType,
                    parameters: []
                )
                order.append(param.nodeId)
            }
            groups[param.nodeId]?.parameters.append(param)
        }
        return order.compactMap { groups[$0] }
    }

    @ViewBuilder
    private func nodeSection(_ group: NodeGroup) -> some View {
        Section {
            ForEach(group.parameters, id: \.paramKey) { param in
                parameterWidget(param)
            }
        } header: {
            VStack(alignment: .leading, spacing: Spacing.xs) {
                Text(group.nodeTitle).font(.civitLabelMedium)
                Text(group.classType).font(.civitBodySmall)
                    .foregroundColor(.civitOnSurfaceVariant)
            }
        }
    }

    @ViewBuilder
    private func parameterWidget(_ param: Feature_comfyuiExtractedParameter) -> some View {
        switch param.paramType {
        case .text:
            textWidget(param)
        case .number:
            numberWidget(param)
        case .select:
            selectWidget(param)
        case .seed:
            seedWidget(param)
        default:
            textWidget(param)
        }
    }

    private func textWidget(_ param: Feature_comfyuiExtractedParameter) -> some View {
        VStack(alignment: .leading, spacing: Spacing.xs) {
            Text(param.paramName).font(.civitBodySmall)
            TextField(param.paramName, text: bindingFor(param), axis: .vertical)
                .lineLimit(param.paramName == "text" ? 3...8 : 1...3)
                .textFieldStyle(.roundedBorder)
        }
    }

    @ViewBuilder
    private func numberWidget(_ param: Feature_comfyuiExtractedParameter) -> some View {
        let min = param.min?.doubleValue
        let max = param.max?.doubleValue
        if let min, let max, max > min {
            sliderWidget(param, min: min, max: max)
        } else {
            numberFieldWidget(param)
        }
    }

    private func sliderWidget(
        _ param: Feature_comfyuiExtractedParameter,
        min: Double,
        max: Double
    ) -> some View {
        let currentVal = Double(param.currentValue) ?? min
        let step = param.step?.doubleValue
        let isInteger = step != nil && step! >= 1.0
        return HStack {
            Text("\(param.paramName): \(formatNumber(param.currentValue))")
                .font(.civitBodySmall)
                .frame(width: sliderLabelWidth, alignment: .leading)
            Slider(
                value: Binding(
                    get: { currentVal.clamped(to: min...max) },
                    set: { newVal in
                        let formatted = isInteger
                            ? "\(Int(newVal))"
                            : String(format: "%.2f", newVal)
                        onParameterChanged(param.nodeId, param.paramName, formatted)
                    }
                ),
                in: min...max
            )
        }
    }

    private func numberFieldWidget(_ param: Feature_comfyuiExtractedParameter) -> some View {
        VStack(alignment: .leading, spacing: Spacing.xs) {
            Text(param.paramName).font(.civitBodySmall)
            TextField(param.paramName, text: bindingFor(param))
                .keyboardType(.decimalPad)
                .textFieldStyle(.roundedBorder)
        }
    }

    private func selectWidget(_ param: Feature_comfyuiExtractedParameter) -> some View {
        let options = param.options as? [String] ?? []
        return Picker(param.paramName, selection: bindingFor(param)) {
            ForEach(options, id: \.self) { option in
                Text(option.components(separatedBy: "/").last ?? option)
                    .tag(option)
                    .lineLimit(1)
            }
        }
        .pickerStyle(.menu)
    }

    private func seedWidget(_ param: Feature_comfyuiExtractedParameter) -> some View {
        HStack(spacing: Spacing.sm) {
            TextField(param.paramName, text: bindingFor(param))
                .keyboardType(.numberPad)
                .textFieldStyle(.roundedBorder)
            Button {
                let randomSeed = Int64.random(in: 0..<Int64.max)
                onParameterChanged(param.nodeId, param.paramName, "\(randomSeed)")
            } label: {
                Image(systemName: "dice")
                    .accessibilityLabel("Randomize seed")
            }
            .buttonStyle(.bordered)
        }
    }

    private func bindingFor(_ param: Feature_comfyuiExtractedParameter) -> Binding<String> {
        Binding(
            get: { param.currentValue },
            set: { newValue in
                onParameterChanged(param.nodeId, param.paramName, newValue)
            }
        )
    }

    private func formatNumber(_ value: String) -> String {
        guard let doubleVal = Double(value) else { return value }
        if doubleVal == Double(Int(doubleVal)) {
            return "\(Int(doubleVal))"
        }
        return String(format: "%.2f", doubleVal)
    }
}

private class NodeGroup {
    let nodeId: String
    let nodeTitle: String
    let classType: String
    var parameters: [Feature_comfyuiExtractedParameter]

    init(nodeId: String, nodeTitle: String, classType: String, parameters: [Feature_comfyuiExtractedParameter]) {
        self.nodeId = nodeId
        self.nodeTitle = nodeTitle
        self.classType = classType
        self.parameters = parameters
    }
}

private extension Feature_comfyuiExtractedParameter {
    /// Unique key for ForEach identification.
    var paramKey: String { "\(nodeId)_\(paramName)" }
}

private extension Double {
    func clamped(to range: ClosedRange<Double>) -> Double {
        Swift.min(Swift.max(self, range.lowerBound), range.upperBound)
    }
}
