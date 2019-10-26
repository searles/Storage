package at.searles.android.storage.demo

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import at.searles.android.storage.StorageActivity
import at.searles.android.storage.dialog.DiscardAndOpenDialogFragment
import at.searles.android.storage.dialog.ReplaceExistingDialogFragment
import at.searles.storage.R

class DemoActivity : AppCompatActivity(), ReplaceExistingDialogFragment.Callback, DiscardAndOpenDialogFragment.Callback {
    private lateinit var provider: FilesProvider
    private var currentName: String? = null
    private var isModified = false

    private lateinit var nameEditText: EditText
    private lateinit var contentEditText: EditText
    private lateinit var saveButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.demo)

        provider = getDemoProvider()

        // init current key
        if(savedInstanceState != null) {
            currentName = savedInstanceState.getString(currentNameKey)
            isModified = savedInstanceState.getBoolean(isModifiedKey)
        }

        nameEditText = findViewById(R.id.nameEditText)
        contentEditText = findViewById(R.id.contentEditText)
        saveButton = findViewById(R.id.saveButton)

        nameEditText.addTextChangedListener(object: TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                updateSaveButtonEnabled()
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        contentEditText.addTextChangedListener(object: TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                isModified = true
                updateSaveButtonEnabled()
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        updateSaveButtonEnabled()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(currentNameKey, currentName)
        outState.putBoolean(isModifiedKey, isModified)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if(resultCode == StorageActivity.openEntry) {
            val name = data!!.getStringExtra(StorageActivity.nameKey)!!
            if(!isModified) {
                discardAndOpen(name)
            } else {
                DiscardAndOpenDialogFragment.create(name).also {
                    supportFragmentManager.beginTransaction().add(it, "dialog").commit()
                }
            }
        }
    }

    override fun discardAndOpen(name: String) {
        // this is also called if nothing has to be discarded.
        try {
            provider.load(name) { content = it }
        } catch(th: Throwable) {
            Toast.makeText(this, resources.getString(at.searles.android.storage.R.string.error, th.localizedMessage), Toast.LENGTH_LONG).show()
            return
        }

        nameEditText.setText(name)
        isModified = false
        currentName = name
        updateSaveButtonEnabled()
    }

    fun onOpenClick(view: View) {
        startStorageActivity()
    }

    override fun replaceExistingAndSave(name: String) {
        try {
            provider.save(name, { content }, true)
        } catch(th: Throwable) {
            Toast.makeText(this, resources.getString(at.searles.android.storage.R.string.error, th.localizedMessage), Toast.LENGTH_LONG).show()
            return
        }

        isModified = false
        this.currentName = name
        updateSaveButtonEnabled()
    }

    fun onSaveClick(view: View) {
        val name = nameEditText.text.toString()
        val status: Boolean

        try {
            status = provider.save(name, {content}, name == currentName)
        } catch (th: Throwable) {
            Toast.makeText(this, resources.getString(at.searles.android.storage.R.string.error, th.localizedMessage), Toast.LENGTH_LONG).show()
            return
        }

        if(status) {
            isModified = false
            currentName = name
            updateSaveButtonEnabled()
        } else {
            ReplaceExistingDialogFragment.create(name)
                .show(supportFragmentManager, "dialog")
        }
    }

    var content: String
        get() = contentEditText.text.toString()
        set(value) { contentEditText.setText(value)}

    private fun getDemoProvider(): FilesProvider {
        return ViewModelProvider(this,
            object: ViewModelProvider.Factory {
                override fun <T : ViewModel> create(modelClass: Class<T>): T = modelClass.newInstance()
            }
        )[FilesProvider::class.java].also { it.setContext(this) }
    }

    private fun updateSaveButtonEnabled() {
        val isEnabled = isModified || currentName != nameEditText.text.toString()
        saveButton.isEnabled = isEnabled
    }

    private fun startStorageActivity() {
        Intent(this, StorageActivity::class.java).also {
            it.putExtra(StorageActivity.providerClassNameKey, provider.javaClass.canonicalName)
            startActivityForResult(it, storageActivityRequestCode)
        }
    }

    companion object {
        const val storageActivityRequestCode = 101
        private const val currentNameKey = "currentName"
        private const val isModifiedKey = "isModified"
    }
}
