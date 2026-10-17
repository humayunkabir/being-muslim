package app.beingmuslim.android

import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Smoke test that the JVM unit-test source set and JUnit are wired up.
 *
 * Feature and calculation tests are added alongside their features
 * (`docs/ARCHITECTURE.md` §8); this only verifies the harness runs.
 */
class ProjectSetupTest {
    @Test
    fun unitTestSourceSetIsWired() {
        assertTrue("JUnit is wired up for :app unit tests", true)
    }
}
