package com.wal.nask

import androidx.appcompat.app.AppCompatActivity
import androidx.security.crypto.EncryptedFile
import androidx.security.crypto.MasterKey
import androidx.security.crypto.MasterKey.Builder
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream


class SimpleFileCryptography(context: AppCompatActivity) {

    companion object{
        const val EXT_ENCRYPTED = ".enc"
        const val EXT_UNENCRYPTED = ".txt"

        /*
        * not a best way to determine if file is actually encrypted but is simple and in our case is good enough
        */
        fun ifFileEncrypted(fileName: String): Boolean{
            val dotPos = fileName.lastIndexOf(".")
            if(dotPos > 0) {
                val ext = fileName.substring(dotPos)
                if(ext == EXT_ENCRYPTED){
                    return true
                }
            }
            return false
        }

    }

    private val applicationContext = context

    private val masterKey: MasterKey = Builder(applicationContext, MasterKey.DEFAULT_MASTER_KEY_ALIAS)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()


    fun encrypt(fileName: String): String{

        val file = File(applicationContext.filesDir, fileName);
        val fileContent = FileInputStream(file).readBytes()

        if(!ifFileEncrypted(fileName)) {
            val fileToWrite = fileName.substring(0, fileName.lastIndexOf(".")) + EXT_ENCRYPTED

            val encryptedFile = EncryptedFile.Builder(
                applicationContext,
                File(applicationContext.filesDir, fileToWrite),
                masterKey,
                EncryptedFile.FileEncryptionScheme.AES256_GCM_HKDF_4KB
            ).build()

            encryptedFile.openFileOutput().apply {
                write(fileContent)
                flush()
                close()
                file.delete()
            }

            return fileToWrite
        }

        return fileName
    }

    fun decrypt(fileName: String): String{

        if(ifFileEncrypted(fileName)) {
            val file = File(applicationContext.filesDir, fileName)

            val encryptedFile = EncryptedFile.Builder(
                applicationContext,
                file,
                masterKey,
                EncryptedFile.FileEncryptionScheme.AES256_GCM_HKDF_4KB
            ).build()

            val inputStream = encryptedFile.openFileInput()
            val byteArrayOutputStream = ByteArrayOutputStream()
            var nextByte: Int = inputStream.read()
            while (nextByte != -1) {
                byteArrayOutputStream.write(nextByte)
                nextByte = inputStream.read()
            }

            val fileToWrite = fileName.substring(0, fileName.lastIndexOf(".")) + EXT_UNENCRYPTED

            FileSave(applicationContext, fileToWrite).save(byteArrayOutputStream.toByteArray())

            file.delete()

            return fileToWrite
        }


        return fileName
    }

}