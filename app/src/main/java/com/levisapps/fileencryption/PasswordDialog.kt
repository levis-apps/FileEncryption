package com.levisapps.fileencryption

import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.View
import android.view.View.INVISIBLE
import androidx.appcompat.app.AppCompatDialogFragment
import kotlinx.android.synthetic.main.new_password_dialog.*

class PasswordDialog : AppCompatDialogFragment() {
    private var listener: PasswordDialogListener? = null

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val builder = AlertDialog.Builder(activity)
        val inflater = requireActivity().layoutInflater
        val view: View = inflater.inflate(R.layout.new_password_dialog, null)
        newPassword.hint = "Enter password"
        useBio.visibility = INVISIBLE

        builder.setView(view)
            //.setTitle("Login")
            .setNegativeButton(
                "Cancel"
            ) { _, _ -> }
            .setPositiveButton(
                "Save"
            ) { _, _ ->
                val password = newPassword.text.toString()
                listener!!.applyPassword(password)
            }

        return builder.create()
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        try {
            listener = context as PasswordDialogListener
        } catch (e: ClassCastException) {
            throw ClassCastException(context.toString() + "must implement NewPasswordDialogListener")
        }
    }

    interface PasswordDialogListener {
        fun applyPassword(password: String) :Boolean
    }
}