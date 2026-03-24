import SwiftUI

struct LibraryView: View {
    @Environment(\.horizontalSizeClass) private var sizeClass

    var body: some View {
        if sizeClass == .regular {
            libraryContent
        } else {
            NavigationStack {
                libraryContent
            }
        }
    }

    private var libraryContent: some View {
        List {
            Section {
                NavigationLink {
                    CollectionsScreen()
                } label: {
                    libraryRow(
                        icon: "folder",
                        title: "Collections",
                        description: "Organize saved models into collections"
                    )
                }

                NavigationLink {
                    DatasetListView()
                } label: {
                    libraryRow(
                        icon: "photo.stack",
                        title: "Datasets",
                        description: "Manage image datasets for training"
                    )
                }

                NavigationLink {
                    SavedPromptsScreen()
                } label: {
                    libraryRow(
                        icon: "text.quote",
                        title: "Saved Prompts",
                        description: "Reuse and manage your prompt templates"
                    )
                }
            }
        }
        .navigationTitle("Library")
    }

    private func libraryRow(icon: String, title: String, description: String) -> some View {
        HStack(spacing: Spacing.md) {
            Image(systemName: icon)
                .font(.title3)
                .foregroundColor(.civitPrimary)
                .frame(width: 32, height: 32)

            VStack(alignment: .leading, spacing: Spacing.xs) {
                Text(title)
                    .font(.civitTitleSmall)

                Text(description)
                    .font(.civitLabelSmall)
                    .foregroundColor(.civitOnSurfaceVariant)
            }
        }
        .padding(.vertical, Spacing.xs)
    }
}
