package nl.hannahsten.texifyidea.psi

import com.intellij.psi.PsiDocumentManager
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import nl.hannahsten.texifyidea.file.LatexFileType
import nl.hannahsten.texifyidea.util.parser.childrenOfType
import org.junit.Test

class LatexConditionalParsingTest : BasePlatformTestCase() {

    @Test
    fun `test conditional parsing`() {
        // given
        myFixture.configureByText(
            LatexFileType,
            """
            \begin{document}
                \lstinputlisting[param1=value1,param2=value2,param3]{some/file}
                \othercommand[foo,bar]{foobar}
            \end{document}
            """.trimIndent()
        )

        // when
        val commands =
            PsiDocumentManager.getInstance(myFixture.project).getPsiFile(myFixture.editor.document)!!.children.first()
                .childrenOfType(LatexCommands::class).toList()

        val inputListing = commands[0]
        val other = commands[1]

        val listingOptionalParams = inputListing.parameterList[0]
        assertNotNull(listingOptionalParams.optionalParam)
        assertEmpty(listingOptionalParams.optionalParam!!.optionalParamContentList)
        assertNotEmpty(listingOptionalParams.optionalParam!!.optionalKeyValPairList)
        val keyValList = listingOptionalParams.optionalParam!!.optionalKeyValPairList
        assertEquals(keyValList[0].optionalKeyValKey.text, "param1")
        assertEquals(keyValList[0].keyValValue!!.text, "value1")

        val otherOptionalParams = other.parameterList[0]
        assertNotNull(otherOptionalParams.optionalParam)
        assertNotEmpty(otherOptionalParams.optionalParam!!.optionalParamContentList)
        assertEmpty(otherOptionalParams.optionalParam!!.optionalKeyValPairList)
    }


}