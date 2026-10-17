import SwiftUI

/// Placeholder home screen for the app shell.
///
/// Real screens are added as product scope is finalized
/// (`docs/PRODUCT_REQUIREMENTS.md` §2).
struct RootView: View {
    var body: some View {
        NavigationStack {
            ContentUnavailableView {
                Label("Being Muslim", systemImage: "book.closed")
            } description: {
                Text("The app shell is running. Features are added as product scope is finalized.")
            }
            .navigationTitle("Being Muslim")
        }
    }
}

#Preview {
    RootView()
}
