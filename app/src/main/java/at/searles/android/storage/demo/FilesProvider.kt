package at.searles.android.storage.demo

import android.content.Context
import android.content.Intent
import android.widget.ImageView
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import at.searles.android.storage.data.DataProvider
import at.searles.android.storage.data.InformationProvider
import at.searles.storage.R
import com.bumptech.glide.Glide
import java.io.*
import java.lang.IllegalArgumentException

class FilesProvider: ViewModel(), InformationProvider, DataProvider<String> {

    private lateinit var directory: File
    private lateinit var files: Map<String, File>
    private lateinit var names: List<String>

    override fun setContext(context: Context) {
        directory = context.getDir(directoryName, 0)
        updateLists()
    }

    fun updateLists() {
        files = directory.listFiles()!!.map{it.name to it}.toMap()
        names = files.keys.toSortedSet().toList() // FIXME natural sort
    }

    override fun size(): Int = files.size

    override fun getNames(): List<String> {
        return names
    }

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
            type = "text/plain"
        }
    }

    override fun import(context: Context, intent: Intent): Map<String, Boolean> {
        try {
            val uri = intent.data!!

            // TODO zip
            val content = context.contentResolver.openInputStream(uri)!!.bufferedReader().readText()

            return content.split("\n").map {it to save( it, {"Hello"}, false) }.toMap()
        } catch (e: IOException) {
            throw IllegalArgumentException(e) // FIXME another exception that is caught by the caller would be better.
        }
    }

    override fun share(context: Context, names: Iterable<String>): Intent {
        // TODO
        val textFile = File.createTempFile(
            "data_${System.currentTimeMillis()}",
            ".txt",
            context.externalCacheDir
        )

        val content = names.joinToString("\n")

        textFile.writeText(content)

        // Share text file
        val contentUri = FileProvider.getUriForFile(context,
            FILE_PROVIDER, textFile)

        return Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_STREAM, contentUri)
            type = "text/plain"
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

    companion object {
        const val FILE_PROVIDER = "at.searles.storage.fileprovider"
        const val directoryName = "demo"
    }
}