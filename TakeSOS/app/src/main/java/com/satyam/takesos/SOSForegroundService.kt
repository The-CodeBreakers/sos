package com.satyam.takesos

import android.Manifest
import android.app.*
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.IBinder
import android.telephony.SmsManager
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority

class SOSForegroundService : Service() {

    private lateinit var bluetoothManager: BluetoothManager
    private val channelId = "SOS_SERVICE_CHANNEL"

    override fun onCreate() {
        super.onCreate()

        bluetoothManager = BluetoothManager(this)

        createNotificationChannel()

        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("Smart SOS Active")
            .setContentText("Listening for emergency trigger...")
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        startForeground(1, notification)

        startListeningForSOS()
    }

    @Suppress("MissingPermission")
    private fun startListeningForSOS() {

        bluetoothManager.startListening {

            if (hasPermission(Manifest.permission.ACCESS_FINE_LOCATION) &&
                hasPermission(Manifest.permission.SEND_SMS)
            ) {

                val fusedLocationClient =
                    LocationServices.getFusedLocationProviderClient(this)

                fusedLocationClient.getCurrentLocation(
                    Priority.PRIORITY_HIGH_ACCURACY,
                    null
                ).addOnSuccessListener { location ->

                    if (location != null) {

                        val mapsLink =
                            "https://maps.google.com/?q=${location.latitude},${location.longitude}"

                        val message =
                            "🚨 EMERGENCY! I need help!\nMy Location:\n$mapsLink"

                        val contactManager = ContactManager(this)
                        val contacts = contactManager.getContacts()

                        val smsManager = SmsManager.getDefault()

                        contacts.forEach { number ->
                            if (number.isNotEmpty()) {
                                smsManager.sendTextMessage(
                                    number,
                                    null,
                                    message,
                                    null,
                                    null
                                )
                            }
                        }

                        Toast.makeText(
                            this,
                            "SOS Sent from Background!",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }
        }
    }

    private fun hasPermission(permission: String): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            permission
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            val channel = NotificationChannel(
                channelId,
                "SOS Service",
                NotificationManager.IMPORTANCE_LOW
            )

            val manager =
                getSystemService(NotificationManager::class.java)

            manager.createNotificationChannel(channel)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null
}