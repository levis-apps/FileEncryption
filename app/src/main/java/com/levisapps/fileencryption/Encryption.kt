package com.levisapps.fileencryption

import android.media.MediaCodec.CryptoException
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.security.Key
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec


class Encryption {
    private val algorithm = "AES"
    private val transformation = "AES"

    @Throws(CryptoException::class)
    fun encrypt(key: String, inputFile: File, outputFile: File) {
        doCrypto(Cipher.ENCRYPT_MODE, key, inputFile, outputFile)
    }

    @Throws(CryptoException::class)
    fun decrypt(key: String, inputFile: File, outputFile: File) {
        doCrypto(Cipher.DECRYPT_MODE, key, inputFile, outputFile)
    }

    @Throws(CryptoException::class)
    private fun doCrypto(
        cipherMode: Int, key: String, inputFile: File,
        outputFile: File
    ) {
        val secretKey: Key = SecretKeySpec(key.toByteArray(), algorithm)
        val cipher: Cipher = Cipher.getInstance(transformation)
        cipher.init(cipherMode, secretKey)

        val inputStream = FileInputStream(inputFile)
        val inputBytes = ByteArray(inputFile.length().toInt())
        inputStream.read(inputBytes)

        val outputBytes: ByteArray = cipher.doFinal(inputBytes)
        val outputStream = FileOutputStream(outputFile)
        outputStream.write(outputBytes)
        inputStream.close()
        outputStream.close()
    }
}