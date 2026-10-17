import XCTest
@testable import BeingMuslim

/// Smoke tests for the app shell.
///
/// Feature-level tests are added alongside each feature
/// (`docs/ARCHITECTURE.md` §8). This verifies the test harness and host app
/// are wired together with the intended identifier namespace.
final class AppShellTests: XCTestCase {
    func testHostAppUsesExpectedIdentifierNamespace() throws {
        let identifier = try XCTUnwrap(Bundle.main.bundleIdentifier)
        XCTAssertTrue(
            identifier.hasPrefix("app.beingmuslim"),
            "Unexpected host app bundle identifier: \(identifier)"
        )
    }
}
