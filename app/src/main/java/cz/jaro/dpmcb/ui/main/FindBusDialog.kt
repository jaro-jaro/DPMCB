package cz.jaro.dpmcb.ui.main

import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import cz.jaro.dpmcb.R
import cz.jaro.dpmcb.data.entities.BusName
import cz.jaro.dpmcb.data.entities.LongLine
import cz.jaro.dpmcb.data.entities.RegistrationNumber
import cz.jaro.dpmcb.data.entities.SequenceCode
import cz.jaro.dpmcb.data.entities.ShortLine
import cz.jaro.dpmcb.data.entities.div
import cz.jaro.dpmcb.data.entities.isUnknown
import cz.jaro.dpmcb.data.entities.shortLine
import cz.jaro.dpmcb.data.entities.toShortLine
import cz.jaro.dpmcb.data.helperclasses.NavigateFunction
import cz.jaro.dpmcb.data.helperclasses.UtilFunctions.toLastDigits
import cz.jaro.dpmcb.ui.chooser.autoFocus

@Composable
fun FindBusDialog(
    showDialog: Boolean,
    onDismiss: () -> Unit,
    navigate: NavigateFunction,
    closeDrawer: () -> Unit,
    findSequences: (String, (List<Pair<SequenceCode, String>>) -> Unit) -> Unit,
    isOnline: State<Boolean>,
    showToast: (String, Int) -> Unit,
    findBusByRegN: (RegistrationNumber, (BusName?) -> Unit) -> Unit,
    findLine: (ShortLine, (LongLine?) -> Unit) -> Unit,
) {
    var isNotFound by rememberSaveable { mutableStateOf(false) }
    var options by rememberSaveable { mutableStateOf(null as List<Pair<SequenceCode, String>>?) }
    var sequence by rememberSaveable { mutableStateOf("") }
    var name by rememberSaveable(stateSaver = busStateSaver()) { mutableStateOf(BusName("")) }
    var evn by rememberSaveable { mutableStateOf("") }
    var line by rememberSaveable { mutableStateOf("") }
    var number by rememberSaveable { mutableStateOf("") }

    fun confirm(busName: BusName) {
        navigate(
            Route.Bus(
                busName = busName
            )
        )
        onDismiss()
        closeDrawer()
        sequence = ""
        name = BusName("")
        evn = ""
        line = ""
        number = ""
    }

    fun confirmSeq(seqId: SequenceCode) {
        navigate(Route.Sequence(sequence = seqId))
        onDismiss()
        closeDrawer()
        sequence = ""
        options = null
        name = BusName("")
        evn = ""
        line = ""
        number = ""
    }

    fun findSequence(searched: String) = findSequences(searched) {
        if (it.isEmpty()) isNotFound = true
        else if (it.size == 1) confirmSeq(it[0].first)
        else options = it
    }

    if (showDialog) AlertDialog(
        onDismissRequest = {
            onDismiss()
            sequence = ""
            name = BusName("")
            evn = ""
            line = ""
            number = ""
        },
        title = {
            Text(stringResource(id = R.string.find_bus_by_id))
        },
        confirmButton = {
            TextButton(onClick = {
                if (line.isNotEmpty() && number.isNotEmpty()) {
                    if (line.length == 6) confirm(line / number)
                    else if (line.length <= 3) findLine(line.toLastDigits(3).toShortLine()) {
                        if (it != null) confirm(it / number)
                        else {
                            showToast("Linka $line neexistuje", Toast.LENGTH_SHORT)
                            onDismiss()
                        }
                    }
                    else {
                        showToast("Linka $line neexistuje", Toast.LENGTH_SHORT)
                        onDismiss()
                    }
                }
                else if (evn.isNotEmpty()) {
                    if (!isOnline.value) {
                        showToast("Jste offline", Toast.LENGTH_SHORT)
                        onDismiss()
                        return@TextButton
                    }
                    findBusByRegN(RegistrationNumber(evn.toInt())) {
                        if (it == null) {
                            showToast("Vůz ev. č. $evn nebyl nalezen.", Toast.LENGTH_LONG)
                            onDismiss()
                            return@findBusByRegN
                        }
                        confirm(it)
                    }
                } else if (sequence.isNotEmpty()) {
                    findSequence(sequence)
                } else if (name.isUnknown()) {
                    findLine(name.shortLine()) {
                        if (it != null) confirm(it / number)
                        else {
                            showToast("Linka $line neexistuje", Toast.LENGTH_SHORT)
                            onDismiss()
                        }
                    }
                } else confirm(name)
            }) {
                Text("Vyhledat")
            }
        },
        dismissButton = {
            TextButton(onClick = {
                onDismiss()
                sequence = ""
                name = BusName("")
                line = ""
                number = ""
            }) {
                Text("Zrušit")
            }
        },
        text = {
            val focusManager = LocalFocusManager.current
            Column {
                Row {
                    TextField(
                        value = line,
                        onValueChange = {
                            line = it
                        },
                        Modifier
                            .weight(1F)
                            .padding(end = 8.dp)
                            .autoFocus(),
                        label = {
                            Text("Linka")
                        },
                        keyboardActions = KeyboardActions {
                            focusManager.moveFocus(FocusDirection.Right)
                        },
                        keyboardOptions = KeyboardOptions(
                            imeAction = ImeAction.Next,
                            keyboardType = KeyboardType.Number,
                        ),
                    )
                    TextField(
                        value = number,
                        onValueChange = {
                            number = it
                        },
                        Modifier.weight(1F),
                        label = {
                            Text("Č. spoje")
                        },
                        keyboardActions = KeyboardActions {
                            if (line.isNotEmpty() && number.isNotEmpty())
                                if (line.length == 6) confirm(line / number)
                                else if (line.length <= 3) findLine(line.toLastDigits(3).toShortLine()) {
                                    if (it != null) confirm(it / number)
                                    else {
                                        showToast("Linka $line neexistuje", Toast.LENGTH_SHORT)
                                        onDismiss()
                                    }
                                }
                                else {
                                    showToast("Linka $line neexistuje", Toast.LENGTH_SHORT)
                                    onDismiss()
                                }
                            else
                                focusManager.moveFocus(FocusDirection.Down)
                        },
                        keyboardOptions = KeyboardOptions(
                            imeAction = if (line.isNotEmpty() && number.isNotEmpty()) ImeAction.Search else ImeAction.Next,
                            keyboardType = KeyboardType.Number,
                        ),
                    )
                }
                TextField(
                    value = evn,
                    onValueChange = {
                        evn = it
                    },
                    Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    label = {
                        Text("Ev. č. vozu")
                    },
                    keyboardActions = KeyboardActions {
                        if (evn.isNotEmpty()) {
                            if (!isOnline.value) {
                                showToast("Jste offline", Toast.LENGTH_SHORT)
                                onDismiss()
                                return@KeyboardActions
                            }
                            findBusByRegN(RegistrationNumber(evn.toInt())) {
                                if (it == null) {
                                    showToast("Vůz ev. č. $evn nebyl nalezen.", Toast.LENGTH_LONG)
                                    onDismiss()
                                    return@findBusByRegN
                                }
                                confirm(it)
                            }
                        } else
                            focusManager.moveFocus(FocusDirection.Down)
                    },
                    keyboardOptions = KeyboardOptions(
                        imeAction = if (evn.isNotEmpty()) ImeAction.Search else ImeAction.Next,
                        keyboardType = KeyboardType.Number,
                    ),
                )
                TextField(
                    value = name.value,
                    onValueChange = {
                        name = BusName(it)
                    },
                    Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    label = {
                        Text("Jméno spoje")
                    },
                    keyboardActions = KeyboardActions {
                        confirm(name)
                    },
                    keyboardOptions = KeyboardOptions(
                        imeAction = ImeAction.Search,
                    ),
                )
                Text(
                    text = "Najít kurz",
                    Modifier.padding(bottom = 16.dp, top = 16.dp),
                    style = MaterialTheme.typography.headlineSmall
                )
                TextField(
                    value = sequence,
                    onValueChange = {
                        sequence = it
                    },
                    Modifier
                        .fillMaxWidth(),
                    label = {
                        Text("Linka nebo název kurzu")
                    },
                    keyboardActions = KeyboardActions {
                        findSequence(sequence)
                    },
                    keyboardOptions = KeyboardOptions(
                        imeAction = ImeAction.Search,
                    ),
                )
            }
        }
    )

    if (isNotFound) AlertDialog(
        onDismissRequest = {
            isNotFound = false
        },
        title = {
            Text("Kurz nenalezen")
        },
        text = {
            Text("Tento kurz ($sequence) bohužel neexistuje :(\nZkontrolujte, zdali jste ho zadali správně.")
        },
        confirmButton = {
            TextButton(
                onClick = {
                    isNotFound = false
                }
            ) {
                Text("OK")
            }
        }
    )

    if (options != null) AlertDialog(
        onDismissRequest = {
            options = null
        },
        title = {
            Text("Nalezeno více kurzů")
        },
        text = {
            Column(
                Modifier
                    .verticalScroll(rememberScrollState())
            ) {
                Text("\"${sequence}\" by mohlo označovat více kurzů, vyberte který jste měli na mysli:")
                options!!.forEach {
                    HorizontalDivider(Modifier.fillMaxWidth())
                    ListItem(
                        headlineContent = {
                            TextButton(
                                onClick = {
                                    confirmSeq(it.first)
                                }
                            ) {
                                Text(it.second)
                            }
                        }
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    options = null
                }
            ) {
                Text("Zrušit")
            }
        }
    )
}

fun busStateSaver(): Saver<BusName, String> = Saver(
    save = { it.value },
    restore = { BusName(it) }
)
