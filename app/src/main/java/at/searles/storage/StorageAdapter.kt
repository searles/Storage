package at.searles.storage

import android.content.Context
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.selection.SelectionTracker
import androidx.recyclerview.widget.RecyclerView

class StorageAdapter(private val context: Context, private val data: Data) : RecyclerView.Adapter<StorageAdapter.EntryViewHolder>() {

    private var selectionTracker: SelectionTracker<String>? = null
    private lateinit var activeKeys: List<String>

    override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): EntryViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.view_item, viewGroup, false)
        return EntryViewHolder(view)
    }

    override fun onBindViewHolder(entryViewHolder: EntryViewHolder, position: Int) {
        val key = activeKeys[position]

        var isSelected = false
        if (selectionTracker != null) {
            if (selectionTracker!!.isSelected(key)) {
                isSelected = true
            }
        }

        entryViewHolder.bind(isSelected, key)
    }

    override fun getItemCount(): Int {
        return activeKeys.size
    }

    fun setActiveKeys(activeKeys: List<String>) {
        this.activeKeys = activeKeys
        this.notifyDataSetChanged()
    }

    fun setSelectionTracker(selectionTracker: SelectionTracker<String>) {
        this.selectionTracker = selectionTracker
    }

    inner class EntryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val titleTextView: TextView = itemView.findViewById(R.id.titleTextView)
        private val subtitleTextView: TextView = itemView.findViewById(R.id.subtitleTextView)

        private lateinit var key: String

        fun bind(isSelected: Boolean, key: String) {
            this.key = key

            // set ui
            titleTextView.text = data.getTitle(key)
            subtitleTextView.text = data.getSubtitle(key)

            itemView.isActivated = isSelected

            if (isSelected) {
                itemView.setBackgroundColor(Color.RED)
            } else {
                itemView.setBackgroundColor(Color.GREEN)
            }
        }
    }

}