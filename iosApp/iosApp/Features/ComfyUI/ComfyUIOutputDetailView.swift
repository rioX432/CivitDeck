import SwiftUI
import Shared

struct ComfyUIOutputDetailView: View {
    let image: ComfyUIGeneratedImage
    let onSave: (String) -> Void

    @Environment(\.dismiss) private var dismiss
    @State private var showAddCollectionAlert = false

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(alignment: .leading, spacing: Spacing.lg) {
                    fullImage
                    metadataSection
                    loraSection
                    actionButtons
                }
                .padding(Spacing.md)
            }
            .navigationTitle("Detail")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Close") { dismiss() }
                }
            }
            .alert("Add to Collection", isPresented: $showAddCollectionAlert) {
                Button("OK", role: .cancel) {}
            } message: {
                Text("ComfyUI generated images are not linked to a CivitAI model " +
                     "and cannot be added to a model collection.")
            }
        }
    }

    // MARK: - Full Image

    private var fullImage: some View {
        CachedAsyncImage(url: URL(string: image.imageUrl)) { phase in
            switch phase {
            case .success(let img):
                img
                    .resizable()
                    .scaledToFit()
                    .clipShape(RoundedRectangle(cornerRadius: 12))
                    .transition(.opacity)
            case .failure:
                RoundedRectangle(cornerRadius: 12)
                    .fill(Color.civitSurfaceVariant)
                    .frame(height: 200)
                    .overlay {
                        Image(systemName: "photo")
                            .foregroundColor(.civitOnSurfaceVariant)
                    }
            case .empty:
                RoundedRectangle(cornerRadius: 12)
                    .fill(Color.civitSurfaceVariant)
                    .frame(height: 200)
                    .shimmer()
            @unknown default:
                EmptyView()
            }
        }
        .frame(maxWidth: .infinity)
    }

    // MARK: - Metadata

    private var metadataSection: some View {
        VStack(alignment: .leading, spacing: Spacing.sm) {
            if !image.meta.positivePrompt.isEmpty {
                promptSection
            }
            metadataChips
        }
    }

    private var promptSection: some View {
        VStack(alignment: .leading, spacing: Spacing.xs) {
            Text("Prompt")
                .font(.civitLabelMedium)
                .foregroundColor(.civitOnSurfaceVariant)
            Text(image.meta.positivePrompt)
                .font(.civitBodySmall)
        }
    }

    private var metadataChips: some View {
        VStack(alignment: .leading, spacing: Spacing.xs) {
            Text("Parameters")
                .font(.civitLabelMedium)
                .foregroundColor(.civitOnSurfaceVariant)
            ScrollView(.horizontal, showsIndicators: false) {
                HStack(spacing: Spacing.sm) {
                    if let seed = image.meta.seed {
                        metaChip(icon: "number", label: "Seed: \(seed.int64Value)")
                    }
                    if let sampler = image.meta.samplerName, !sampler.isEmpty {
                        metaChip(icon: "wand.and.stars", label: sampler)
                    }
                    if let cfg = image.meta.cfg {
                        metaChip(icon: "slider.horizontal.3", label: "CFG: \(String(format: "%.1f", cfg.doubleValue))")
                    }
                    if let steps = image.meta.steps {
                        metaChip(icon: "arrow.triangle.2.circlepath", label: "Steps: \(steps.int32Value)")
                    }
                }
            }
        }
    }

    private func metaChip(icon: String, label: String) -> some View {
        HStack(spacing: Spacing.xs) {
            Image(systemName: icon)
                .font(.civitBodySmall)
            Text(label)
                .font(.civitBodySmall)
        }
        .padding(.horizontal, Spacing.sm)
        .padding(.vertical, Spacing.xs)
        .background(Color.civitSurfaceVariant)
        .clipShape(Capsule())
    }

    // MARK: - LoRA Section

    @ViewBuilder
    private var loraSection: some View {
        let loras = image.meta.loraNames as? [String] ?? []
        if !loras.isEmpty {
            VStack(alignment: .leading, spacing: Spacing.xs) {
                Text("LoRAs")
                    .font(.civitLabelMedium)
                    .foregroundColor(.civitOnSurfaceVariant)
                ScrollView(.horizontal, showsIndicators: false) {
                    HStack(spacing: Spacing.sm) {
                        ForEach(loras, id: \.self) { lora in
                            Text(lora.components(separatedBy: "/").last ?? lora)
                                .font(.civitBodySmall)
                                .padding(.horizontal, Spacing.sm)
                                .padding(.vertical, Spacing.xs)
                                .background(Color.civitPrimary.opacity(0.15))
                                .foregroundColor(.civitPrimary)
                                .clipShape(Capsule())
                        }
                    }
                }
            }
        }
    }

    // MARK: - Action Buttons

    private var actionButtons: some View {
        HStack(spacing: Spacing.sm) {
            Button {
                onSave(image.imageUrl)
                dismiss()
            } label: {
                Label("Save to Photos", systemImage: "star.fill")
                    .frame(maxWidth: .infinity)
            }
            .buttonStyle(.borderedProminent)

            Button {
                showAddCollectionAlert = true
            } label: {
                Label("Add to Collection", systemImage: "plus")
                    .frame(maxWidth: .infinity)
            }
            .buttonStyle(.bordered)
        }
    }
}
