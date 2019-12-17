package at.searles.android.storage.dialog

import android.app.Dialog
import androidx.fragment.app.DialogFragment
import androidx.appcompat.app.AlertDialog
import android.os.Bundle
import at.searles.android.storage.R

/**
 * Parent activity must implement the can-save-
 */
class DiscardAndOpenDialogFragment : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val name = arguments?.getString(nameKey)!!

        return AlertDialog.Builder(activity!!)
                .setTitle(R.string.unsavedChanges)
                .setMessage(context!!.resources.getString(R.string.discardChangesQuestion, name))
                .setNegativeButton(android.R.string.no) { _, _ -> }
                .setPositiveButton(android.R.string.yes) { _, _ ->
                    (activity as Callback).discardAndOpen(name)
                }
                .create()
    }

    companion object {
        const val nameKey = "name"

        fun create(name: String): DiscardAndOpenDialogFragment {
            return DiscardAndOpenDialogFragment().also {
                val args = Bundle()
                args.putString(nameKey, name)
                it.arguments = args
            }
        }
    }

    interface Callback {
        fun discardAndOpen(name: String)
    }
}