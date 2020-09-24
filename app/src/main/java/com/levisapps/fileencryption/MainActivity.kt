package com.levisapps.fileencryption

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricPrompt
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.security.crypto.EncryptedFile
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import com.nbsp.materialfilepicker.MaterialFilePicker
import com.nbsp.materialfilepicker.ui.FilePickerActivity
import kotlinx.android.synthetic.main.activity_main.*
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.charset.StandardCharsets
import java.util.regex.Pattern
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey

class MainActivity : AppCompatActivity(), NewPasswordDialog.NewPasswordDialogListener,
    PasswordDialog.PasswordDialogListener {
    private var filepath = ""
    //private var mUseBio = true
    //private var firstFile = 2

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(findViewById(R.id.toolbar))
        toolbar.showOverflowMenu()

        checkPermissions()

        /*val sharedPreferences = EncryptedSharedPreferences.create(
            "ID",
            masterKeyAlias,
            this,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )

        mUseBio = sharedPreferences.getBoolean("use_biometrics", true)
        firstFile = sharedPreferences.getInt("first_file", 1)*/

        save.setOnClickListener {
            checkPermissions()
            createEncryptedFile()
            closeNewFileUI()
        }

        cancel.setOnClickListener { closeNewFileUI() }
    }

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
        } else {
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

    @SuppressLint("InflateParams")
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

    private fun makeDir() {
        val f = File(Environment.getExternalStorageDirectory(), "File Encryption")
        if (!f.exists()) {
            f.mkdirs()
        }
    }

    fun newFile(view: View) {
        newFile.visibility = GONE
        newFileLayout.visibility = VISIBLE
        /*if (firstFile == 1) {
            val sharedPreferences = EncryptedSharedPreferences.create(
                "ID",
                masterKeyAlias,
                this,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
            sharedPreferences.edit().putInt("first_file", ++firstFile).apply()
            openNewPasswordDialog()
        }*/
    }

    fun pickFile(view: View) {
        closeNewFileUI()
        MaterialFilePicker()
            // Pass a source of context. Can be:
            //    .withActivity(Activity activity)
            //    .withFragment(Fragment fragment)
            //    .withSupportFragment(androidx.fragment.app.Fragment fragment)
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
                FILE_PICKER_REQUEST_CODE -> {
                    filepath = data!!.getStringExtra(FilePickerActivity.RESULT_FILE_PATH)!!
                    bioID()
                    /*if (mUseBio) {
                            bioID()
                        } else {
                            //openPasswordDialog()
                        }*/

                }
                IMAGE_PICKER_REQUEST_CODE -> {
                    val key = if (sharedPreferences.contains("key")) {
                        sharedPreferences.getString("key", null)
                    } else {
                        keyGenerator()
                    }

                    val imagepath = data!!.getStringExtra(FilePickerActivity.RESULT_FILE_PATH)!!
                    var counter = 1
                    var name = "EncryptedImage$counter"
                    val path = Environment.getExternalStorageDirectory().toString() + "/File Encryption"
                    var file = File(path, name)
                    while (file.exists()) {
                        name = "EncryptedFile${++counter}.jpg"
                        file = File(path, name)
                    }

                    val encryption = Encryption()
                    encryption.encrypt(key!!, File(imagepath), file)
                    Toast.makeText(this, "Encrypted image saved to $path with this name: $name", Toast.LENGTH_LONG).show()

                }
                ENC_IMAGE_PICKER_REQUEST_CODE -> {
                    val key = sharedPreferences.getString("key", null)
                    val imagepath = data!!.getStringExtra(FilePickerActivity.RESULT_FILE_PATH)!!
                    var counter = 1
                    var name = "DecryptedImage$counter"
                    val path = Environment.getExternalStorageDirectory().toString() + "/File Encryption"
                    var file = File(path, name)
                    val file1 = File(imagepath)
                    while (file.exists()) {
                        name = "DecryptedFile${++counter}.jpg"
                        file = File(path, name)
                    }

                    val encryption = Encryption()
                    encryption.decrypt(key!!, file1, file)
                    file1.delete()
                    Toast.makeText(this, "Decrypted image saved to $path with this name: $name", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

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
                    //openPasswordDialog()
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
                    //openPasswordDialog()
                }
            })

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Biometric identification for decrypting")
            .setSubtitle("Decrypt using your biometric credential")
            .setNegativeButtonText("Cancel")
            .build()

        biometricPrompt.authenticate(promptInfo)
    }

    /* private fun openNewPasswordDialog() {
         val newPassDialog = NewPasswordDialog()
         newPassDialog.show(supportFragmentManager, "new password dialog")
     }

     private fun openPasswordDialog() {
         val newPassDialog = PasswordDialog()
         newPassDialog.show(supportFragmentManager, "password dialog")
     }*/

    private fun closeNewFileUI() {
        fname.setText("")
        fcontent.setText("")
        newFileLayout.visibility = GONE
        newFile.visibility = VISIBLE
    }

    /*fun exportKey(view: View) {
        AlertDialog.Builder(this@MainActivity)
            .setTitle("Are you sure you want to export the encryption key to a file?")
            .setMessage("The key will be written in plain text to a .txt file.\nYou should not keep that file on this device!")
            .setPositiveButton("yes") { _, _ ->
                val fileName = Environment.getExternalStorageDirectory().toString() + "/File Encryption/AES256_Key.txt"
                File(fileName).writeText(masterKeyAlias)
            }
            .setNegativeButton("no") { _, _ -> }
            .show()
    }*/

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

    private fun infoDialog() {
        AlertDialog.Builder(this@MainActivity)
            .setTitle("Info")
            .setMessage(
                "This app creates encrypted text files in which you can store passwords or other sensitive information.\n\n"
                        + "These files are encrypted with a unique, randomly generated AES 256-bit key that is used only locally on your device and the encrypted files can only be read using this app and only on this device.\n\n"
                        + "For any questions or suggestions: levisappss@gmail.com"
            )
            .setPositiveButton("ok") { _, _ ->
            }
            .show()
    }

    fun pickImage(view: View) {
        closeNewFileUI()
        MaterialFilePicker()
            // Pass a source of context. Can be:
            //    .withActivity(Activity activity)
            //    .withFragment(Fragment fragment)
            //    .withSupportFragment(androidx.fragment.app.Fragment fragment)
            .withActivity(this)
            // With cross icon on the right side of toolbar for closing picker straight away
            .withCloseMenu(true)
            // Entry point path (user will start from it)
            .withPath(Environment.getExternalStorageDirectory().toString())
            // Root path (user won't be able to come higher than it)
            .withRootPath(Environment.getExternalStorageDirectory().toString())
            // Showing hidden files
            .withHiddenFiles(true)
            // Want to choose only jpg images
            .withFilter(Pattern.compile(".*\\.(jpg|jpeg)$"))
            // Don't apply filter to directories names
            .withFilterDirectories(false)
            .withTitle("Pick an image")
            .withRequestCode(IMAGE_PICKER_REQUEST_CODE)
            .start()
    }

    fun pickEncImage(view: View) {
        closeNewFileUI()
        MaterialFilePicker()
            // Pass a source of context. Can be:
            //    .withActivity(Activity activity)
            //    .withFragment(Fragment fragment)
            //    .withSupportFragment(androidx.fragment.app.Fragment fragment)
            .withActivity(this)
            // With cross icon on the right side of toolbar for closing picker straight away
            .withCloseMenu(true)
            // Entry point path (user will start from it)
            .withPath(Environment.getExternalStorageDirectory().toString() + "/File Encryption")
            // Root path (user won't be able to come higher than it)
            .withRootPath(Environment.getExternalStorageDirectory().toString() + "/File Encryption")
            // Showing hidden files
            .withHiddenFiles(true)
            // Want to choose only jpg images
            .withFilter(Pattern.compile(".*\\.(jpg|jpeg)$"))
            // Don't apply filter to directories names
            .withFilterDirectories(false)
            .withTitle("Pick an encrypted image")
            .withRequestCode(ENC_IMAGE_PICKER_REQUEST_CODE)
            .start()
    }

    private fun keyGenerator() :String {
        val path = Environment.getExternalStorageDirectory().toString() + "/File Encryption/" + "AESKey_for_images.txt"
        val file = File(path)

        val keyGen = KeyGenerator.getInstance("AES")
        keyGen.init(256)
        val key = keyGen.generateKey()
        file.writeText(key.toString())
        Toast.makeText(this, "AES key for images has been exported to a file and should be saved somewhere else or deleted. Path: $path", Toast.LENGTH_LONG).show()

        val sharedPreferences = getSharedPreferences("sharedprefs", MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.putString("key", key.toString())
        editor.apply()

        return key.toString()
    }

    override fun applyNewPassword(password: String, useBiometrics: Boolean) {
        val sharedPreferences = EncryptedSharedPreferences.create(
            "ID",
            masterKeyAlias,
            this,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )

        sharedPreferences
            .edit()
            .putString("users_password", password)
            .putBoolean("use_biometrics", useBiometrics)
            .apply()
        //mUseBio = useBiometrics
    }

    override fun applyPassword(password: String): Boolean {
        val sharedPreferences = EncryptedSharedPreferences.create(
            "ID",
            masterKeyAlias,
            this,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )

        return sharedPreferences.getString("users_password", null) == password
    }

    companion object {
        val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
        const val FILE_PICKER_REQUEST_CODE = 42
        const val IMAGE_PICKER_REQUEST_CODE = 43
        const val ENC_IMAGE_PICKER_REQUEST_CODE = 44
    }
}