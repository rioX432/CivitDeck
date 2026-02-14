import SwiftUI
import Shared

struct HiddenModelsView: View {
    let models: [HiddenModelEntity]
    let onUnhide: (Int64) -> Void

    var body: some View {
        List {
            if models.isEmpty {
                Text("No hidden models.\nLong-press a model card on the Search screen to hide it.")
                    .foregroundColor(.civitOnSurfaceVariant)
            } else {
                ForEach(models, id: \.modelId) { model in
                    HStack {
                        Text(model.modelName)
                            .font(.civitBodyMedium)
                        Spacer()
                        Button("Unhide") {
                            onUnhide(model.modelId)
                        }
                        .foregroundColor(.civitPrimary)
                    }
                }
            }
        }
        .navigationTitle("Hidden Models")
        .navigationBarTitleDisplayMode(.inline)
    }
}
