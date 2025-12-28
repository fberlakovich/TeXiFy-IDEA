package nl.hannahsten.texifyidea.index

import com.intellij.testFramework.fixtures.BasePlatformTestCase
import nl.hannahsten.texifyidea.file.LatexFileType

/**
 * Test for IndexUtilBase to ensure no deadlocks occur when accessing stub indices.
 *
 * Regression test for issue #4354/#4327 where nested stub index operations caused deadlocks.
 */
class IndexUtilBaseTest : BasePlatformTestCase() {

    override fun getTestDataPath(): String {
        return ""
    }

    /**
     * Test that getting all items from the commands index doesn't cause a deadlock.
     *
     * The bug was that getItems() called getKeys() and then immediately called getItemsByName()
     * for each key within the same iteration, creating a nested stub index operation that
     * could deadlock.
     *
     * With the fix, keys are collected first before processing, avoiding the nested operation.
     */
    fun testGetItemsDoesNotDeadlock() {
        // Create a simple LaTeX file with some commands
        myFixture.configureByText(LatexFileType, """
            \documentclass{article}
            \begin{document}
            \section{Test}
            \textbf{Bold text}
            \end{document}
        """.trimIndent())

        // This should complete without deadlocking
        // The old code could deadlock here due to nested stub index operations
        val commands = LatexCommandsIndex.getItems(project)

        // Verify we got some commands
        assertTrue("Should find commands in the file", commands.isNotEmpty())
    }

    /**
     * Test that getting items in a file set doesn't cause a deadlock.
     *
     * This exercises the getItemsInFileSet path which also calls getItems internally.
     */
    fun testGetItemsInFileSetDoesNotDeadlock() {
        val file = myFixture.configureByText(LatexFileType, """
            \documentclass{article}
            \begin{document}
            \newcommand{\mycommand}{test}
            \mycommand
            \end{document}
        """.trimIndent())

        // This should complete without deadlocking
        val commands = LatexCommandsIndex.getItemsInFileSet(file)

        // Verify we got some commands
        assertTrue("Should find commands in the file", commands.isNotEmpty())
    }

    /**
     * Test that getting environments doesn't cause a deadlock.
     *
     * This tests a different index to ensure the fix applies to all IndexUtilBase subclasses.
     */
    fun testGetEnvironmentsDoesNotDeadlock() {
        myFixture.configureByText(LatexFileType, """
            \documentclass{article}
            \begin{document}
            \begin{itemize}
                \item Test
            \end{itemize}
            \end{document}
        """.trimIndent())

        // This should complete without deadlocking
        val environments = LatexEnvironmentsIndex.getItems(project)

        // Verify we got some environments
        assertTrue("Should find environments in the file", environments.isNotEmpty())
    }
}
