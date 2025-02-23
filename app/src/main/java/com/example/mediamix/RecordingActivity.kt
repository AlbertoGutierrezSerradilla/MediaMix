package com.example.mediamix

import android.Manifest
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.FileOutputOptions
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.camera.video.VideoRecordEvent
import androidx.core.content.ContextCompat
import com.example.mediamix.databinding.ActivityRecordingBinding
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
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

    private var audioRecord: AudioRecord? = null
    private var recordingThread: Thread? = null
    private var audioFile: File? = null

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

    // Funciones para la grabación de audio
    private fun toggleAudioRecording() {
        if (isRecordingAudio) {
            stopAudioRecording()
        } else {
            startAudioRecording()
        }
    }

    private fun startAudioRecording() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED
        ) {
            Toast.makeText(this, "Permiso de audio no concedido", Toast.LENGTH_SHORT).show()
            return
        }

        val sampleRate = 44100
        val channelConfig = AudioFormat.CHANNEL_IN_MONO
        val audioFormat = AudioFormat.ENCODING_PCM_16BIT
        val bufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)
        if (bufferSize <= 0) {
            Log.e("RecordingActivity", "Tamaño de buffer inválido")
            return
        }

        audioFile = getOutputFile("AUDIO")
        audioRecord = AudioRecord(MediaRecorder.AudioSource.MIC, sampleRate, channelConfig, audioFormat, bufferSize)

        isRecordingAudio = true
        audioRecord?.startRecording()

        recordingThread = Thread {
            writeAudioDataToFile(bufferSize)
        }
        recordingThread?.start()
        binding.btnRecordAudio.setImageResource(R.drawable.ic_stop)
    }

    private fun writeAudioDataToFile(bufferSize: Int) {
        try {
            FileOutputStream(audioFile).use { outputStream ->
                val audioData = ByteArray(bufferSize)
                while (isRecordingAudio) {
                    val read = audioRecord?.read(audioData, 0, bufferSize) ?: 0
                    if (read > 0) {
                        outputStream.write(audioData, 0, read)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("RecordingActivity", "Error al escribir audio en archivo", e)
        }
    }

    private fun stopAudioRecording() {
        isRecordingAudio = false
        audioRecord?.stop()
        audioRecord?.release()
        audioRecord = null
        recordingThread?.join()
        binding.btnRecordAudio.setImageResource(R.drawable.ic_record_audio)
        Toast.makeText(this, "🎙️ Audio guardado!!", Toast.LENGTH_SHORT).show()
    }

    // Función para inicializar la cámara y configurar la grabación de video
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

    // Funciones para la grabación de video
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

        val pendingRecording = videoCapture?.output
            ?.prepareRecording(this, outputOptions)
            ?.apply {
                if (hasAudioPermission) {
                    withAudioEnabled()
                }
            }
        activeRecording = pendingRecording?.start(ContextCompat.getMainExecutor(this)) { recordEvent ->
            if (recordEvent is VideoRecordEvent.Finalize) {
                if (recordEvent.hasError()) {
                    Log.e("RecordingActivity", "Error al grabar video: ${recordEvent.cause}", recordEvent.cause)
                } else {
                    Log.e("RecordingActivity", "Video guardado en: ${videoFile.absolutePath}", recordEvent.cause)
                }
                binding.btnRecordVideo.setImageResource(R.drawable.ic_record_video)
                isRecordingVideo = false
            }
        }
        binding.btnRecordVideo.setImageResource(R.drawable.ic_stop)
        isRecordingVideo = true
    }

    private fun stopVideoRecording() {
        activeRecording?.stop()
        activeRecording = null
        isRecordingVideo = false
        binding.btnRecordVideo.setImageResource(R.drawable.ic_record_video)
        Toast.makeText(this, "🎬 Video guardado!!", Toast.LENGTH_SHORT).show()
    }

    // Función para generar el archivo de salida (audio o video)
    private fun getOutputFile(type: String): File {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val directory = when (type) {
            "AUDIO" -> getExternalFilesDir(Environment.DIRECTORY_MUSIC)
            else -> getExternalFilesDir(Environment.DIRECTORY_MOVIES)
        }

        directory?.let {
            if (!it.exists()) {
                it.mkdirs()
            }
        }

        val extension = if (type == "AUDIO") ".pcm" else ".mp4"  // Ahora guarda en .pcm
        return File(directory, "${type}_$timestamp$extension")
    }
    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }
}