package at.searles.storage.data

import android.content.Context
import android.content.Intent
import android.widget.ImageView

interface InformationProvider {
    fun size(): Int
    fun getNames(): List<String>
    fun getDescription(name: String): String
    fun setImageInView(name: String, imageView: ImageView)

    interface Mutable: InformationProvider {
        fun delete(name: String)
        fun rename(oldName: String, newName: String)

        /**
         * Import items. The intent holds information on which items.
         */
        fun import(context: Context, intent: Intent): Iterable<String>

        /**
         * Export items. Internally, write a temporary file and return its Uri.
         */
        fun share(context: Context, names: Iterable<String>): Intent

        fun createImportIntent(context: Context): Intent
    }
}