package com.apipragatos.bluetoothchat.ui

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.apipragatos.bluetoothchat.BluetoothController
import com.apipragatos.bluetoothchat.ChatMessage

@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("MissingPermission")
@Composable
fun ChatScreen(controller: BluetoothController) {
    val messages by controller.messages.collectAsState()
    val isConnected by controller.isConnected.collectAsState()
    val deviceName by controller.connectedDeviceName.collectAsState()
    var textState by remember { mutableStateOf("") }
    var pairedDevices by remember { mutableStateOf(controller.getPairedDevices()) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(text = "Api pra gatos 1 - Mensagens Offline", fontSize = 18.sp)
                        Text(
                            text = if (isConnected) "Conectado a: $deviceName" else "Offline (Aguardando conexão)",
                            fontSize = 12.sp,
                            color = if (isConnected) Color(0xFF2E7D32) else Color.Gray
                        )
                    }
                },
                actions = {
                    if (!isConnected) {
                        IconButton(onClick = {
                            pairedDevices = controller.getPairedDevices()
                            controller.startServer()
                        }) {
                            Text("Esperar", fontSize = 12.sp)
                        }
                    } else {
                        IconButton(onClick = { controller.disconnect() }) {
                            Text("Sair", fontSize = 12.sp)
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (!isConnected) {
                Text(
                    text = "Selecione um dispositivo próximo para conversar via Bluetooth:",
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.bodyMedium
                )
                LazyColumn(modifier = Modifier.weight(1f)) {
                    items(pairedDevices) { device: BluetoothDevice ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 4.dp)
                                .clickable { controller.connectToDevice(device) }
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(text = device.name ?: "Sem nome", style = MaterialTheme.typography.titleMedium)
                                Text(text = device.address, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    reverseLayout = true
                ) {
                    items(messages.reversed()) { message: ChatMessage ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = if (message.isFromMe) Arrangement.End else Arrangement.Start
                        ) {
                            Box(
                                modifier = Modifier
                                    .background(
                                        color = if (message.isFromMe) Color(0xFF00897B) else Color(0xFFE0E0E0),
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    .padding(horizontal = 14.dp, vertical = 8.dp)
                            ) {
                                Text(
                                    text = message.text,
                                    color = if (message.isFromMe) Color.White else Color.Black
                                )
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextField(
                        value = textState,
                        onValueChange = { textState = it },
                        placeholder = { Text("Digite uma mensagem offline...") },
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (controller.sendMessage(textState)) {
                                textState = ""
                            }
                        }
                    ) {
                        Text("Enviar")
                    }
                }
            }
        }
    }
}