package at.searles.android.storage.dialog

import android.app.Dialog
import androidx.fragment.app.DialogFragment
import androidx.appcompat.app.AlertDialog
import android.os.Bundle
import at.searles.android.storage.R

/**
 * Parent activity must implement the can-save-
 */
class ReplaceExistingDialogFragment : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val name = arguments?.getString(nameKey)!!

        return AlertDialog.Builder(activity!!)
                .setTitle(getString(R.string.entryExists, name))
                .setMessage(R.string.replaceExistingEntryQuestion)
                .setNegativeButton(android.R.string.no) { _, _ -> }
                .setPositiveButton(android.R.string.yes) { _, _ ->
                    (activity as Callback).replaceExistingAndSave(name)
                }
                .create()
    }

    companion object {
        const val nameKey = "name"

        fun create(key: String): ReplaceExistingDialogFragment {
            return ReplaceExistingDialogFragment().also {
                val args = Bundle()
                args.putString(nameKey, key)
                it.arguments = args
            }
        }
    }

    interface Callback {
        fun replaceExistingAndSave(name: String)
    }
}