import SwiftUI

struct ResolutionFilterSheet: View {
    @State private var minWidth: Int
    @State private var minHeight: Int
    let onApply: (Int, Int) -> Void
    @Environment(\.dismiss) private var dismiss

    init(initialMinWidth: Int = 0, initialMinHeight: Int = 0, onApply: @escaping (Int, Int) -> Void) {
        _minWidth = State(initialValue: initialMinWidth)
        _minHeight = State(initialValue: initialMinHeight)
        self.onApply = onApply
    }

    var body: some View {
        NavigationStack {
            Form {
                Section("Minimum Width") {
                    Stepper("Width: \(minWidth)px", value: $minWidth, in: 0...4096, step: 64)
                }
                Section("Minimum Height") {
                    Stepper("Height: \(minHeight)px", value: $minHeight, in: 0...4096, step: 64)
                }
                Section {
                    Button("Clear Filter") {
                        minWidth = 0
                        minHeight = 0
                    }
                    .foregroundColor(Color.civitError)
                }
            }
            .navigationTitle("Resolution Filter")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarLeading) {
                    Button("Cancel") { dismiss() }
                }
                ToolbarItem(placement: .navigationBarTrailing) {
                    Button("Apply") {
                        onApply(minWidth, minHeight)
                        dismiss()
                    }
                    .bold()
                }
            }
        }
        .presentationDetents([.medium])
    }
}
