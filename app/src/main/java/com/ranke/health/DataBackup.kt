package com.ranke.health

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class DataBackup(private val context: Context, private val store: LocalStore) {
    suspend fun saveTo(uri: Uri): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            context.contentResolver.openOutputStream(uri, "wt")?.bufferedWriter()?.use {
                it.write(store.exportBackupJson())
            } ?: error("Unable to open document")
            true
        }.getOrDefault(false)
    }

    suspend fun restoreFrom(uri: Uri): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            val content = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                ?: error("Unable to read document")
            store.importBackupJson(content)
        }.getOrDefault(false)
    }
}
