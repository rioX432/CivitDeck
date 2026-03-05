import SwiftUI

struct SearchBarView: View {
    @Binding var text: String
    var placeholder: String = "Search"
    var onSubmit: (() -> Void)?
    var isFocused: FocusState<Bool>.Binding?

    var body: some View {
        HStack {
            Image(systemName: "magnifyingglass")
                .accessibilityHidden(true)
                .foregroundColor(.civitOnSurfaceVariant)
            textField
            if !text.isEmpty {
                Button {
                    text = ""
                } label: {
                    Image(systemName: "xmark.circle.fill")
                        .accessibilityLabel("Clear search")
                        .foregroundColor(.civitOnSurfaceVariant)
                }
            }
        }
        .padding(Spacing.smPlus)
        .overlay(
            RoundedRectangle(cornerRadius: CornerRadius.searchBar)
                .stroke(Color.civitOutlineVariant, lineWidth: 1)
        )
        .padding(.horizontal, Spacing.lg)
        .padding(.vertical, Spacing.sm)
    }

    @ViewBuilder
    private var textField: some View {
        if let isFocused {
            TextField(placeholder, text: $text)
                .submitLabel(.search)
                .focused(isFocused)
                .onSubmit { onSubmit?() }
        } else {
            TextField(placeholder, text: $text)
                .submitLabel(.search)
                .onSubmit { onSubmit?() }
        }
    }
}
