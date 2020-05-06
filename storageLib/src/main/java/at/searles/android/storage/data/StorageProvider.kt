package at.searles.android.storage.data

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.core.content.FileProvider
import at.searles.commons.strings.NaturalComparator
import java.io.*
import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.text.SimpleDateFormat
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

class StorageProvider(private val pathName: String, private val context: Context) {

    val path: File = context.getDir(pathName, 0)
    val size: Int
        get() = path.listFiles()!!.size

    private fun encode(name: String): String {
        return URLEncoder.encode(name, StandardCharsets.UTF_8.toString())
    }

    private fun decode(filename: String): String {
        return URLDecoder.decode(filename, StandardCharsets.UTF_8.toString())
    }

    val names
        get() = path.listFiles()!!.map { decode(it.name) }.sortedWith(NaturalComparator)

    fun findPathEntry(name: String): File? {
        return path.listFiles()!!.find { decode(it.name) == name }
    }

    private fun getExistingFileForName(name: String): File {
        return File(path, encode(name)).let {
            if(it.exists()) {
                it
            } else {
                findPathEntry(name) ?: throw FileNotFoundException(it.path)
            }
        }
    }

    /**
     * Throws an exception if there is no entry with 'name'.
     */
    fun load(name: String): String {
        val inFile = getExistingFileForName(name)
        return load(FileInputStream(inFile))
    }

    /**
     * Throws an exception if 'name' is invalid. Existing entries will
     * be overwritten.
     */
    fun save(name: String, value: String) {
        if(name.isEmpty() || name == "." || name == "..") {
            throw InvalidNameException("Bad filename: '$name'", null)
        }

        val outFile = File(path, encode(name))
        save(FileOutputStream(outFile), value)
    }

    fun exists(name: String): Boolean {
        return findPathEntry(name) != null
    }

    fun deleteAll(names: List<String>): List<String> {
        val notDeleted = ArrayList<String>()

        names.forEach {
            if(!getExistingFileForName(it).delete()) {
                Log.i(javaClass.simpleName, "Could not delete $it")
                notDeleted.add(it)
            }
        }

        return notDeleted
    }

    fun rename(oldName: String, newName: String): Boolean {
        val oldFile = getExistingFileForName(oldName)
        val newFile = File(path, encode(newName))

        if(!oldFile.renameTo(newFile)) {
            Log.i(javaClass.simpleName, "Could not rename $oldName to $newName")
            return false
        }

        return true
    }

    fun load(inputStream: InputStream): String {
        inputStream.reader().use {
            return it.readText()
        }
    }

    fun save(outputStream: OutputStream, value: String) {
        outputStream.writer().use {
            it.write(value)
        }
    }

    fun import(uri: Uri): Map<String, String> {
        val inStream = context.contentResolver.openInputStream(uri)!!
        return readZipFromIs(inStream)
    }

    fun readZipFromIs(inputStream: InputStream): Map<String, String> {
        val importedEntries: HashMap<String, String> = HashMap()

        ZipInputStream(inputStream).use { zipIn ->
            while (true) {
                val entry: ZipEntry = zipIn.nextEntry ?: break

                try {
                    val value = load(object: InputStream() {
                        override fun read(): Int {
                            return zipIn.read()
                        }

                        override fun close() {
                            // do nothing.
                        }
                    })
                    val name = decode(entry.name)
                    importedEntries[name] = value
                } catch(e: Exception) {
                    Log.i(javaClass.simpleName, "Could not read ${entry.name}")
                    e.printStackTrace()
                }

                zipIn.closeEntry()
            }
        }

        return importedEntries
    }

    fun writeZipToOs(names: List<String>, outputStream: OutputStream) {
        ZipOutputStream(outputStream).use { zipOut ->
            for(name in names) {
                zipOut.putNextEntry(ZipEntry(encode(name)))

                save(object: OutputStream() {
                    override fun write(b: Int) {
                        zipOut.write(b)
                    }

                    override fun close() {
                        // do nothing.
                    }
                }, load(name))
                zipOut.closeEntry()
            }
        }
    }

    fun share(names: List<String>): Intent {

        val outFile = File.createTempFile(
            "${pathName}_${System.currentTimeMillis()}",
            ".zip",
            context.externalCacheDir
        )

        writeZipToOs(names, FileOutputStream(outFile))

        val contentUri = FileProvider.getUriForFile(context,
            FILE_PROVIDER, outFile)

        return Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_STREAM, contentUri)
            type =
                mimeType
        }
    }

    fun export(names: List<String>, uri: Uri) {
        val outputStream = context.contentResolver.openOutputStream(uri)!!
        writeZipToOs(names, outputStream)
    }

    companion object {
        const val FILE_PROVIDER = "at.searles.storage.fileprovider"
        const val mimeType = "application/zip"
    }
}