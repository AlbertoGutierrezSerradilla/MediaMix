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

    // Enlace a la vista del fragment usando ViewBinding
    private var _binding: FragmentBibliotecaBinding? = null
    private val binding get() = _binding!!

    // Adaptador personalizado para mostrar la lista de archivos de la biblioteca
    private lateinit var archivoAdapter: BibliotecaAdapter

    // ExoPlayer para reproducir archivos de audio/video
    private lateinit var exoPlayer: ExoPlayer

    // Lista mutable de archivos que se mostrarán en la interfaz
    private var listaArchivos = mutableListOf<File>()

    // Infla la vista del fragment (FragmentBibliotecaBinding)
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentBibliotecaBinding.inflate(inflater, container, false)
        return binding.root
    }

    // Configura la vista cuando ya ha sido creada y asociada al ciclo de vida
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Inicializa ExoPlayer y asigna el player a la vista de reproducción
        exoPlayer = ExoPlayer.Builder(requireContext()).build()
        binding.playerView.player = exoPlayer

        // Carga y asigna la lista de archivos locales al adaptador
        listaArchivos = cargarArchivos().toMutableList()
        archivoAdapter = BibliotecaAdapter(requireContext(), listaArchivos, exoPlayer) { uri ->
            reproducirVideo(uri)
        }

        // Asocia el adaptador a un RecyclerView con un LinearLayoutManager vertical
        binding.listaArchivos.layoutManager = LinearLayoutManager(requireContext())
        binding.listaArchivos.adapter = archivoAdapter
    }

    //Busca y retorna una lista de archivos presentes en las carpetas de música y videos.
    private fun cargarArchivos(): List<File> {
        val directorioMusica = requireContext().getExternalFilesDir(Environment.DIRECTORY_MUSIC)
        val directorioVideos = requireContext().getExternalFilesDir(Environment.DIRECTORY_MOVIES)

        val archivosMusica = directorioMusica?.listFiles()?.toList() ?: emptyList()
        val archivosVideos = directorioVideos?.listFiles()?.toList() ?: emptyList()

        // Combina ambos en una sola lista
        return archivosMusica + archivosVideos
    }

    //Reproduce el archivo de video a partir de la URI recibida.
    private fun reproducirVideo(uri: Uri) {
        binding.playerView.visibility = View.VISIBLE
        exoPlayer.setMediaItem(androidx.media3.common.MediaItem.fromUri(uri))
        exoPlayer.prepare()
        exoPlayer.play()
    }

    //Libera la vista y el ExoPlayer cuando el fragment ya no está visible.
    override fun onDestroyView() {
        super.onDestroyView()
        exoPlayer.release()
        _binding = null
    }
}
