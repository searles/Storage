package at.searles.android.storage

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import at.searles.android.storage.StorageActivity
import at.searles.android.storage.data.FilesProvider
import at.searles.android.storage.dialog.DiscardAndOpenDialogFragment
import at.searles.android.storage.dialog.ReplaceExistingDialogFragment

abstract class OpenSaveActivity : AppCompatActivity(), ReplaceExistingDialogFragment.Callback, DiscardAndOpenDialogFragment.Callback {
    abstract val fileNameEditor: EditText

    abstract val saveButton: Button

    /**
     * This field contains the filename under which the item
     * has been saved the latest.
     */
    private var currentFileName: String? = null
        private set

    var isModified = false
        private set

    val mustAskForSaveOnFinish: Boolean
        get() = fileNameEditor.text.isNotEmpty() && isModified


    abstract val provider: FilesProvider

    abstract var contentString: String

    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)

        // init current key
        if(savedInstanceState != null) {
            currentFileName = savedInstanceState.getString(currentNameKey)
            isModified = savedInstanceState.getBoolean(isModifiedKey)
        }

        fileNameEditor.addTextChangedListener(object: TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                updateSaveButtonEnabled()
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        saveButton.setOnClickListener { onSaveButtonClicked() }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        outState.putString(currentNameKey, currentFileName)
        outState.putBoolean(isModifiedKey, isModified)
    }

    private fun onSaveButtonClicked() {
        val name = fileNameEditor.text.toString()
        val status: Boolean

        try {
            status = provider.save(name, {contentString}, name == currentFileName)
        } catch (th: Throwable) {
            Toast.makeText(this, resources.getString(at.searles.android.storage.R.string.error, th.localizedMessage), Toast.LENGTH_LONG).show()
            return
        }

        if(status) {
            notifyItemLoaded(name)
        } else {
            ReplaceExistingDialogFragment.create(name)
                    .show(supportFragmentManager, "dialog")
        }
    }

    override fun discardAndOpen(name: String) {
        // this is also called if nothing has to be discarded.
        try {
            provider.load(name) { contentString = it }
        } catch(th: Throwable) {
            Toast.makeText(this, resources.getString(at.searles.android.storage.R.string.error, th.localizedMessage), Toast.LENGTH_LONG).show()
            return
        }

        notifyItemLoaded(name)
    }

    override fun replaceExistingAndSave(name: String) {
        try {
            provider.save(name, { contentString }, true)
        } catch(th: Throwable) {
            Toast.makeText(this, resources.getString(at.searles.android.storage.R.string.error, th.localizedMessage), Toast.LENGTH_LONG).show()
            return
        }

        notifyItemLoaded(name)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if(requestCode == storageActivityRequestCode) {
            if (resultCode == Activity.RESULT_OK) {
                val name = data!!.getStringExtra(StorageActivity.nameKey)!!
                if (!isModified) {
                    discardAndOpen(name)
                } else {
                    DiscardAndOpenDialogFragment.create(name).also {
                        supportFragmentManager.beginTransaction().add(it, "dialog").commit()
                    }
                }
            }
        }
    }

    private fun notifyItemLoaded(name: String) {
        fileNameEditor.setText(name)
        isModified = false
        currentFileName = name
        updateSaveButtonEnabled()
    }

    fun startStorageActivity() {
        Intent(this, StorageActivity::class.java).also {
            // FIXME set title
            it.putExtra(StorageActivity.providerClassNameKey, provider.javaClass.canonicalName)
            startActivityForResult(it, storageActivityRequestCode)
        }
    }

    fun contentChanged() {
        isModified = true
        updateSaveButtonEnabled()
    }

    fun updateSaveButtonEnabled() {
        val isEnabled = isModified || currentFileName != fileNameEditor.text.toString()
        saveButton.isEnabled = isEnabled
    }

    companion object {
        const val sourceKey = "source"
        private const val currentNameKey = "currentName"
        private const val isModifiedKey = "isModified"
        const val storageActivityRequestCode = 101
    }
}