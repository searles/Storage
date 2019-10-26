package at.searles.android.storage.data

import android.content.Context
import android.content.Intent
import android.widget.ImageView

interface InformationProvider {
    fun size(): Int
    fun getNames(): List<String>
    fun getDescription(name: String): String
    fun setImageInView(name: String, imageView: ImageView)

    fun exists(name: String): Boolean

    fun deleteAll(names: List<String>): Map<String, Boolean>

    /**
     * rename entry from oldName to newName.
     * @return false if newName exists or another non-critical error occurs (eg name is invalid)
     */
    fun rename(oldName: String, newName: String): Boolean

    /**
     * Import items. The intent holds information on which items.
     */
    fun import(context: Context, intent: Intent): Map<String, Boolean>

    /**
     * Export items. Internally, write a temporary file and return its Uri.
     */
    fun share(context: Context, names: Iterable<String>): Intent

    /**
     * Create intent for exporting/sharing
     */
    fun createImportIntent(context: Context): Intent

    /**
     * DEPRECATED! Find another way.
     */
    fun setContext(context: Context)
}