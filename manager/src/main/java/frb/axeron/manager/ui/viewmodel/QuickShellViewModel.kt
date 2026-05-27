package frb.axeron.manager.ui.viewmodel

import android.app.Application
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.core.content.edit
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import frb.axeron.api.Axeron
import frb.axeron.api.AxeronCommandSession
import frb.axeron.api.core.AxeronSettings
import frb.axeron.api.utils.AnsiFilter
import frb.axeron.manager.R
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch

class QuickShellViewModel(application: Application) : AndroidViewModel(application) {

    var session: AxeronCommandSession = AxeronCommandSession()
    private var savedCommand: TextFieldValue? = null

    init {
        session.setProcessListener(object : AxeronCommandSession.ProcessListener {
            override fun onProcessCreated(pid: Int, command: String) {
                Log.i("QuickShellViewModel", "onProcessCreated: $pid")
                if (command.lines().size > 1) {
                    append(OutputType.TYPE_COMMAND, "[command]")
                    append(OutputType.TYPE_COMMAND, command.trim())
                } else append(OutputType.TYPE_COMMAND, "[command] ${command.trim()}")
                append(OutputType.TYPE_START, "[start] pid=$pid")
                savedCommand = TextFieldValue(text = command, selection = TextRange(command.length))
                if (isClearCommandEnabled) commandText = TextFieldValue("") // clear input
                execMode = "Inputs"
                isRunning = true
            }

            override fun onProcessRunning(input: String) {
                Log.i("QuickShellViewModel", "onProcessRunning: $input")
                val tagInput = if (input.lines().size > 1) "[input]\n" else "[input] "
                append(OutputType.TYPE_STDIN, tagInput + input.trim())
                if (isClearCommandEnabled) commandText = TextFieldValue("")
            }

            override fun onProcessFinished(exitCode: Int, lastOutput: String) {
                Log.i("QuickShellViewModel", "onProcessFinished: $exitCode")
                if (!AnsiFilter.isScreenControl(lastOutput)) {
                    append(OutputType.TYPE_EXIT, "[exit] code=$exitCode")
                    append(OutputType.TYPE_SPACE, "")
                }
                execMode = "Commands"
                if (!isClearCommandEnabled && savedCommand != null) {
                    commandText = savedCommand!!
                    savedCommand = null
                }
                isRunning = false
            }
        })

        session.setResultListener(object : AxeronCommandSession.ResultListener {
            override fun output(output: CharSequence?) {
                output?.let {
                    append(OutputType.TYPE_STDOUT, it.toString())
                }
            }

            override fun onError(error: CharSequence?) {
                error?.let {
                    append(OutputType.TYPE_STDERR, it.toString())
                }
            }

        })
    }

    private val prefs = AxeronSettings.getPreferences()

    enum class OutputType(
        val labelId: Int,
        val color: Color? = null
    ) {
        TYPE_COMMAND(R.string.type_command),
        TYPE_START(
            R.string.type_start,
            Color(0xFF00FFFF)
        ),
        TYPE_STDIN(R.string.type_stdin),
        TYPE_STDOUT(R.string.type_stdout),
        TYPE_STDERR(
            R.string.type_stderr,
            Color(0xFFEF4444)
        ),
        TYPE_THROW(
            R.string.type_throw,
            Color(0xFFEF4444)
        ),
        TYPE_SPACE(R.string.type_space),
        TYPE_EXIT(
            R.string.type_exit,
            Color(0xFFF59E0B)
        )
    }

    enum class KeyEventType(val labelId: Int) {
        VOLUME_UP(R.string.volume_up),
        VOLUME_DOWN(R.string.volume_down)
    }

    data class Output(val type: OutputType, var output: String, var completed: Boolean = false)

    var isShellRestrictionEnabled: Boolean by mutableStateOf(
        prefs.getBoolean("shell_restriction", true)
    )
        private set

    fun setShellRestriction(enable: Boolean) {
        isShellRestrictionEnabled = enable
        prefs.edit {
            putBoolean("shell_restriction", enable)
        }
    }

    var isCompatModeEnabled: Boolean by mutableStateOf(
        prefs.getBoolean("shell_compat_mode", true)
    )
        private set

    fun setCompatMode(enable: Boolean) {
        isCompatModeEnabled = enable
        prefs.edit {
            putBoolean("shell_compat_mode", enable)
        }
    }

    var isClearCommandEnabled: Boolean by mutableStateOf(
        prefs.getBoolean("clear_command", true)
    )
        private set

    fun setClearCommand(enable: Boolean) {
        isClearCommandEnabled = enable
        prefs.edit {
            putBoolean("clear_command", enable)
        }
    }

    private val _output = MutableSharedFlow<Output>(extraBufferCapacity = 64)
    val output: SharedFlow<Output> = _output

    var isRunning by mutableStateOf(false)
        private set

    var commandText by mutableStateOf(TextFieldValue(""))
        private set

    var clear by mutableStateOf(false)
        private set

    var execMode by mutableStateOf("Commands")
        private set

    private var savedCommandsList by mutableStateOf(loadSavedCommands())
    val savedCommands: List<String> get() = savedCommandsList

    private fun loadSavedCommands(): MutableList<String> {
        val json = prefs.getString("saved_commands", null) ?: return mutableListOf()
        return try {
            Gson().fromJson(json, object : TypeToken<MutableList<String>>() {}.type)
        } catch (_: Exception) { mutableListOf() }
    }

    private fun persistSavedCommands() {
        prefs.edit { putString("saved_commands", Gson().toJson(savedCommandsList)) }
    }

    fun saveCurrentCommand() {
        val trimmed = commandText.text.trim()
        if (trimmed.isEmpty() || trimmed in savedCommandsList) return
        savedCommandsList = (savedCommandsList + trimmed).toMutableList()
        persistSavedCommands()
    }

    fun removeSavedCommand(index: Int) {
        savedCommandsList = savedCommandsList.toMutableList().apply { removeAt(index) }
        persistSavedCommands()
    }

    fun loadSavedCommand(index: Int) {
        val cmd = savedCommandsList.getOrNull(index) ?: return
        commandText = TextFieldValue(text = cmd, selection = TextRange(cmd.length))
    }

    fun setCommand(text: TextFieldValue) {
        commandText = text
    }

    fun clear() {
        //make a toggle state
        clear = !clear
    }

    fun stop() {
        session.killSession()
    }

    fun runShell() {
        val cmd = commandText.text.ifBlank { return }
            .replace(Regex("[^\\p{Print}\\n]"), "") // sanitize

        session.runCommand(cmd, isCompatModeEnabled)
    }

    private fun append(type: OutputType, output: String) {
        viewModelScope.launch {
            _output.emit(Output(type, output))
        }
    }

    override fun onCleared() {
        super.onCleared()
        if (Axeron.pingBinder()) stop()
    }
}


