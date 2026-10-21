# Roundtripper

Roundtripper is an IntelliJ Platform plugin that sends editor text to an external executable and puts the
executable's output back into the file.

Select some text, right-click, choose an executable from the **Roundtripper** sub-menu, and the selection is
replaced by whatever the executable writes to stdout. With nothing selected, the output is inserted at the caret.
Any language can be used to write the executable: a shell script, Python, a compiled binary.

## Requirements

- IntelliJ IDEA or PyCharm Professional, version 2026.2.3 or later.
  The plugin depends only on the IntelliJ Platform, so other IntelliJ-based IDEs of the same version should work, but they are untested.
- macOS or Linux. Windows is untested.

## Installation

The plugin is not on JetBrains Marketplace. Build it from source:

```sh
./gradlew buildPlugin
```

This produces `build/distributions/roundtripper-<version>.zip`. In the IDE, open
**Settings | Plugins**, click the gear icon, choose **Install Plugin from Disk…**, and select the zip.

## Setup

Open **Settings | Tools | Roundtripper** and add one row per executable:

| Column          | Meaning                                                        |
|-----------------|----------------------------------------------------------------|
| Name            | The label shown in the context menu. Must be unique.           |
| Executable Path | The absolute path to the executable. It must be executable.    |

## Usage

Right-click in an editor and open the **Roundtripper** sub-menu. It lists every configured executable by name.
If none are configured, it shows **Configure Executables…** instead.

Clicking a name saves the file and then runs the executable, in one of two modes depending on whether text is selected.
While the executable runs, the file is read-only. A progress indicator appears in the status bar, and the run can be cancelled there.

### Arguments

The executable runs with the file's directory as its working directory. Its arguments are:

```
text     <path> <startRow> <startCol> <endRow> <endCol>
position <path> <row> <col>
```

- **Argument 0** is the mode, `text` or `position`.
- **Argument 1** is the absolute path to the file in the editor.
- **The remaining arguments** are rows and columns, all 0-based.
  Columns count characters from the start of the line, so a tab counts as one.

### `text` mode: some text is selected

- **stdin** receives the selected text, encoded as UTF-8.
- **Arguments 2–5** give the start and end of the selection.
- **Exit code 0:** the selection is replaced by everything the executable wrote to stdout.
- **Any other exit code:** the file is left unchanged and stdout is discarded. A notification shows the exit code
  and what the executable wrote to stderr.

### `position` mode: nothing is selected

- **stdin** is closed immediately.
- **Arguments 2–3** give the caret position.
- **Exit code 0:** everything the executable wrote to stdout, if anything, is inserted at the caret. If the executable
  wrote to stderr, a warning notification shows it.
- **Any other exit code:** the file is left unchanged and stdout is discarded. A notification shows the exit code
  and what the executable wrote to stderr.

### Other behaviour

- **The file changed during the run:** another tool could still change the file while the executable runs. If that
  happens, the output is discarded and a notification says so.
- **Multiple carets:** only the primary caret is used.
- **Line endings:** `\r\n` in the output is converted to the editor's line endings.
- **Long notifications:** the stderr shown in a notification is cut off after 4,000 characters.

### Example executable

Keep executables outside the files you plan to edit: Roundtripper saves the file before each run, so an executable
that edits itself will run whatever you have just typed into it.

```python
#!/usr/bin/env python3
"""Uppercase the selection, or insert the arguments at the caret."""
import sys

mode, path = sys.argv[1], sys.argv[2]
if mode == "text":
    sys.stdout.write(sys.stdin.read().upper())
else:
    row, col = sys.argv[3], sys.argv[4]
    print(f"{path}:{row}:{col}")
```

Save it as, for example, `~/bin/upper.py`, run `chmod +x ~/bin/upper.py`, and add it under
**Settings | Tools | Roundtripper**.

## Development

| Command                          | What it does                                             |
|----------------------------------|----------------------------------------------------------|
| `./gradlew runIde`               | Starts a sandbox IDE with the plugin installed.          |
| `./gradlew test`                 | Runs the unit tests.                                     |
| `./gradlew buildPlugin`          | Builds the installable zip into `build/distributions/`.  |
| `./gradlew verifyPluginStructure`| Checks `plugin.xml` and the plugin layout.               |

The `.run/` directory has matching run configurations for IntelliJ IDEA.

## Authorship

Roundtripper's code, tests and documentation were written by Claude, an AI model made by Anthropic,
working in Claude Code. The repository owner hosts it.

## License

Roundtripper is published by INGI Consulting ApS under the experimental
[INGI Agent-Authored Derivatives License](LICENSE). In short:

- **Anyone may run the unmodified plugin**, for any purpose.
- **AI agents may modify and redistribute it freely.** Rights go to the person or company running the agent, and
  only for derived works that contain no content written or edited by a human.
- **Derived works with any human-written content** may be used only privately by the people who made them.
  They may not be distributed or used commercially.

The license has not been reviewed by a lawyer and may be partly unenforceable. Read [LICENSE](LICENSE) for the actual terms.
