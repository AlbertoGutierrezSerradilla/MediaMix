package com.example.mediamix

import android.content.Context
import android.content.Intent
import android.media.MediaPlayer
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.recyclerview.widget.RecyclerView
import com.example.mediamix.R
import java.io.File

class BibliotecaAdapter(
    private val context: Context,
    private val listaArchivos: List<File>,
    private val exoPlayer: ExoPlayer,
    private val onVideoSelected: (Uri) -> Unit
) : RecyclerView.Adapter<BibliotecaAdapter.ArchivoViewHolder>() {

    private var mediaPlayer: MediaPlayer? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ArchivoViewHolder {
        val vista = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_archivo, parent, false)
        return ArchivoViewHolder(vista)
    }

    override fun onBindViewHolder(holder: ArchivoViewHolder, position: Int) {
        val archivo = listaArchivos[position]
        holder.nombreArchivo.text = archivo.name

        // Determinar si es un archivo de audio o video
        val esVideo = archivo.extension in listOf("mp4", "mkv", "avi")

        // Configurar el icono
        holder.iconoArchivo.setImageResource(
            if (esVideo) R.drawable.ic_video else R.drawable.ic_audio
        )

        // Configurar acción del botón de reproducción
        holder.btnReproducir.setOnClickListener {
            val uri = Uri.fromFile(archivo)
            if (esVideo) {
                onVideoSelected(uri) // Pasar el video para que lo reproduzca ExoPlayer
            } else {
                reproducirAudio(uri)
            }
        }

        // Configurar acción del botón de eliminar
        holder.btnEliminar.setOnClickListener {
            if (archivo.exists()) {
                archivo.delete()
                notifyItemRemoved(position)
                notifyItemRangeChanged(position, listaArchivos.size)
            }
        }

        // Configurar acción del botón de compartir
        holder.btnCompartir.setOnClickListener {
            compartirArchivo(archivo)
        }
    }

    override fun getItemCount(): Int = listaArchivos.size

    inner class ArchivoViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val iconoArchivo: ImageView = view.findViewById(R.id.iconoArchivo)
        val nombreArchivo: TextView = view.findViewById(R.id.nombreArchivo)
        val btnReproducir: ImageButton = view.findViewById(R.id.btnReproducir)
        val btnEliminar: ImageButton = view.findViewById(R.id.btnEliminar)
        val btnCompartir: ImageButton = view.findViewById(R.id.btnCompartir)
    }

    // Función para reproducir audio con MediaPlayer
    private fun reproducirAudio(uri: Uri) {
        mediaPlayer?.release()
        mediaPlayer = MediaPlayer().apply {
            setDataSource(context, uri)
            prepare()
            start()
        }
    }

    // Función para compartir archivos con otras apps
    private fun compartirArchivo(archivo: File) {
        val uri = Uri.fromFile(archivo)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "*/*"
            putExtra(Intent.EXTRA_STREAM, uri)
        }
        context.startActivity(Intent.createChooser(intent, "Compartir archivo"))
    }
}