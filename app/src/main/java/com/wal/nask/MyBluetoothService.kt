package com.wal.nask

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothServerSocket
import android.bluetooth.BluetoothSocket
import android.util.Log
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.*
import kotlin.concurrent.thread

private const val TAG = "NASK_APP_DEBUG_TAG"


class MyBluetoothService(private val handler: MainActivity) {

    companion object{
        const val BLUETOOTH_DEVICE_NAME = "nask"
        const val BLUETOOTH_DEVICE_UUID = "85a4d453-eb37-4c8e-8d4c-ee030769d01f"
    }


    private var bluetoothAdapter: BluetoothAdapter = handler.bluetoothAdapter

    private var serviceUUID: UUID = UUID.fromString(BLUETOOTH_DEVICE_UUID)

    private var acceptThread: AcceptThread = AcceptThread()
    private var connectThread: ConnectThread? = null
    private var openedSocket: BluetoothSocket? = null

    fun accept(){
        acceptThread.start()
    }

    private fun isConnectedToDevice(device: BluetoothDevice): Boolean{
        if(connectThread?.device?.address.equals(device.address) && openedSocket != null){
           return true
        }
        return false
    }

    private fun connect(device: BluetoothDevice){
        connectThread?.cancel()
        connectThread = ConnectThread(device)
        connectThread!!.start()
    }

    private fun closeSocket() {
        try {
            openedSocket?.close()
        } catch (e: IOException) {
            Log.e(TAG, "Could not close the connect socket", e)
        }
    }

    private fun manageMyConnectedSocket(socket: BluetoothSocket){
        closeSocket()
        openedSocket = socket

        val connectedThread = ConnectedThread(socket)
        connectedThread.start()

    }


    fun sendToDevice(device: BluetoothDevice, bytesToSend: ByteArray){
        if(isConnectedToDevice(device)) {
            send(bytesToSend)
        }else{
            connect(device)
            thread(start = true){
                while(!isConnectedToDevice(device)){
                    Thread.sleep(100)
                }
                send(bytesToSend)
            }
        }
    }

    private fun send(bytesToSend: ByteArray){

        if(openedSocket != null) {

            val mmOutStream: OutputStream = openedSocket!!.outputStream

            try {
                mmOutStream.write(bytesToSend)
            } catch (e: IOException) {
                Log.e(TAG, "Error occurred when sending data", e)
                handler.toastBluetoothError("Error occurred when sending data")
            }

        }
    }

    private inner class AcceptThread : Thread() {

        private val mmServerSocket: BluetoothServerSocket? by lazy(LazyThreadSafetyMode.NONE) {
            bluetoothAdapter.listenUsingInsecureRfcommWithServiceRecord(BLUETOOTH_DEVICE_NAME, serviceUUID)
        }

        override fun run() {
            // Keep listening until exception occurs or a socket is returned.
            var shouldLoop = true
            while (shouldLoop) {
                val socket: BluetoothSocket? = try {
                    mmServerSocket?.accept()
                } catch (e: IOException) {
                    Log.e(TAG, "Socket's accept() method failed", e)
                    handler.toastBluetoothError("Socket's accept() method failed")
                    shouldLoop = false
                    null
                }
                socket?.also {
                    manageMyConnectedSocket(it)
                    mmServerSocket?.close()
                    shouldLoop = false
                }
            }
        }

        // Closes the connect socket and causes the thread to finish.
        fun cancel() {
            try {
                mmServerSocket?.close()
            } catch (e: IOException) {
                Log.e(TAG, "Could not close the connect socket", e)
            }
        }
    }

    private inner class ConnectThread(device: BluetoothDevice) : Thread() {

        val device: BluetoothDevice = device

        private val mmSocket: BluetoothSocket? by lazy(LazyThreadSafetyMode.NONE) {
            device.createRfcommSocketToServiceRecord(serviceUUID)
        }

        public override fun run() {
            // Cancel discovery because it otherwise slows down the connection.
            bluetoothAdapter.cancelDiscovery()

            mmSocket?.let { socket ->
                // Connect to the remote device through the socket. This call blocks
                // until it succeeds or throws an exception.
                try {
                    socket.connect()

                    // The connection attempt succeeded. Perform work associated with
                    // the connection in a separate thread.
                    manageMyConnectedSocket(socket)
                } catch (e: IOException) {
                    Log.e(TAG, "Could not connect to socket", e)
                    handler.toastBluetoothError("Could not connect to device [" + device.name + "]")
                }
            }
        }

        // Closes the client socket and causes the thread to finish.
        fun cancel() {
            try {
                mmSocket?.close()
            } catch (e: IOException) {
                Log.e(TAG, "Could not close the client socket", e)
            }
        }
    }


    private inner class ConnectedThread(private val mmSocket: BluetoothSocket) : Thread() {

        private val mmInStream: InputStream = mmSocket.inputStream
        private val mmBuffer: ByteArray = ByteArray(1024) // mmBuffer store for the stream

        override fun run() {
            var numBytes: Int // bytes returned from read()

            // Keep listening to the InputStream until an exception occurs.
            while (true) {
                // Read from the InputStream.
                numBytes = try {
                    mmInStream.read(mmBuffer)
                } catch (e: IOException) {
                    Log.d(TAG, "Input stream was disconnected", e)
                    break
                }

                handler.setInputData(mmBuffer.decodeToString(0, numBytes))

            }
        }

        // Call this method from the main activity to shut down the connection.
        fun cancel() {
            try {
                mmSocket.close()
            } catch (e: IOException) {
                Log.e(TAG, "Could not close the connect socket", e)
            }
        }
    }


}