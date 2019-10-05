package at.searles.storage

import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.*
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.selection.*
import androidx.recyclerview.selection.SelectionPredicates.createSelectAnything
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.util.HashMap
import android.view.MenuInflater

class MainActivity : AppCompatActivity() {

    // TODO Data should be a fragment.
    private lateinit var data: Data

    private lateinit var selectionTracker: SelectionTracker<String>
    private lateinit var adapter: StorageAdapter

    // these next ones could be part of the key provider
    private lateinit var activeKeys: ArrayList<String>
    private lateinit var activeKeyPositions: MutableMap<String, Int>

    private var actionMode: ActionMode? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // set up data
        this.data = Data()
        activeKeyPositions = HashMap()
        activeKeys = ArrayList<String>(data.size()).also {
            data.keys().forEach { key -> it.add(key) }
        }

        adapter = StorageAdapter(this, data)

        afterActiveKeysUpdated()

        // set up view
        val filterEditText = findViewById<EditText>(R.id.filterEditText)
        filterEditText.addTextChangedListener(FilterWatcher())

        val recyclerView = findViewById<RecyclerView>(R.id.contentRecyclerView).apply {
            setHasFixedSize(true)
        }

        recyclerView.layoutManager = LinearLayoutManager(this)

        // adapter must be set before building selectionTracker
        recyclerView.adapter = adapter

        recyclerView.setBackgroundColor(Color.BLUE) // FIXME remove

        // recyclerView.addItemDecoration(new SpacesItemDecoration(this, R.dimen.item_spacing));

        selectionTracker = SelectionTracker.Builder(
            "entry-selection", //unique id
            recyclerView,
            EntryKeyProvider(),
            EntryDetailsLookup(recyclerView),
            StorageStrategy.createStringStorage()
        ).withSelectionPredicate(
            createSelectAnything())
        .build()

        adapter.setSelectionTracker(selectionTracker)

        //toolbarView.setNavigationOnClickListener { selectionTracker.clearSelection() }

        selectionTracker.addObserver(object : SelectionTracker.SelectionObserver<String>() {
            override fun onSelectionChanged() {
                selectionUpdated() // toggle action mode
                super.onSelectionChanged()
            }

            override fun onSelectionRestored() {
                selectionUpdated() // toggle action mode
                super.onSelectionRestored()
            }
        })

        // if there is a filter text still present, apply it.
        updatePattern(filterEditText.text.toString())
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
            selectionTracker.clearSelection() // FIXME does this call observer?
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
            actionMode = null
        }
    }

    private fun selectionUpdated() {
        if(selectionTracker.hasSelection()) {
            actionMode = startActionMode(actionModeCallback)
        } else {
            actionMode?.finish()
        }
    }

    private fun removeSelected() {
        selectionTracker.selection.forEach { data.remove(it) }
        selectionTracker.clearSelection()
    }

    /**
     * Selects all active (!) keys
     */
    private fun selectAll() {
        activeKeys.forEach { selectionTracker.select(it) }
    }

    private fun updatePattern(pattern: String) {
        activeKeys.clear()

        if(pattern.isEmpty()) {
            data.keys().forEach { activeKeys.add(it) }
        } else {
            // TODO Change pattern matcher.
            data.keys().filter { it.indexOf(pattern) != -1 }.forEach { activeKeys.add(it) }
        }

        afterActiveKeysUpdated()
    }

    private fun afterActiveKeysUpdated() {
        activeKeys.forEachIndexed { index, key -> activeKeyPositions[key] = index }
        adapter.setActiveKeys(activeKeys)
    }

    private inner class FilterWatcher : TextWatcher {
        override fun afterTextChanged(s: Editable) {
            updatePattern(s.toString())
        }

        override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {
            // ignore
        }

        override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
            // ignore
        }
    }


    private inner class EntryKeyProvider : ItemKeyProvider<String>(SCOPE_CACHED) {

        override fun getKey(i: Int): String? {
            return activeKeys[i]
        }

        override fun getPosition(s: String): Int {
            return activeKeyPositions[s]!!
        }
    }

    private class EntryItemDetails : ItemDetailsLookup.ItemDetails<String>() {
        var pos: Int = 0
        var key: String? = null

        override fun getPosition(): Int = pos

        override fun getSelectionKey(): String? = key

        override fun inSelectionHotspot(e: MotionEvent): Boolean {
            return true
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

            if (viewHolder is StorageAdapter.EntryViewHolder) {
                val position = viewHolder.adapterPosition
                entryDetails.pos = position
                entryDetails.key = activeKeys[position]

                return entryDetails
            }

            return null
        }
    }
}
