package com.omnicast.tvsignage.controller

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.omnicast.tvsignage.R
import com.omnicast.tvsignage.data.TvDevice
import com.omnicast.tvsignage.databinding.ItemTvDeviceBinding

class TvDeviceAdapter(
    private val tvList: MutableList<TvDevice>,
    private val onWakeClick: (TvDevice) -> Unit,
    private val onSendUrlClick: (TvDevice, Boolean) -> Unit,
    private val onUploadVideoClick: (TvDevice, Boolean) -> Unit,
    private val onControlClick: (TvDevice, String) -> Unit,
    private val onItemLongClick: (TvDevice) -> Unit
) : RecyclerView.Adapter<TvDeviceAdapter.TvViewHolder>() {

    inner class TvViewHolder(val binding: ItemTvDeviceBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TvViewHolder {
        val binding = ItemTvDeviceBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return TvViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TvViewHolder, position: Int) {
        val tv = tvList[position]
        val binding = holder.binding
        val context = binding.root.context

        binding.tvName.text = tv.name
        val macTxt = if (tv.macAddress.isNotEmpty()) tv.macAddress else "Sin MAC"
        binding.tvNetworkInfo.text = "IP: ${tv.ipAddress}:${tv.port} | MAC: $macTxt"

        if (tv.isOnline) {
            binding.tvStatus.text = "En Línea"
            binding.tvStatus.setTextColor(ContextCompat.getColor(context, R.color.online_green))
            binding.statusIndicator.setBackgroundResource(R.drawable.circle_green)
        } else {
            binding.tvStatus.text = "Desconectado / Standby"
            binding.tvStatus.setTextColor(ContextCompat.getColor(context, R.color.offline_red))
            binding.statusIndicator.setBackgroundResource(R.drawable.circle_red)
        }

        binding.tvCurrentVideo.text = if (!tv.currentVideoUrl.isNullOrEmpty()) {
            "Video: ${tv.currentVideoUrl}"
        } else {
            "Video: Sin video asignado"
        }

        binding.switchLoop.isChecked = tv.isLooping
        binding.switchLoop.setOnCheckedChangeListener { _, isChecked ->
            tv.isLooping = isChecked
        }

        binding.btnWake.setOnClickListener {
            onWakeClick(tv)
        }

        binding.btnSendUrl.setOnClickListener {
            onSendUrlClick(tv, binding.switchLoop.isChecked)
        }

        binding.btnUploadVideo.setOnClickListener {
            onUploadVideoClick(tv, binding.switchLoop.isChecked)
        }

        binding.btnPauseResume.setOnClickListener {
            onControlClick(tv, "resume")
        }

        binding.btnRestart.setOnClickListener {
            onControlClick(tv, "restart")
        }

        binding.root.setOnLongClickListener {
            onItemLongClick(tv)
            true
        }
    }

    override fun getItemCount(): Int = tvList.size

    fun updateDevices(newList: List<TvDevice>) {
        tvList.clear()
        tvList.addAll(newList)
        notifyDataSetChanged()
    }
}
