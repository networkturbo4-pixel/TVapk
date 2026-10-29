package com.omnicast.tvsignage.utils

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress

object WakeOnLanHelper {

    private const val PORT = 9

    /**
     * Envía un Magic Packet de Wake-on-LAN para encender la TV Android.
     * @param macAddress Dirección MAC en formato "AA:BB:CC:DD:EE:FF" o "AA-BB-CC-DD-EE-FF"
     * @param broadcastIp Dirección de broadcast (opcional, por defecto "255.255.255.255")
     */
    suspend fun wakeUp(macAddress: String, broadcastIp: String = "255.255.255.255"): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                val cleanMac = macAddress.replace(":", "").replace("-", "").trim()
                require(cleanMac.length == 12) { "Dirección MAC inválida: $macAddress" }

                val macBytes = ByteArray(6) { i ->
                    cleanMac.substring(i * 2, (i * 2) + 2).toInt(16).toByte()
                }

                // 6 bytes de 0xFF seguidos de la MAC repetida 16 veces
                val bytes = ByteArray(6 + (16 * macBytes.size))
                for (i in 0 until 6) {
                    bytes[i] = 0xFF.toByte()
                }
                for (i in 0 until 16) {
                    System.arraycopy(macBytes, 0, bytes, 6 + (i * 6), 6)
                }

                val address = InetAddress.getByName(broadcastIp)
                val packet = DatagramPacket(bytes, bytes.size, address, PORT)

                DatagramSocket().use { socket ->
                    socket.broadcast = true
                    // Enviar 3 veces para asegurar la recepción en la red
                    repeat(3) {
                        socket.send(packet)
                        Thread.sleep(50)
                    }
                }
                Result.success(Unit)
            } catch (e: Exception) {
                e.printStackTrace()
                Result.failure(e)
            }
        }
    }
}
