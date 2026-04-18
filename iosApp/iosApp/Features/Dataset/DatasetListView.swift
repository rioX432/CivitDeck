import SwiftUI
import Shared

struct DatasetListView: View {
    @StateObject private var viewModel = DatasetListViewModelOwner()
    @State private var showCreateSheet = false
    @State private var showRenameSheet = false
    @State private var createName = ""
    @State private var renameName = ""
    @State private var renameTargetId: Int64?

    var body: some View {
        NavigationStack {
            Group {
                if viewModel.datasets.isEmpty {
                    emptyView
                } else {
                    datasetList
                }
            }
            .navigationTitle("Datasets")
            .toolbar {
                ToolbarItem(placement: .navigationBarTrailing) {
                    Button {
                        createName = ""
                        showCreateSheet = true
                    } label: {
                        Image(systemName: "plus")
                            .accessibilityLabel("Create dataset")
                    }
                }
            }
            .navigationDestination(for: DatasetDestination.self) { dest in
                DatasetDetailView(datasetId: dest.datasetId, datasetName: dest.datasetName)
            }
            .sheet(isPresented: $showCreateSheet) {
                DatasetNameSheet(
                    title: "New Dataset",
                    name: $createName,
                    actionLabel: "Create"
                ) {
                    let name = createName.trimmingCharacters(in: .whitespaces)
                    if !name.isEmpty {
                        viewModel.createDataset(name: name)
                    }
                    showCreateSheet = false
                }
            }
            .sheet(isPresented: $showRenameSheet) {
                DatasetNameSheet(
                    title: "Rename Dataset",
                    name: $renameName,
                    actionLabel: "Rename"
                ) {
                    if let targetId = renameTargetId {
                        let name = renameName.trimmingCharacters(in: .whitespaces)
                        if !name.isEmpty {
                            viewModel.renameDataset(id: targetId, name: name)
                        }
                    }
                    showRenameSheet = false
                }
            }
        }
        .task { await viewModel.observeDatasets() }
    }

    private var datasetList: some View {
        List {
            ForEach(viewModel.datasets, id: \.id) { dataset in
                NavigationLink(value: DatasetDestination(datasetId: dataset.id, datasetName: dataset.name)) {
                    DatasetRow(dataset: dataset)
                }
                .swipeActions(edge: .trailing) {
                    Button(role: .destructive) {
                        viewModel.deleteDataset(id: dataset.id)
                    } label: {
                        Label("Delete", systemImage: "trash")
                    }
                    Button {
                        renameName = dataset.name
                        renameTargetId = dataset.id
                        showRenameSheet = true
                    } label: {
                        Label("Rename", systemImage: "pencil")
                    }
                    .tint(.civitTertiary)
                }
                .contextMenu {
                    Button {
                        renameName = dataset.name
                        renameTargetId = dataset.id
                        showRenameSheet = true
                    } label: {
                        Label("Rename", systemImage: "pencil")
                    }
                    Button(role: .destructive) {
                        viewModel.deleteDataset(id: dataset.id)
                    } label: {
                        Label("Delete", systemImage: "trash")
                    }
                }
            }
        }
        .listStyle(.plain)
    }

    private var emptyView: some View {
        EmptyStateView(
            icon: "photo.on.rectangle.angled",
            title: "No datasets yet",
            subtitle: "Create a dataset to organize training images"
        )
    }
}

struct DatasetDestination: Hashable {
    let datasetId: Int64
    let datasetName: String
}

// MARK: - Dataset Name Sheet

private struct DatasetNameSheet: View {
    let title: String
    @Binding var name: String
    let actionLabel: String
    let onConfirm: () -> Void

    @Environment(\.dismiss) private var dismiss
    @FocusState private var isNameFocused: Bool

    var body: some View {
        NavigationStack {
            Form {
                Section {
                    TextField("Dataset name", text: $name)
                        .focused($isNameFocused)
                }
            }
            .navigationTitle(title)
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Cancel") { dismiss() }
                }
                ToolbarItem(placement: .confirmationAction) {
                    Button(actionLabel) { onConfirm() }
                        .disabled(name.trimmingCharacters(in: .whitespaces).isEmpty)
                }
            }
            .onAppear { isNameFocused = true }
        }
        .presentationDetents([.medium])
    }
}

private struct DatasetRow: View {
    let dataset: DatasetCollection

    var body: some View {
        HStack(spacing: Spacing.md) {
            RoundedRectangle(cornerRadius: CornerRadius.image)
                .fill(Color.civitSurfaceVariant)
                .frame(width: 56, height: 56)
                .overlay {
                    Image(systemName: "photo.on.rectangle.angled")
                        .foregroundColor(.civitOnSurfaceVariant)
                        .accessibilityHidden(true)
                }
            VStack(alignment: .leading, spacing: Spacing.xs) {
                Text(dataset.name)
                    .font(.civitTitleSmall)
                    .lineLimit(1)
                Text("\(dataset.imageCount) images")
                    .font(.civitLabelSmall)
                    .foregroundColor(.civitOnSurfaceVariant)
            }
            Spacer()
        }
        .padding(.vertical, Spacing.xs)
    }
}
