package at.searles.android.storage.demo

import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.ImageView
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
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

    override fun deleteAll(names: List<String>): Map<String, Boolean> {
        try {
            return names.map { it to File(directory, it).delete() }.toMap()
        } finally {
            updateLists()
        }
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

        try {
            ZipInputStream(inStream).use { zipIn ->
                while (true) {
                    val entry: ZipEntry = zipIn.nextEntry ?: break
                    if (!entry.isDirectory && !exists(entry.name)) {
                        Log.d("FilesProvider", "Reading ${entry.name} from zip")

                        FileOutputStream(File(directory, entry.name)).use { fileOut ->
                            zipIn.copyTo(fileOut)
                        }
                        statusMap[entry.name] = true
                    } else {
                        Log.d("FilesProvider", "Skipping ${entry.name}")
                        statusMap[entry.name] = false
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
                Log.d("FilesProvider", "Putting $name into zip")
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

        try {
            File(directory, name).writeText(value.invoke())
        } finally{
            updateLists()
        }

        return true
    }

    override fun load(name: String, contentHolder: (String) -> Unit) {
        contentHolder.invoke(File(directory, name).readText())
    }

    companion object {
        const val FILE_PROVIDER = "at.searles.storage.fileprovider"
        const val directoryName = "demo"
        const val mimeType = "application/zip"
    }

}