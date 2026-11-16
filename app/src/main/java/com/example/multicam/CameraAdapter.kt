package com.example.multicam

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.rtsp.RtspMediaSource
import androidx.recyclerview.widget.RecyclerView
import com.example.multicam.databinding.ItemCameraWidgetBinding

class CameraAdapter(
    private val context: Context,
    private val cameras: List<CameraItem>,
    private val onDeleteClick: (CameraItem) -> Unit
) : RecyclerView.Adapter<CameraAdapter.CameraViewHolder>() {

    private val activePlayers = mutableMapOf<String, ExoPlayer>()

    inner class CameraViewHolder(val binding: ItemCameraWidgetBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CameraViewHolder {
        val binding = ItemCameraWidgetBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return CameraViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CameraViewHolder, position: Int) {
        val camera = cameras[position]
        holder.binding.tvCameraName.text = camera.name
        holder.binding.btnDelete.setOnClickListener { onDeleteClick(camera) }

        activePlayers[camera.id]?.release()

        val player = ExoPlayer.Builder(context).build().apply {
            val mediaItem = MediaItem.fromUri(camera.rtspUrl)
            val mediaSource = RtspMediaSource.Factory().createMediaSource(mediaItem)
            setMediaSource(mediaSource)
            prepare()
            playWhenReady = true
            repeatMode = Player.REPEAT_MODE_ALL
        }

        activePlayers[camera.id] = player
        holder.binding.playerView.player = player
    }

    override fun getItemCount(): Int = cameras.size

    fun releaseAllPlayers() {
        activePlayers.values.forEach { it.release() }
        activePlayers.clear()
    }
}
