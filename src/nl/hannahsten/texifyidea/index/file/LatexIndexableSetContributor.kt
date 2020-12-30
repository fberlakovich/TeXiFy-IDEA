package nl.hannahsten.texifyidea.index.file

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.util.indexing.IndexableSetContributor
import nl.hannahsten.texifyidea.settings.sdk.LatexSdkUtil

/**
 * Specify the paths that have to be indexed for the [LatexExternalCommandIndex].
 */
class LatexIndexableSetContributor : IndexableSetContributor() {
    override fun getAdditionalProjectRootsToIndex(project: Project): MutableSet<VirtualFile> {
        return LatexSdkUtil.getSdkSourceRoots(project).toMutableSet()
    }

    override fun getAdditionalRootsToIndex() = mutableSetOf<VirtualFile>()
}