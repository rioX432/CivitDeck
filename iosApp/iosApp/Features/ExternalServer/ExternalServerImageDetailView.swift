import SwiftUI
import Shared

struct ExternalServerImageDetailView: View {
    let image: ServerImage
    @Environment(\.dismiss) private var dismiss
    @State private var showShareSheet = false
    @StateObject private var shareHashtagVM = ShareHashtagViewModel()

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: Spacing.md) {
                // Full image
                CivitAsyncImageView(
                    imageUrl: image.file,
                    contentMode: .fit,
                    aspectRatio: nil
                )

                // Metadata
                VStack(alignment: .leading, spacing: Spacing.md) {
                    // Prompt section
                    if let prompt = image.prompt, !prompt.isEmpty {
                        PromptSection(prompt: prompt)
                    }

                    Divider()

                    // Details
                    DetailsSection(image: image)
                }
                .padding(.horizontal, Spacing.lg)
                .padding(.bottom, Spacing.lg)
            }
        }
        .navigationTitle(image.character ?? "Image Detail")
        .navigationBarTitleDisplayMode(.inline)
        .toolbar {
            ToolbarItem(placement: .cancellationAction) {
                Button("Close") { dismiss() }
            }
            ToolbarItem(placement: .topBarTrailing) {
                Button {
                    showShareSheet = true
                } label: {
                    Image(systemName: "square.and.arrow.up")
                        .accessibilityLabel("Share")
                }
            }
        }
        .sheet(isPresented: $showShareSheet) {
            SocialShareSheet(
                hashtags: shareHashtagVM.hashtags,
                onToggle: { tag, enabled in shareHashtagVM.toggle(tag: tag, isEnabled: enabled) },
                onAdd: { tag in shareHashtagVM.add(tag: tag) },
                onRemove: { tag in shareHashtagVM.remove(tag: tag) }
            )
            .presentationDetents([.medium, .large])
        }
        .task { await shareHashtagVM.startObserving() }
    }
}

private struct PromptSection: View {
    let prompt: String

    var body: some View {
        VStack(alignment: .leading, spacing: Spacing.sm) {
            HStack {
                Text("Prompt")
                    .font(.civitTitleSmall)
                Spacer()
                Button {
                    UIPasteboard.general.string = prompt
                } label: {
                    Image(systemName: "doc.on.doc")
                        .accessibilityLabel("Copy")
                        .font(.civitBodySmall)
                }
            }

            Text(prompt)
                .font(.civitBodySmall)
                .padding(Spacing.md)
                .frame(maxWidth: .infinity, alignment: .leading)
                .background(Color.civitSurfaceVariant)
                .cornerRadius(Spacing.sm)
        }
    }
}

private struct DetailsSection: View {
    let image: ServerImage

    var body: some View {
        VStack(alignment: .leading, spacing: Spacing.sm) {
            Text("Details")
                .font(.civitTitleSmall)

            if let character = image.character {
                MetadataRow(label: "Character", value: character)
            }
            if let costume = image.costume {
                MetadataRow(label: "Costume", value: costume)
            }
            if let scenario = image.scenario {
                MetadataRow(label: "Scenario", value: scenario)
            }
            if let score = image.aestheticScore {
                MetadataRow(label: "Aesthetic Score", value: String(format: "%.2f", Double(score)))
            }
            if let seed = image.seed {
                MetadataRow(label: "Seed", value: "\(seed)")
            }
            if let postStatus = image.postStatus {
                MetadataRow(label: "Post Status", value: postStatus)
            }
            MetadataRow(label: "NSFW", value: image.nsfw ? "Yes" : "No")
            if let createdAt = image.createdAt {
                MetadataRow(label: "Created", value: createdAt)
            }
        }
    }
}

private struct MetadataRow: View {
    let label: String
    let value: String

    var body: some View {
        HStack {
            Text(label)
                .font(.civitBodyMedium)
                .foregroundColor(.civitOnSurfaceVariant)
            Spacer()
            Text(value)
                .font(.civitBodyMedium)
        }
    }
}
