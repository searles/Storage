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
    // TODO as collapsable action view this would be great...
    abstract val fileNameEditor: EditText
    abstract val saveButton: Button

    abstract val storageActivityTitle: String

    /**
     * This field contains the filename under which the item
     * has been saved the latest.
     */
    private var currentFileName: String? = null

    var isModified = false
        private set

    private val mustAskForSaveOnFinish: Boolean
        get() = fileNameEditor.text.isNotEmpty() && (isModified || currentFileName != fileNameEditor.text.toString())

    abstract val provider: FilesProvider

    abstract var contentString: String

    abstract fun createReturnIntent(): Intent

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

    override fun onPostResume() {
        super.onPostResume()
        updateSaveButtonEnabled()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        outState.putString(currentNameKey, currentFileName)
        outState.putBoolean(isModifiedKey, isModified)
    }

    override fun onBackPressed() {
        if(mustAskForSaveOnFinish) {
            CloseWithoutSavingDialogFragment.create().show(supportFragmentManager, "dialog")
            return
        }

        closeWithoutSaving()
    }

    @Suppress("unused")
    fun finishAndReturnContent() {
        if(mustAskForSaveOnFinish) {
            ReturnWithoutSavingDialogFragment.create().show(supportFragmentManager, "dialog")
            return
        }

        returnWithoutSaving()
    }

    internal fun returnWithoutSaving() {
        setResult(Activity.RESULT_OK, createReturnIntent())
        finish()
    }

    internal fun closeWithoutSaving() {
        Intent().also {
            setResult(Activity.RESULT_CANCELED, it)
        }

        finish()
    }

    private fun onSaveButtonClicked() {
        val name = fileNameEditor.text.toString()
        val status: Boolean

        try {
            status = provider.save(name, {contentString}, name == currentFileName)
        } catch (th: Throwable) {
            Toast.makeText(this, resources.getString(R.string.error, th.localizedMessage), Toast.LENGTH_LONG).show()
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
            Toast.makeText(this, resources.getString(R.string.error, th.localizedMessage), Toast.LENGTH_LONG).show()
            return
        }

        notifyItemLoaded(name)
    }

    override fun replaceExistingAndSave(name: String) {
        try {
            provider.save(name, { contentString }, true)
        } catch(th: Throwable) {
            Toast.makeText(this, resources.getString(R.string.error, th.localizedMessage), Toast.LENGTH_LONG).show()
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
            it.putExtra(StorageActivity.titleKey, storageActivityTitle)
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
        private const val currentNameKey = "currentName"
        private const val isModifiedKey = "isModified"
        const val storageActivityRequestCode = 101
    }
}