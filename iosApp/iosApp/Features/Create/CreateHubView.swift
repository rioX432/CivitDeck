import SwiftUI

struct CreateHubView: View {
    @Environment(\.horizontalSizeClass) private var sizeClass

    var body: some View {
        if sizeClass == .regular {
            createContent
        } else {
            NavigationStack {
                createContent
            }
        }
    }

    private var createContent: some View {
        List {
            Section {
                NavigationLink {
                    ComfyUISettingsView()
                } label: {
                    createRow(
                        icon: "sparkles",
                        title: "ComfyUI",
                        description: "Node-based image generation workflow"
                    )
                }

                NavigationLink {
                    SDWebUISettingsView()
                } label: {
                    createRow(
                        icon: "paintbrush",
                        title: "SD WebUI",
                        description: "Stable Diffusion web interface"
                    )
                }

                NavigationLink {
                    ExternalServerSettingsView()
                } label: {
                    createRow(
                        icon: "server.rack",
                        title: "External Server",
                        description: "Connect to a custom generation server"
                    )
                }

                NavigationLink {
                    ModelFileBrowserScreen()
                } label: {
                    createRow(
                        icon: "doc.text",
                        title: "Model Files",
                        description: "Browse and manage local model files"
                    )
                }
            }
        }
        .navigationTitle("Create")
    }

    private func createRow(icon: String, title: String, description: String) -> some View {
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
