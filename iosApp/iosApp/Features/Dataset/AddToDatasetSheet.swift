import SwiftUI
import Shared

struct AddToDatasetSheet: View {
    let datasets: [DatasetCollection]
    let onSelectDataset: (Int64) -> Void
    let onCreateAndSelect: (String) -> Void

    @State private var showCreateAlert = false
    @State private var newDatasetName = ""

    var body: some View {
        NavigationView {
            List {
                ForEach(datasets, id: \.id) { dataset in
                    Button {
                        onSelectDataset(dataset.id)
                    } label: {
                        HStack {
                            Text(dataset.name)
                                .foregroundColor(.primary)
                            Spacer()
                            Text("\(dataset.imageCount) images")
                                .font(.civitBodySmall)
                                .foregroundColor(.civitOnSurfaceVariant)
                        }
                    }
                }

                Button {
                    newDatasetName = ""
                    showCreateAlert = true
                } label: {
                    Label("Create New Dataset", systemImage: "plus")
                }
            }
            .navigationTitle("Add to Dataset")
            .navigationBarTitleDisplayMode(.inline)
            .alert("New Dataset", isPresented: $showCreateAlert) {
                TextField("Dataset name", text: $newDatasetName)
                Button("Create") {
                    let name = newDatasetName.trimmingCharacters(in: .whitespaces)
                    if !name.isEmpty {
                        onCreateAndSelect(name)
                    }
                }
                Button("Cancel", role: .cancel) {}
            }
        }
    }
}
