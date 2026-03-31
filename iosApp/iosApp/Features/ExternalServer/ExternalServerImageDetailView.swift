import SwiftUI
import Shared
import Photos

struct ExternalServerImageDetailView: View {
    let images: [ServerImage]
    let initialIndex: Int
    @State private var currentIndex: Int
    @Environment(\.dismiss) private var dismiss

    init(images: [ServerImage], initialIndex: Int) {
        self.images = images
        self.initialIndex = initialIndex
        self._currentIndex = State(initialValue: initialIndex)
    }

    var body: some View {
        TabView(selection: $currentIndex) {
            ForEach(Array(images.enumerated()), id: \.element.id) { idx, image in
                ServerImageDetailPage(image: image)
                    .tag(idx)
            }
        }
        .tabViewStyle(.page(indexDisplayMode: .never))
        .navigationTitle("\(currentIndex + 1) / \(images.count)")
        .navigationBarTitleDisplayMode(.inline)
        .toolbar {
            ToolbarItem(placement: .cancellationAction) {
                Button("Close") { dismiss() }
            }
        }
    }
}

// MARK: - Detail Page

private struct ServerImageDetailPage: View {
    let image: ServerImage
    @State private var showImageViewer = false
    @State private var showShareSheet = false
    @StateObject private var shareHashtagVM = ShareHashtagViewModel()

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: Spacing.md) {
                CivitAsyncImageView(
                    imageUrl: image.file,
                    contentMode: .fit,
                    aspectRatio: nil
                )
                .simultaneousGesture(TapGesture().onEnded { showImageViewer = true })
                .contentShape(Rectangle())

                VStack(alignment: .leading, spacing: Spacing.md) {
                    if let prompt = image.prompt, !prompt.isEmpty {
                        PromptSection(prompt: prompt)
                    }
                    Divider()
                    DetailsSection(image: image)
                }
                .padding(.horizontal, Spacing.lg)
                .padding(.bottom, Spacing.lg)
            }
        }
        .toolbar {
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
        .fullScreenCover(isPresented: $showImageViewer) {
            ServerImageViewer(url: image.file, isPresented: $showImageViewer)
        }
    }
}

// MARK: - Prompt Section

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

// MARK: - Details Section

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

// MARK: - Fullscreen Image Viewer

private struct ServerImageViewer: View {
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
            Color.civitScrim
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

    private var viewerControls: some View {
        VStack {
            HStack {
                circleButton(systemName: "xmark", label: "Close") {
                    isPresented = false
                }
                Spacer()
            }
            .padding(Spacing.lg)
            Spacer()
            HStack {
                Spacer()
                circleButton(systemName: "arrow.down.to.line", label: "Save") {
                    saveImage()
                }
            }
            .padding(Spacing.lg)
        }
    }

    private func circleButton(
        systemName: String,
        label: String,
        action: @escaping () -> Void
    ) -> some View {
        Button(action: action) {
            Image(systemName: systemName)
                .font(.title3)
                .fontWeight(.semibold)
                .foregroundColor(.white)
                .padding(Spacing.smPlus)
                .background(.ultraThinMaterial, in: Circle())
        }
        .accessibilityLabel(label)
    }

    private func saveImage() {
        guard let imageUrl = URL(string: url) else {
            showToast("Download failed")
            return
        }
        Task {
            do {
                let request = URLRequest(url: imageUrl, cachePolicy: .returnCacheDataElseLoad)
                let (data, _) = try await ImageURLSession.shared.data(for: request)
                guard let uiImage = UIImage(data: data) else {
                    showToast("Download failed")
                    return
                }
                let status = await PHPhotoLibrary.requestAuthorization(for: .addOnly)
                guard status == .authorized || status == .limited else {
                    showToast("Permission denied")
                    return
                }
                try await PHPhotoLibrary.shared().performChanges {
                    PHAssetChangeRequest.creationRequestForAsset(from: uiImage)
                }
                showToast("Saved to Photos")
            } catch {
                showToast("Download failed")
            }
        }
    }

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
                .foregroundColor(.white)
                .padding(.horizontal, Spacing.lg)
                .padding(.vertical, Spacing.smPlus)
                .background(.ultraThinMaterial, in: Capsule())
                .padding(.bottom, Spacing.floatingOffset)
        }
        .transition(.opacity)
    }
}
