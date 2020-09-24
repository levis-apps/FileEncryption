package com.levisapps.fileencryption

import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.View
import android.widget.CheckBox
import android.widget.EditText
import androidx.appcompat.app.AppCompatDialogFragment
import kotlinx.android.synthetic.main.new_password_dialog.*


class NewPasswordDialog : AppCompatDialogFragment() {
    private var listener: NewPasswordDialogListener? = null

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val builder = AlertDialog.Builder(activity)
        val inflater = requireActivity().layoutInflater
        val view: View = inflater.inflate(R.layout.new_password_dialog, null)

        val mNewPassword = view.findViewById<EditText>(R.id.newPassword)
        builder.setView(view)
            //.setTitle("Login")
            .setNegativeButton(
                "Cancel"
            ) { _, i -> }
            .setPositiveButton(
                "Save"
            ) { _, i ->
                val password = mNewPassword.text.toString()
                val useBiometrics = useBio.isChecked
                listener!!.applyNewPassword(password, useBiometrics)
            }

        return builder.create()
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        try {
            listener = context as NewPasswordDialogListener
        } catch (e: ClassCastException) {
            throw ClassCastException(context.toString() + "must implement NewPasswordDialogListener")
        }
    }

    interface NewPasswordDialogListener {
        fun applyNewPassword(password: String, useBiometrics: Boolean)
    }
}