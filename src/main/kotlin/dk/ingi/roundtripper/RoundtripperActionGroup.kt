package dk.ingi.roundtripper

import com.intellij.openapi.actionSystem.ActionGroup
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.options.ShowSettingsUtil
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.DumbAwareAction

/** The "Roundtripper" editor context sub-menu: one entry per configured executable. */
class RoundtripperActionGroup : ActionGroup(), DumbAware {

    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

    override fun update(e: AnActionEvent) {
        e.presentation.isEnabledAndVisible = e.getData(CommonDataKeys.EDITOR) != null
    }

    override fun getChildren(e: AnActionEvent?): Array<AnAction> {
        val executables = RoundtripperSettings.getInstance().executables
        if (executables.isEmpty()) return arrayOf(OpenSettingsAction())
        return executables.map { RunExecutableAction(it) }.toTypedArray()
    }

    private class OpenSettingsAction : DumbAwareAction(RoundtripperBundle.message("action.configure")) {
        override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

        override fun actionPerformed(e: AnActionEvent) {
            ShowSettingsUtil.getInstance().showSettingsDialog(e.project, RoundtripperConfigurable::class.java)
        }
    }
}
