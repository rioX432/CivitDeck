import SwiftUI

struct ExcludedTagsView: View {
    let tags: [String]
    let onAdd: (String) -> Void
    let onRemove: (String) -> Void

    @State private var newTag = ""

    var body: some View {
        List {
            Section {
                HStack {
                    TextField("Add tag", text: $newTag)
                        .autocapitalization(.none)
                        .disableAutocorrection(true)
                    Button("Add") {
                        onAdd(newTag)
                        newTag = ""
                    }
                    .disabled(newTag.trimmingCharacters(in: .whitespaces).isEmpty)
                }
            }
            Section {
                if tags.isEmpty {
                    Text("No excluded tags")
                        .foregroundColor(.civitOnSurfaceVariant)
                } else {
                    ForEach(tags, id: \.self) { tag in
                        HStack {
                            Text(tag)
                                .font(.civitBodyMedium)
                            Spacer()
                            Button("Remove") {
                                onRemove(tag)
                            }
                            .foregroundColor(.civitError)
                        }
                    }
                }
            }
        }
        .navigationTitle("Excluded Tags")
        .navigationBarTitleDisplayMode(.inline)
    }
}
