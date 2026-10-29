package com.omnicast.tvsignage

import android.app.UiModeManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.omnicast.tvsignage.controller.ControllerActivity
import com.omnicast.tvsignage.data.AppPreferences
import com.omnicast.tvsignage.databinding.ActivityMainBinding
import com.omnicast.tvsignage.tv.TvPlayerActivity

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var preferences: AppPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        preferences = AppPreferences(this)

        // Si ya hay un rol seleccionado previamente, ir directo a su pantalla
        when (preferences.role) {
            AppPreferences.ROLE_TV -> {
                startTvMode()
                return
            }
            AppPreferences.ROLE_CONTROLLER -> {
                startControllerMode()
                return
            }
        }

        // Si es un televisor Android (Leanback / TV mode) y es la primera vez, autoconfigurar como TV
        if (isAndroidTvDevice()) {
            preferences.role = AppPreferences.ROLE_TV
            startTvMode()
            return
        }

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupListeners()
    }

    private fun setupListeners() {
        binding.cardModeTv.setOnClickListener {
            preferences.role = AppPreferences.ROLE_TV
            startTvMode()
        }
        binding.btnSelectTv.setOnClickListener {
            preferences.role = AppPreferences.ROLE_TV
            startTvMode()
        }

        binding.cardModeController.setOnClickListener {
            preferences.role = AppPreferences.ROLE_CONTROLLER
            startControllerMode()
        }
        binding.btnSelectController.setOnClickListener {
            preferences.role = AppPreferences.ROLE_CONTROLLER
            startControllerMode()
        }
    }

    private fun startTvMode() {
        val intent = Intent(this, TvPlayerActivity::class.java)
        startActivity(intent)
        finish()
    }

    private fun startControllerMode() {
        val intent = Intent(this, ControllerActivity::class.java)
        startActivity(intent)
        finish()
    }

    private fun isAndroidTvDevice(): Boolean {
        val uiModeManager = getSystemService(Context.UI_MODE_SERVICE) as? UiModeManager
        val isTelevision = uiModeManager?.currentModeType == Configuration.UI_MODE_TYPE_TELEVISION
        val hasLeanback = packageManager.hasSystemFeature(PackageManager.FEATURE_LEANBACK)
        return isTelevision || hasLeanback
    }
}
