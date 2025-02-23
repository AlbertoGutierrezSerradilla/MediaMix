package com.example.mediamix

import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.media3.exoplayer.ExoPlayer
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.mediamix.adapters.BibliotecaAdapter
import com.example.mediamix.databinding.FragmentBibliotecaBinding
import java.io.File

class FragmentoBiblioteca : Fragment() {

    private var _binding: FragmentBibliotecaBinding? = null
    private val binding get() = _binding!!
    private lateinit var archivoAdapter: BibliotecaAdapter
    private lateinit var exoPlayer: ExoPlayer
    private var listaArchivos = mutableListOf<File>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentBibliotecaBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        exoPlayer = ExoPlayer.Builder(requireContext()).build()
        binding.playerView.player = exoPlayer

        listaArchivos = cargarArchivos().toMutableList()
        archivoAdapter = BibliotecaAdapter(requireContext(), listaArchivos, exoPlayer) { uri ->
            reproducirVideo(uri)
        }

        binding.listaArchivos.layoutManager = LinearLayoutManager(requireContext())
        binding.listaArchivos.adapter = archivoAdapter
    }

    private fun cargarArchivos(): List<File> {
        val directorioMusica = requireContext().getExternalFilesDir(Environment.DIRECTORY_MUSIC)
        val directorioVideos = requireContext().getExternalFilesDir(Environment.DIRECTORY_MOVIES)

        val archivosMusica = directorioMusica?.listFiles()?.toList() ?: emptyList()
        val archivosVideos = directorioVideos?.listFiles()?.toList() ?: emptyList()

        return archivosMusica + archivosVideos
    }

    private fun reproducirVideo(uri: Uri) {
        binding.playerView.visibility = View.VISIBLE
        exoPlayer.setMediaItem(androidx.media3.common.MediaItem.fromUri(uri))
        exoPlayer.prepare()
        exoPlayer.play()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        exoPlayer.release()
        _binding = null
    }
}