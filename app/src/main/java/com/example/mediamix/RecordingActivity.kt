package com.example.mediamix

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.*
import androidx.core.content.ContextCompat
import com.example.mediamix.databinding.ActivityRecordingBinding
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class RecordingActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRecordingBinding
    private var isRecordingVideo = false
    private var isRecordingAudio = false
    private lateinit var cameraExecutor: ExecutorService
    private var videoCapture: VideoCapture<Recorder>? = null
    private var activeRecording: Recording? = null
    private var audioRecorder: Recorder? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRecordingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        requestPermissions()
        binding.btnRecordVideo.setOnClickListener { toggleVideoRecording() }
        binding.btnRecordAudio.setOnClickListener { toggleAudioRecording() }

        startCamera()
        cameraExecutor = Executors.newSingleThreadExecutor()
    }

    private fun requestPermissions() {
        val permissions = mutableListOf(
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.CAMERA
        )

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.READ_MEDIA_VIDEO)
            permissions.add(Manifest.permission.READ_MEDIA_AUDIO)
        }

        val requestPermissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { permissionsMap ->
            if (!permissionsMap.values.all { it }) {
                Toast.makeText(this, "Permisos necesarios no otorgados", Toast.LENGTH_SHORT).show()
                finish()
            }
        }

        if (!permissions.all { ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED }) {
            requestPermissionLauncher.launch(permissions.toTypedArray())
        }
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(binding.viewFinder.surfaceProvider)
            }

            val recorder = Recorder.Builder()
                .setQualitySelector(QualitySelector.from(Quality.HIGHEST))
                .build()

            videoCapture = VideoCapture.withOutput(recorder)
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(this, cameraSelector, preview, videoCapture)
            } catch (exc: Exception) {
                Log.e("RecordingActivity", "Error al iniciar la cámara", exc)
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun toggleVideoRecording() {
        if (isRecordingVideo) {
            stopVideoRecording()
        } else {
            startVideoRecording()
        }
    }

    private fun startVideoRecording() {
        val videoFile = getOutputFile("VIDEO")
        val outputOptions = FileOutputOptions.Builder(videoFile).build()

        val hasAudioPermission = ContextCompat.checkSelfPermission(
            this, Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED

        val recording = videoCapture?.output?.prepareRecording(this, outputOptions)

        if (hasAudioPermission) {
            recording?.withAudioEnabled()
        }

        activeRecording = recording?.start(ContextCompat.getMainExecutor(this)) { recordEvent ->
            if (recordEvent is VideoRecordEvent.Finalize) {
                if (recordEvent.hasError()) {
                    Log.e("RecordingActivity", "Error al grabar video: ${recordEvent.cause}", recordEvent.cause)
                } else {
                    Toast.makeText(this, "Video guardado en: ${videoFile.absolutePath}", Toast.LENGTH_SHORT).show()
                }
            }
        }

        isRecordingVideo = true
        binding.btnRecordVideo.text = "Detener Video"
    }

    private fun stopVideoRecording() {
        activeRecording?.stop()
        activeRecording = null
        isRecordingVideo = false
        binding.btnRecordVideo.text = "Grabar Video"
    }

    private fun toggleAudioRecording() {
        if (isRecordingAudio) {
            stopAudioRecording()
        } else {
            startAudioRecording()
        }
    }

    private fun startAudioRecording() {
        val audioFile = getOutputFile("AUDIO")
        val outputOptions = FileOutputOptions.Builder(audioFile).build()

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "Permiso de grabación de audio no otorgado", Toast.LENGTH_SHORT).show()
            return
        }

        audioRecorder = Recorder.Builder()
            .setQualitySelector(QualitySelector.from(Quality.LOWEST))
            .build()

        activeRecording = audioRecorder?.prepareRecording(this, outputOptions)
            ?.withAudioEnabled()
            ?.start(ContextCompat.getMainExecutor(this)) { recordEvent ->
                if (recordEvent is VideoRecordEvent.Finalize) {
                    if (recordEvent.hasError()) {
                        Log.e("RecordingActivity", "Error al grabar audio: ${recordEvent.cause}", recordEvent.cause)
                    } else {
                        Toast.makeText(this, "Audio guardado en: ${audioFile.absolutePath}", Toast.LENGTH_SHORT).show()
                    }
                }
            }

        isRecordingAudio = true
        binding.btnRecordAudio.text = "Detener Audio"
    }

    private fun stopAudioRecording() {
        activeRecording?.stop()
        activeRecording = null
        isRecordingAudio = false
        binding.btnRecordAudio.text = "Grabar Audio"
    }

    private fun getOutputFile(type: String): File {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(System.currentTimeMillis())
        val extension = if (type == "AUDIO") ".m4a" else ".mp4"
        return File(getExternalFilesDir(null), "${type}_$timestamp$extension")
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }
}
