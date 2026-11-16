package com.example.multicam

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.rtsp.RtspMediaSource
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import androidx.recyclerview.widget.RecyclerView
import com.example.multicam.databinding.ItemCameraWidgetBinding

@OptIn(UnstableApi::class)
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
        holder.binding.tvStatus.visibility = View.VISIBLE
        holder.binding.tvStatus.text = "Conectando..."
        holder.binding.btnDelete.setOnClickListener { onDeleteClick(camera) }

        activePlayers[camera.id]?.release()

        // Desactivar pista de audio para evitar que paquetes de voz bloqueen el video
        val trackSelector = DefaultTrackSelector(context).apply {
            setParameters(
                buildUponParameters()
                    .setTrackTypeDisabled(C.TRACK_TYPE_AUDIO, true)
            )
        }

        val player = ExoPlayer.Builder(context)
            .setTrackSelector(trackSelector)
            .build()
            .apply {
                val mediaItem = MediaItem.fromUri(camera.rtspUrl)
                // Forzar TCP y aumentar timeout para cámaras Wi-Fi
                val mediaSource = RtspMediaSource.Factory()
                    .setForceUseRtpTcp(true)
                    .setTimeoutMs(10000L)
                    .createMediaSource(mediaItem)

                addListener(object : Player.Listener {
                    override fun onPlaybackStateChanged(playbackState: Int) {
                        when (playbackState) {
                            Player.STATE_BUFFERING -> {
                                holder.binding.tvStatus.visibility = View.VISIBLE
                                holder.binding.tvStatus.text = "Conectando..."
                            }
                            Player.STATE_READY -> {
                                holder.binding.tvStatus.visibility = View.GONE
                            }
                            Player.STATE_ENDED -> {
                                holder.binding.tvStatus.visibility = View.VISIBLE
                                holder.binding.tvStatus.text = "Flujo finalizado"
                            }
                            else -> {}
                        }
                    }

                    override fun onPlayerError(error: PlaybackException) {
                        holder.binding.tvStatus.visibility = View.VISIBLE
                        holder.binding.tvStatus.text = "Error: ${error.errorCodeName}"
                    }
                })

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
