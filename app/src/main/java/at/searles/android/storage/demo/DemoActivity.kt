package at.searles.android.storage.demo

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.Menu
import android.view.MenuItem
import android.widget.Button
import android.widget.EditText
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import at.searles.android.storage.OpenSaveActivity
import at.searles.android.storage.data.PathContentProvider
import at.searles.android.storage.dialog.DiscardAndOpenDialogFragment
import at.searles.android.storage.dialog.ReplaceExistingDialogFragment
import at.searles.storage.R

class DemoActivity : OpenSaveActivity(), ReplaceExistingDialogFragment.Callback, DiscardAndOpenDialogFragment.Callback {

    override val fileNameEditor: EditText by lazy {
        findViewById<EditText>(R.id.nameEditText)
    }

    override val saveButton: Button by lazy {
        findViewById<Button>(R.id.saveButton)
    }
    override val storageActivityTitle: String
        get() = "Open Something"

    private val contentEditText: EditText by lazy {
        findViewById<EditText>(R.id.contentEditText)
    }

    override lateinit var provider: PathContentProvider

    override var contentString: String
        get() = contentEditText.text.toString()
        set(value) {contentEditText.setText(value)}

    private val toolbar: Toolbar by lazy {
        findViewById<Toolbar>(R.id.toolbar)
    }

    override fun createReturnIntent(): Intent {
        return Intent()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.demo)

        setSupportActionBar(toolbar)

        toolbar.subtitle = "Subtitle"
        toolbar.setNavigationIcon(R.drawable.ic_edit_24dp)
    }

    override fun onResume() {
        super.onResume()

        provider = getDemoProvider()

        contentEditText.addTextChangedListener(object: TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                contentChanged()
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val inflater = menuInflater
        inflater.inflate(R.menu.demo_main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when(item.itemId) {
            R.id.open -> {
                startStorageActivity()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun getDemoProvider(): DemoPathContentProvider {
        return ViewModelProvider(this,
            object: ViewModelProvider.Factory {
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    @Suppress("UNCHECKED_CAST")
                    return DemoPathContentProvider(this@DemoActivity) as T
                }
            }
        )[DemoPathContentProvider::class.java]
    }
}
