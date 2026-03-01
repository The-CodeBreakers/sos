package com.satyam.takesos

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.telephony.SmsManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority

class MainActivity : ComponentActivity() {

    private lateinit var fusedLocationClient: com.google.android.gms.location.FusedLocationProviderClient
    private lateinit var bluetoothManager: BluetoothManager
    private var savedNumbers: List<String> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        bluetoothManager = BluetoothManager(this)

        requestPermissionsIfNeeded()

        val pairedDevices =
            bluetoothManager.getPairedDevices()?.toList() ?: emptyList()

        setContent {
            SOSScreen(
                onSOSClick = { numbers ->
                    savedNumbers = numbers
                    sendSOS(numbers)
                },

                onConnectDevice = { device ->
                    connectToDevice(device)
                },

                onSendToESPClick = { numbers ->
                    savedNumbers = numbers
                    sendNumbersToESP(numbers)
                },

                pairedDevices = pairedDevices
            )
        }
    }

    // ================= CONNECT DEVICE =================

    @SuppressLint("MissingPermission")
    private fun connectToDevice(device: BluetoothDevice) {

        if (!hasPermission(Manifest.permission.BLUETOOTH_CONNECT)) {
            Toast.makeText(this, "Bluetooth permission required", Toast.LENGTH_SHORT).show()
            requestPermissionsIfNeeded()
            return
        }

        try {

            val connected = bluetoothManager.connect(device)

            Toast.makeText(
                this,
                if (connected) "Connected to ${device.name}" else "Connection Failed",
                Toast.LENGTH_SHORT
            ).show()

            if (connected) {

                bluetoothManager.startListening { message: String ->

                    runOnUiThread {

                        if (message.trim() == "SOS") {

                            Toast.makeText(
                                this,
                                "🚨 SOS Triggered from ESP32!",
                                Toast.LENGTH_SHORT
                            ).show()

                            if (savedNumbers.isNotEmpty()) {
                                sendSOS(savedNumbers)
                            } else {
                                Toast.makeText(
                                    this,
                                    "No numbers saved!",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    }
                }
            }

        } catch (_: SecurityException) {
            Toast.makeText(this, "Bluetooth Permission Error", Toast.LENGTH_SHORT).show()
        }
    }

    // ================= PERMISSIONS =================

    private fun requestPermissionsIfNeeded() {

        val permissions = mutableListOf<String>()

        if (!hasPermission(Manifest.permission.ACCESS_FINE_LOCATION))
            permissions.add(Manifest.permission.ACCESS_FINE_LOCATION)

        if (!hasPermission(Manifest.permission.SEND_SMS))
            permissions.add(Manifest.permission.SEND_SMS)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (!hasPermission(Manifest.permission.BLUETOOTH_CONNECT))
                permissions.add(Manifest.permission.BLUETOOTH_CONNECT)
        }

        if (permissions.isNotEmpty()) {
            ActivityCompat.requestPermissions(
                this,
                permissions.toTypedArray(),
                100
            )
        }
    }

    private fun hasPermission(permission: String): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            permission
        ) == PackageManager.PERMISSION_GRANTED
    }

    // ================= SEND NUMBERS TO ESP =================

    private fun sendNumbersToESP(numbers: List<String>) {

        if (!bluetoothManager.isConnected()) {
            Toast.makeText(this, "Bluetooth not connected", Toast.LENGTH_SHORT).show()
            return
        }

        val formatted =
            numbers.filter { it.isNotBlank() }
                .joinToString(",") + "\n"

        bluetoothManager.sendData(formatted)

        Toast.makeText(this, "Numbers sent to ESP32", Toast.LENGTH_SHORT).show()
    }

    // ================= SEND SOS =================

    @SuppressLint("MissingPermission")
    private fun sendSOS(numbers: List<String>) {

        if (!hasPermission(Manifest.permission.ACCESS_FINE_LOCATION) ||
            !hasPermission(Manifest.permission.SEND_SMS)
        ) {
            Toast.makeText(this, "Permissions not granted", Toast.LENGTH_SHORT).show()
            requestPermissionsIfNeeded()
            return
        }

        try {

            fusedLocationClient.getCurrentLocation(
                Priority.PRIORITY_HIGH_ACCURACY,
                null
            ).addOnSuccessListener { location ->

                if (location == null) {
                    Toast.makeText(this, "Location not found", Toast.LENGTH_SHORT).show()
                    return@addOnSuccessListener
                }

                val message =
                    "🚨 EMERGENCY!\nhttps://maps.google.com/?q=${location.latitude},${location.longitude}"

                val smsManager: SmsManager =
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        getSystemService(SmsManager::class.java)
                    } else {
                        @Suppress("DEPRECATION")
                        SmsManager.getDefault()
                    }

                numbers.filter { it.isNotBlank() }.forEach { number ->
                    try {
                        smsManager.sendTextMessage(
                            number,
                            null,
                            message,
                            null,
                            null
                        )
                    } catch (_: SecurityException) {
                        Toast.makeText(
                            this,
                            "SMS Permission denied",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }

                Toast.makeText(this, "SOS Sent Successfully!", Toast.LENGTH_LONG).show()

            }.addOnFailureListener {
                Toast.makeText(this, "Failed to get location", Toast.LENGTH_SHORT).show()
            }

        } catch (_: SecurityException) {
            Toast.makeText(this, "Permission Error", Toast.LENGTH_SHORT).show()
        }
    }
}