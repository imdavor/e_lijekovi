package com.example.e_lijekovi.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/**
 * Simple file-based repository for Medication list stored as JSON in app files directory.
 * This is synchronous and intended for small datasets and demo/testing.
 */
class MedicationRepository(private val context: Context) {
    private val fileName = "medications.json"

    private fun storageFile(): File = File(context.filesDir, fileName)

    fun loadAll(): List<Medication> {
        val f = storageFile()
        if (!f.exists()) return emptyList()
        val text = f.readText()
        return try {
            val arr = JSONArray(text)
            val result = mutableListOf<Medication>()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val id = obj.optString("id")
                val name = obj.optString("name")
                val dosePerTake = obj.optInt("dosePerTake", 1)
                val totalQuantity = obj.optInt("totalQuantity", 0)
                val imageUri = if (obj.has("imageUri") && !obj.isNull("imageUri")) obj.optString("imageUri") else null
                val enabled = obj.optBoolean("enabled", true)
                val scheduleArr = obj.optJSONArray("schedule")
                val schedule = mutableSetOf<DobaDana>()
                if (scheduleArr != null) {
                    for (j in 0 until scheduleArr.length()) {
                        val s = scheduleArr.optString(j)
                        try {
                            schedule.add(DobaDana.valueOf(s))
                        } catch (_: Exception) {
                        }
                    }
                }
                result.add(
                    Medication(
                        id = id,
                        name = name,
                        dosePerTake = dosePerTake,
                        totalQuantity = totalQuantity,
                        schedule = schedule,
                        imageUri = imageUri,
                        enabled = enabled
                    )
                )
            }
            result
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    private fun toJson(list: List<Medication>): JSONArray {
        val arr = JSONArray()
        for (m in list) {
            val obj = JSONObject()
            obj.put("id", m.id)
            obj.put("name", m.name)
            obj.put("dosePerTake", m.dosePerTake)
            obj.put("totalQuantity", m.totalQuantity)
            obj.put("imageUri", m.imageUri)
            obj.put("enabled", m.enabled)
            val scheduleArr = JSONArray()
            for (s in m.schedule) scheduleArr.put(s.name)
            obj.put("schedule", scheduleArr)
            arr.put(obj)
        }
        return arr
    }

    fun saveAll(list: List<Medication>) {
        val f = storageFile()
        f.parentFile?.mkdirs()
        f.writeText(toJson(list).toString())
        // Try to notify scheduler about changes. If class missing, this will be a no-op when resolved later.
        try {
            com.example.e_lijekovi.notifications.NotificationScheduler.updateAll(context)
        } catch (t: Throwable) {
            // ignore - scheduler may not be implemented yet
        }
    }

    fun add(med: Medication) {
        val current = loadAll().toMutableList()
        current.add(med)
        saveAll(current)
    }

    fun update(updated: Medication) {
        val current = loadAll().toMutableList()
        val idx = current.indexOfFirst { it.id == updated.id }
        if (idx >= 0) current[idx] = updated
        else current.add(updated)
        saveAll(current)
    }

    fun delete(id: String) {
        val current = loadAll().filterNot { it.id == id }
        saveAll(current)
    }
}
