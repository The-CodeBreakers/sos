package com.satyam.takesos

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.Context
import java.io.InputStream
import java.io.OutputStream
import java.util.*

class BluetoothManager(private val context: Context) {

    private val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
    private var socket: BluetoothSocket? = null
    private var inputStream: InputStream? = null
    private var outputStream: OutputStream? = null

    fun getPairedDevices(): Set<BluetoothDevice>? {
        return bluetoothAdapter?.bondedDevices
    }

    @SuppressLint("MissingPermission")
    fun connect(device: BluetoothDevice): Boolean {
        return try {
            val uuid: UUID = device.uuids?.get(0)?.uuid
                ?: UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

            socket = device.createRfcommSocketToServiceRecord(uuid)
            socket?.connect()

            inputStream = socket?.inputStream
            outputStream = socket?.outputStream

            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun isConnected(): Boolean {
        return socket?.isConnected == true
    }

    fun sendData(data: String) {
        try {
            outputStream?.write(data.toByteArray())
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun startListening(onMessageReceived: (String) -> Unit) {

        Thread {
            val buffer = ByteArray(1024)

            while (true) {
                try {
                    val bytes = inputStream?.read(buffer) ?: break
                    val message = String(buffer, 0, bytes)
                    onMessageReceived(message)
                } catch (e: Exception) {
                    break
                }
            }
        }.start()
    }
}