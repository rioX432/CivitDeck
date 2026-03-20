import Foundation
import Shared

@MainActor
final class ComfyHubDetailViewModel: ObservableObject {
    @Published var workflow: ComfyHubWorkflow?
    @Published var isLoading: Bool = true
    @Published var error: String?
    @Published var isImporting: Bool = false
    @Published var importResult: ImportResult?
    @Published var nodeNames: [String] = []

    enum ImportResult {
        case success
        case failure(String)
    }

    private let workflowId: String
    private let getDetail = KoinHelper.shared.getGetComfyHubWorkflowDetailUseCase()
    private let importWorkflow = KoinHelper.shared.getImportComfyHubWorkflowUseCase()

    init(workflowId: String) {
        self.workflowId = workflowId
        Task { await loadDetail() }
    }

    func retry() {
        Task { await loadDetail() }
    }

    func onImport() {
        guard let workflow else { return }
        Task {
            isImporting = true
            do {
                _ = try await importWorkflow.invoke(workflowJson: workflow.workflowJson)
                isImporting = false
                importResult = .success
            } catch {
                isImporting = false
                importResult = .failure(error.localizedDescription)
            }
        }
    }

    func dismissImportResult() {
        importResult = nil
    }

    private func loadDetail() async {
        isLoading = true
        error = nil
        do {
            let detail = try await getDetail.invoke(workflowId: workflowId)
            workflow = detail
            nodeNames = parseNodeNames(detail.workflowJson)
            isLoading = false
        } catch {
            self.error = error.localizedDescription
            isLoading = false
        }
    }

    private func parseNodeNames(_ json: String) -> [String] {
        guard let data = json.data(using: .utf8),
              let obj = try? JSONSerialization.jsonObject(with: data) as? [String: Any] else {
            return []
        }
        var names = Set<String>()
        for (_, value) in obj {
            if let node = value as? [String: Any],
               let classType = node["class_type"] as? String {
                names.insert(classType)
            }
        }
        return names.sorted()
    }
}
