package com.docvault.app.repository

import android.content.ContentResolver
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.provider.OpenableColumns
import android.webkit.MimeTypeMap
import com.docvault.app.data.AppDatabase
import com.docvault.app.data.ItemKind
import com.docvault.app.data.SavedItem
import com.docvault.app.security.FileHasher
import com.docvault.app.source.SourceDetector
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

class VaultRepository(private val context: Context) {

    companion object {
        const val DUPLICATE_FILE_MESSAGE = "DUPLICATE_VAULT_FILE"
    }

    private val dao = AppDatabase.get(context).savedItemDao()
    val items = dao.observeAll()

    private val vaultDir: File
        get() = File(context.filesDir, "vault").apply { mkdirs() }

    private val thumbDir: File
        get() = File(context.filesDir, "thumbnails").apply { mkdirs() }

    suspend fun importFromUri(uri: Uri): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val resolver = context.contentResolver
            val mime = resolver.getType(uri) ?: guessMimeFromUri(uri)
            val displayName = queryDisplayName(resolver, uri)
                ?: "file_${System.currentTimeMillis()}"
            val id = UUID.randomUUID().toString()
            val ext = extensionFor(mime, displayName)
            val dest = File(vaultDir, "$id.$ext")

            resolver.openInputStream(uri)?.use { input ->
                FileOutputStream(dest).use { output -> input.copyTo(output) }
            } ?: error("Could not read file")

            val contentHash = FileHasher.sha256Hex(dest)
            if (dao.countByContentHash(contentHash) > 0) {
                dest.delete()
                error(DUPLICATE_FILE_MESSAGE)
            }

            val kind = classify(mime, displayName)
            val thumbName = buildThumbnail(dest, kind, id)
            val source = SourceDetector.detect(uri)

            dao.insert(
                SavedItem(
                    id = id,
                    displayName = displayName,
                    mimeType = mime,
                    storedFileName = dest.name,
                    thumbFileName = thumbName,
                    kind = kind,
                    sourceLabel = source,
                    contentHash = contentHash
                )
            )
        }
    }

    suspend fun delete(item: SavedItem) = withContext(Dispatchers.IO) {
        File(vaultDir, item.storedFileName).delete()
        item.thumbFileName?.let { File(thumbDir, it).delete() }
        dao.delete(item)
    }

    fun fileFor(item: SavedItem): File = File(vaultDir, item.storedFileName)

    fun thumbFile(item: SavedItem): File? =
        item.thumbFileName?.let { File(thumbDir, it) }

    private fun classify(mime: String, name: String): ItemKind {
        val m = mime.lowercase()
        val n = name.lowercase()
        return when {
            m.startsWith("image/") -> ItemKind.IMAGE
            m == "application/pdf" || n.endsWith(".pdf") -> ItemKind.PDF
            m == "application/msword" ||
                m == "application/vnd.openxmlformats-officedocument.wordprocessingml.document" ||
                n.endsWith(".doc") || n.endsWith(".docx") -> ItemKind.WORD
            else -> ItemKind.OTHER
        }
    }

    private fun buildThumbnail(
        file: File,
        kind: ItemKind,
        id: String
    ): String? {
        val maxSide = 256
        return when (kind) {
            ItemKind.IMAGE -> {
                val bmp = decodeSampledBitmap(file, maxSide) ?: return null
                val out = File(thumbDir, "t_$id.jpg")
                FileOutputStream(out).use { fos ->
                    bmp.compress(Bitmap.CompressFormat.JPEG, 85, fos)
                }
                if (!bmp.isRecycled) bmp.recycle()
                out.name
            }
            ItemKind.PDF -> renderPdfThumb(file, id, maxSide)
            else -> null
        }
    }

    private fun renderPdfThumb(file: File, id: String, maxSide: Int): String? {
        return try {
            val pfd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
            PdfRenderer(pfd).use { renderer ->
                if (renderer.pageCount == 0) return null
                val page = renderer.openPage(0)
                try {
                    val ratio = maxSide.toFloat() / maxOf(page.width, page.height)
                    val w = (page.width * ratio).toInt().coerceAtLeast(1)
                    val h = (page.height * ratio).toInt().coerceAtLeast(1)
                    val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
                    page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                    val out = File(thumbDir, "t_$id.jpg")
                    FileOutputStream(out).use { fos ->
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 85, fos)
                    }
                    if (!bitmap.isRecycled) bitmap.recycle()
                    out.name
                } finally {
                    page.close()
                }
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun decodeSampledBitmap(file: File, maxSide: Int): Bitmap? {
        val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.absolutePath, opts)
        var sample = 1
        while (opts.outWidth / sample > maxSide || opts.outHeight / sample > maxSide) {
            sample *= 2
        }
        opts.inJustDecodeBounds = false
        opts.inSampleSize = sample
        return BitmapFactory.decodeFile(file.absolutePath, opts)
    }

    private fun extensionFor(mime: String, displayName: String): String {
        MimeTypeMap.getSingleton().getExtensionFromMimeType(mime)?.let { return it }
        val dot = displayName.lastIndexOf('.')
        if (dot >= 0 && dot < displayName.length - 1) {
            return displayName.substring(dot + 1).lowercase()
        }
        return when {
            mime.contains("pdf") -> "pdf"
            mime.contains("word") || mime.contains("msword") -> "docx"
            mime.startsWith("image/png") -> "png"
            mime.startsWith("image/jpeg") -> "jpg"
            else -> "bin"
        }
    }

    private fun queryDisplayName(resolver: ContentResolver, uri: Uri): String? {
        val cursor = resolver.query(uri, null, null, null, null) ?: return null
        cursor.use {
            if (!it.moveToFirst()) return null
            val idx = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            return if (idx >= 0) it.getString(idx) else null
        }
    }

    private fun guessMimeFromUri(uri: Uri): String {
        val path = uri.toString().lowercase()
        return when {
            path.endsWith(".pdf") -> "application/pdf"
            path.endsWith(".png") -> "image/png"
            path.endsWith(".jpg") || path.endsWith(".jpeg") -> "image/jpeg"
            path.endsWith(".doc") -> "application/msword"
            path.endsWith(".docx") ->
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
            else -> "application/octet-stream"
        }
    }
}
