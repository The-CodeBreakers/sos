package com.satyam.takesos

import android.bluetooth.BluetoothDevice
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Suppress("MissingPermission")
@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun SOSScreen(
    onSOSClick: (List<String>) -> Unit,
    onConnectDevice: (BluetoothDevice) -> Unit,
    onSendToESPClick: (List<String>) -> Unit,
    pairedDevices: List<BluetoothDevice>
) {

    var number1 by remember { mutableStateOf("") }
    var number2 by remember { mutableStateOf("") }
    var number3 by remember { mutableStateOf("") }

    var connectedDevice by remember { mutableStateOf<BluetoothDevice?>(null) }
    var isHolding by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()

    val numbers = listOf(number1, number2, number3)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {

        // 🔹 Number Input Fields
        OutlinedTextField(
            value = number1,
            onValueChange = { number1 = it },
            label = { Text("Emergency Number 1") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = number2,
            onValueChange = { number2 = it },
            label = { Text("Emergency Number 2") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = number3,
            onValueChange = { number3 = it },
            label = { Text("Emergency Number 3") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(20.dp))

        // 🔥 Long Press SOS Button
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(70.dp)
                .background(
                    if (isHolding) Color.Red
                    else MaterialTheme.colorScheme.error
                )
                .combinedClickable(
                    onClick = {},
                    onLongClick = {
                        scope.launch {
                            isHolding = true
                            delay(2000)
                            onSOSClick(numbers)     // ✅ FIXED
                            isHolding = false
                        }
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = if (isHolding)
                    "Sending SOS..."
                else
                    "🚨 HOLD TO SEND SOS",
                color = Color.White
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        Button(
            onClick = { onSendToESPClick(numbers) },   // ✅ FIXED
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("📤 SEND NUMBERS TO ESP")
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "Paired Devices",
            style = MaterialTheme.typography.titleMedium
        )

        Spacer(modifier = Modifier.height(10.dp))

        LazyColumn(
            modifier = Modifier.weight(1f)
        ) {
            items(pairedDevices) { device ->

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clickable {
                            connectedDevice = device
                            onConnectDevice(device)
                        },
                    colors = CardDefaults.cardColors(
                        containerColor =
                            if (connectedDevice == device)
                                Color(0xFFB2DFDB)
                            else
                                MaterialTheme.colorScheme.surface
                    )
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {

                        Text(
                            text = device.name ?: "Bluetooth Device",
                            style = MaterialTheme.typography.bodyLarge
                        )

                        Text(
                            text = device.address,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }
    }
}