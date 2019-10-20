package at.searles.android.storage

import android.os.Bundle
import androidx.fragment.app.DialogFragment
import android.app.Dialog
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import java.lang.IllegalArgumentException

class RenameDialogFragment: DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val oldNameKey = arguments!!.getString(oldNameKey)!!

        val builder = AlertDialog.Builder(activity!!)

        builder
            .setView(R.layout.rename_dialog)
            .setTitle(R.string.rename)
            .setPositiveButton(android.R.string.ok) { _, _ -> run { rename(); dismiss() } }
            .setNegativeButton(android.R.string.cancel) { _, _ -> dismiss() }
            .setCancelable(true)

        val dialog = builder.show()

        val renameEditText = dialog.findViewById<EditText>(R.id.renameEditText)!!
        renameEditText.setText(oldNameKey)
        renameEditText.selectAll()

        renameEditText.requestFocus()

        return dialog
    }


    private fun rename() {
        val renameEditText = dialog!!.findViewById<EditText>(R.id.renameEditText)

        val oldName = arguments!!.getString(oldNameKey)?:throw IllegalArgumentException()
        val newName = renameEditText.text.toString()

        (activity as StorageActivity).rename(oldName, newName)
    }

    companion object {
        const val oldNameKey = "oldNameKey"

        fun create(oldName: String): RenameDialogFragment {
            val dialogFragment = RenameDialogFragment()
            dialogFragment.arguments = Bundle().also { it.putString(oldNameKey, oldName) }
            return dialogFragment
        }
    }
}