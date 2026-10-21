package dk.ingi.roundtripper

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.components.service
import com.intellij.util.xmlb.annotations.Tag
import com.intellij.util.xmlb.annotations.XCollection

/** An executable the user has registered, shown in the editor context menu under [name]. */
@Tag("executable")
data class ExecutableEntry(
    var name: String = "",
    var path: String = "",
)

@Service(Service.Level.APP)
@State(name = "RoundtripperSettings", storages = [Storage("roundtripper.xml")])
class RoundtripperSettings : PersistentStateComponent<RoundtripperSettings.State> {
    class State {
        @XCollection(style = XCollection.Style.v2)
        var executables: MutableList<ExecutableEntry> = mutableListOf()
    }

    private var state = State()

    var executables: List<ExecutableEntry>
        get() = state.executables.map { it.copy() }
        set(value) {
            state.executables = value.map { it.copy() }.toMutableList()
        }

    override fun getState(): State = state

    override fun loadState(state: State) {
        this.state = state
    }

    companion object {
        fun getInstance(): RoundtripperSettings = service()
    }
}
