package nl.hannahsten.texifyidea.run

import com.intellij.testFramework.fixtures.BasePlatformTestCase
import nl.hannahsten.texifyidea.run.linuxpdfviewer.InternalPdfViewer

/**
 * Test for InternalPdfViewer to ensure lazy loading of conversations prevents class loading errors.
 *
 * Regression test for issue #4323 where SumatraConversation was eagerly instantiated,
 * causing NoClassDefFoundError for DDEException when the DDE library wasn't available.
 */
class InternalPdfViewerTest : BasePlatformTestCase() {

    /**
     * Test that InternalPdfViewer enum can be initialized without causing class loading errors.
     *
     * The bug was that SumatraConversation() was created at enum initialization time,
     * which triggered loading of DDEClientConversation and DDEException classes even
     * when Sumatra wasn't being used.
     *
     * With the fix, the conversation is created lazily via factory function only when
     * accessed, preventing premature class loading.
     */
    fun testEnumInitializationDoesNotLoadSumatraClasses() {
        // Simply accessing the enum values should not cause class loading errors
        // even if DDE library is not available
        val viewers = InternalPdfViewer.values()

        assertNotNull("Should be able to get PDF viewer enum values", viewers)
        assertTrue("Should have multiple PDF viewers", viewers.size > 1)

        // Verify all standard viewers are present
        assertTrue("Should have EVINCE", viewers.any { it.name == "EVINCE" })
        assertTrue("Should have OKULAR", viewers.any { it.name == "OKULAR" })
        assertTrue("Should have SUMATRA", viewers.any { it.name == "SUMATRA" })
        assertTrue("Should have NONE", viewers.any { it.name == "NONE" })
    }

    /**
     * Test that checking availability doesn't cause class loading errors.
     *
     * This exercises the checkAvailability() path which is called during initialization.
     */
    fun testCheckAvailabilityDoesNotCrash() {
        // Check availability for each viewer - this should not crash
        InternalPdfViewer.values().forEach { viewer ->
            try {
                // Just calling checkAvailability() should not throw NoClassDefFoundError
                viewer.checkAvailability()
            } catch (e: NoClassDefFoundError) {
                fail("checkAvailability() for ${viewer.name} should not throw NoClassDefFoundError: ${e.message}")
            }
        }
    }

    /**
     * Test that getting available subset doesn't cause class loading errors.
     *
     * This is a common operation that should work even without optional dependencies.
     */
    fun testAvailableSubsetDoesNotCrash() {
        try {
            // This should complete without throwing NoClassDefFoundError
            val available = InternalPdfViewer.availableSubset()

            assertNotNull("Should get available viewers list", available)
            // Should at least have NONE as available
            assertTrue("Should have at least NONE available", available.any { it == InternalPdfViewer.NONE })
        } catch (e: NoClassDefFoundError) {
            fail("availableSubset() should not throw NoClassDefFoundError: ${e.message}")
        }
    }

    /**
     * Test that accessing conversation property is lazy and doesn't crash during enum init.
     *
     * The conversation should only be created when accessed, not during enum initialization.
     */
    fun testConversationIsLazy() {
        // Getting the enum values should not create any conversations
        val sumatra = InternalPdfViewer.SUMATRA

        assertNotNull("Should be able to get SUMATRA enum", sumatra)

        // Accessing the conversation on a non-Windows system or without DDE library
        // might return null, but should not crash with NoClassDefFoundError
        try {
            val conversation = sumatra.conversation
            // On Windows with DDE library, conversation might be created
            // On other systems or without library, it might be null
            // Either is fine - just shouldn't crash
        } catch (e: NoClassDefFoundError) {
            fail("Accessing conversation property should not throw NoClassDefFoundError during class loading: ${e.message}")
        }
    }
}
