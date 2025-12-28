package nl.hannahsten.texifyidea.psi

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiManager
import com.intellij.psi.PsiTreeChangeEvent
import com.intellij.psi.PsiTreeChangeListener
import com.intellij.util.Alarm

/**
 * @author Hannah Schellekens
 */
class StructurePsiChangeListener(val project: Project) : PsiTreeChangeListener {

    // Use alarm to debounce cache updates and avoid UI freezes
    private val alarm = Alarm(Alarm.ThreadToUse.POOLED_THREAD, project)
    private val DEBOUNCE_DELAY_MS = 500

    private fun updateTracker() {
        // Cancel any pending cache drops and schedule a new one
        alarm.cancelAllRequests()
        alarm.addRequest({
            ApplicationManager.getApplication().runReadAction {
                if (!project.isDisposed) {
                    PsiManager.getInstance(project).dropPsiCaches()
                }
            }
        }, DEBOUNCE_DELAY_MS)
    }

    override fun beforeChildAddition(psiTreeChangeEvent: PsiTreeChangeEvent) {
        // Do nothing.
    }

    override fun beforeChildRemoval(psiTreeChangeEvent: PsiTreeChangeEvent) {
        // Do nothing.
    }

    override fun beforeChildReplacement(psiTreeChangeEvent: PsiTreeChangeEvent) {
        // Do nothing.
    }

    override fun beforeChildMovement(psiTreeChangeEvent: PsiTreeChangeEvent) {
        // Do nothing.
    }

    override fun beforeChildrenChange(psiTreeChangeEvent: PsiTreeChangeEvent) {
        // Do nothing.
    }

    override fun beforePropertyChange(psiTreeChangeEvent: PsiTreeChangeEvent) {
        // Do nothing.
    }

    override fun childAdded(psiTreeChangeEvent: PsiTreeChangeEvent) {
        updateTracker()
    }

    override fun childRemoved(psiTreeChangeEvent: PsiTreeChangeEvent) {
        updateTracker()
    }

    override fun childReplaced(psiTreeChangeEvent: PsiTreeChangeEvent) {
        updateTracker()
    }

    override fun childrenChanged(psiTreeChangeEvent: PsiTreeChangeEvent) {
        updateTracker()
    }

    override fun childMoved(psiTreeChangeEvent: PsiTreeChangeEvent) {
        updateTracker()
    }

    override fun propertyChanged(psiTreeChangeEvent: PsiTreeChangeEvent) {
        // Do nothing
    }
}
