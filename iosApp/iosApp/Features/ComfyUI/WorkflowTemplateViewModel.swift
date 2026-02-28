import Foundation
import Shared

@MainActor
class WorkflowTemplateViewModel: ObservableObject {
    @Published var templates: [WorkflowTemplate] = []
    @Published var isLoading = true
    @Published var error: String?
    @Published var exportedJson: String?
    @Published var importError: String?

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
                }
            } catch {
                isLoading = false
                self.error = error.localizedDescription
            }
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

    static func emptyTemplate(type: WorkflowTemplateType = WorkflowTemplateType.txt2img) -> WorkflowTemplate {
        WorkflowTemplate(
            id: 0,
            name: "",
            type: type,
            variables: defaultVariables(for: type),
            isBuiltIn: false,
            createdAt: 0
        )
    }

    static func defaultVariables(for type: WorkflowTemplateType) -> [TemplateVariable] {
        switch type {
        case .txt2img:
            return [
                TemplateVariable(name: "positive_prompt", type: .text, defaultValue: "", options: [], required: true),
                TemplateVariable(name: "negative_prompt", type: .text, defaultValue: "", options: [], required: false),
                TemplateVariable(name: "checkpoint", type: .text, defaultValue: "", options: [], required: true),
                TemplateVariable(name: "steps", type: .number, defaultValue: "20", options: [], required: false),
                TemplateVariable(name: "cfg", type: .number, defaultValue: "7.0", options: [], required: false),
                TemplateVariable(name: "width", type: .number, defaultValue: "512", options: [], required: false),
                TemplateVariable(name: "height", type: .number, defaultValue: "512", options: [], required: false),
            ]
        case .img2img:
            return [
                TemplateVariable(name: "positive_prompt", type: .text, defaultValue: "", options: [], required: true),
                TemplateVariable(name: "negative_prompt", type: .text, defaultValue: "", options: [], required: false),
                TemplateVariable(name: "checkpoint", type: .text, defaultValue: "", options: [], required: true),
                TemplateVariable(name: "steps", type: .number, defaultValue: "20", options: [], required: false),
                TemplateVariable(name: "cfg", type: .number, defaultValue: "7.0", options: [], required: false),
                TemplateVariable(name: "width", type: .number, defaultValue: "512", options: [], required: false),
                TemplateVariable(name: "height", type: .number, defaultValue: "512", options: [], required: false),
                TemplateVariable(name: "denoise_strength", type: .number, defaultValue: "0.75", options: [], required: false),
            ]
        case .inpainting:
            return [
                TemplateVariable(name: "positive_prompt", type: .text, defaultValue: "", options: [], required: true),
                TemplateVariable(name: "negative_prompt", type: .text, defaultValue: "", options: [], required: false),
                TemplateVariable(name: "checkpoint", type: .text, defaultValue: "", options: [], required: true),
                TemplateVariable(name: "steps", type: .number, defaultValue: "20", options: [], required: false),
                TemplateVariable(name: "cfg", type: .number, defaultValue: "7.0", options: [], required: false),
                TemplateVariable(name: "denoise_strength", type: .number, defaultValue: "1.0", options: [], required: false),
            ]
        case .upscale:
            return [
                TemplateVariable(name: "input_image", type: .text, defaultValue: "", options: [], required: true),
                TemplateVariable(name: "upscale_factor", type: .number, defaultValue: "2", options: [], required: false),
            ]
        default:
            return []
        }
    }
}
