import SwiftUI
import Shared
import Photos

struct ComfyUIOutputDetailView: View {
    let image: ComfyUIGeneratedImage

    @State private var showAddCollectionAlert = false
    @State private var imageSaveSuccess: Bool?
    @State private var showSaveAlert = false
    @State private var showImageViewer = false
    @State private var showDatasetPicker = false
    @State private var datasets: [DatasetCollection] = []

    private let saveImageUseCase = KoinHelper.shared.getSaveGeneratedImageUseCase()
    private let observeDatasetUseCase = KoinHelper.shared.getObserveDatasetCollectionsUseCase()
    private let addToDatasetUseCase = KoinHelper.shared.getAddImageToDatasetUseCase()
    private let createDatasetUseCase = KoinHelper.shared.getCreateDatasetCollectionUseCase()

    var body: some View {
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
            ToolbarItem(placement: .topBarTrailing) {
                Button {
                    showDatasetPicker = true
                } label: {
                    Image(systemName: "folder.badge.plus")
                }
            }
        }
        .sheet(isPresented: $showDatasetPicker) {
            AddToDatasetSheet(
                datasets: datasets,
                onSelectDataset: { datasetId in
                    showDatasetPicker = false
                    onDatasetSelected(datasetId: datasetId)
                },
                onCreateAndSelect: { name in
                    showDatasetPicker = false
                    onCreateDatasetAndSelect(name: name)
                }
            )
        }
        .task {
            for await list in observeDatasetUseCase.invoke() {
                datasets = list.compactMap { $0 as? DatasetCollection }
            }
        }
        .alert("Add to Collection", isPresented: $showAddCollectionAlert) {
            Button("OK", role: .cancel) {}
        } message: {
            Text("ComfyUI generated images are not linked to a CivitAI model " +
                 "and cannot be added to a model collection.")
        }
        .alert(
            imageSaveSuccess == true ? "Saved to Photos" : "Save failed",
            isPresented: $showSaveAlert
        ) {
            Button("OK") { imageSaveSuccess = nil }
        }
        .onChange(of: imageSaveSuccess) { newValue in
            if newValue != nil { showSaveAlert = true }
        }
        .fullScreenCover(isPresented: $showImageViewer) {
            SingleImageViewer(url: image.imageUrl, isPresented: $showImageViewer)
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
        .onTapGesture { showImageViewer = true }
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
                onSaveImage()
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

    // MARK: - Dataset

    private func onDatasetSelected(datasetId: Int64) {
        let tags = buildTags()
        Task {
            _ = try? await addToDatasetUseCase.invoke(
                datasetId: datasetId,
                imageUrl: image.imageUrl,
                sourceType: ImageSource.generated,
                trainable: true,
                tags: tags
            )
        }
    }

    private func onCreateDatasetAndSelect(name: String) {
        let tags = buildTags()
        Task {
            if let datasetId = try? await createDatasetUseCase.invoke(name: name, description: "") {
                _ = try? await addToDatasetUseCase.invoke(
                    datasetId: datasetId.int64Value,
                    imageUrl: image.imageUrl,
                    sourceType: ImageSource.generated,
                    trainable: true,
                    tags: tags
                )
            }
        }
    }

    private func buildTags() -> [String] {
        var tags: [String] = []
        if let seed = image.meta.seed {
            tags.append("seed:\(seed.int64Value)")
        }
        if let sampler = image.meta.samplerName, !sampler.isEmpty {
            tags.append("sampler:\(sampler)")
        }
        if !image.meta.positivePrompt.isEmpty {
            tags.append("prompt_hash:\(abs(image.meta.positivePrompt.hashValue))")
        }
        return tags
    }

    // MARK: - Save

    private func onSaveImage() {
        Task {
            do {
                let success = try await saveImageUseCase.invoke(url: image.imageUrl, filename: "civitdeck_history")
                imageSaveSuccess = success.boolValue
            } catch {
                imageSaveSuccess = false
            }
        }
    }
}

// MARK: - Single Image Viewer

private struct SingleImageViewer: View {
    let url: String
    @Binding var isPresented: Bool

    @State private var controlsVisible = true
    @State private var dragOffset: CGFloat = 0
    @State private var toastMessage: String?

    private var backgroundOpacity: Double {
        let progress = abs(dragOffset) / 100
        return Double(max(1.0 - progress / 4.0, 0.0))
    }

    var body: some View {
        ZStack {
            Color.black
                .opacity(backgroundOpacity)
                .ignoresSafeArea()

            ZoomableImageView(
                url: url,
                onFocusModeChanged: { isFocusMode in controlsVisible = !isFocusMode },
                onDismiss: { isPresented = false },
                onDragYChanged: { dragOffset = $0 }
            )
            .ignoresSafeArea()

            if controlsVisible && dragOffset == 0 {
                viewerControls
                    .transition(.opacity)
            }

            if let message = toastMessage {
                toastView(message: message)
            }
        }
        .animation(MotionAnimation.fast, value: controlsVisible)
    }

    // MARK: - Controls

    private var viewerControls: some View {
        VStack {
            HStack {
                controlCircleButton(systemName: "xmark", label: "Close") {
                    isPresented = false
                }
                Spacer()
            }
            .padding(Spacing.lg)
            Spacer()
            HStack {
                Spacer()
                controlCircleButton(systemName: "arrow.down.to.line", label: "Save") {
                    saveImage()
                }
            }
            .padding(Spacing.lg)
        }
    }

    private func controlCircleButton(
        systemName: String,
        label: String,
        action: @escaping () -> Void
    ) -> some View {
        Button(action: action) {
            Image(systemName: systemName)
                .font(.title3)
                .fontWeight(.semibold)
                .foregroundColor(.civitInverseOnSurface)
                .padding(Spacing.smPlus)
                .background(.ultraThinMaterial, in: Circle())
        }
        .accessibilityLabel(label)
    }

    // MARK: - Save

    private func saveImage() {
        guard let url = URL(string: url) else {
            showToast("Download failed")
            return
        }
        Task {
            do {
                let request = URLRequest(url: url, cachePolicy: .returnCacheDataElseLoad)
                let (data, _) = try await ImageURLSession.shared.data(for: request)
                guard let image = UIImage(data: data) else {
                    showToast("Download failed")
                    return
                }
                let status = await PHPhotoLibrary.requestAuthorization(for: .addOnly)
                guard status == .authorized || status == .limited else {
                    showToast("Permission denied")
                    return
                }
                try await PHPhotoLibrary.shared().performChanges {
                    PHAssetChangeRequest.creationRequestForAsset(from: image)
                }
                showToast("Saved to Photos")
            } catch {
                showToast("Download failed")
            }
        }
    }

    // MARK: - Toast

    private func showToast(_ message: String) {
        withAnimation(MotionAnimation.fast) { toastMessage = message }
        DispatchQueue.main.asyncAfter(deadline: .now() + 2) {
            withAnimation(MotionAnimation.fast) { toastMessage = nil }
        }
    }

    private func toastView(message: String) -> some View {
        VStack {
            Spacer()
            Text(message)
                .font(.subheadline)
                .fontWeight(.medium)
                .foregroundColor(.civitInverseOnSurface)
                .padding(.horizontal, Spacing.lg)
                .padding(.vertical, Spacing.smPlus)
                .background(.ultraThinMaterial, in: Capsule())
                .padding(.bottom, 80)
        }
        .transition(.opacity)
    }
}
