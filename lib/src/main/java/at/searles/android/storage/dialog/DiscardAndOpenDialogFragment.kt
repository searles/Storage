package at.searles.android.storage.dialog

import android.app.Dialog
import androidx.fragment.app.DialogFragment
import androidx.appcompat.app.AlertDialog
import android.os.Bundle
import at.searles.android.storage.StorageDialogCallback

/**
 * Parent activity must implement the can-save-
 */
class DiscardAndOpenDialogFragment : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val name = arguments?.getString(nameKey)!!

        return AlertDialog.Builder(activity!!)
                .setTitle("Modifications will be lost")
                .setMessage("Do you want open \"%s\" and discard changes?")
                .setNegativeButton(android.R.string.no) { _, _ -> }
                .setPositiveButton(android.R.string.yes) { _, _ ->
                    (activity as StorageDialogCallback).discardAndOpen(name)
                }
                .create()
    }

    companion object {
        val nameKey = "name"

        fun create(name: String): DiscardAndOpenDialogFragment {
            return DiscardAndOpenDialogFragment().also {
                val args = Bundle()
                args.putString(nameKey, name)
                it.arguments = args
            }
        }
    }
}