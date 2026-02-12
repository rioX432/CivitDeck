import SwiftUI
import Shared

// MARK: - Carousel Viewer

struct CarouselViewer: View {
    let images: [ModelImage]
    @Binding var selectedIndex: Int?

    var body: some View {
        if let startIndex = selectedIndex, !images.isEmpty {
            ZStack {
                Color.black.ignoresSafeArea()

                TabView(selection: Binding(
                    get: { startIndex },
                    set: { selectedIndex = $0 }
                )) {
                    ForEach(Array(images.enumerated()), id: \.offset) { i, image in
                        ZoomableImageView(
                            url: image.url,
                            pageIndex: i,
                            currentPageIndex: startIndex
                        )
                        .ignoresSafeArea()
                        .tag(i)
                    }
                }
                .tabViewStyle(.page(indexDisplayMode: .automatic))

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
                }
            }
        }
    }
}

// MARK: - Image Grid Sheet

struct ImageGridSheet: View {
    let images: [ModelImage]
    let onDismiss: () -> Void
    let onImageSelected: (Int) -> Void

    var body: some View {
        NavigationStack {
            ScrollView {
                LazyVGrid(
                    columns: [
                        GridItem(.flexible(), spacing: Spacing.sm),
                        GridItem(.flexible(), spacing: Spacing.sm),
                    ],
                    spacing: Spacing.sm
                ) {
                    ForEach(Array(images.enumerated()), id: \.element.url) { index, image in
                        gridImageCell(image: image, index: index)
                    }
                }
                .padding(Spacing.sm)
            }
            .navigationTitle("Version Images (\(images.count))")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Close") { onDismiss() }
                }
            }
        }
    }

    private func gridImageCell(image: ModelImage, index: Int) -> some View {
        let aspectRatio = (image.width > 0 && image.height > 0)
            ? CGFloat(image.width) / CGFloat(image.height) : 1.0
        return Button {
            onDismiss()
            DispatchQueue.main.asyncAfter(deadline: .now() + 0.3) {
                onImageSelected(index)
            }
        } label: {
            AsyncImage(url: URL(string: image.url)) { phase in
                switch phase {
                case .success(let img):
                    img.resizable().scaledToFill().transition(.opacity)
                case .failure:
                    Rectangle().fill(Color.civitSurfaceVariant)
                        .overlay { SwiftUI.Image(systemName: "photo")
                            .foregroundColor(.civitOnSurfaceVariant) }
                case .empty:
                    Rectangle().fill(Color.civitSurfaceVariant).shimmer()
                @unknown default:
                    EmptyView()
                }
            }
            .aspectRatio(aspectRatio, contentMode: .fill)
            .clipped()
            .clipShape(RoundedRectangle(cornerRadius: CornerRadius.image))
        }
        .buttonStyle(.plain)
    }
}

// MARK: - Grid Image Viewer

struct GridImageViewer: View {
    let images: [ModelImage]
    @Binding var selectedIndex: Int?

    var body: some View {
        if let startIndex = selectedIndex, !images.isEmpty {
            ZStack {
                Color.black.ignoresSafeArea()
                TabView(selection: Binding(
                    get: { startIndex },
                    set: { selectedIndex = $0 }
                )) {
                    ForEach(Array(images.enumerated()), id: \.offset) { i, image in
                        ZoomableImageView(
                            url: image.url,
                            pageIndex: i,
                            currentPageIndex: startIndex
                        )
                        .ignoresSafeArea()
                        .tag(i)
                    }
                }
                .tabViewStyle(.page(indexDisplayMode: .automatic))

                VStack {
                    HStack {
                        Button { selectedIndex = nil } label: {
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
                }
            }
        }
    }
}

// MARK: - Helpers

func htmlToPlainText(_ html: String) -> String {
    guard let data = html.data(using: .utf8),
          let attributedString = try? NSAttributedString(
            data: data,
            options: [
                .documentType: NSAttributedString.DocumentType.html,
                .characterEncoding: String.Encoding.utf8.rawValue,
            ],
            documentAttributes: nil
          ) else {
        return html
    }
    return attributedString.string
}

// MARK: - Wrapping HStack for Tags

struct WrappingHStack: View {
    let tags: [String]

    var body: some View {
        LazyVGrid(
            columns: [GridItem(.adaptive(minimum: 80), spacing: Spacing.sm)],
            alignment: .leading,
            spacing: Spacing.sm
        ) {
            ForEach(tags, id: \.self) { tag in
                Text(tag)
                    .font(.civitLabelMedium)
                    .padding(.horizontal, 10)
                    .padding(.vertical, Spacing.xs)
                    .background(Color.civitSurfaceVariant)
                    .clipShape(Capsule())
            }
        }
    }
}
