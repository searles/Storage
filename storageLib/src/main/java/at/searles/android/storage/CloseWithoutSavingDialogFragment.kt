package at.searles.android.storage

import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import androidx.fragment.app.DialogFragment

/**
 * Parent activity must implement the can-save-
 */
class CloseWithoutSavingDialogFragment : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return AlertDialog.Builder(activity!!)
                .setTitle(R.string.unsavedChanges)
                .setMessage(context!!.resources.getString(R.string.cancelWithoutSavingQuestion))
                .setNegativeButton(android.R.string.cancel) { _, _ -> }
                .setPositiveButton(android.R.string.yes) { _, _ ->
                    (activity as OpenSaveActivity).closeWithoutSaving()
                }
                .create()
    }

    companion object {
        fun create(): CloseWithoutSavingDialogFragment {
            return CloseWithoutSavingDialogFragment().also {
                val args = Bundle()
                it.arguments = args
            }
        }
    }
}