package com.levisapps.fileencryption

import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.security.GeneralSecurityException
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.PBEParameterSpec

class FileEncryption {
    //Arbitrarily selecting an 8 byte salt
    private val salt = byteArrayOf(
        0x43.toByte(), 0x76.toByte(), 0x95.toByte(), 0xc7.toByte(),
        0x5b.toByte(), 0xd7.toByte(), 0x45.toByte(), 0x17.toByte()
    )

    @Throws(GeneralSecurityException::class)
    private fun makeCipher(pass: String, decryptMode: Boolean): Cipher? {
        //Generating key from password
        val keySpec = PBEKeySpec(pass.toCharArray())
        val keyFactory: SecretKeyFactory = SecretKeyFactory.getInstance("PBEWithMD5AndDES")
        val key: SecretKey = keyFactory.generateSecret(keySpec)

        //Creating parameters from the salt and an arbitrary number of iterations
        val pbeParamSpec = PBEParameterSpec(salt, 42)

        //setting up the cipher
        val cipher: Cipher = Cipher.getInstance("PBEWithMD5AndDES")

        if (decryptMode) {
            cipher.init(Cipher.ENCRYPT_MODE, key, pbeParamSpec)
        } else {
            cipher.init(Cipher.DECRYPT_MODE, key, pbeParamSpec)
        }
        return cipher
    }

    //Encrypts a file to a new file with a provided password
    @Throws(IOException::class, GeneralSecurityException::class)
    fun encryptFile(fileName: String, outFileName: String, password: String?) {
        val decData: ByteArray
        val encData: ByteArray
        val inFile = File(fileName)
        //Generates the cipher with the password
        val cipher: Cipher = makeCipher(password!!, true)!!

        val inStream = FileInputStream(inFile)
        val blockSize = 8
        //Get amount of padded bytes
        val paddingCount = blockSize - inFile.length().toInt() % blockSize

        //Get full size including padding
        val padded = inFile.length().toInt() + paddingCount
        decData = ByteArray(padded)
        inStream.read(decData)
        inStream.close()

        //Write out padding bytes as per PKCS5 algorithm
        for (i in inFile.length().toInt() until padded) {
            decData[i] = paddingCount.toByte()
        }

        //Encrypt the file
        encData = cipher.doFinal(decData)

        //Write the encrypted data to a file
        val outStream = FileOutputStream(File(outFileName))
        outStream.write(encData)
        outStream.close()
    }

    //Decrypts a file to a new file using the provided password
    @Throws(GeneralSecurityException::class, IOException::class)
    fun decryptFile(fileName: String, outFileName: String, password: String?): Boolean {
        try {
            val encData: ByteArray
            var decData: ByteArray
            val inpFile = File(fileName)

            //Generate cipher with the password
            val cipher: Cipher = makeCipher(password!!, false)!!

            //Read the file
            val inpStream = FileInputStream(inpFile)
            encData = ByteArray(inpFile.length().toInt())
            inpStream.read(encData)
            inpStream.close()

            //Decrypt data
            decData = cipher.doFinal(encData)

            //Get amount padding to remove
            val paddingCount = decData[decData.size - 1].toInt()
            //Checking that padCount bytes at the end have same value
            if (paddingCount in 1..8) {
                decData = decData.copyOfRange(0, decData.size - paddingCount)
            }

            //Write the decrypted data to a file
            val target = FileOutputStream(File(outFileName))
            target.write(decData)
            target.close()

        } catch (e :Exception) { //Catching wrong passwords
            println("Wrong password")
            return false
        }
        return true
    }
}