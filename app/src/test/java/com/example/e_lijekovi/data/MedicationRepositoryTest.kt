package com.example.e_lijekovi.data

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class MedicationRepositoryTest {
    private lateinit var repo: MedicationRepository
    private val ctx: Context = ApplicationProvider.getApplicationContext()

    @Before
    fun setup() {
        // clear storage file before each test
        repo = MedicationRepository(ctx)
        val f = ctx.filesDir.resolve("medications.json")
        if (f.exists()) f.delete()
    }

    @Test
    fun add_and_load() {
        val med = Medication(name = "Aspirin", dosePerTake = 1, totalQuantity = 30)
        repo.add(med)
        val list = repo.loadAll()
        assertEquals(1, list.size)
        assertEquals("Aspirin", list[0].name)
    }

    @Test
    fun update_and_delete() {
        val med = Medication(name = "VitC", dosePerTake = 2, totalQuantity = 60)
        repo.add(med)
        val added = repo.loadAll().first()
        val updated = added.copy(totalQuantity = 58)
        repo.update(updated)
        val afterUpdate = repo.loadAll().first()
        assertEquals(58, afterUpdate.totalQuantity)
        repo.delete(afterUpdate.id)
        assertTrue(repo.loadAll().isEmpty())
    }
}

