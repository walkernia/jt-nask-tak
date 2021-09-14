package com.wal.nask

import androidx.appcompat.app.AppCompatActivity
import java.io.File
import java.io.FileOutputStream

class FileSave(activity: AppCompatActivity, fileName: String) {

    private var context: AppCompatActivity = activity
    private var fileName: String = fileName.replace("[\\\\/:*?\"<>|]".toRegex(), "_")

    fun save(data: ByteArray):String {

        val file = File(context.filesDir, fileName)

        FileOutputStream(file).use{
            it.write(data)
        }
        return fileName
    }


}