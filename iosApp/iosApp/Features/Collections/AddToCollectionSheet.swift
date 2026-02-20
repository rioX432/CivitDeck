import SwiftUI
import Shared

struct AddToCollectionSheet: View {
    let collections: [ModelCollection]
    let modelCollectionIds: [Int64]
    let onToggleCollection: (Int64) -> Void
    let onCreateCollection: (String) -> Void

    @State private var showCreateAlert = false
    @State private var newCollectionName = ""

    var body: some View {
        NavigationView {
            List {
                ForEach(collections, id: \.id) { collection in
                    Button {
                        onToggleCollection(collection.id)
                    } label: {
                        HStack {
                            Text(collection.name)
                                .foregroundColor(.primary)
                            Spacer()
                            if modelCollectionIds.contains(collection.id) {
                                Image(systemName: "checkmark")
                                    .foregroundColor(.accentColor)
                            }
                        }
                    }
                }

                Button {
                    newCollectionName = ""
                    showCreateAlert = true
                } label: {
                    Label("Create New Collection", systemImage: "plus")
                }
            }
            .navigationTitle("Add to Collection")
            .navigationBarTitleDisplayMode(.inline)
            .alert("New Collection", isPresented: $showCreateAlert) {
                TextField("Collection name", text: $newCollectionName)
                Button("Create") {
                    let name = newCollectionName.trimmingCharacters(in: .whitespaces)
                    if !name.isEmpty {
                        onCreateCollection(name)
                    }
                }
                Button("Cancel", role: .cancel) {}
            }
        }
    }
}
