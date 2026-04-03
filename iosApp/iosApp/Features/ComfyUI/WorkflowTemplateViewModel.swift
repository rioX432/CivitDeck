import Foundation
import Shared

@MainActor
class WorkflowTemplateViewModel: ObservableObject {
    @Published var templates: [WorkflowTemplate] = []
    @Published var filteredTemplates: [WorkflowTemplate] = []
    @Published var isLoading = true
    @Published var error: String?
    @Published var exportedJson: String?
    @Published var importError: String?
    @Published var searchQuery = "" {
        didSet { applyFilters() }
    }
    @Published var selectedCategory: WorkflowTemplateCategory? {
        didSet { applyFilters() }
    }
    @Published var selectedType: WorkflowTemplateType? {
        didSet { applyFilters() }
    }

    private let getTemplates = KoinHelper.shared.getGetWorkflowTemplatesUseCase()
    private let saveTemplate = KoinHelper.shared.getSaveWorkflowTemplateUseCase()
    private let deleteTemplate = KoinHelper.shared.getDeleteWorkflowTemplateUseCase()
    private let exportTemplate = KoinHelper.shared.getExportWorkflowTemplateUseCase()
    private let importTemplate = KoinHelper.shared.getImportWorkflowTemplateUseCase()
    private var observeTask: Task<Void, Never>?

    init() {
        startObserving()
    }

    deinit {
        observeTask?.cancel()
    }

    private func startObserving() {
        observeTask = Task {
            do {
                let flow = getTemplates.invoke()
                for await list in flow {
                    guard !Task.isCancelled else { return }
                    templates = list as? [WorkflowTemplate] ?? []
                    isLoading = false
                    applyFilters()
                }
            } catch {
                isLoading = false
                self.error = error.localizedDescription
            }
        }
    }

    private func applyFilters() {
        filteredTemplates = templates.filter { template in
            let matchesSearch = searchQuery.isEmpty
                || template.name.localizedCaseInsensitiveContains(searchQuery)
                || template.description_.localizedCaseInsensitiveContains(searchQuery)
            let matchesCategory = selectedCategory == nil || template.category == selectedCategory
            let matchesType = selectedType == nil || template.type == selectedType
            return matchesSearch && matchesCategory && matchesType
        }
    }

    func onDeleteTemplate(id: Int64) {
        Task {
            do {
                try await deleteTemplate.invoke(id: id)
            } catch {
                self.error = error.localizedDescription
            }
        }
    }

    func onExportTemplate(_ template: WorkflowTemplate) {
        exportedJson = exportTemplate.invoke(template: template)
    }

    func onDismissExport() {
        exportedJson = nil
    }

    func onImportTemplate(jsonString: String) {
        Task {
            do {
                try await importTemplate.invoke(jsonString: jsonString)
                importError = nil
            } catch {
                importError = error.localizedDescription
            }
        }
    }

    func onDismissImportError() {
        importError = nil
    }

    func onSaveTemplate(_ template: WorkflowTemplate) {
        Task {
            do {
                try await saveTemplate.invoke(template: template)
            } catch {
                self.error = error.localizedDescription
            }
        }
    }

    func dismissError() {
        error = nil
    }

    // MARK: - Factory helpers

    static func emptyTemplate(
        type: WorkflowTemplateType = WorkflowTemplateType.txt2Img
    ) -> WorkflowTemplate {
        WorkflowTemplate(
            id: 0,
            name: "",
            description: "",
            type: type,
            category: .general,
            variables: defaultVariables(for: type),
            isBuiltIn: false,
            version: 1,
            author: "",
            createdAt: 0
        )
    }

    // swiftlint:disable function_body_length
    static func defaultVariables(for type: WorkflowTemplateType) -> [TemplateVariable] {
        switch type {
        case .txt2Img:
            return txt2imgVariables()
        case .img2Img:
            return txt2imgVariables() + [denoiseVariable()]
        case .inpainting:
            return [
                promptVariable(), negativePromptVariable(), checkpointVariable(),
                stepsVariable(), cfgVariable(), denoiseVariable(default: "1.0"),
            ]
        case .upscale:
            return [
                TemplateVariable(
                    name: "input_image", label: "Input Image", description: "",
                    type: .text, defaultValue: "", min: nil, max: nil, step: nil,
                    options: [], required: true
                ),
                TemplateVariable(
                    name: "upscale_factor", label: "Upscale Factor", description: "",
                    type: .slider, defaultValue: "2",
                    min: 1.0, max: 4.0, step: 0.5,
                    options: [], required: false
                ),
            ]
        case .lora:
            return [
                promptVariable(), negativePromptVariable(), checkpointVariable(),
                TemplateVariable(
                    name: "lora_name", label: "LoRA Model", description: "",
                    type: .text, defaultValue: "", min: nil, max: nil, step: nil,
                    options: [], required: true
                ),
                TemplateVariable(
                    name: "lora_strength", label: "LoRA Strength", description: "",
                    type: .slider, defaultValue: "0.8",
                    min: 0.0, max: 2.0, step: 0.05,
                    options: [], required: false
                ),
                stepsVariable(), cfgVariable(), widthVariable(), heightVariable(),
            ]
        default:
            return []
        }
    }
    // swiftlint:enable function_body_length

    // MARK: - Variable builders

    private static func promptVariable() -> TemplateVariable {
        TemplateVariable(
            name: "positive_prompt", label: "Prompt", description: "",
            type: .text, defaultValue: "", min: nil, max: nil, step: nil,
            options: [], required: true
        )
    }

    private static func negativePromptVariable() -> TemplateVariable {
        TemplateVariable(
            name: "negative_prompt", label: "Negative Prompt", description: "",
            type: .text, defaultValue: "", min: nil, max: nil, step: nil,
            options: [], required: false
        )
    }

    private static func checkpointVariable() -> TemplateVariable {
        TemplateVariable(
            name: "checkpoint", label: "Checkpoint", description: "",
            type: .text, defaultValue: "", min: nil, max: nil, step: nil,
            options: [], required: true
        )
    }

    private static func stepsVariable() -> TemplateVariable {
        TemplateVariable(
            name: "steps", label: "Steps", description: "",
            type: .slider, defaultValue: "20",
            min: 1.0, max: 150.0, step: 1.0,
            options: [], required: false
        )
    }

    private static func cfgVariable() -> TemplateVariable {
        TemplateVariable(
            name: "cfg", label: "CFG Scale", description: "",
            type: .slider, defaultValue: "7.0",
            min: 1.0, max: 30.0, step: 0.5,
            options: [], required: false
        )
    }

    private static func widthVariable() -> TemplateVariable {
        TemplateVariable(
            name: "width", label: "Width", description: "",
            type: .select, defaultValue: "512", min: nil, max: nil, step: nil,
            options: ["256", "384", "512", "640", "768", "832", "896", "1024", "1280"],
            required: false
        )
    }

    private static func heightVariable() -> TemplateVariable {
        TemplateVariable(
            name: "height", label: "Height", description: "",
            type: .select, defaultValue: "512", min: nil, max: nil, step: nil,
            options: ["256", "384", "512", "640", "768", "832", "896", "1024", "1280"],
            required: false
        )
    }

    private static func denoiseVariable(default value: String = "0.75") -> TemplateVariable {
        TemplateVariable(
            name: "denoise_strength", label: "Denoise Strength", description: "",
            type: .slider, defaultValue: value,
            min: 0.0, max: 1.0, step: 0.05,
            options: [], required: false
        )
    }

    private static func txt2imgVariables() -> [TemplateVariable] {
        [promptVariable(), negativePromptVariable(), checkpointVariable(),
         stepsVariable(), cfgVariable(), widthVariable(), heightVariable()]
    }
}
