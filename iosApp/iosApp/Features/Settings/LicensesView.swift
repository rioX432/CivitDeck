import SwiftUI

struct LicensesView: View {
    private let libraries = [
        LibraryInfo(name: "Ktor", license: "Apache License 2.0", url: "https://ktor.io"),
        LibraryInfo(name: "Koin", license: "Apache License 2.0", url: "https://insert-koin.io"),
        LibraryInfo(name: "Room (AndroidX)", license: "Apache License 2.0", url: "https://developer.android.com/jetpack/androidx/releases/room"),
        LibraryInfo(name: "Coil", license: "Apache License 2.0", url: "https://coil-kt.github.io/coil"),
        LibraryInfo(name: "SKIE", license: "Apache License 2.0", url: "https://skie.touchlab.co"),
        LibraryInfo(name: "kotlinx-serialization", license: "Apache License 2.0", url: "https://github.com/Kotlin/kotlinx.serialization"),
        LibraryInfo(name: "SQLite (AndroidX)", license: "Apache License 2.0", url: "https://developer.android.com/jetpack/androidx/releases/sqlite"),
    ]

    var body: some View {
        List(libraries) { library in
            VStack(alignment: .leading, spacing: 4) {
                Text(library.name)
                    .font(.civitBodyMedium)
                Text(library.license)
                    .font(.civitBodySmall)
                    .foregroundColor(.civitOnSurfaceVariant)
            }
        }
        .navigationTitle("Open Source Licenses")
        .navigationBarTitleDisplayMode(.inline)
    }
}

private struct LibraryInfo: Identifiable {
    let id = UUID()
    let name: String
    let license: String
    let url: String
}
