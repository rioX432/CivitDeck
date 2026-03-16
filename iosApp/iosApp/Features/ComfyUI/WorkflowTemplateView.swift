import SwiftUI
import Shared

private let templateJsonEditorMinHeight: CGFloat = 200

struct WorkflowTemplateView: View {
    @StateObject private var viewModel = WorkflowTemplateViewModel()
    let isPicker: Bool
    var onSelect: ((WorkflowTemplate) -> Void)?

    @State private var showImportSheet = false
    @State private var importText = ""
    @State private var showCreateEditor = false
    @State private var editingTemplate: WorkflowTemplate?
    @Environment(\.dismiss) private var dismiss

    init(isPicker: Bool = false, onSelect: ((WorkflowTemplate) -> Void)? = nil) {
        self.isPicker = isPicker
        self.onSelect = onSelect
    }

    var body: some View {
        List {
            if viewModel.isLoading {
                ProgressView()
            } else if viewModel.templates.isEmpty {
                Text("No templates yet. Tap + to create one.")
                    .font(.civitBodyMedium)
                    .foregroundColor(.civitOnSurfaceVariant)
            } else {
                ForEach(viewModel.templates, id: \.id) { template in
                    TemplateRow(
                        template: template,
                        isPicker: isPicker,
                        onSelect: onSelect != nil ? { onSelect?(template); dismiss() } : nil,
                        onExport: { viewModel.onExportTemplate(template) },
                        onDelete: template.isBuiltIn ? nil : {
                            viewModel.onDeleteTemplate(id: template.id)
                        }
                    )
                }
            }
        }
        .navigationTitle(isPicker ? "Pick Template" : "Workflow Templates")
        .navigationBarTitleDisplayMode(.inline)
        .toolbar {
            if !isPicker {
                ToolbarItem(placement: .navigationBarTrailing) {
                    HStack {
                        Button {
                            showImportSheet = true
                        } label: {
                            Image(systemName: "square.and.arrow.down")
                        }
                        Button {
                            showCreateEditor = true
                        } label: {
                            Image(systemName: "plus")
                        }
                    }
                }
            }
        }
        .sheet(isPresented: $showImportSheet) {
            importSheet
        }
        .sheet(isPresented: $showCreateEditor) {
            NavigationStack {
                WorkflowTemplateEditorView(
                    initialTemplate: WorkflowTemplateViewModel.emptyTemplate(),
                    viewModel: viewModel
                )
            }
        }
        .sheet(item: $editingTemplate) { template in
            NavigationStack {
                WorkflowTemplateEditorView(initialTemplate: template, viewModel: viewModel)
            }
        }
        .alert("Export Template", isPresented: .init(
            get: { viewModel.exportedJson != nil },
            set: { if !$0 { viewModel.onDismissExport() } }
        )) {
            Button("Done") { viewModel.onDismissExport() }
        } message: {
            if let json = viewModel.exportedJson {
                Text(json)
            }
        }
        .alert("Import Error", isPresented: .init(
            get: { viewModel.importError != nil },
            set: { if !$0 { viewModel.onDismissImportError() } }
        )) {
            Button("OK") { viewModel.onDismissImportError() }
        } message: {
            if let err = viewModel.importError {
                Text(err)
            }
        }
    }

    private var importSheet: some View {
        NavigationStack {
            VStack(spacing: Spacing.md) {
                TextEditor(text: $importText)
                    .font(.civitMonoCaption)
                    .frame(maxWidth: .infinity, minHeight: templateJsonEditorMinHeight)
                    .overlay(
                        RoundedRectangle(cornerRadius: CornerRadius.image)
                            .stroke(Color.civitOnSurfaceVariant.opacity(0.4))
                    )
                    .padding(.horizontal, Spacing.md)
            }
            .padding(.top, Spacing.md)
            .navigationTitle("Import Template JSON")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Cancel") { showImportSheet = false }
                }
                ToolbarItem(placement: .confirmationAction) {
                    Button("Import") {
                        viewModel.onImportTemplate(jsonString: importText)
                        importText = ""
                        showImportSheet = false
                    }
                }
            }
        }
    }
}

private struct TemplateRow: View {
    let template: WorkflowTemplate
    let isPicker: Bool
    var onSelect: (() -> Void)?
    var onExport: () -> Void
    var onDelete: (() -> Void)?
    @Environment(\.civitTheme) private var theme

    var body: some View {
        HStack(alignment: .top) {
            VStack(alignment: .leading, spacing: Spacing.xs) {
                Text(template.name)
                    .font(.civitBodyMedium)
                Text("\(typeLabel(template.type))\(template.isBuiltIn ? " · Built-in" : "")")
                    .font(.civitBodySmall)
                    .foregroundColor(.civitOnSurfaceVariant)
                if !template.variables.isEmpty {
                    Text("Variables: \(template.variables.map { $0.name }.joined(separator: ", "))")
                        .font(.civitBodySmall)
                        .foregroundColor(.civitOnSurfaceVariant)
                        .lineLimit(2)
                }
            }
            Spacer()
            if isPicker {
                Button(action: { onSelect?() }) {
                    Image(systemName: "checkmark.circle")
                        .foregroundColor(theme.primary)
                }
                .buttonStyle(.plain)
            } else {
                Menu {
                    Button("Export JSON", action: onExport)
                    if let del = onDelete {
                        Button("Delete", role: .destructive, action: del)
                    }
                } label: {
                    Image(systemName: "ellipsis.circle")
                        .foregroundColor(.civitOnSurfaceVariant)
                }
                .buttonStyle(.plain)
            }
        }
        .contentShape(Rectangle())
        .onTapGesture { if isPicker { onSelect?() } }
    }
}

private func typeLabel(_ type: WorkflowTemplateType) -> String {
    switch type {
    case .txt2Img: return "txt2img"
    case .img2Img: return "img2img"
    case .inpainting: return "Inpainting"
    case .upscale: return "Upscale"
    default: return type.name
    }
}

extension WorkflowTemplate: Identifiable {}
