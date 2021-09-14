package com.wal.nask

import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.companion.AssociationRequest
import android.companion.BluetoothDeviceFilter
import android.companion.CompanionDeviceManager
import android.content.Context
import android.content.Intent
import android.content.IntentSender
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import com.google.android.material.card.MaterialCardView
import kotlin.concurrent.thread

class PairedBluetoothDevicesActivity : AppCompatActivity() {

    companion object{
        const val SELECT_DEVICE_REQUEST_CODE = 42
    }

    private lateinit var bluetoothAdapter: BluetoothAdapter

    private lateinit var listOfBluetoothDevicesLinearLayout: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_paired_bluetooth_devices)

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()

        listOfBluetoothDevicesLinearLayout = findViewById(R.id.bluetoothDevicesListLinearLayout)

        fillTheList()

        var searchBlueToothDeviceButton = findViewById<Button>(R.id.searchBlueToothDeviceButton)
        searchBlueToothDeviceButton.setOnClickListener {
            val deviceFilter: BluetoothDeviceFilter = BluetoothDeviceFilter.Builder().build()

            val pairingRequest: AssociationRequest = AssociationRequest.Builder()
                .addDeviceFilter(deviceFilter)
                .build()


            val deviceManager = this.getSystemService(Context.COMPANION_DEVICE_SERVICE) as CompanionDeviceManager

            deviceManager.associate(pairingRequest,
                object : CompanionDeviceManager.Callback() {
                    // Called when a device is found. Launch the IntentSender so the user
                    // can select the device they want to pair with.
                    override fun onDeviceFound(chooserLauncher: IntentSender) {
                        startIntentSenderForResult(chooserLauncher, SELECT_DEVICE_REQUEST_CODE, null, 0, 0, 0)
                    }

                    override fun onFailure(error: CharSequence?) {
                        // Handle the failure.
                    }
                }, null)

            searchBlueToothDeviceButton.isEnabled = false
        }

    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        when (requestCode) {
            MainActivity.SELECT_DEVICE_REQUEST_CODE -> when(resultCode) {
                Activity.RESULT_OK -> {
                    // The user chose to pair the app with a Bluetooth device.
                    val deviceToPair: BluetoothDevice? = data?.getParcelableExtra(CompanionDeviceManager.EXTRA_DEVICE)
                    deviceToPair?.let { device ->
                        device.createBond()
                        // Continue to interact with the paired device.
                        addDeviceToList(device)
                    }
                }
            }
            else -> super.onActivityResult(requestCode, resultCode, data)
        }
    }

    private fun addDeviceToList(device: BluetoothDevice){
        val card = MaterialCardView.inflate(this, R.layout.bt_device_card_view_layout, null)

        val deviceNameTextView = card.findViewById<TextView>(R.id.deviceNameTextView)
        deviceNameTextView.text = device.name

        val deviceMACaddressTextView = card.findViewById<TextView>(R.id.deviceMACaddressTextView)

        deviceMACaddressTextView.text = device.address

        card.setOnClickListener {
            val intent = Intent()
            intent.putExtra(MainActivity.EXTRA_BLUETOOTH_DEVICE_NAME, device.name)
            intent.putExtra(MainActivity.EXTRA_BLUETOOTH_DEVICE_ADDRESS, device.address)
            setResult(Activity.RESULT_OK, intent)
            finish()
        }

        listOfBluetoothDevicesLinearLayout.addView(card)

    }

    private fun fillTheList(){
        val pairedDevices: Set<BluetoothDevice>? = bluetoothAdapter.bondedDevices
        pairedDevices?.forEach { device ->
            addDeviceToList(device)
        }
    }
}