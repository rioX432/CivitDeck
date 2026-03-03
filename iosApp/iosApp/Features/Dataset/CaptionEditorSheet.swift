import SwiftUI
import Shared

struct CaptionEditorSheet: View {
    let image: DatasetImage
    let onSave: (String) -> Void
    @State private var text: String
    @Environment(\.dismiss) private var dismiss

    init(image: DatasetImage, onSave: @escaping (String) -> Void) {
        self.image = image
        self.onSave = onSave
        _text = State(initialValue: image.caption?.text ?? "")
    }

    var body: some View {
        NavigationStack {
            VStack(alignment: .leading, spacing: Spacing.md) {
                TextEditor(text: $text)
                    .frame(minHeight: 120)
                    .padding(Spacing.sm)
                    .background(Color.civitSurfaceVariant.opacity(0.3))
                    .clipShape(RoundedRectangle(cornerRadius: 8))
                Text("\(text.count) characters")
                    .font(.caption)
                    .foregroundColor(.secondary)
                Spacer()
            }
            .padding(Spacing.lg)
            .navigationTitle("Edit Caption")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarLeading) {
                    Button("Cancel") { dismiss() }
                }
                ToolbarItem(placement: .navigationBarTrailing) {
                    Button("Save") {
                        onSave(text)
                        dismiss()
                    }
                    .bold()
                }
            }
        }
        .presentationDetents([.medium, .large])
    }
}
