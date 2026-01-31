import SwiftUI
import Shared

struct ModelSearchScreen: View {
    @StateObject private var viewModel = ModelSearchViewModel()

    private let columns = [
        GridItem(.flexible(), spacing: 12),
        GridItem(.flexible(), spacing: 12),
    ]

    var body: some View {
        NavigationStack {
            VStack(spacing: 0) {
                searchBar
                typeFilterChips

                ZStack {
                    if viewModel.isLoading && viewModel.models.isEmpty {
                        ProgressView()
                    } else if let error = viewModel.error, viewModel.models.isEmpty {
                        errorView(message: error)
                    } else if viewModel.models.isEmpty && !viewModel.isLoading {
                        emptyView
                    } else {
                        modelGrid
                    }
                }
                .frame(maxHeight: .infinity)
            }
            .navigationTitle("CivitDeck")
            .navigationBarTitleDisplayMode(.inline)
            .toolbarBackground(.visible, for: .navigationBar)
            .navigationDestination(for: Int64.self) { modelId in
                ModelDetailScreen(modelId: modelId)
            }
        }
    }

    private var searchBar: some View {
        HStack {
            Image(systemName: "magnifyingglass")
                .foregroundColor(.secondary)
            TextField("Search models...", text: $viewModel.query)
                .submitLabel(.search)
                .onSubmit {
                    viewModel.onSearch()
                }
            if !viewModel.query.isEmpty {
                Button {
                    viewModel.query = ""
                } label: {
                    Image(systemName: "xmark.circle.fill")
                        .foregroundColor(.secondary)
                }
            }
        }
        .padding(10)
        .overlay(
            RoundedRectangle(cornerRadius: 8)
                .stroke(Color(.systemGray4), lineWidth: 1)
        )
        .padding(.horizontal, 16)
        .padding(.vertical, 8)
    }

    private var modelGrid: some View {
        ScrollView {
            LazyVGrid(columns: columns, spacing: 12) {
                ForEach(Array(viewModel.models.enumerated()), id: \.element.id) { index, model in
                    NavigationLink(value: model.id) {
                        ModelCardView(model: model)
                    }
                    .buttonStyle(.plain)
                    .onAppear {
                        if index == viewModel.models.count - 3 {
                            viewModel.loadMore()
                        }
                    }
                }
            }
            .padding(.horizontal, 12)

            if viewModel.isLoadingMore {
                ProgressView()
                    .padding()
            }
        }
        .refreshable {
            viewModel.refresh()
        }
    }

    private var typeFilterChips: some View {
        ScrollView(.horizontal, showsIndicators: false) {
            HStack(spacing: 8) {
                chipButton(label: "All", isSelected: viewModel.selectedType == nil) {
                    viewModel.onTypeSelected(nil)
                }
                ForEach(modelTypeOptions, id: \.self) { type in
                    chipButton(label: type.name, isSelected: viewModel.selectedType == type) {
                        viewModel.onTypeSelected(type)
                    }
                }
            }
            .padding(.horizontal, 12)
            .padding(.vertical, 8)
        }
    }

    private func chipButton(label: String, isSelected: Bool, action: @escaping () -> Void) -> some View {
        Button(action: action) {
            Text(label)
                .font(.caption)
                .fontWeight(isSelected ? .semibold : .regular)
                .padding(.horizontal, 12)
                .padding(.vertical, 6)
                .background(
                    isSelected
                        ? Color.accentColor.opacity(0.2)
                        : Color(.systemGray5)
                )
                .foregroundColor(isSelected ? .accentColor : .primary)
                .clipShape(Capsule())
        }
    }

    private func errorView(message: String) -> some View {
        VStack(spacing: 16) {
            Text(message)
                .foregroundColor(.red)
                .multilineTextAlignment(.center)
            Button("Retry") {
                viewModel.refresh()
            }
            .buttonStyle(.bordered)
        }
        .padding()
    }

    private var emptyView: some View {
        VStack(spacing: 8) {
            Image(systemName: "magnifyingglass")
                .font(.largeTitle)
                .foregroundColor(.secondary)
            Text("No models found")
                .foregroundColor(.secondary)
        }
    }
}

private let modelTypeOptions: [ModelType] = [
    .checkpoint, .lora, .loCon, .controlnet,
    .textualInversion, .hypernetwork, .upscaler, .vae,
    .poses, .wildcards, .workflows, .motionModule,
    .aestheticGradient, .other,
]
