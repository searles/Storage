package at.searles.storage

import android.os.Bundle
import androidx.fragment.app.DialogFragment
import android.app.Dialog
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import java.lang.IllegalArgumentException

class RenameDialogFragment: DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val builder = AlertDialog.Builder(activity!!)

        builder
            .setView(R.layout.rename_dialog)
            .setPositiveButton(android.R.string.ok) { _, _ -> run { rename(); dismiss() } }
            .setCancelable(true)

        val dialog = builder.show()

        val renameEditText = dialog.findViewById<EditText>(R.id.renameEditText)!!
        renameEditText.setText(arguments!!.getString(oldNameKey)!!)
        renameEditText.selectAll()

        // TODO validate input and check for errors

        return dialog
    }


    private fun rename() {
        val renameEditText = dialog!!.findViewById<EditText>(R.id.renameEditText)

        val oldName = arguments!!.getString(oldNameKey)?:throw IllegalArgumentException()
        val newName = renameEditText.text.toString()

        (activity as MainActivity).rename(oldName, newName)
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