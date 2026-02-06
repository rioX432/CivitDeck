import SwiftUI
import Shared

struct ImageViewerScreen: View {
    let images: [CivitImage]
    @Binding var selectedIndex: Int?

    @State private var showMetadata = false
    @State private var controlsVisible = true

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
                    MetadataSheet(meta: meta)
                        .presentationDetents([.medium, .large])
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

            if images[safe: currentIndex]?.meta != nil {
                HStack {
                    Spacer()
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
                .padding(16)
            }
        }
    }
}

// MARK: - Zoomable Image

private struct ZoomableImageView: View {
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
