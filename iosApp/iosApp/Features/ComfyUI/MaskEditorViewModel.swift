import Foundation
import Shared
import UIKit

@MainActor
final class MaskEditorViewModelOwner: ObservableObject {
    let vm: Feature_comfyuiMaskEditorViewModel
    private let store = ViewModelStore()

    @Published var pathSegments: [Feature_comfyuiPathSegment] = []
    @Published var brushSize: Float = 40
    @Published var isEraserMode = false
    @Published var isInverted = false
    @Published var isUploading = false
    @Published var uploadError: String?
    @Published var uploadedMaskFilename: String?
    @Published var canUndo = false
    @Published var canRedo = false
    @Published var hasContent = false

    init() {
        vm = KoinHelper.shared.createMaskEditorViewModel()
        store.put(key: "MaskEditorViewModel", viewModel: vm)
    }

    deinit { store.clear() }

    func observeUiState() async {
        for await state in vm.uiState {
            pathSegments = state.pathSegments as? [Feature_comfyuiPathSegment] ?? []
            brushSize = state.brushSize
            isEraserMode = state.isEraserMode
            isInverted = state.isInverted
            isUploading = state.isUploading
            uploadError = state.uploadError
            uploadedMaskFilename = state.uploadedMaskFilename
            canUndo = state.canUndo
            canRedo = state.canRedo
            hasContent = state.hasContent
        }
    }

    // MARK: - Actions

    func onStrokeCompleted(_ points: [(Float, Float)]) {
        let kotlinPairs: [KotlinPair<KotlinFloat, KotlinFloat>] = points.map { point in
            KotlinPair(first: KotlinFloat(value: point.0), second: KotlinFloat(value: point.1))
        }
        vm.onStrokeCompleted(points: kotlinPairs)
    }

    func onUndo() { vm.onUndo() }
    func onRedo() { vm.onRedo() }
    func onClear() { vm.onClear() }
    func onToggleEraser() { vm.onToggleEraser() }
    func onInvertMask() { vm.onInvertMask() }

    func onBrushSizeChanged(_ size: Float) {
        brushSize = size
        vm.onBrushSizeChanged(size: size)
    }

    func onUploadMask(_ pngBytes: Data) {
        let byteArray = pngBytes.toKotlinByteArray()
        vm.onUploadMask(maskPngBytes: byteArray)
    }
}

private extension Data {
    func toKotlinByteArray() -> KotlinByteArray {
        let byteArray = KotlinByteArray(size: Int32(count))
        for (index, byte) in enumerated() {
            byteArray.set(index: Int32(index), value: Int8(bitPattern: byte))
        }
        return byteArray
    }
}
