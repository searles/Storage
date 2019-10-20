package at.searles.android.storage

import android.content.Context
import android.text.SpannableString
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.selection.SelectionTracker
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import at.searles.android.storage.data.InformationProvider

class StorageAdapter(private val context: Context, private val informationProvider: InformationProvider) : ListAdapter<SpannableString, StorageAdapter.EntryViewHolder>(
    DiffCallback
) {

    private lateinit var selectionTracker: SelectionTracker<String>
    var listener: ((View, Int) -> Unit)? = null

    override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): EntryViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.view_item, viewGroup, false)
        val viewHolder = EntryViewHolder(view)
        view.setOnClickListener(viewHolder)
        return viewHolder
    }

    override fun onBindViewHolder(entryViewHolder: EntryViewHolder, position: Int) {
        val name = getItem(position)
        val isSelected = selectionTracker.isSelected(name.toString())
        entryViewHolder.bind(isSelected, name)
    }

    fun setSelectionTracker(selectionTracker: SelectionTracker<String>) {
        this.selectionTracker = selectionTracker
    }

    inner class EntryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView), View.OnClickListener {
        private val nameTextView: TextView = itemView.findViewById(R.id.nameTextView)
        private val descriptionTextView: TextView = itemView.findViewById(R.id.descriptionTextView)
        private val iconImageView: ImageView = itemView.findViewById(R.id.iconImageView)

        override fun onClick(view: View) {
            if(!selectionTracker.hasSelection()) {
                listener?.invoke(view, adapterPosition)
            }

            // otherwise  it is a long click in a selection.
        }

        fun bind(isSelected: Boolean, name: SpannableString) {
            // set ui
            nameTextView.text = name

            descriptionTextView.text = informationProvider.getDescription(name.toString())
            informationProvider.setImageInView(name.toString(), iconImageView)

            itemView.isActivated = isSelected
        }
    }

    object DiffCallback: DiffUtil.ItemCallback<SpannableString>() {
        override fun areItemsTheSame(oldItem: SpannableString, newItem: SpannableString): Boolean {
            return oldItem.toString() == newItem.toString()
        }

        override fun areContentsTheSame(oldItem: SpannableString, newItem: SpannableString): Boolean {
            return oldItem == newItem
        }

        override fun getChangePayload(oldItem: SpannableString, newItem: SpannableString): Any? {
            return dummyChangePayload
        }
    }

    companion object {
        val dummyChangePayload = Object()
    }
}