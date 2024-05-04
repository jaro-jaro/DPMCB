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
import cz.jaro.dpmcb.data.helperclasses.NavigateFunction
import cz.jaro.dpmcb.ui.chooser.autoFocus
import cz.jaro.dpmcb.ui.destinations.BusDestination
import cz.jaro.dpmcb.ui.destinations.SequenceDestination

@Composable
fun FindBusDialog(
    showDialog: Boolean,
    onDismiss: () -> Unit,
    navigate: NavigateFunction,
    closeDrawer: () -> Unit,
    findSequences: (String, (List<Pair<String, String>>) -> Unit) -> Unit,
    isOnline: State<Boolean>,
    showToast: (String, Int) -> Unit,
    findBusByEvn: (String, (String?) -> Unit) -> Unit,
) {
    var isNotFound by rememberSaveable { mutableStateOf(false) }
    var options by rememberSaveable { mutableStateOf(null as List<Pair<String, String>>?) }
    var id by rememberSaveable { mutableStateOf("") }
    var sequence by rememberSaveable { mutableStateOf("") }
    var name by rememberSaveable { mutableStateOf("") }
    var evn by rememberSaveable { mutableStateOf("") }
    var line by rememberSaveable { mutableStateOf("") }
    var number by rememberSaveable { mutableStateOf("") }

    fun confirm(busId: String) {
        navigate(
            BusDestination(
                busId = busId
            )
        )
        onDismiss()
        closeDrawer()
        id = ""
        sequence = ""
        name = ""
        evn = ""
        line = ""
        number = ""
    }

    fun confirmSeq(seqId: String) {
        navigate(
            SequenceDestination(
                sequence = seqId
            )
        )
        onDismiss()
        closeDrawer()
        id = ""
        sequence = ""
        options = null
        name = ""
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
            id = ""
            sequence = ""
            name = ""
            evn = ""
            line = ""
            number = ""
        },
        title = {
            Text(stringResource(id = R.string.find_bus_by_id))
        },
        confirmButton = {
            TextButton(onClick = {
                if (line.isNotEmpty() && number.isNotEmpty()) confirm(
                    "S-325${
                        when (line.length) {
                            1 -> "00$line"
                            2 -> "0$line"
                            else -> line
                        }
                    }-$number"
                )
                else if (evn.isNotEmpty()) {
                    if (!isOnline.value) {
                        showToast("Jste offline", Toast.LENGTH_SHORT)
                        onDismiss()
                        return@TextButton
                    }
                    findBusByEvn(evn) {
                        if (it == null) {
                            showToast("Vůz ev. č. $evn nebyl nalezen.", Toast.LENGTH_LONG)
                            onDismiss()
                            return@findBusByEvn
                        }
                        confirm(it)
                    }
                } else if (name.isNotEmpty()) confirm("S-${name.replace("/", "-")}")
                else if (sequence.isNotEmpty()) {
                    findSequence(sequence)
                } else confirm(id)
            }) {
                Text("Vyhledat")
            }
        },
        dismissButton = {
            TextButton(onClick = {
                onDismiss()
                id = ""
                sequence = ""
                name = ""
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
                                confirm(
                                    "S-325${
                                        when (line.length) {
                                            1 -> "00$line"
                                            2 -> "0$line"
                                            else -> line
                                        }
                                    }-$number"
                                )
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
                            findBusByEvn(evn) {
                                if (it == null) {
                                    showToast("Vůz ev. č. $evn nebyl nalezen.", Toast.LENGTH_LONG)
                                    onDismiss()
                                    return@findBusByEvn
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
                    value = name,
                    onValueChange = {
                        name = it
                    },
                    Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    label = {
                        Text("Jméno spoje")
                    },
                    keyboardActions = KeyboardActions {
                        if (name.isNotEmpty())
                            confirm("S-${name.replace("/", "-")}")
                        else
                            focusManager.moveFocus(FocusDirection.Down)
                    },
                    keyboardOptions = KeyboardOptions(
                        imeAction = if (name.isNotEmpty()) ImeAction.Search else ImeAction.Next,
                    ),
                )
                TextField(
                    value = id,
                    onValueChange = {
                        id = it
                    },
                    Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    label = {
                        Text("ID spoje")
                    },
                    keyboardActions = KeyboardActions {
                        confirm(id)
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
            Text("Tento kurz ($sequence) bohužel neexistuje :(\nZkontrolujte, zda jste zadali správně ID.")
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