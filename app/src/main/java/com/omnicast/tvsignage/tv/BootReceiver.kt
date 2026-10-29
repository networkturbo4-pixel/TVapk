package com.omnicast.tvsignage.tv

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.omnicast.tvsignage.data.AppPreferences

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        Log.d("BootReceiver", "Acción de arranque recibida: $action")

        val preferences = AppPreferences(context)

        // Si este dispositivo está configurado como modo TV y tiene autoarranque activo
        if (preferences.role == AppPreferences.ROLE_TV && preferences.autoBootEnabled) {
            val launchIntent = Intent(context, TvPlayerActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            }
            context.startActivity(launchIntent)
        }
    }
}
