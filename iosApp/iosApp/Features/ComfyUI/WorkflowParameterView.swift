import SwiftUI
import Shared
import PhotosUI

private let sliderLabelWidth: CGFloat = 130

/// Renders extracted workflow parameters as a SwiftUI Form grouped by APP mode groups or nodes.
struct WorkflowParameterView: View {
    let parameters: [Feature_comfyuiExtractedParameter]
    let onParameterChanged: (String, String, String) -> Void
    let onRefresh: () -> Void
    var onImagePickRequested: ((String, String) -> Void)?
    @Environment(\.dismiss) private var dismiss
    @State private var showAdvanced = false

    var body: some View {
        NavigationStack {
            List {
                ForEach(groupedSections, id: \.title) { section in
                    sectionView(section)
                }
                if !advancedParameters.isEmpty {
                    advancedSection
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

    // MARK: - Grouped sections

    private var groupedSections: [ParameterSection] {
        let hasGroups = parameters.contains { $0.group != nil }
        if hasGroups {
            return buildAppModeGroups()
        }
        return buildNodeGroups()
    }

    private var advancedParameters: [Feature_comfyuiExtractedParameter] {
        let hasGroups = parameters.contains { $0.group != nil }
        guard hasGroups else { return [] }
        return parameters.filter { $0.group == nil }
    }

    private func buildAppModeGroups() -> [ParameterSection] {
        var groups: [String: [Feature_comfyuiExtractedParameter]] = [:]
        var order: [String] = []
        for param in parameters where param.group != nil {
            let group = param.group!
            if groups[group] == nil {
                order.append(group)
                groups[group] = []
            }
            groups[group]?.append(param)
        }
        return order.compactMap { key in
            guard let params = groups[key] else { return nil }
            let sorted = params.sorted { $0.order < $1.order }
            return ParameterSection(title: key, parameters: sorted)
        }
    }

    private func buildNodeGroups() -> [ParameterSection] {
        var groups: [String: ParameterSection] = [:]
        var order: [String] = []
        for param in parameters {
            if groups[param.nodeId] == nil {
                groups[param.nodeId] = ParameterSection(
                    title: param.nodeTitle,
                    parameters: []
                )
                order.append(param.nodeId)
            }
            groups[param.nodeId]?.parameters.append(param)
        }
        return order.compactMap { groups[$0] }
    }

    // MARK: - Section views

    @ViewBuilder
    private func sectionView(_ section: ParameterSection) -> some View {
        Section {
            ForEach(section.parameters, id: \.paramKey) { param in
                parameterWidget(param)
            }
        } header: {
            Text(section.title).font(.civitLabelMedium)
        }
    }

    @ViewBuilder
    private var advancedSection: some View {
        Section {
            DisclosureGroup(
                isExpanded: $showAdvanced,
                content: {
                    ForEach(advancedParameters, id: \.paramKey) { param in
                        parameterWidget(param)
                    }
                },
                label: {
                    Text("Advanced Parameters")
                        .font(.civitBodyMedium)
                }
            )
        }
    }

    // MARK: - Parameter widgets

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
        case .boolean_:
            booleanWidget(param)
        case .image:
            imageWidget(param)
        default:
            textWidget(param)
        }
    }

    private func booleanWidget(_ param: Feature_comfyuiExtractedParameter) -> some View {
        let isOn = param.currentValue.lowercased() == "true" || param.currentValue == "1"
        return Toggle(param.paramName, isOn: Binding(
            get: { isOn },
            set: { newValue in
                // Update immediately to prevent visual revert
                onParameterChanged(param.nodeId, param.paramName, "\(newValue)")
            }
        ))
        .font(.civitBodyMedium)
    }

    private func imageWidget(_ param: Feature_comfyuiExtractedParameter) -> some View {
        VStack(alignment: .leading, spacing: Spacing.xs) {
            Text(param.paramName).font(.civitBodySmall)
            HStack(spacing: Spacing.sm) {
                Image(systemName: "photo")
                    .font(.title2)
                    .frame(width: 48, height: 48)
                    .foregroundColor(.civitOnSurfaceVariant)
                    .overlay(
                        RoundedRectangle(cornerRadius: 12)
                            .stroke(Color.civitOutline, lineWidth: 1)
                    )
                VStack(alignment: .leading, spacing: Spacing.xs) {
                    Text(param.currentValue.isEmpty ? "No image selected" : param.currentValue)
                        .font(.civitBodySmall)
                        .lineLimit(1)
                    Button("Gallery") {
                        onImagePickRequested?(param.nodeId, param.paramName)
                    }
                    .buttonStyle(.bordered)
                    .controlSize(.small)
                }
            }
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

    // MARK: - Helpers

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

// MARK: - Supporting types

private class ParameterSection {
    let title: String
    var parameters: [Feature_comfyuiExtractedParameter]

    init(title: String, parameters: [Feature_comfyuiExtractedParameter]) {
        self.title = title
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
