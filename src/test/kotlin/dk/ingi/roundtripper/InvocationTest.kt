package dk.ingi.roundtripper

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class InvocationTest {
    @Test
    fun `text invocation passes subcommand, path and selection bounds, selection on stdin`() {
        val invocation = Invocation.Text(
            filePath = "/tmp/a.txt",
            selectedText = "hello\nworld",
            start = TextPosition(2, 4),
            end = TextPosition(3, 5),
            startOffset = 10,
            endOffset = 21,
        )
        assertEquals(listOf("text", "/tmp/a.txt", "2", "4", "3", "5"), invocation.arguments)
        assertEquals("hello\nworld", invocation.stdin)
    }

    @Test
    fun `position invocation passes subcommand, path and caret, nothing on stdin`() {
        val invocation = Invocation.Position(filePath = "/tmp/a.txt", caret = TextPosition(0, 7), caretOffset = 7)
        assertEquals(listOf("position", "/tmp/a.txt", "0", "7"), invocation.arguments)
        assertNull(invocation.stdin)
    }
}
