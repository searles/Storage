package at.searles.android.storage.demo

import android.content.Context
import android.content.Intent
import android.widget.ImageView
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import at.searles.android.storage.data.DataProvider
import at.searles.android.storage.data.InformationProvider
import at.searles.storage.R
import com.bumptech.glide.Glide
import java.io.*
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

class FilesProvider: ViewModel(), InformationProvider, DataProvider<String> {

    private lateinit var directory: File
    private lateinit var files: Map<String, File>
    private lateinit var names: List<String>

    override fun setContext(context: Context) {
        directory = context.getDir(directoryName, 0)
        updateLists()
    }

    private fun updateLists() {
        files = directory.listFiles()!!.map{it.name to it}.toMap()
        names = files.keys.toSortedSet().toList() // Natural order!
    }

    override fun getNames(): List<String> {
        return names
    }

    override fun size(): Int = files.size

    override fun getDescription(name: String): String {
        return files[name]?.canonicalPath ?: ""
    }

    override fun setImageInView(name: String, imageView: ImageView) {
        Glide
            .with(imageView.context)
            .load(R.drawable.ic_launcher_foreground)
            .centerCrop()
            // TODO .placeholder(R.drawable.loading_spinner)
            .into(imageView)
    }

    override fun exists(name: String): Boolean {
        return File(directory, name).exists()
    }

    override fun delete(name: String): Boolean {
        if(!File(directory, name).delete()) {
            return false
        }

        updateLists()
        return true
    }

    override fun rename(oldName: String, newName: String): Boolean {
        if(exists(newName)) {
            return false
        }

        if(!File(directory, oldName).renameTo(File(directory, newName))) {
            return false
        }

        updateLists()
        return true
    }

    override fun createImportIntent(context: Context): Intent {
        return Intent().apply {
            action = Intent.ACTION_OPEN_DOCUMENT
            addCategory(Intent.CATEGORY_OPENABLE)
            type = mimeType
        }
    }

    override fun import(context: Context, intent: Intent): Map<String, Boolean> {
        val uri = intent.data?:return emptyMap()

        val statusMap = HashMap<String, Boolean>()

        val inStream = context.contentResolver.openInputStream(uri)!!

        ZipInputStream(inStream).use { zipIn ->
            var entry: ZipEntry
            while(zipIn.nextEntry.also{ entry = it} != null) {
                if(!entry.isDirectory && !exists(entry.name)) {
                    FileOutputStream(File(directory, entry.name)).use { fileOut ->
                        zipIn.copyTo(fileOut)
                    }
                    statusMap[entry.name] = true
                } else {
                    statusMap[entry.name] = false
                }
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
                zipOut.putNextEntry(ZipEntry(name))
                FileInputStream(File(directory, name)).use {
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

    override fun save(name: String, value: () -> String, allowOverride: Boolean): Boolean {
        if(!allowOverride && exists(name)) {
            return false
        }

        File(directory, name).writeText(value.invoke())

        return true
    }

    override fun load(name: String, contentHolder: (String) -> Unit) {
        contentHolder.invoke(File(directory, name).readText())
    }

    class Factory: ViewModelProvider.Factory {
        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
            require(modelClass == FilesProvider::class.java) { "bad class" }

            @Suppress("UNCHECKED_CAST")
            return FilesProvider() as T
        }
    }

    companion object {
        const val FILE_PROVIDER = "at.searles.storage.fileprovider"
        const val directoryName = "demo"
        const val mimeType = "application/zip"
    }

}