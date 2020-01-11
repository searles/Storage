package at.searles.android.storage.data

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import at.searles.stringsort.NaturalComparator
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream


/**
 * Override this one with the correct directory in the constructor call.
 * Use context.getDir(directoryName, 0) to obtain the directory.
 * Make sure to avoid '/' in filenames since they allow changing the
 * directory.
 */
abstract class PathContentProvider(private val path: File) : ViewModel(), InformationProvider, DataProvider<String> {

    private lateinit var files: Map<String, File>
    private lateinit var names: List<String>

    init {
        updateLists()
    }

    private fun toName(encodedFile: File): String {
        return URLDecoder.decode(encodedFile.name, StandardCharsets.UTF_8.toString())
    }

    private fun updateLists() {
        files = path.listFiles()!!.map { toName(it) to it }.toMap()
        names = files.keys.toSortedSet(NaturalComparator).toList() // Natural order!
    }

    override fun getNames(): List<String> {
        return names
    }

    override fun size(): Int = files.size

    override fun getDescription(name: String): String {
        return files[name]?.canonicalPath ?: ""
    }

    override fun exists(name: String): Boolean {
        return toFile(name).exists()
    }

    private fun toFile(name: String): File {
        val encodedName = URLEncoder.encode(name, StandardCharsets.UTF_8.toString())
        return File(path, encodedName)
    }

    override fun deleteAll(names: List<String>): Map<String, Boolean> {
        try {
            return names.map { it to toFile(it).delete() }.toMap()
        } finally {
            updateLists()
        }
    }

    override fun rename(oldName: String, newName: String): Boolean {
        if(exists(newName)) {
            return false
        }

        if(!toFile(oldName).renameTo(toFile(newName))) {
            return false
        }

        updateLists()
        return true
    }

    override fun import(context: Context, intent: Intent): Map<String, Boolean> {
        val uri = intent.data?:return emptyMap()

        val statusMap = HashMap<String, Boolean>()

        val inStream = context.contentResolver.openInputStream(uri)!!

        try {
            ZipInputStream(inStream).use { zipIn ->
                while (true) {
                    val entry: ZipEntry = zipIn.nextEntry ?: break

                    val entryName = toName(File(path, entry.name))

                    // normalize filenames.
                    val targetFile = toFile(entryName)

                    if (!entry.isDirectory && !targetFile.exists()) {
                        Log.d("FilesProvider", "Reading $entryName to $targetFile from zip")

                        FileOutputStream(targetFile).use { fileOut ->
                            zipIn.copyTo(fileOut)
                        }
                        statusMap[entryName] = true
                    } else {
                        Log.d("FilesProvider", "Skipping $entryName")
                        statusMap[entryName] = false
                    }

                    zipIn.closeEntry()
                }
            }
        } finally {
            updateLists()
        }

        return statusMap
    }

    override fun share(context: Context, names: Iterable<String>): Intent {
        val outFile = File.createTempFile(
            "data_${System.currentTimeMillis()}",
            ".zip",
            context.externalCacheDir
        )

        ZipOutputStream(FileOutputStream(outFile)).use { zipOut ->
            for(name in names) {
                val encodedFile = toFile(name)
                Log.d("FilesProvider", "Putting $name as $encodedFile into zip")
                zipOut.putNextEntry(ZipEntry(encodedFile.name))
                FileInputStream(encodedFile).use {
                    it.copyTo(zipOut)
                }
                zipOut.closeEntry()
            }
        }

        val contentUri = FileProvider.getUriForFile(context, FILE_PROVIDER, outFile)

        return Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_STREAM, contentUri)
            type = mimeType
        }
    }

    override fun export(context: Context, intent: Intent, names: Iterable<String>) {
        ZipOutputStream(context.contentResolver.openOutputStream(intent.data!!)).use { zipOut ->
            for(name in names) {
                val encodedFile = toFile(name)
                Log.d("FilesProvider", "Putting $name as $encodedFile into zip")
                zipOut.putNextEntry(ZipEntry(encodedFile.name))
                FileInputStream(encodedFile).use {
                    it.copyTo(zipOut)
                }
                zipOut.closeEntry()
            }
        }
    }

    override fun save(name: String, value: () -> String, allowOverride: Boolean): Boolean {
        if(name.isEmpty()) {
            throw InvalidNameException("Name must not be empty", null)
        }

        if(name == "." || name == "..") {
            throw InvalidNameException("name must not be '.'. or '..'", null)
        }

        if(!allowOverride && exists(name)) {
            return false
        }

        try {
            toFile(name).writeText(value.invoke())
        } finally {
            updateLists()
        }

        return true
    }

    override fun load(name: String, contentHolder: (String) -> Unit) {
        contentHolder.invoke(toFile(name).readText())
    }

    companion object {
        const val FILE_PROVIDER = "at.searles.storage.fileprovider"
        const val mimeType = "application/zip"
    }
}