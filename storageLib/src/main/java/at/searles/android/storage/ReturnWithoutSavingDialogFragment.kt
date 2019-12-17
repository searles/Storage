package at.searles.android.storage

import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import androidx.fragment.app.DialogFragment

/**
 * Parent activity must implement the can-save-
 */
class ReturnWithoutSavingDialogFragment : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return AlertDialog.Builder(activity!!)
                .setTitle(R.string.unsavedChanges)
                .setMessage(context!!.resources.getString(R.string.returnWithoutSavingQuestion))
                .setNegativeButton(android.R.string.no) { _, _ -> }
                .setPositiveButton(android.R.string.yes) { _, _ ->
                    (activity as OpenSaveActivity).returnWithoutSaving()
                }
                .create()
    }

    companion object {
        fun create(): ReturnWithoutSavingDialogFragment {
            return ReturnWithoutSavingDialogFragment().also {
                val args = Bundle()
                it.arguments = args
            }
        }
    }
}