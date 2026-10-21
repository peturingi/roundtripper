package dk.ingi.roundtripper

import com.intellij.openapi.options.Configurable
import com.intellij.openapi.options.ConfigurationException
import com.intellij.ui.ToolbarDecorator
import com.intellij.ui.table.TableView
import com.intellij.util.ui.ColumnInfo
import com.intellij.util.ui.ListTableModel
import javax.swing.JComponent

/** Settings page listing the executables as an editable (friendly name, path) table. */
class RoundtripperConfigurable : Configurable {
    private val model = ListTableModel<ExecutableEntry>(NameColumn(), PathColumn())
    private var table: TableView<ExecutableEntry>? = null

    override fun getDisplayName(): String = RoundtripperBundle.message("settings.displayName")

    override fun createComponent(): JComponent {
        val table = TableView(model).apply {
            setShowGrid(false)
            emptyText.text = RoundtripperBundle.message("settings.table.empty")
        }
        this.table = table
        return ToolbarDecorator.createDecorator(table)
            .setAddAction {
                table.stopEditing()
                model.addRow(ExecutableEntry())
                val row = model.rowCount - 1
                table.selectionModel.setSelectionInterval(row, row)
                table.editCellAt(row, 0)
                table.editorComponent?.requestFocusInWindow()
            }
            .createPanel()
    }

    override fun isModified(): Boolean = model.items != RoundtripperSettings.getInstance().executables

    override fun apply() {
        table?.stopEditing()
        val entries = model.items.map { it.copy(name = it.name.trim(), path = it.path.trim()) }
        entries.forEachIndexed { index, entry ->
            if (entry.name.isEmpty()) {
                throw ConfigurationException(RoundtripperBundle.message("settings.error.missingName", index + 1))
            }
            if (entry.path.isEmpty()) {
                throw ConfigurationException(RoundtripperBundle.message("settings.error.missingPath", entry.name))
            }
        }
        entries.groupBy { it.name }.filterValues { it.size > 1 }.keys.firstOrNull()?.let {
            throw ConfigurationException(RoundtripperBundle.message("settings.error.duplicateName", it))
        }
        RoundtripperSettings.getInstance().executables = entries
        model.items = entries.toMutableList()
    }

    override fun reset() {
        model.items = RoundtripperSettings.getInstance().executables.toMutableList()
    }

    override fun disposeUIResources() {
        table = null
    }

    private abstract class EditableColumn(name: String) : ColumnInfo<ExecutableEntry, String>(name) {
        override fun isCellEditable(item: ExecutableEntry): Boolean = true
    }

    private class NameColumn : EditableColumn(RoundtripperBundle.message("settings.column.name")) {
        override fun valueOf(item: ExecutableEntry): String = item.name
        override fun setValue(item: ExecutableEntry, value: String) {
            item.name = value
        }
    }

    private class PathColumn : EditableColumn(RoundtripperBundle.message("settings.column.path")) {
        override fun valueOf(item: ExecutableEntry): String = item.path
        override fun setValue(item: ExecutableEntry, value: String) {
            item.path = value
        }
    }
}
