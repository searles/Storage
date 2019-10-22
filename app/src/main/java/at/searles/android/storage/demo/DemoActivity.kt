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
import at.searles.android.storage.StorageDialogCallback
import at.searles.android.storage.data.InformationProvider
import at.searles.android.storage.dialog.DiscardAndOpenDialogFragment
import at.searles.android.storage.dialog.RenameDialogFragment
import at.searles.android.storage.dialog.ReplaceExistingDialogFragment
import at.searles.storage.R

class DemoActivity : AppCompatActivity(), StorageDialogCallback {

    private var currentName: String? = null // if null, there is no current key
    private var isModified = false

    private lateinit var provider: DemoProvider

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.demo)

        // init current key
        if(savedInstanceState != null) {
            currentName = savedInstanceState.getString(currentNameKey)
            isModified = savedInstanceState.getBoolean(isModifiedKey)
        }

        provider = getDemoProvider()

        findViewById<EditText>(R.id.nameEditText).addTextChangedListener(object: TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                setSaveEnabled(currentName != s.toString())
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        setSaveEnabled(isModified)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        outState.putString(currentNameKey, currentName)
        outState.putBoolean(isModifiedKey, isModified)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if(resultCode == StorageActivity.openEntry) {
            val newName = data!!.getStringExtra(StorageActivity.nameKey)!!

            if(isModified) {
                // ask whether to discard changes.
                DiscardAndOpenDialogFragment.create(newName)
                    .show(supportFragmentManager, "dialog")
            } else {
                currentName = newName
                findViewById<EditText>(R.id.nameEditText).setText(newName)
                isModified = false
                setSaveEnabled(false)
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data)
        }
    }

    fun onOpenClick(view: View) {
        startStorageActivity()
    }

    fun onSaveClick(view: View) {
        // todo currentName
        val name = findViewById<EditText>(R.id.nameEditText).text.toString()

        if(provider.exists(name)) {
            if(name != currentName) {
                ReplaceExistingDialogFragment.create(name)
                    .show(supportFragmentManager, "dialog")
            } else {
                if(!provider.save(this, name, Any(), true)) {
                    Toast.makeText(this, "Could not save \"%s\"", Toast.LENGTH_LONG).show()
                } else {
                    currentName = name
                    setSaveEnabled(false)
                }
            }
        } else {
            if(!provider.save(this, name, Any(), false)) {
                Toast.makeText(this, "Could not save \"%s\"", Toast.LENGTH_LONG).show()
            } else {
                currentName = name
                isModified = false
                setSaveEnabled(false)
            }
        }
    }

    override fun overrideAndSaveAs(name: String) {
        if(!provider.save(this, name, Any(), true)) {
            Toast.makeText(this, "Could not save \"%s\"", Toast.LENGTH_LONG).show()
            return
        }

        currentName = name
        findViewById<EditText>(R.id.nameEditText).setText(name)
        isModified = false
        setSaveEnabled(false)
    }

    override fun discardAndOpen(name: String) {
        provider.load(this, name)

        currentName = name
        findViewById<EditText>(R.id.nameEditText).setText(name)
        isModified = false
        setSaveEnabled(false)
    }

    fun getDemoProvider(): DemoProvider {
        return ViewModelProvider(this,
            object: ViewModelProvider.Factory {
                override fun <T : ViewModel> create(modelClass: Class<T>): T = modelClass.newInstance()
            }
        )[DemoProvider::class.java]
    }

    fun toggleModification(view: View) {
        if(!isModified) {
            isModified = true
            setSaveEnabled(true)
        }
    }

    private fun setSaveEnabled(state: Boolean) {
        findViewById<Button>(R.id.saveButton)!!.isEnabled = state
    }

    private fun startStorageActivity() {
        Intent(this, StorageActivity::class.java).also {
            it.putExtra(StorageActivity.providerClassNameKey, DemoProvider::class.java.canonicalName)
            startActivityForResult(it, storageActivityRequestCode)
        }
    }

    companion object {
        const val storageActivityRequestCode = 101
        private const val currentNameKey = "currentName"
        private const val isModifiedKey = "isModified"
    }
}
