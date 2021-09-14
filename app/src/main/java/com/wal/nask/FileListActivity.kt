package com.wal.nask

import android.content.res.Configuration
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.*
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.io.File
import java.util.concurrent.Executor

private val charPool : List<Char> = ('a'..'z') + ('A'..'Z') + ('0'..'9')

class FileListActivity : AppCompatActivity() {

    private lateinit var adapter: CustomAdapter

    private lateinit var simpleFileCryptography: SimpleFileCryptography

    private var nightModeFlags: Int = Configuration.UI_MODE_NIGHT_UNDEFINED


    private lateinit var fileListRecyclerView: RecyclerView
    private lateinit var linearLayoutManager: LinearLayoutManager

    private lateinit var executor: Executor
    private lateinit var biometricPrompt: BiometricPrompt
    private lateinit var promptInfo: BiometricPrompt.PromptInfo

    private lateinit var fileNameToDecrypt: String
    private lateinit var fileNameDecryptTextView:TextView
    private lateinit var decryptImageButton: ImageButton

    private var fileList: ArrayList<String> = ArrayList()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_file_list)

        simpleFileCryptography = SimpleFileCryptography(this)

        nightModeFlags = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK

        linearLayoutManager = LinearLayoutManager(this)
        fileListRecyclerView = findViewById(R.id.fileListRecyclerView)
        fileListRecyclerView.layoutManager = linearLayoutManager


        fileList.addAll(File(this.filesDir.toURI()).list())

        adapter = CustomAdapter(fileList, this)
        fileListRecyclerView.adapter = adapter


        executor = ContextCompat.getMainExecutor(this)
        biometricPrompt = BiometricPrompt(this, executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    Toast.makeText(applicationContext, "Authentication error: $errString", Toast.LENGTH_SHORT).show()
                }

                override fun onAuthenticationSucceeded(
                    result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    Toast.makeText(applicationContext, "Authentication succeeded!", Toast.LENGTH_SHORT).show()

                    fileNameDecryptTextView.text = simpleFileCryptography.decrypt(fileNameToDecrypt)
                    decryptImageButton.setImageResource(R.drawable.ic_baseline_no_encryption_24)

                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    Toast.makeText(applicationContext, "Authentication failed", Toast.LENGTH_SHORT).show()
                }
            })


        val add100RandFilesButton = findViewById<Button>(R.id.add100RandFilesButton)
        add100RandFilesButton.setOnClickListener{
            add100RandFilesButton.isEnabled = false

            val activity = this

            GlobalScope.launch {
                for (x in 0 until 100) {
                    val randomFileNameString = (1..16)
                        .map { _ -> kotlin.random.Random.nextInt(0, charPool.size) }
                        .map(charPool::get)
                        .joinToString("");

                    val fileSave = FileSave(activity, randomFileNameString + SimpleFileCryptography.EXT_UNENCRYPTED)

                    val randomDataString = (1..512)
                        .map { _ -> kotlin.random.Random.nextInt(0, charPool.size) }
                        .map(charPool::get)
                        .joinToString("");

                    fileSave.save(randomDataString.encodeToByteArray())

                    runOnUiThread {
                        fileList.add(randomFileNameString + SimpleFileCryptography.EXT_UNENCRYPTED)
                        adapter.notifyItemInserted(fileList.size -1)
                    }

                }

                runOnUiThread {
                    Toast.makeText(applicationContext, "100 random files added", Toast.LENGTH_SHORT).show()
                    add100RandFilesButton.isEnabled = true
                }
            }
        }

    }

    fun getBiometricPrompt(): BiometricPrompt{
        return biometricPrompt
    }

    fun setDecryptUpdate(fileName: String, textView: TextView, imageButton: ImageButton){
        fileNameToDecrypt = fileName
        fileNameDecryptTextView = textView
        decryptImageButton = imageButton
    }

}