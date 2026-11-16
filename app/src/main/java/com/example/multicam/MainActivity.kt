package com.example.multicam

import android.os.Bundle
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import com.example.multicam.databinding.ActivityMainBinding
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.util.UUID

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var cameraAdapter: CameraAdapter
    private val cameraList = mutableListOf<CameraItem>()
    private val PREFS_NAME = "multicam_prefs"
    private val KEY_CAMERAS = "saved_cameras"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        loadCameras()

        binding.recyclerViewCameras.layoutManager = GridLayoutManager(this, 2)
        cameraAdapter = CameraAdapter(this, cameraList) { camera ->
            deleteCamera(camera)
        }
        binding.recyclerViewCameras.adapter = cameraAdapter

        binding.fabAddCamera.setOnClickListener {
            showAddCameraDialog()
        }
    }

    private fun showAddCameraDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_camera, null)
        val etName = dialogView.findViewById<EditText>(R.id.etCameraName)
        val etUrl = dialogView.findViewById<EditText>(R.id.etRtspUrl)

        // Prellenar con tu URL de ejemplo
        etUrl.setText("rtsp://admin:Camara2026@192.168.1.201:554/live/ch00_0")
        etName.setText("Cámara Principal")

        AlertDialog.Builder(this)
            .setTitle("Agregar Widget de Cámara RTSP")
            .setView(dialogView)
            .setPositiveButton("Guardar") { _, _ ->
                val name = etName.text.toString().trim()
                val url = etUrl.text.toString().trim()
                if (name.isNotEmpty() && url.isNotEmpty()) {
                    val newCamera = CameraItem(id = UUID.randomUUID().toString(), name = name, rtspUrl = url)
                    cameraList.add(newCamera)
                    saveCameras()
                    cameraAdapter.notifyItemInserted(cameraList.size - 1)
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun deleteCamera(camera: CameraItem) {
        val index = cameraList.indexOf(camera)
        if (index != -1) {
            cameraList.removeAt(index)
            saveCameras()
            cameraAdapter.notifyItemRemoved(index)
            cameraAdapter.releaseAllPlayers()
            cameraAdapter.notifyDataSetChanged()
        }
    }

    private fun saveCameras() {
        val prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        val json = Gson().toJson(cameraList)
        prefs.edit().putString(KEY_CAMERAS, json).apply()
    }

    private fun loadCameras() {
        val prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        val json = prefs.getString(KEY_CAMERAS, null)
        if (json != null) {
            val type = object : TypeToken<List<CameraItem>>() {}.type
            val saved: List<CameraItem> = Gson().fromJson(json, type)
            cameraList.clear()
            cameraList.addAll(saved)
        } else {
            // Cámara por defecto inicial solicitada
            cameraList.add(
                CameraItem(
                    id = UUID.randomUUID().toString(),
                    name = "Cámara 1 (Principal)",
                    rtspUrl = "rtsp://admin:Camara2026@192.168.1.201:554/live/ch00_0"
                )
            )
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraAdapter.releaseAllPlayers()
    }
}
