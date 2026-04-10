import SwiftUI
import Shared

struct ComfyUIOutputMetadataView: View {
    let meta: ComfyUIGenerationMeta
    @Environment(\.civitTheme) private var theme

    var body: some View {
        VStack(alignment: .leading, spacing: Spacing.sm) {
            if !meta.positivePrompt.isEmpty {
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
            Text(meta.positivePrompt)
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
                    if let seed = meta.seed {
                        metaChip(icon: "number", label: "Seed: \(seed.int64Value)")
                    }
                    if let sampler = meta.samplerName, !sampler.isEmpty {
                        metaChip(icon: "wand.and.stars", label: sampler)
                    }
                    if let cfg = meta.cfg {
                        metaChip(icon: "slider.horizontal.3", label: "CFG: \(String(format: "%.1f", cfg.doubleValue))")
                    }
                    if let steps = meta.steps {
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
                .accessibilityHidden(true)
            Text(label)
                .font(.civitBodySmall)
        }
        .padding(.horizontal, Spacing.sm)
        .padding(.vertical, Spacing.xs)
        .background(Color.civitSurfaceVariant)
        .clipShape(Capsule())
    }
}

struct ComfyUIOutputLoraSection: View {
    let loraNames: [String]
    @Environment(\.civitTheme) private var theme

    var body: some View {
        if !loraNames.isEmpty {
            VStack(alignment: .leading, spacing: Spacing.xs) {
                Text("LoRAs")
                    .font(.civitLabelMedium)
                    .foregroundColor(.civitOnSurfaceVariant)
                ScrollView(.horizontal, showsIndicators: false) {
                    HStack(spacing: Spacing.sm) {
                        ForEach(loraNames, id: \.self) { lora in
                            Text(lora.components(separatedBy: "/").last ?? lora)
                                .font(.civitBodySmall)
                                .padding(.horizontal, Spacing.sm)
                                .padding(.vertical, Spacing.xs)
                                .background(theme.primary.opacity(0.15))
                                .foregroundColor(theme.primary)
                                .clipShape(Capsule())
                        }
                    }
                }
            }
        }
    }
}
