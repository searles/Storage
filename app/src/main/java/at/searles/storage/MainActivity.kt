package at.searles.storage

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
import java.util.HashMap
import android.view.MenuInflater
import android.widget.Toast
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.DividerItemDecoration
import at.searles.stringsort.NaturalPatternMatcher

class MainActivity : AppCompatActivity() {

    // TODO Data should be a viewmodel with a livedata-key-list
    private lateinit var data: Data
    private lateinit var active: List<String>
    private lateinit var activePositions: Map<String, Int>

    private lateinit var filterEditText: EditText

    private lateinit var selectionTracker: SelectionTracker<String>
    private lateinit var adapter: StorageAdapter

    private var selectionActionMode: ActionMode? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // set up data
        this.data = ViewModelProvider(this,
            object: ViewModelProvider.Factory {
                override fun <T : ViewModel> create(modelClass: Class<T>): T = modelClass.newInstance()
            }
        )[Data::class.java]

        // set up data structures for viewing items
        adapter = StorageAdapter(this, data)

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

        adapter.listener = { // FIXME remove
                _, position -> Toast.makeText(this, "Hello ${active[position]}", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val inflater = menuInflater
        inflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when(item.itemId) {
            R.id.select_all -> {
                selectAll()
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

    private val actionModeCallback = object : ActionMode.Callback {
        // Called when the action mode is created; startActionMode() was called
        override fun onCreateActionMode(mode: ActionMode, menu: Menu): Boolean {
            // Inflate a menu resource providing context menu items
            val inflater: MenuInflater = mode.menuInflater
            inflater.inflate(R.menu.context_menu, menu)
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
                R.id.remove_items -> {
                    removeSelected()
                    mode.finish()
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
            val count = data.size()

            selectionActionMode!!.title = "$selectedCount ($count) selected"
        } else {
            if(selectionActionMode != null) {
                selectionActionMode!!.finish()
                selectionActionMode = null
            }
        }
    }

    private fun removeSelected() {
        selectionTracker.selection.forEach { data.remove(it) }
        selectionTracker.clearSelection()
        updateActiveKeys()
    }

    /**
     * Selects all active (!) names
     */
    private fun selectAll() {
        selectionTracker.setItemsSelected(active, true)
    }

    private fun updateActiveKeys() {
        val pattern = filterEditText.text.toString()

        this.active = if(pattern.isEmpty()) {
            ArrayList(data.names())
        } else {
            data.names().filter { NaturalPatternMatcher.match(it, pattern) }
        }

        this.activePositions = HashMap<String, Int>(active.size).apply {
            active.forEachIndexed { index, name -> this[name] = index }
        }

        adapter.submitList(active.map {
            SpannableString(it).apply {
                NaturalPatternMatcher.match(this, pattern) {
                        start, end ->
                            setSpan(StyleSpan(Typeface.BOLD), start, end, Spannable.SPAN_INCLUSIVE_INCLUSIVE)
                }
            }
        })
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
}
