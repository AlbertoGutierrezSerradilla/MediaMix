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

        // Solicita permisos necesarios (audio, cámara, etc.)
        requestPermissions()

        // Asigna listeners a los botones de grabación
        binding.btnRecordVideo.setOnClickListener { toggleVideoRecording() }
        binding.btnRecordAudio.setOnClickListener { toggleAudioRecording() }

        // Inicia la cámara
        startCamera()
        // Crea un hilo para manejar la cámara en segundo plano
        cameraExecutor = Executors.newSingleThreadExecutor()
    }

    // Solicita los permisos de cámara y audio; si no se conceden, se cierra la actividad
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

    // Alterna entre iniciar y detener la grabación de audio
    private fun toggleAudioRecording() {
        if (isRecordingAudio) {
            stopAudioRecording()
        } else {
            startAudioRecording()
        }
    }

    // Configura parámetros y comienza a grabar audio
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

        // Hilo que escribirá el audio en un archivo mientras grabamos
        recordingThread = Thread {
            writeAudioDataToFile(bufferSize)
        }
        recordingThread?.start()
        binding.btnRecordAudio.setImageResource(R.drawable.ic_stop)
    }

    // Lee datos de audio y los escribe en el archivo
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

    // Detiene la grabación de audio y libera recursos
    private fun stopAudioRecording() {
        isRecordingAudio = false
        audioRecord?.stop()
        audioRecord?.release()
        audioRecord = null
        recordingThread?.join()
        binding.btnRecordAudio.setImageResource(R.drawable.ic_record_audio)
        Toast.makeText(this, "🎙️ Audio guardado!!", Toast.LENGTH_SHORT).show()
    }

    // Inicia la cámara con CameraX y configura el preview y la grabación
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
                cameraProvider.bindToLifecycle(
                    this,
                    cameraSelector,
                    preview,
                    videoCapture
                )
            } catch (exc: Exception) {
                Log.e("RecordingActivity", "Error al iniciar la cámara", exc)
            }
        }, ContextCompat.getMainExecutor(this))
    }

    // Alterna entre iniciar y detener la grabación de video
    private fun toggleVideoRecording() {
        if (isRecordingVideo) {
            stopVideoRecording()
        } else {
            startVideoRecording()
        }
    }

    // Comienza a grabar video, habilitando audio si está permitido
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

    // Detiene la grabación de video y muestra un mensaje
    private fun stopVideoRecording() {
        activeRecording?.stop()
        activeRecording = null
        isRecordingVideo = false
        binding.btnRecordVideo.setImageResource(R.drawable.ic_record_video)
        Toast.makeText(this, "🎬 Video guardado!!", Toast.LENGTH_SHORT).show()
    }

    // Genera un archivo de salida para audio (.pcm) o video (.mp4)
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
        val extension = if (type == "AUDIO") ".pcm" else ".mp4"
        return File(directory, "${type}_$timestamp$extension")
    }

    // Libera los recursos del Executor al finalizar la actividad
    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }
}
