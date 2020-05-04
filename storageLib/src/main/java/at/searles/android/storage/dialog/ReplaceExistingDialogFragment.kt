package at.searles.android.storage.dialog

import android.app.Dialog
import androidx.fragment.app.DialogFragment
import androidx.appcompat.app.AlertDialog
import android.os.Bundle
import at.searles.android.storage.R
import at.searles.android.storage.StorageEditorCallback

/**
 * Parent activity must implement the can-save-
 */
class ReplaceExistingDialogFragment : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val newName = arguments?.getString(nameKey)!!

        return AlertDialog.Builder(activity!!)
                .setTitle(getString(R.string.entryExists, newName))
                .setMessage(R.string.replaceExistingEntryQuestion)
                .setNegativeButton(android.R.string.no) { _, _ -> }
                .setPositiveButton(android.R.string.yes) { _, _ ->
                    (activity as StorageEditorCallback<*>).storageEditor.forceSaveAs(newName)
                }
                .create()
    }

    companion object {
        const val nameKey = "name"

        fun newInstance(newName: String): ReplaceExistingDialogFragment {
            val args = Bundle().apply {
                putString(nameKey, newName)
            }

            return ReplaceExistingDialogFragment().apply {
                arguments = args
            }
        }
    }
}