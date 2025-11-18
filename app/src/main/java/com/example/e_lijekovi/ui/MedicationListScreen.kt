package com.example.e_lijekovi.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.e_lijekovi.data.Medication

@Composable
fun MedicationListScreen(
    medications: List<Medication>,
    onAdd: () -> Unit,
    onEdit: (Medication) -> Unit,
    onDelete: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(text = "Spisak lijekova")
            Button(onClick = onAdd) { Text("Dodaj") }
        }

        if (medications.isEmpty()) {
            Text(text = "Nema upisanih lijekova.")
        } else {
            LazyColumn {
                items(medications) { med ->
                    Card(modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)) {
                        Row(modifier = Modifier
                            .padding(12.dp)
                            .fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(text = med.name)
                                Text(text = "Doza: ${med.dosePerTake}, Ukupno: ${med.totalQuantity}")
                                if (med.schedule.isNotEmpty()) {
                                    Text(text = "Raspored: ${med.schedule.joinToString(", ")}")
                                }
                            }
                            Row {
                                IconButton(onClick = { onEdit(med) }) {
                                    Icon(Icons.Filled.Edit, contentDescription = "Uredi")
                                }
                                IconButton(onClick = { onDelete(med.id) }) {
                                    Icon(Icons.Filled.Delete, contentDescription = "Obriši")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
