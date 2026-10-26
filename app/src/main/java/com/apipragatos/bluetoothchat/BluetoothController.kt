package com.apipragatos.bluetoothchat

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothServerSocket
import android.bluetooth.BluetoothSocket
import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.UUID

data class ChatMessage(
    val text: String,
    val isFromMe: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)

class BluetoothController(private val context: Context) {
    private val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
    private val bluetoothAdapter: BluetoothAdapter? = bluetoothManager?.adapter

    private val serviceUuid: UUID = UUID.fromString("fa87c0d0-afac-11de-8a39-0800200c9a66")
    private val serviceName = "ApiPraGatosChat"

    private var serverSocket: BluetoothServerSocket? = null
    private var activeSocket: BluetoothSocket? = null
    private var inputStream: InputStream? = null
    private var outputStream: OutputStream? = null

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    private val _connectedDeviceName = MutableStateFlow<String?>(null)
    val connectedDeviceName: StateFlow<String?> = _connectedDeviceName.asStateFlow()

    private val coroutineScope = CoroutineScope(Dispatchers.IO)

    val isBluetoothEnabled: Boolean
        get() = bluetoothAdapter?.isEnabled == true

    @SuppressLint("MissingPermission")
    fun getPairedDevices(): List<BluetoothDevice> {
        return bluetoothAdapter?.bondedDevices?.toList() ?: emptyList()
    }

    @SuppressLint("MissingPermission")
    fun startServer() {
        coroutineScope.launch {
            try {
                serverSocket = bluetoothAdapter?.listenUsingRfcommWithServiceRecord(serviceName, serviceUuid)
                var shouldLoop = true
                while (shouldLoop) {
                    val socket = serverSocket?.accept()
                    if (socket != null) {
                        manageConnection(socket, socket.remoteDevice.name ?: "Dispositivo Proximo")
                        serverSocket?.close()
                        shouldLoop = false
                    }
                }
            } catch (e: IOException) {
                _isConnected.value = false
            }
        }
    }

    @SuppressLint("MissingPermission")
    fun connectToDevice(device: BluetoothDevice) {
        coroutineScope.launch {
            try {
                bluetoothAdapter?.cancelDiscovery()
                val socket = device.createRfcommSocketToServiceRecord(serviceUuid)
                socket.connect()
                manageConnection(socket, device.name ?: "Desconhecido")
            } catch (e: IOException) {
                _isConnected.value = false
                try {
                    activeSocket?.close()
                } catch (_: IOException) {}
            }
        }
    }

    private fun manageConnection(socket: BluetoothSocket, deviceName: String) {
        activeSocket = socket
        inputStream = socket.inputStream
        outputStream = socket.outputStream
        _connectedDeviceName.value = deviceName
        _isConnected.value = true

        val buffer = ByteArray(1024)
        while (_isConnected.value) {
            try {
                val bytes = inputStream?.read(buffer) ?: -1
                if (bytes > 0) {
                    val receivedText = String(buffer, 0, bytes)
                    val msg = ChatMessage(text = receivedText, isFromMe = false)
                    _messages.value = _messages.value + msg
                }
            } catch (e: IOException) {
                disconnect()
                break
            }
        }
    }

    fun sendMessage(text: String): Boolean {
        if (text.isBlank() || outputStream == null) return false
        return try {
            outputStream?.write(text.toByteArray())
            outputStream?.flush()
            val msg = ChatMessage(text = text, isFromMe = true)
            _messages.value = _messages.value + msg
            true
        } catch (e: IOException) {
            disconnect()
            false
        }
    }

    fun disconnect() {
        try {
            inputStream?.close()
            outputStream?.close()
            activeSocket?.close()
            serverSocket?.close()
        } catch (_: IOException) {}
        _isConnected.value = false
        _connectedDeviceName.value = null
    }
}