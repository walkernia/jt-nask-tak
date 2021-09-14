package com.wal.nask

import android.app.Activity
import android.content.Intent
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.biometric.BiometricPrompt
import androidx.recyclerview.widget.RecyclerView

class CustomAdapter(private val dataSet: ArrayList<String>, private val activity: FileListActivity) : RecyclerView.Adapter<CustomAdapter.ViewHolder>()  {

    private var promptInfo: BiometricPrompt.PromptInfo

    private var simpleFileCryptography: SimpleFileCryptography

    init {
        simpleFileCryptography = SimpleFileCryptography(activity)

        promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Biometric authentication")
            .setSubtitle("File decryption requires biometric authentication")
            .setNegativeButtonText("Use account password")
            .build()

    }

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view), View.OnClickListener {
        val fileNumberTextView: TextView
        val fileNameTextView: TextView
        val encryptionImageButton: ImageButton

        init {

            view.setOnClickListener(this)

            fileNumberTextView = view.findViewById(R.id.fileNumberTextView)
            fileNameTextView = view.findViewById(R.id.fileNameTextView)
            encryptionImageButton = view.findViewById(R.id.encryptionImageButton)

        }

        override fun onClick(v: View) {
            Log.d("RecyclerView", "CLICK!")
            val intent = Intent()
            intent.putExtra(MainActivity.EXTRA_FILE_NAME, fileNameTextView.text)
            activity.setResult(Activity.RESULT_OK, intent)
            activity.finish()

        }
    }

    override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(viewGroup.context).inflate(R.layout.card_view_layout, viewGroup, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(viewHolder: ViewHolder, position: Int) {

        val fileNumber = (position + 1).toString() + "."
        viewHolder.fileNumberTextView.text = fileNumber

        viewHolder.fileNameTextView.text = dataSet[position]


        if(SimpleFileCryptography.ifFileEncrypted(dataSet[position])){
            viewHolder.encryptionImageButton.setImageResource(R.drawable.ic_baseline_enhanced_encryption_24)
        }else{
            viewHolder.encryptionImageButton.setImageResource(R.drawable.ic_baseline_no_encryption_24)
        }

        viewHolder.encryptionImageButton.setOnClickListener{
            if(SimpleFileCryptography.ifFileEncrypted(viewHolder.fileNameTextView.text.toString())){
                activity.getBiometricPrompt().authenticate(promptInfo)
                activity.setDecryptUpdate(dataSet[position], viewHolder.fileNameTextView, viewHolder.encryptionImageButton)
            }else{
                viewHolder.fileNameTextView.text = simpleFileCryptography.encrypt(dataSet[position])
                viewHolder.encryptionImageButton.setImageResource(R.drawable.ic_baseline_enhanced_encryption_24)
            }
        }

    }

    override fun getItemCount() = dataSet.size

}