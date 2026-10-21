package dk.ingi.roundtripper

import com.intellij.execution.ExecutionException
import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.process.CapturingProcessHandler
import com.intellij.execution.process.ProcessOutput
import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.text.StringUtil
import java.io.File
import java.io.IOException
import java.nio.charset.StandardCharsets

/** Runs one configured executable against the current editor and applies its output. */
class RunExecutableAction(private val entry: ExecutableEntry) : DumbAwareAction(entry.name) {

    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

    override fun update(e: AnActionEvent) {
        e.presentation.isEnabledAndVisible = e.project != null && e.getData(CommonDataKeys.EDITOR) != null
    }

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val editor = e.getData(CommonDataKeys.EDITOR) ?: return
        val document = editor.document
        val fileDocumentManager = FileDocumentManager.getInstance()
        val file = fileDocumentManager.getFile(document)

        if (file == null || !file.isInLocalFileSystem) {
            notify(project, NotificationType.ERROR, RoundtripperBundle.message("notification.notLocalFile"))
            return
        }
        if (!document.isWritable) {
            notify(project, NotificationType.ERROR, RoundtripperBundle.message("notification.readOnly"))
            return
        }
        val executable = File(entry.path)
        if (!executable.canExecute()) {
            notify(project, NotificationType.ERROR, RoundtripperBundle.message("notification.notExecutable", entry.path))
            return
        }

        // The executable gets the file's path, so make sure what's on disk matches the editor.
        fileDocumentManager.saveDocument(document)

        val invocation = capture(editor, file.toNioPath().toAbsolutePath().toString())
        document.setReadOnly(true)
        val modificationStamp = document.modificationStamp

        object : Task.Backgroundable(project, RoundtripperBundle.message("task.title", entry.name), true) {
            private var output: ProcessOutput? = null
            private var failure: ExecutionException? = null

            override fun run(indicator: ProgressIndicator) {
                try {
                    output = execute(executable, invocation, indicator)
                } catch (e: ExecutionException) {
                    failure = e
                }
            }

            override fun onFinished() {
                document.setReadOnly(false)
                failure?.let {
                    notify(project, NotificationType.ERROR, RoundtripperBundle.message("notification.failedToStart", it.message ?: it.toString()))
                    return
                }
                val result = output ?: return
                if (result.isCancelled) return
                if (document.modificationStamp != modificationStamp) {
                    notify(project, NotificationType.ERROR, RoundtripperBundle.message("notification.documentChanged"))
                    return
                }
                handleResult(project, document, invocation, result)
            }
        }.queue()
    }

    private fun capture(editor: Editor, filePath: String): Invocation {
        val document = editor.document
        val caret = editor.caretModel.primaryCaret

        fun positionOf(offset: Int): TextPosition {
            val line = document.getLineNumber(offset)
            return TextPosition(line, offset - document.getLineStartOffset(line))
        }

        return if (caret.hasSelection()) {
            Invocation.Text(
                filePath = filePath,
                selectedText = caret.selectedText.orEmpty(),
                start = positionOf(caret.selectionStart),
                end = positionOf(caret.selectionEnd),
                startOffset = caret.selectionStart,
                endOffset = caret.selectionEnd,
            )
        } else {
            Invocation.Position(filePath = filePath, caret = positionOf(caret.offset), caretOffset = caret.offset)
        }
    }

    private fun execute(executable: File, invocation: Invocation, indicator: ProgressIndicator): ProcessOutput {
        val commandLine = GeneralCommandLine(executable.path)
            .withParameters(invocation.arguments)
            .withCharset(StandardCharsets.UTF_8)
            .withWorkDirectory(File(invocation.filePath).parentFile)
            .withParentEnvironmentType(GeneralCommandLine.ParentEnvironmentType.CONSOLE)
        val handler = CapturingProcessHandler(commandLine)

        // Feed stdin from another thread so a process that writes before it finishes reading can't deadlock us.
        val stdin = invocation.stdin
        ApplicationManager.getApplication().executeOnPooledThread {
            try {
                handler.processInput.use { stream -> stdin?.let { stream.write(it.toByteArray(StandardCharsets.UTF_8)) } }
            } catch (e: IOException) {
                // The process exited or closed stdin without reading everything; its exit code tells the story.
                thisLogger().debug("Could not write stdin to ${entry.path}", e)
            }
        }
        return handler.runProcessWithProgressIndicator(indicator)
    }

    private fun handleResult(project: Project, document: Document, invocation: Invocation, result: ProcessOutput) {
        if (result.exitCode != 0) {
            notify(
                project,
                NotificationType.ERROR,
                RoundtripperBundle.message("notification.nonZeroExit", entry.name, result.exitCode) + formatStream(result.stderr),
            )
            return
        }

        val stdout = StringUtil.convertLineSeparators(result.stdout)
        val commandName = RoundtripperBundle.message("command.name", entry.name)
        when (invocation) {
            is Invocation.Text -> WriteCommandAction.runWriteCommandAction(project, commandName, null, {
                document.replaceString(invocation.startOffset, invocation.endOffset, stdout)
            })

            is Invocation.Position -> {
                if (stdout.isNotEmpty()) {
                    WriteCommandAction.runWriteCommandAction(project, commandName, null, {
                        document.insertString(invocation.caretOffset, stdout)
                    })
                }
                if (result.stderr.isNotBlank()) {
                    notify(project, NotificationType.WARNING, RoundtripperBundle.message("notification.stderr", entry.name) + formatStream(result.stderr))
                }
            }
        }
    }

    private fun formatStream(text: String): String {
        if (text.isBlank()) return ""
        val shown = StringUtil.shortenTextWithEllipsis(text.trimEnd(), MAX_NOTIFICATION_CHARS, 0)
        return "<pre>" + StringUtil.escapeXmlEntities(shown) + "</pre>"
    }

    private fun notify(project: Project, type: NotificationType, content: String) {
        NotificationGroupManager.getInstance()
            .getNotificationGroup(NOTIFICATION_GROUP_ID)
            .createNotification(RoundtripperBundle.message("notification.title"), content, type)
            .notify(project)
    }

    private companion object {
        const val NOTIFICATION_GROUP_ID = "Roundtripper"
        const val MAX_NOTIFICATION_CHARS = 4000
    }
}
