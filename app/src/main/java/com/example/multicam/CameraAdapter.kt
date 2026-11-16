package com.example.multicam

import android.content.Context
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.multicam.databinding.ItemCameraWidgetBinding
import org.videolan.libvlc.LibVLC
import org.videolan.libvlc.Media
import org.videolan.libvlc.MediaPlayer

class CameraAdapter(
    context: Context,
    private val cameras: List<CameraItem>,
    private val onDeleteClick: (CameraItem) -> Unit
) : RecyclerView.Adapter<CameraAdapter.CameraViewHolder>() {

    private val libVLC = LibVLC(
        context.applicationContext,
        arrayListOf("--network-caching=400", "--no-audio")
    )
    private val activePlayers = mutableMapOf<String, MediaPlayer>()

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

        activePlayers[camera.id]?.let { oldPlayer ->
            oldPlayer.stop()
            oldPlayer.detachViews()
            oldPlayer.release()
        }

        val mediaPlayer = MediaPlayer(libVLC)
        mediaPlayer.attachViews(holder.binding.videoLayout, null, false, false)

        mediaPlayer.setEventListener { event ->
            holder.itemView.post {
                when (event.type) {
                    MediaPlayer.Event.Opening -> {
                        holder.binding.tvStatus.visibility = View.VISIBLE
                        holder.binding.tvStatus.text = "Conectando..."
                    }
                    MediaPlayer.Event.Buffering -> {
                        if (event.buffering < 100f) {
                            holder.binding.tvStatus.visibility = View.VISIBLE
                            holder.binding.tvStatus.text = "Cargando (${event.buffering.toInt()}%)..."
                        } else {
                            holder.binding.tvStatus.visibility = View.GONE
                        }
                    }
                    MediaPlayer.Event.Playing,
                    MediaPlayer.Event.Vout -> {
                        holder.binding.tvStatus.visibility = View.GONE
                    }
                    MediaPlayer.Event.EncounteredError -> {
                        holder.binding.tvStatus.visibility = View.VISIBLE
                        holder.binding.tvStatus.text = "Error de conexión RTSP"
                    }
                    MediaPlayer.Event.EndReached -> {
                        holder.binding.tvStatus.visibility = View.VISIBLE
                        holder.binding.tvStatus.text = "Señal interrumpida"
                    }
                }
            }
        }

        val media = Media(libVLC, Uri.parse(camera.rtspUrl)).apply {
            setHWDecoderEnabled(true, false)
            addOption(":network-caching=400")
            addOption(":no-audio")
        }

        mediaPlayer.media = media
        media.release()
        mediaPlayer.play()

        activePlayers[camera.id] = mediaPlayer
    }

    override fun getItemCount(): Int = cameras.size

    fun stopAllPlayers() {
        activePlayers.values.forEach { player ->
            player.stop()
            player.detachViews()
            player.release()
        }
        activePlayers.clear()
    }

    fun releaseEngine() {
        stopAllPlayers()
        libVLC.release()
    }
}
