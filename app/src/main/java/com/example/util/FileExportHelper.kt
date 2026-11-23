package com.example.util

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import java.io.File

object FileExportHelper {
    fun shareGeneratedFile(
        context: Context,
        fileName: String,
        mimeType: String,
        writer: (File) -> Unit
    ) {
        val dir = File(context.cacheDir, "exports").apply { mkdirs() }
        val target = File(dir, fileName)
        writer(target)
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            target
        )
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, fileName)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(
            Intent.createChooser(shareIntent, "Bagikan / Simpan $fileName")
        )
    }
}
