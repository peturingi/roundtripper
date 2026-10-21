package dk.ingi.roundtripper

/** A 0-based (row, column) position in a document. */
data class TextPosition(val row: Int, val column: Int)

/**
 * The editor state captured at the moment an executable is invoked, together with the
 * arguments and stdin it implies. Offsets are document offsets used to apply the result.
 */
sealed class Invocation {
    abstract val filePath: String
    abstract val arguments: List<String>
    abstract val stdin: String?

    /** Text was selected: `text <path> <startRow> <startCol> <endRow> <endCol>`, selection on stdin. */
    data class Text(
        override val filePath: String,
        val selectedText: String,
        val start: TextPosition,
        val end: TextPosition,
        val startOffset: Int,
        val endOffset: Int,
    ) : Invocation() {
        override val arguments: List<String>
            get() = listOf(
                "text", filePath,
                start.row.toString(), start.column.toString(),
                end.row.toString(), end.column.toString(),
            )
        override val stdin: String get() = selectedText
    }

    /** Nothing was selected: `position <path> <row> <col>`, nothing on stdin. */
    data class Position(
        override val filePath: String,
        val caret: TextPosition,
        val caretOffset: Int,
    ) : Invocation() {
        override val arguments: List<String>
            get() = listOf("position", filePath, caret.row.toString(), caret.column.toString())
        override val stdin: String? get() = null
    }
}
