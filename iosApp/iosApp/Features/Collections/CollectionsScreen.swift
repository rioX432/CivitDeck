import SwiftUI
import Shared

struct CollectionsScreen: View {
    @StateObject private var viewModel = CollectionsViewModel()
    @State private var showCreateSheet = false
    @State private var newCollectionName = ""
    @State private var renameTarget: (id: Int64, name: String)?
    @State private var navigationPath = NavigationPath()

    var body: some View {
        NavigationStack(path: $navigationPath) {
            Group {
                if viewModel.collections.isEmpty {
                    emptyView
                } else {
                    collectionsList
                }
            }
            .navigationTitle("Collections")
            .toolbar {
                ToolbarItem(placement: .navigationBarTrailing) {
                    Button {
                        newCollectionName = ""
                        showCreateSheet = true
                    } label: {
                        Image(systemName: "plus")
                    }
                }
            }
            .navigationDestination(for: CollectionDestination.self) { dest in
                CollectionDetailScreen(
                    collectionId: dest.collectionId,
                    collectionName: dest.collectionName
                )
            }
            .navigationDestination(for: Int64.self) { modelId in
                ModelDetailScreen(modelId: modelId)
            }
            .navigationDestination(for: CompareDestination.self) { dest in
                ModelCompareScreen(
                    leftModelId: dest.leftModelId,
                    rightModelId: dest.rightModelId
                )
            }
            .alert("New Collection", isPresented: $showCreateSheet) {
                TextField("Collection name", text: $newCollectionName)
                Button("Create") {
                    let name = newCollectionName.trimmingCharacters(in: .whitespaces)
                    if !name.isEmpty {
                        viewModel.createCollection(name: name)
                    }
                }
                Button("Cancel", role: .cancel) {}
            }
            .alert(
                "Rename Collection",
                isPresented: Binding(
                    get: { renameTarget != nil },
                    set: { if !$0 { renameTarget = nil } }
                )
            ) {
                let currentName = renameTarget?.name ?? ""
                TextField("Collection name", text: $newCollectionName)
                    .onAppear { newCollectionName = currentName }
                Button("Rename") {
                    if let target = renameTarget {
                        let name = newCollectionName.trimmingCharacters(in: .whitespaces)
                        if !name.isEmpty {
                            viewModel.renameCollection(id: target.id, name: name)
                        }
                    }
                }
                Button("Cancel", role: .cancel) {}
            }
        }
    }

    private var collectionsList: some View {
        List {
            ForEach(viewModel.collections, id: \.id) { collection in
                Button {
                    navigationPath.append(
                        CollectionDestination(
                            collectionId: collection.id,
                            collectionName: collection.name
                        )
                    )
                } label: {
                    CollectionRow(collection: collection)
                }
                .buttonStyle(.plain)
                .swipeActions(edge: .trailing) {
                    if !collection.isDefault {
                        Button(role: .destructive) {
                            viewModel.deleteCollection(id: collection.id)
                        } label: {
                            Label("Delete", systemImage: "trash")
                        }
                        Button {
                            renameTarget = (id: collection.id, name: collection.name)
                        } label: {
                            Label("Rename", systemImage: "pencil")
                        }
                        .tint(.orange)
                    }
                }
            }
        }
        .listStyle(.plain)
    }

    private var emptyView: some View {
        VStack(spacing: Spacing.sm) {
            Image(systemName: "folder")
                .font(.largeTitle)
                .foregroundColor(.civitOnSurfaceVariant)
            Text("No collections yet")
                .font(.civitTitleMedium)
                .foregroundColor(.civitOnSurfaceVariant)
            Text("Create a collection to organize your models")
                .font(.civitBodyMedium)
                .foregroundColor(.civitOnSurfaceVariant)
        }
    }
}

struct CollectionDestination: Hashable {
    let collectionId: Int64
    let collectionName: String
}

private struct CollectionRow: View {
    let collection: ModelCollection

    var body: some View {
        HStack(spacing: Spacing.md) {
            thumbnailView
            VStack(alignment: .leading, spacing: Spacing.xs) {
                Text(collection.name)
                    .font(.civitTitleSmall)
                    .lineLimit(1)
                Text("\(collection.modelCount) models")
                    .font(.civitLabelSmall)
                    .foregroundColor(.civitOnSurfaceVariant)
            }
            Spacer()
            Image(systemName: "chevron.right")
                .foregroundColor(.civitOnSurfaceVariant)
                .font(.caption)
        }
        .padding(.vertical, Spacing.xs)
    }

    @ViewBuilder
    private var thumbnailView: some View {
        if let urlString = collection.thumbnailUrl,
           let url = URL(string: urlString) {
            CachedAsyncImage(url: url) { phase in
                switch phase {
                case .success(let image):
                    image
                        .resizable()
                        .scaledToFill()
                case .failure, .empty:
                    folderPlaceholder
                @unknown default:
                    folderPlaceholder
                }
            }
            .frame(width: 56, height: 56)
            .clipShape(RoundedRectangle(cornerRadius: CornerRadius.image))
        } else {
            folderPlaceholder
        }
    }

    private var folderPlaceholder: some View {
        RoundedRectangle(cornerRadius: CornerRadius.image)
            .fill(Color.civitSurfaceVariant)
            .frame(width: 56, height: 56)
            .overlay {
                Image(systemName: "folder")
                    .foregroundColor(.civitOnSurfaceVariant)
            }
    }
}
