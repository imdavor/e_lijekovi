package com.example.e_lijekovi.data

import java.util.UUID

data class Medication(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val dosePerTake: Int = 1,
    val totalQuantity: Int = 0,
    val schedule: Set<DobaDana> = emptySet(),
    val imageUri: String? = null,
    val enabled: Boolean = true
)
