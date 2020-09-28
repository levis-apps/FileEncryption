package com.levisapps.fileencryption

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.text.method.LinkMovementMethod
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricPrompt
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.security.crypto.EncryptedFile
import androidx.security.crypto.MasterKeys
import com.nbsp.materialfilepicker.MaterialFilePicker
import com.nbsp.materialfilepicker.ui.FilePickerActivity
import kotlinx.android.synthetic.main.activity_main.*
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.charset.StandardCharsets
import java.util.regex.Pattern

class MainActivity : AppCompatActivity() {

    private var filepath = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(findViewById(R.id.toolbar))
        toolbar.showOverflowMenu()

        donate.movementMethod = LinkMovementMethod.getInstance()
        googlePlay.movementMethod = LinkMovementMethod.getInstance()
        github.movementMethod = LinkMovementMethod.getInstance()

        checkPermissions()

        save.setOnClickListener {
            checkPermissions()
            createEncryptedFile()
            closeNewFileUI()
        }

        cancel.setOnClickListener { closeNewFileUI() }
    }

    //Checking for permissions and prompting in case of a denied permission
    private fun checkPermissions() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            if (ContextCompat.checkSelfPermission(
                    applicationContext,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                ) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(
                    applicationContext,
                    Manifest.permission.READ_EXTERNAL_STORAGE
                ) != PackageManager.PERMISSION_GRANTED
            ) {

                ActivityCompat.requestPermissions(
                    this@MainActivity, arrayOf(
                        Manifest.permission.WRITE_EXTERNAL_STORAGE,
                        Manifest.permission.READ_EXTERNAL_STORAGE
                    ), 101
                )
            }
        } else { //MANAGE_EXTERNAL_STORAGE permission required for Android 11
            if (ContextCompat.checkSelfPermission(
                    applicationContext,
                    Manifest.permission.MANAGE_EXTERNAL_STORAGE
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this@MainActivity, arrayOf(
                        Manifest.permission.MANAGE_EXTERNAL_STORAGE
                    ), 101
                )
            }
        }
    }

    //Creates a text file which will later be used for writing encrypted text to it
    private fun createEncryptedFile() {
        makeDir()
        var counter = 1
        val path = Environment.getExternalStorageDirectory()
            .toString() + "/File Encryption" //Default directory
        var name = "Encrypted_File$counter" //Default name
        val content = fcontent.text.toString()
        var file = File(path, name)

        val nameInp = fname.text.toString()
        if (nameInp != "") {
            name = "$nameInp.txt"
            file = File(path, name)
            writeToEncryptedFile(file, content)
        } else {
            //Incrementing the integer counter if a file with the default name already exists
            while (file.exists()) {
                name = "Encrypted_File${++counter}.txt"
                file = File(path, name)
            }
            writeToEncryptedFile(file, content)
        }

        Toast.makeText(this, "Encrypted file was saved to $path", Toast.LENGTH_LONG).show()
    }

    //Writing to an encrypted text file using AES with jetpack security
    private fun writeToEncryptedFile(file: File, inp: String) {
        val encryptedFile = EncryptedFile.Builder(
            file,
            applicationContext,
            masterKeyAlias,
            EncryptedFile.FileEncryptionScheme.AES256_GCM_HKDF_4KB
        ).build()

        val fileContent = inp.toByteArray(StandardCharsets.UTF_8)
        encryptedFile.openFileOutput().apply {
            write(fileContent)
            flush()
            close()
        }
    }

    //Reading from an encrypted text file using AES with jetpack security
    private fun readFromEncryptedFile(filePath: String): String {
        val encryptedFile = EncryptedFile.Builder(
            File(filePath),
            applicationContext,
            masterKeyAlias,
            EncryptedFile.FileEncryptionScheme.AES256_GCM_HKDF_4KB
        ).build()

        val inputStream = encryptedFile.openFileInput()
        val byteArrayOutputStream = ByteArrayOutputStream()
        var nextByte: Int = inputStream.read()
        while (nextByte != -1) {
            byteArrayOutputStream.write(nextByte)
            nextByte = inputStream.read()
        }

        return byteArrayOutputStream.toString()
    }

    //Creates a folder in the external storage
    //Requires MANAGE_EXTERNAL_STORAGE permission on Android 11
    private fun makeDir() {
        val f = File(Environment.getExternalStorageDirectory(), "File Encryption")
        if (!f.exists()) {
            f.mkdirs()
        }
    }

    fun newFile(view: View) {
        newFile.visibility = GONE
        newFileLayout.visibility = VISIBLE
    }

    //Opens a file chooser for picking an encrypted text file for decryption
    fun pickFile(view: View) {
        closeNewFileUI()
        MaterialFilePicker()
            .withActivity(this)
            // With cross icon on the right side of toolbar for closing picker straight away
            .withCloseMenu(true)
            // Entry point path (user will start from it)
            .withPath(Environment.getExternalStorageDirectory().toString() + "/File Encryption")
            // Root path (user won't be able to come higher than it)
            .withRootPath(Environment.getExternalStorageDirectory().toString())
            // Showing hidden files
            .withHiddenFiles(true)
            // Want to choose only text files
            .withFilter(Pattern.compile(".*\\.(txt)$"))
            // Don't apply filter to directories names
            .withFilterDirectories(false)
            .withTitle("Pick a text file")
            .withRequestCode(FILE_PICKER_REQUEST_CODE)
            .start()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode != RESULT_CANCELED && resultCode == RESULT_OK) {
            val sharedPreferences = getSharedPreferences("sharedprefs", MODE_PRIVATE)
            when (requestCode) {
                //An encrypted text file has been picked and is ready for decryption
                FILE_PICKER_REQUEST_CODE -> {
                    filepath = data!!.getStringExtra(FilePickerActivity.RESULT_FILE_PATH)!!
                    bioID()
                }
                //A file has been picked and is ready for encryption
                FILE_TO_ENC_PICKER_REQUEST_CODE -> {
                    //Prompting for a password
                    val alertDialog = AlertDialog.Builder(this)
                    alertDialog.setTitle("Enter Password")
                    val input = EditText(this)
                    val lp = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.MATCH_PARENT
                    )
                    input.layoutParams = lp
                    alertDialog.setView(input)

                    alertDialog.setPositiveButton("encrypt") { _, _ ->
                        //Encrypting the file
                        val password = input.text.toString()

                        if (password == "") {
                            Toast.makeText(
                                this,
                                "Password is empty, cancelling...",
                                Toast.LENGTH_LONG
                            ).show()

                            return@setPositiveButton
                        }

                        val fileName = data!!.getStringExtra(FilePickerActivity.RESULT_FILE_PATH)!!
                        var counter = 1
                        val extension = getFileExtension(fileName)
                        var name = "EncryptedFile$counter.$extension"
                        val path =
                            Environment.getExternalStorageDirectory()
                                .toString() + "/File Encryption"
                        var file = File(path, name)
                        while (file.exists()) {
                            name = "EncryptedFile${++counter}.$extension"
                            file = File(path, name)
                        }

                        file.createNewFile()
                        val encryption = FileEncryption()
                        encryption.encryptFile(fileName, "$path/$name", password)
                        Toast.makeText(
                            this,
                            "Encrypted image saved to $path with this name: $name",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                    alertDialog.show()
                }
                //An encrypted file has been picked and is ready for decryption
                ENC_FILE_PICKER_REQUEST_CODE -> {
                    //Prompting for a password
                    val alertDialog = AlertDialog.Builder(this)
                    alertDialog.setTitle("Enter Password")
                    val input = EditText(this)
                    val lp = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.MATCH_PARENT
                    )
                    input.layoutParams = lp
                    alertDialog.setView(input)

                    alertDialog.setPositiveButton("decrypt") { _, _ ->
                        //Decrypting the file
                        val password = input.text.toString()

                        if (password == "") {
                            Toast.makeText(
                                this,
                                "Password is empty, cancelling...",
                                Toast.LENGTH_LONG
                            ).show()

                            return@setPositiveButton
                        }

                        val fileName = data!!.getStringExtra(FilePickerActivity.RESULT_FILE_PATH)!!
                        val extension = getFileExtension(fileName)
                        var counter = 1
                        var name = "DecryptedFile$counter.$extension"
                        val path =
                            Environment.getExternalStorageDirectory()
                                .toString() + "/File Encryption"
                        var file = File(path, name)
                        val file1 = File(fileName)
                        while (file.exists()) {
                            name = "DecryptedFile${++counter}.$extension"
                            file = File(path, name)
                        }

                        file.createNewFile()
                        val decryption = FileEncryption()
                        val success = decryption.decryptFile(fileName, "$path/$name", password)
                        if (success) {
                            file1.delete()
                            Toast.makeText(
                                this,
                                "Decrypted image saved to $path with this name: $name",
                                Toast.LENGTH_LONG
                            ).show()
                        } else {
                            Toast.makeText(
                                this,
                                "Wrong password!",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                    alertDialog.show()
                }
            }
        }
    }

    //Biometric authentication for decrypting files
    //Will prompt only if available
    private fun bioID() {
        val executor = ContextCompat.getMainExecutor(this)
        val biometricPrompt = BiometricPrompt(this, executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationError(
                    errorCode: Int,
                    errString: CharSequence
                ) {
                    super.onAuthenticationError(errorCode, errString)
                    Toast.makeText(
                        applicationContext,
                        "Authentication error: $errString", Toast.LENGTH_SHORT
                    )
                        .show()
                }

                override fun onAuthenticationSucceeded(
                    result: BiometricPrompt.AuthenticationResult
                ) {
                    super.onAuthenticationSucceeded(result)
                    val fileContent = "The file's content:\n" + readFromEncryptedFile(filepath)
                    decFile.text = fileContent
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    Toast.makeText(
                        applicationContext, "Authentication failed",
                        Toast.LENGTH_SHORT
                    )
                        .show()
                }
            })

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Biometric identification for decrypting")
            .setSubtitle("Decrypt using your biometric credential")
            .setNegativeButtonText("Cancel")
            .build()

        biometricPrompt.authenticate(promptInfo)
    }

    private fun closeNewFileUI() {
        fname.setText("")
        fcontent.setText("")
        newFileLayout.visibility = GONE
        newFile.visibility = VISIBLE
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId
        return if (id == R.id.action_info) {
            infoDialog()
            true
        } else super.onOptionsItemSelected(item)
    }

    override fun onPrepareOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return super.onCreateOptionsMenu(menu)
    }

    //Shows a dialog with information
    private fun infoDialog() {
        AlertDialog.Builder(this@MainActivity)
            .setTitle("Info")
            .setMessage(getString(R.string.info_dialog))
            .setPositiveButton("ok") {_, _ ->
            }
            .show()
    }

    //Opens a file chooser for picking a file for encryption
    fun pickFileToEnc(view: View) {
        closeNewFileUI()
        MaterialFilePicker()
            .withActivity(this)
            // With cross icon on the right side of toolbar for closing picker straight away
            .withCloseMenu(true)
            // Entry point path (user will start from it)
            .withPath(Environment.getExternalStorageDirectory().toString())
            // Root path (user won't be able to come higher than it)
            .withRootPath(Environment.getExternalStorageDirectory().toString())
            // Showing hidden files
            .withHiddenFiles(true)
            // Don't apply filter to directories names
            .withFilterDirectories(false)
            .withTitle("Pick a file")
            .withRequestCode(FILE_TO_ENC_PICKER_REQUEST_CODE)
            .start()
    }

    //Opens a file chooser for picking an encrypted file for decryption
    fun pickEncFile(view: View) {
        closeNewFileUI()
        MaterialFilePicker()
            .withActivity(this)
            // With cross icon on the right side of toolbar for closing picker straight away
            .withCloseMenu(true)
            // Entry point path (user will start from it)
            .withPath(Environment.getExternalStorageDirectory().toString() + "/File Encryption")
            // Root path (user won't be able to come higher than it)
            .withRootPath(Environment.getExternalStorageDirectory().toString() + "/File Encryption")
            // Showing hidden files
            .withHiddenFiles(true)
            // Don't apply filter to directories names
            .withFilterDirectories(false)
            .withTitle("Pick an encrypted file")
            .withRequestCode(ENC_FILE_PICKER_REQUEST_CODE)
            .start()
    }

    //Returns the extensions of files
    //Example: txt for text
    private fun getFileExtension(fullName: String?): String? {
        checkNotNull(fullName)
        val fileName = File(fullName).name
        val dotIndex = fileName.lastIndexOf('.')
        return if (dotIndex == -1) "" else fileName.substring(dotIndex + 1)
    }

    companion object {
        val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
        const val FILE_PICKER_REQUEST_CODE = 42
        const val FILE_TO_ENC_PICKER_REQUEST_CODE = 43
        const val ENC_FILE_PICKER_REQUEST_CODE = 44
    }
}