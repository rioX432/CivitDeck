import SwiftUI
import Shared

struct DatasetListView: View {
    @StateObject private var viewModel = DatasetListViewModel()
    @State private var showCreateAlert = false
    @State private var newDatasetName = ""
    @State private var renameTarget: (id: Int64, name: String)?

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
                        newDatasetName = ""
                        showCreateAlert = true
                    } label: {
                        Image(systemName: "plus")
                    }
                }
            }
            .navigationDestination(for: DatasetDestination.self) { dest in
                DatasetDetailView(datasetId: dest.datasetId, datasetName: dest.datasetName)
            }
            .alert("New Dataset", isPresented: $showCreateAlert) {
                TextField("Dataset name", text: $newDatasetName)
                Button("Create") {
                    let name = newDatasetName.trimmingCharacters(in: .whitespaces)
                    if !name.isEmpty {
                        viewModel.createDataset(name: name)
                    }
                }
                Button("Cancel", role: .cancel) {}
            }
            .alert(
                "Rename Dataset",
                isPresented: Binding(
                    get: { renameTarget != nil },
                    set: { if !$0 { renameTarget = nil } }
                )
            ) {
                let currentName = renameTarget?.name ?? ""
                TextField("Dataset name", text: $newDatasetName)
                    .onAppear { newDatasetName = currentName }
                Button("Rename") {
                    if let target = renameTarget {
                        let name = newDatasetName.trimmingCharacters(in: .whitespaces)
                        if !name.isEmpty {
                            viewModel.renameDataset(id: target.id, name: name)
                        }
                    }
                }
                Button("Cancel", role: .cancel) {}
            }
        }
        .task { viewModel.startObserving() }
        .onDisappear { viewModel.stopObserving() }
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
                        newDatasetName = dataset.name
                        renameTarget = (id: dataset.id, name: dataset.name)
                    } label: {
                        Label("Rename", systemImage: "pencil")
                    }
                    .tint(.orange)
                }
                .contextMenu {
                    Button {
                        newDatasetName = dataset.name
                        renameTarget = (id: dataset.id, name: dataset.name)
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
