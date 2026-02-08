import SwiftUI
import Shared

struct ImageViewerScreen: View {
    let images: [CivitImage]
    @Binding var selectedIndex: Int?
    var onSavePrompt: (ImageGenerationMeta, String) -> Void = { _, _ in }

    @State private var showMetadata = false
    @State private var controlsVisible = true
    @State private var showShareSheet = false

    var body: some View {
        if let index = selectedIndex {
            ZStack {
                Color.black.ignoresSafeArea()

                TabView(selection: Binding(
                    get: { index },
                    set: { selectedIndex = $0 }
                )) {
                    ForEach(Array(images.enumerated()), id: \.element.id) { i, image in
                        ZoomableImageView(url: image.url)
                            .tag(i)
                    }
                }
                .tabViewStyle(.page(indexDisplayMode: .automatic))

                if controlsVisible {
                    viewerControls(currentIndex: index)
                        .transition(.opacity)
                }
            }
            .animation(MotionAnimation.fast, value: controlsVisible)
            .onTapGesture {
                controlsVisible.toggle()
            }
            .sheet(isPresented: $showMetadata) {
                if let meta = images[safe: index]?.meta {
                    MetadataSheet(meta: meta) {
                        onSavePrompt(meta, images[safe: index]?.url ?? "")
                    }
                    .presentationDetents([.medium, .large])
                }
            }
            .sheet(isPresented: $showShareSheet) {
                if let image = images[safe: index] {
                    let text = Self.formatShareText(imageUrl: image.url, meta: image.meta)
                    ShareSheet(items: [text])
                }
            }
        }
    }

    // MARK: - Controls

    private func viewerControls(currentIndex: Int) -> some View {
        VStack {
            HStack {
                Button {
                    selectedIndex = nil
                } label: {
                    SwiftUI.Image(systemName: "xmark")
                        .font(.title3)
                        .fontWeight(.semibold)
                        .foregroundColor(.white)
                        .padding(10)
                        .background(.ultraThinMaterial, in: Circle())
                }
                Spacer()
            }
            .padding(16)

            Spacer()

            HStack {
                Spacer()
                Button {
                    showShareSheet = true
                } label: {
                    SwiftUI.Image(systemName: "square.and.arrow.up")
                        .font(.title3)
                        .foregroundColor(.white)
                        .padding(10)
                        .background(.ultraThinMaterial, in: Circle())
                }
                if images[safe: currentIndex]?.meta != nil {
                    Button {
                        showMetadata = true
                    } label: {
                        SwiftUI.Image(systemName: "info.circle")
                            .font(.title3)
                            .foregroundColor(.white)
                            .padding(10)
                            .background(.ultraThinMaterial, in: Circle())
                    }
                }
            }
            .padding(16)
        }
    }

    // MARK: - Share Text

    static func formatShareText(imageUrl: String, meta: ImageGenerationMeta?) -> String {
        var text = imageUrl + "\n"
        if let meta {
            text += "\n"
            if let prompt = meta.prompt {
                text += "Prompt: \(prompt)\n"
            }
            if let negative = meta.negativePrompt {
                text += "Negative: \(negative)\n"
            }
            var params: [String] = []
            if let model = meta.model { params.append("Model: \(model)") }
            if let steps = meta.steps { params.append("Steps: \(steps)") }
            if let cfg = meta.cfgScale { params.append("CFG: \(cfg)") }
            if let sampler = meta.sampler { params.append("Sampler: \(sampler)") }
            if !params.isEmpty {
                text += params.joined(separator: " | ") + "\n"
            }
        }
        text += "\nShared via CivitDeck"
        return text
    }
}

// MARK: - Share Sheet

private struct ShareSheet: UIViewControllerRepresentable {
    let items: [Any]

    func makeUIViewController(context: Context) -> UIActivityViewController {
        UIActivityViewController(activityItems: items, applicationActivities: nil)
    }

    func updateUIViewController(_ uiViewController: UIActivityViewController, context: Context) {}
}

// MARK: - Zoomable Image

struct ZoomableImageView: View {
    let url: String

    @State private var scale: CGFloat = 1.0
    @State private var lastScale: CGFloat = 1.0
    @State private var offset: CGSize = .zero
    @State private var lastOffset: CGSize = .zero

    var body: some View {
        GeometryReader { geometry in
            AsyncImage(url: URL(string: url)) { phase in
                switch phase {
                case .success(let image):
                    image
                        .resizable()
                        .scaledToFit()
                        .scaleEffect(scale)
                        .offset(offset)
                        .gesture(zoomGesture)
                        .gesture(panGesture)
                        .onTapGesture(count: 2) {
                            withAnimation(MotionAnimation.springBouncy) {
                                if scale > 1.0 {
                                    scale = 1.0
                                    lastScale = 1.0
                                    offset = .zero
                                    lastOffset = .zero
                                } else {
                                    scale = 2.5
                                    lastScale = 2.5
                                }
                            }
                        }
                        .transition(.opacity)
                case .failure:
                    SwiftUI.Image(systemName: "photo")
                        .foregroundColor(.gray)
                case .empty:
                    ProgressView()
                        .tint(.white)
                @unknown default:
                    EmptyView()
                }
            }
            .frame(width: geometry.size.width, height: geometry.size.height)
        }
    }

    private var zoomGesture: some Gesture {
        MagnificationGesture()
            .onChanged { value in
                let newScale = lastScale * value
                scale = min(max(newScale, 0.5), 5.0)
            }
            .onEnded { value in
                lastScale = scale
                if scale < 1.0 {
                    withAnimation(MotionAnimation.springBouncy) {
                        scale = 1.0
                        lastScale = 1.0
                        offset = .zero
                        lastOffset = .zero
                    }
                }
            }
    }

    private var panGesture: some Gesture {
        DragGesture()
            .onChanged { value in
                if scale > 1.0 {
                    offset = CGSize(
                        width: lastOffset.width + value.translation.width,
                        height: lastOffset.height + value.translation.height
                    )
                }
            }
            .onEnded { _ in
                lastOffset = offset
            }
    }
}

// MARK: - Safe Array Access

private extension Array {
    subscript(safe index: Int) -> Element? {
        indices.contains(index) ? self[index] : nil
    }
}
