package at.searles.storage

import android.content.Context
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.selection.SelectionTracker
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView

class StorageAdapter(private val context: Context, private val data: Data) : ListAdapter<String, StorageAdapter.EntryViewHolder>(DiffCallback) {

    private var selectionTracker: SelectionTracker<String>? = null
    var listener: ((View, Int) -> Unit)? = null

    override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): EntryViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.view_item, viewGroup, false)
        val viewHolder = EntryViewHolder(view)
        view.setOnClickListener(viewHolder)
        return viewHolder
    }

    override fun onBindViewHolder(entryViewHolder: EntryViewHolder, position: Int) {
        val key = getItem(position)

        var isSelected = false
        if (selectionTracker != null) {
            if (selectionTracker!!.isSelected(key)) {
                isSelected = true
            }
        }

        entryViewHolder.bind(isSelected, key)
    }

    fun setSelectionTracker(selectionTracker: SelectionTracker<String>) {
        this.selectionTracker = selectionTracker
    }

    inner class EntryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView), View.OnClickListener {

        private val titleTextView: TextView = itemView.findViewById(R.id.titleTextView)
        private val subtitleTextView: TextView = itemView.findViewById(R.id.subtitleTextView)
        private val iconImageView: ImageView = itemView.findViewById(R.id.iconImageView)

        private lateinit var key: String

        override fun onClick(view: View) {
            listener?.invoke(view, adapterPosition)
        }

        fun bind(isSelected: Boolean, key: String) {
            this.key = key

            // set ui
            titleTextView.text = data.getTitle(key)
            subtitleTextView.text = data.getSubtitle(key)
            data.getImageInView(key, iconImageView)

            itemView.isActivated = isSelected

            if (isSelected) {
                itemView.setBackgroundColor(Color.RED)
            } else {
                itemView.setBackgroundColor(Color.GREEN)
            }
        }
    }

    object DiffCallback: DiffUtil.ItemCallback<String>() {
        override fun areItemsTheSame(oldItem: String, newItem: String): Boolean {
            return oldItem === newItem
        }

        override fun areContentsTheSame(oldItem: String, newItem: String): Boolean {
            return oldItem == newItem
        }
    }

}