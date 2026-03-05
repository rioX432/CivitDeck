import SwiftUI
import Shared

// MARK: - Notes Section

struct ModelNotesSection: View {
    let note: ModelNote?
    let onSave: (String) -> Void
    @State private var isEditing = false
    @State private var editText = ""

    var body: some View {
        VStack(alignment: .leading, spacing: Spacing.sm) {
            noteHeader
            if isEditing {
                noteEditor
            } else {
                noteDisplay
            }
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(.horizontal, Spacing.lg)
        .onChange(of: note?.noteText) { _ in
            if !isEditing { editText = note?.noteText ?? "" }
        }
        .onAppear { editText = note?.noteText ?? "" }
    }

    private var noteHeader: some View {
        HStack {
            Divider()
            Text("My Notes")
                .font(.civitTitleSmall)
            Spacer()
            if !isEditing {
                Button {
                    editText = note?.noteText ?? ""
                    isEditing = true
                } label: {
                    Image(systemName: note != nil ? "pencil" : "plus")
                        .accessibilityLabel(note != nil ? "Edit note" : "Add note")
                        .font(.caption)
                }
            }
        }
    }

    private var noteDisplay: some View {
        Group {
            if let text = note?.noteText, !text.isEmpty {
                Text(text)
                    .font(.civitBodyMedium)
                    .foregroundColor(.civitOnSurface)
            } else {
                Text("Tap + to add a personal note")
                    .font(.civitBodySmall)
                    .foregroundColor(.civitOnSurfaceVariant)
                    .onTapGesture { isEditing = true }
            }
        }
    }

    private var noteEditor: some View {
        VStack(alignment: .trailing, spacing: Spacing.sm) {
            TextEditor(text: $editText)
                .font(.civitBodyMedium)
                .frame(minHeight: 60, maxHeight: 120)
                .overlay(
                    RoundedRectangle(cornerRadius: CornerRadius.image)
                        .stroke(Color.civitOutline, lineWidth: 1)
                )
            HStack(spacing: Spacing.sm) {
                Button("Cancel") {
                    editText = note?.noteText ?? ""
                    isEditing = false
                }
                .font(.civitLabelMedium)
                Button("Save") {
                    onSave(editText)
                    isEditing = false
                }
                .font(.civitLabelMedium)
            }
        }
    }
}

// MARK: - Personal Tags Section

struct PersonalTagsSection: View {
    let tags: [PersonalTag]
    let onAdd: (String) -> Void
    let onRemove: (String) -> Void
    @State private var showAddField = false
    @State private var newTagText = ""

    var body: some View {
        VStack(alignment: .leading, spacing: Spacing.sm) {
            tagsHeader
            tagChips
            if showAddField {
                tagInput
            }
            if tags.isEmpty && !showAddField {
                Text("Tap + to add personal tags")
                    .font(.civitBodySmall)
                    .foregroundColor(.civitOnSurfaceVariant)
            }
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(.horizontal, Spacing.lg)
    }

    private var tagsHeader: some View {
        HStack {
            Divider()
            Text("My Tags")
                .font(.civitTitleSmall)
            Spacer()
            Button {
                showAddField.toggle()
            } label: {
                Image(systemName: "plus")
                    .accessibilityLabel("Add tag")
                    .font(.caption)
            }
        }
    }

    private var tagChips: some View {
        LazyVGrid(
            columns: [GridItem(.adaptive(minimum: 80), spacing: Spacing.sm)],
            alignment: .leading,
            spacing: Spacing.sm
        ) {
            ForEach(tags, id: \.tag) { tag in
                tagChip(tag.tag)
            }
        }
    }

    private func tagChip(_ tag: String) -> some View {
        HStack(spacing: 4) {
            Text(tag)
                .font(.civitLabelMedium)
                .lineLimit(1)
            Button {
                onRemove(tag)
            } label: {
                Image(systemName: "xmark")
                    .accessibilityLabel("Remove tag")
                    .font(.system(size: 10, weight: .semibold))
            }
        }
        .padding(.horizontal, Spacing.smPlus)
        .padding(.vertical, Spacing.xs)
        .background(Color.civitPrimary.opacity(0.15))
        .foregroundColor(.civitPrimary)
        .clipShape(Capsule())
    }

    private var tagInput: some View {
        HStack(spacing: Spacing.sm) {
            TextField("Tag name", text: $newTagText)
                .textFieldStyle(.roundedBorder)
                .font(.civitBodyMedium)
                .onSubmit { submitTag() }
            Button("Add") { submitTag() }
                .font(.civitLabelMedium)
                .disabled(newTagText.trimmingCharacters(in: .whitespaces).isEmpty)
        }
    }

    private func submitTag() {
        let trimmed = newTagText.trimmingCharacters(in: .whitespaces)
        guard !trimmed.isEmpty else { return }
        onAdd(trimmed)
        newTagText = ""
    }
}
