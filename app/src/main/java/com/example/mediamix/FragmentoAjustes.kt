package com.example.mediamix

import android.content.Context
import android.content.SharedPreferences
import android.media.AudioManager
import android.os.Bundle
import android.os.Environment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatDelegate
import androidx.fragment.app.Fragment
import com.example.mediamix.databinding.FragmentAjustesBinding
import java.io.File

class FragmentoAjustes : Fragment() {

    private var _binding: FragmentAjustesBinding? = null
    private val binding get() = _binding!!
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var audioManager: AudioManager

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAjustesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        sharedPreferences = requireContext().getSharedPreferences("AppSettings", 0)
        audioManager = requireContext().getSystemService(Context.AUDIO_SERVICE) as AudioManager

        // Configurar y aplicar Modo Oscuro
        binding.switchModoOscuro.isChecked = sharedPreferences.getBoolean("ModoOscuro", false)
        binding.switchModoOscuro.setOnCheckedChangeListener { _, isChecked ->
            sharedPreferences.edit().putBoolean("ModoOscuro", isChecked).apply()
            AppCompatDelegate.setDefaultNightMode(
                if (isChecked) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO
            )
        }

        // Configurar SeekBar de Volumen
        val volumenMaximo = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        val volumenActual = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
        binding.seekBarVolumen.max = volumenMaximo
        binding.seekBarVolumen.progress = volumenActual

        binding.seekBarVolumen.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, progress, 0)
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        // Botón para eliminar archivos multimedia
        binding.btnEliminarArchivos.setOnClickListener {
            eliminarArchivos()
        }
    }

    private fun eliminarArchivos() {
        val directorioMusica = requireContext().getExternalFilesDir(Environment.DIRECTORY_MUSIC)
        val directorioVideos = requireContext().getExternalFilesDir(Environment.DIRECTORY_MOVIES)

        val archivosMusica = directorioMusica?.listFiles()
        val archivosVideos = directorioVideos?.listFiles()

        archivosMusica?.forEach { it.delete() }
        archivosVideos?.forEach { it.delete() }

        Toast.makeText(requireContext(), "Todos los archivos han sido eliminados", Toast.LENGTH_SHORT).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}