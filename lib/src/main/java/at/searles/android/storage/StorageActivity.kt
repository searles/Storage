package at.searles.android.storage

import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.text.Editable
import android.text.Spannable
import android.text.SpannableString
import android.text.TextWatcher
import android.text.style.StyleSpan
import android.view.*
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.selection.*
import androidx.recyclerview.selection.SelectionPredicates.createSelectAnything
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.view.MenuInflater
import android.widget.Toast
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.DividerItemDecoration
import at.searles.android.storage.data.InformationProvider
import at.searles.android.storage.dialog.RenameDialogFragment
import at.searles.stringsort.NaturalPatternMatcher
import java.util.*

open class StorageActivity : AppCompatActivity(), LifecycleOwner, RenameDialogFragment.Callback {

    private lateinit var informationProvider: InformationProvider

    private lateinit var active: List<String>
    private lateinit var activePositions: Map<String, Int>

    private lateinit var filterEditText: EditText

    private lateinit var selectionTracker: SelectionTracker<String>
    private lateinit var adapter: StorageAdapter

    private var selectionActionMode: ActionMode? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.storage_activity_main)

        // set up data
        this.informationProvider = getInformationProvider()

        // set up data structures for viewing items
        adapter = StorageAdapter(this, informationProvider)

        adapter.listener = { _, position -> confirm(active[position]) }

        // set up views
        filterEditText = findViewById(R.id.filterEditText)
        filterEditText.addTextChangedListener(FilterWatcher())

        // if there is a filter text still present, apply it.
        updateActiveKeys()

        // and now for the recycler view
        val recyclerView = findViewById<RecyclerView>(R.id.contentRecyclerView).apply {
            setHasFixedSize(true)
        }

        recyclerView.layoutManager = LinearLayoutManager(this)

        recyclerView.adapter = adapter

        recyclerView.addItemDecoration(DividerItemDecoration(this, LinearLayoutManager.VERTICAL))

        selectionTracker = SelectionTracker.Builder(
            "entry-selection",
            recyclerView,
            EntryKeyProvider(),
            EntryDetailsLookup(recyclerView),
            StorageStrategy.createStringStorage()
        ).withSelectionPredicate(
            createSelectAnything())
            .build()

        adapter.setSelectionTracker(selectionTracker)

        selectionTracker.addObserver(object : SelectionTracker.SelectionObserver<String>() {
            override fun onSelectionChanged() {
                selectionUpdated()
                super.onSelectionChanged()
            }

            override fun onSelectionRestored() {
                selectionUpdated()
                super.onSelectionRestored()
            }
        })
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val inflater = menuInflater
        inflater.inflate(R.menu.storage_main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when(item.itemId) {
            R.id.select_all -> {
                selectAll()
                true
            }
            R.id.import_items -> {
                importData()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        selectionTracker.onRestoreInstanceState(savedInstanceState)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        selectionTracker.onSaveInstanceState(outState)
        super.onSaveInstanceState(outState)
    }

    override fun onBackPressed() {
        if (selectionTracker.hasSelection()) {
            selectionTracker.clearSelection()
        } else {
            super.onBackPressed()
        }
    }

    /**
     * Override this method to change the default behavior
     */
    protected fun confirm(name: String) {
        Intent().also {
            it.putExtra(nameKey, name)
            setResult(openEntry, it)
        }

        finish()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, intent: Intent?) {
        super.onActivityResult(requestCode, resultCode, intent)

        if (intent != null && requestCode == importCode) {
            val importedBySuccess = informationProvider.import(this, intent)
            updateActiveKeys()
            importedBySuccess.forEach { (name, status) -> if(status) selectionTracker.select(name) }
            val failCount = importedBySuccess.count { (_, status) -> !status }
            Toast.makeText(this, resources.getString(R.string.importPartlyFailed, failCount, importedBySuccess.size), Toast.LENGTH_LONG).show()
        }
    }

    /**
     * Override this to modify the information provider
     */
    protected fun getInformationProvider(): InformationProvider {
        val clazzName = intent.getStringExtra(providerClassNameKey)!!

        @Suppress("UNCHECKED_CAST")
        val clazz = Class.forName(clazzName) as Class<ViewModel>

        return (ViewModelProvider(this,
            object: ViewModelProvider.Factory {
                override fun <T : ViewModel> create(modelClass: Class<T>): T = modelClass.newInstance()
            }
        )[clazz] as InformationProvider).also {
            it.setContext(this)
        }
    }

    private val actionModeCallback = object : ActionMode.Callback {
        // Called when the action mode is created; startActionMode() was called
        override fun onCreateActionMode(mode: ActionMode, menu: Menu): Boolean {
            // Inflate a menu resource providing context menu items
            val inflater: MenuInflater = mode.menuInflater
            inflater.inflate(R.menu.storage_context_menu, menu)
            return true
        }

        // Called each time the action mode is shown. Always called after onCreateActionMode, but
        // may be called multiple times if the mode is invalidated.
        override fun onPrepareActionMode(mode: ActionMode, menu: Menu): Boolean {
            return false // Return false if nothing is done
        }

        // Called when the user selects a contextual menu item
        override fun onActionItemClicked(mode: ActionMode, item: MenuItem): Boolean {
            return when (item.itemId) {
                R.id.select_all -> {
                    selectAll()
                    true
                }
                R.id.delete_items -> {
                    deleteSelected()
                    mode.finish()
                    true
                }
                R.id.rename_items -> {
                    RenameDialogFragment.create(selectionTracker.selection.first())
                        .show(supportFragmentManager, "dialog")
                    true
                }
                R.id.import_items -> {
                    importData()
                    true
                }
                R.id.share_items -> {
                    shareSelection()
                    true
                }
                else -> false
            }
        }

        // Called when the user exits the action mode
        override fun onDestroyActionMode(mode: ActionMode) {
            if(selectionActionMode == mode) {
                selectionTracker.clearSelection()
                selectionActionMode = null
            }
        }
    }

    private fun selectionUpdated() {
        if(selectionTracker.hasSelection()) {
            if(selectionActionMode == null) {
                selectionActionMode = startActionMode(actionModeCallback)
            }

            val selectedCount = selectionTracker.selection.size()
            val count = getInformationProvider().size()

            selectionActionMode!!.title = "$selectedCount ($count) selected"

            val renameMenu = selectionActionMode!!.menu.findItem(R.id.rename_items)
            renameMenu.isEnabled = selectedCount == 1
        } else {
            if(selectionActionMode != null) {
                selectionActionMode!!.finish()
                selectionActionMode = null
            }
        }
    }

    private fun deleteSelected() {
        val selection = ArrayList<String>(selectionTracker.selection.size()).apply {
            selectionTracker.selection.forEach { this.add(it) }
        }

        for(selected in selection) {
            selectionTracker.deselect(selected)

            try {
                if(!informationProvider.delete(selected)) {
                    notification(resources.getString(R.string.deleteFail, selected))
                    return
                }
            } catch(th: Throwable) {
                errorNotification(th)
                selectionTracker.select(selected)
                updateActiveKeys()
                return
            }
        }

        updateActiveKeys()
    }

    override fun rename(oldName: String, newName: String) {
        selectionTracker.deselect(oldName)

        val status: Boolean

        try {
            status = informationProvider.rename(oldName, newName)
        } catch(th: Throwable) {
            selectionTracker.select(oldName)
            errorNotification(th)
            return
        }

        if(status) {
            updateActiveKeys()
            selectionTracker.select(newName)
        } else {
            selectionTracker.select(oldName)
            Toast.makeText(this, resources.getString(R.string.renameFail, newName), Toast.LENGTH_LONG).show()
        }
    }

    private fun notification(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_LONG).show()
    }

    private fun errorNotification(th: Throwable) {
        Toast.makeText(this, resources.getString(R.string.error, th.localizedMessage), Toast.LENGTH_LONG).show()
    }

    /**
     * Selects all active (!) names
     */
    private fun selectAll() {
        selectionTracker.setItemsSelected(active, true)
    }

    private fun updateActiveKeys() {
        val pattern = filterEditText.text.toString()
        val names = informationProvider.getNames()

        this.active = if(pattern.isEmpty()) {
            ArrayList(names)
        } else {
            names.filter { NaturalPatternMatcher.match(it, pattern) }
        }

        this.activePositions = HashMap<String, Int>(active.size).apply {
            active.forEachIndexed { index, name -> this[name] = index }
        }

        adapter.submitList(active.map {
            SpannableString(it).apply {
                if(pattern.isNotEmpty())
                    NaturalPatternMatcher.match(this, pattern) {
                            start, end ->
                        setSpan(StyleSpan(Typeface.BOLD), start, end, Spannable.SPAN_INCLUSIVE_INCLUSIVE)
                    }
            }
        })
    }

    private fun shareSelection() {
        val intent = informationProvider.share(this, selectionTracker.selection)
        startActivity(Intent.createChooser(intent, getString(R.string.share)))
    }

    private fun importData() {
        startActivityForResult(
            informationProvider.createImportIntent(this),
            importCode
        )
    }



    private inner class FilterWatcher : TextWatcher {
        override fun afterTextChanged(s: Editable) {
            updateActiveKeys()
        }

        override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}

        override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}
    }

    private inner class EntryKeyProvider : ItemKeyProvider<String>(SCOPE_CACHED) {
        override fun getKey(position: Int): String {
            return active[position]
        }

        override fun getPosition(name: String): Int {
            return activePositions[name]?: active.size
        }
    }

    private class EntryItemDetails : ItemDetailsLookup.ItemDetails<String>() {
        var pos: Int = 0
        lateinit var name: String

        override fun getPosition(): Int = pos

        override fun getSelectionKey(): String = name

        override fun inSelectionHotspot(e: MotionEvent): Boolean {
            // clicks are not selections
            return false
        }

        override fun inDragRegion(e: MotionEvent): Boolean {
            return true
        }
    }

    private inner class EntryDetailsLookup(private val recyclerView: RecyclerView) : ItemDetailsLookup<String>() {
        private val entryDetails = EntryItemDetails()

        override fun getItemDetails(motionEvent: MotionEvent): ItemDetails<String>? {
            val view = recyclerView.findChildViewUnder(motionEvent.x, motionEvent.y) ?: return null

            val viewHolder = recyclerView.getChildViewHolder(view)

            entryDetails.pos = viewHolder.adapterPosition
            entryDetails.name = active[viewHolder.adapterPosition]

            return entryDetails
        }
    }

    companion object {
        const val importCode = 463
        const val nameKey = "name"
        const val providerClassNameKey = "providerClassName"
        const val openEntry = 252
    }
}
