package com.omnicast.tvsignage.controller

import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.CheckBox
import android.widget.EditText
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.omnicast.tvsignage.R
import com.omnicast.tvsignage.data.AppPreferences
import com.omnicast.tvsignage.data.TvDevice
import com.omnicast.tvsignage.databinding.ActivityControllerBinding
import com.omnicast.tvsignage.utils.WakeOnLanHelper
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream

class ControllerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityControllerBinding
    private lateinit var preferences: AppPreferences
    private lateinit var adapter: TvDeviceAdapter
    private val tvList = mutableListOf<TvDevice>()

    private val apiClient = TvApiClient()
    private var nsdDiscoveryHelper: NsdDiscoveryHelper? = null
    private var pendingTvForUpload: TvDevice? = null
    private var pendingLoopForUpload: Boolean = true

    // Selector de videos del sistema para subir a la TV
    private val pickVideoLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let { videoUri ->
            val tv = pendingTvForUpload ?: return@registerForActivityResult
            uploadVideoToTv(tv, videoUri, pendingLoopForUpload)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        preferences = AppPreferences(this)
        binding = ActivityControllerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupRecyclerView()
        setupListeners()
        loadSavedDevices()
        startDiscovery()
    }

    private fun setupRecyclerView() {
        adapter = TvDeviceAdapter(
            tvList = tvList,
            onWakeClick = { tv -> wakeTv(tv) },
            onSendUrlClick = { tv, loop -> showSendUrlDialog(tv, loop) },
            onUploadVideoClick = { tv, loop ->
                pendingTvForUpload = tv
                pendingLoopForUpload = loop
                pickVideoLauncher.launch("video/*")
            },
            onControlClick = { tv, action -> sendControl(tv, action) },
            onItemLongClick = { tv -> showManageDeviceDialog(tv) }
        )

        binding.rvTvDevices.layoutManager = LinearLayoutManager(this)
        binding.rvTvDevices.adapter = adapter
    }

    private fun setupListeners() {
        binding.btnScanNetwork.setOnClickListener {
            refreshDevices()
        }

        binding.btnAddManualTv.setOnClickListener {
            showAddManualTvDialog()
        }

        binding.swipeRefresh.setOnRefreshListener {
            refreshDevices()
            binding.swipeRefresh.isRefreshing = false
        }
    }

    private fun loadSavedDevices() {
        tvList.clear()
        tvList.addAll(preferences.getSavedTvs())
        updateEmptyState()
        adapter.notifyDataSetChanged()
        checkAllDevicesStatus()
    }

    private fun startDiscovery() {
        nsdDiscoveryHelper = NsdDiscoveryHelper(this) { name, host, port ->
            runOnUiThread {
                val existing = tvList.find { it.ipAddress == host }
                if (existing != null) {
                    existing.isOnline = true
                    existing.name = name
                    existing.port = port
                    adapter.notifyDataSetChanged()
                } else {
                    val newTv = TvDevice(
                        id = host,
                        name = name,
                        ipAddress = host,
                        port = port,
                        isOnline = true
                    )
                    tvList.add(newTv)
                    preferences.saveTvDevice(newTv)
                    updateEmptyState()
                    adapter.notifyDataSetChanged()
                }
                // Consultar información detallada de la TV recién descubierta
                checkDeviceStatus(tvList.first { it.ipAddress == host })
            }
        }
        nsdDiscoveryHelper?.startDiscovery()
    }

    private fun refreshDevices() {
        nsdDiscoveryHelper?.stopDiscovery()
        nsdDiscoveryHelper?.startDiscovery()
        checkAllDevicesStatus()
        Toast.makeText(this, "Escaneando red local...", Toast.LENGTH_SHORT).show()
    }

    private fun checkAllDevicesStatus() {
        tvList.forEach { tv ->
            checkDeviceStatus(tv)
        }
    }

    private fun checkDeviceStatus(tv: TvDevice) {
        lifecycleScope.launch {
            val result = apiClient.getStatus(tv)
            if (result.isSuccess) {
                val json = result.getOrNull()
                tv.isOnline = true
                if (json != null) {
                    if (json.has("mac")) tv.macAddress = json.get("mac").asString
                    if (json.has("currentVideo")) tv.currentVideoUrl = json.get("currentVideo").asString
                    if (json.has("isLooping")) tv.isLooping = json.get("isLooping").asBoolean
                }
                preferences.saveTvDevice(tv)
            } else {
                tv.isOnline = false
            }
            adapter.notifyDataSetChanged()
        }
    }

    private fun wakeTv(tv: TvDevice) {
        if (tv.macAddress.isEmpty()) {
            Toast.makeText(this, "Se requiere la MAC address para encender la TV", Toast.LENGTH_LONG).show()
            showEditMacDialog(tv)
            return
        }

        lifecycleScope.launch {
            val result = WakeOnLanHelper.wakeUp(tv.macAddress)
            if (result.isSuccess) {
                Toast.makeText(this@ControllerActivity, "Señal de encendido enviada a ${tv.name}", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this@ControllerActivity, "Error al enviar señal: ${result.exceptionOrNull()?.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun showSendUrlDialog(tv: TvDevice, loop: Boolean) {
        val input = EditText(this).apply {
            hint = "https://ejemplo.com/video.mp4"
            setText(tv.currentVideoUrl ?: "")
        }

        val chkDefault = CheckBox(this).apply {
            text = "Guardar como video predeterminado al encender la TV"
            isChecked = true
        }

        val container = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            setPadding(48, 24, 48, 24)
            addView(input)
            addView(chkDefault)
        }

        MaterialAlertDialogBuilder(this)
            .setTitle("Reproducir Video en ${tv.name}")
            .setMessage("Ingresa el enlace directo del video (MP4, MKV, HLS/M3U8):")
            .setView(container)
            .setPositiveButton("Reproducir") { _, _ ->
                val url = input.text.toString().trim()
                if (url.isNotEmpty()) {
                    lifecycleScope.launch {
                        val res = apiClient.sendPlayUrl(tv, url, loop, chkDefault.isChecked)
                        if (res.isSuccess) {
                            tv.currentVideoUrl = url
                            tv.isLooping = loop
                            preferences.saveTvDevice(tv)
                            adapter.notifyDataSetChanged()
                            Toast.makeText(this@ControllerActivity, "Video enviado con éxito", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(this@ControllerActivity, "Fallo al enviar: ${res.exceptionOrNull()?.message}", Toast.LENGTH_LONG).show()
                        }
                    }
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun uploadVideoToTv(tv: TvDevice, videoUri: Uri, loop: Boolean) {
        binding.layoutProgress.visibility = View.VISIBLE
        binding.tvProgressText.text = "Transfiriendo video a ${tv.name}..."

        lifecycleScope.launch {
            try {
                // Copiar URI a archivo temporal en caché para enviar por OkHttp
                val tempFile = File(cacheDir, "upload_${System.currentTimeMillis()}.mp4")
                contentResolver.openInputStream(videoUri)?.use { input ->
                    FileOutputStream(tempFile).use { output ->
                        input.copyTo(output)
                    }
                }

                val result = apiClient.uploadVideo(tv, tempFile, loop, saveAsDefault = true)
                binding.layoutProgress.visibility = View.GONE
                tempFile.delete()

                if (result.isSuccess) {
                    Toast.makeText(this@ControllerActivity, "¡Video enviado y guardado en la TV!", Toast.LENGTH_LONG).show()
                    checkDeviceStatus(tv)
                } else {
                    Toast.makeText(this@ControllerActivity, "Error al transferir: ${result.exceptionOrNull()?.message}", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                binding.layoutProgress.visibility = View.GONE
                Toast.makeText(this@ControllerActivity, "Error al preparar video: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun sendControl(tv: TvDevice, action: String) {
        lifecycleScope.launch {
            apiClient.sendControl(tv, action)
        }
    }

    private fun showAddManualTvDialog() {
        val view = layoutInflater.inflate(R.layout.dialog_add_tv, null)
        val etName = view.findViewById<EditText>(R.id.etDialogTvName)
        val etIp = view.findViewById<EditText>(R.id.etDialogTvIp)
        val etMac = view.findViewById<EditText>(R.id.etDialogTvMac)

        MaterialAlertDialogBuilder(this)
            .setTitle("Agregar TV Manualmente")
            .setView(view)
            .setPositiveButton("Guardar") { _, _ ->
                val name = etName.text.toString().ifEmpty { "TV Android" }
                val ip = etIp.text.toString().trim()
                val mac = etMac.text.toString().trim()

                if (ip.isNotEmpty()) {
                    val tv = TvDevice(
                        id = ip,
                        name = name,
                        ipAddress = ip,
                        macAddress = mac,
                        port = 8888
                    )
                    tvList.add(tv)
                    preferences.saveTvDevice(tv)
                    updateEmptyState()
                    adapter.notifyDataSetChanged()
                    checkDeviceStatus(tv)
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun showEditMacDialog(tv: TvDevice) {
        val input = EditText(this).apply {
            hint = "Ejemplo: AA:BB:CC:11:22:33"
            setText(tv.macAddress)
        }

        MaterialAlertDialogBuilder(this)
            .setTitle("Configurar MAC para Wake-on-LAN")
            .setMessage("Ingresa la dirección MAC de la tarjeta de red (Wi-Fi o LAN) del televisor:")
            .setView(input)
            .setPositiveButton("Guardar") { _, _ ->
                tv.macAddress = input.text.toString().trim()
                preferences.saveTvDevice(tv)
                adapter.notifyDataSetChanged()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun showManageDeviceDialog(tv: TvDevice) {
        val options = arrayOf("Editar MAC Address", "Eliminar de la lista")
        MaterialAlertDialogBuilder(this)
            .setTitle(tv.name)
            .setItems(options) { _, which ->
                when (which) {
                    0 -> showEditMacDialog(tv)
                    1 -> {
                        preferences.removeTvDevice(tv.id)
                        tvList.remove(tv)
                        updateEmptyState()
                        adapter.notifyDataSetChanged()
                    }
                }
            }
            .show()
    }

    private fun updateEmptyState() {
        binding.tvEmptyState.visibility = if (tvList.isEmpty()) View.VISIBLE else View.GONE
    }

    override fun onDestroy() {
        super.onDestroy()
        nsdDiscoveryHelper?.stopDiscovery()
    }
}
