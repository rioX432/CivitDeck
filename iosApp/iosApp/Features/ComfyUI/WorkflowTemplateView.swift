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
    @State private var parameterTemplate: WorkflowTemplate?
    @Environment(\.dismiss) private var dismiss

    init(isPicker: Bool = false, onSelect: ((WorkflowTemplate) -> Void)? = nil) {
        self.isPicker = isPicker
        self.onSelect = onSelect
    }

    var body: some View {
        VStack(spacing: 0) {
            searchBar
            filterSection
            templateList
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
        .sheet(isPresented: $showImportSheet) { importSheet }
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
        .sheet(item: $parameterTemplate) { template in
            NavigationStack {
                TemplateParameterView(template: template) { _ in
                    parameterTemplate = nil
                }
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

    private var searchBar: some View {
        HStack {
            Image(systemName: "magnifyingglass")
                .foregroundColor(.civitOnSurfaceVariant)
            TextField("Search templates...", text: $viewModel.searchQuery)
                .font(.civitBodyMedium)
            if !viewModel.searchQuery.isEmpty {
                Button {
                    viewModel.searchQuery = ""
                } label: {
                    Image(systemName: "xmark.circle.fill")
                        .foregroundColor(.civitOnSurfaceVariant)
                }
            }
        }
        .padding(.horizontal, Spacing.md)
        .padding(.vertical, Spacing.sm)
    }

    private var filterSection: some View {
        VStack(spacing: Spacing.xs) {
            categoryFilterRow
            typeFilterRow
        }
        .padding(.horizontal, Spacing.md)
        .padding(.bottom, Spacing.sm)
    }

    private var categoryFilterRow: some View {
        ScrollView(.horizontal, showsIndicators: false) {
            HStack(spacing: Spacing.xs) {
                chipButton(title: "All", isSelected: viewModel.selectedCategory == nil) {
                    viewModel.selectedCategory = nil
                }
                ForEach(WorkflowTemplateCategory.allCases, id: \.self) { category in
                    chipButton(
                        title: categoryLabel(category),
                        isSelected: viewModel.selectedCategory == category
                    ) {
                        viewModel.selectedCategory = viewModel.selectedCategory == category ? nil : category
                    }
                }
            }
        }
    }

    private var typeFilterRow: some View {
        ScrollView(.horizontal, showsIndicators: false) {
            HStack(spacing: Spacing.xs) {
                chipButton(title: "All Types", isSelected: viewModel.selectedType == nil) {
                    viewModel.selectedType = nil
                }
                ForEach(WorkflowTemplateType.allCases, id: \.self) { type in
                    chipButton(
                        title: typeLabel(type),
                        isSelected: viewModel.selectedType == type
                    ) {
                        viewModel.selectedType = viewModel.selectedType == type ? nil : type
                    }
                }
            }
        }
    }

    private func chipButton(title: String, isSelected: Bool, action: @escaping () -> Void) -> some View {
        Button(action: action) {
            Text(title)
                .font(.civitBodySmall)
                .padding(.horizontal, Spacing.sm)
                .padding(.vertical, Spacing.xs)
                .background(isSelected ? Color.civitPrimary.opacity(0.15) : Color.clear)
                .foregroundColor(isSelected ? .civitPrimary : .civitOnSurfaceVariant)
                .cornerRadius(CornerRadius.image)
                .overlay(
                    RoundedRectangle(cornerRadius: CornerRadius.image)
                        .stroke(isSelected ? Color.civitPrimary : Color.civitOnSurfaceVariant.opacity(0.3))
                )
        }
        .buttonStyle(.plain)
    }

    private var templateList: some View {
        List {
            if viewModel.isLoading {
                ProgressView()
            } else if viewModel.filteredTemplates.isEmpty {
                Text(
                    viewModel.templates.isEmpty
                        ? "No templates yet. Tap + to create one."
                        : "No templates match your filters."
                )
                .font(.civitBodyMedium)
                .foregroundColor(.civitOnSurfaceVariant)
            } else {
                ForEach(viewModel.filteredTemplates, id: \.id) { template in
                    TemplateRow(
                        template: template,
                        isPicker: isPicker,
                        onSelect: isPicker
                            ? { onSelect?(template); dismiss() }
                            : { parameterTemplate = template },
                        onExport: { viewModel.onExportTemplate(template) },
                        onEdit: template.isBuiltIn ? nil : { editingTemplate = template },
                        onDelete: template.isBuiltIn ? nil : {
                            viewModel.onDeleteTemplate(id: template.id)
                        }
                    )
                }
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
    var onEdit: (() -> Void)?
    var onDelete: (() -> Void)?
    @Environment(\.civitTheme) private var theme

    var body: some View {
        HStack(alignment: .top) {
            VStack(alignment: .leading, spacing: Spacing.xs) {
                Text(template.name)
                    .font(.civitBodyMedium)
                if !template.description_.isEmpty {
                    Text(template.description_)
                        .font(.civitBodySmall)
                        .foregroundColor(.civitOnSurfaceVariant)
                        .lineLimit(2)
                }
                Text(templateMetaText(template))
                    .font(.civitBodySmall)
                    .foregroundColor(.civitOnSurfaceVariant)
                if !template.variables.isEmpty {
                    Text("\(template.variables.count) parameters")
                        .font(.civitBodySmall)
                        .foregroundColor(.civitOnSurfaceVariant)
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
                    if let select = onSelect {
                        Button("Use Template", action: select)
                    }
                    Button("Export JSON", action: onExport)
                    if let edit = onEdit {
                        Button("Edit", action: edit)
                    }
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
        .onTapGesture {
            if isPicker { onSelect?() } else { onSelect?() }
        }
    }

    private func templateMetaText(_ template: WorkflowTemplate) -> String {
        var parts = [typeLabel(template.type)]
        if template.isBuiltIn { parts.append("Built-in") }
        parts.append(categoryLabel(template.category))
        if template.version > 1 { parts.append("v\(template.version)") }
        return parts.joined(separator: " \u{2022} ")
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

extension WorkflowTemplate: Identifiable {}
