package com.omnicast.tvsignage.utils

import android.content.Context
import android.net.wifi.WifiManager
import java.net.Inet4Address
import java.net.NetworkInterface
import java.util.Collections

object NetworkUtils {

    /**
     * Obtiene la dirección IP local IPv4 del dispositivo en la red Wi-Fi o Ethernet.
     */
    fun getLocalIpAddress(): String? {
        try {
            val interfaces = Collections.list(NetworkInterface.getNetworkInterfaces())
            for (intf in interfaces) {
                if (intf.isLoopback || !intf.isUp) continue
                val addrs = Collections.list(intf.inetAddresses)
                for (addr in addrs) {
                    if (!addr.isLoopbackAddress && addr is Inet4Address) {
                        return addr.hostAddress
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }

    /**
     * Intenta obtener la dirección MAC local de la interfaz activa.
     */
    fun getMacAddress(): String {
        try {
            val interfaces = Collections.list(NetworkInterface.getNetworkInterfaces())
            for (intf in interfaces) {
                if (intf.isLoopback || !intf.isUp) continue
                val macBytes = intf.hardwareAddress ?: continue
                val sb = StringBuilder()
                for (b in macBytes) {
                    sb.append(String.format("%02X:", b))
                }
                if (sb.isNotEmpty()) {
                    sb.deleteCharAt(sb.length - 1)
                }
                val mac = sb.toString()
                if (mac.isNotEmpty()) return mac
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return "00:00:00:00:00:00"
    }
}
