package com.example.e_lijekovi.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.e_lijekovi.data.DobaDana
import com.example.e_lijekovi.data.Medication

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MedicationFormScreen(
    onSave: (Medication) -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
    initial: Medication? = null
) {
    // Initialize state using the optional initial medication when editing
    val name = remember(initial) { mutableStateOf(initial?.name ?: "") }
    val dose = remember(initial) { mutableStateOf((initial?.dosePerTake ?: 1).toString()) }
    val total = remember(initial) { mutableStateOf((initial?.totalQuantity ?: 0).toString()) }
    val jutro = remember(initial) { mutableStateOf(initial?.schedule?.contains(DobaDana.JUTRO) ?: false) }
    val podne = remember(initial) { mutableStateOf(initial?.schedule?.contains(DobaDana.PODNE) ?: false) }
    val vecer = remember(initial) { mutableStateOf(initial?.schedule?.contains(DobaDana.VECER) ?: false) }

    Column(modifier = modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(text = if (initial != null) "Uredi lijek" else "Dodaj novi lijek")
        OutlinedTextField(
            value = name.value,
            onValueChange = { name.value = it },
            label = { Text("Naziv") }
        )
        OutlinedTextField(
            value = dose.value,
            onValueChange = { dose.value = it },
            label = { Text("Doza po uzimanju (npr. 1)") }
        )
        OutlinedTextField(
            value = total.value,
            onValueChange = { total.value = it },
            label = { Text("Ukupna količina (npr. 30)") }
        )

        Row(horizontalArrangement = Arrangement.Start) {
            Row(modifier = Modifier.padding(end = 8.dp)) {
                Checkbox(checked = jutro.value, onCheckedChange = { jutro.value = it })
                Spacer(modifier = Modifier.width(4.dp))
                Text(text = "JUTRO")
            }
            Row(modifier = Modifier.padding(end = 8.dp)) {
                Checkbox(checked = podne.value, onCheckedChange = { podne.value = it })
                Spacer(modifier = Modifier.width(4.dp))
                Text(text = "PODNE")
            }
            Row {
                Checkbox(checked = vecer.value, onCheckedChange = { vecer.value = it })
                Spacer(modifier = Modifier.width(4.dp))
                Text(text = "VECER")
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = onCancel) {
                Text("Cancel")
            }
            Button(onClick = {
                val doseInt = dose.value.toIntOrNull() ?: 1
                val totalInt = total.value.toIntOrNull() ?: 0
                val schedule = mutableSetOf<DobaDana>()
                if (jutro.value) schedule.add(DobaDana.JUTRO)
                if (podne.value) schedule.add(DobaDana.PODNE)
                if (vecer.value) schedule.add(DobaDana.VECER)

                val med = if (initial != null) {
                    // preserve id when editing
                    initial.copy(
                        name = name.value.ifBlank { "(no name)" },
                        dosePerTake = doseInt,
                        totalQuantity = totalInt,
                        schedule = schedule
                    )
                } else {
                    Medication(
                        name = name.value.ifBlank { "(no name)" },
                        dosePerTake = doseInt,
                        totalQuantity = totalInt,
                        schedule = schedule
                    )
                }

                onSave(med)
            }) {
                Text("Save")
            }
        }
    }
}
