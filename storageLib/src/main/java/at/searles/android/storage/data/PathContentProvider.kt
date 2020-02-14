package at.searles.android.storage.data

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import at.searles.commons.strings.NaturalComparator
import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException
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

    private fun toName(encodedFile: File): String {
        return URLDecoder.decode(encodedFile.name, StandardCharsets.UTF_8.toString())
    }

    override fun getNames(): List<String> {
        return path.listFiles()!!.map { toName(it) }.sortedWith(NaturalComparator)
    }

    override fun size(): Int = path.listFiles()!!.size

    override fun getDescription(name: String): String {
        return ""
    }

    override fun exists(name: String): Boolean {
        return getExistingFile(name) != null
    }

    /**
     * This method should only be used for new files. For existing ones, use the mapping.
     */
    private fun getNewFileForName(name: String): File {
        val encodedName = URLEncoder.encode(name, StandardCharsets.UTF_8.toString())
        return File(path, encodedName)
    }

    private fun getExistingFile(name: String): File? {
        return path.listFiles()!!.find { toName(it) == name }
    }
    
    override fun deleteAll(names: List<String>): Map<String, Boolean> {
        return names.map { it to (getExistingFile(it)?.delete() ?: false) }.toMap()
    }

    override fun rename(oldName: String, newName: String): Boolean {
        if(exists(newName)) {
            return false
        }

        return getExistingFile(oldName)?.renameTo(getNewFileForName(newName)) == true
    }

    override fun import(context: Context, intent: Intent): Map<String, Boolean> {
        val uri = intent.data?:return emptyMap()

        val statusMap = HashMap<String, Boolean>()

        val inStream = context.contentResolver.openInputStream(uri)!!

        ZipInputStream(inStream).use { zipIn ->
            while (true) {
                val entry: ZipEntry = zipIn.nextEntry ?: break

                val entryName = toName(File(path, entry.name))

                // normalize filenames.
                val targetFile = getNewFileForName(entryName)

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
                val encodedFile = getExistingFile(name)

                if(encodedFile == null) {
                    Log.e("PathContentProvider", "$encodedFile does not exist!")
                    continue
                }

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
                val encodedFile = getExistingFile(name)

                if(encodedFile == null) {
                    Log.e("PathContentProvider", "$encodedFile does not exist!")
                    continue
                }

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

        getNewFileForName(name).writeText(value.invoke())

        return true
    }

    override fun load(name: String, contentHolder: (String) -> Unit) {
        val file = getExistingFile(name) ?: throw FileNotFoundException("$name not found on file system")

        contentHolder.invoke(file.readText())
    }

    companion object {
        const val FILE_PROVIDER = "at.searles.storage.fileprovider"
        const val mimeType = "application/zip"
    }
}