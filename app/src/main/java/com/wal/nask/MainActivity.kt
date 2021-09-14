package com.wal.nask

import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.DialogInterface
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.InputType
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.google.android.material.textfield.TextInputEditText
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import java.io.File
import java.io.FileInputStream

class MainActivity : AppCompatActivity() {

    private lateinit var openFileListLauncher: ActivityResultLauncher<Intent>
    private lateinit var bluetoothDeviceListLauncher: ActivityResultLauncher<Intent>

    private lateinit var dataInput:TextInputEditText

    val bluetoothAdapter: BluetoothAdapter = BluetoothAdapter.getDefaultAdapter()

    private lateinit var myBluetoothService: MyBluetoothService

    companion object{
        const val EXTRA_FILE_NAME = "FILE_NAME"
        const val EXTRA_BLUETOOTH_DEVICE_NAME = "BLUETOOTH_DEVICE_NAME"
        const val EXTRA_BLUETOOTH_DEVICE_ADDRESS = "BLUETOOTH_DEVICE_ADDRESS"
        const val SELECT_DEVICE_REQUEST_CODE = 42
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        dataInput = findViewById(R.id.dataInput)

        val saveToFileFileBtn = findViewById<Button>(R.id.saveToFileFileBtn)
        saveToFileFileBtn.setOnClickListener{
            if(!dataInput.text.isNullOrEmpty()) {
                fileNameInputDialog();
            }else{
                Toast.makeText(this, "Saving an empty file is pointless ", Toast.LENGTH_LONG).show()
            }
        }


        val openFileBtn = findViewById<Button>(R.id.openFileBtn)
        openFileBtn.setOnClickListener{
            val intent = Intent(this, FileListActivity::class.java)
            openFileListLauncher.launch(intent)
        }


        openFileListLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            val data: Intent? = result.data
            if (result.resultCode == Activity.RESULT_OK) {
                if (data != null) {
                    val pickedFileName = data.getStringExtra(EXTRA_FILE_NAME)
                    if(!pickedFileName.isNullOrEmpty()){
                        val file = File(this.filesDir, pickedFileName);
                        FileInputStream(file).bufferedReader().use {
                            dataInput.setText(it.readText())
                        }
                    }
                }
            }
        }

        bluetoothDeviceListLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            val data: Intent? = result.data
            if (result.resultCode == Activity.RESULT_OK) {
                if (data != null) {
                    val deviceAddress = data.getStringExtra(EXTRA_BLUETOOTH_DEVICE_ADDRESS)
                    if(!deviceAddress.isNullOrEmpty()){
                        val pairedDevices: Set<BluetoothDevice>? = bluetoothAdapter.bondedDevices
                        pairedDevices?.forEach { device ->
                            if(device.address.equals(deviceAddress)) {
                                if(device.bondState == BluetoothDevice.BOND_BONDED) {
                                    myBluetoothService.sendToDevice(
                                        device,
                                        dataInput.text.toString().encodeToByteArray()
                                    )
                                }else{
                                    Toast.makeText(this, "Selected device ["+ device.name +"] is not bonded", Toast.LENGTH_LONG).show()
                                }
                            }
                        }
                    }
                }
            }
        }

        val bluetoothSendButton = findViewById<Button>(R.id.bluetoothSendButton)
        bluetoothSendButton.setOnClickListener{

            if(bluetoothSendButton.text.isNotEmpty()) {
                val intent = Intent(this, PairedBluetoothDevicesActivity::class.java)
                bluetoothDeviceListLauncher.launch(intent)
            }else{
                Toast.makeText(this, "Empty input", Toast.LENGTH_SHORT).show()
            }

        }

        myBluetoothService = MyBluetoothService(this)
        myBluetoothService.accept()

    }


    private fun fileNameInputDialog(){
        val builder: AlertDialog.Builder = AlertDialog.Builder(this)
        builder.setTitle("Save file as")

        val input = EditText(this)
        input.hint = "Enter file name here"
        input.inputType = InputType.TYPE_CLASS_TEXT
        builder.setView(input)

        builder.setPositiveButton("OK", DialogInterface.OnClickListener { dialog, which ->
            var fileName = input.text.toString()

            val fileSave = FileSave(this, "$fileName.txt")

            fileName = fileSave.save(dataInput.text.toString().toByteArray())

            Toast.makeText(this, "File [$fileName] Saved", Toast.LENGTH_SHORT).show()

        })
        builder.setNegativeButton("Cancel", DialogInterface.OnClickListener { dialog, which -> dialog.cancel() })

        builder.show()
    }


    fun setInputData(data: String){
        runOnUiThread {
            dataInput.setText(data)
        }
    }

    fun toastBluetoothError(message: String){
        runOnUiThread {
            Toast.makeText(this, message, Toast.LENGTH_LONG).show()
        }
    }

}