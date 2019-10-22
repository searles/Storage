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

class DemoProvider: ViewModel(), InformationProvider, DataProvider<Any> {
    private val items = ArrayList<String>(100).also {
        (1..1000).forEach { i -> it.add("abc$i") }
    }

    override fun size(): Int = items.size

    override fun getNames(): List<String> {
        return items
    }

    override fun getDescription(name: String): String {
        return "subtitle $name"
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
        return items.contains(name)
    }

    override fun delete(name: String) {
        items.remove(name)
    }

    override fun rename(oldName: String, newName: String): Boolean {
        if(items.contains(newName)) {
            return false
        }

        items.remove(oldName)
        items.add(newName)
        return true
    }

    override fun createImportIntent(context: Context): Intent {
        return Intent().apply {
            action = Intent.ACTION_OPEN_DOCUMENT
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "text/plain"
        }
    }

    override fun import(context: Context, intent: Intent, allowOverride: Boolean): Iterable<String> {
        try {
            val uri = intent.data!!
            val content = context.contentResolver.openInputStream(uri)!!.bufferedReader().readText()

            return content.split("\n").filter { save(context, it, 1, allowOverride) }
        } catch (e: IOException) {
            throw IllegalArgumentException(e) // FIXME another exception that is caught by the caller would be better.
        }
    }

    override fun getNamesFromImportIntent(context: Context, intent: Intent): Iterable<String> {
        val uri = intent.data!!
        val content = context.contentResolver.openInputStream(uri)!!.bufferedReader().readText()

        return content.split("\n")
    }

    override fun share(context: Context, names: Iterable<String>): Intent {
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

    override fun exists(context: Context, name: String): Boolean {
        return items.contains(name)
    }

    override fun save(context: Context, name: String, value: Any, allowOverride: Boolean): Boolean {
        if(items.contains(name)) {
            return allowOverride
        }

        items.add(name)
        return true
    }

    override fun load(context: Context, name: String): Any {
        return Any()
    }

    companion object {
        const val FILE_PROVIDER = "at.searles.storage.fileprovider"
    }
}