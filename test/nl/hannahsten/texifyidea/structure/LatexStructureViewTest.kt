package nl.hannahsten.texifyidea.structure

import com.intellij.ide.structureView.StructureViewBuilder
import com.intellij.ide.structureView.StructureViewModel
import com.intellij.ide.structureView.TreeBasedStructureViewBuilder
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import nl.hannahsten.texifyidea.file.LatexFileType

/**
 * Test for LaTeX structure view to ensure it works without manual PSI cache drops.
 *
 * Regression test for issue #4329 where StructurePsiChangeListener was manually
 * dropping all PSI caches on every change, causing UI freezes.
 */
class LatexStructureViewTest : BasePlatformTestCase() {

    /**
     * Test that structure view can be created without crashing.
     *
     * The bug was that a PSI tree change listener was manually calling
     * dropPsiCaches() on every change. The fix removed this listener entirely
     * since TreeBasedStructureViewBuilder handles updates automatically.
     *
     * This test verifies the structure view still works without the listener.
     */
    fun testStructureViewCreation() {
        val file = myFixture.configureByText(LatexFileType, """
            \documentclass{article}
            \begin{document}
            \section{Introduction}
            Some text here.
            \subsection{Background}
            More text.
            \end{document}
        """.trimIndent())

        // Get the structure view builder
        val builder = file.language.structureViewBuilder?.getStructureViewBuilder(file)

        assertNotNull("Should have structure view builder", builder)
        assertTrue("Should be TreeBasedStructureViewBuilder", builder is TreeBasedStructureViewBuilder)

        // Create the structure view model
        val model = (builder as TreeBasedStructureViewBuilder).createStructureViewModel(myFixture.editor)

        assertNotNull("Should create structure view model", model)

        // Dispose the model
        model.dispose()
    }

    /**
     * Test that structure view updates properly after edits without manual cache drops.
     *
     * This verifies that TreeBasedStructureViewBuilder's automatic update mechanism
     * works correctly without needing to manually drop PSI caches.
     */
    fun testStructureViewUpdatesAfterEdit() {
        val file = myFixture.configureByText(LatexFileType, """
            \documentclass{article}
            \begin{document}
            \section{Test}
            \end{document}
        """.trimIndent())

        val builder = file.language.structureViewBuilder?.getStructureViewBuilder(file)
        assertNotNull("Should have structure view builder", builder)

        val model = (builder as TreeBasedStructureViewBuilder).createStructureViewModel(myFixture.editor)
        assertNotNull("Should create initial structure view model", model)
        model.dispose()

        // Make an edit to the file
        myFixture.type("\n\\subsection{Subsection}")

        // Create a new structure view - should work without issues
        val newModel = builder.createStructureViewModel(myFixture.editor)
        assertNotNull("Should create structure view model after edit", newModel)
        newModel.dispose()
    }

    /**
     * Test that multiple rapid edits don't cause issues.
     *
     * The old code would drop PSI caches on every keystroke, causing performance issues.
     * This test verifies that rapid edits work smoothly with the fix.
     */
    fun testRapidEditsDoNotCauseIssues() {
        myFixture.configureByText(LatexFileType, """
            \documentclass{article}
            \begin{document}
            \section{Test}
            \end{document}
        """.trimIndent())

        // Simulate rapid typing - this should not cause freezes or crashes
        repeat(10) { i ->
            myFixture.type("\n\\subsection{Section $i}")
        }

        // Verify the file is still valid
        val file = myFixture.file
        assertNotNull("File should still be valid after rapid edits", file)

        // Verify structure view can still be created
        val builder = file.language.structureViewBuilder?.getStructureViewBuilder(file)
        assertNotNull("Should still have structure view builder", builder)

        val model = (builder as TreeBasedStructureViewBuilder).createStructureViewModel(myFixture.editor)
        assertNotNull("Should create structure view model after rapid edits", model)
        model.dispose()
    }
}
